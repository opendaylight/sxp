/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpDomainFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.FilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.acl.filter.entries.AclEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtMatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.SxpDomainFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.fields.DomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.fields.domains.DomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class, BindingDispatcher.class}) public class SxpDomainTest {

    private static int PORT = 64999;
    @Rule public ExpectedException exception = ExpectedException.none();

    private SxpNode sxpNode;
    private SxpDomain domain;
    private SxpDatabaseInf sxpDatabase = mock(SxpDatabaseImpl.class);
    private MasterDatabaseInf masterDatabase = mock(MasterDatabaseImpl.class);
    private List<SxpConnection> connections = new ArrayList<>();
    private BindingDispatcher dispatcher;
    private List<SxpDomain> domains = new ArrayList<>();

    @Before public void setUp() {
        sxpNode = mock(SxpNode.class);
        dispatcher = mock(BindingDispatcher.class);
        when(sxpNode.getSvcBindingDispatcher()).thenReturn(dispatcher);
        when(sxpDatabase.getBindings()).thenReturn(Collections.singletonList(mock(SxpDatabaseBinding.class)));
        when(sxpDatabase.addBinding(any(NodeId.class), anyList())).thenReturn(
                Collections.singletonList(mock(SxpDatabaseBinding.class)));
        when(sxpDatabase.deleteBindings(any(NodeId.class), anyList())).thenReturn(
                Collections.singletonList(mock(SxpDatabaseBinding.class)));
        when(masterDatabase.getLocalBindings()).thenReturn(
                Collections.singletonList(mock(MasterDatabaseBinding.class)));
        when(masterDatabase.addBindings(anyList())).thenReturn(
                Collections.singletonList(mock(MasterDatabaseBinding.class)));
        when(masterDatabase.deleteBindings(anyList())).thenReturn(
                Collections.singletonList(mock(MasterDatabaseBinding.class)));
        domain = SxpDomain.createInstance(sxpNode, "domain", sxpDatabase, masterDatabase);
        domains.add(domain);
        domains.add(SxpDomain.createInstance(sxpNode, "domain1", sxpDatabase, masterDatabase));
        connections.clear();
    }

    private SxpConnection getSxpConnection(String address) {
        SxpConnection connection = mock(SxpConnection.class);
        when(connection.getDestination()).thenReturn(new InetSocketAddress(address, PORT));
        when(connection.isModeListener()).thenReturn(true);
        when(connection.getNodeIdRemote()).thenReturn(new NodeId("0.0.0.0"));
        connections.add(connection);
        return connection;
    }

    @Test public void testGetMasterDatabase() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        assertNotNull(domain.getMasterDatabase());
        assertEquals(masterDatabase, domain.getMasterDatabase());
    }

    @Test public void testGetSxpDatabase() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        assertNotNull(domain.getSxpDatabase());
        assertEquals(sxpDatabase, domain.getSxpDatabase());
    }

    @Test public void testGetName() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        assertEquals("domain", domain.getName());
    }

    @Test public void testGetConnections() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        domain.putConnection(getSxpConnection("127.0.0.1"));
        domain.putConnection(getSxpConnection("127.0.0.2"));
        domain.putConnection(getSxpConnection("127.0.0.3"));
        assertEquals(3, domain.getConnections().size());
        domain.putConnection(getSxpConnection("127.0.0.4"));
        domain.putConnection(getSxpConnection("127.0.0.5"));
        assertEquals(5, domain.getConnections().size());
        domain.removeConnection(new InetSocketAddress("127.0.0.1", PORT));
        assertEquals(4, domain.getConnections().size());
    }

    @Test public void testGetConnection() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        domain.putConnection(getSxpConnection("127.0.0.1"));
        assertNull(domain.getConnection(new InetSocketAddress("127.0.0.2", PORT)));
        assertNotNull(domain.getConnection(new InetSocketAddress("127.0.0.1", PORT)));
    }

    @Test public void testHasConnection() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        domain.putConnection(getSxpConnection("127.0.0.1"));
        domain.putConnection(getSxpConnection("127.0.0.3"));
        assertTrue(domain.hasConnection(new InetSocketAddress("127.0.0.1", PORT)));
        assertFalse(domain.hasConnection(new InetSocketAddress("127.0.0.2", PORT)));
        assertTrue(domain.hasConnection(new InetSocketAddress("127.0.0.3", PORT)));
        domain.removeConnection(new InetSocketAddress("127.0.0.1", PORT));
        assertFalse(domain.hasConnection(new InetSocketAddress("127.0.0.1", PORT)));
        assertFalse(domain.hasConnection(new InetSocketAddress("127.0.0.2", PORT)));
        assertTrue(domain.hasConnection(new InetSocketAddress("127.0.0.3", PORT)));
    }

    @Test public void testPutConnection() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        domain.putConnection(getSxpConnection("127.0.0.1"));
        domain.putConnection(getSxpConnection("127.0.0.2"));
        domain.putConnection(getSxpConnection("127.0.0.3"));
        assertEquals(3, domain.getConnections().size());

        exception.expect(IllegalArgumentException.class);
        domain.putConnection(getSxpConnection("127.0.0.1"));
    }

    @Test public void testRemoveConnection() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        domain.putConnection(getSxpConnection("127.0.0.1"));
        domain.putConnection(getSxpConnection("127.0.0.2"));
        domain.putConnection(getSxpConnection("127.0.0.3"));
        assertNotNull(domain.removeConnection(new InetSocketAddress("127.0.0.2", PORT)));
        assertNull(domain.removeConnection(new InetSocketAddress("127.0.0.2", PORT)));

        assertNotNull(domain.removeConnection(new InetSocketAddress("127.0.0.1", PORT)));
        assertNotNull(domain.removeConnection(new InetSocketAddress("127.0.0.3", PORT)));

        assertNull(domain.removeConnection(new InetSocketAddress("127.0.0.1", PORT)));
        assertNull(domain.removeConnection(new InetSocketAddress("127.0.0.3", PORT)));
    }

    @Test public void testClose() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        domain.putConnection(getSxpConnection("127.0.0.1"));
        domain.putConnection(getSxpConnection("127.0.0.2"));
        domain.putConnection(getSxpConnection("127.0.0.3"));

        domain.close();
        connections.forEach(c -> verify(c).shutdown());
    }

    private SxpBindingFilter<?, ? extends SxpDomainFilterFields> getFilter(FilterSpecific type, String... domain) {
        SxpBindingFilter filter = mock(SxpBindingFilter.class);
        when(filter.test(any(SxpBindingFields.class))).thenReturn(true);
        when(filter.getIdentifier()).thenReturn("test2");
        SxpDomainFilterBuilder filterBuilder = new SxpDomainFilterBuilder();
        Arrays.sort(domain);
        String name = Arrays.deepToString(domain);
        filterBuilder.setFilterName(name.substring(1, name.length() - 1));
        filterBuilder.setFilterSpecific(type);
        filterBuilder.setDomains(new DomainsBuilder().setDomain(Arrays.asList(domain)
                .stream()
                .map(d -> new DomainBuilder().setName(d).build())
                .collect(Collectors.toList())).build());
        filterBuilder.setFilterEntries(mock(FilterEntries.class));
        when(filter.getSxpFilter()).thenReturn(filterBuilder.build());
        return filter;
    }

    @Test public void testGetFilters() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        assertNotNull(domain.getFilters());
        assertTrue(domain.getConnections().isEmpty());
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain1")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.PeerSequence, "domain1")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain2", "domain3")));
        assertFalse(domain.getFilters().isEmpty());
        assertEquals(3, domain.getFilters().size());
    }

    @Test public void testAddFilter() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        domain.putConnection(getSxpConnection("127.0.0.1"));
        assertFalse(domain.addFilter(null));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain8")));
        verify(dispatcher, never()).propagateUpdate(anyList(), anyList(), anyList());
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain1")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.PeerSequence, "domain1")));
        assertFalse(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain1")));
        assertFalse(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain5", "domain1")));
        verify(dispatcher, atLeast(1)).propagateUpdate(anyList(), anyList(), anyList());
    }

    @Test public void testRemoveFilter() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        domain.putConnection(getSxpConnection("127.0.0.1"));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain8")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain1")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.PeerSequence, "domain1")));

        assertNull(domain.removeFilter(FilterSpecific.AccessOrPrefixList, "tmp"));
        assertNull(domain.removeFilter(FilterSpecific.PeerSequence, "domain0"));
        assertNotNull(domain.removeFilter(FilterSpecific.AccessOrPrefixList, "domain1"));
        assertNotNull(domain.removeFilter(FilterSpecific.PeerSequence, "domain1"));
    }

    @Test public void testPropagateToSharedDomains() throws Exception {
        when(sxpNode.getDomains()).thenReturn(domains);
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain8")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain1")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.PeerSequence, "domain1")));

        dispatcher = mock(BindingDispatcher.class);
        when(sxpNode.getSvcBindingDispatcher()).thenReturn(dispatcher);

        domain.pushToSharedMasterDatabases(null, null);
        verify(dispatcher, never()).propagateUpdate(anyList(), anyList(), anyList());
        domain.pushToSharedMasterDatabases(null, new ArrayList<>());
        verify(dispatcher, never()).propagateUpdate(anyList(), anyList(), anyList());
        domain.pushToSharedMasterDatabases(new ArrayList<>(), null);
        verify(dispatcher, never()).propagateUpdate(anyList(), anyList(), anyList());
        domain.pushToSharedMasterDatabases(new ArrayList<>(), new ArrayList<>());
        verify(dispatcher, never()).propagateUpdate(anyList(), anyList(), anyList());

        domain.pushToSharedMasterDatabases(Collections.singletonList(mock(MasterDatabaseBinding.class)),
                Collections.singletonList(mock(MasterDatabaseBinding.class)));
        verify(dispatcher).propagateUpdate(anyList(), anyList(), anyList());
    }

    private DomainFilter getDomainFilter(String id, String domain, FilterEntryType entryType, Integer... sgts) {
        return new DomainFilterBuilder().setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                .setFilterName(id)
                .setDomains(new DomainsBuilder().setDomain(
                        Collections.singletonList(new DomainBuilder().setName(domain).build())).build())
                .setFilterEntries(new AclFilterEntriesBuilder().setAclEntry(Collections.singletonList(
                        new AclEntryBuilder().setEntrySeq(1)
                                .setEntryType(entryType)
                                .setSgtMatch(new SgtMatchesBuilder().setMatches(
                                        Arrays.asList(sgts).stream().map(Sgt::new).collect(Collectors.toList()))
                                        .build())
                                .build())).build())
                .build();
    }

    private MasterDatabaseBinding getMasterDatabaseBinding(String prefix, int sgt) {
        return new MasterDatabaseBindingBuilder().setSecurityGroupTag(new Sgt(sgt))
                .setIpPrefix(new IpPrefix(prefix.toCharArray()))
                .build();
    }

    @Test public void testUpdateFilter() throws Exception {
        List<MasterDatabaseBinding> masterDatabaseBindings = new ArrayList<>();
        masterDatabaseBindings.add(getMasterDatabaseBinding("1.1.1.1/32", 1));
        masterDatabaseBindings.add(getMasterDatabaseBinding("2.2.2.2/32", 2));
        masterDatabaseBindings.add(getMasterDatabaseBinding("3.3.3.3/32", 3));
        masterDatabaseBindings.add(getMasterDatabaseBinding("4.4.4.4/32", 4));
        masterDatabaseBindings.add(getMasterDatabaseBinding("5.5.5.5/32", 5));
        masterDatabaseBindings.add(getMasterDatabaseBinding("6.6.6.6/32", 6));
        when(masterDatabase.getLocalBindings()).thenReturn(masterDatabaseBindings);

        SxpDatabaseInf sxpDatabase = mock(SxpDatabaseImpl.class);
        MasterDatabaseInf masterDatabase = mock(MasterDatabaseImpl.class);
        ArgumentCaptor<List<MasterDatabaseBinding>> captorAdd = ArgumentCaptor.forClass((Class) List.class),
                captorDell = ArgumentCaptor.forClass((Class) List.class);
        when(masterDatabase.addBindings(captorAdd.capture())).thenReturn(new ArrayList<>());
        when(masterDatabase.deleteBindings(captorDell.capture())).thenReturn(new ArrayList<>());
        domains.add(SxpDomain.createInstance(sxpNode, "testDomain", sxpDatabase, masterDatabase));
        when(sxpNode.getDomains()).thenReturn(domains);

        SxpBindingFilter<?, ? extends SxpDomainFilterFields>
                bindingFilter =
                SxpBindingFilter.generateFilter(
                        getDomainFilter(domain.getName(), "testDomain", FilterEntryType.Permit, 1, 2, 3, 4, 5),
                        domain.getName());
        domain.addFilter(bindingFilter);
        assertEquals(5, captorAdd.getAllValues().get(captorAdd.getAllValues().size() - 1).size());
        bindingFilter =
                SxpBindingFilter.generateFilter(
                        getDomainFilter(domain.getName(), "testDomain", FilterEntryType.Permit, 1, 3, 5),
                        domain.getName());
        domain.updateFilter(bindingFilter);
        assertEquals(0, captorAdd.getAllValues().get(captorAdd.getAllValues().size() - 1).size());
        assertEquals(2, captorDell.getAllValues().get(captorDell.getAllValues().size() - 1).size());

        bindingFilter =
                SxpBindingFilter.generateFilter(
                        getDomainFilter(domain.getName(), "testDomain", FilterEntryType.Permit, 1, 4),
                        domain.getName());
        domain.updateFilter(bindingFilter);
        assertEquals(1, captorAdd.getAllValues().get(captorAdd.getAllValues().size() - 1).size());
        assertEquals(2, captorDell.getAllValues().get(captorDell.getAllValues().size() - 1).size());
    }
}
