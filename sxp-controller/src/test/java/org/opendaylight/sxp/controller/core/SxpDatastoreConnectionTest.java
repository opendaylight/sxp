/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.BindingTransactionChain;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionChainListener;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.capabilities.fields.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SxpDatastoreConnectionTest {

    private SxpDatastoreConnection connection;

    @Mock
    private static ThreadsWorker worker;
    @Mock
    private SxpNode sxpNode;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(worker.scheduleTask(any(Callable.class), anyInt(), any(TimeUnit.class))).thenReturn(
                mock(ListenableScheduledFuture.class));
        when(worker.executeTask(any(Runnable.class), any(ThreadsWorker.WorkerType.class))).thenReturn(
                mock(ListenableFuture.class));
        when(worker.executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class))).thenReturn(
                mock(ListenableFuture.class));
        when(sxpNode.getWorker()).thenReturn(worker);
        DatastoreAccess datastoreAccess = prepareDataStore(dataBroker, readTransaction, writeTransaction);
        connection =
                SxpDatastoreConnection.create(datastoreAccess, sxpNode,
                        new ConnectionBuilder().setPeerAddress(IpAddressBuilder.getDefaultInstance("127.0.0.1"))
                                .setTcpPort(new PortNumber(64999))
                                .setMode(ConnectionMode.None)
                                .setState(ConnectionState.On)
                                .setVersion(Version.Version2)
                                .build(), "test");
    }

    @Test
    public void testSetTimers() throws Exception {
        ConnectionTimers
                timers =
                new ConnectionTimersBuilder().setHoldTime(60).setHoldTimeMax(180).setHoldTimeMinAcceptable(90).build();
        connection.setTimers(timers);
        assertEquals(60, connection.getHoldTime());
        assertEquals(180, connection.getHoldTimeMax());
        assertEquals(90, connection.getHoldTimeMinAcceptable());
        verify(writeTransaction, atLeastOnce()).put(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), eq(timers));
    }

    @Test
    public void testSetCapabilities() throws Exception {
        Capabilities capabilities = Configuration.getCapabilities(Version.Version4);
        connection.setCapabilities(capabilities);
        assertFalse(connection.getCapabilitiesRemote().contains(CapabilityType.Ipv4Unicast));
        assertFalse(connection.getCapabilitiesRemote().contains(CapabilityType.Ipv6Unicast));
        assertFalse(connection.getCapabilitiesRemote().contains(CapabilityType.LoopDetection));
        assertFalse(connection.getCapabilitiesRemote().contains(CapabilityType.SubnetBindings));
        assertFalse(connection.getCapabilitiesRemote().contains(CapabilityType.SxpCapabilityExchange));
        verify(writeTransaction, atLeastOnce()).put(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), eq(capabilities));
    }

    @Test
    public void testSetVersion() throws Exception {
        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        connection.setVersion(Version.Version1);
        assertEquals(Version.Version1, connection.getVersion());
        verify(writeTransaction, atLeastOnce()).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                captor.capture());
        assertEquals(Version.Version1, captor.getValue().getVersion());
    }

    @Test
    public void testSetState() throws Exception {
        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        connection.setState(ConnectionState.DeleteHoldDown);
        assertEquals(ConnectionState.DeleteHoldDown, connection.getState());
        verify(writeTransaction, atLeastOnce()).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                captor.capture());
        assertEquals(ConnectionState.DeleteHoldDown, captor.getValue().getState());
    }

    @Test
    public void testSetNodeIdRemote() throws Exception {
        NodeId nodeId = new NodeId("1.1.1.1");
        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        connection.setNodeIdRemote(nodeId);
        assertEquals(nodeId, connection.getNodeIdRemote());
        verify(writeTransaction, atLeastOnce()).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                captor.capture());
        assertEquals(nodeId, captor.getValue().getNodeId());
    }

    @Test
    public void testSetUpdateOrKeepaliveMessageTimestamp() throws Exception {
        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        connection.setUpdateOrKeepaliveMessageTimestamp();
        verify(writeTransaction, atLeastOnce()).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                captor.capture());
        assertEquals(connection.getTimestampUpdateOrKeepAliveMessage(),
                TimeConv.toLong(captor.getValue().getTimestampUpdateOrKeepAliveMessage()));
    }

    @Test
    public void testShutdown() throws Exception {
        connection.shutdown();
        verify(readTransaction, atLeastOnce()).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    /**
     * Prepare {@link DatastoreAccess} mock instance backed by {@link DataBroker} for tests.
     * <p>
     * {@link ReadTransaction} and {@link WriteTransaction} are assumed to be created by
     * {@link DatastoreAccess} {@link BindingTransactionChain}.
     * <p>
     * {@link ReadTransaction} reads an mock instance of {@link DataObject} on any read.
     * {@link WriteTransaction} is committed successfully.
     *
     * @param dataBroker mock of {@link DataBroker}
     * @param readTransaction mock of {@link ReadTransaction}
     * @param writeTransaction mock of {@link WriteTransaction}
     * @return mock of {@link DatastoreAccess}
     */
    private static DatastoreAccess prepareDataStore(DataBroker dataBroker, ReadTransaction readTransaction,
            WriteTransaction writeTransaction) {
        BindingTransactionChain transactionChain = mock(BindingTransactionChain.class);
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
