/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.monitor.provider.provider;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.monitor.provider.packetHandler.PacketInListener;
import org.opendaylight.yangtools.concepts.Registration;

import java.util.concurrent.ExecutionException;

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

    public MonitorProvider(DataBroker dataBroker, NotificationProviderService notificationService) {
        LOG.info("MonitorProvider starting the MonitorProvider plugin...");

        this.notificationService = notificationService;
        this.flowListner = new FlowListner(dataBroker);
        this.ForwarderFlowListner = new ForwarderFlowListner(dataBroker);
        this.packetInListener =  new PacketInListener(notificationService);

        //this.pktInRegistration = notificationService.registerNotificationListener(null);

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

        } catch(Exception e) {
            LOG.error("SfcL2Renderer auto-closed exception {}", e.getMessage());
        }
    }
}
