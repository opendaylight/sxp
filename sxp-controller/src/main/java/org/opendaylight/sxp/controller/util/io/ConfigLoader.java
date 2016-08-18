/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.io;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.controller.config.yang.sxp.controller.conf.Connection;
import org.opendaylight.controller.config.yang.sxp.controller.conf.ConnectionTimers;
import org.opendaylight.controller.config.yang.sxp.controller.conf.Connections;
import org.opendaylight.controller.config.yang.sxp.controller.conf.SxpController;
import org.opendaylight.controller.config.yang.sxp.controller.conf.SxpNode;
import org.opendaylight.controller.config.yang.sxp.controller.conf.Timers;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * <pre>
 *  ODL-Karaf @see http://localhost:8181/restconf/config/network-topology:network-topology/topology/sxp
 * </pre>
 */
public final class ConfigLoader {

    private final DatastoreAccess datastoreAccess;

    public ConfigLoader(DatastoreAccess datastoreAccess) {
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
    }

    /**
     * @param nodeName             NodeId used for initialization
     * @param logicalDatastoreType Logical datastore type where topology will be initialized
     * @param datastoreAccess      Datastore access used for initialization
     * @return If Topology was successfully initialized
     */
    public static boolean initTopologyNode(final String nodeName, final LogicalDatastoreType logicalDatastoreType,
            final DatastoreAccess datastoreAccess) {
        InstanceIdentifier<Node>
                nodeIdentifier =
                NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                nodeName)));
        return datastoreAccess.checkAndPut(nodeIdentifier, new NodeBuilder().setKey(new NodeKey(
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                        nodeName))).build(), logicalDatastoreType, false);
    }

    /**
     * @param configuration Configuration containing data about SxpNode that will be written into DataStore
     */
    public void load(SxpController configuration) {
        if (configuration == null || configuration.getSxpNode() == null)
            return;
        configuration.getSxpNode().stream().forEach(n -> {
            final String nodeId = NodeIdConv.toString(n.getNodeId());
            if (initTopologyNode(nodeId, LogicalDatastoreType.CONFIGURATION, datastoreAccess)) {
                datastoreAccess.checkAndPut(NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                        nodeId))).augmentation(SxpNodeIdentity.class), parseNode(n),
                        LogicalDatastoreType.CONFIGURATION, false);
            }
        });
    }

    /**
     * @param node SxpNode containing data
     * @return SxpNodeIdentity containing provided data
     */
    private SxpNodeIdentity parseNode(SxpNode node) {
        SxpNodeIdentityBuilder identityBuilder = new SxpNodeIdentityBuilder();
        identityBuilder.setEnabled(node.getEnabled());
        identityBuilder.setSourceIp(node.getSourceIp());
        identityBuilder.setVersion(node.getVersion());
        identityBuilder.setTcpPort(node.getTcpPort());
        identityBuilder.setMappingExpanded(node.getMappingExpanded());
        identityBuilder.setSecurity(
                new SecurityBuilder().setPassword(Preconditions.checkNotNull(node.getSecurity()).getPassword())
                        .build());

        identityBuilder.setSxpPeerGroups(new SxpPeerGroupsBuilder().build());
        identityBuilder.setDescription(node.getDescription());
        List<SxpDomain> domains = new ArrayList<>();
        SxpDomainBuilder domain = new SxpDomainBuilder();
        domain.setDomainName(org.opendaylight.sxp.core.SxpNode.DEFAULT_DOMAIN);
        domain.setConnections(parseConnections(node.getConnections()));
        domain.setMasterDatabase(parseMasterDatabase(node.getMasterDatabase()));
        domains.add(domain.build());
        identityBuilder.setSxpDomains(new SxpDomainsBuilder().setSxpDomain(domains).build());
        identityBuilder.setTimers(parseNodeTimers(node.getTimers()));
        return identityBuilder.build();
    }

    /**
     * @param connections Connections containing data
     * @return Connections wrapping data
     */
    private org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections parseConnections(
            Connections connections) {
        ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder().setConnection(new ArrayList<>());
        if (connections != null && connections.getConnection() != null) {
            connectionsBuilder.setConnection(
                    connections.getConnection().stream().map(this::parseConnection).collect(Collectors.toList()));
        }
        return connectionsBuilder.build();
    }

    /**
     * @param timers Timers containing timer data
     * @return Timers wrapping data
     */
    private org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.Timers parseNodeTimers(
            Timers timers) {
        TimersBuilder timersBuilder = new TimersBuilder();
        if (timers != null) {
            //Speaker
            timersBuilder.setHoldTimeMinAcceptable(timers.getHoldTimeMinAcceptable());
            timersBuilder.setKeepAliveTime(timers.getKeepAliveTime());
            //Listener
            timersBuilder.setHoldTime(timers.getHoldTime());
            timersBuilder.setHoldTimeMax(timers.getHoldTimeMax());
            timersBuilder.setHoldTimeMin(timers.getHoldTimeMin());
            timersBuilder.setDeleteHoldDownTime(timers.getDeleteHoldDownTime());
            timersBuilder.setReconciliationTime(timers.getDeleteHoldDownTime());
            //Node
            timersBuilder.setRetryOpenTime(timers.getRetryOpenTime());
        }
        return timersBuilder.build();
    }

    /**
     * @param connections Connection containing data about peer
     * @return Connection data wrapper
     */
    private org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection parseConnection(
            Connection connections) {
        ConnectionBuilder connectionBuilder = new ConnectionBuilder();
        connectionBuilder.setPeerAddress(connections.getPeerAddress());
        connectionBuilder.setTcpPort(connections.getTcpPort());
        connectionBuilder.setMode(connections.getMode());
        connectionBuilder.setPassword(connections.getPassword());
        connectionBuilder.setDescription(connections.getDescription());
        connectionBuilder.setVersion(connections.getVersion());
        connectionBuilder.setConnectionTimers(parseConnectionTimers(connections.getConnectionTimers()));
        return connectionBuilder.build();
    }

    /**
     * @param timers Timers containing data about connection timers
     * @return Timers data wrapper
     */
    private org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimers parseConnectionTimers(
            ConnectionTimers timers) {
        ConnectionTimersBuilder timersBuilder = new ConnectionTimersBuilder();
        //Speaker
        timersBuilder.setHoldTimeMinAcceptable(timers.getHoldTimeMinAcceptable());
        timersBuilder.setKeepAliveTime(timers.getKeepAliveTime());
        //Listener
        timersBuilder.setHoldTime(timers.getHoldTime());
        timersBuilder.setHoldTimeMax(timers.getHoldTimeMax());
        timersBuilder.setHoldTimeMin(timers.getHoldTimeMin());
        timersBuilder.setDeleteHoldDownTime(timers.getDeleteHoldDownTime());
        timersBuilder.setReconciliationTime(timers.getDeleteHoldDownTime());
        return timersBuilder.build();
    }

    /**
     * @param database MasterDatabase containing bindings
     * @return MasterDatabase data wrapper
     */
    private MasterDatabase parseMasterDatabase(
            org.opendaylight.controller.config.yang.sxp.controller.conf.MasterDatabase database) {
        List<MasterDatabaseBinding> bindings = new ArrayList<>();
        MasterDatabaseBuilder databaseBuilder = new MasterDatabaseBuilder();
        databaseBuilder.setMasterDatabaseBinding(bindings);
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        bindingBuilder.setTimestamp(TimeConv.toDt(System.currentTimeMillis()));
        bindingBuilder.setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());
        if (database != null && database.getBinding() != null) {
            database.getBinding().stream().forEach(b -> {
                bindingBuilder.setSecurityGroupTag(b.getSgt());
                b.getIpPrefix().stream().forEach(p -> bindings.add(bindingBuilder.setIpPrefix(p).build()));
            });
        }
        return databaseBuilder.build();
    }

    /**
     * @param database MasterDatabase containing bindings
     * @return MasterDatabase data wrapper
     */
    public static MasterDatabase parseMasterDatabase(
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.MasterDatabase database) {
        List<MasterDatabaseBinding> bindings = new ArrayList<>();
        MasterDatabaseBuilder databaseBuilder = new MasterDatabaseBuilder();
        databaseBuilder.setMasterDatabaseBinding(bindings);
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        bindingBuilder.setTimestamp(TimeConv.toDt(System.currentTimeMillis()));
        bindingBuilder.setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());
        if (database != null && database.getBinding() != null) {
            database.getBinding().stream().forEach(b -> {
                bindingBuilder.setSecurityGroupTag(b.getSgt());
                b.getIpPrefix().stream().forEach(p -> bindings.add(bindingBuilder.setIpPrefix(p).build()));
            });
        }
        return databaseBuilder.build();
    }

    /**
     * @param connections Connection containing data about peer
     * @return Connection data wrapper
     */
    public static org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections parseConnections(
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections connections) {
        ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder().setConnection(new ArrayList<>());
        if (connections != null && connections.getConnection() != null) {
            connectionsBuilder.setConnection(connections.getConnection());
        }
        return connectionsBuilder.build();
    }
}
