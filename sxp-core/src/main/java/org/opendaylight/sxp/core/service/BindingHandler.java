/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

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
import org.opendaylight.sxp.util.exception.unknown.UnknownNodeIdException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.TlvType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessageLegacy;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlv.fields.tlv.optional.fields.SourceGroupTagTlvAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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

        peerSequence = null;
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
    private static PrefixGroup getPrefixGroups(String updateMessage, int sgt, List<IpPrefix> prefixes)
            throws UpdateMessageSgtException, UpdateMessagePrefixException {
        if (sgt == -1) {
            throw new UpdateMessageSgtException(updateMessage);
        } else if (prefixes.isEmpty()) {
            throw new UpdateMessagePrefixException(updateMessage);
        }

        PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder();
        // TODO: prefixGroupBuilder.setAttribute(value);
        prefixGroupBuilder.setSgt(new Sgt(sgt));
        List<Binding> bindings = new ArrayList<Binding>();

        DateAndTime timestamp = TimeConv.toDt(System.currentTimeMillis());

        for (IpPrefix ipPrefix : prefixes) {
            BindingBuilder bindingBuilder = new BindingBuilder();
            bindingBuilder.setIpPrefix(ipPrefix);
            bindingBuilder.setTimestamp(timestamp);
            bindings.add(bindingBuilder.build());
        }
        prefixGroupBuilder.setBinding(bindings);

        sgt = -1;
        prefixes.clear();
        return prefixGroupBuilder.build();
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
    public static SxpDatabase processMessageAddition(NodeId nodeId, UpdateMessageLegacy message)
            throws TlvNotFoundException, UpdateMessagePrefixGroupsException, UpdateMessagePeerSequenceException {
        SxpDatabaseBuilder databaseBuilder = new SxpDatabaseBuilder();

        DateAndTime timestamp = TimeConv.toDt(System.currentTimeMillis());
        List<PrefixGroup> prefixGroups = new ArrayList<>();

        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord mappingRecord : message
                .getMappingRecord()) {
            if (mappingRecord.getOperationCode().equals(AttributeType.AddIpv4)
                    || mappingRecord.getOperationCode().equals(AttributeType.AddIpv6)) {
                int sgt = ((SourceGroupTagTlvAttribute) MappingRecord.create(mappingRecord.getTlv()).get(TlvType.Sgt))
                        .getSourceGroupTagTlvAttributes().getSgt();

                boolean contains = false;
                for (PrefixGroup prefixGroup : prefixGroups) {
                    if (prefixGroup.getSgt().getValue() == sgt) {
                        BindingBuilder bindingBuilder = new BindingBuilder();
                        bindingBuilder.setIpPrefix(mappingRecord.getAddress());
                        bindingBuilder.setTimestamp(timestamp);
                        prefixGroup.getBinding().add(bindingBuilder.build());
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder();
                    prefixGroupBuilder.setSgt(new Sgt(sgt));

                    List<Binding> bindings = new ArrayList<Binding>();
                    BindingBuilder bindingBuilder = new BindingBuilder();
                    bindingBuilder.setIpPrefix(mappingRecord.getAddress());
                    bindingBuilder.setTimestamp(new DateAndTime(timestamp));
                    bindings.add(bindingBuilder.build());
                    prefixGroupBuilder.setBinding(bindings);

                    prefixGroups.add(prefixGroupBuilder.build());
                }
            }
        }

        if (prefixGroups.isEmpty()) {
            databaseBuilder.setAttribute(new ArrayList<Attribute>());
            databaseBuilder.setPathGroup(new ArrayList<PathGroup>());
            return databaseBuilder.build();
        }
        String updateMessage = MessageFactory.toString(message);
        List<NodeId> peerSequence = new ArrayList<NodeId>();
        peerSequence.add(nodeId);

        databaseBuilder.setAttribute(new ArrayList<Attribute>());
        databaseBuilder.setPathGroup(getPathGroups(updateMessage, new ArrayList<PathGroup>(), peerSequence,
                prefixGroups));
        return databaseBuilder.build();
    }

    /**
     * Parse UpdateMessage and process addition of Bindings into new SxpDatabase
     *
     * @param message UpdateMessage containing data to be proceed
     * @return SxpDatabase containing added Bindings
     * @throws TlvNotFoundException               If Tlv isn't present in message
     * @throws UpdateMessagePrefixGroupsException If PrefixGroup isn't correct in message
     * @throws UpdateMessagePeerSequenceException If PeerSequence isn't correct in message
     */
    public static SxpDatabase processMessageAddition(UpdateMessage message)
            throws UpdateMessageSgtException, UpdateMessagePrefixException, UpdateMessagePrefixGroupsException,
            UpdateMessagePeerSequenceException {
        String updateMessage = MessageFactory.toString(message);

        SxpDatabaseBuilder databaseBuilder = new SxpDatabaseBuilder();

        List<Attribute> attGlobalOptional = new ArrayList<>();
        List<PathGroup> pathGroups = new ArrayList<>();
        List<PrefixGroup> prefixGroups = new ArrayList<>();
        List<IpPrefix> prefixes = new ArrayList<IpPrefix>();

        List<NodeId> peerSequence = null;
        int sgt = -1;
        int seq = 0, sseq = 0;
        for (Attribute attribute : message.getAttribute()) {
            if (seq == 0 && attribute.getType() == AttributeType.PeerSequence) {
                seq++;
            }

            // 4. Path-groups processing.
            // 4.1 Per-path common optional attributes.
            // 4.2 Add-Prefix groups.
            // 4.1.1 Per <path, SGT> optional attribute.
            if (seq == 1) {
                while (true) {
                    if (sseq == 0) {
                        if (attribute.getType() == AttributeType.PeerSequence) {
                            peerSequence = ((PeerSequenceAttribute) attribute.getAttributeOptionalFields())
                                    .getPeerSequenceAttributes().getNodeId();
                            break;
                        } else {
                            sseq++;
                        }
                    }
                    if (sseq == 1) {
                        if (attribute.getType() == AttributeType.SourceGroupTag) {
                            sgt = ((SourceGroupTagAttribute) attribute.getAttributeOptionalFields())
                                    .getSourceGroupTagAttributes().getSgt();
                            break;
                        } else {
                            sseq++;
                        }
                    }
                    if (sseq == 2) {
                        // 4.1.2 IPv4-Add-Prefix attribute.
                        if (attribute.getType() == AttributeType.Ipv4AddPrefix) {
                            prefixes.addAll(((Ipv4AddPrefixAttribute) attribute.getAttributeOptionalFields())
                                    .getIpv4AddPrefixAttributes().getIpPrefix());
                            break;
                        }
                        // 4.1.3 IPv6-Add-Prefix attribute.
                        else if (attribute.getType() == AttributeType.Ipv6AddPrefix) {
                            prefixes.addAll(((Ipv6AddPrefixAttribute) attribute.getAttributeOptionalFields())
                                    .getIpv6AddPrefixAttributes().getIpPrefix());
                            break;
                        }
                        // 4.3 TODO: IPv4-Add-Table attribute.
                        else if (attribute.getType() == AttributeType.Ipv4AddTable) {
                            // prefixes.addAll(((Ipv4AddTableAttribute)
                            // attribute.getAttributeOptionalFields()).getIpPrefix());
                            break;
                        }
                        // 4.4 TODO: IPv6-Add-Table attribute.
                        else if (attribute.getType() == AttributeType.Ipv6AddTable) {
                            // prefixes.addAll(((Ipv6AddTableAttribute)
                            // attribute.getAttributeOptionalFields()).getIpPrefix());
                            break;
                        }
                        // 4.5 Add-IPv4 attributes.
                        else if (attribute.getType() == AttributeType.AddIpv4) {
                            prefixes.add(((AddIpv4Attribute) attribute.getAttributeOptionalFields())
                                    .getAddIpv4Attributes().getIpPrefix());
                            break;
                        }
                        // 4.6 Add-IPv6 attributes.
                        else if (attribute.getType() == AttributeType.AddIpv6) {
                            prefixes.add(((AddIpv6Attribute) attribute.getAttributeOptionalFields())
                                    .getAddIpv6Attributes().getIpPrefix());
                            break;

                        } else if (attribute.getType() == AttributeType.SourceGroupTag) {
                            sseq = 1;
                            // Prefix groups.
                            prefixGroups.add(getPrefixGroups(updateMessage, sgt, prefixes));
                            continue;

                        } else if (attribute.getType() == AttributeType.PeerSequence) {
                            // If an each prefix group is introduced by its own
                            // PEER_SEQUENCE attribute.
                            if (!prefixes.isEmpty() && prefixGroups.isEmpty()) {
                                prefixGroups.add(getPrefixGroups(updateMessage, sgt, prefixes));
                            }
                            sseq = 0;
                            // Path groups.
                            pathGroups = getPathGroups(updateMessage, pathGroups, peerSequence, prefixGroups);
                            continue;

                        } else {
                            seq++;
                            break;
                        }
                    }
                }

                // The last attribute.
                if (message.getAttribute().indexOf(attribute) == message.getAttribute().size() - 1) {
                    databaseBuilder.setAttribute(attGlobalOptional);
                    // Prefix groups.
                    prefixGroups.add(getPrefixGroups(updateMessage, sgt, prefixes));
                    // Path groups.
                    databaseBuilder.setPathGroup(getPathGroups(updateMessage, pathGroups, peerSequence, prefixGroups));
                    return databaseBuilder.build();
                }

            }
            // 5. Trailing optional non-transitive attributes
            // processing.
            if (seq == 2) {
                if (attribute.getFlags().isOptional() && attribute.getFlags().isNonTransitive()) {
                    attGlobalOptional.add(attribute);
                    continue;
                } else {
                    seq++;
                }
            }
        }
        databaseBuilder.setAttribute(attGlobalOptional);
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

        SxpDatabaseBuilder databaseBuilder = new SxpDatabaseBuilder();

        List<Attribute> attGlobalOptional = new ArrayList<Attribute>();
        List<IpPrefix> prefixes = new ArrayList<IpPrefix>();

        int seq = 0;
        for (Attribute attribute : message.getAttribute()) {
            // 2. Global optional attributes processing.
            if (seq == 0) {
                if (attribute.getFlags().isOptional()
                        && (attribute.getFlags().isPartial() || !attribute.getFlags().isNonTransitive())) {
                    attGlobalOptional.add(attribute);
                    continue;
                } else {
                    seq++;
                }
            }
            // 3. Delete attributes processing.
            if (seq == 1) {
                if (attribute.getType() == AttributeType.DelIpv4) {
                    prefixes.add(((DeleteIpv4Attribute) attribute.getAttributeOptionalFields())
                            .getDeleteIpv4Attributes().getIpPrefix());
                    continue;
                } else if (attribute.getType() == AttributeType.DelIpv6) {
                    prefixes.add(((DeleteIpv6Attribute) attribute.getAttributeOptionalFields())
                            .getDeleteIpv6Attributes().getIpPrefix());
                    continue;
                } else if (attribute.getType() == AttributeType.Ipv4DeletePrefix) {
                    prefixes.addAll(((Ipv4DeletePrefixAttribute) attribute.getAttributeOptionalFields())
                            .getIpv4DeletePrefixAttributes().getIpPrefix());
                    continue;
                } else if (attribute.getType() == AttributeType.Ipv6DeletePrefix) {
                    prefixes.addAll(((Ipv6DeletePrefixAttribute) attribute.getAttributeOptionalFields())
                            .getIpv6DeletePrefixAttributes().getIpPrefix());
                    continue;
                } else {
                    break;
                }
            }
        }
        if (prefixes.isEmpty()) {
            databaseBuilder.setAttribute(attGlobalOptional);
            databaseBuilder.setPathGroup(new ArrayList<PathGroup>());
            return databaseBuilder.build();
        }
        String updateMessage = MessageFactory.toString(message);

        List<PrefixGroup> prefixGroups = new ArrayList<>();
        prefixGroups.add(getPrefixGroups(updateMessage, Configuration.DEFAULT_PREFIX_GROUP, prefixes));
        List<NodeId> peerSequence = new ArrayList<NodeId>();
        peerSequence.add(nodeId);

        databaseBuilder.setAttribute(attGlobalOptional);
        databaseBuilder.setPathGroup(getPathGroups(updateMessage, new ArrayList<PathGroup>(), peerSequence,
                prefixGroups));
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
        SxpDatabaseBuilder databaseBuilder = new SxpDatabaseBuilder();

        List<IpPrefix> prefixes = new ArrayList<IpPrefix>();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord mappingRecord : message
                .getMappingRecord()) {
            if (mappingRecord.getOperationCode().equals(AttributeType.DelIpv4)) {
                prefixes.add(mappingRecord.getAddress());
            } else if (mappingRecord.getOperationCode().equals(AttributeType.DelIpv6)) {
                prefixes.add(mappingRecord.getAddress());
            }
        }

        if (prefixes.isEmpty()) {
            databaseBuilder.setAttribute(new ArrayList<Attribute>());
            databaseBuilder.setPathGroup(new ArrayList<PathGroup>());
            return databaseBuilder.build();
        }
        String updateMessage = MessageFactory.toString(message);

        List<PrefixGroup> prefixGroups = new ArrayList<>();
        prefixGroups.add(getPrefixGroups(updateMessage, Configuration.DEFAULT_PREFIX_GROUP, prefixes));
        List<NodeId> peerSequence = new ArrayList<NodeId>();
        peerSequence.add(nodeId);

        databaseBuilder.setAttribute(new ArrayList<Attribute>());
        databaseBuilder.setPathGroup(getPathGroups(updateMessage, new ArrayList<PathGroup>(), peerSequence,
                prefixGroups));
        return databaseBuilder.build();
    }

    /**
     * Validate if UpdateMessageLegacy isn't corrupted
     *
     * @param updateMessage UpdateMessageLegacy to be checked for errors
     * @return Validated UpdateMessageLegacy
     */
    private static UpdateMessageLegacy validateLegacyMessage(UpdateMessageLegacy updateMessage) {
        // TODO: Message validation.
        // Message decomposition.
        // Message attribute validation.
        return updateMessage;
    }

    /**
     * Validate if UpdateMessage isn't corrupted
     *
     * @param updateMessage UpdateMessage to be checked for errors
     * @return Validated UpdateMessage
     */
    private static UpdateMessage validateMessage(UpdateMessage updateMessage) {
        // TODO: Message validation.
        // Message decomposition.
        // Message attribute validation.
        return updateMessage;
    }

    /**
     * Execute new task which perform SXP-DB changes according to received Update Messages
     * and recursively check,if connection has Update Messages to proceed, if so start again.
     *
     * @param task       Task containing logic for exporting changes to SXP-DB
     * @param connection Connection on which Update Messages was received
     */
    private static void startBindingHandle(Callable<?> task, final SxpConnection connection) {
        if (task == null) {
            return;
        }
        ListenableFuture
                future =
                connection.getOwner().getWorker().executeTask(task, ThreadsWorker.WorkerType.INBOUND);
        connection.getOwner().getWorker().addListener(future, new Runnable() {

            @Override public void run() {
                if (connection.getInboundMonitor().decrementAndGet() > 0) {
                    startBindingHandle(connection.pollUpdateMessageInbound(), connection);
                }
            }
        });
    }

    /**
     * Parse UpdateLegacyNotification for deleted and added Bindings,
     * afterwards propagate those changes into SxpDatabase and
     * notifies BindingManager to export Bindings
     *
     * @param updateLegacyNotification UpdateLegacyNotification containing received message
     */
    private static void processUpdateLegacyNotification(UpdateLegacyNotification updateLegacyNotification) {
        SxpNode owner = updateLegacyNotification.getConnection().getOwner();
        // Validate message.
        validateLegacyMessage(updateLegacyNotification.getMessage());

        // Get message relevant peer node ID.
        NodeId peerId;
        try {
            peerId = NodeIdConv.createNodeId(updateLegacyNotification.getConnection().getDestination().getAddress());
        } catch (UnknownNodeIdException e) {
            LOG.warn(owner + " Unknown message relevant peer node ID | {} | {}", e.getClass().getSimpleName(),
                    e.getMessage());
            return;
        }

        // Prefixes deletion.
        SxpDatabase database;
        try {
            database = processMessageDeletion(peerId, updateLegacyNotification.getMessage());

            if (!database.getPathGroup().isEmpty()) {
                List<SxpBindingIdentity> deletedIdentities;
                synchronized (owner.getBindingSxpDatabase()) {
                    deletedIdentities = owner.getBindingSxpDatabase().deleteBindings(database);
                }
                LOG.info(owner + " Deleted legacy bindings | {}", deletedIdentities);
                // Notify the manager.
                owner.setSvcBindingManagerNotify();
            }
        } catch (DatabaseAccessException | UpdateMessagePeerSequenceException | UpdateMessagePrefixGroupsException | UpdateMessagePrefixException | UpdateMessageSgtException e) {
            LOG.warn("{} Process legacy message deletion ", owner, e);
            return;
        }

        // Prefixes addition.
        try {
            database = processMessageAddition(peerId, updateLegacyNotification.getMessage());

            if (!database.getPathGroup().isEmpty()) {
                boolean added = false;
                synchronized (owner.getBindingSxpDatabase()) {
                    added = owner.getBindingSxpDatabase().addBindings(database);
                }
                if (added) {
                    LOG.info(owner + " Added legacy bindings | {}", new SxpDatabaseImpl(database).toString());
                    // Notify the manager.
                    owner.setSvcBindingManagerNotify();
                }
            }
        } catch (DatabaseAccessException | UpdateMessagePeerSequenceException | UpdateMessagePrefixGroupsException | TlvNotFoundException e) {
            LOG.warn(" Process legacy message addition ", owner, e);
        }
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
        Callable task = new Callable<Void>() {

            @Override public Void call() throws Exception {
                processUpdateNotification(UpdateNotification.create(message, connection));
                return null;
            }
        };
        if (connection.getInboundMonitor().getAndIncrement() == 0) {
            startBindingHandle(task, connection);
        } else {
            connection.pushUpdateMessageInbound(task);
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
        Callable task = new Callable<Void>() {

            @Override public Void call() throws Exception {
                processUpdateLegacyNotification(UpdateLegacyNotification.create(message, connection));
                return null;
            }
        };
        if (connection.getInboundMonitor().getAndIncrement() == 0) {
            startBindingHandle(task, connection);
        } else {
            connection.pushUpdateMessageInbound(task);
        }
    }

    /**
     * Parse UpdateNotification for deleted and added Bindings,
     * afterwards propagate those changes into SxpDatabase and
     * notifies BindingManager to export Bindings
     *
     * @param updateNotification UpdateNotification containing received message
     */
    private static void processUpdateNotification(UpdateNotification updateNotification)
            throws UpdateMessagePrefixGroupsException {
        SxpNode owner = updateNotification.getConnection().getOwner();
        // Validate message.
        validateMessage(updateNotification.getMessage());

        // Get message relevant peer node ID.
        NodeId peerId = updateNotification.getConnection().getNodeIdRemote();
        if (peerId == null) {
            LOG.warn(owner + " Unknown message relevant peer node ID");
            return;
        }

        // Prefixes deletion.
        SxpDatabase database;
        try {
            database = processMessageDeletion(peerId, updateNotification.getMessage());

            if (!database.getPathGroup().isEmpty()) {
                List<SxpBindingIdentity> deletedIdentities;
                synchronized (owner.getBindingSxpDatabase()) {
                    deletedIdentities = owner.getBindingSxpDatabase().deleteBindings(database);
                }
                LOG.info(owner + " Deleted bindings | {}", deletedIdentities);
                // Notify the manager.
                owner.setSvcBindingManagerNotify();
            }
        } catch (DatabaseAccessException | UpdateMessagePeerSequenceException | UpdateMessagePrefixException | UpdateMessagePrefixGroupsException | UpdateMessageSgtException e) {
            LOG.warn(" Process message deletion ", owner, e);
            return;
        }

        // Prefixes addition.
        try {
            database = processMessageAddition(updateNotification.getMessage());
        } catch (UpdateMessagePrefixException | UpdateMessageSgtException | UpdateMessagePeerSequenceException e) {
            LOG.warn(" Process message addition ", owner, e);
            return;
        }
        // Loop detection.
        if (updateNotification.getConnection().getCapabilities().contains(CapabilityType.LoopDetection)) {
            database = loopDetection(owner.getNodeId(), database);
        }
        // Prefixes addition.
        try {
            if (!database.getPathGroup().isEmpty()) {
                boolean added = false;
                synchronized (owner.getBindingSxpDatabase()) {
                    added = owner.getBindingSxpDatabase().addBindings(database);
                }
                if (added) {
                    LOG.info(owner + " Added bindings | {}", new SxpDatabaseImpl(database).toString());
                    // Notify the manager.
                    owner.setSvcBindingManagerNotify();
                }
            }
        } catch (DatabaseAccessException e) {
            LOG.warn(" Process message addition | {} | {}", owner, e);
        }
    }
}
