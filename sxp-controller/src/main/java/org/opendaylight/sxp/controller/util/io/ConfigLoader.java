/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.io;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.config.yang.sxp.controller.conf.Binding;
import org.opendaylight.controller.config.yang.sxp.controller.conf.Connection;
import org.opendaylight.controller.config.yang.sxp.controller.conf.SxpController;
import org.opendaylight.controller.config.yang.sxp.controller.conf.Timers;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.util.database.DatastoreValidator;
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.controller.util.database.SxpDatastoreImpl;
import org.opendaylight.sxp.controller.util.database.access.DatastoreAccess;
import org.opendaylight.sxp.controller.util.exception.ConfigurationException;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.util.exception.connection.NoNetworkInterfacesException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.PasswordType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.network.topology.topology.node.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.network.topology.topology.node.timers.ListenerProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.network.topology.topology.node.timers.SpeakerProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * <pre>
 *  ODL-Karaf @see http://localhost:8181/restconf/config/network-topology:network-topology/topology/sxp
 * </pre>
 */
public class ConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoader.class.getName());

    public static ConfigLoader create(DatastoreValidator datastoreValidator) {
        return new ConfigLoader(datastoreValidator);
    }

    private DatastoreValidator datastoreValidator;

    private ConfigLoader(DatastoreValidator datastoreValidator) {
        this.datastoreValidator = Preconditions.checkNotNull(datastoreValidator);
    }

    public void load(SxpController configuration) {
        for (org.opendaylight.controller.config.yang.sxp.controller.conf.SxpNode nodeConfiguration : configuration
                .getSxpNode()) {
            try {
                NodeId _nodeId = null;
                if (nodeConfiguration.getNodeId() == null) {
                    InetAddress bestLocalAddress = Search.getBestLocalDeviceAddress();
                    if (bestLocalAddress != null) {
                        _nodeId = NodeIdConv.createNodeId(bestLocalAddress);
                    }
                } else {
                    _nodeId = new NodeId(nodeConfiguration.getNodeId());
                }

                String nodeId = Configuration.CONTROLLER_NAME;
                if (_nodeId != null) {
                    nodeId = NodeIdConv.toString(_nodeId);
                }

                datastoreValidator.validateSxpNodePath(nodeId, LogicalDatastoreType.CONFIGURATION);

                datastoreValidator.validateSxpNodePath(nodeId, LogicalDatastoreType.OPERATIONAL);

                org.opendaylight.sxp.core.SxpNode node = parseNode(_nodeId, nodeConfiguration);

                LOG.info("{} SXP module created", node);

                Configuration.register(node);

            } catch (Exception e) {
                String name = nodeConfiguration.getNodeId() != null ? NodeIdConv
                        .toString(nodeConfiguration.getNodeId()) : Configuration.CONTROLLER_NAME;
                LOG.error("[{}] Node configuration error ", name, e);
            }
        }
    }

    private Connections parseConnections(NodeId nodeId,
            org.opendaylight.controller.config.yang.sxp.controller.conf.SxpNode configuration) {
        ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder();
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.Connection> connections = new ArrayList<>();
        if (configuration.getConnections() == null) {
            LOG.info("No connections found in config file.");
            return connectionsBuilder.setConnection(
                Collections.<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.Connection> emptyList())
                    .build();
        }

        for (Connection connection : configuration.getConnections().getConnection()) {
            ConnectionBuilder connectionBuilder = new ConnectionBuilder();

            connectionBuilder.setVpn(connection.getVpn());

            IpAddress peerAddress = connection.getPeerAddress();
            if (peerAddress == null) {
                LOG.error("[{}] Connection configuration error | Parameter 'peer-address' not defined",
                        NodeIdConv.toString(nodeId));
                continue;
            }
            connectionBuilder.setPeerAddress(peerAddress);
            connectionBuilder.setTcpPort(connection.getTcpPort());
            connectionBuilder.setKey(new ConnectionKey(peerAddress, connection.getTcpPort()));

            PasswordType _passwordType = connection.getPassword();
            if (_passwordType == null) {
                _passwordType = PasswordType.None;
            }
            connectionBuilder.setPassword(_passwordType);

            ConnectionMode _connectionMode = connection.getMode();
            if (_connectionMode == null) {
                LOG.error("[{}] Connection configuration error | Parameter 'mode' not defined",
                        NodeIdConv.toString(nodeId));
                continue;
            }

            connectionBuilder.setMode(connection.getMode());
            connectionBuilder.setVersion(connection.getVersion());
            String _description = "";
            if (connection.getDescription() != null) {
                _description = connection.getDescription();
            }
            connectionBuilder.setDescription(_description);

            ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder();
            if (_connectionMode.equals(ConnectionMode.Speaker) || _connectionMode.equals(ConnectionMode.Both)) {
                connectionTimersBuilder.setHoldTimeMinAcceptable(connection.getConnectionTimers()
                        .getHoldTimeMinAcceptable());
                connectionTimersBuilder.setKeepAliveTime(connection.getConnectionTimers().getKeepAliveTime());
            }
            if (_connectionMode.equals(ConnectionMode.Listener) || _connectionMode.equals(ConnectionMode.Both)) {
                connectionTimersBuilder.setReconciliationTime(connection.getConnectionTimers().getReconciliationTime());
                connectionTimersBuilder.setHoldTime(connection.getConnectionTimers().getHoldTime());
                connectionTimersBuilder.setHoldTimeMin(connection.getConnectionTimers().getHoldTimeMin());
                connectionTimersBuilder.setHoldTimeMax(connection.getConnectionTimers().getHoldTimeMax());
            }
            connectionBuilder.setConnectionTimers(connectionTimersBuilder.build());

            connections.add(connectionBuilder.build());
        }
        connectionsBuilder.setConnection(connections);
        return connectionsBuilder.build();
    }

    private List<MasterDatabaseBinding> parseMasterDatabase(
            org.opendaylight.controller.config.yang.sxp.controller.conf.SxpNode configuration) {
        List<MasterDatabaseBinding> bindings = new ArrayList<>();

        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        bindingBuilder.setTimestamp(TimeConv.toDt(System.currentTimeMillis()));
        bindingBuilder.setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());
        if (configuration.getMasterDatabase() != null) {
            if (configuration.getMasterDatabase().getBinding() != null) {
                for (Binding binding : configuration.getMasterDatabase().getBinding()) {
                    bindingBuilder.setSecurityGroupTag(binding.getSgt());
                    for (IpPrefix prefix : binding.getIpPrefix()) {
                        bindings.add(bindingBuilder.setIpPrefix(prefix).build());
                    }
                }
            }
        }
        return bindings;
    }

    private org.opendaylight.sxp.core.SxpNode parseNode(NodeId nodeId,
            org.opendaylight.controller.config.yang.sxp.controller.conf.SxpNode configuration)
            throws ConfigurationException, NoNetworkInterfacesException, SocketException {

        if (nodeId == null || nodeId.toString().isEmpty()) {
            throw new ConfigurationException("Parameter 'node-id' not defined");
        }

        // SXP node identity.
        SxpNodeIdentityBuilder nodeBuilder = new SxpNodeIdentityBuilder();
        boolean enabled = false;
        if (configuration.getEnabled() != null) {
            enabled = configuration.getEnabled();
        }
        nodeBuilder.setEnabled(enabled);
        nodeBuilder.setSourceIp(configuration.getSourceIp());

        int tcpPort = Configuration.getConstants().getPort();
        if (configuration.getTcpPort() != null && configuration.getTcpPort().getValue() > 0) {
            tcpPort = configuration.getTcpPort().getValue();
        }
        nodeBuilder.setTcpPort(new PortNumber(tcpPort));
        nodeBuilder.setVersion(configuration.getVersion());

        SecurityBuilder securityBuilder = new SecurityBuilder();
        if (configuration.getSecurity() != null && configuration.getSecurity().getPassword() != null
                && !configuration.getSecurity().getPassword().isEmpty()) {
            securityBuilder.setPassword(configuration.getSecurity().getPassword());
        }
        nodeBuilder.setSecurity(securityBuilder.build());

        int expansionQuantity = 0;
        if (configuration.getMappingExpanded() != null) {
            expansionQuantity = configuration.getMappingExpanded();
        }
        nodeBuilder.setMappingExpanded(expansionQuantity);
        nodeBuilder.setDescription(configuration.getDescription());

        // Connections.
        nodeBuilder.setConnections(parseConnections(nodeId, configuration));

        // Timers.
        nodeBuilder.setTimers(parseTimers(configuration.getTimers()));

        InstanceIdentifier<SxpNodeIdentity> nodeIdentifier = InstanceIdentifier
                .builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class,
                        new NodeKey(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                        NodeIdConv.toString(nodeId)))).augmentation(SxpNodeIdentity.class).build();

        DatastoreAccess datastoreAccess = datastoreValidator.getDatastoreAccess();

        SxpNodeIdentity node = nodeBuilder.build();
        try {
            datastoreAccess.merge(nodeIdentifier, node, LogicalDatastoreType.CONFIGURATION).get();
        } catch (CancellationException | ExecutionException | InterruptedException e) {
            throw new ConfigurationException("Failed to create node \"" + nodeId
                    + "\" identity in configuration datastore");
        }

        // Capabilities.
        nodeBuilder.setCapabilities(Configuration.getCapabilities(configuration.getVersion()));

        SxpDatastoreImpl sxpDatabaseProvider = new SxpDatastoreImpl(datastoreAccess, nodeId.getValue());

        MasterDatastoreImpl ipSgtMasterDatabaseProvider = new MasterDatastoreImpl(datastoreAccess, nodeId.getValue());

        // Local bindings.
        ipSgtMasterDatabaseProvider.addBindings(parseMasterDatabase(configuration));

        return org.opendaylight.sxp.core.SxpNode.createInstance(nodeId, nodeBuilder.build(), ipSgtMasterDatabaseProvider,
                sxpDatabaseProvider);
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.network.topology.topology.node.Timers parseTimers(
            Timers timers) {
        TimersBuilder timersBuilder = new TimersBuilder();
        timersBuilder.setRetryOpenTime(timers.getRetryOpenTime());

        // Listener profile.
        ListenerProfileBuilder lprofileBuilder = new ListenerProfileBuilder();
        lprofileBuilder.setDeleteHoldDownTime(timers.getDeleteHoldDownTime());
        lprofileBuilder.setHoldTimeMax(timers.getHoldTimeMax());
        lprofileBuilder.setHoldTimeMin(timers.getHoldTimeMin());
        lprofileBuilder.setHoldTime(timers.getHoldTime());
        lprofileBuilder.setReconciliationTime(timers.getReconciliationTime());
        timersBuilder.setListenerProfile(lprofileBuilder.build());

        // Speaker profile.
        SpeakerProfileBuilder sprofileBuilder = new SpeakerProfileBuilder();
        sprofileBuilder.setHoldTimeMinAcceptable(timers.getHoldTimeMinAcceptable());
        sprofileBuilder.setKeepAliveTime(timers.getKeepAliveTime());
        timersBuilder.setSpeakerProfile(sprofileBuilder.build());
        return timersBuilder.build();
    }
}
