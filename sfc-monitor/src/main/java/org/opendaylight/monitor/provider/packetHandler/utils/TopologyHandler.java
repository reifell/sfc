/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.monitor.provider.packetHandler.utils;

import com.google.common.base.Preconditions;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.concurrent.ExecutionException;

/**
 * Created by rafael on 5/19/16.
 */
public class TopologyHandler {
    private InstanceIdentifier.InstanceIdentifierBuilder<Nodes> nodesInsIdBuilder = InstanceIdentifier.<Nodes>builder(Nodes.class);




    private final InstanceIdentifier<NetworkTopology> networkII

            = InstanceIdentifier.builder(NetworkTopology.class).build();

    public InstanceIdentifier<Topology> getTopologyII() throws ExecutionException, InterruptedException {

        TopologyId tid = new TopologyId("flow:1");
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(tid));
        InstanceIdentifier<Topology> topologyII = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key);
        return topologyII;
    }




    public TerminationPoint readTerminationPoint(String bridgeName, String portName) {
        NodeId nodeId = new NodeId(bridgeName);
        InstanceIdentifier<TerminationPoint> tpIid =
                InstanceIdentifier.create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(new TopologyId(new Uri("flow:1"))))
                        .child(Node.class, new NodeKey(new NodeId(nodeId)))
                        .child(TerminationPoint.class, new TerminationPointKey(new TpId(String.valueOf(portName))));

        return SfcDataStoreAPI.readTransactionAPI(tpIid, LogicalDatastoreType.OPERATIONAL);
    }



    public Node readNode(String bridgeName) {
        NodeId nodeId = new NodeId(bridgeName);
        InstanceIdentifier<Node> tpIid =
                InstanceIdentifier.create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(new TopologyId(new Uri("flow:1"))))
                        .child(Node.class, new NodeKey(new NodeId(nodeId)));

        return SfcDataStoreAPI.readTransactionAPI(tpIid, LogicalDatastoreType.OPERATIONAL);
    }

    public ServiceFunctionForwarder readSFF(String bridgeName) {

        InstanceIdentifier<ServiceFunctionForwarders> sffIID =
                InstanceIdentifier.create(ServiceFunctionForwarders.class);
//
//        InstanceIdentifier<ServiceFunctionForwarders> sffIID =
//                InstanceIdentifier.create(ServiceFunctionForwarders.class).child(ServiceNode.class, new ServiceNodeKey(new SnName("" )));

        ServiceFunctionForwarders sffs = SfcDataStoreAPI.readTransactionAPI(sffIID, LogicalDatastoreType.CONFIGURATION);
        for (ServiceFunctionForwarder sff : sffs.getServiceFunctionForwarder()) {
            if (bridgeName.equals(sff.getServiceNode().getValue())) {
                return sff;
            }
        }
        return null;

    }


    public String readSfDplFromSff(ServiceFunctionForwarder sff, String port) {
        String dplName = null;
        for(SffDataPlaneLocator dpl : sff.getSffDataPlaneLocator()) {
            SffDataPlaneLocator1 dplPort = dpl.getAugmentation(SffDataPlaneLocator1.class);
            if (dplPort != null) {
                if (dplPort.getOfsPort().getPortId().equals(port)) {
                    dplName = dpl.getName().getValue();
                    break;
                }
            }
        }
        String sfDpl = null;
        if (dplName.equals("egress")) {
            return dplName;
        }
        if (dplName != null) {
            for ( ServiceFunctionDictionary sf : sff.getServiceFunctionDictionary()) {
                if (sf.getSffSfDataPlaneLocator().getSffDplName().getValue().equals(dplName)) {
                    sfDpl = sf.getSffSfDataPlaneLocator().getSfDplName().getValue();
                }
            }
        }
        return sfDpl;
    }

    public ServiceFunction readSfName(String dpl) {
        InstanceIdentifier<ServiceFunctions> sfIID =
                InstanceIdentifier.create(ServiceFunctions.class);
        ServiceFunctions sfs = SfcDataStoreAPI.readTransactionAPI(sfIID, LogicalDatastoreType.CONFIGURATION);

        for (ServiceFunction sf : sfs.getServiceFunction()) {
            for ( SfDataPlaneLocator sfDpl : sf.getSfDataPlaneLocator()) {
                if (sfDpl.getName().getValue().equals(dpl)) {
                    return sf;
                }
            }
        }
        return null;
    }

}
