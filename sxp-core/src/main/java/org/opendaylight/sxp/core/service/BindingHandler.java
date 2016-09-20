/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.legacy.MappingRecord;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.SxpDatabase;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.exception.message.attribute.TlvNotFoundException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.TlvType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.AddIpv4Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.AddIpv6Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.DeleteIpv4Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.DeleteIpv6Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.Ipv4AddPrefixAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.Ipv4DeletePrefixAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.Ipv6AddPrefixAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.Ipv6DeletePrefixAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.PeerSequenceAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.SourceGroupTagAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlv.fields.tlv.optional.fields.SourceGroupTagTlvAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BindingHandler class contains logic for parsing and propagating
 * changes into SxpDatabase based on received UpdateMessages
 */
public final class BindingHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(BindingHandler.class.getName());
    private final SxpNode sxpNode;
    private final BindingDispatcher dispatcher;

    /**
     * @param node       Owner of Handler
     * @param dispatcher Dispatcher service used for sending Bindings
     */
    public BindingHandler(SxpNode node, BindingDispatcher dispatcher) {
        this.sxpNode = Preconditions.checkNotNull(node);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
    }

    /**
     * Removes all paths that contains specified NodeId,
     * thus performs loop filtering
     *
     * @param nodeId   NodeId to be used as filter
     * @param bindings List of bindings to be checked
     * @return SxpDatabase without loops
     */
    public static <T extends SxpBindingFields> List<T> loopDetection(NodeId nodeId, List<T> bindings) {
        if (nodeId != null && bindings != null && !bindings.isEmpty()) {
            bindings.removeIf(b -> NodeIdConv.getPeerSequence(b.getPeerSequence()).contains(nodeId));
        }
        return bindings;
    }

    /**
     * Parse UpdateMessageLegacy and process addition of Bindings into new SxpDatabase
     *
     * @param message UpdateMessageLegacy containing data to be proceed
     * @param filter SxpBinding filter that will be applied to bindings
     * @param nodeIdRemote
     * @return List of new Bindings
     * @throws TlvNotFoundException               If Tlv isn't present in message
     */
    public static List<SxpDatabaseBinding> processMessageAddition(UpdateMessageLegacy message, SxpBindingFilter filter,
            NodeId nodeIdRemote)
            throws TlvNotFoundException {
        List<SxpDatabaseBinding> bindings = new ArrayList<>();
        List<Peer> peers=new ArrayList<>();
        peers.add(new PeerBuilder().setSeq(0).setNodeId(Preconditions.checkNotNull(nodeIdRemote)).build());
        SxpDatabaseBindingBuilder
                bindingBuilder =
                new SxpDatabaseBindingBuilder().setTimestamp(TimeConv.toDt(System.currentTimeMillis()))
                        .setPeerSequence(new PeerSequenceBuilder().setPeer(peers).build());

        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord mappingRecord : message
                .getMappingRecord()) {
            switch (mappingRecord.getOperationCode()) {
                case AddIpv4:
                case AddIpv6:
                    bindingBuilder.setSecurityGroupTag(
                            new Sgt(((SourceGroupTagTlvAttribute) MappingRecord.create(mappingRecord.getTlv())
                                    .get(TlvType.Sgt)).getSourceGroupTagTlvAttributes().getSgt()));
                    SxpDatabaseBinding binding = bindingBuilder.setIpPrefix(mappingRecord.getAddress()).build();
                    if (filter == null || !filter.apply(binding)) {
                        bindings.add(binding);
                    }
                    break;
            }
        }
        return bindings;
    }

    /**
     * Parse UpdateMessage and process addition of Bindings into new SxpDatabase
     *
     * @param message UpdateMessage containing data to be proceed
     * @param filter SxpBinding filter that will be applied to bindings
     * @return List of new Bindings
     */
    public static List<SxpDatabaseBinding> processMessageAddition(UpdateMessage message, SxpBindingFilter filter) {
        List<SxpDatabaseBinding> bindings = new ArrayList<>();
        List<IpPrefix> prefixes = new ArrayList<>();
        SxpDatabaseBindingBuilder
                bindingBuilder =
                new SxpDatabaseBindingBuilder().setTimestamp(TimeConv.toDt(System.currentTimeMillis()));

        for (Attribute attribute : message.getAttribute()) {
            switch (attribute.getType()) {
                case AddIpv4:
                    prefixes.add(((AddIpv4Attribute) attribute.getAttributeOptionalFields()).getAddIpv4Attributes()
                            .getIpPrefix());
                    break;
                case AddIpv6:
                    prefixes.add(((AddIpv6Attribute) attribute.getAttributeOptionalFields()).getAddIpv6Attributes()
                            .getIpPrefix());
                    break;
                case Ipv4AddPrefix:
                    prefixes.addAll(
                            ((Ipv4AddPrefixAttribute) attribute.getAttributeOptionalFields()).getIpv4AddPrefixAttributes()
                                    .getIpPrefix());
                    break;
                case Ipv6AddPrefix:
                    prefixes.addAll(
                            ((Ipv6AddPrefixAttribute) attribute.getAttributeOptionalFields()).getIpv6AddPrefixAttributes()
                                    .getIpPrefix());
                    break;
                case PeerSequence:
                    bindingBuilder.setPeerSequence(NodeIdConv.createPeerSequence(
                            ((PeerSequenceAttribute) attribute.getAttributeOptionalFields()).getPeerSequenceAttributes()
                                    .getNodeId()));
                    break;
                case SourceGroupTag:
                    bindingBuilder.setSecurityGroupTag(
                            new Sgt(((SourceGroupTagAttribute) attribute.getAttributeOptionalFields()).getSourceGroupTagAttributes()
                                    .getSgt()));
                    break;
            }
            prefixes.stream().forEach(p -> {
                SxpDatabaseBinding binding = bindingBuilder.setIpPrefix(p).build();
                if (filter == null || !filter.apply(binding)) {
                    bindings.add(binding);
                }
            });
            prefixes.clear();
        }
        return bindings;
    }

    /**
     * Parse UpdateMessage and process deletion of Bindings into new SxpDatabase
     *
     * @param message UpdateMessage containing data to be proceed
     * @return List of deleted Bindings
     */
    public static List<SxpDatabaseBinding> processMessageDeletion(UpdateMessage message) {
        List<IpPrefix> prefixes = new ArrayList<>();
        SxpDatabaseBindingBuilder
                bindingsBuilder =
                new SxpDatabaseBindingBuilder().setSecurityGroupTag(new Sgt(Configuration.DEFAULT_PREFIX_GROUP))
                        .setTimestamp(TimeConv.toDt(System.currentTimeMillis()))
                        .setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());

        for (Attribute attribute : message.getAttribute()) {
            if (attribute.getFlags().isOptional() && (attribute.getFlags().isPartial() || !attribute.getFlags()
                    .isNonTransitive())) {
                continue;
            }
            switch (attribute.getType()) {
                case DelIpv4:
                    prefixes.add(
                            ((DeleteIpv4Attribute) attribute.getAttributeOptionalFields()).getDeleteIpv4Attributes()
                                    .getIpPrefix());
                    break;
                case DelIpv6:
                    prefixes.add(
                            ((DeleteIpv6Attribute) attribute.getAttributeOptionalFields()).getDeleteIpv6Attributes()
                                    .getIpPrefix());
                    break;
                case Ipv4DeletePrefix:
                    prefixes.addAll(
                            ((Ipv4DeletePrefixAttribute) attribute.getAttributeOptionalFields()).getIpv4DeletePrefixAttributes()
                                    .getIpPrefix());
                    break;
                case Ipv6DeletePrefix:
                    prefixes.addAll(
                            ((Ipv6DeletePrefixAttribute) attribute.getAttributeOptionalFields()).getIpv6DeletePrefixAttributes()
                                    .getIpPrefix());
                    break;
            }
        }
        return prefixes.stream().map(p -> bindingsBuilder.setIpPrefix(p).build()).collect(Collectors.toList());
    }

    /**
     * Parse UpdateMessageLegacy and process deletion of Bindings into new SxpDatabase
     *
     * @param message UpdateMessageLegacy containing data to be proceed
     * @return List of deleted Bindings
     */
    public static List<SxpDatabaseBinding> processMessageDeletion(UpdateMessageLegacy message) {
        List<SxpDatabaseBinding> bindings = new ArrayList<>();
        SxpDatabaseBindingBuilder
                bindingsBuilder =
                new SxpDatabaseBindingBuilder().setSecurityGroupTag(new Sgt(Configuration.DEFAULT_PREFIX_GROUP))
                        .setTimestamp(TimeConv.toDt(System.currentTimeMillis()))
                        .setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());

        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord mappingRecord : message
                .getMappingRecord()) {
            switch (mappingRecord.getOperationCode()) {
                case DelIpv4:
                case DelIpv6:
                    bindings.add(bindingsBuilder.setIpPrefix(mappingRecord.getAddress()).build());
                    break;
            }
        }
        return bindings;
    }

    /**
     * Add Purge to inbound message queue and proceed it
     *
     * @param connection SxpConnection for which PurgeAll will be proceed
     */
    public static ListenableFuture<Void> processPurgeAllMessage(final SxpConnection connection) {
        return Preconditions.checkNotNull(connection).getOwner().getWorker().executeTaskInSequence(() -> {
            processPurgeAllMessageSync(connection);
            return null;
        }, ThreadsWorker.WorkerType.INBOUND, connection);
    }

    /**
     * Add Purge to inbound message queue and proceed it
     *
     * @param connection SxpConnection for which PurgeAll will be proceed
     */
    public static void processPurgeAllMessageSync(final SxpConnection connection) {
        final Map<NodeId, SxpBindingFilter>
                filterMap =
                SxpDatabase.getInboundFilters(connection.getOwner(), connection.getDomainName());
        final SxpDomain sxpDomain = connection.getOwner().getDomain(connection.getDomainName());
        synchronized (sxpDomain) {
            List<SxpDatabaseBinding> removed = sxpDomain.getSxpDatabase().deleteBindings(connection.getId()),
                    replace =
                            SxpDatabase.getReplaceForBindings(removed, sxpDomain.getSxpDatabase(), filterMap);
            connection.propagateUpdate(sxpDomain.getMasterDatabase().deleteBindings(removed),
                    sxpDomain.getMasterDatabase().addBindings(replace));
        }
    }

    /**
     * Handle received bindings and add them into Sxp/MasterDatabase
     *
     * @param databaseDelete Bindings received as delete
     * @param databaseAdd    Bindings received as add
     * @param connection     SxpConnection on which bindings were received
     * @param <T>            Any type extending SxpBindingFields
     */
    public <T extends SxpBindingFields> void processUpdate(List<T> databaseDelete, List<T> databaseAdd,
            SxpConnection connection) {
        final SxpDomain domain = sxpNode.getDomain(connection.getDomainName());
        final SxpDatabaseInf sxpDatabase = domain.getSxpDatabase();
        final MasterDatabaseInf masterDatabase = domain.getMasterDatabase();
        // Loop detection.
        if (Preconditions.checkNotNull(connection).getCapabilities().contains(CapabilityType.LoopDetection)) {
            databaseAdd = loopDetection(connection.getOwnerId(), databaseAdd);
        }
        Map<NodeId, SxpBindingFilter> filterMap = SxpDatabase.getInboundFilters(sxpNode, domain.getName());
        SxpBindingFilter<?, ? extends SxpFilterFields> filter = connection.getFilter(FilterType.Inbound);

        List<SxpDatabaseBinding> added = new ArrayList<>(), removed = new ArrayList<>(), replace = new ArrayList<>();
        List<SxpConnection> sxpConnections = sxpNode.getAllOnSpeakerConnections(domain.getName());
        synchronized (domain) {
            if (databaseDelete != null && !databaseDelete.isEmpty()) {
                removed = sxpDatabase.deleteBindings(connection.getId(), databaseDelete);
                replace = SxpDatabase.getReplaceForBindings(removed, sxpDatabase, filterMap);
            }
            if (databaseAdd != null && !databaseAdd.isEmpty()) {
                added = sxpDatabase.addBinding(connection.getId(), databaseAdd);
                if (filter != null)
                    added.removeIf(b -> !filter.test(b));
            }
            added.addAll(replace);
            List<MasterDatabaseBinding> deletedMaster = masterDatabase.deleteBindings(removed),
                    addedMaster = masterDatabase.addBindings(added);
            dispatcher.propagateUpdate(deletedMaster, addedMaster, sxpConnections);
            domain.pushToSharedSxpDatabases(connection.getId(), filter, removed, added);
            if (!removed.isEmpty() || !added.isEmpty()) {
                LOG.info(connection.getOwnerId() + "[Deleted/Added] bindings [{}/{}]", deletedMaster.size(),
                        addedMaster.size(), replace.size());
            }
        }
    }
}
