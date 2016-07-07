/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.io;

import com.google.common.util.concurrent.AbstractCheckedFuture;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.opendaylight.controller.config.yang.sxp.controller.conf.ConnectionTimers;
import org.opendaylight.controller.config.yang.sxp.controller.conf.Security;
import org.opendaylight.controller.config.yang.sxp.controller.conf.SxpController;
import org.opendaylight.controller.config.yang.sxp.controller.conf.SxpNode;
import org.opendaylight.controller.config.yang.sxp.controller.conf.Timers;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class) @PrepareForTest({DatastoreAccess.class, org.opendaylight.sxp.core.SxpNode.class,
        org.opendaylight.sxp.util.inet.Search.class, Configuration.class}) public class ConfigLoaderTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    private static DatastoreAccess access;
    private ConfigLoader configLoader;

    @Before public void init() throws Exception {
        access = PowerMockito.mock(DatastoreAccess.class);
        PowerMockito.when(
                access.put(any(InstanceIdentifier.class), any(SxpNodeIdentity.class), any(LogicalDatastoreType.class)))
                .thenReturn(mock(AbstractCheckedFuture.class));
        PowerMockito.when(access.checkAndPut(any(InstanceIdentifier.class), any(SxpNodeIdentity.class),
                any(LogicalDatastoreType.class), anyBoolean())).thenReturn(true);
        PowerMockito.mockStatic(org.opendaylight.sxp.core.SxpNode.class, RETURNS_MOCKS);
        PowerMockito.mockStatic(org.opendaylight.sxp.util.inet.Search.class);
        configLoader = new ConfigLoader(access);
    }

    @Test public void testInitTopologyNode() throws Exception {
        assertTrue(ConfigLoader.initTopologyNode("0.0.0.0", LogicalDatastoreType.OPERATIONAL, access));
        assertTrue(ConfigLoader.initTopologyNode("0.0.0.0", LogicalDatastoreType.CONFIGURATION, access));
    }

    private org.opendaylight.controller.config.yang.sxp.controller.conf.Connection getConnection(Version version,
            ConnectionMode mode) {
        org.opendaylight.controller.config.yang.sxp.controller.conf.Connection
                connection =
                new org.opendaylight.controller.config.yang.sxp.controller.conf.Connection();
        connection.setVersion(version);
        connection.setMode(mode);
        connection.setPeerAddress(new IpAddress("1.1.1.1".toCharArray()));
        connection.setTcpPort(PortNumber.getDefaultInstance("60000"));
        connection.setConnectionTimers(new ConnectionTimers());
        return connection;
    }

    private org.opendaylight.controller.config.yang.sxp.controller.conf.Binding getBinding(Integer sgt,
            String... strings) {
        org.opendaylight.controller.config.yang.sxp.controller.conf.Binding
                binding =
                new org.opendaylight.controller.config.yang.sxp.controller.conf.Binding();
        if (sgt != null) {
            binding.setSgt(new Sgt(sgt));
        }
        if (strings != null) {
            List<IpPrefix> ipPrefixes = new ArrayList<>();
            for (String s : strings) {
                if (s.contains(":")) {
                    ipPrefixes.add(new IpPrefix(Ipv6Prefix.getDefaultInstance(s)));
                } else {
                    ipPrefixes.add(new IpPrefix(Ipv4Prefix.getDefaultInstance(s)));
                }
            }
            binding.setIpPrefix(ipPrefixes);
        }
        return binding;
    }

    private SxpNode getNode(String nodeId) {
        SxpNode node = new SxpNode();
        node.setNodeId(nodeId == null ? null : new NodeId(nodeId));
        node.setSecurity(new Security());
        node.getSecurity().setPassword("pass");
        node.setEnabled(true);
        node.setDescription("Desc");
        node.setTcpPort(new PortNumber(64999));
        node.setSourceIp(new IpAddress(new Ipv4Address("127.0.0.1")));
        node.setConnections(new org.opendaylight.controller.config.yang.sxp.controller.conf.Connections());
        node.getConnections().setConnection(new ArrayList<>());
        node.getConnections().getConnection().add(getConnection(Version.Version4, ConnectionMode.Speaker));
        node.getConnections().getConnection().add(getConnection(Version.Version4, ConnectionMode.Listener));
        node.setMasterDatabase(new org.opendaylight.controller.config.yang.sxp.controller.conf.MasterDatabase());
        node.getMasterDatabase().setBinding(new ArrayList<>());
        node.getMasterDatabase().getBinding().add(getBinding(150, "1.1.1.1/32", "2.2.2.2/32"));
        node.getMasterDatabase().getBinding().add(getBinding(1500, "10.10.10.10/32", "20.20.20.20/32"));
        node.setTimers(new Timers());
        return node;
    }

    @Test public void testLoad() throws Exception {
        configLoader.load(null);
        verify(access, never()).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.CONFIGURATION));

        SxpController controller = new SxpController();
        configLoader.load(controller);
        verify(access, never()).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.CONFIGURATION));

        controller.setSxpNode(new ArrayList<>());
        configLoader.load(controller);
        verify(access, never()).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.CONFIGURATION));

        controller.getSxpNode().add(getNode(null));
        controller.getSxpNode().add(getNode("0.0.0.0"));
        controller.getSxpNode().add(getNode("1.1.1.1"));
        configLoader.load(controller);
        verify(access, times(2)).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.CONFIGURATION));
    }

    @Test public void testParseMasterDatabase() throws Exception {
        MasterDatabase masterDatabase = ConfigLoader.parseMasterDatabase(null);
        assertNotNull(masterDatabase);
        assertNotNull(masterDatabase.getMasterDatabaseBinding());
        assertTrue(masterDatabase.getMasterDatabaseBinding().isEmpty());

        masterDatabase = ConfigLoader.parseMasterDatabase(new MasterDatabaseBuilder().build());
        assertNotNull(masterDatabase);
        assertNotNull(masterDatabase.getMasterDatabaseBinding());
        assertTrue(masterDatabase.getMasterDatabaseBinding().isEmpty());

        List<Binding> bindings = new ArrayList<>();
        List<IpPrefix> prefixList = new ArrayList<>();
        prefixList.add(new IpPrefix("5.5.5.5/32".toCharArray()));
        prefixList.add(new IpPrefix("50.50.50.50/32".toCharArray()));
        bindings.add(new BindingBuilder().setSgt(new Sgt(25)).setIpPrefix(prefixList).build());

        masterDatabase = ConfigLoader.parseMasterDatabase(new MasterDatabaseBuilder().setBinding(bindings).build());
        assertNotNull(masterDatabase);
        assertNotNull(masterDatabase.getMasterDatabaseBinding());
        assertEquals(2, masterDatabase.getMasterDatabaseBinding().size());
    }

    @Test public void testParseConnections() throws Exception {
        Connections connections = ConfigLoader.parseConnections(null);
        assertNotNull(connections);
        assertNotNull(connections.getConnection());
        assertTrue(connections.getConnection().isEmpty());

        connections = ConfigLoader.parseConnections(new ConnectionsBuilder().build());
        assertNotNull(connections);
        assertNotNull(connections.getConnection());
        assertTrue(connections.getConnection().isEmpty());

        List<Connection> connectionList = new ArrayList<>();
        connectionList.add(new ConnectionBuilder().build());
        connectionList.add(new ConnectionBuilder().build());
        connections = ConfigLoader.parseConnections(new ConnectionsBuilder().setConnection(connectionList).build());
        assertNotNull(connections);
        assertNotNull(connections.getConnection());
        assertEquals(2, connections.getConnection().size());
    }
}
