/*
 * Copyright (c) 2016 Rafael Eichelberger, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.monitor.provider.provider;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FlowListner implements DataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(MonitorProvider.class);


    private DataBroker dataBroker;
    private ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;

    FlowListner(DataBroker broker) {
        dataBroker = broker;
        InstanceIdentifier<?> IID = OpendaylightSfc.RSP_ENTRY_IID;
        registerAsDataChangeListener(LogicalDatastoreType.OPERATIONAL, IID);
    }
    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> data) {
        LOG.info("---- MONITOR---------------");
        Map<InstanceIdentifier<?>, DataObject> dataCreatedConfigurationObject = data.getCreatedData();


    }

    public void registerAsDataChangeListener(LogicalDatastoreType datastoreType, InstanceIdentifier<?> iID) {
        dataChangeListenerRegistration = this.dataBroker.registerDataChangeListener(datastoreType, iID,
                this, DataBroker.DataChangeScope.SUBTREE);
    }
}
