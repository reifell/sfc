/*
 * Copyright (c) 2016 Rafael Eichelberger, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.monitor.packetHandler;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;

import org.opendaylight.monitor.packetHandler.utils.TraceElement;
import org.opendaylight.monitor.packetHandler.utils.TopologyHandler;
import org.opendaylight.monitor.packetHandler.utils.TraceWriter;
import org.opendaylight.monitor.provider.TraceRpc;
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
import java.util.concurrent.*;

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
    private final static int PACKET_OFFSET_IP_PROTO = PACKET_OFFSET_IP + 9;
    private final static int PACKET_OFFSET_UDP = PACKET_OFFSET_IP + 20;
    private final static int PACKET_OFFSET_NSH = PACKET_OFFSET_IP + 8 ; // eth + nsh offsets




    private final static int PACKET_OFFSET_IP_SRC = PACKET_OFFSET_IP + 12;
    private final static int PACKET_OFFSET_IP_DST = PACKET_OFFSET_IP + 16;
    public final static int ETHERTYPE_IPV4 = 0x0800;
    public final static int ETHERTYPE_NSH = 0x894f;

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

    public static final String TABLE_ZERO_IDENTIFICATION = "8989";

    private static final int SCHEDULED_THREAD_POOL_SIZE = 1;
    private static final int QUEUE_SIZE = 10000;
    private static final int ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS = 300;
    private static final long SHUTDOWN_TIME = 5;

    public static final short PROBE_PACKET_FULL_TRACE_ID = 2;
    public static final short PROBE_PACKET_TIME_STAMP_ID = 3;

    private static final int TEN_SECONDS = 10 * 1000;
    private static final int TREE_SECONDS = 3 * 1000;

    private static PacketInListener packetInListenerObj = null;

    private TraceWriter traceLogWriter = null;
    private TraceWriter traceWriter = null;
    private TraceWriter plotWriter = null;
    private int nFiles = 0;


    private PacketOutSender packetOutSender = null;

    ThreadPoolExecutor threadPoolExecutorService = null;

    long timetoUpdate = 0;

    long timeFromLastTrace = 0;

    private ConcurrentHashMap<Long, Set<TraceElement>> traceMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String,  Set<TraceElement>> storedTraces = new ConcurrentHashMap<>();

    private ConcurrentSkipListSet<Long> packetInCounter = new ConcurrentSkipListSet<>();


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
        plotWriter.close();
        traceLogWriter.close();
    }

    public PacketInListener(NotificationProviderService notificationProviderService, PacketOutSender packetOutSender) {

        this.packetOutSender = packetOutSender;
        this.threadPoolExecutorService = new ThreadPoolExecutor(SCHEDULED_THREAD_POOL_SIZE, 500,
                ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(QUEUE_SIZE));

        notificationProviderService.registerNotificationListener(this);
        if (packetInListenerObj == null) {
            packetInListenerObj = this;
        }

        TraceRpc traceRpc = new TraceRpc();
        traceRpc.putPacketInListerObject(this);

        //final BindingAwareBroker.RpcRegistration<SfcMonitorImplService> rpcRegistration;
        //rpcProviderRegistry.addRpcImplementation(SfcMonitorImplService.class, traceRpc);


        try {
            traceWriter = new TraceWriter(new File("/tmp/trace"));
            plotWriter = new TraceWriter(new File("/tmp/plot.csv"));
            traceLogWriter = new TraceWriter(new File("/tmp/trace.log"));
        } catch (IOException e) {
            LOG.error("Error open trace file");
        }
        traceWriter.open();
        plotWriter.open();
        traceLogWriter.open();

    }


    public static PacketInListener getPacketInListenerObj() {
        if (packetInListenerObj != null) {
            return PacketInListener.packetInListenerObj;
        } else {
            LOG.error("PacketInListener not initialized");
            return null;
        }
    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        long intime = System.currentTimeMillis();
        if (packetReceived == null) {
            return;
        }
        packetInCounter.add(intime);

        BigInteger cookie = packetReceived.getFlowCookie().getValue();
        // Make sure the PacketIn is due to our Classification table pktInAction
        if (cookie.equals(TRACE_INGRESS_PROBE_COOKIE) || cookie.equals(TRACE_EGRESS_PROBE_COOKIE) || cookie.equals(TRACE_FULL_COKIE)) {
            ProcessPaketIn processPaketIn = new ProcessPaketIn(packetReceived, intime);
            try {
                threadPoolExecutorService.execute(processPaketIn);
            } catch (Exception ex) {
                LOG.error("error processing SFC trace collection {}", ex.getMessage());
                throw ex;

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

    private byte[] popNsh(final byte[] rawPacket) {
        int nshLan = getNshLen(rawPacket);
        final byte[] payload = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP + (4 * nshLan) , rawPacket.length);
        return payload;
    }

    /**
     * Given a raw packet, return the SrcIp
     *
     * @param rawPacket
     * @return srcIp String
     */
    private int getSrcIp(final byte[] rawPacket) { // just the end of the addres
        final byte[] ipSrcBytes = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP_SRC +2, PACKET_OFFSET_IP_SRC + 4);

        int ipSrcEnd = packShort(ipSrcBytes, 2);

        return ipSrcEnd;

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
     static int packShort(byte[] bytes, int nByte) {
        short shortVal;
        if (nByte == 2) {
            shortVal = (short) (((bytes[0]& 0xff) << 8) | (bytes[1]& 0xff));
        } else if (nByte == 1) {
            shortVal = (short) (bytes[0]);
        } else {
            return 0;
        }

        return shortVal >= 0 ? shortVal : shortVal & 0xFFFF;
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
    //get IP ID + flags and fragmentation

    private long getPacktIdentification (final byte[] rawPacket) {
        //LOG.info("ipFlag {} ipId {}", getIpflags(rawPacket), getIpId(rawPacket));
        return (((long)getSrcIp(rawPacket)) << 32) | (getIpId(rawPacket) & 0xffffffffL);
    }

    private int getIpflags(final byte[] rawPacket) {
        final byte[] ipFirtsByte = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP_ID +2, PACKET_OFFSET_IP_ID + 2 + 2);

        int ipflags = packShort(ipFirtsByte, 2);
        return ipflags;
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


    private int getNshLen(final byte[] rawPacket) {
        final byte[] ipFirtsByte = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP + 1, PACKET_OFFSET_IP + 1 + 1);

        int nshLen = packShort(ipFirtsByte, 1);
        return nshLen;
    }

    private int getNsp(final byte[] rawPacket) {
        final byte[] frst2Bytes = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP + 4, PACKET_OFFSET_IP + 4 + 2);
        final byte[] secondByte = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP + 6, PACKET_OFFSET_IP + 6 + 1);

        int left = packShort(frst2Bytes, 2);
        int right = packShort(secondByte, 1);
        int nsp = (left << 16) | (right & 0xFFFF);
        return nsp;
    }

    private int getNsi(final byte[] rawPacket) {
        final byte[] ipFirtsByte = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP + 7, PACKET_OFFSET_IP + 7 + 1);

        int nsi = (0x000000FF & ((int)ipFirtsByte[0]));
        return nsi;
    }

    private int getUDPPort(final byte[] rawPacket) {
        final byte[] ipFirtsByte = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_UDP + 2, PACKET_OFFSET_UDP + 4);

        int udpPort = packShort(ipFirtsByte, 2);
        return udpPort;
    }


    public void cleanTraceMaps() {
        traceMap.clear();
        storedTraces.clear();
        packetInCounter.clear();
        plotWriter.close();
        traceLogWriter.close();
        nFiles++;
        String FileName = String.format("/tmp/plot-%d.csv", nFiles);
        String logFileName = String.format("/tmp/trace-%d.log", nFiles);
        try {
            plotWriter = new TraceWriter(new File(FileName));
            plotWriter.open();
            traceLogWriter = new TraceWriter(new File(logFileName));
            traceLogWriter.open();

        } catch (IOException e) {
            LOG.error("Error open plot file {}", FileName);
        }
    }

    public String getTrace() {
        updateStoredChains(System.currentTimeMillis());
        LOG.info("print trace information");

        traceWriter.append("Start: [ ");
        int sizeout = storedTraces.entrySet().size();
        StringBuilder output = new StringBuilder();
        for (ConcurrentHashMap.Entry<String, Set<TraceElement>> entry : storedTraces.entrySet()) {
            LOG.info(":::::::::::::::: Traceout {} ::::::::::::::    ", entry.getKey());
            output.append(String.format(" |   Traceout %s \n", entry.getKey()));
            traceWriter.append("[ ");
            int size = entry.getValue().size();
            int i = 0;
            for (TraceElement trace : entry.getValue()) {

                i++;
                traceWriter.append(trace.getTraceGraph());
                if (--size != 0) {
                    traceWriter.append(",");
                }
                traceWriter.append("] ");
                if (--sizeout != 0) {
                    traceWriter.append(",");
                }

                //skip first all zeros computation for hops delay
                if( i == 1) {
                    String traceFormat = String.format("{[%d] %s} - <%d>", trace.getPktCount(), trace.getTraceHop(), trace.getLastTime());
                    LOG.info(traceFormat);
                    output.append(String.format("[%d] %s \n", trace.getPktCount(), trace.getTraceHop()));
                    String plotElement = String.format("[%s, %s]\n", entry.getKey(), trace.getTraceHop());
                    plotWriter.append(plotElement);
                    continue;
                }
//                            String hopDStr = null;
//                            for (TraceElement.TimeAndHop hopD : trace.getTimeAndHop()) {
//                                hopDStr += String.format("%d, ", hopD.hopDelay);
//                            }
//                            LOG.info(" delays [{}]", hopDStr);
                float avgDelay = trace.getHopDelayAverage();
                String avfHopDelay = String.format("avg hop delay (%.2f)", avgDelay);
                LOG.info(avfHopDelay);
                output.append(String.format("avg delay (%.2f) \n", avgDelay));


                String traceFormat = String.format("{[%d] %s} - <%d>", trace.getPktCount(), trace.getTraceHop(), trace.getLastTime());
                LOG.info(traceFormat);
                output.append(String.format("[%d] %s \n", trace.getPktCount(), trace.getTraceHop()));

                String plotter = trace.getPlot(packetInCounter);
                if (plotter != null) {
                    plotWriter.append(plotter);
                }
                String plotElement = String.format("[%s, %s]\n", entry.getKey(), trace.getTraceHop());
                plotWriter.append(plotElement);

            }

        }
        traceWriter.append("] ");
        LOG.info("mapSize {} ", traceMap.size());
        return output.toString();
    }

    private synchronized void updateStoredChains(long inTime) {
//        int i = 0;
//
//        for (ConcurrentHashMap.Entry<Long, Set<TraceElement>> traceMapElement : traceMap.entrySet()) {
//            if(traceMapElement.getValue().size() != 3) {
//                LOG.error("[xxx] size {}", traceMapElement.getValue().size());
//                i++;
//            }
//
//        }
//        LOG.error("total size {}", i);


        ArrayList<Long> found = new ArrayList<>();
        for (ConcurrentHashMap.Entry<Long, Set<TraceElement>> traceMapElement : traceMap.entrySet()) {

            int size = traceMapElement.getValue().size();
            if (size == 0) continue;
            Iterator<TraceElement> itTrace = traceMapElement.getValue().iterator();
            //just store older traces
            if (inTime > traceMapElement.getValue().iterator().next().getInTime() + TEN_SECONDS ||
                    inTime > timeFromLastTrace + TREE_SECONDS) {
                boolean foundTrace = false;

                //sort by ttl to keep consistency with trace hops
                //Collections.sort(traceMapElement.getValue(), new TraceElement.TraceEmentComparator());

                for (Set<TraceElement> tracesFromStore : storedTraces.values()) {

                    //compare the ordered trace using the name of each hop

                    if (TraceElement.isSameChain(tracesFromStore, traceMapElement.getValue())) {
                        //update timestamp from each hop
                        long previousHopDelay = 0;

                        //iterate in the stored chain to add new timestamps from traceMap
                        for (TraceElement trace : tracesFromStore) {
                            TraceElement traceElement = itTrace.next();
                            int hopDelay;
                            long hopTime = traceElement.getInTime();
                            if (previousHopDelay == 0) {
                                hopDelay = 0;
                            } else {
                                hopDelay = (int) (hopTime - previousHopDelay);
                                if (hopDelay < 0) hopDelay = 0;
                            }
                            trace.setTimeAndHopDelay(hopTime, hopDelay);
                            previousHopDelay = hopTime;
                        }
                        foundTrace = true;
                        break;
                    }
                }
                if (!foundTrace) {
                    String chainName = String.format("chain-%d", storedTraces.size());
                    storedTraces.putIfAbsent(chainName, traceMapElement.getValue());
                }
                found.add(traceMapElement.getKey());
            }
        }
        for (Long key : found) {
            traceMap.remove(key);
        }
    }

    private byte[] getPayLoad(final byte[] inPacket) {
        int eth = getEtherType(inPacket);

        // Get the EtherType and check that its an IP packet
        if (eth != ETHERTYPE_IPV4 && eth != VLAN && eth != ETHERTYPE_NSH) {
            LOG.error("PacketInListener discarding NON-IPv4");
            return null;
        }

        byte[] rawPacketVlan = inPacket;
        if (eth == VLAN) {
            rawPacketVlan = popVlan(inPacket);
            eth = getEtherType(rawPacketVlan);
            if (eth != ETHERTYPE_IPV4) {
                LOG.error("PacketInListener discarding NON-IPv4 after VLAN");
                return null;
            }
        }
        return  rawPacketVlan;
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

            byte[] rawPacketVlan = getPayLoad(packetReceived.getPayload());

            if (rawPacketVlan == null) {
                return;
            }

            byte[] rawPacketNSH = rawPacketVlan;
            // test if the packet is encapsulated with VxLAN
            int eth = getEtherType(rawPacketVlan);
            int nsp = 0;
            int nsi = 0;
            if (eth == ETHERTYPE_NSH) {
                nsp = getNsp(rawPacketNSH);
                nsi = getNsi(rawPacketNSH);
                rawPacketNSH = popNsh(rawPacketVlan);
            }

            byte[] rawPacket = getPayLoad(rawPacketNSH);

            int eth2 = getEtherType(rawPacket);

            // classifier hop seem s to encap more than once
            while (eth2 == ETHERTYPE_NSH) {
                rawPacketNSH = popNsh(rawPacket);
                rawPacket = getPayLoad(rawPacketNSH);
                eth2 = getEtherType(rawPacket);
            }

            if (rawPacket == null) {
                return;
            }

            if (getEcn(rawPacket) != PROBE_PACKET_FULL_TRACE_ID && getEcn(rawPacket) != PROBE_PACKET_TIME_STAMP_ID) {
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

            TerminationPoint tp = topo.readTerminationPoint(nodeName, nodeConector.getValue());


            if (tp == null) {
                LOG.error("Not found Termination point [{}]", nodeName);
                return;
            }

            ServiceFunctionForwarder sff = topo.readSFF(nodeName);

            // get next output port
            String outSfDpl = null;
            BigInteger metadataPort = new BigInteger("0");
            if (packetReceived.getMatch().getMetadata() != null) {
                metadataPort = packetReceived.getMatch().getMetadata().getMetadata();
                if (metadataPort != null) {
                    outSfDpl = topo.readSfDplFromSff(sff, metadataPort.toString());
                } else {
                    LOG.error("could not read metadata");
                }
            }

            // get input from previous SF
            if (tp.getTpId() == null) {
                return;
            }
            String parts[] = tp.getTpId().getValue().split(":");
            //String inSfDpl = topo.readSfDplFromSff(sff, parts[2]);

            ServiceFunction sfOut = null;
            if (packetReceived.getFlowCookie().getValue().equals(TRACE_FULL_COKIE)) {
                timeFromLastTrace = inTime;
                Set<TraceElement> traceOut;
                Long packetID = new Long(getPacktIdentification(rawPacket));
                //updateStoredChains(inTime);
                synchronized (traceMap) {
                    //traceOut = traceMap.putIfAbsent(packetID, new ConcurrentSkipListSet<TraceElement>(new TraceElement.TraceEmentComparator()));
                    traceOut = traceMap.get(packetID);
                    if (traceOut == null) {
                        traceMap.putIfAbsent(packetID, new ConcurrentSkipListSet<TraceElement>(new TraceElement.TraceEmentComparator()));
                        traceOut = traceMap.get(packetID);
                        String logTrace = String.format("add new element %d \n", traceMap.size());
                        traceLogWriter.append(logTrace);
                    }
                }

                if (outSfDpl == null) {
                    // if table zero pakcet it not going to an SF. Pakcet is going to other siwtch.
                    BigInteger metadataTalbeId = new BigInteger(TABLE_ZERO_IDENTIFICATION, COOKIE_BIGINT_HEX_RADIX);
                    if (!metadataPort.equals(metadataTalbeId)) {
                        sfOut = topo.readSfNameByRsp(nsp, nsi);
                    }
                    metadataPort = new BigInteger("0");
                } else {
                    sfOut = topo.readSfName(outSfDpl);
                }
                TraceElement traceElement = TraceElement.setTraceNode(sff, sfOut, Integer.parseInt(parts[2]), metadataPort.intValue(), getIpTtl(rawPacket));

                if (traceElement == null) {
                    LOG.error("could no find SF in the dpl {}", outSfDpl);
                }

                traceElement.setInTime(inTime);
                traceOut.add(traceElement);

                String logTrace = String.format("size M %d, T %d -> [%d] -> %s [%d] \n",
                        traceMap.size(), traceOut.size(), packetID, traceElement.getTraceHop(), traceElement.getInTime());
                traceLogWriter.append(logTrace);



                //traceWriter.append(traceElement.getTraceHop());

                //Collections.sort(traceOut);


                //packetOutSender.sendPacketToPort(nodeName, packetReceived.getMatch().getMetadata().getMetadata().toString(), packetReceived.getPayload());
                if (timetoUpdate == 0) {
                    timetoUpdate = inTime;
                } else if ( inTime  >  timetoUpdate + (5 * 1000)) {
                    timetoUpdate = inTime;
                    updateStoredChains(inTime);
                }

            }
        }
    }
}
