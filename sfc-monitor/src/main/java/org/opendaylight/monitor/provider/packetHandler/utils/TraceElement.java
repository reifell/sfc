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

import java.util.*;

/**
 * Created by rafael on 7/18/16.
 */
public class TraceElement  {
    private String sffName = null;
    private String sfName = null;
    private int ingressPort = 0;
    private int egressPort = 0;
    private ArrayList<Long> timestamp = new ArrayList<>();
    private String traceHop;
    private ArrayList<Integer> hopDelay = new ArrayList<>();
    private ArrayList<TimeAndHop> timeAndHops = new ArrayList<>();
    private int ttl = 0;
    private TreeMap<Long, Integer> plotDelay = new TreeMap<>();
    private Long lastPlot = new Long(0);
    private long inTime;

    TraceElement (String sffName, String sfName, int in, int out, int ttl) {
        this.sffName = sffName;
        this.sfName = sfName;
        this.ingressPort = in;
        this.egressPort = out;
        this.ttl = ttl;

        buildTraceOut();
    }

    public class TimeAndHop {
        TimeAndHop(long time, int hop) {
            this.timestamp = time;
            this.hopDelay = hop;
        }
        public long timestamp;
        public int hopDelay;
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
       String printTraceOut = String.format("[%d - %s - %s] -", ingressPort, sffName, egressPort);
        if (sfName != null) {
            traceHop = String.format("%s[%s] - ", printTraceOut, sfName);
        } else {
            traceHop = printTraceOut;
        }
    }
    //   [
    // [ [4,1,"SFF1","SF1",19], [3,8,"SFF1","SF1",19], [] ],
    //         [ [4,1,"SFF1","SF1",19], [3,8,"SFF1","SF1",19], [] ],
    //         ]
    public String getTraceGraph() {
        String sfName = "#None";
        if (this.sfName != null) {
            sfName = this.sfName;
        }
        return String.format("[%d, %d, \"%s\", \"%s\", %d]", ingressPort, egressPort, sffName, sfName, getPktCount());
    }

    public void setInTime(long timestamp) {
        this.inTime = timestamp;
    }

    public long getInTime() {
        return this.inTime;
    }
//
//    public void setHopDelay(Integer delay) {
//        this.hopDelay.add(delay);
//    }

    public void setTimeAndHopDelay(long timestamp, int delay) {
        timeAndHops.add(new TimeAndHop(timestamp, delay));
    }

    public String getPlot() {
        StringBuilder plot = new StringBuilder();
        float sum = 0;
        int i = 0;
        Long initialTime = new Long(0);
        if (!plotDelay.isEmpty()) {
            initialTime = plotDelay.firstKey();
        }
        int nStepInTheGraph = plotDelay.size()/500;
        if (nStepInTheGraph < 1) nStepInTheGraph = 1;
        for (Map.Entry<Long, Integer> element : plotDelay.entrySet()) {
            sum += element.getValue();
            if (i == nStepInTheGraph) {
                float avg = sum / (float)nStepInTheGraph;
                plot.append(String.format("%.2f; %.2f\n", (float)(element.getKey() - initialTime)/(float)1000, avg));
                sum = 0;
                i = 0;
            }
            i++;
        }
        if (!plotDelay.isEmpty()) {
            lastPlot = plotDelay.lastKey();
            plotDelay.clear();
            return plot.toString();
        } else {
            return null;
        }
    }

    public ArrayList<TimeAndHop> getTimeAndHop() {
        return timeAndHops;
    }

    public float getHopDelayAverage() {
        float sum = 0;
        for (TimeAndHop d : timeAndHops) {
            sum += (float)d.hopDelay;
            if (d.timestamp > lastPlot) {
                plotDelay.put(d.timestamp, d.hopDelay);
            }
        }
        if (timeAndHops.size() == 0) return sum;
        return sum / (float)timeAndHops.size();
    }
    public long getPktCount() {
        return timeAndHops.size();
    }

    public long getLastTime() {
        if (timeAndHops.size() == 0) {
            return 0;
        } else {
            return timeAndHops.get(timeAndHops.size() - 1).timestamp;
        }
    }


    public static class TraceEmentComparator implements Comparator<TraceElement> {
        @Override
        public int compare(TraceElement p1, TraceElement p2) {
//            TraceElement p1 = traceElement;
//            TraceElement p2 = this;
            int ret = -1;

            if (p1.ttl == p2.ttl) {
                ret = 0;
            } else if (p1.ttl > p2.ttl) {
                ret = -1;
            } else if (p1.ttl < p2.ttl) {
                ret = 1;
            }
            return ret;
        }
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

    @Override
    public int hashCode() {
        int result = sffName.hashCode();
        result = 31 * result + sfName.hashCode();
        return result;
    }
    public String getSffName() {
        return sffName;
    }

    public void setTraceHop(String trace) {
        this.traceHop = trace;
    }

    public String getTraceHop(){
        return traceHop;
    }
}
