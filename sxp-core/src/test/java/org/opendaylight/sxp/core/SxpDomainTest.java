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
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpDomainFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.FilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.SxpDomainFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.fields.DomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.fields.domains.DomainBuilder;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
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

    @Before public void setUp() {
        sxpNode = mock(SxpNode.class);
        dispatcher = mock(BindingDispatcher.class);
        when(sxpNode.getSvcBindingDispatcher()).thenReturn(dispatcher);
        when(sxpDatabase.getBindings()).thenReturn(new ArrayList<>());
        when(masterDatabase.getLocalBindings()).thenReturn(
                Collections.singletonList(mock(MasterDatabaseBinding.class)));
        domain = SxpDomain.createInstance(sxpNode, "domain", sxpDatabase, masterDatabase);
        List<SxpDomain> domains = new ArrayList<>();
        domains.add(domain);
        domains.add(SxpDomain.createInstance(sxpNode, "domain1", sxpDatabase, masterDatabase));
        when(sxpNode.getDomains()).thenReturn(domains);
        connections.clear();
    }

    private SxpConnection getSxpConnection(String address) {
        SxpConnection connection = mock(SxpConnection.class);
        when(connection.getDestination()).thenReturn(new InetSocketAddress(address, PORT));
        connections.add(connection);
        return connection;
    }

    @Test public void testGetMasterDatabase() throws Exception {
        assertNotNull(domain.getMasterDatabase());
        assertEquals(masterDatabase, domain.getMasterDatabase());
    }

    @Test public void testGetSxpDatabase() throws Exception {
        assertNotNull(domain.getSxpDatabase());
        assertEquals(sxpDatabase, domain.getSxpDatabase());
    }

    @Test public void testGetName() throws Exception {
        assertEquals("domain", domain.getName());
    }

    @Test public void testGetConnections() throws Exception {
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
        domain.putConnection(getSxpConnection("127.0.0.1"));
        assertNull(domain.getConnection(new InetSocketAddress("127.0.0.2", PORT)));
        assertNotNull(domain.getConnection(new InetSocketAddress("127.0.0.1", PORT)));
    }

    @Test public void testHasConnection() throws Exception {
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
        domain.putConnection(getSxpConnection("127.0.0.1"));
        domain.putConnection(getSxpConnection("127.0.0.2"));
        domain.putConnection(getSxpConnection("127.0.0.3"));
        assertEquals(3, domain.getConnections().size());

        exception.expect(IllegalArgumentException.class);
        domain.putConnection(getSxpConnection("127.0.0.1"));
    }

    @Test public void testRemoveConnection() throws Exception {
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
        domain.putConnection(getSxpConnection("127.0.0.1"));
        domain.putConnection(getSxpConnection("127.0.0.2"));
        domain.putConnection(getSxpConnection("127.0.0.3"));

        domain.close();
        connections.forEach(c -> verify(c).shutdown());
    }

    private SxpBindingFilter<?, ? extends SxpDomainFilterFields> getFilter(FilterSpecific type, String... domain) {
        SxpBindingFilter filter = mock(SxpBindingFilter.class);
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
        assertNotNull(domain.getFilters());
        assertTrue(domain.getConnections().isEmpty());
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain1")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.PeerSequence, "domain1")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain2", "domain3")));
        assertFalse(domain.getFilters().isEmpty());
        assertEquals(3, domain.getFilters().size());
    }

    @Test public void testAddFilter() throws Exception {
        assertFalse(domain.addFilter(null));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain8")));
        verify(dispatcher, never()).propagateUpdate(anyList(), anyList(), anyList());
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain1")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.PeerSequence, "domain1")));
        assertFalse(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain1")));
        assertFalse(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain5", "domain1")));
        verify(dispatcher).propagateUpdate(anyList(), anyList(), anyList());
    }

    @Test public void testRemoveFilter() throws Exception {
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain8")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain1")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.PeerSequence, "domain1")));

        assertNull(domain.removeFilter(FilterSpecific.AccessOrPrefixList, "tmp"));
        assertNull(domain.removeFilter(FilterSpecific.PeerSequence, "domain0"));
        assertNotNull(domain.removeFilter(FilterSpecific.AccessOrPrefixList, "domain1"));
        assertNotNull(domain.removeFilter(FilterSpecific.PeerSequence, "domain1"));
    }

    @Test public void testPropagateToSharedDomains() throws Exception {
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain8")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.AccessOrPrefixList, "domain1")));
        assertTrue(domain.addFilter(getFilter(FilterSpecific.PeerSequence, "domain1")));

        dispatcher = mock(BindingDispatcher.class);
        when(sxpNode.getSvcBindingDispatcher()).thenReturn(dispatcher);

        domain.propagateToSharedDomains(null, null);
        verify(dispatcher, never()).propagateUpdate(anyList(), anyList(), anyList());
        domain.propagateToSharedDomains(null, new ArrayList<>());
        verify(dispatcher, never()).propagateUpdate(anyList(), anyList(), anyList());
        domain.propagateToSharedDomains(new ArrayList<>(), null);
        verify(dispatcher, never()).propagateUpdate(anyList(), anyList(), anyList());
        domain.propagateToSharedDomains(new ArrayList<>(), new ArrayList<>());
        verify(dispatcher, never()).propagateUpdate(anyList(), anyList(), anyList());

        domain.propagateToSharedDomains(Collections.singletonList(mock(MasterDatabaseBinding.class)),
                Collections.singletonList(mock(MasterDatabaseBinding.class)));
        verify(dispatcher).propagateUpdate(anyList(), anyList(), anyList());
    }
}
