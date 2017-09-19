/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.controller.util.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.AbstractCheckedFuture;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatastoreAccess.class, org.opendaylight.sxp.core.SxpNode.class,
        org.opendaylight.sxp.util.inet.Search.class, Configuration.class})
public class ConfigLoaderTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    private DatastoreAccess access;

    @Before
    public void init() throws Exception {
        access = PowerMockito.mock(DatastoreAccess.class);
        PowerMockito.when(
                access.put(any(InstanceIdentifier.class), any(SxpNodeIdentity.class), any(LogicalDatastoreType.class)))
                .thenReturn(mock(AbstractCheckedFuture.class));
        PowerMockito.when(access.checkAndPut(any(InstanceIdentifier.class), any(SxpNodeIdentity.class),
                any(LogicalDatastoreType.class), anyBoolean())).thenReturn(true);
        PowerMockito.mockStatic(org.opendaylight.sxp.core.SxpNode.class, RETURNS_MOCKS);
        PowerMockito.mockStatic(org.opendaylight.sxp.util.inet.Search.class);
    }

    @Test
    public void testInitTopologyNode() throws Exception {
        assertTrue(ConfigLoader.initTopologyNode("0.0.0.0", LogicalDatastoreType.OPERATIONAL, access));
        assertTrue(ConfigLoader.initTopologyNode("0.0.0.0", LogicalDatastoreType.CONFIGURATION, access));
    }

    @Test
    public void testParseMasterDatabase() throws Exception {
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

    @Test
    public void testParseConnections() throws Exception {
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
