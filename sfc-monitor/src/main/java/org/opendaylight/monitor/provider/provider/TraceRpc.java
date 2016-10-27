/*
 * Copyright (c) 2016 Rafael Eichelberger, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.monitor.provider.provider;

import org.opendaylight.monitor.provider.packetHandler.PacketInListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sfc.monitor.impl.rev160506.SfcMonitorImplService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sfc.monitor.impl.rev160506.TraceSfcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sfc.monitor.impl.rev160506.TraceSfcOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Future;

/**
 * Created by rafael on 10/1/16.
 */
public class TraceRpc implements SfcMonitorImplService {

    private PacketInListener packetInListener = null;

    private static final Logger LOG = LoggerFactory.getLogger(TraceRpc.class);

    @Override
    public Future<RpcResult<TraceSfcOutput>> traceSfc() {
        LOG.info("Get Trace Information");
        if (packetInListener == null) {
            packetInListener = PacketInListener.getPacketInListenerObj();
        }
        try {

            String out = packetInListener.getTrace();

            return RpcResultBuilder
                    .success(new TraceSfcOutputBuilder().setResult(out).build())
                    .buildFuture();
        } catch (Exception e) {
            return RpcResultBuilder
                    .<TraceSfcOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, e.getMessage(), e)
                    .buildFuture();
        }

        //return Futures.immediateFuture(
        //        RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, "No data provider.").build());
    }
    public void putPacketInListerObject (PacketInListener listner) {
        packetInListener = listner;
    }


    private void trigger() {
        LOG.info("cleanTraceSfc");
    }

    @Override
    public Future<RpcResult<Void>> cleanTraceSfc() {
        LOG.info("Clear Trace Information");
        packetInListener.cleanTraceMaps();

        try {
            trigger();
            return RpcResultBuilder
                    .<Void>success()
                    .buildFuture();
        } catch (Exception e) {
            return RpcResultBuilder
                    .<Void>failed()
                    .withError(RpcError.ErrorType.APPLICATION, e.getMessage(), e)
                    .buildFuture();
        }

        //return Futures.immediateFuture(
        //        RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, "No data provider.").build());
    }

}
