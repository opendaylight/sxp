/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.exception.node.DomainNotFoundException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.acl.filter.entries.AclEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.acl.filter.entries.AclEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtMatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.fields.DomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.fields.domains.DomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpPeersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.sxp.peers.SxpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.sxp.peers.SxpPeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.Security;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.Timers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

public class SxpNodeTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    private SxpNode node;

    private SxpNodeIdentity nodeIdentity;
    private MasterDatabaseInf databaseProvider;
    private SxpDatabaseInf sxpDatabaseProvider;
    private ThreadsWorker worker;
    private Timers timers;
    private int ip4Adrres = 0;

    @Before
    public void init() throws Exception {
        worker = mock(ThreadsWorker.class);
        ListenableFuture future = Futures.immediateFuture(null);
        when(worker.scheduleTask(any(Callable.class), anyInt(), any(TimeUnit.class)))
                .thenReturn(mock(ListenableScheduledFuture.class));
        when(worker.executeTask(any(Runnable.class), any(ThreadsWorker.WorkerType.class)))
                .thenReturn(future);
        when(worker.executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class)))
                .thenReturn(future);
        when(worker.executeTaskInSequence(any(Callable.class), any(ThreadsWorker.WorkerType.class),
                any(SxpConnection.class))).thenReturn(future);
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
        when(nodeIdentity.getSourceIp()).thenReturn(IpAddressBuilder.getDefaultInstance("127.1.1.1"));
        when(nodeIdentity.getTcpPort()).thenReturn(PortNumber.getDefaultInstance("64999"));

        databaseProvider = mock(MasterDatabaseInf.class);
        sxpDatabaseProvider = mock(SxpDatabaseInf.class);
        node = SxpNode.createInstance(NodeIdConv.createNodeId("127.0.0.1"), nodeIdentity, databaseProvider,
                        sxpDatabaseProvider, worker);

        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        Field serverChannel = node.getClass().getDeclaredField("serverChannel");
        serverChannel.setAccessible(true);
        serverChannel.set(node, channel);
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
        when(connection.getPeerAddress()).thenReturn(IpAddressBuilder.getDefaultInstance("127.0.0." + (++ip4Adrres)));
        when(connection.getState()).thenReturn(state);
        when(connection.getPassword()).thenReturn("Default");
        when(connection.getVersion()).thenReturn(Version.Version4);
        return connection;
    }

    @Test
    public void testCreateInstance() throws Exception {
        when(nodeIdentity.getSourceIp()).thenReturn(new IpAddress(Ipv4Address.getDefaultInstance("0.0.0.0")));
        assertNotNull(SxpNode.createInstance(NodeIdConv.createNodeId("127.0.0.1"), nodeIdentity, databaseProvider,
                sxpDatabaseProvider, worker));
        assertNotNull(SxpNode.createInstance(NodeIdConv.createNodeId("127.0.0.1"), nodeIdentity, databaseProvider,
                sxpDatabaseProvider));
        assertNotNull(SxpNode.createInstance(NodeIdConv.createNodeId("127.0.0.1"), nodeIdentity));
    }

    @Test
    public void setSecurity() throws Exception {
        node.setSecurity(new SecurityBuilder().build());
        assertNotNull(node.getSslContextFactory());
        assertNotNull(node.getPassword());
        assertEquals("", node.getPassword());

        node.setSecurity(new SecurityBuilder().setPassword("passwd").setTls(null).build());
        assertNotNull(node.getSslContextFactory());
        assertNotNull(node.getPassword());
        assertEquals("passwd", node.getPassword());
    }

    @Test
    public void testGetAllDeleteHoldDownConnections() throws Exception {
        node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.DeleteHoldDown));
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.On));
        node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.Off));
        assertEquals(2, node.getAllDeleteHoldDownConnections().size());
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
        node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.PendingOn));
        assertEquals(3, node.getAllDeleteHoldDownConnections().size());
    }

    @Test
    public void testGetAllOffConnections() throws Exception {
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.Off));
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.PendingOn));
        node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
        node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.Off));
        assertEquals(2, node.getAllOffConnections().size());
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.Off));
        assertEquals(3, node.getAllOffConnections().size());
    }

    @Test
    public void testGetAllOnConnections() throws Exception {
        node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.DeleteHoldDown));
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.PendingOn));
        node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.Off));
        assertEquals(1, node.getAllOnConnections().size());
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
        assertEquals(1, node.getAllOnConnections().size());
    }

    @Test
    public void testGetAllOnListenerConnections() throws Exception {
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.PendingOn));
        node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.Off));
        assertEquals(1, node.getAllOnListenerConnections().size());
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.On));
        assertEquals(1, node.getAllOnListenerConnections().size());
    }

    @Test
    public void testGetAllOnSpeakerConnections() throws Exception {
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
                        s.getIpv4Address() != null ? s.getIpv4Address().getValue() : s.getIpv6Address().getValue());
        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 64999);
        return inetSocketAddress;
    }

    @Test
    public void testSxpNodeGetters() throws Exception {
        assertEquals((long) timers.getRetryOpenTime(), node.getRetryOpenTime());
        assertEquals(150l, node.getExpansionQuantity());
        assertEquals((long) timers.getHoldTime(), node.getHoldTime());
        assertEquals((long) timers.getHoldTimeMax(), node.getHoldTimeMax());
        assertEquals((long) timers.getHoldTimeMin(), node.getHoldTimeMin());
        assertEquals((long) timers.getHoldTimeMinAcceptable(), node.getHoldTimeMinAcceptable());
        assertEquals((long) timers.getKeepAliveTime(), node.getKeepAliveTime());
        assertEquals("NAME", node.getName());
        assertEquals(NodeId.getDefaultInstance("127.0.0.1"), node.getNodeId());
        assertEquals("default", node.getPassword());
        assertEquals(64999, node.getServerPort());

        SxpNodeIdentity mockIdentity = mock(SxpNodeIdentity.class);
        when(mockIdentity.getSourceIp()).thenReturn(IpAddressBuilder.getDefaultInstance("127.1.1.1"));

        SxpNode node = SxpNode.createInstance(NodeIdConv.createNodeId("127.0.0.1"), mockIdentity,
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

    @Test
    public void testGetByAddress() throws Exception {
        Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);
        Connection connection1 = mockConnection(ConnectionMode.Listener, ConnectionState.On);

        node.addConnection(connection);
        assertNotNull(node.getByAddress(getInetSocketAddress(connection.getPeerAddress())));
        assertNull(node.getByAddress(getInetSocketAddress(connection1.getPeerAddress())));

        node.addConnection(connection1);
        assertNotNull(node.getByAddress(getInetSocketAddress(connection.getPeerAddress())));
        assertNotNull(node.getByAddress(getInetSocketAddress(connection1.getPeerAddress())));
    }

    @Test
    public void testGetByPort() throws Exception {
        Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);

        node.addConnection(connection);
        assertNotNull(node.getByPort(64999));
        assertNull(node.getByPort(64950));
    }

    @Test
    public void testOpenConnections() throws Exception {
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.DeleteHoldDown));
        node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.DeleteHoldDown));
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.PendingOn));
        node.addConnection(mockConnection(ConnectionMode.Listener, ConnectionState.On));
        node.openConnections();
        verify(worker, times(5)).executeTask(any(Runnable.class), any(ThreadsWorker.WorkerType.class));
        node.addConnection(mockConnection(ConnectionMode.Speaker, ConnectionState.Off));
        node.openConnections();
        verify(worker, times(7)).executeTask(any(Runnable.class), any(ThreadsWorker.WorkerType.class));
    }

    @Test
    public void testShutdownConnections() throws Exception {
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

    @Test
    public void testStart() throws Exception {
        final SxpNode node = SxpNode.createInstance(NodeIdConv.createNodeId("127.1.1.1"), nodeIdentity);
        node.start().get();
        for (int i = 0; i < 3 && !node.isEnabled(); i++) {
            TimeUnit.SECONDS.sleep(1);
        }
        assertTrue(node.isEnabled());

        node.shutdown().get();
        for (int i = 0; i < 3 && node.isEnabled(); i++) {
            TimeUnit.SECONDS.sleep(1);
        }
        assertFalse(node.isEnabled());
    }

    private MasterDatabaseBinding getBinding(String prefix, int sgt) {
        return new MasterDatabaseBindingBuilder().setIpPrefix(IpPrefixBuilder.getDefaultInstance(prefix))
                .setSecurityGroupTag(new Sgt(sgt))
                .setOrigin(BindingOriginsConfig.LOCAL_ORIGIN)
                .build();
    }

    @Test
    public void testPutBindingsMasterDatabase() throws Exception {
        assertNotNull(
                node.putBindingsMasterDatabase(Collections.singletonList(getBinding("1.1.1.1/32", 56)), "global"));
        verify(databaseProvider).addBindings(anyList());
        assertNotNull(
                node.putBindingsMasterDatabase(Collections.singletonList(getBinding("1.1.1.1/32", 56)), "global"));
        verify(databaseProvider, times(2)).addBindings(anyList());
        exception.expect(DomainNotFoundException.class);
        node.putBindingsMasterDatabase(Collections.singletonList(getBinding("1.1.1.1/32", 56)), "badDomain");
    }

    @Test
    public void testRemoveBindingsMasterDatabase() throws Exception {
        assertNotNull(node.removeBindingsMasterDatabase(Collections.singletonList(getBinding("1.1.1.1/32", 56)),
                "global"));
        verify(databaseProvider).deleteBindings(anyList());
        assertNotNull(node.removeBindingsMasterDatabase(Collections.singletonList(getBinding("1.1.1.1/32", 56)),
                "global"));
        verify(databaseProvider, times(2)).deleteBindings(anyList());
        exception.expect(DomainNotFoundException.class);
        node.removeBindingsMasterDatabase(Collections.singletonList(getBinding("1.1.1.1/32", 56)), "badDomain");
    }

    @Test
    public void testRemoveConnection() throws Exception {
        Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);
        node.addConnection(connection);
        SxpConnection sxpConnection = node.removeConnection(getInetSocketAddress(connection.getPeerAddress()));
        assertEquals(ConnectionState.Off, sxpConnection.getState());
    }

    @Test
    public void testGetConnection() throws Exception {
        Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);
        node.addConnection(connection);

        SxpConnection sxpConnection = node.getConnection(getInetSocketAddress(connection.getPeerAddress()));
        assertNotNull(connection);
        assertEquals(connection.getMode(), sxpConnection.getMode());
        assertEquals(connection.getState(), sxpConnection.getState());

        assertNull(node.getConnection(getInetSocketAddress(IpAddressBuilder.getDefaultInstance("0.9.9.9"))));
    }

    @Test
    public void testAddConnection() throws Exception {
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
    }

    @Test
    public void testAddConnections() throws Exception {
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
        aclEntryBuilder.setSgtMatch(matchesBuilder.build());
        aclEntries.add(aclEntryBuilder.build());
        aclFilterEntriesBuilder.setAclEntry(aclEntries);
        builder.setFilterEntries(aclFilterEntriesBuilder.build());
        return builder.build();
    }

    private DomainFilter getFilter(String domainName, String... domain) {
        DomainFilterBuilder filterBuilder = new DomainFilterBuilder();
        Arrays.sort(domain);
        filterBuilder.setFilterName(domainName);
        filterBuilder.setDomains(new DomainsBuilder().setDomain(Arrays.asList(domain)
                .stream()
                .map(d -> new DomainBuilder().setName(d).build())
                .collect(Collectors.toList())).build());
        AclFilterEntriesBuilder aclFilterEntriesBuilder = new AclFilterEntriesBuilder();
        ArrayList<AclEntry> aclEntries = new ArrayList<>();
        AclEntryBuilder aclEntryBuilder = new AclEntryBuilder();
        aclEntryBuilder.setEntrySeq(1);
        aclEntryBuilder.setEntryType(FilterEntryType.Permit);
        SgtMatchesBuilder matchesBuilder = new SgtMatchesBuilder();
        ArrayList<Sgt> sgts = new ArrayList<>();
        sgts.add(new Sgt(5));
        matchesBuilder.setMatches(sgts);
        aclEntryBuilder.setSgtMatch(matchesBuilder.build());
        aclEntries.add(aclEntryBuilder.build());
        aclFilterEntriesBuilder.setAclEntry(aclEntries);
        filterBuilder.setFilterEntries(aclFilterEntriesBuilder.build());
        return filterBuilder.build();
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

    @Test
    public void testAddPeerGroup() throws Exception {
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

    @Test
    public void testGetPeerGroup() throws Exception {
        assertNull(node.getPeerGroup("TEST"));
        SxpPeerGroup peerGroup = getGroup("TEST", new ArrayList<SxpFilter>(), null);
        node.addPeerGroup(peerGroup);
        assertEquals(peerGroup, node.getPeerGroup("TEST"));
        peerGroup = getGroup("TEST3", getFilters(FilterType.Inbound, FilterType.Outbound), null);
        node.addPeerGroup(peerGroup);
        assertEquals(peerGroup, node.getPeerGroup("TEST3"));
        assertNotEquals(peerGroup, node.getPeerGroup("TEST"));
    }

    @Test
    public void testRemovePeerGroup() throws Exception {
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

    @Test
    public void testGetPeerGroups() throws Exception {
        assertTrue(node.getPeerGroups().isEmpty());
        SxpPeerGroup peerGroup = getGroup("TEST", new ArrayList<SxpFilter>(), null);
        node.addPeerGroup(peerGroup);
        assertTrue(node.getPeerGroups().contains(peerGroup));
        peerGroup = getGroup("TEST3", getFilters(FilterType.Inbound, FilterType.Outbound), null);
        node.addPeerGroup(peerGroup);
        assertTrue(node.getPeerGroups().contains(peerGroup));
        assertEquals(2, node.getPeerGroups().size());
    }

    @Test
    public void testAddFilterToPeerGroup() throws Exception {
        assertFalse(node.addPeerGroup(null));
        assertTrue(node.getPeerGroups().isEmpty());
        List<SxpPeer> sxpPeers = new ArrayList<>();
        sxpPeers.add(
                new SxpPeerBuilder().setPeerAddress(new IpAddress(Ipv4Address.getDefaultInstance("5.5.5.0"))).build());
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

    @Test
    public void testRemoveFilterFromPeerGroup() throws Exception {
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

    @Test
    public void testUpdateFilterInPeerGroup() throws Exception {
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

    @Test
    public void testGetNodeIdentity() throws Exception {
        assertNotNull(node.getNodeIdentity());
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain getDomain(
            String name, Connections connections) {
        SxpDomainBuilder builder = new SxpDomainBuilder();
        builder.setDomainName(name);
        builder.setConnections(connections);
        return builder.build();
    }

    @Test
    public void testAddDomain() throws Exception {
        assertTrue(node.addDomain(getDomain("private", null)));
        assertFalse(node.addDomain(getDomain("private", null)));
        assertTrue(node.addDomain(getDomain("test", null)));
    }

    @Test
    public void testRemoveDomain() throws Exception {
        node.addDomain(getDomain("private", new ConnectionsBuilder().setConnection(
                Collections.singletonList(mockConnection(ConnectionMode.Listener, ConnectionState.On))).build()));
        node.addDomain(getDomain("test", null));

        assertNotNull(node.removeDomain("test"));
        exception.expect(IllegalStateException.class);
        assertNotNull(node.removeDomain("private"));
    }

    @Test
    public void testSetPassword() throws Exception {
        node.setSecurity(new SecurityBuilder().setPassword("test").build());
        assertEquals("test", node.getPassword());
    }

    @Test
    public void testToString() throws Exception {
        assertEquals("[NAME:127.0.0.1]", node.toString());
    }

    @Test
    public void testGetDomains() throws Exception {
        assertNotNull(node.getDomains());
        assertFalse(node.getDomains().isEmpty());
        node.addDomain(getDomain("private", null));
        node.addDomain(getDomain("test", null));

        assertNotNull(node.getDomains());
        assertFalse(node.getDomains().isEmpty());
        assertEquals(3, node.getDomains().size());
    }

    @Test
    public void testAddFilterToDomain() throws Exception {
        node.addDomain(getDomain("custom", null));
        SxpDomain domain = node.getDomain("custom");
        node.addFilterToDomain("custom", getFilter("custom", "global"));
        assertEquals(1, domain.getFilters().size());
        node.addFilterToDomain("custom", getFilter("custom", "global"));
        assertEquals(1, domain.getFilters().size());
    }

    @Test
    public void testRemoveFilterFromDomain() throws Exception {
        node.addDomain(getDomain("custom", null));
        SxpDomain domain = node.getDomain("custom");
        node.addFilterToDomain("custom", getFilter("custom", "global"));
        assertEquals(1, domain.getFilters().size());
        node.removeFilterFromDomain("domain", FilterSpecific.AccessOrPrefixList, "custom");
        assertEquals(1, domain.getFilters().size());
        node.removeFilterFromDomain("custom", FilterSpecific.PeerSequence, "custom");
        assertEquals(1, domain.getFilters().size());
        node.removeFilterFromDomain("custom", FilterSpecific.AccessOrPrefixList, "filter");
        assertEquals(1, domain.getFilters().size());
        node.removeFilterFromDomain("custom", FilterSpecific.AccessOrPrefixList, "custom");
        assertEquals(0, domain.getFilters().size());
    }

    @Test
    public void testUpdateDomainFilter() throws Exception {
        node.addDomain(getDomain("custom", null));
        SxpDomain domain = node.getDomain("custom");
        node.addFilterToDomain("custom", getFilter("custom", "global"));
        assertEquals(1, domain.getFilters().size());

        node.updateDomainFilter("domain", getFilter("custom", "global"));
        assertEquals(1, domain.getFilters().size());
        assertTrue(domain.getFilters().containsKey("global"));

        node.updateDomainFilter("custom", getFilter("custom", "newGlobal"));
        assertEquals(1, domain.getFilters().size());
        assertTrue(domain.getFilters().containsKey("newGlobal"));

        node.updateDomainFilter("custom", getFilter("custom", "global", "test"));
        assertEquals(2, domain.getFilters().size());
        assertTrue(domain.getFilters().containsKey("global"));
        assertTrue(domain.getFilters().containsKey("test"));
    }

    @Test
    public void testGetBindingMasterDatabase() throws Exception {
        assertNotNull(node.getBindingMasterDatabase());
        node.addDomain(getDomain("custom", null));
        assertNotNull(node.getBindingMasterDatabase("custom"));
        exception.expect(DomainNotFoundException.class);
        node.getBindingMasterDatabase("badDomain");
    }

    @Test
    public void testGetBindingSxpDatabase() throws Exception {
        assertNotNull(node.getBindingSxpDatabase());
        node.addDomain(getDomain("custom", null));
        assertNotNull(node.getBindingSxpDatabase("custom"));
        exception.expect(DomainNotFoundException.class);
        node.getBindingSxpDatabase("badDomain");
    }

    @Test
    public void testSetMessageMergeSize() throws Exception {
        node.setMessageMergeSize(25);
        exception.expect(IllegalArgumentException.class);
        node.setMessageMergeSize(0);
    }

    @Test
    public void testSetMessagePartitionSize() throws Exception {
        node.setMessagePartitionSize(25);
        exception.expect(IllegalArgumentException.class);
        node.setMessagePartitionSize(250);
    }
}
