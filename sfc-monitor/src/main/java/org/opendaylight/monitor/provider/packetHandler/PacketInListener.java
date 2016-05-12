/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.monitor.provider.packetHandler;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ethernet Packet Decoder
 */
public class PacketInListener implements PacketProcessingListener {

    private static final Logger LOG = LoggerFactory.getLogger(PacketInListener.class);

    public static final Integer LENGTH_MAX = 1500;
    public static final Integer ETHERTYPE_MIN = 1536;
    public static final Integer ETHERTYPE_8021Q = 0x8100;
    public static final Integer ETHERTYPE_QINQ = 0x9100;

    public PacketInListener(NotificationProviderService notificationProviderService) {
        notificationProviderService.registerNotificationListener(this);    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        String tableID = "bla";
        if(packetReceived.getTableId() != null) {
            tableID = packetReceived.getTableId().toString();
        }
        String getEthernetMatch = "bla";
        if(packetReceived.getMatch().getEthernetMatch() != null) {
            getEthernetMatch = packetReceived.getMatch().getEthernetMatch().toString();
        }
        LOG.info("Pakcet in match {} and--- packet Reson {} TableID {} ethernetMatch {}",
                packetReceived.getMatch().toString(), packetReceived.getPacketInReason().toString(),tableID , getEthernetMatch );

        String getIpMatch = "bla";
        if(packetReceived.getMatch().getIpMatch() != null) {
            getIpMatch = packetReceived.getMatch().getIpMatch().toString();
        }
        String getLayer3Match = "bla";
        if(packetReceived.getMatch(). getLayer3Match() != null) {
            getLayer3Match = packetReceived.getMatch().getLayer3Match().toString();
        }
        LOG.info("Pakcet in match {} layer 3 {}", getIpMatch, getLayer3Match);

    }



}
