/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.monitor.provider.packetHandler;

import org.opendaylight.monitor.provider.packetHandler.utils.TopologyHandler;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * Created by rafael on 7/6/16.
 */
public class PacketOutSender {

    private static final Logger LOG = LoggerFactory.getLogger(PacketOutSender.class);

    private static final int SCHEDULED_THREAD_POOL_SIZE = 1;
    private static final int QUEUE_SIZE = 1000;
    private static final int ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS = 300;
    private static final long SHUTDOWN_TIME = 5;
    ThreadPoolExecutor threadPoolExecutorService = null;

    PacketProcessingService rpc;
    boolean close = false;

    TopologyHandler topo = new TopologyHandler();

    public PacketOutSender(RpcProviderRegistry rpcProvider) {
        rpc = rpcProvider.getRpcService(PacketProcessingService.class);

        this.threadPoolExecutorService = new ThreadPoolExecutor(SCHEDULED_THREAD_POOL_SIZE, SCHEDULED_THREAD_POOL_SIZE,
                ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(QUEUE_SIZE));


    }

    public void init () {
        sender sender_ = new sender(10);
        threadPoolExecutorService.execute(sender_);
    }

    public void test() {
        String sendSw = null;

        ServiceFunctionForwarders sffs = topo.readAllSFFs();
        if (sffs != null) {
            for (ServiceFunctionForwarder sff : sffs.getServiceFunctionForwarder()) {
                String sw = sff.getServiceNode().getValue();
                // if (sw.equals("openflow:2")) {
                sendSw = sw;
                //  }

                if (sendSw != null) {
                    NodeBuilder nodeBuilder = new NodeBuilder();
                    nodeBuilder.setId(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(sendSw));
                    nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

                    InstanceIdentifier<Node> NodeII = InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class, nodeBuilder.getKey()).build();

                    NodeConnectorBuilder nodeConBuilder = new NodeConnectorBuilder();
                    nodeConBuilder.setId(new NodeConnectorId("openflow:2:3"));
                    nodeConBuilder.setKey(new NodeConnectorKey(nodeConBuilder.getId()));

                    InstanceIdentifier<NodeConnector> NodeConII = InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class, nodeBuilder.getKey()).child(NodeConnector.class, nodeConBuilder.getKey()).build();

                    // Node sffNode = SfcDataStoreAPI.readTransactionAPI(NodeII, LogicalDatastoreType.OPERATIONAL);
                    // if (sffNode != null) {
                    //     LOG.info("Sff node {} ", sffNode.getKey().getId().getValue());

                    // arp request arp_tpa=100.0.0.101
                    byte[] data = new byte[]{-1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 1, 8, 6, 0, 1, 8, 0, 6, 4, 0, 1, 0,
                            0, 0, 0, 0, 1, 10, 0, 0, 1, -1, -1, -1, -1, -1, -1, 100, 0, 0, 101};

                    LOG.info("pakcet sent {} time {}", sendSw, System.currentTimeMillis());

                    sendPacket(NodeII, NodeConII, data);
                    // }
                }
            }
        } else {
            LOG.info("no SFF detected ");
        }


    }

    /**
     * This method sends the packet on given output port
     * @param nodeIID SwithID
     * @param ncIID Node connector (ouput port)
     * @param data packet to transmit
     */
    public void sendPacket(InstanceIdentifier<Node> nodeIID,
                            InstanceIdentifier<NodeConnector> ncIID,
                            byte[] data) {

        TransmitPacketInputBuilder txBuilder = new TransmitPacketInputBuilder();

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        OutputActionBuilder output = new OutputActionBuilder();
        output.setMaxLength(Integer.valueOf(0xffff));
        Uri value = new Uri(OutputPortValues.TABLE.toString());
        output.setOutputNodeConnector(value);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());


        txBuilder.setPayload(data)
                .setNode(new NodeRef(nodeIID))
                .setAction(actionList)
                .setEgress(new NodeConnectorRef(ncIID));

        rpc.transmitPacket(txBuilder.build());

    }

    public void close() throws ExecutionException, InterruptedException {
        close = true;
        threadPoolExecutorService.shutdown();
        if (!threadPoolExecutorService.awaitTermination(SHUTDOWN_TIME, TimeUnit.SECONDS)) {
            LOG.error("PacketInListener Executor did not terminate in the specified time.");
            List<Runnable> droppedTasks = threadPoolExecutorService.shutdownNow();
            LOG.error("PacketInListener Executor was abruptly shut down. [{}] tasks will not be executed.",
                    droppedTasks.size());
        }
    }

    class sender implements Runnable {

        long inTime;

        public sender(long inTime) {
            this.inTime = inTime;
        }


        public void run() {
            while (!close) {
                LOG.info("send calibration packet");
                test();
                try {
                    TimeUnit.SECONDS.sleep(inTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
