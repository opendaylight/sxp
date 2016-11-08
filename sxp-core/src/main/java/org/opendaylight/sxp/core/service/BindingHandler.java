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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final AtomicInteger bufferLimit = new AtomicInteger(1);
    private final SxpNode sxpNode;
    private final BindingDispatcher dispatcher;
    private final Map<SxpConnection, Deque<DecodedMessage>> buffer = new HashMap<>();

    /**
     * @param node       Owner of Handler
     * @param dispatcher Dispatcher service used for sending Bindings
     */
    public BindingHandler(SxpNode node, BindingDispatcher dispatcher) {
        this.sxpNode = Preconditions.checkNotNull(node);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);

    }

    /**
     * @param node       Owner of Handler
     * @param dispatcher Dispatcher service used for sending Bindings
     * @param bufferSize  Size which will be used for message joining
     */
    public BindingHandler(SxpNode node, BindingDispatcher dispatcher, int bufferSize) {
        this.sxpNode = Preconditions.checkNotNull(node);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        setBufferLimit(bufferSize);
    }

    /**
     * Set max number of Add update messages to be merged together.
     *
     * @param limit Size which will be used for message joining
     * @throws IllegalArgumentException If size of message merge is bellow 1
     */
    public void setBufferLimit(int limit) {
        if (limit > 0) {
            bufferLimit.set(limit);
        } else {
            throw new IllegalArgumentException("Buffer limit must be at least 1");
        }
    }

    /**
     * Removes all paths that contains specified NodeId,
     * thus performs loop filtering
     *
     * @param nodeId   NodeId to be used as filter
     * @param bindings List of bindings to be checked
     * @return SxpDatabase without loops
     */
    public static <T extends SxpBindingFields> Stream<T> loopDetection(NodeId nodeId, Stream<T> bindings) {
        if (nodeId != null && bindings != null) {
            return bindings.filter(b -> !NodeIdConv.getPeerSequence(b.getPeerSequence()).contains(nodeId));
        }
        return bindings;
    }

    /**
     * Parse UpdateMessageLegacy and process addition of Bindings into new SxpDatabase
     *
     * @param message      UpdateMessageLegacy containing data to be proceed
     * @param filter       SxpBinding filter that will be applied to bindings
     * @param nodeIdRemote
     * @return List of new Bindings
     * @throws TlvNotFoundException If Tlv isn't present in message
     */
    public static List<SxpDatabaseBinding> processMessageAddition(UpdateMessageLegacy message, SxpBindingFilter filter,
            NodeId nodeIdRemote) throws TlvNotFoundException {
        List<SxpDatabaseBinding> bindings = new ArrayList<>();
        List<Peer> peers = new ArrayList<>();
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
     * @param filter  SxpBinding filter that will be applied to bindings
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
     * @return List of delBindings Bindings
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
     * @return List of delBindings Bindings
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
        SxpBindingFilter<?, ? extends SxpFilterFields> filter = connection.getFilter(FilterType.Inbound);
        synchronized (sxpDomain) {
            List<SxpDatabaseBinding> removed = sxpDomain.getSxpDatabase().deleteBindings(connection.getId()),
                    replace =
                            SxpDatabase.getReplaceForBindings(removed, sxpDomain.getSxpDatabase(), filterMap);
            connection.propagateUpdate(sxpDomain.getMasterDatabase().deleteBindings(removed),
                    sxpDomain.getMasterDatabase().addBindings(replace), sxpDomain.getConnections());
            sxpDomain.pushToSharedSxpDatabases(connection.getId(), filter, removed, replace);
        }
    }

    /**
     * Handle received bindings and add them into Sxp/MasterDatabase
     *
     * @param databaseDelete Bindings received as delete
     * @param databaseAdd    Bindings received as add
     * @param connection     SxpConnection on which bindings were received
     */
    public <T extends SxpBindingFields> void processUpdate(final List<T> databaseDelete, final List<T> databaseAdd,
            final SxpConnection connection) {
        final Deque<DecodedMessage> deque;
        final DecodedMessage message = new DecodedMessage(databaseDelete, databaseAdd);
        synchronized (buffer) {
            if (!buffer.containsKey(connection)) {
                buffer.put(connection, (deque = new ArrayDeque<>()));
            } else {
                deque = buffer.get(connection);
            }
        }
        deque.addLast(message);
        if (deque.size() == 1) {
            connection.getOwner()
                    .getWorker()
                    .executeTaskInSequence(() -> updateMessageCallback(message, connection, deque),
                            ThreadsWorker.WorkerType.INBOUND, connection);
        }
    }

    /**
     * @param message    Decoded message that will be proceed
     * @param connection Connection on which message was received
     * @param deque      Buffer containing Update messages from peer
     * @return Message that was proceed
     */
    private DecodedMessage updateMessageCallback(final DecodedMessage message, final SxpConnection connection,
            final Deque<DecodedMessage> deque) {
        if (message == null || connection == null || deque == null) {
            return new DecodedMessage(Collections.emptyList(), Collections.emptyList());
        }
        deque.pollFirst();
        boolean startNewCallback = false;
        for (int i = 0; !deque.isEmpty(); i++) {
            if ((startNewCallback = deque.peekFirst().containsDeleteBindings() || i >= bufferLimit.get() - 1)) {
                break;
            }
            message.joinAddBindings(deque.pollFirst().getAddBindings());
        }
        pushUpdate(message.getDelBindings(), message.getAddBindings(), connection);
        if (startNewCallback) {
            connection.getOwner()
                    .getWorker()
                    .executeTaskInSequence(() -> updateMessageCallback(deque.peekFirst(), connection, deque),
                            ThreadsWorker.WorkerType.INBOUND, connection);
        }
        return message;
    }

    /**
     * Handle received bindings and add them into Sxp/MasterDatabase
     *
     * @param databaseDelete Bindings received as delete
     * @param databaseAdd    Bindings received as add
     * @param connection     SxpConnection on which bindings were received
     */
    private void pushUpdate(Stream<SxpBindingFields> databaseDelete, Stream<SxpBindingFields> databaseAdd,
            SxpConnection connection) {
        final SxpDomain domain = sxpNode.getDomain(Objects.requireNonNull(connection).getDomainName());
        final SxpDatabaseInf sxpDatabase = domain.getSxpDatabase();
        final MasterDatabaseInf masterDatabase = domain.getMasterDatabase();
        // Loop detection.
        if (connection.getCapabilities().contains(CapabilityType.LoopDetection)) {
            databaseAdd = loopDetection(connection.getOwnerId(), databaseAdd);
        }
        Map<NodeId, SxpBindingFilter> filterMap = SxpDatabase.getInboundFilters(sxpNode, domain.getName());
        SxpBindingFilter<?, ? extends SxpFilterFields> filter = connection.getFilter(FilterType.Inbound);

        List<SxpDatabaseBinding> added = new ArrayList<>(), removed = new ArrayList<>(), replace = new ArrayList<>();
        List<SxpConnection> sxpConnections = sxpNode.getAllOnSpeakerConnections(domain.getName());
        synchronized (domain) {
            if (Objects.nonNull(databaseDelete)) {
                removed = sxpDatabase.deleteBindings(connection.getId(), databaseDelete.collect(Collectors.toList()));
                replace = SxpDatabase.getReplaceForBindings(removed, sxpDatabase, filterMap);
            }
            if (Objects.nonNull(databaseAdd)) {
                added = sxpDatabase.addBinding(connection.getId(), databaseAdd.collect(Collectors.toList()));
                if (filter != null)
                    added.removeIf(b -> !filter.test(b));
            }
            added.addAll(replace);
            List<MasterDatabaseBinding> deletedMaster = masterDatabase.deleteBindings(removed),
                    addedMaster =
                            masterDatabase.addBindings(added);
            dispatcher.propagateUpdate(deletedMaster, addedMaster, sxpConnections);
            domain.pushToSharedSxpDatabases(connection.getId(), filter, removed, added);
            if (!removed.isEmpty() || !added.isEmpty()) {
                LOG.info(connection.getOwnerId() + "[Deleted/Added] bindings [{}/{}]", deletedMaster.size(),
                        addedMaster.size(), replace.size());
            }
        }
    }

    /**
     * UpdateMessage wrapper
     */
    private class DecodedMessage {

        private final boolean containsDeleteBindings;
        private Stream<SxpBindingFields> addBindings, delBindings;

        /**
         * @param deleted Bindings to delete
         * @param added   Bindings to add
         */
        DecodedMessage(List<? extends SxpBindingFields> deleted, List<? extends SxpBindingFields> added) {
            this.delBindings = (Stream<SxpBindingFields>) Objects.requireNonNull(deleted).stream();
            this.addBindings = (Stream<SxpBindingFields>) Objects.requireNonNull(added).stream();
            this.containsDeleteBindings = !deleted.isEmpty();
        }

        /**
         * @return Bindings that will be addBindings
         */
        Stream<SxpBindingFields> getAddBindings() {
            return addBindings;
        }

        /**
         * @param stream Stream to be addBindings to dded bindings
         */
        private void joinAddBindings(Stream<SxpBindingFields> stream) {
            addBindings = Stream.concat(addBindings, Objects.requireNonNull(stream));
        }

        /**
         * @return Bindings that will be delBindings
         */
        Stream<SxpBindingFields> getDelBindings() {
            return delBindings;
        }

        /**
         * @return If message contains any binding to be delBindings
         */
        boolean containsDeleteBindings() {
            return containsDeleteBindings;
        }
    }
}
