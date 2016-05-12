/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.monitor.provider.provider;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    public static final short TABLE_INDEX_TRANSPORT_EGRESS = 10;
    public static final int COOKIE_BIGINT_HEX_RADIX = 16;
    public static final BigInteger TRANSPORT_EGRESS_COOKIE =
            new BigInteger("BA5EBA11BA5EBA11", COOKIE_BIGINT_HEX_RADIX);
    public static final BigInteger SFC_MONIOR_COOKIE = new BigInteger("1FFFF1", COOKIE_BIGINT_HEX_RADIX);

    public ForwarderFlowListner (final DataBroker db) {
        dataBroker = Preconditions.checkNotNull(db, "DataBroker can not be null!");
        registrationListener(db);
    }

    public void close () throws ExecutionException, InterruptedException {
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
            final InstanceIdentifier<FlowCapableNode> nodeIdent =
                    key.firstIdentifierOf(FlowCapableNode.class);
            DataTreeIdentifier<Flow> dif = change.getRootPath();

            Flow flow = mod.getDataAfter();

            if (flow.getTableId() == TABLE_INDEX_TRANSPORT_EGRESS && flow.getCookie().getValue().equals(TRANSPORT_EGRESS_COOKIE) ) {
                Match match = flow.getMatch();
                LOG.info("match - {}", match.toString());
                FlowBuilder flowBuilder = new FlowBuilder(flow);
                flowBuilder.setCookie(new FlowCookie(flow.getCookie().getValue().add(BigInteger.ONE)));
                flowBuilder.setCookieMask(new FlowCookie(flow.getCookieMask().getValue().add(BigInteger.ONE)));
//                flowBuilder.setPriority(flow.getPriority()+1);
//                flowBuilder.setId(flow.getId());
//                flowBuilder.setKey(flow.getKey());
//                flowBuilder.setTableId(TABLE_INDEX_TRANSPORT_EGRESS);
//                flowBuilder.setFlowName("sfc-monitor");
//                IpMatch ipMatch = match.getIpMatch();
//                IpMatchBuilder ipMatchBuider;
//                if (ipMatch != null) {
//                    LOG.info("ip match - {}", ipMatch.toString());
//                    ipMatchBuider = new IpMatchBuilder(ipMatch);
//                } else {
//                    LOG.info("ip match null");
//                    ipMatchBuider = new IpMatchBuilder();
//                }
//                Short enc = 1;
//                ipMatchBuider.setIpEcn(enc);
               // MatchBuilder matchBuilder = new MatchBuilder(match);
//                matchBuilder.setIpMatch(ipMatchBuider.build());
                //flowBuilder.setMatch(matchBuilder.build());

                Instructions instruction = flow.getInstructions();
                List<Instruction> instructionList =  instruction.getInstruction();

                short nextTable = TABLE_INDEX_TRANSPORT_EGRESS + 1;
                instructionList.add(setActionGoToTable(nextTable));
                InstructionsBuilder newInstructions = new InstructionsBuilder();


                newInstructions.setInstruction(instructionList);

//                InstructionBuilder ib = new InstructionBuilder();
//                List<Action> actions = new ArrayList<>();
//
//                actions.add(setOutputAction(0).build());
//                ApplyActionsBuilder aab = new ApplyActionsBuilder();
//                aab.setAction(actions);
//
//                ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
//                ib.setOrder(0);
//                ib.setKey(new InstructionKey(0));


                //newInstructions.getInstruction().add(ib.build());


                flowBuilder.setInstructions(newInstructions.build());




//                ActionBuilder ab = new ActionBuilder();
//                ab.setOrder(1);
//                ab.setKey(new ActionKey(1));
//                Uri value = new Uri(OutputPortValues.CONTROLLER.toString());
//
//                OutputActionBuilder output = new OutputActionBuilder();
//                output.setOutputNodeConnector(value);
//                ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
//                List<Action> actions = new ArrayList<>();
//                Action outaction = ab.build();
//                ApplyActionsBuilder aab = new ApplyActionsBuilder();
//                actions.add(outaction);
//
//                aab.setAction(actions);
//
//                // Wrap our Apply Action in an Instruction
//                InstructionBuilder ib = new InstructionBuilder();
//                ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
//                ib.setOrder(0);
//                ib.setKey(new InstructionKey(0));
//
//                // Put our Instruction in a list of Instructions
//                List<Instruction> instructions = new ArrayList<>();
//                instructions.add(ib.build());
//                InstructionsBuilder isb = new InstructionsBuilder();
//                isb.setInstruction(instructions);
//                flowBuilder.setInstructions(isb.build());
                LOG.info("-----------------------------------------------------");
                LOG.info("SEND FLOWWWWWWWWW - ----- {}    action {}",flowBuilder.getMatch().toString(), flowBuilder.getInstructions().toString());
                LOG.info("-----------------------------------------------------");
                //NodeRef sffnode =  new NodeRef(nodeIdent.firstIdentifierOf(Node.class));
//                FlowCapableNode fc = nodeIdent.getTargetType().cast(FlowCapableNode.class);
//
//                final TableKey tableKey = identifier.firstKeyOf(Table.class, TableKey.class);
//               // if (tableIdValidationPrecondition(tableKey, addDataObj)) {
//                    final AddFlowInputBuilder builder = new AddFlowInputBuilder(addDataObj);
//
//                    builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
//
//                    AddFlowInput gg = builder.build();
//
//               // }

                writeFlow.writeFlowToConfig("openflow:2", flowBuilder);

            }

        }
    }

    private Instruction setActionGoToTable(final short toTable) {
        GoToTableBuilder gotoTb = new GoToTableBuilder();
        gotoTb.setTableId(toTable);

        InstructionBuilder ib = new InstructionBuilder();
        int order = 1;
        ib.setKey(new InstructionKey(order));
        ib.setOrder(order);
        ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoTb.build()).build());

        return ib.build();
    }

    private ActionBuilder setOutputAction (int order) {

        OutputActionBuilder output = new OutputActionBuilder();
        output.setMaxLength(new Integer(0xffff));
        Uri controllerPort = new Uri(OutputPortValues.CONTROLLER.toString());
        output.setOutputNodeConnector(controllerPort);

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());

        return ab;
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

