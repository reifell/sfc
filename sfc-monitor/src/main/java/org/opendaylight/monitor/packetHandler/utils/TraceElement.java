/*
 * Copyright (c) 2016 Rafael Eichelberger, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.monitor.packetHandler.utils;

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
    private String traceHop;
    private ArrayList<TimeAndHop> timeAndHops = new ArrayList<>();
    private int ttl = 0;
    private TreeMap<Long, HopAndPktCounter> plotDelay = new TreeMap<>();
    private Long lastPlot = new Long(0);
    private long inTime;
    private double stdDev;

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

    public class HopAndPktCounter {
        HopAndPktCounter(float hopDelay, int pktCounter) {
            this.hopDelayBySec = hopDelay;
            this.pktCounter = pktCounter;
        }
        public float hopDelayBySec;
        public int pktCounter;
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

    public void setTimeAndHopDelay(long timestamp, int delay) {
        timeAndHops.add(new TimeAndHop(timestamp, delay));
    }

    public String getPlot() {
        if (plotDelay == null) {
            return null;
        }

        StringBuilder plot = new StringBuilder();
        float sum = 0;
        long ppCount = 0;
        Long initialTime;
        Long range;
        if (!plotDelay.isEmpty()) {
            initialTime = plotDelay.firstKey();
            Long finalTime = plotDelay.lastKey();
            range = finalTime - initialTime;
        } else {
            return null;
        }

        long graphStep = range/(long)100;
        Long firstCycleTime = (long)0;
        int countPos = 0;
        for (Map.Entry<Long, HopAndPktCounter> element : plotDelay.entrySet()) {
            if (ppCount == 0) firstCycleTime = element.getKey();
            ppCount += element.getValue().pktCounter;
            sum += element.getValue().hopDelayBySec;
            countPos++;
            if (element.getKey() >= firstCycleTime + graphStep) {
                float rangeTimeInSec = (float)(element.getKey() - firstCycleTime)/(float)1000;
                float pktPerSecond = ppCount / rangeTimeInSec;
                float avg = sum / (float)countPos;
//                plot.append(String.format(" debug %d; %d; %.2f\n", ppCount, pktInCounter.subSet(firstCycleTime, element.getKey()).size(), rangeTimeInSec));

                float totalPacketIn = 0; // pktInCounter.subSet(firstCycleTime, element.getKey()).size()/rangeTimeInSec;
                //format:  timestamp  -  averge delay   -   pkts per senconds   -  total pktin per second
                plot.append(String.format("%.2f; %.2f; %.2f; %.2f\n", (float)(element.getKey() - initialTime)/(float)1000, avg, pktPerSecond, totalPacketIn));
                sum = 0;
                ppCount = 0;
                countPos = 0;
            }
            if (element.equals(plotDelay.lastEntry())) {
                plot.append(String.format("total count %d - %d\n",ppCount,  getPktCount()) );
                sum = 0;
                ppCount = 0;
                countPos = 0;
            }
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
            sum += (float) d.hopDelay;

            HopAndPktCounter hopAndPktCounter = plotDelay.get(d.timestamp);
            //as map does not support duplicate keys, compute the average beetween both keys and store in the same timestamp
            if (hopAndPktCounter == null) {
                hopAndPktCounter = new HopAndPktCounter(d.hopDelay, 1);
            } else {
                hopAndPktCounter.hopDelayBySec = (plotDelay.get(d.timestamp).hopDelayBySec + (float) d.hopDelay) / (float)2;
                hopAndPktCounter.pktCounter++;
            }

            plotDelay.put(d.timestamp, hopAndPktCounter);
        }
        float avg;
        if (timeAndHops.size() == 0) avg = sum;
        avg = sum / (float)timeAndHops.size();
        return avg;
    }

    public double getStdDev(float avg) {

        ArrayList<Double> stdDeviation = new ArrayList<>();
        for (TimeAndHop d : timeAndHops) {
            stdDeviation.add(Math.pow((d.hopDelay-avg),2));
        }
        long sum = 0;
        for (Double d : stdDeviation) {
            sum += d;
        }
        float avgSum = sum / (float)timeAndHops.size();
        stdDev = Math.sqrt(avgSum);

        return stdDev;
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

    public static boolean isSameChain(Set<TraceElement> p1, Set<TraceElement> p2) {
        if (p1 == null || p2 == null) return false;
        if (p1.size() != p2.size()) {
            return false;
        }
        Iterator<TraceElement> it1 = p1.iterator();
        Iterator<TraceElement> it2 = p2.iterator();
        while (it1.hasNext()) {
            TraceElement element1 = it1.next();
            TraceElement element2 = it2.next();

            if (element1.sffName.equals(element2.sffName)) {
                if (element1.sfName == null && (element2.sfName) == null) {
                    continue;
                } else if (element1.sfName != null) {
                    if (element1.sfName.equals(element2.sfName)) {
                        continue;
                    }
                }
                return false;
            }
            return false;
        }
        return true;
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
