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
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.ThreadsWorker;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.core.messaging.legacy.MappingRecord;
import org.opendaylight.sxp.util.database.SxpBindingIdentity;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.exception.message.UpdateMessagePeerSequenceException;
import org.opendaylight.sxp.util.exception.message.UpdateMessagePrefixException;
import org.opendaylight.sxp.util.exception.message.UpdateMessagePrefixGroupsException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageSgtException;
import org.opendaylight.sxp.util.exception.message.attribute.TlvNotFoundException;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabaseBuilder;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * BindingHandler class contains logic for parsing and propagating
 * changes into SxpDatabase based on received UpdateMessages
 */
public final class BindingHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(BindingHandler.class.getName());

    /**
     * Gets PathGroup generated from specified values
     *
     * @param updateMessage String representation of message
     * @param pathGroups    PathGroup where the new one will be placed
     * @param peerSequence  PeerSequence used in new PathGroup
     * @param prefixGroups  PrefixGroup used in new PathGroup
     * @return List of PathGroups with newly generated one
     * @throws UpdateMessagePeerSequenceException If PeerSequence is empty or null
     * @throws UpdateMessagePrefixGroupsException If PrefixGroup is empty or null
     */
    private static List<PathGroup> getPathGroups(String updateMessage, List<PathGroup> pathGroups,
            List<NodeId> peerSequence, List<PrefixGroup> prefixGroups)
            throws UpdateMessagePeerSequenceException, UpdateMessagePrefixGroupsException {
        if (peerSequence == null || peerSequence.isEmpty()) {
            throw new UpdateMessagePeerSequenceException(updateMessage);
        } else if (prefixGroups == null || prefixGroups.isEmpty()) {
            throw new UpdateMessagePrefixGroupsException(updateMessage);
        }

        PathGroupBuilder pathGroupBuilder = new PathGroupBuilder();
        // TODO: pathGroupBuilder.setAttribute(value);
        pathGroupBuilder.setPathHash(NodeIdConv.hashCode(peerSequence));
        pathGroupBuilder.setPeerSequence(NodeIdConv.createPeerSequence(peerSequence));
        pathGroupBuilder.setPrefixGroup(new ArrayList<>(prefixGroups));
        pathGroups.add(pathGroupBuilder.build());

        prefixGroups.clear();
        return pathGroups;
    }

    /**
     * Gets PrefixGroup generated from specified values
     *
     * @param updateMessage String representation of message
     * @param sgt           Sgt value assigned to PrefixGroup
     * @param prefixes      IpPrefixes used in new PrefixGroup
     * @return Newly created PrefixGroup
     * @throws UpdateMessageSgtException    If is Sgt value isn't correct
     * @throws UpdateMessagePrefixException If Prefixes are empty or null
     */
    private static PrefixGroup getPrefixGroups(String updateMessage, int sgt, List<IpPrefix> prefixes,
            SxpBindingFilter filter)
            throws UpdateMessageSgtException, UpdateMessagePrefixException {
        if (sgt == -1) {
            throw new UpdateMessageSgtException(updateMessage);
        } else if (prefixes.isEmpty()) {
            throw new UpdateMessagePrefixException(updateMessage);
        }

        List<Binding> bindings = new ArrayList<>();
        PrefixGroup prefixGroup = new PrefixGroupBuilder().setSgt(new Sgt(sgt)).setBinding(bindings).build();
        DateAndTime timestamp = TimeConv.toDt(System.currentTimeMillis());

        for (IpPrefix ipPrefix : prefixes) {
            Binding binding = new BindingBuilder().setIpPrefix(ipPrefix).setTimestamp(timestamp).build();
            if (filter != null && filter.filter(
                    SxpBindingIdentity.create(binding, prefixGroup, new PathGroupBuilder().build()))) {
                continue;
            }
            bindings.add(binding);
        }
        prefixes.clear();
        return prefixGroup;
    }

    /**
     * Removes all paths that contains specified NodeId,
     * thus performs loop filtering
     *
     * @param nodeId   NodeId to be used as filter
     * @param database SxpDatabase containing data
     * @return SxpDatabase without loops
     */
    public static SxpDatabase loopDetection(NodeId nodeId, SxpDatabase database) {
        List<PathGroup> removed = new ArrayList<>();
        for (PathGroup pathGroup : database.getPathGroup()) {
            for (NodeId _nodeId : NodeIdConv.getPeerSequence(pathGroup.getPeerSequence())) {
                if (NodeIdConv.equalTo(_nodeId, nodeId)) {
                    removed.add(pathGroup);
                }
            }
        }
        database.getPathGroup().removeAll(removed);
        return database;
    }

    /**
     * Parse UpdateMessageLegacy and process addition of Bindings into new SxpDatabase
     *
     * @param nodeId  NodeId of Peer where message came from
     * @param message UpdateMessageLegacy containing data to be proceed
     * @return SxpDatabase containing added Bindings
     * @throws TlvNotFoundException               If Tlv isn't present in message
     * @throws UpdateMessagePrefixGroupsException If PrefixGroup isn't correct in message
     * @throws UpdateMessagePeerSequenceException If PeerSequence isn't correct in message
     */
    public static SxpDatabase processMessageAddition(NodeId nodeId, UpdateMessageLegacy message, SxpBindingFilter filter)
            throws TlvNotFoundException, UpdateMessagePrefixGroupsException, UpdateMessagePeerSequenceException {
        DateAndTime timestamp = TimeConv.toDt(System.currentTimeMillis());
        List<PathGroup> pathGroups = new ArrayList<>();
        List<NodeId> peerSequence = new ArrayList<>();
        Map<Sgt,PrefixGroup> prefixGroupMap = new HashMap<>();

        SxpDatabaseBuilder databaseBuilder = new SxpDatabaseBuilder().setPathGroup(pathGroups);
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord mappingRecord : message
                .getMappingRecord()) {
            switch (mappingRecord.getOperationCode()) {
                case AddIpv4:
                case AddIpv6:
                    Sgt
                            sgt =
                            new Sgt(((SourceGroupTagTlvAttribute) MappingRecord.create(mappingRecord.getTlv())
                                    .get(TlvType.Sgt)).getSourceGroupTagTlvAttributes().getSgt());
                    Binding
                            binding =
                            new BindingBuilder().setIpPrefix(mappingRecord.getAddress())
                                    .setTimestamp(new DateAndTime(timestamp))
                                    .build();
                    PrefixGroup prefixGroup = prefixGroupMap.get(sgt);
                    if (prefixGroup == null) {
                        PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder();
                        prefixGroupBuilder.setSgt(new Sgt(sgt));
                        prefixGroupBuilder.setBinding(new ArrayList<Binding>());
                        prefixGroup = prefixGroupBuilder.build();
                        prefixGroupMap.put(sgt, prefixGroup);
                    }
                    if (filter != null && filter.filter(
                            SxpBindingIdentity.create(binding, prefixGroup, new PathGroupBuilder().build()))) {
                        continue;
                    }
                    prefixGroup.getBinding().add(binding);
                    break;
            }
        }

        peerSequence.add(nodeId);
        if (!prefixGroupMap.isEmpty()) {
            databaseBuilder.setPathGroup(getPathGroups(MessageFactory.toString(message), pathGroups, peerSequence,
                    new ArrayList<PrefixGroup>(prefixGroupMap.values())));
        }
        return databaseBuilder.build();
    }

    /**
     * Parse UpdateMessage and process addition of Bindings into new SxpDatabase
     *
     * @param message UpdateMessage containing data to be proceed
     * @return SxpDatabase containing added Bindings
     * @throws UpdateMessagePrefixGroupsException If PrefixGroup isn't correct in message
     * @throws UpdateMessagePeerSequenceException If PeerSequence isn't correct in message
     */
    public static SxpDatabase processMessageAddition(UpdateMessage message, SxpBindingFilter filter)
            throws UpdateMessageSgtException, UpdateMessagePrefixException, UpdateMessagePrefixGroupsException,
            UpdateMessagePeerSequenceException {
        String updateMessage = MessageFactory.toString(message);
        SxpDatabaseBuilder databaseBuilder = new SxpDatabaseBuilder();

        List<PathGroup> pathGroups = new ArrayList<>();
        List<PrefixGroup> prefixGroups = new ArrayList<>();
        List<IpPrefix> prefixes = new ArrayList<>();
        List<NodeId> peerSequence = null;
        int sgt = -1;
        for (Attribute attribute : message.getAttribute()) {
            if (attribute.getFlags().isOptional() && attribute.getFlags().isNonTransitive()) {
                continue;
            }
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
                    if (peerSequence != null && !prefixes.isEmpty() && prefixGroups.isEmpty()) {
                        prefixGroups.add(getPrefixGroups(updateMessage, sgt, prefixes, filter));
                        pathGroups = getPathGroups(updateMessage, pathGroups, peerSequence, prefixGroups);
                    }
                    peerSequence =
                            ((PeerSequenceAttribute) attribute.getAttributeOptionalFields()).getPeerSequenceAttributes()
                                    .getNodeId();
                    break;
                case SourceGroupTag:
                    if (sgt != -1 && !prefixGroups.isEmpty()){
                        prefixGroups.add(getPrefixGroups(updateMessage, sgt, prefixes, filter));
                    }
                    sgt =
                            ((SourceGroupTagAttribute) attribute.getAttributeOptionalFields()).getSourceGroupTagAttributes()
                                    .getSgt();
                    break;
            }
        }
        if (peerSequence != null && !prefixes.isEmpty() && prefixGroups.isEmpty()) {
            prefixGroups.add(getPrefixGroups(updateMessage, sgt, prefixes, filter));
            pathGroups = getPathGroups(updateMessage, pathGroups, peerSequence, prefixGroups);
        }
        databaseBuilder.setPathGroup(pathGroups);
        return databaseBuilder.build();
    }

    /**
     * Parse UpdateMessage and process deletion of Bindings into new SxpDatabase
     *
     * @param nodeId  NodeId of Peer where message came from
     * @param message UpdateMessage containing data to be proceed
     * @return SxpDatabase containing deleted Bindings
     * @throws UpdateMessageSgtException          If Sgt in message isn't correct
     * @throws UpdateMessagePrefixException       If Prefix isn't correct in message
     * @throws UpdateMessagePrefixGroupsException If PrefixGroup isn't correct in message
     * @throws UpdateMessagePeerSequenceException If PeerSequence isn't correct in message
     */
    public static SxpDatabase processMessageDeletion(NodeId nodeId, UpdateMessage message)
            throws UpdateMessageSgtException, UpdateMessagePrefixException, UpdateMessagePrefixGroupsException,
            UpdateMessagePeerSequenceException {
        SxpDatabaseBuilder databaseBuilder = new SxpDatabaseBuilder().setPathGroup(new ArrayList<PathGroup>());

        List<IpPrefix> prefixes = new ArrayList<>();
        List<PrefixGroup> prefixGroups = new ArrayList<>();
        List<NodeId> peerSequence = new ArrayList<>();
        peerSequence.add(nodeId);

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
        if (!prefixes.isEmpty()) {
            String updateMessage = MessageFactory.toString(message);
            prefixGroups.add(getPrefixGroups(updateMessage, Configuration.DEFAULT_PREFIX_GROUP, prefixes, null));
            databaseBuilder.setPathGroup(
                    getPathGroups(updateMessage, databaseBuilder.getPathGroup(), peerSequence, prefixGroups));
        }
        return databaseBuilder.build();
    }

    /**
     * Parse UpdateMessageLegacy and process deletion of Bindings into new SxpDatabase
     *
     * @param nodeId  NodeId of Peer where message came from
     * @param message UpdateMessageLegacy containing data to be proceed
     * @return SxpDatabase containing deleted Bindings
     * @throws UpdateMessageSgtException          If Sgt in message isn't correct
     * @throws UpdateMessagePrefixException       If Prefix isn't correct in message
     * @throws UpdateMessagePrefixGroupsException If PrefixGroup isn't correct in message
     * @throws UpdateMessagePeerSequenceException If PeerSequence isn't correct in message
     */
    public static SxpDatabase processMessageDeletion(NodeId nodeId, UpdateMessageLegacy message)
            throws UpdateMessagePrefixGroupsException, UpdateMessagePeerSequenceException, UpdateMessageSgtException,
            UpdateMessagePrefixException {
        SxpDatabaseBuilder databaseBuilder = new SxpDatabaseBuilder().setPathGroup(new ArrayList<PathGroup>());
        List<PrefixGroup> prefixGroups = new ArrayList<>();
        List<NodeId> peerSequence = new ArrayList<>();

        List<IpPrefix> prefixes = new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord mappingRecord : message
                .getMappingRecord()) {
            switch (mappingRecord.getOperationCode()) {
                case DelIpv4:
                case DelIpv6:
                    prefixes.add(mappingRecord.getAddress());
                    break;
            }
        }
        if (!prefixes.isEmpty()) {
            String updateMessage = MessageFactory.toString(message);
            prefixGroups.add(getPrefixGroups(updateMessage, Configuration.DEFAULT_PREFIX_GROUP, prefixes, null));
            peerSequence.add(nodeId);
            databaseBuilder.setPathGroup(
                    getPathGroups(updateMessage, databaseBuilder.getPathGroup(), peerSequence, prefixGroups));
        }
        return databaseBuilder.build();
    }

    /**
     * Execute new task which perform SXP-DB changes according to received Update Messages
     * and recursively check,if connection has Update Messages to proceed, if so start again.
     *
     * @param connection Connection on which Update Messages was received
     */
    public static void startBindingHandle(final SxpConnection connection) {
        Callable task = connection.pollUpdateMessageInbound();
        if (task == null) {
            return;
        }
        ListenableFuture
                future =
                connection.getOwner().getWorker().executeTask(task, ThreadsWorker.WorkerType.INBOUND);
        connection.getOwner().getWorker().addListener(future, new Runnable() {

            @Override public void run() {
                if (connection.getInboundMonitor().decrementAndGet() > 0) {
                    startBindingHandle(connection);
                }
            }
        });
    }

    /**
     * Adds received UpdateMessage into queue for its
     * parsing and propagating changes contained in message
     * Used for Version 4
     *
     * @param message    UpdateMessage containing data to be proceed
     * @param connection Connection on which Update Messages was received
     */
    public static void processUpdateMessage(final UpdateMessage message, final SxpConnection connection) {
        synchronized (Preconditions.checkNotNull(connection).getInboundMonitor()) {
            if (connection.getNodeIdRemote() == null) {
                LOG.warn(connection.getOwner() + " Unknown message relevant peer node ID");
                return;
            }
            Callable task = new Callable<Void>() {

                @Override public Void call() throws Exception {
                    try {
                        SxpDatabase databaseDelete = processMessageDeletion(connection.getNodeIdRemote(), message),
                                databaseAdd = processMessageAddition(message, connection.getFilter(FilterType.InboundDiscarding));
                        processUpdate(databaseDelete, databaseAdd, connection.getOwner(), connection);
                    } catch (DatabaseAccessException | UpdateMessagePeerSequenceException | UpdateMessagePrefixException |
                            UpdateMessagePrefixGroupsException | UpdateMessageSgtException e) {
                        LOG.warn(" Process message addition/deletion ", connection.getOwner(), e);
                    }
                    return null;
                }
            };
            connection.pushUpdateMessageInbound(task);
            if (connection.getInboundMonitor().getAndIncrement() == 0) {
                startBindingHandle(connection);
            }
        }
    }

    /**
     * Adds received UpdateMessageLegacy into queue for its
     * parsing and propagating changes contained in message
     * Used for Version 1/2/3
     *
     * @param message    UpdateMessageLegacy containing data to be proceed
     * @param connection Connection on which Update Messages was received
     */
    public static void processUpdateMessage(final UpdateMessageLegacy message, final SxpConnection connection) {
        synchronized (Preconditions.checkNotNull(connection).getInboundMonitor()) {
            if (connection.getNodeIdRemote() == null) {
                LOG.warn(connection.getOwner() + " Unknown message relevant peer node ID");
                return;
            }
            Callable task = new Callable<Void>() {

                @Override public Void call() throws Exception {
                    try {
                        SxpDatabase databaseDelete = processMessageDeletion(connection.getNodeIdRemote(), message),
                                databaseAdd = processMessageAddition(connection.getNodeIdRemote(), message, connection.getFilter(FilterType.InboundDiscarding));
                        processUpdate(databaseDelete, databaseAdd, connection.getOwner(), connection);
                    } catch (DatabaseAccessException | UpdateMessagePeerSequenceException | UpdateMessagePrefixGroupsException | TlvNotFoundException | UpdateMessageSgtException | UpdateMessagePrefixException e) {
                        LOG.warn(" Process legacy message addition/deletion ", connection.getOwner(), e);
                    }
                    return null;
                }
            };
            connection.pushUpdateMessageInbound(task);
            if (connection.getInboundMonitor().getAndIncrement() == 0) {
                startBindingHandle(connection);
            }
        }
    }

    public static void processPurgeAllMessage(final SxpConnection connection) {
        synchronized (Preconditions.checkNotNull(connection).getInboundMonitor()) {
            if (connection == null || connection.getNodeIdRemote() == null) {
                return;
            }
            connection.pushUpdateMessageInbound(new Callable<Void>() {

                @Override public Void call() throws Exception {
                    connection.setPurgeAllMessageReceived();
                    connection.getContext().getOwner().purgeBindings(connection.getNodeIdRemote());
                    connection.getContext().getOwner().setSvcBindingManagerNotify();
                    return null;
                }
            });
            if (connection.getInboundMonitor().getAndIncrement() == 0) {
                startBindingHandle(connection);
            }
        }
    }

    private static void processUpdate(SxpDatabase databaseDelete, SxpDatabase databaseAdd, SxpNode owner,
            SxpConnection connection) throws DatabaseAccessException {
        // Loop detection.
        if (connection != null && connection.getCapabilities().contains(CapabilityType.LoopDetection)) {
            databaseAdd = loopDetection(owner.getNodeId(), databaseAdd);
        }
        List<SxpBindingIdentity> deletedIdentities = null;
        boolean added = false;
        synchronized (owner.getBindingSxpDatabase()) {
            if (!databaseDelete.getPathGroup().isEmpty()) {
                deletedIdentities = owner.getBindingSxpDatabase().deleteBindings(databaseDelete);
            }
            if (!databaseAdd.getPathGroup().isEmpty()) {
                added = owner.getBindingSxpDatabase().addBindings(databaseAdd);
            }
        }
        if ((deletedIdentities != null && !deletedIdentities.isEmpty()) || added) {
            owner.setSvcBindingManagerNotify();
        }
        if (deletedIdentities != null && !deletedIdentities.isEmpty()) {
            LOG.info(owner + " Deleted bindings | {}", deletedIdentities);
        }
        if (added) {
            LOG.info(owner + " Added bindings | {}", new SxpDatabaseImpl(databaseAdd).toString());
        }
    }
}
