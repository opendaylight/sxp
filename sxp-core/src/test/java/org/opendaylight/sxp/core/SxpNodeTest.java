/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import io.netty.channel.Channel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opendaylight.sxp.core.service.BindingHandler;
import org.opendaylight.sxp.core.service.ConnectFacade;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseProvider;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseProvider;
import org.opendaylight.sxp.util.exception.node.DatabaseNotFoundException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpConnectionException;
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.network.topology.topology.node.Timers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.node.fields.Security;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class) @PrepareForTest({ConnectFacade.class, BindingHandler.class}) public class SxpNodeTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static SxpNode node;
        private static SxpNodeIdentity nodeIdentity;
        private static MasterDatabaseProvider databaseProvider;
        private static SxpDatabaseProvider sxpDatabaseProvider;
        private static ThreadsWorker worker;
        private static Timers timers;
        private static int ip4Adrres = 0;

        @Before public void init() throws Exception {
                worker = mock(ThreadsWorker.class);
                when(worker.scheduleTask(any(Callable.class), anyInt(), any(TimeUnit.class))).thenReturn(
                        mock(ListenableScheduledFuture.class));
                when(worker.executeTask(any(Runnable.class), any(ThreadsWorker.WorkerType.class))).thenReturn(
                        mock(ListenableFuture.class));
                when(worker.executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class))).thenReturn(
                        mock(ListenableFuture.class));
                timers = mock(Timers.class);
                nodeIdentity = mock(SxpNodeIdentity.class);
                when(nodeIdentity.getTimers()).thenReturn(timers);
                when(nodeIdentity.getVersion()).thenReturn(Version.Version4);
                Security security = mock(Security.class);
                when(security.getPassword()).thenReturn("default");
                when(nodeIdentity.getSecurity()).thenReturn(security);

                databaseProvider = mock(MasterDatabaseProvider.class);
                sxpDatabaseProvider = mock(SxpDatabaseProvider.class);
                node =
                        SxpNode.createInstance(NodeIdConv.createNodeId("127.0.0.1"), nodeIdentity, databaseProvider,
                                sxpDatabaseProvider, worker);
        }

        private Connection mockConnection(ConnectionMode mode, ConnectionState state) {
                Connection connection = mock(Connection.class);
                when(connection.getMode()).thenReturn(mode);
                when(connection.getPeerAddress()).thenReturn(
                        Ipv4Address.getDefaultInstance("127.0.0." + (++ip4Adrres)));
                when(connection.getState()).thenReturn(state);
                return connection;
        }

        @Test public void testGetAllDeleteHoldDownConnections() throws Exception {
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.DeleteHoldDown));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.On));
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.Off));
                assertEquals(2, node.getAllDeleteHoldDownConnections().size());
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.PendingOn));
                assertEquals(3, node.getAllDeleteHoldDownConnections().size());
        }

        @Test public void testGetAllOffConnections() throws Exception {
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.Off));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.PendingOn));
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.Off));
                assertEquals(2, node.getAllOffConnections().size());
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.Off));
                assertEquals(3, node.getAllOffConnections().size());
        }

        @Test public void testGetAllOnConnections() throws Exception {
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.DeleteHoldDown));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.PendingOn));
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.Off));
                assertEquals(1, node.getAllOnConnections().size());
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
                assertEquals(1, node.getAllOnConnections().size());
        }

        @Test public void testGetAllOnListenerConnections() throws Exception {
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.PendingOn));
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.Off));
                assertEquals(1, node.getAllOnListenerConnections().size());
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.On));
                assertEquals(1, node.getAllOnListenerConnections().size());
        }

        @Test public void testGetAllOnSpeakerConnections() throws Exception {
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.DeleteHoldDown));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.PendingOn));
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.Off));
                assertEquals(0, node.getAllOnSpeakerConnections().size());
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.On));
                assertEquals(1, node.getAllOnSpeakerConnections().size());
        }

        private InetSocketAddress getInetSocketAddress(String s) throws UnknownHostException {
                InetAddress inetAddress = InetAddress.getByName(s);
                InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 64999);
                return inetSocketAddress;
        }

        @Test public void testGetByAddress() throws Exception {
                Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);
                Connection connection1 = mockConnection(ConnectionMode.Listener, ConnectionState.On);

                node.addConnection(connection);
                assertNotNull(node.getByAddress(getInetSocketAddress(connection.getPeerAddress().getValue())));
                assertNull(node.getByAddress(getInetSocketAddress(connection1.getPeerAddress().getValue())));

                node.addConnection(connection1);
                assertNotNull(node.getByAddress(getInetSocketAddress(connection.getPeerAddress().getValue())));
                assertNotNull(node.getByAddress(getInetSocketAddress(connection1.getPeerAddress().getValue())));
        }

        @Test public void testGetByPort() throws Exception {
                Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);

                node.addConnection(connection);
                assertNotNull(node.getByPort(64999));
                assertNull(node.getByPort(64950));
        }

        @Test public void testOpenConnections() throws Exception {
                ArgumentCaptor<Runnable> argument = ArgumentCaptor.forClass(Runnable.class);
                node.setServerChannel(mock(Channel.class));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.DeleteHoldDown));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.PendingOn));
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
                node.openConnections();
                verify(worker, times(1)).executeTask(any(Runnable.class), any(ThreadsWorker.WorkerType.class));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.Off));
                node.openConnections();
                verify(worker, times(2)).executeTask(argument.capture(), any(ThreadsWorker.WorkerType.class));

                PowerMockito.mockStatic(ConnectFacade.class);
                argument.getValue().run();
                PowerMockito.verifyStatic();
        }

        @Test public void testSetPassword() throws Exception {
                Security security = mock(Security.class);
                when(security.getPassword()).thenReturn("");

                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.On));

                assertEquals("", node.setPassword(security).getPassword());
                when(security.getPassword()).thenReturn("cisco");
                assertEquals(2, node.getAllOnConnections().size());
                assertEquals("cisco", node.setPassword(security).getPassword());

                when(security.getPassword()).thenReturn("cisco123");
                assertEquals("07982c55db2b9985d3391f02e639db9c", node.setPassword(security).getMd5Digest());
                assertEquals(2, node.getAllOffConnections().size());
        }

        @Test public void testSetTimer() throws Exception {
                exception.expect(UnknownTimerTypeException.class);
                node.setTimer(TimerType.DeleteHoldDownTimer, 50);
                exception.expect(UnknownTimerTypeException.class);
                node.setTimer(TimerType.ReconciliationTimer, 50);

                node.setTimer(TimerType.RetryOpenTimer, 0);
                assertNull(node.getTimer(TimerType.RetryOpenTimer));

                node.setTimer(TimerType.RetryOpenTimer, 50);
                assertNotNull(node.getTimer(TimerType.RetryOpenTimer));
        }

        @Test public void testShutdown() throws Exception {
                node.shutdown();
                assertNull(node.getTimer(TimerType.RetryOpenTimer));
                assertFalse(node.isEnabled());
        }

        @Test public void testShutdownConnections() throws Exception {
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
                node.shutdownConnections();

                verify(sxpDatabaseProvider).purgeBindings((NodeId) any());

                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.On));
                node.shutdownConnections();

                verify(sxpDatabaseProvider).purgeBindings((NodeId) any());

                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
                node.shutdownConnections();

                verify(sxpDatabaseProvider, times(2)).purgeBindings((NodeId) any());
        }

        @Test public void testStart() throws Exception {
                PowerMockito.mockStatic(ConnectFacade.class);
                ArgumentCaptor<Runnable> argument = ArgumentCaptor.forClass(Runnable.class);
                when(nodeIdentity.getMasterDatabase()).thenReturn(null);
                when(timers.getRetryOpenTime()).thenReturn(50);

                node.start();
                verify(worker).executeTask(argument.capture(), (ThreadsWorker.WorkerType) anyObject());
                assertEquals(50, node.getRetryOpenTime());
                assertNotNull(node.getTimer(TimerType.RetryOpenTimer));
                assertTrue(node.isEnabled());

                argument.getValue().run();
                PowerMockito.verifyStatic();
        }

        @Test public void testGetBindingMasterDatabase() throws Exception {
                assertNotNull(node.getBindingMasterDatabase());

                node = SxpNode.createInstance(NodeId.getDefaultInstance("0.0.0.0"), nodeIdentity, null, null);
                exception.expect(DatabaseNotFoundException.class);
                node.getBindingMasterDatabase();
        }

        @Test public void testGetBindingSxpDatabase() throws Exception {
                assertNotNull(node.getBindingSxpDatabase());

                node = SxpNode.createInstance(NodeId.getDefaultInstance("0.0.0.0"), nodeIdentity, null, null);
                exception.expect(DatabaseNotFoundException.class);
                node.getBindingSxpDatabase();
        }

        @Test public void testPutLocalBindingsMasterDatabase() throws Exception {
                MasterDatabase masterDatabase = mock(MasterDatabase.class);
                List<Source> sourceList = new ArrayList<>();
                Source source = mock(Source.class);
                when(source.getBindingSource()).thenReturn(DatabaseBindingSource.Local);
                sourceList.add(source);
                List<PrefixGroup> prefixGroups = new ArrayList<>();
                prefixGroups.add(mock(PrefixGroup.class));
                when(source.getPrefixGroup()).thenReturn(prefixGroups);
                when(masterDatabase.getSource()).thenReturn(sourceList);

                node.putLocalBindingsMasterDatabase(masterDatabase);
                verify(databaseProvider).addBindingsLocal(anyList());

                when(source.getBindingSource()).thenReturn(DatabaseBindingSource.Sxp);
                node.putLocalBindingsMasterDatabase(masterDatabase);
                verify(databaseProvider).addBindingsLocal(anyList());
        }

        @Test public void testRemoveConnection() throws Exception {
                Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);
                node.addConnection(connection);
                SxpConnection
                        sxpConnection =
                        node.removeConnection(getInetSocketAddress(connection.getPeerAddress().getValue()));
                assertEquals(ConnectionState.Off, sxpConnection.getState());
        }

        @Test public void testGetConnection() throws Exception {
                Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);
                node.addConnection(connection);

                SxpConnection
                        sxpConnection =
                        node.getConnection(getInetSocketAddress(connection.getPeerAddress().getValue()));
                assertNotNull(connection);
                assertEquals(connection.getMode(), sxpConnection.getMode());
                assertEquals(connection.getState(), sxpConnection.getState());

                exception.expect(UnknownSxpConnectionException.class);
                node.getConnection(getInetSocketAddress("0.9.9.9"));
        }

        @Test public void testProcessUpdateMessage() throws Exception {
                PowerMockito.mockStatic(BindingHandler.class);
                node.processUpdateMessage(mock(UpdateMessage.class), mock(SxpConnection.class));
                PowerMockito.verifyStatic();
        }

        @Test public void testProcessUpdateMessageLegacy() throws Exception {
                PowerMockito.mockStatic(BindingHandler.class);
                node.processUpdateMessage(mock(UpdateMessageLegacy.class), mock(SxpConnection.class));
                PowerMockito.verifyStatic();
        }

        @Test public void testAddConnection() throws Exception {
                PowerMockito.mockStatic(ConnectFacade.class);
                node.addConnection(null);
                assertEquals(0, node.size());

                node.addConnection(mockConnection(ConnectionMode.Both, ConnectionState.On));
                assertEquals(1, node.size());
                PowerMockito.verifyStatic();
        }

        @Test public void testAddConnections() throws Exception {
                PowerMockito.mockStatic(ConnectFacade.class);
                List<Connection> connection = new ArrayList<>();

                node.addConnections(null);
                assertEquals(0, node.size());

                Connections connections = mock(Connections.class);
                when(connections.getConnection()).thenReturn(connection);
                node.addConnections(connections);
                assertEquals(0, node.size());

                connection.add(mockConnection(ConnectionMode.Both, ConnectionState.On));
                node.addConnections(connections);
                assertEquals(1, node.size());
                PowerMockito.verifyStatic();
        }
}
