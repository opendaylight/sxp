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
import io.netty.channel.ChannelFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opendaylight.sxp.core.service.BindingHandler;
import org.opendaylight.sxp.core.service.ConnectFacade;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.acl.filter.entries.AclEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.acl.filter.entries.AclEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtMatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpPeersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.sxp.peers.SxpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.sxp.peers.SxpPeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.Security;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.Timers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
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

@RunWith(PowerMockRunner.class) @PrepareForTest({ConnectFacade.class, BindingHandler.class})
public class SxpNodeTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static SxpNode node;
        private static SxpNodeIdentity nodeIdentity;
        private static MasterDatabaseInf databaseProvider;
        private static SxpDatabaseInf sxpDatabaseProvider;
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
                timers = getNodeTimers();
                nodeIdentity = mock(SxpNodeIdentity.class);
                when(nodeIdentity.getTimers()).thenReturn(timers);
                when(nodeIdentity.getVersion()).thenReturn(Version.Version4);
                Security security = mock(Security.class);
                when(security.getPassword()).thenReturn("default");
                when(nodeIdentity.getName()).thenReturn("NAME");
                when(nodeIdentity.getSecurity()).thenReturn(security);
                when(nodeIdentity.getMappingExpanded()).thenReturn(150);
                when(nodeIdentity.getTcpPort()).thenReturn(new PortNumber(64999));
                when(nodeIdentity.getSourceIp()).thenReturn(new IpAddress("127.1.1.1".toCharArray()));
                when(nodeIdentity.getTcpPort()).thenReturn(PortNumber.getDefaultInstance("64999"));

                databaseProvider = mock(MasterDatabaseInf.class);
                sxpDatabaseProvider = mock(SxpDatabaseInf.class);
                node =
                        SxpNode.createInstance(NodeIdConv.createNodeId("127.0.0.1"), nodeIdentity, databaseProvider,
                                sxpDatabaseProvider, worker);
                PowerMockito.field(SxpNode.class, "serverChannel").set(node, mock(Channel.class));
        }

        private Timers getNodeTimers() {
                TimersBuilder timersBuilder = new TimersBuilder();
                timersBuilder.setRetryOpenTime(5);

                timersBuilder.setHoldTimeMinAcceptable(45);
                timersBuilder.setKeepAliveTime(30);

                timersBuilder.setHoldTime(90);
                timersBuilder.setHoldTimeMin(90);
                timersBuilder.setHoldTimeMax(180);

                return timersBuilder.build();
        }

        private Connection mockConnection(ConnectionMode mode, ConnectionState state) {
                Connection connection = mock(Connection.class);
                when(connection.getMode()).thenReturn(mode);
                when(connection.getPeerAddress()).thenReturn(new IpAddress(("127.0.0." + (++ip4Adrres)).toCharArray()));
                when(connection.getState()).thenReturn(state);
                when(connection.getPassword()).thenReturn("Default");
                when(connection.getVersion()).thenReturn(Version.Version4);
                return connection;
        }

        @Test public void testCreateInstance() throws Exception {
                when(nodeIdentity.getSourceIp()).thenReturn(new IpAddress(Ipv4Address.getDefaultInstance("0.0.0.0")));
                assertNotNull(
                        SxpNode.createInstance(NodeIdConv.createNodeId("127.0.0.1"), nodeIdentity, databaseProvider,
                                sxpDatabaseProvider, worker));
                assertNotNull(
                        SxpNode.createInstance(NodeIdConv.createNodeId("127.0.0.1"), nodeIdentity, databaseProvider,
                                sxpDatabaseProvider));
                assertNotNull(SxpNode.createInstance(NodeIdConv.createNodeId("127.0.0.1"), nodeIdentity));
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

        @Test public void testSxpNodeGetters() throws Exception {
                assertEquals((long) timers.getRetryOpenTime(), node.getRetryOpenTime());
                assertEquals(150l, node.getExpansionQuantity());
                assertEquals((long) timers.getHoldTime(), node.getHoldTime());
                assertEquals((long) timers.getHoldTimeMax(), node.getHoldTimeMax());
                assertEquals((long) timers.getHoldTimeMin(), node.getHoldTimeMin());
                assertEquals((long) timers.getHoldTimeMinAcceptable(),
                        node.getHoldTimeMinAcceptable());
                assertEquals((long) timers.getKeepAliveTime(), node.getKeepAliveTime());
                assertEquals("NAME", node.getName());
                assertEquals(NodeId.getDefaultInstance("127.0.0.1"), node.getNodeId());
                assertEquals("default", node.getPassword());
                assertEquals(64999, node.getServerPort());

                SxpNode
                        node =
                        SxpNode.createInstance(NodeIdConv.createNodeId("127.0.0.1"), mock(SxpNodeIdentity.class),
                                databaseProvider, sxpDatabaseProvider, worker);

                assertEquals(Version.Version4, node.getVersion());
                assertEquals(0, node.getExpansionQuantity());
                assertEquals(0, node.getRetryOpenTime());
                assertEquals(0, node.getHoldTime());
                assertEquals(0, node.getHoldTimeMax());
                assertEquals(0, node.getHoldTimeMin());
                assertEquals(0, node.getHoldTimeMinAcceptable());
                assertEquals(0, node.getKeepAliveTime());
                assertEquals(-1, node.getServerPort());
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
                when(worker.executeTaskInSequence(any(Callable.class), any(ThreadsWorker.WorkerType.class),
                        any(SxpConnection.class))).thenReturn(mock(ListenableFuture.class))
                        .thenReturn(mock(ListenableFuture.class))
                        .thenReturn(mock(ListenableFuture.class));
                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
                node.shutdownConnections();
                assertEquals(1, node.getAllOffConnections().size());

                node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.On));
                node.shutdownConnections();
                assertEquals(2, node.getAllOffConnections().size());

                node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
                node.shutdownConnections();
                assertEquals(3, node.getAllOffConnections().size());
        }

        @Test public void testStart() throws Exception {
                node.start();
                assertTrue(node.isEnabled());
                node.shutdown();
                assertFalse(node.isEnabled());
        }

        @Test public void testPutLocalBindingsMasterDatabase() throws Exception {
        }

        @Test public void testRemoveLocalBindingsMasterDatabase() throws Exception {
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

                assertNull(node.getConnection(getInetSocketAddress(new IpAddress("0.9.9.9".toCharArray()))));
        }

        @Test public void testAddConnection() throws Exception {
                PowerMockito.mockStatic(ConnectFacade.class);
                assertEquals(0, node.getAllConnections().size());

                List<SxpPeer> sxpPeers = new ArrayList<>();
                Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);
                sxpPeers.add(new SxpPeerBuilder().setPeerAddress(connection.getPeerAddress()).build());
                node.addPeerGroup(getGroup("TEST", null, sxpPeers));
                node.addFilterToPeerGroup("TEST", getFilter(FilterType.Inbound));
                node.addConnection(connection);
                assertEquals(1, node.getAllConnections().size());

                node.addConnection(mockConnection(ConnectionMode.Both, ConnectionState.On));
                assertEquals(2, node.getAllConnections().size());
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

                Channel channel = mock(Channel.class);
                when(channel.isActive()).thenReturn(true);
                when(channel.close()).thenReturn(mock(ChannelFuture.class));

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
                builder.setFilterSpecific(FilterSpecific.AccessOrPrefixList);
                AclFilterEntriesBuilder aclFilterEntriesBuilder = new AclFilterEntriesBuilder();
                ArrayList<AclEntry> aclEntries = new ArrayList<>();
                AclEntryBuilder aclEntryBuilder = new AclEntryBuilder();
                aclEntryBuilder.setEntrySeq(1);
                aclEntryBuilder.setEntryType(FilterEntryType.Permit);
                SgtMatchesBuilder matchesBuilder = new SgtMatchesBuilder();
                ArrayList<Sgt> sgts = new ArrayList<>();
                sgts.add(new Sgt(5));
                matchesBuilder.setMatches(sgts);
                aclEntryBuilder.setSgtMatch( matchesBuilder.build());
                aclEntries.add(aclEntryBuilder.build());
                aclFilterEntriesBuilder.setAclEntry(aclEntries);
                builder.setFilterEntries(aclFilterEntriesBuilder.build());
                return builder.build();
        }

        private SxpPeerGroup getGroup(String name, List<SxpFilter> filters, List<SxpPeer> sxpPeers) {
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

                node.addPeerGroup(getGroup("TEST3", getFilters(FilterType.Inbound, FilterType.Inbound), null));
                node.addPeerGroup(getGroup("TEST3", getFilters(FilterType.Outbound, FilterType.Outbound), null));
                assertEquals(2, node.getPeerGroups().size());
                node.addPeerGroup(getGroup("TEST2", getFilters(FilterType.Inbound), null));
                assertEquals(3, node.getPeerGroups().size());
                exception.expect(IllegalArgumentException.class);
                node.addPeerGroup(getGroup("TEST3", getFilters(FilterType.Inbound, FilterType.Outbound), null));
        }

        @Test public void testGetPeerGroup() throws Exception {
                assertNull(node.getPeerGroup("TEST"));
                SxpPeerGroup peerGroup = getGroup("TEST", new ArrayList<SxpFilter>(), null);
                node.addPeerGroup(peerGroup);
                assertEquals(peerGroup, node.getPeerGroup("TEST"));
                peerGroup = getGroup("TEST3", getFilters(FilterType.Inbound, FilterType.Outbound), null);
                node.addPeerGroup(peerGroup);
                assertEquals(peerGroup, node.getPeerGroup("TEST3"));
                assertNotEquals(peerGroup, node.getPeerGroup("TEST"));
        }

        @Test public void testRemovePeerGroup() throws Exception {
                assertTrue(node.getPeerGroups().isEmpty());
                node.addPeerGroup(getGroup("TEST", null, null));
                node.addPeerGroup(getGroup("TEST1", new ArrayList<SxpFilter>(), null));
                node.addPeerGroup(getGroup("TEST3", null, null));
                assertEquals(3, node.getPeerGroups().size());

                node.removePeerGroup("TEST");
                assertEquals(2, node.getPeerGroups().size());
                node.removePeerGroup("TEST3");
                assertEquals(1, node.getPeerGroups().size());
                assertEquals(getGroup("TEST1", new ArrayList<SxpFilter>(), null), node.getPeerGroup("TEST1"));
                node.removePeerGroup("TEST1");
                assertEquals(0, node.getPeerGroups().size());
        }

        @Test public void testGetPeerGroups() throws Exception {
                assertTrue(node.getPeerGroups().isEmpty());
                SxpPeerGroup peerGroup = getGroup("TEST", new ArrayList<SxpFilter>(), null);
                node.addPeerGroup(peerGroup);
                assertTrue(node.getPeerGroups().contains(peerGroup));
                peerGroup = getGroup("TEST3", getFilters(FilterType.Inbound, FilterType.Outbound), null);
                node.addPeerGroup(peerGroup);
                assertTrue(node.getPeerGroups().contains(peerGroup));
                assertEquals(2, node.getPeerGroups().size());
        }

        @Test public void testAddFilterToPeerGroup() throws Exception {
                assertFalse(node.addPeerGroup(null));
                assertTrue(node.getPeerGroups().isEmpty());
                List<SxpPeer> sxpPeers = new ArrayList<>();
                sxpPeers.add(
                        new SxpPeerBuilder().setPeerAddress(new IpAddress(Ipv4Address.getDefaultInstance("5.5.5.0")))
                                .build());
                node.addPeerGroup(getGroup("TEST", null, sxpPeers));
                node.addPeerGroup(getGroup("TEST1", null, null));
                assertEquals(2, node.getPeerGroups().size());

                node.addFilterToPeerGroup(null, getFilter(FilterType.Inbound));
                node.addFilterToPeerGroup("TEST", null);
                assertTrue(node.getPeerGroup("TEST").getSxpFilter().isEmpty());

                node.addFilterToPeerGroup("TEST", getFilter(FilterType.Inbound));
                node.addFilterToPeerGroup("TEST", getFilter(FilterType.Inbound));
                assertEquals(1, node.getPeerGroup("TEST").getSxpFilter().size());

                node.addFilterToPeerGroup("TEST1", getFilter(FilterType.Outbound));
                assertEquals(1, node.getPeerGroup("TEST1").getSxpFilter().size());

                node.addFilterToPeerGroup("TEST", getFilter(FilterType.Outbound));
                node.addFilterToPeerGroup("TEST1", getFilter(FilterType.Inbound));
                assertEquals(1, node.getPeerGroup("TEST").getSxpFilter().size());
                assertEquals(1, node.getPeerGroup("TEST1").getSxpFilter().size());
        }

        @Test public void testRemoveFilterFromPeerGroup() throws Exception {
                assertTrue(node.getPeerGroups().isEmpty());
                node.addPeerGroup(getGroup("TEST", null, null));
                node.addFilterToPeerGroup("TEST", getFilter(FilterType.Outbound));
                node.removeFilterFromPeerGroup("TEST", FilterType.Inbound, FilterSpecific.AccessOrPrefixList);
                assertEquals(1, node.getPeerGroup("TEST").getSxpFilter().size());
                node.removeFilterFromPeerGroup("TEST", FilterType.Outbound, FilterSpecific.AccessOrPrefixList);
                assertEquals(0, node.getPeerGroup("TEST").getSxpFilter().size());

                node.addPeerGroup(getGroup("TEST2", getFilters(FilterType.Inbound, FilterType.Outbound), null));
                assertEquals(2, node.getPeerGroup("TEST2").getSxpFilter().size());
                node.removeFilterFromPeerGroup("TEST2", FilterType.Outbound, FilterSpecific.AccessOrPrefixList);
                node.removeFilterFromPeerGroup("TEST2", FilterType.Inbound, FilterSpecific.AccessOrPrefixList);
                assertEquals(0, node.getPeerGroup("TEST2").getSxpFilter().size());
        }

        @Test public void testUpdateFilterInPeerGroup() throws Exception {
                assertFalse(node.addPeerGroup(null));
                assertTrue(node.getPeerGroups().isEmpty());
                node.addPeerGroup(getGroup("TEST", null, null));
                SxpFilter filter = getFilter(FilterType.Inbound);
                node.addFilterToPeerGroup("TEST", filter);
                assertEquals(1, node.getPeerGroup("TEST").getSxpFilter().size());

                assertNull(node.updateFilterInPeerGroup(null, getFilter(FilterType.Outbound)));
                assertNull(node.updateFilterInPeerGroup("TEST", getFilter(FilterType.Outbound)));
                assertEquals(filter, node.updateFilterInPeerGroup("TEST", getFilter(FilterType.Inbound)));
                assertEquals(1, node.getPeerGroup("TEST").getSxpFilter().size());
        }
}
