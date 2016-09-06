/*
 * Copyright (c) 2016 Rafael Eichelberger, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.monitor.provider.packetHandler;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;

import org.opendaylight.monitor.provider.packetHandler.utils.TopologyHandler;
import org.opendaylight.monitor.provider.packetHandler.utils.TraceElement;
import org.opendaylight.monitor.provider.packetHandler.utils.TraceWriter;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Ethernet Packet Decoder
 */
public class PacketInListener implements PacketProcessingListener {

    private static final Logger LOG = LoggerFactory.getLogger(PacketInListener.class);

    private final static int PACKET_OFFSET_ETHERTYPE = 12;
    private final static int PACKET_OFFSET_IP = 14;
    private final static int PACKET_OFFSET_IP_TOS = PACKET_OFFSET_IP + 1;
    private final static int PACKET_OFFSET_IP_ID = PACKET_OFFSET_IP + 4;
    private final static int PACKET_OFFSET_IP_TTL = PACKET_OFFSET_IP + 8;

    private final static int PACKET_OFFSET_IP_SRC = PACKET_OFFSET_IP + 12;
    private final static int PACKET_OFFSET_IP_DST = PACKET_OFFSET_IP + 16;
    public final static int ETHERTYPE_IPV4 = 0x0800;
    public final static int VLAN = 0x08100;
    private final static short ECN_MASK = 3;

    public static final int COOKIE_BIGINT_HEX_RADIX = 16;
    public static final BigInteger TRACE_INGRESS_PROBE_COOKIE =
            new BigInteger("FF11FF", COOKIE_BIGINT_HEX_RADIX);

    public static final BigInteger TRACE_EGRESS_PROBE_COOKIE =
            new BigInteger("FF22FF", COOKIE_BIGINT_HEX_RADIX);

    public static final BigInteger CALIBRATION_PACKET =
            new BigInteger("FF33FF", COOKIE_BIGINT_HEX_RADIX);

    public static final BigInteger TRACE_FULL_COKIE =
            new BigInteger("FF44FF", COOKIE_BIGINT_HEX_RADIX);

    private static final int SCHEDULED_THREAD_POOL_SIZE = 1;
    private static final int QUEUE_SIZE = 1000;
    private static final int ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS = 300;
    private static final long SHUTDOWN_TIME = 5;

    public static final short PROBE_PACKET_FULL_TRACE_ID = 2;
    public static final short PROBE_PACKET_TIME_STAMP_ID = 3;


    private TraceWriter traceWriter = null;

    private PacketOutSender packetOutSender = null;

    ThreadPoolExecutor threadPoolExecutorService = null;

    long timeFromFirstElement = 0;

    long timeFromLastTrace = 0;

    int chainUnity = 0;

    private List<TraceElement> traceOut = new ArrayList<>();


    public void close() throws ExecutionException, InterruptedException {
        // When we close this service we need to shutdown our executor!
        threadPoolExecutorService.shutdown();
        if (!threadPoolExecutorService.awaitTermination(SHUTDOWN_TIME, TimeUnit.SECONDS)) {
            LOG.error("PacketInListener Executor did not terminate in the specified time.");
            List<Runnable> droppedTasks = threadPoolExecutorService.shutdownNow();
            LOG.error("PacketInListener Executor was abruptly shut down. [{}] tasks will not be executed.",
                    droppedTasks.size());
        }
        traceWriter.close();
    }

    public PacketInListener(NotificationProviderService notificationProviderService, PacketOutSender packetOutSender) {

        this.packetOutSender = packetOutSender;
        this.threadPoolExecutorService = new ThreadPoolExecutor(SCHEDULED_THREAD_POOL_SIZE, 50,
                ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(QUEUE_SIZE));

        notificationProviderService.registerNotificationListener(this);

        try {
            traceWriter = new TraceWriter(new File("/tmp/trace"));
        } catch (IOException e) {
            LOG.error("Error open trace file");
        }
        traceWriter.open();
    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        long intime = System.currentTimeMillis();
        if (packetReceived == null) {
            return;
        }
        BigInteger cookie = packetReceived.getFlowCookie().getValue();
        // Make sure the PacketIn is due to our Classification table pktInAction
        if (cookie.equals(TRACE_INGRESS_PROBE_COOKIE) || cookie.equals(TRACE_EGRESS_PROBE_COOKIE) || cookie.equals(TRACE_FULL_COKIE)) {
            ProcessPaketIn processPaketIn = new ProcessPaketIn(packetReceived, intime);
            try {
                threadPoolExecutorService.execute(processPaketIn);
            } catch (Exception ex) {
                LOG.error("error trying to configure SFC monitor rule", ex.toString());
            }

        } else if (cookie.equals(CALIBRATION_PACKET)) {

            final String nodeName =
                    packetReceived.getIngress()
                            .getValue()
                            .firstKeyOf(Node.class, NodeKey.class)
                            .getId().getValue();

            LOG.info("get packet {}  {}", nodeName.toString(), intime);

        }
    }

    private int getEtherType(final byte[] rawPacket) {
        final byte[] etherTypeBytes = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_ETHERTYPE, PACKET_OFFSET_ETHERTYPE + 2);
        return packShort(etherTypeBytes, 2);
    }

    private byte[] popVlan(final byte[] rawPacket) {
        final byte[] etherTypeBytes = Arrays.copyOfRange(rawPacket, 0, PACKET_OFFSET_ETHERTYPE);
        final byte[] payload = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_ETHERTYPE + 4, rawPacket.length);
        return concatenateByteArrays(etherTypeBytes, payload);
    }

    /**
     * Given a raw packet, return the SrcIp
     *
     * @param rawPacket
     * @return srcIp String
     */
    private String getSrcIpStr(final byte[] rawPacket) {
        final byte[] ipSrcBytes = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP_SRC, PACKET_OFFSET_IP_SRC + 4);
        String pktSrcIpStr = null;
        try {
            pktSrcIpStr = InetAddress.getByAddress(ipSrcBytes).getHostAddress();
        } catch (Exception e) {
            LOG.error("Exception getting Src IP address [{}]", e.getMessage(), e);
        }

        return pktSrcIpStr;
    }

    /**
     * Given a raw packet, return the DstIp
     *
     * @param rawPacket
     * @return dstIp String
     */
    private String getDstIpStr(final byte[] rawPacket) {
        final byte[] ipDstBytes = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP_DST, PACKET_OFFSET_IP_DST + 4);
        String pktDstIpStr = null;
        try {
            pktDstIpStr = InetAddress.getByAddress(ipDstBytes).getHostAddress();
        } catch (Exception e) {
            LOG.error("Exception getting Dst IP address [{}]", e.getMessage(), e);
        }

        return pktDstIpStr;
    }

    /**
     * Simple internal utility function to convert from a 2-byte array to a short
     *
     * @param bytes
     * @return the bytes packed into a short
     */
    private int packShort(byte[] bytes, int nByte) {
        short shortVal;
        if (nByte == 2) {
            shortVal = (short) ((bytes[0] << 8) | (bytes[1]));
        } else if (nByte == 1) {
            shortVal = (short) (bytes[0]);
        } else {
            return 0;
        }

        return shortVal >= 0 ? shortVal : 0x10000 + shortVal;
    }

    byte[] concatenateByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private int getEcn(final byte[] rawPacket) {
        final byte[] ipFirtsByte = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP_TOS, PACKET_OFFSET_IP_TOS + 1);

        int tos = packShort(ipFirtsByte, 1);
        int ecn = 0;
        ecn = tos & ECN_MASK;
        return ecn;
    }

    private int getIpId(final byte[] rawPacket) {
        final byte[] ipFirtsByte = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP_ID, PACKET_OFFSET_IP_ID + 2);

        int ipId = packShort(ipFirtsByte, 2);
        return ipId;
    }

    private int getIpTtl(final byte[] rawPacket) {
        final byte[] ipFirtsByte = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP_TTL, PACKET_OFFSET_IP_TTL + 1);

        int ipTtl = packShort(ipFirtsByte, 1);
        return ipTtl;
    }

    private TraceElement getLastTraceElement() {
        return traceOut.get(traceOut.size() - 1);
    }


    /**
     * A thread class used to detect pp and generate a SFC trace
     */
    class ProcessPaketIn implements Runnable {

        PacketReceived packetReceived;
        long inTime;

        public ProcessPaketIn(PacketReceived packetReceived, long inTime) {
            this.packetReceived = packetReceived;
            this.inTime = inTime;
        }


        public void run() {
            String packetDirec = "IN";
            if (packetReceived.getFlowCookie().getValue().equals(TRACE_EGRESS_PROBE_COOKIE)) {
                packetDirec = "OUT";
            }
            final byte[] rawPacketOrig = packetReceived.getPayload();
            int eth = getEtherType(rawPacketOrig);
            // Get the EtherType and check that its an IP packet
            if (eth != ETHERTYPE_IPV4 && eth != VLAN) {
                LOG.debug("PacketInListener discarding NON-IPv4");
                return;
            }

            byte[] rawPacket = rawPacketOrig;
            if (eth == VLAN) {
                rawPacket = popVlan(rawPacketOrig);
                eth = getEtherType(rawPacket);
                if (eth != ETHERTYPE_IPV4) {
                    LOG.debug("PacketInListener discarding NON-IPv4 after VLAN");
                    return;
                }
            }

            if (getEcn(rawPacket) != PROBE_PACKET_FULL_TRACE_ID && getEcn(rawPacket) != PROBE_PACKET_TIME_STAMP_ID) {
                return;
            }

            LOG.info("PacketInListener get packet identification - [{}]", getIpId(rawPacket));
            LOG.info("PacketInListener get packet ttl - [{}]", getIpTtl(rawPacket));


            // Get the SrcIp and DstIp Addresses
            String pktSrcIpStr = getSrcIpStr(rawPacket);
            if (pktSrcIpStr == null) {
                LOG.error("PacketInListener Cant get Src IP address, discarding packet");
                return;
            }

            String pktDstIpStr = getDstIpStr(rawPacket);
            if (pktDstIpStr == null) {
                LOG.error("PacketInListener Cant get Src IP address, discarding packet");
                return;
            }


            // Get the metadata
            if (packetReceived.getMatch() == null) {
                LOG.error("PacketInListener Cant get packet flow match");
                return;
            }
            if (packetReceived.getMatch().getMetadata() != null) {
                //LOG.info("PacketInListener get packet flow match metadata");


                Metadata pktMatchMetadata = packetReceived.getMatch().getMetadata();
                BigInteger metadata = pktMatchMetadata.getMetadata();

                short ulPathId = metadata.shortValue();
                // Assuming the RSP is symmetric
                short dlPathId = (short) (ulPathId + 1);

                //LOG.info("++++ PacketInListener Src IP [{}] Dst IP [{}] ulPathId [{}] dlPathId [{}]",
                //        pktSrcIpStr, pktDstIpStr, ulPathId, dlPathId);
            }
            // Get the Node name, by getting the following
            // - Ingress nodeConnectorRef
            // - instanceID for the Node in the tree above us
            // - instance identifier for the nodeConnectorRef
            final String nodeName =
                    packetReceived.getIngress()
                            .getValue()
                            .firstKeyOf(Node.class, NodeKey.class)
                            .getId().getValue();

            final NodeConnectorId nodeConector =
                    packetReceived.getIngress()
                            .getValue()
                            .firstKeyOf(NodeConnector.class, NodeConnectorKey.class)
                            .getId();//.getValue();

            TopologyHandler topo = new TopologyHandler();


            //LOG.info("+++++ packet {} time [{}]", packetDirec, inTime);

            //LOG.info("+++++ NodeName [{}] => {}", nodeName, nodeConector.getValue());
            TerminationPoint tp = topo.readTerminationPoint(nodeName, nodeConector.getValue());


            if (tp == null) {
                LOG.error("Not found Termination point [{}]", nodeName);
            }

            ServiceFunctionForwarder sff = topo.readSFF(nodeName);

            // get next output port
            BigInteger metadataPort = packetReceived.getMatch().getMetadata().getMetadata();
            String outSfDpl = null;
            if (metadataPort != null) {
                outSfDpl = topo.readSfDplFromSff(sff, metadataPort.toString());
            } else {
                LOG.error("could not read metadata");
            }

            // get input from previous SF
            String parts[] = tp.getTpId().getValue().split(":");
            String inSfDpl = topo.readSfDplFromSff(sff, parts[2]);


            ServiceFunction sfOut = null;
            if (packetReceived.getFlowCookie().getValue().equals(TRACE_FULL_COKIE)) {
                if (inTime > timeFromLastTrace + 10 * 1000) {
                    LOG.info("cleaning previous trace information");
                    traceOut.clear();
                    traceOut = new ArrayList<>();
                    chainUnity = 0;
                }
                timeFromLastTrace = inTime;
                String printTraceOut = String.format("[%s - %s - %s] -", parts[2], sff.getName().getValue(), metadataPort.toString());

                TraceElement te = TraceElement.setTraceNode(sff.getName().getValue(), getIpTtl(rawPacket));
                te.setTraceHop(printTraceOut);
                traceOut.add(te);


                if (outSfDpl != null) {
                    sfOut = topo.readSfName(outSfDpl);
                    if (sfOut != null) {
                        printTraceOut = String.format("%s[%s] - ", printTraceOut, sfOut.getName().getValue());
                        TraceElement teF = TraceElement.setTraceNode(sfOut.getName().getValue(), getIpTtl(rawPacket));
                        teF.setTraceHop(String.format("[%s] - ", sfOut.getName().getValue()));
                        traceOut.add(teF);
                    } else {
                        LOG.error("could no find SF in the dpl {}", outSfDpl);
                    }
                }

                LOG.info(printTraceOut);


                traceWriter.append(printTraceOut);

                Collections.sort(traceOut);

                LOG.info("Traceout::::    ");
                for (TraceElement trace : traceOut) {
                    String timeTrace = String.format("{[%s]:  %s} - ", trace.getNodeName(), trace.getTraceHop());
                    LOG.info(timeTrace);
                }

                //packetOutSender.sendPacketToPort(nodeName, packetReceived.getMatch().getMetadata().getMetadata().toString(), packetReceived.getPayload());
            } else if(packetReceived.getFlowCookie().getValue().equals(TRACE_EGRESS_PROBE_COOKIE)) {

                if (traceOut.size() == 0) {
                    LOG.error("There is no trace information to colect timestamps");
                    return;
                }

                sfOut = topo.readSfName(outSfDpl);
                if (sfOut != null) {
                    for (TraceElement traceElement : traceOut) {
                        traceElement.setTime(sff.getName().getValue(), chainUnity, 6);
                        traceElement.incremmentPktCount(sfOut.getName().getValue());
                        String timeTrace = String.format("{[%d:c(%d)]%d [%s]} - ", traceElement.timesSize(), traceElement.getPktCount(), traceElement.getLastTime(), traceElement.getNodeName());
                        LOG.info(timeTrace);
                        if (traceElement.setTime(sfOut.getName().getValue(), chainUnity, inTime)) {
                            break;
                        }
                    }
                } else if ( sff != null) {
                    for (TraceElement traceElement : traceOut) {
                        if (traceElement.setTime(sff.getName().getValue(), chainUnity, inTime)) {
                            if (traceElement.getNodeName().equals(traceOut.get(0).getNodeName())) {
                                timeFromFirstElement = inTime;
                            }
                            if (traceElement.getNodeName().equals(getLastTraceElement().getNodeName()) && getLastTraceElement().timesSize() > chainUnity) {
                                chainUnity++;
                                LOG.info("Times: ------");
                                for (TraceElement trace : traceOut) {
                                    String timeTrace = String.format("{[%d:c(%d)]%d [%s]} - ", trace.timesSize(), trace.getPktCount(), trace.getLastTime(), trace.getNodeName());
                                    LOG.info(timeTrace);
                                }
                            }
                            break;
                        }
                    }
                }

            }
        }
    }
}
