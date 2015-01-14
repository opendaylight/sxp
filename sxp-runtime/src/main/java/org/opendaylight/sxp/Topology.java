/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.PasswordType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.master.database.Vpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Topology {

    protected enum TopologyType {
        TOPOLOGY_MESH, TOPOLOGY_NAG, TOPOLOGY_RING, TOPOLOGY_STAR_LOCAL, TOPOLOGY_STAR_REMOTE, TOPOLOGY_SXP_LAB, TOPOLOGY_DUMMY_NODE
    }

    public static String DEFAULT_PASSWORD = null;

    public static boolean DEVICES_BIDIRECTIONAL = false;

    public static int EXPANSION_QUANTITY = 100;

    private static final Logger LOG = LoggerFactory.getLogger(Topology.class.getName());

    public static final String ROUTER_PREFIX = "R";

    public static Version VERSION = Version.Version4;

    private static Connections createConnections(ConnectionMode connectionMode, String ipAddress, int port)
            throws Exception {
        List<Connection> connections = new ArrayList<Connection>();

        ConnectionBuilder connectionBuilder = new ConnectionBuilder();
        connectionBuilder.setMode(connectionMode);
        connectionBuilder.setVersion(Version.Version4);
        connectionBuilder.setPeerAddress(new Ipv4Address(ipAddress));
        connectionBuilder.setTcpPort(new PortNumber(port));
        connectionBuilder.setPassword(PasswordType.None);
        connectionBuilder.setDescription("Connection from " + port);
        connectionBuilder.setConnectionTimers(Configuration.getConnectionTimersListener());
        connections.add(connectionBuilder.build());

        ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder();
        connectionsBuilder.setConnection(connections);
        return connectionsBuilder.build();
    }

    private static Connections createConnections(String lname, String laddress, int lport, String rname,
            String raddress, int sport) throws Exception {
        List<Connection> connections = new ArrayList<Connection>();

        ConnectionBuilder connectionBuilder = new ConnectionBuilder();
        if (DEVICES_BIDIRECTIONAL) {
            connectionBuilder.setMode(ConnectionMode.Both);
            connectionBuilder.setConnectionTimers(Configuration.getConnectionTimersBoth());
        } else {
            connectionBuilder.setMode(ConnectionMode.Listener);
            connectionBuilder.setConnectionTimers(Configuration.getConnectionTimersListener());
        }
        connectionBuilder.setVersion(VERSION);
        connectionBuilder.setPeerAddress(new Ipv4Address(laddress));
        if (lport > 0) {
            connectionBuilder.setTcpPort(new PortNumber(lport));
        }
        connectionBuilder.setPassword(PasswordType.None);
        connectionBuilder.setDescription("Connection to " + lname);
        connections.add(connectionBuilder.build());

        connectionBuilder = new ConnectionBuilder();
        if (DEVICES_BIDIRECTIONAL) {
            connectionBuilder.setMode(ConnectionMode.Both);
            connectionBuilder.setConnectionTimers(Configuration.getConnectionTimersBoth());
        } else {
            connectionBuilder.setMode(ConnectionMode.Speaker);
            connectionBuilder.setConnectionTimers(Configuration.getConnectionTimersSpeaker());
        }
        connectionBuilder.setVersion(VERSION);
        connectionBuilder.setPeerAddress(new Ipv4Address(raddress));
        if (sport > 0) {
            connectionBuilder.setTcpPort(new PortNumber(sport));
        }
        connectionBuilder.setPassword(PasswordType.None);
        connectionBuilder.setDescription("Connection to " + rname);
        connections.add(connectionBuilder.build());

        ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder();
        connectionsBuilder.setConnection(connections);
        return connectionsBuilder.build();
    }

    public static Connections createConnections(String lname, String laddress, String rname, String raddress)
            throws Exception {
        return createConnections(lname, laddress, 0, rname, raddress, 0);
    }

    public static SxpNode createLocalNode(String nodeName, NodeId nodeId, int port, List<PrefixGroup> prefixGroups,
            Connections connections) throws Exception {

        SxpNodeIdentityBuilder nodeBuilder = new SxpNodeIdentityBuilder();
        nodeBuilder.setName(nodeName);
        nodeBuilder.setEnabled(true);
        nodeBuilder.setSourceIp(null);
        nodeBuilder.setTcpPort(new PortNumber(port));
        nodeBuilder.setVersion(VERSION);

        SecurityBuilder securityBuilder = new SecurityBuilder();
        securityBuilder.setPassword(DEFAULT_PASSWORD);
        nodeBuilder.setSecurity(securityBuilder.build());

        nodeBuilder.setMappingExpanded(EXPANSION_QUANTITY);
        nodeBuilder.setConnections(connections);

        SourceBuilder sourceBuilder = new SourceBuilder();
        sourceBuilder.setAttribute(new ArrayList<Attribute>());
        sourceBuilder.setBindingSource(DatabaseBindingSource.Local);
        sourceBuilder.setPrefixGroup(prefixGroups);

        List<Source> sources = new ArrayList<>();
        sources.add(sourceBuilder.build());

        MasterDatabaseBuilder masterDatabaseBuilder = new MasterDatabaseBuilder();
        masterDatabaseBuilder.setAttribute(new ArrayList<Attribute>());
        masterDatabaseBuilder.setSource(sources);
        masterDatabaseBuilder.setVpn(new ArrayList<Vpn>());
        nodeBuilder.setMasterDatabase(masterDatabaseBuilder.build());

        nodeBuilder.setTimers(Configuration.getNodeTimers());

        return Configuration.register(SxpNode.createInstance(nodeId, nodeBuilder.build()));
    }

    public static void createTopologyMesh(List<PrefixGroup> prefixGroups, int nNodes, String address, int initPort)
            throws Exception {

        SxpNode baseNode = createLocalNode(ROUTER_PREFIX + 0, NodeIdConv.create(128), Configuration.getConstants()
                .getPort(), prefixGroups, createConnections("ISR", "172.20.161.178", "ASR1K", "172.20.161.50"));
        Configuration.register(baseNode);

        LOG.info("Loading mesh topology:");
        for (int m = 0; m < 2; m++) {
            for (int j = 1; j <= nNodes; j++) {

                int i = j - 1;
                int k = j + 1;

                if (m == 0) {
                    LOG.info("[" + baseNode.getName() + "/" + address + "/" + baseNode.getServerPort() + "]-["
                            + ROUTER_PREFIX + j + "/" + address + "/" + (initPort + j) + "]-[" + ROUTER_PREFIX + k
                            + "/" + address + "/" + (initPort + k) + "]");

                    j++;
                    i = j - 1;
                    k = j + 1;

                    LOG.info("[" + ROUTER_PREFIX + i + "/" + address + "/" + (initPort + i) + "]-[" + ROUTER_PREFIX + j
                            + "/" + address + "/" + (initPort + j) + "]-[" + baseNode.getName() + "/" + address + "/"
                            + baseNode.getServerPort() + "]");

                    LOG.info("[" + ROUTER_PREFIX + j + "/" + address + "/" + (initPort + j) + "]-["
                            + baseNode.getName() + "/" + address + "/" + baseNode.getServerPort() + "]-["
                            + ROUTER_PREFIX + i + "/" + address + "/" + (initPort + i) + "]");
                } else {
                    // Node M: Listener to base, Speaker to N.
                    Connections connections = createConnections(baseNode.getName(), address, baseNode.getServerPort(),
                            ROUTER_PREFIX + k, address, initPort + k);
                    Configuration.register(createLocalNode(ROUTER_PREFIX + j, NodeIdConv.create(j), initPort + j, null,
                            connections));

                    j++;
                    i = j - 1;
                    k = j + 1;

                    // Node N: Listener to M, Speaker to B.
                    connections = createConnections(ROUTER_PREFIX + i, address, initPort + i, baseNode.getName(),
                            address, baseNode.getServerPort());
                    Configuration.register(createLocalNode(ROUTER_PREFIX + j, NodeIdConv.create(j), initPort + j, null,
                            connections));

                    // Node B: Listener to N, Speaker to M.
                    connections = createConnections(ROUTER_PREFIX + j, address, initPort + j, ROUTER_PREFIX + i,
                            address, initPort + i);

                    baseNode.addConnections(connections);
                }
            }
        }

    }

    /** TS: NAG demo topology. */
    public static void createTopologyNag(List<PrefixGroup> prefixGroups) throws Exception {
        List<Connection> connections = new ArrayList<Connection>();

        ConnectionBuilder connectionBuilder = new ConnectionBuilder();
        connectionBuilder.setMode(ConnectionMode.Listener);
        connectionBuilder.setVersion(Version.Version4);
        connectionBuilder.setPeerAddress(new Ipv4Address("10.10.11.1"));
        connectionBuilder.setPassword(PasswordType.None);
        connectionBuilder.setDescription("Connection from 3750");
        connectionBuilder.setConnectionTimers(Configuration.getConnectionTimersListener());
        // connections.add(connectionBuilder.build());

        connectionBuilder = new ConnectionBuilder();
        connectionBuilder.setMode(ConnectionMode.Speaker);
        connectionBuilder.setVersion(Version.Version4);
        connectionBuilder.setPeerAddress(new Ipv4Address("10.99.188.1"));
        connectionBuilder.setPassword(PasswordType.None);
        connectionBuilder.setDescription("Connection to ISR1900");
        connectionBuilder.setConnectionTimers(Configuration.getConnectionTimersSpeaker());
        connections.add(connectionBuilder.build());

        connectionBuilder = new ConnectionBuilder();
        connectionBuilder.setMode(ConnectionMode.Speaker);
        connectionBuilder.setVersion(Version.Version4);
        connectionBuilder.setPeerAddress(new Ipv4Address("10.99.10.12"));
        // connectionBuilder.setPeerAddress(new Ipv4Address("10.99.1.10"));
        connectionBuilder.setPassword(PasswordType.None);
        connectionBuilder.setDescription("Connection to ASR1K");
        connectionBuilder.setConnectionTimers(Configuration.getConnectionTimersSpeaker());
        connections.add(connectionBuilder.build());

        connectionBuilder = new ConnectionBuilder();
        connectionBuilder.setMode(ConnectionMode.Listener);
        // connectionBuilder.setMode(ConnectionMode.Speaker);
         connectionBuilder.setVersion(Version.Version2);
        connectionBuilder.setPeerAddress(new Ipv4Address("10.3.99.2"));
        connectionBuilder.setPassword(PasswordType.None);
        connectionBuilder.setDescription("Connection to ASA");
        connectionBuilder.setConnectionTimers(Configuration.getConnectionTimersSpeaker());
        connections.add(connectionBuilder.build());

        connectionBuilder = new ConnectionBuilder();
        connectionBuilder.setMode(ConnectionMode.Speaker);
        connectionBuilder.setVersion(Version.Version1);
        connectionBuilder.setPeerAddress(new Ipv4Address("10.99.1.3"));
        connectionBuilder.setPassword(PasswordType.None);
        connectionBuilder.setDescription("Connection to N7K");
        connectionBuilder.setConnectionTimers(Configuration.getConnectionTimersSpeaker());
        connections.add(connectionBuilder.build());

        ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder();
        connectionsBuilder.setConnection(connections);

        Configuration.register(createLocalNode(ROUTER_PREFIX + 0, NodeIdConv.create(128), Configuration.getConstants()
                .getPort(), prefixGroups, connectionsBuilder.build()));

    }

    public static void createTopologyRing(List<PrefixGroup> initPrefixGroups, int nNodes, String address, int initPort)
            throws Exception {
        LOG.info("Loading ring topology: ");
        for (int m = 0; m < 2; m++) {
            for (int j = 1; j <= nNodes; j++) {
                List<PrefixGroup> _prefixGroups = null;

                int i = j - 1;
                int k = j + 1;
                if (j == 1) {
                    _prefixGroups = initPrefixGroups;
                    i = nNodes;
                } else if (j == nNodes) {
                    k = 1;
                }
                if (m == 0) {
                    LOG.info("[" + ROUTER_PREFIX + i + "/" + address + "/" + (initPort + i) + "]-[" + ROUTER_PREFIX + j
                            + "/" + address + "/" + (initPort + j) + "]-[" + ROUTER_PREFIX + k + "/" + address + "/"
                            + (initPort + k) + "]");
                } else {
                    Connections connections = createConnections(ROUTER_PREFIX + i, address, initPort + i, ROUTER_PREFIX
                            + k, address, initPort + k);

                    Configuration.register(createLocalNode(ROUTER_PREFIX + j, NodeIdConv.create(j), initPort + j,
                            _prefixGroups, connections));
                }
            }
        }
    }

    public static void createTopologyStarLocalBase(ConnectionMode connectionMode, int nConnections, String ipAddress,
            int initRemotePort, int localBasePort) throws Exception {
        LOG.info("Loading star topology / local base:");
        List<Connection> connections = new ArrayList<Connection>();

        initRemotePort--;
        for (int i = 1; i <= nConnections; i++) {
            ConnectionBuilder connectionBuilder = new ConnectionBuilder();
            connectionBuilder.setMode(connectionMode);
            connectionBuilder.setVersion(Version.Version4);
            connectionBuilder.setPeerAddress(new Ipv4Address(ipAddress));
            connectionBuilder.setTcpPort(new PortNumber(initRemotePort + i));
            connectionBuilder.setPassword(PasswordType.None);
            connectionBuilder.setDescription("Connection from " + (initRemotePort + i));
            connectionBuilder.setConnectionTimers(Configuration.getConnectionTimersListener());
            connections.add(connectionBuilder.build());
        }

        ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder();
        connectionsBuilder.setConnection(connections);

        Configuration.register(createLocalNode(ROUTER_PREFIX + 0, NodeIdConv.create(128), localBasePort, null,
                connectionsBuilder.build()));
    }

    public static void createTopologyStarRemoteNodes(ConnectionMode connectionMode, List<PrefixGroup> initPrefixGroups,
            int nNodes, String ipAddress, int initLocalPort, int remoteBasePort) throws Exception {
        LOG.info("Loading star topology / remote nodes:");

        initLocalPort--;
        for (int j = 1; j <= nNodes; j++) {
            Connections connections = createConnections(connectionMode, ipAddress, remoteBasePort);
            Configuration.register(createLocalNode(ROUTER_PREFIX + j, NodeIdConv.create(j), initLocalPort + j,
                    initPrefixGroups, connections));
        }
    }

    /** TS: SXP speaker and listener devices counterparts. */
    public static void createTopologySxpLab(List<PrefixGroup> prefixGroups) throws Exception {
        Connections connections = createConnections("ISR", "172.20.161.178", "ASR1K", "172.20.161.50");
        Configuration.register(createLocalNode(ROUTER_PREFIX + 0, NodeIdConv.create(128), Configuration.getConstants()
                .getPort(), prefixGroups, connections));
    }

    public static void createTopologyDummyNode() throws Exception {
        Configuration.register(createLocalNode(ROUTER_PREFIX + 0, NodeIdConv.create(128), Configuration.getConstants()
                .getPort(), null, null));
    }
}
