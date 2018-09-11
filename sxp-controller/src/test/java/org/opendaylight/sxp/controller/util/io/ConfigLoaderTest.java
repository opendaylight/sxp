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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.mdsal.binding.api.BindingTransactionChain;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionChainListener;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ConfigLoaderTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    private DatastoreAccess access;

    @Before
    public void init() throws Exception {
        DataBroker dataBroker = mock(DataBroker.class);
        BindingTransactionChain transactionChain = mock(BindingTransactionChain.class);
        ReadTransaction readTransaction = mock(ReadTransaction.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.empty()));
        when(readTransaction.read(any(LogicalDatastoreType.class), eq(NodeIdentityListener.SUBSCRIBED_PATH)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(mock(Topology.class))));
        when(transactionChain.newReadOnlyTransaction())
                .thenReturn(readTransaction);
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        doReturn(CommitInfo.emptyFluentFuture()).when(writeTransaction).commit();
        when(transactionChain.newWriteOnlyTransaction())
                .thenReturn(writeTransaction);
        when(dataBroker.createTransactionChain(any(TransactionChainListener.class)))
                .thenReturn(transactionChain);
        access = DatastoreAccess.getInstance(dataBroker);
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
