/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.monitor.provider.provider;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by rafael on 5/6/16.
 */
public class WriteFlow {
    private static final int SCHEDULED_THREAD_POOL_SIZE = 1;
    private static final int QUEUE_SIZE = 1000;
    private static final int ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS = 300;
    private static final long SHUTDOWN_TIME = 5;

    ThreadPoolExecutor threadPoolExecutorService = null;
    private static final Logger LOG = LoggerFactory.getLogger(WriteFlow.class);

    public WriteFlow() {

        this.threadPoolExecutorService = new ThreadPoolExecutor(SCHEDULED_THREAD_POOL_SIZE, SCHEDULED_THREAD_POOL_SIZE,
                                                            ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS, TimeUnit.SECONDS,
                                                            new LinkedBlockingQueue<Runnable>(QUEUE_SIZE));
    }

    public void shutdown() throws ExecutionException, InterruptedException {
        // When we close this service we need to shutdown our executor!
        threadPoolExecutorService.shutdown();
        if (!threadPoolExecutorService.awaitTermination(SHUTDOWN_TIME, TimeUnit.SECONDS)) {
            LOG.error("WriteFlow Executor did not terminate in the specified time.");
            List<Runnable> droppedTasks = threadPoolExecutorService.shutdownNow();
            LOG.error("WriteFlow Executor was abruptly shut down. [{}] tasks will not be executed.",
                    droppedTasks.size());
        }
    }
    public void writeFlowToConfig(String sffNodeName, FlowBuilder flow) {

        // Create the NodeBuilder
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // Create the flow path, which will include the Node, Table, and Flow
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId()))
                .child(Flow.class, flow.getKey())
                .build();

        LOG.debug("writeFlowToConfig writing flow to Node {}, table {}", sffNodeName, flow.getTableId());


        FlowWriterTask writerThread = new FlowWriterTask(sffNodeName, flowInstanceId, flow);
        try {
            threadPoolExecutorService.execute(writerThread);
        } catch (Exception ex) {
            LOG.error("error trying to configure SFC monitor rule", ex.toString());
        }
    }

    /**
     * A thread class used to write the flows to the data store.
     */
    class FlowWriterTask implements Runnable {
        String sffNodeName;
        InstanceIdentifier<Flow> flowInstanceId;
        FlowBuilder flowBuilder;

        public FlowWriterTask(String sffNodeName, InstanceIdentifier<Flow> flowInstanceId, FlowBuilder flowBuilder) {
            this.sffNodeName = sffNodeName;
            this.flowInstanceId = flowInstanceId;
            this.flowBuilder = flowBuilder;
        }

        public void run(){
            if (!SfcDataStoreAPI.writeMergeTransactionAPI(
                    this.flowInstanceId,
                    this.flowBuilder.build(),
                    LogicalDatastoreType.CONFIGURATION)) {
                LOG.error("{}: Failed to create Flow on node: {}", Thread.currentThread().getStackTrace()[1], this.sffNodeName);
            }
        }
    }
}
