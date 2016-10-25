/*
 * Copyright (c) 2016 Rafael Eichelberger, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.monitor.provider.provider;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.monitor.provider.packetHandler.PacketInListener;
import org.opendaylight.monitor.provider.packetHandler.PacketOutSender;
import org.opendaylight.yangtools.concepts.Registration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
// This class is instantiated from:
//      org.opendaylight.controller.config.yang.config.sfcofl2_provider.impl.SfcOFL2ProviderModule.createInstance()
// It is a general entry point for the sfcofl2 feature/plugin
//

public class MonitorProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MonitorProvider.class);

//    private SfcL2FlowProgrammerInterface sfcL2FlowProgrammer;
    private Registration pktInRegistration;
    FlowListner flowListner = null;
    ForwarderFlowListner ForwarderFlowListner = null;
    NotificationProviderService notificationService = null;
    PacketInListener packetInListener = null;
    PacketOutSender packetOutSender = null;
    private static MonitorProvider monitorProviderObj = null;

    protected static DataBroker dataProvider;
    protected static BindingAwareBroker broker;

    private static final long SHUTDOWN_TIME = 5;
    private static final ThreadFactory THREAD_FACTORY =
            new ThreadFactoryBuilder().setNameFormat("SFC-Monitor-%d").setDaemon(false).build();

    public static final int EXECUTOR_THREAD_POOL_SIZE = 100;

    private static final ExecutorService executor =
            Executors.newFixedThreadPool(EXECUTOR_THREAD_POOL_SIZE, THREAD_FACTORY);

    public static MonitorProvider getMonitorProviderObj() {
        return MonitorProvider.monitorProviderObj;
    }

    public MonitorProvider() {
        if (monitorProviderObj == null) {
            monitorProviderObj = this;
            LOG.info("MonitorProviderObj Initialized");
        }
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setDataProvider(DataBroker dataProvider) {
        MonitorProvider.dataProvider = dataProvider;
    }

    public DataBroker getDataProvider() {
        return MonitorProvider.dataProvider;
    }

    public void setBroker(BindingAwareBroker broker) {
        this.broker = broker;
    }

    public BindingAwareBroker getBroker() {
        return this.broker;
    }

    public static MonitorProvider getOpendaylightSfcObj() {
        return MonitorProvider.monitorProviderObj;
    }



    public MonitorProvider(DataBroker dataBroker, NotificationProviderService notificationService, RpcProviderRegistry rpcProvider) {
        LOG.info("MonitorProvider starting the MonitorProvider plugin...");

        this.notificationService = notificationService;
        this.flowListner = new FlowListner(dataBroker);
        this.ForwarderFlowListner = new ForwarderFlowListner(dataBroker);
        this.packetOutSender = new PacketOutSender(rpcProvider);
        this.packetInListener =  new PacketInListener(notificationService, packetOutSender, rpcProvider);

        //this.packetOutSender.init();

        LOG.info("MonitorProvider successfully started the MonitorProvider plugin");
    }


    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        LOG.info("MonitorProvider auto-closed");
        try {
            if(ForwarderFlowListner != null) {
                ForwarderFlowListner.close();
            }
            if (packetInListener != null) {
                packetInListener.close();
            }
            if (packetOutSender != null) {
                packetOutSender.close();
            }

        } catch(Exception e) {
            LOG.error("SfcL2Renderer auto-closed exception {}", e.getMessage());
        }
    }
}
