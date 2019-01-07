/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.netty.ImmediateCancelledFuture;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.Security;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SxpDatastoreNodeTest {
    private final static String ID = "127.0.0.1";

    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;
    @Mock
    private SxpNodeIdentity nodeIdentity;

    private SxpDatastoreNode node;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        DatastoreAccess datastoreAccess = prepareDataStore(dataBroker, readTransaction, writeTransaction);
        when(nodeIdentity.getVersion()).thenReturn(Version.Version4);
        Security security = mock(Security.class);
        when(security.getPassword()).thenReturn("default");
        when(nodeIdentity.getName()).thenReturn("NAME");
        when(nodeIdentity.getSecurity()).thenReturn(security);
        when(nodeIdentity.getMappingExpanded()).thenReturn(150);
        when(nodeIdentity.getTcpPort()).thenReturn(new PortNumber(64977));
        when(nodeIdentity.getSourceIp()).thenReturn(IpAddressBuilder.getDefaultInstance(ID));
        when(nodeIdentity.getTcpPort()).thenReturn(PortNumber.getDefaultInstance("64977"));
        when(nodeIdentity.getTimers()).thenReturn(new TimersBuilder().build());
        when(nodeIdentity.isEnabled()).thenReturn(Boolean.TRUE);
        node = SxpDatastoreNode.createInstance(NodeIdConv.createNodeId(ID), datastoreAccess, nodeIdentity);
        node.addDomain(new SxpDomainBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN).build());
    }

    @Test
    public void testGetIdentifier() throws Exception {
        assertEquals(SxpNodeIdentity.class, SxpDatastoreNode.getIdentifier(ID).getTargetType());
    }

    @Test
    public void testAddDomain() throws Exception {
        assertTrue(node.addDomain(new SxpDomainBuilder().setDomainName("private").build()));
        assertFalse(node.addDomain(new SxpDomainBuilder().setDomainName("private").build()));
        assertTrue(node.addDomain(new SxpDomainBuilder().setDomainName("test").build()));
    }

    @Test
    public void testAddConnection() throws Exception {
        SxpConnection
                sxpConnection =
                node.addConnection(new ConnectionBuilder().setPeerAddress(IpAddressBuilder.getDefaultInstance("1.1.1.1"))
                        .setTcpPort(new PortNumber(64977))
                        .setMode(ConnectionMode.Both)
                        .setVersion(Version.Version4)
                        .build(), SxpNode.DEFAULT_DOMAIN);
        assertTrue(sxpConnection instanceof SxpDatastoreConnection);
    }

    @Test
    public void testPutBindingsMasterDatabase() throws Exception {
        assertNotNull(node.putBindingsMasterDatabase(Collections.emptyList(), SxpNode.DEFAULT_DOMAIN));
    }

    @Test
    public void testRemoveBindingsMasterDatabase() throws Exception {
        assertNotNull(node.removeBindingsMasterDatabase(Collections.emptyList(), SxpNode.DEFAULT_DOMAIN));
    }

    @Test
    public void testGetDatastoreAccess() throws Exception {
        assertNotNull(node.getDatastoreAccess());
    }

    @Test
    public void testShutdown() throws Exception {
        node.start().get();
        assertTrue(node.shutdown().get());
    }

    /**
     * Test that it is possible to successfully start/stop the same node more than once.
     */
    @Test
    public void testDoubleShutdown() throws Exception {
        node.start().get();
        assertTrue(node.shutdown().get());
        node.start().get();
        assertTrue(node.shutdown().get());
    }

    @Test
    public void testClose() throws Exception {
        final SxpConnection sxpConnection = mock(SxpConnection.class);
        when(sxpConnection.getDomainName()).thenReturn(SxpNode.DEFAULT_DOMAIN);
        when(sxpConnection.getDestination()).thenReturn(InetSocketAddress.createUnresolved("127.0.0.2", 64977));
        when(sxpConnection.openConnection()).thenReturn(new ImmediateCancelledFuture<>());

        node.start().get();
        node.addConnection(sxpConnection);
        verify(sxpConnection).openConnection();
        node.close();
        verify(sxpConnection).shutdown();
    }

    /**
     * Prepare {@link DatastoreAccess} mock instance backed by {@link DataBroker} for tests.
     * <p>
     * {@link ReadTransaction} and {@link WriteTransaction} are assumed to be created by {@link TransactionChain}.
     * <p>
     * {@link ReadTransaction} reads a mock instance of {@link DataObject} on any read.
     * {@link WriteTransaction} is committed successfully.
     *
     * @param dataBroker mock of {@link DataBroker}
     * @param readTransaction mock of {@link ReadTransaction}
     * @param writeTransaction mock of {@link WriteTransaction}
     * @return mock of {@link DatastoreAccess}
     */
    private static DatastoreAccess prepareDataStore(DataBroker dataBroker, ReadTransaction readTransaction,
            WriteTransaction writeTransaction) {
        TransactionChain transactionChain = mock(TransactionChain.class);
        doReturn(CommitInfo.emptyFluentFuture())
                .when(writeTransaction).commit();
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(mock(DataObject.class))));
        when(transactionChain.newReadOnlyTransaction())
                .thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction())
                .thenReturn(writeTransaction);
        when(dataBroker.createTransactionChain(any(TransactionChainListener.class)))
                .thenReturn(transactionChain);

        return DatastoreAccess.getInstance(dataBroker);
    }
}
