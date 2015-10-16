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
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpConnectionException;
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpPeers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpPeersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.sxp.peers.SxpPeer;
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
                timers = Configuration.getNodeTimers();
                nodeIdentity = mock(SxpNodeIdentity.class);
                when(nodeIdentity.getTimers()).thenReturn(timers);
                when(nodeIdentity.getVersion()).thenReturn(Version.Version4);
                Security security = mock(Security.class);
                when(security.getPassword()).thenReturn("default");
                when(nodeIdentity.getName()).thenReturn("NAME");
                when(nodeIdentity.getSecurity()).thenReturn(security);
                when(nodeIdentity.getMappingExpanded()).thenReturn(150);

                databaseProvider = mock(MasterDatabaseProvider.class);
                sxpDatabaseProvider = mock(SxpDatabaseProvider.class);
                node =
                        SxpNode.createInstance(NodeIdConv.createNodeId("127.0.0.1"), nodeIdentity, databaseProvider,
                                sxpDatabaseProvider, worker);
        }

        private Connection mockConnection(ConnectionMode mode, ConnectionState state) {
                Connection connection = mock(Connection.class);
                when(connection.getMode()).thenReturn(mode);
                when(connection.getPeerAddress()).thenReturn(new IpAddress(("127.0.0." + (++ip4Adrres)).toCharArray()));
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

        private InetSocketAddress getInetSocketAddress(IpAddress s) throws UnknownHostException {
                InetAddress
                        inetAddress =
                        InetAddress.getByName(
                                s.getIpv4Address() != null ? s.getIpv4Address().getValue() : s.getIpv6Address()
                                        .getValue());
                InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 64999);
                return inetSocketAddress;
        }

        @Test public void testSxpNodeGetters() {
                assertEquals((long) timers.getRetryOpenTime(), node.getRetryOpenTime());
                assertEquals(150l, node.getExpansionQuantity());
                assertEquals((long) timers.getListenerProfile().getHoldTime(), node.getHoldTime());
                assertEquals((long) timers.getListenerProfile().getHoldTimeMax(), node.getHoldTimeMax());
                assertEquals((long) timers.getListenerProfile().getHoldTimeMin(), node.getHoldTimeMin());
                assertEquals((long) timers.getSpeakerProfile().getHoldTimeMinAcceptable(),
                        node.getHoldTimeMinAcceptable());
                assertEquals((long) timers.getSpeakerProfile().getKeepAliveTime(), node.getKeepAliveTime());
                assertEquals("NAME", node.getName());
                assertEquals(NodeId.getDefaultInstance("127.0.0.1"), node.getNodeId());
                assertEquals("default", node.getPassword());

        }

        @Test public void testGetByAddress() throws Exception {
                Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);
                Connection connection1 = mockConnection(ConnectionMode.Listener, ConnectionState.On);

                node.addConnection(connection);
                assertNotNull(node.getByAddress(getInetSocketAddress(connection.getPeerAddress())));
                assertNull(node.getByAddress(getInetSocketAddress(connection1.getPeerAddress())));

                node.addConnection(connection1);
                assertNotNull(node.getByAddress(getInetSocketAddress(connection.getPeerAddress())));
                assertNotNull(node.getByAddress(getInetSocketAddress(connection1.getPeerAddress())));
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
                node.setTimer(TimerType.RetryOpenTimer, 0);
                assertNull(node.getTimer(TimerType.RetryOpenTimer));

                node.setTimer(TimerType.RetryOpenTimer, 50);
                assertNotNull(node.getTimer(TimerType.RetryOpenTimer));

                exception.expect(UnknownTimerTypeException.class);
                node.setTimer(TimerType.ReconciliationTimer, 50);
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
                when(nodeIdentity.getMasterDatabase()).thenReturn(null);

                node.start();
                verify(worker).executeTask(any(Runnable.class), any(ThreadsWorker.WorkerType.class));

                node.start();
                verify(worker).executeTask(any(Runnable.class), any(ThreadsWorker.WorkerType.class));
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
                verify(databaseProvider).addBindingsLocal(any(SxpNode.class), anyList());

                when(source.getBindingSource()).thenReturn(DatabaseBindingSource.Sxp);
                node.putLocalBindingsMasterDatabase(masterDatabase);
                verify(databaseProvider).addBindingsLocal(any(SxpNode.class), anyList());
        }

        @Test public void testRemoveConnection() throws Exception {
                Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);
                node.addConnection(connection);
                SxpConnection sxpConnection = node.removeConnection(getInetSocketAddress(connection.getPeerAddress()));
                assertEquals(ConnectionState.Off, sxpConnection.getState());
        }

        @Test public void testGetConnection() throws Exception {
                Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);
                node.addConnection(connection);

                SxpConnection sxpConnection = node.getConnection(getInetSocketAddress(connection.getPeerAddress()));
                assertNotNull(connection);
                assertEquals(connection.getMode(), sxpConnection.getMode());
                assertEquals(connection.getState(), sxpConnection.getState());

                exception.expect(UnknownSxpConnectionException.class);
                node.getConnection(getInetSocketAddress(new IpAddress("0.9.9.9".toCharArray())));
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
                assertEquals(0, node.getAllConnections().size());

                node.addConnection(mockConnection(ConnectionMode.Both, ConnectionState.On));
                assertEquals(1, node.getAllConnections().size());
                PowerMockito.verifyStatic();
        }

        @Test public void testAddConnections() throws Exception {
                PowerMockito.mockStatic(ConnectFacade.class);
                List<Connection> connection = new ArrayList<>();

                node.addConnections(null);
                assertEquals(0, node.getAllConnections().size());

                Connections connections = mock(Connections.class);
                when(connections.getConnection()).thenReturn(connection);
                node.addConnections(connections);
                assertEquals(0, node.getAllConnections().size());

                connection.add(mockConnection(ConnectionMode.Both, ConnectionState.On));
                node.addConnections(connections);
                assertEquals(1, node.getAllConnections().size());
                PowerMockito.verifyStatic();
        }

        private ArrayList<SxpFilter> getFilters(FilterType... types) {
                ArrayList<SxpFilter> sxpFilters = new ArrayList<>();
                for (FilterType filterType : types) {
                        sxpFilters.add(getFilter(filterType));
                }
                return sxpFilters;
        }

        private SxpFilter getFilter(FilterType type) {
                SxpFilterBuilder builder = new SxpFilterBuilder();
                builder.setFilterType(type);
                return builder.build();
        }

        private SxpPeerGroup getGroup(String name, ArrayList<SxpFilter> filters, ArrayList<SxpPeer> sxpPeers) {
                SxpPeerGroupBuilder builder = new SxpPeerGroupBuilder();
                builder.setName(name);
                builder.setSxpFilter(filters);
                SxpPeersBuilder sxpPeersBuilder = new SxpPeersBuilder();
                sxpPeersBuilder.setSxpPeer(sxpPeers);
                builder.setSxpPeers(sxpPeersBuilder.build());
                return builder.build();
        }

        @Test public void testAddPeerGroup() throws Exception {
                assertFalse(node.addPeerGroup(null));
                assertTrue(node.getPeerGroups().isEmpty());
                node.addPeerGroup(getGroup("TEST", null, null));
                assertEquals(1, node.getPeerGroups().size());
                node.addPeerGroup(getGroup("TEST1", null, null));
                assertEquals(2, node.getPeerGroups().size());
                node.addPeerGroup(getGroup("TEST", null, null));
                assertEquals(2, node.getPeerGroups().size());

                node.addPeerGroup(getGroup("TEST2", getFilters(FilterType.Inbound), null));
                assertEquals(3, node.getPeerGroups().size());
                node.addPeerGroup(getGroup("TEST3", getFilters(FilterType.Inbound, FilterType.Inbound), null));
                assertEquals(3, node.getPeerGroups().size());
                node.addPeerGroup(getGroup("TEST3", getFilters(FilterType.Inbound, FilterType.Outbound), null));
                exception.expect(IllegalArgumentException.class);
        }

        @Test public void testGetPeerGroup() throws Exception {

        }

        @Test public void testRemovePeerGroup() throws Exception {

        }

        @Test public void testGetPeerGroups() throws Exception {

        }

        @Test public void testAddFilterToPeerGroup() throws Exception {

        }

        @Test public void testRemoveFilterFromPeerGroup() throws Exception {

        }
}
