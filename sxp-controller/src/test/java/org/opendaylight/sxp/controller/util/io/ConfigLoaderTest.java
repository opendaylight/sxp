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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ConfigLoaderTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    private DatastoreAccess access;

    @Before
    public void init() throws Exception {
        access = mock(DatastoreAccess.class);
        when(
                access.put(any(InstanceIdentifier.class), any(SxpNodeIdentity.class), any(LogicalDatastoreType.class)))
                .thenReturn(CommitInfo.emptyFluentFuture());
        when(access.checkAndPut(any(InstanceIdentifier.class), any(SxpNodeIdentity.class),
                any(LogicalDatastoreType.class), anyBoolean())).thenReturn(true);
    }

    @Test
    public void testInitTopologyNode() throws Exception {
        assertTrue(ConfigLoader.initTopologyNode("0.0.0.0", LogicalDatastoreType.OPERATIONAL, access));
        assertTrue(ConfigLoader.initTopologyNode("0.0.0.0", LogicalDatastoreType.CONFIGURATION, access));
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
