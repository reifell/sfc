/*
 * Copyright (c) 2016 Rafael Eichelberger, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.monitor.provider.packetHandler.utils;

import java.util.ArrayList;

/**
 * Created by rafael on 7/18/16.
 */
public class TraceElement implements Comparable<TraceElement> {
    private String nodeName = null;
    private ArrayList<Long> timestamp = new ArrayList<>();
    private String traceHop;
    private long pktConnt = 0;
    private int ttl = 0;

    TraceElement (String nodeName, int ttl) {
        this.nodeName = nodeName;
        this.traceHop = null;
        this.ttl = ttl;
    }
    TraceElement() {}

    static public TraceElement setTraceNode(String node, int ttl) {
        if (node != null) {
            return new TraceElement(node, ttl);
        } else {
            return null;
        }
    }

    public boolean setTime(String node, int nMeasurment, long timestamp) {

        boolean wasSet = false;

        if (this.nodeName.equals(node)) {
            if (this.timestamp.size() == nMeasurment) {
                this.timestamp.add(timestamp);
                wasSet = true;
            }
        }
        return wasSet;
    }


    public void incremmentPktCount(String node) {
        if (this.nodeName.equals(node)) {
            this.pktConnt++;
        }
    }

    public long getPktCount() {
    return pktConnt;
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

    public String getNodeName() {
        return nodeName;
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
