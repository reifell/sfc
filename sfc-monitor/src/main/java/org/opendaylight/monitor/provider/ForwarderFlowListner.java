/*
 * Copyright (c) 2016 Rafael Eichelberger, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.monitor.provider;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.monitor.packetHandler.PacketInListener;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionPopNshNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;


import javax.annotation.Nonnull;


/**
 * GroupForwarder
 * It implements {@link org.opendaylight.controller.md.sal.binding.api.DataChangeListener}}
 * for WildCardedPath to {@link Flow} and ForwardingRulesCommiter interface for methods:
 *  add, update and remove {@link Flow} processing for
 *  {@link org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class ForwarderFlowListner implements ClusteredDataTreeChangeListener<Flow> {

    private static final Logger LOG = LoggerFactory.getLogger(ForwarderFlowListner.class);

    private final DataBroker dataBroker;
    private ListenerRegistration<ForwarderFlowListner> listenerRegistration;
    private WriteFlow writeFlow = new WriteFlow();

    public static final short TABLE_INDEX_INGRESS = 0;
    public static final short TABLE_INDEX_TRANSPORT_EGRESS = 10;
    public static final int COOKIE_BIGINT_HEX_RADIX = 16;
    public static final BigInteger TRANSPORT_EGRESS_COOKIE =
            new BigInteger("BA5EBA11BA5EBA11ba5eba11", COOKIE_BIGINT_HEX_RADIX);
    public static final BigInteger SFC_MONIOR_COOKIE = new BigInteger("1FFFF1", COOKIE_BIGINT_HEX_RADIX);

    public static final BigInteger METADATA_MASK_SFP_MATCH =
            new BigInteger("FFFFFFFFFFFFFFFF", COOKIE_BIGINT_HEX_RADIX);


    public Map<String, List<FlowBuilder>> flowMap = new HashMap<>();

    public ForwarderFlowListner(final DataBroker db) {
        dataBroker = Preconditions.checkNotNull(db, "DataBroker can not be null!");
        registrationListener(db);
    }

    public void close() throws ExecutionException, InterruptedException {
        if (writeFlow != null) {
            writeFlow.shutdown();
        }
    }

    private void registrationListener(final DataBroker db) {

        final DataTreeIdentifier<Flow> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getWildCardPath());
        listenerRegistration = db.registerDataTreeChangeListener(treeId, ForwarderFlowListner.this);

    }


    private InstanceIdentifier<Flow> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class)
                .augmentation(FlowCapableNode.class).child(Table.class).child(Flow.class);
    }


    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Flow>> collection) {

        for (DataTreeModification<Flow> change : collection) {
            final InstanceIdentifier<Flow> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Flow> mod = change.getRootNode();

            Flow flow = mod.getDataAfter();
            final String nodeName = key.firstKeyOf(Node.class, NodeKey.class).getId().getValue();

            long cookieLong = 0;
            if (flow.getCookie() != null) {
                cookieLong = flow.getCookie().getValue().longValue();
            }

            if (flow.getTableId() == TABLE_INDEX_TRANSPORT_EGRESS && Long.toHexString(cookieLong).toUpperCase().startsWith(SfcOfFlowProgrammerImpl.TRANSPORT_EGRESS_COOKIE_STR_BASE)) {

                //action to only send packets to next table
                LOG.info("SEND TRACE FLOW TO - ----- {} ", nodeName);
                FlowBuilder modifiedFlow = new FlowBuilder(flow);
                modifiedFlow = addTraceRule(modifiedFlow, false);
                writeFlow.writeFlowToConfig(nodeName, modifiedFlow);

            } else if (flow.getTableId() == TABLE_INDEX_INGRESS) {
                if(flow.getId() != null & flow.getId().getValue().startsWith("classifier.rule.out")) {
                    LOG.info("SET TRACE TO FLOW in Classifier - ----- {} ", nodeName);
                    FlowBuilder modifiedFlow = new FlowBuilder(flow);
                    modifiedFlow = setFlowToBeTraced(modifiedFlow);
                    writeFlow.writeFlowToConfig(nodeName, modifiedFlow);

                } else if (cookieLong != PacketInListener.TRACE_FULL_COKIE.longValue()){
                    if (getOutputPort(flow.getInstructions().getInstruction()) != null) {
                        // just write flows for non POP NSH rules (bug on OVS??)
                        if (!IsPopNshRule(flow.getInstructions().getInstruction())) {
                            LOG.info("SEND TRACE FLOW TO Classifier - ----- {} ", nodeName);
                            FlowBuilder modifiedFlow = new FlowBuilder(flow);
                            modifiedFlow = addTraceRule(modifiedFlow, true);
                            writeFlow.writeFlowToConfig(nodeName, modifiedFlow);
                        }
                    }
                }
            }
        }
    }

    public void writeFlows(String nodeName, Flow flow) {

        //action to only send packets to next table
        LOG.info("SEND TIMESTAMP FLOW TO - ----- {} ", nodeName);
        FlowBuilder modifiedFlow = new FlowBuilder(flow);
        modifiedFlow = addTraceRule(modifiedFlow, false);
        writeFlow.writeFlowToConfig(nodeName, modifiedFlow);

        try {
            TimeUnit.MILLISECONDS.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // trace action to send packet to controller
        LOG.info("SEND TRACE FLOW TO - ----- {} ", nodeName);
        FlowBuilder newFlow = new FlowBuilder(flow);
        newFlow = traceAction(newFlow);
        writeFlow.writeFlowToConfig(nodeName, newFlow);

    }

    private FlowBuilder testFlow() {

        LOG.info("testFlow");
        MatchBuilder newMatch = new MatchBuilder();
        SfcOpenflowUtils.addMatchDstMac(newMatch, "00:00:00:00:00:02");

        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        Action actionSetNwDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst("10.10.10.10", order++);
        actionList.add(actionSetNwDst);
        actionList.add(SfcOpenflowUtils.createActionNxLoadTunGpeNp((short) 0x4, order++));

        actionList.add(SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), order++));

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        int ibOrder = 0;
        InstructionBuilder actionsIb = new InstructionBuilder();
        actionsIb.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        actionsIb.setKey(new InstructionKey(ibOrder));
        actionsIb.setOrder(ibOrder++);

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = SfcOpenflowUtils.createInstructionsBuilder(actionsIb);


        FlowBuilder newFlowBuilder = SfcOpenflowUtils.createFlowBuilder(
                (short) 0,
                50000,
                PacketInListener.TRACE_FULL_COKIE,
                "test_flow", newMatch, isb);

        return newFlowBuilder;
    }

    private FlowBuilder addTraceRule(FlowBuilder flowBuilder, Boolean tableZero) {

        Instructions instruction = flowBuilder.getInstructions();
        List<Instruction> instructionList = instruction.getInstruction();

        Match match = flowBuilder.getMatch();
        MatchBuilder newMatch = new MatchBuilder(match);
        SfcOpenflowUtils.addMatchEcn(newMatch, PacketInListener.PROBE_PACKET_FULL_TRACE_ID);

        int j = 0;
        List<Action> newActionList = new ArrayList<>();
        for (Instruction instruct : instructionList) {
            if (instruct.getInstruction() instanceof ApplyActionsCase) {
                ApplyActionsCase actionscase = (ApplyActionsCase) instruct.getInstruction();
                ApplyActions actions = actionscase.getApplyActions();
                newActionList.add(SfcOpenflowUtils.createActionDecTTL(0));
                for (Action action : actions.getAction()) {
                    ActionBuilder ab = createActionBuilder(action.getOrder() + 1);
                    ab.setAction(action.getAction());
                    newActionList.add(ab.build());
                }
                break;
            }
            j++;
        }

        instructionList.remove(j);
        // add actions
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(newActionList);
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setKey(new InstructionKey(j));
        ib.setOrder(j);
        instructionList.add(ib.build());


        // add write metadata with port information
        String outPort;
        if(tableZero) { // identify table zero rule by wirte metadata code
            outPort = PacketInListener.TABLE_ZERO_IDENTIFICATION;
        } else {
            outPort = getOutputPort(instructionList);
            if (String.valueOf("INPORT").equals(outPort)) {
                outPort = "0";
            }
        }
        BigInteger metadataPort = new BigInteger(outPort, COOKIE_BIGINT_HEX_RADIX);

        int ibOrder = instructionList.size();
        addMetadata(instructionList, metadataPort, ibOrder);
        ibOrder++;

        short nextTable = TABLE_INDEX_TRANSPORT_EGRESS + 1;
        instructionList.add(setActionGoToTable(nextTable, ibOrder));
        InstructionsBuilder newInstructions = new InstructionsBuilder();

        newInstructions.setInstruction(instructionList);

        flowBuilder.setInstructions(newInstructions.build());

        FlowBuilder newFlowBuilder = SfcOpenflowUtils.createFlowBuilder(
                flowBuilder.getTableId(),
                flowBuilder.getPriority() + 1,
                PacketInListener.TRACE_FULL_COKIE,
                "trace_flow", newMatch, newInstructions);

        return newFlowBuilder;
    }

    private FlowBuilder setFlowToBeTraced(FlowBuilder flowBuilder) {

        Instructions instruction = flowBuilder.getInstructions();
        List<Instruction> instructionList = instruction.getInstruction();

        int j = 0;
        List<Action> newActionList = new ArrayList<>();
        for (Instruction instruct : instructionList) {
            if (instruct.getInstruction() instanceof ApplyActionsCase) {
                ApplyActionsCase actionscase = (ApplyActionsCase) instruct.getInstruction();
                ApplyActions actions = actionscase.getApplyActions();
                newActionList.add(SfcOpenflowUtils.createActionDecTTL(0));
                newActionList.add(SfcOpenflowUtils.createActionAddProbePacket(1));
                for (Action action : actions.getAction()) {
                    ActionBuilder ab = createActionBuilder(action.getOrder() + 2);
                    ab.setAction(action.getAction());
                    newActionList.add(ab.build());
                }
                break;
            }
            j++;
        }

        instructionList.remove(j);
        // add actions
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(newActionList);
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setKey(new InstructionKey(j));
        ib.setOrder(j);
        instructionList.add(ib.build());


        // add write metadata with port information
        String outPort = getOutputPort(instructionList);
        if (String.valueOf("INPORT").equals(outPort)) {
                outPort = "0";
            }

        BigInteger metadataPort = new BigInteger(outPort, COOKIE_BIGINT_HEX_RADIX);

        int ibOrder = instructionList.size();
        addMetadata(instructionList, metadataPort, ibOrder);
        ibOrder++;

        short nextTable = TABLE_INDEX_TRANSPORT_EGRESS + 1;
        instructionList.add(setActionGoToTable(nextTable, ibOrder));
        InstructionsBuilder newInstructions = new InstructionsBuilder();

        newInstructions.setInstruction(instructionList);

        flowBuilder.setInstructions(newInstructions.build());

        Match match = flowBuilder.getMatch();
        MatchBuilder newMatch = new MatchBuilder(match);

        FlowBuilder newFlowBuilder = SfcOpenflowUtils.createFlowBuilder(
                flowBuilder.getTableId(),
                flowBuilder.getPriority() + 1,
                PacketInListener.TRACE_FULL_COKIE,
                "trace_flow", newMatch, newInstructions);

        return newFlowBuilder;
    }

      private FlowBuilder addDecttlRule(FlowBuilder flowBuilder) {

          Instructions instruction = flowBuilder.getInstructions();
          List<Instruction> instructionList = instruction.getInstruction();

          int i = 0;
          int j = 0;
          List<Action> newActionList = new ArrayList<>();
          for (Instruction instruct : instructionList) {
              if (instruct.getInstruction() instanceof ApplyActionsCase) {
                  ApplyActionsCase actionscase = (ApplyActionsCase) instruct.getInstruction();
                  ApplyActions actions = actionscase.getApplyActions();
                  newActionList.add(SfcOpenflowUtils.createActionDecTTL(i));
                  i++;
                  for (Action action : actions.getAction()) {
                      ActionBuilder ab = createActionBuilder(i);
                      ab.setAction(action.getAction());
                      newActionList.add(ab.build());
                      i++;
                  }
                  break;
              }
              j++;
          }

          instructionList.remove(j);
          // add actions
          ApplyActionsBuilder aab = new ApplyActionsBuilder();
          aab.setAction(newActionList);
          InstructionBuilder ib = new InstructionBuilder();
          ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
          ib.setKey(new InstructionKey(j));
          ib.setOrder(j);
          instructionList.add(ib.build());

          InstructionsBuilder newInstructions = new InstructionsBuilder();
          newInstructions.setInstruction(instructionList);

          //MatchBuilder newMatch = new MatchBuilder(flowBuilder.getMatch());
          //SfcOpenflowUtils.addMatchEtherType(newMatch,SfcOpenflowUtils.ETHERTYPE_IPV4);

          MatchBuilder newMatch = new MatchBuilder(flowBuilder.getMatch());
          SfcOpenflowUtils.addMatchIP(newMatch);

          FlowBuilder newFlowBuilder = SfcOpenflowUtils.createFlowBuilder(
                  flowBuilder.getTableId(),
                  flowBuilder.getPriority() +1,
                  flowBuilder.getCookie().getValue().add(BigInteger.ONE),
                  flowBuilder.getFlowName(), newMatch, newInstructions);

          return newFlowBuilder;
      }

        private FlowBuilder traceAction(FlowBuilder flowBuilder) {

        List<Instruction> instructionList = flowBuilder.getInstructions().getInstruction();

        String port = null;
        List<Action> newActionList = new ArrayList<>();
        int j = 0;
        //search output action get forwarder port and remove this action to be replaced by controller action
        for (Instruction instruct : instructionList) {
            if (instruct.getInstruction() instanceof ApplyActionsCase) {
                ApplyActionsCase actionscase = (ApplyActionsCase) instruct.getInstruction();
                int i = 0;
                ApplyActions actions = actionscase.getApplyActions();
                for (Action action : actions.getAction()) {
                    if (action.getAction() instanceof OutputActionCase) {
                        OutputActionCase outputActionCase = (OutputActionCase) action.getAction();
                        port = outputActionCase.getOutputAction().getOutputNodeConnector().getValue();
                    } else {
                        newActionList.add(action);
                    }
                    i++;
                }
                break;
            }
            j++;
        }
        instructionList.remove(j);


        // add actions
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(newActionList);
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setKey(new InstructionKey(j));
        ib.setOrder(j);


        instructionList.add(ib.build());

        // add probe packet filter in the match rule
        Match match = flowBuilder.getMatch();
        MatchBuilder newMatch = new MatchBuilder(match);

        SfcOpenflowUtils.addMatchEcn(newMatch, PacketInListener.PROBE_PACKET_FULL_TRACE_ID);

        InstructionsBuilder newInstructions = new InstructionsBuilder();
        newInstructions.setInstruction(instructionList);

        FlowBuilder newFlowBuilder = SfcOpenflowUtils.createFlowBuilder(
                flowBuilder.getTableId(),
                flowBuilder.getPriority() + 1,
                PacketInListener.TRACE_FULL_COKIE,
                "trace_flow", newMatch, newInstructions);

        return newFlowBuilder;
    }

    private String getOutputPort(List<Instruction> instructionList) {

        //search output action get forwarder port and remove this action to be replaced by controller action
        for (Instruction instruct : instructionList) {
            if (instruct.getInstruction() instanceof ApplyActionsCase) {
                ApplyActionsCase actionscase = (ApplyActionsCase) instruct.getInstruction();
                ApplyActions actions = actionscase.getApplyActions();
                for (Action action : actions.getAction()) {
                    if (action.getAction() instanceof OutputActionCase) {
                        OutputActionCase outputActionCase = (OutputActionCase) action.getAction();
                        return outputActionCase.getOutputAction().getOutputNodeConnector().getValue();
                    }
                }
            }
        }
        return null;
    }

    private Boolean IsPopNshRule(List<Instruction> instructionList) {

        //search output action get forwarder port and remove this action to be replaced by controller action
        for (Instruction instruct : instructionList) {
            if (instruct.getInstruction() instanceof ApplyActionsCase) {
                ApplyActionsCase actionscase = (ApplyActionsCase) instruct.getInstruction();
                ApplyActions actions = actionscase.getApplyActions();
                for (Action action : actions.getAction()) {
                    if (action.getAction() instanceof NxActionPopNshNodesNodeTableFlowApplyActionsCase) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Instruction setActionGoToTable(final short toTable, int order) {
        GoToTableBuilder gotoTb = new GoToTableBuilder();
        gotoTb.setTableId(toTable);

        InstructionBuilder ib = new InstructionBuilder();
        ib.setKey(new InstructionKey(order));
        ib.setOrder(order);
        ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoTb.build()).build());

        return ib.build();
    }


    private ActionBuilder setOutputAction(int order) {

        OutputActionBuilder output = new OutputActionBuilder();
        output.setMaxLength(new Integer(0xffff));
        Uri controllerPort = new Uri(OutputPortValues.CONTROLLER.toString());
        output.setOutputNodeConnector(controllerPort);

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());

        return ab;
    }

    private List<Instruction> addMetadata(List<Instruction> instructions, BigInteger value, int order) {
        InstructionBuilder metadataIb = new InstructionBuilder();
        metadataIb.setInstruction(
                SfcOpenflowUtils.createInstructionMetadata(
                        order,
                        value, METADATA_MASK_SFP_MATCH));
        metadataIb.setKey(new InstructionKey(order));
        metadataIb.setOrder(order++);
        instructions.add(metadataIb.build());
        return instructions;
    }


    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.StaleFlow> getStaleFlowInstanceIdentifier(StaleFlow staleFlow, InstanceIdentifier<FlowCapableNode> nodeIdent) {
        return nodeIdent
                .child(Table.class, new TableKey(staleFlow.getTableId()))
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.StaleFlow.class,
                        new StaleFlowKey(new FlowId(staleFlow.getId())));
    }

    private static ActionBuilder createActionBuilder(int order) {
        ActionBuilder ab = new ActionBuilder();
        ab.setOrder(order);
        ab.setKey(new ActionKey(order));

        return ab;
    }
}

