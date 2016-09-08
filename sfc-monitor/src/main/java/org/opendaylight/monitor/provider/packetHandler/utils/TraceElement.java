/*
 * Copyright (c) 2016 Rafael Eichelberger, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.monitor.provider.packetHandler.utils;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;

import java.util.ArrayList;

/**
 * Created by rafael on 7/18/16.
 */
public class TraceElement implements Comparable<TraceElement> {
    private String sffName = null;
    private String sfName = null;
    private int ingressPort = 0;
    private int egressPort = 0;
    private ArrayList<Long> timestamp = new ArrayList<>();
    private Long timestampDic = null;
    private String traceHop;
    private long pktConnt = 0;
    private int ttl = 0;

    TraceElement (String sffName, String sfName, int in, int out, int ttl) {
        this.sffName = sffName;
        this.sfName = sfName;
        this.ingressPort = in;
        this.egressPort = out;
        this.ttl = ttl;

        buildTraceOut();
    }
    TraceElement() {}

    static public TraceElement setTraceNode(ServiceFunctionForwarder sffNode, ServiceFunction sfNone, int in, int out, int ttl) {
        String sfName = null;
        if (sffNode.getName().getValue() != null) {
            if (sfNone != null && sfNone.getName() != null) {
                SfName sfNames = sfNone.getName();
                sfName = sfNames.getValue();
            }
            return new TraceElement(sffNode.getName().getValue(), sfName, in, out, ttl);
        }
        return null;
    }

    private void buildTraceOut() {
       String printTraceOut = String.format("[%d - %s - %s] -", ingressPort, sffName, egressPort, sfName);
        if (sfName != null) {
            traceHop = String.format("%s[%s] - ", printTraceOut, sfName);
        } else {
            traceHop = printTraceOut;
        }
    }

    public void setTime(long timestamp) {
        this.timestamp.add(timestamp);
    }

    public void setDirectTime(long timestamp) {

           this.timestampDic = timestamp;

    }

    public long getDirectTime() {
        return this.timestampDic;
    }


    public void incremmentPktCount(String node) {
        if (this.sffName.equals(node)) {
            this.pktConnt++;
        }
    }

    public long getPktCount() {
        return timestamp.size();
    }

    public long getLastTime() {
        if (timestamp.size() == 0) {
            return 0;
        } else {
            return timestamp.get(timestamp.size() - 1);
        }
    }

    public long getTimestamp(int pos) {
        if (timestamp.get(pos) < timestamp.size()) {
            return timestamp.get(pos);
        } else {
            return 7;
        }
    }

    @Override
    public int compareTo(TraceElement traceElement) {
        TraceElement p1 =  traceElement;
        TraceElement p2 =  this;
        int ret = -1;

        if (p1.ttl == p2.ttl) {
            ret = 0;
        } else if (p1.ttl > p2.ttl) {
            ret = 1;
        } else if (p1.ttl < p2.ttl) {
            ret = -1;
        }
        return ret;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TraceElement)) {
            return false;
        }
        TraceElement other = (TraceElement) obj;
        if (this.sffName.equals(other.sffName)) {
            if (this.sfName == null && (other.sfName) == null) {
                return true;
            } else if (this.sfName != null) {
                if (this.sfName.equals(other.sfName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getSffName() {
        return sffName;
    }

    public long timesSize() {
        return timestamp.size();
    }


    public void setTraceHop(String trace) {
        this.traceHop = trace;
    }

    public String getTraceHop(){
        return traceHop;
    }
}
