/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hazelcast.config.Config;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.HazelcastBackedSxpDB;
import org.opendaylight.sxp.util.database.SxpDatabase;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.filtering.PrefixListFilter;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.BindingDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PrefixListFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.prefix.list.filter.entries.PrefixListEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.prefix.list.filter.entries.PrefixListEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copy-paste from {@link org.opendaylight.sxp.util.database.SxpDatabaseImplTest}
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SxpNode.class})
@PowerMockIgnore("javax.management.*")
public class HazelcastBackedSxpDBIT {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastBackedSxpDBIT.class);

    private static HazelcastBackedSxpDB database;
    private static SxpNode node;
    private static List<SxpConnection> sxpConnections = new ArrayList<>();
    @Rule
    public Timeout globalTimeout = new Timeout(120_000);
    @Rule
    public TestRule watcher = new StartTestWatcher();

    @Before
    public void init() {
        LOG.info("Test init started");
        Config hcConfig = new Config();
        hcConfig.getGroupConfig().setName(UUID.randomUUID().toString());
        database = new HazelcastBackedSxpDB("ACTIVE_BINDINGS", "TENTATIVE_BINDINGS", hcConfig);
        node = PowerMockito.mock(SxpNode.class);
        PowerMockito.when(node.getBindingSxpDatabase()).thenReturn(database);
        PowerMockito.when(node.getAllConnections()).thenReturn(sxpConnections);
        PowerMockito.when(node.getAllConnections(any())).thenReturn(sxpConnections);
        LOG.info("Test init done");
    }

    @After
    public void shutdown() {
        LOG.info("Test finished, shutting down database");
        database.close();
        LOG.info("Database closed, cleanup finished.");
    }

    private SxpConnection mockConnection(String remoteId) {
        SxpConnection connection = mock(SxpConnection.class);
        when(connection.getNodeIdRemote()).thenReturn(NodeId.getDefaultInstance(remoteId));
        when(connection.isModeListener()).thenReturn(true);
        return connection;
    }

    private <T extends SxpBindingFields> T getBinding(String prefix, int sgt, String... peers) {
        SxpDatabaseBindingBuilder bindingBuilder = new SxpDatabaseBindingBuilder();
        bindingBuilder.setIpPrefix(IpPrefixBuilder.getDefaultInstance(prefix));
        bindingBuilder.setSecurityGroupTag(new Sgt(sgt));
        PeerSequenceBuilder sequenceBuilder = new PeerSequenceBuilder();
        sequenceBuilder.setPeer(new ArrayList<>());
        for (int i = 0; i < peers.length; i++) {
            sequenceBuilder.getPeer()
                    .add(new PeerBuilder().setSeq(i).setNodeId(NodeId.getDefaultInstance(peers[i])).build());
        }
        bindingBuilder.setPeerSequence(sequenceBuilder.build());
        return (T) bindingBuilder.build();
    }

    private <T extends SxpBindingFields> List<T> mergeBindings(T... binding) {
        return new ArrayList<>(Arrays.asList(binding));
    }

    private <T extends SxpBindingFields, R extends SxpBindingFields> void assertBindings(
            List<T> bindings1, List<R> bindings2) {
        bindings1.forEach(b -> assertTrue(bindings2.stream().anyMatch(
                r -> Objects.equals(r.getSecurityGroupTag(), b.getSecurityGroupTag())
                        && Objects.equals(r.getIpPrefix(), b.getIpPrefix()))));
    }

    private PrefixListEntry getPrefixListEntry(FilterEntryType entryType, PrefixListMatch prefixListMatch) {
        PrefixListEntryBuilder builder = new PrefixListEntryBuilder();
        builder.setEntryType(entryType);
        builder.setPrefixListMatch(prefixListMatch);
        return builder.build();
    }

    private PrefixListMatch getPrefixListMatch(String prefix) {
        PrefixListMatchBuilder builder = new PrefixListMatchBuilder();
        if (prefix.contains(":")) {
            builder.setIpPrefix(new IpPrefix(Ipv6Prefix.getDefaultInstance(prefix)));
        } else {
            builder.setIpPrefix(new IpPrefix(Ipv4Prefix.getDefaultInstance(prefix)));
        }
        return builder.build();
    }

    @Test
    public void testDeleteBindings() throws Exception {
        assertEquals(0, database.deleteBindings(NodeId.getDefaultInstance("10.10.10.10")).size());
        assertEquals(0, database.deleteBindings(NodeId.getDefaultInstance("10.10.10.10"), new ArrayList<>()).size());

        database.addBinding(NodeId.getDefaultInstance("10.10.10.10"),
                mergeBindings(getBinding("0.0.0.0/0", 5, "10.10.10.10"), getBinding("2.2.2.2/32", 200, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));

        database.addBinding(NodeId.getDefaultInstance("20.20.20.20"),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("1.1.1.1/32", 10, "20.20.20.20")));

        database.addBinding(NodeId.getDefaultInstance("30.30.30.30"),
                mergeBindings(getBinding("25.2.2.6/32", 20, "30.30.30.30", "20.20.20.20", "10.10.10.10"),
                        getBinding("1.1.1.1/32", 10, "30.30.30.30")));

        assertEquals(0, database.deleteBindings(NodeId.getDefaultInstance("10.10.10.10"), new ArrayList<>()).size());
        assertEquals(2, database.deleteBindings(NodeId.getDefaultInstance("10.10.10.10")).size());
        assertEquals(0, database.getBindings(NodeId.getDefaultInstance("10.10.10.10")).size());

        assertEquals(1, database.deleteBindings(NodeId.getDefaultInstance("20.20.20.20"),
                mergeBindings(getBinding("1.1.1.1/32", 10, "20.20.20.20"))).size());
        assertBindings(database.getBindings(NodeId.getDefaultInstance("20.20.20.20")),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10")));

        assertBindings(database.getBindings(NodeId.getDefaultInstance("30.30.30.30")),
                mergeBindings(getBinding("25.2.2.6/32", 20, "30.30.30.30", "20.20.20.20", "10.10.10.10"),
                        getBinding("1.1.1.1/32", 10, "30.30.30.30")));
    }

    @Test
    public void testDeleteBindingsOnBadInputs() {
        List<SxpDatabaseBinding> deleted = database.deleteBindings(null);
        assertTrue(deleted.isEmpty());
        List<SxpDatabaseBinding> deleted2 = database.deleteBindings(null, Collections.EMPTY_LIST);
        assertTrue(deleted2.isEmpty());
        List<SxpDatabaseBinding> deleted3 = database.deleteBindings(NodeId.getDefaultInstance("30.30.30.30"), Collections.EMPTY_LIST);
        assertTrue(deleted3.isEmpty());
        List<SxpDatabaseBinding> deleted4 = database.deleteBindings(NodeId.getDefaultInstance("30.30.30.30"), (List) null);
        assertTrue(deleted4.isEmpty());
        List<SxpDatabaseBinding> deleted5 = database.deleteBindings(null, (List) null);
        assertTrue(deleted5.isEmpty());
    }

    @Test
    public void testGetBindingsOnNullNode() {
        NodeId nodeId = null;
        List<SxpDatabaseBinding> bindings = database.getBindings(nodeId);
        assertTrue(bindings.isEmpty());
    }

    @Test
    public void testAddBindingsToNullNode() {
        NodeId nodeId = null;
        List<SxpDatabaseBinding> bindings = database.addBinding(nodeId, Collections.EMPTY_LIST);
        assertTrue(bindings.isEmpty());
    }

    @Test
    public void testAddBindingsWithNull() {
        NodeId nodeId = NodeId.getDefaultInstance("20.20.20.20");
        List<SxpDatabaseBinding> bindings = database.addBinding(nodeId, null);
        assertTrue(bindings.isEmpty());
    }

    @Test
    public void testAddBindingsWithEmptyList() {
        NodeId nodeId = NodeId.getDefaultInstance("20.20.20.20");
        List<SxpDatabaseBinding> bindings = database.addBinding(nodeId, Collections.EMPTY_LIST);
        assertTrue(bindings.isEmpty());
    }

    @Test
    public void testSetReconcilliationOnNullNode() {
        NodeId nodeId = null;
        database.setReconciliation(nodeId);
    }

    @Test
    public void testReconcileBindingsOnNullNode() {
        NodeId nodeId = null;
        Collection<SxpDatabaseBinding> reconciled = database.reconcileBindings(nodeId);
        assertTrue(reconciled.isEmpty());
    }

    @Test
    public void testGetInboundFilters() {
        SxpConnection lConnectionMock = mockConnection("127.0.0.2");
        SxpConnection sConnectionMock = mockConnection("127.0.0.2");
        when(sConnectionMock.isModeListener()).thenReturn(Boolean.FALSE);
        sxpConnections.add(lConnectionMock);
        sxpConnections.add(sConnectionMock);

        Map<NodeId, SxpBindingFilter> inboundFilters = SxpDatabaseImpl.getInboundFilters(node, "test");
        assertTrue(!inboundFilters.isEmpty());
    }

    @Test
    public void testFilterDatabase() throws Exception {
        List<PrefixListEntry> prefixListEntryList = new ArrayList<>();
        PrefixListFilterEntriesBuilder builder = new PrefixListFilterEntriesBuilder();
        builder.setPrefixListEntry(prefixListEntryList);
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Permit, getPrefixListMatch("127.0.0.0/16")));
        SxpFilterBuilder filterBuilder = new SxpFilterBuilder();
        filterBuilder.setFilterType(FilterType.Inbound);
        filterBuilder.setFilterEntries(builder.build());
        PrefixListFilter filter = new PrefixListFilter(filterBuilder.build(), "TEST");

        //Fill DB
        database.addBinding(NodeId.getDefaultInstance("127.0.0.1"),
                mergeBindings(getBinding("127.25.0.1/32", 10, "127.0.0.1"),
                        getBinding("127.0.0.15/32", 20, "127.0.0.1", "127.0.1.0"),
                        getBinding("2001:0:0:0:0:0:0:1/128", 30, "127.0.0.5", "127.0.0.10")));
        //DB to be removed
        assertBindings(SxpDatabase.filterDatabase(database, NodeId.getDefaultInstance("127.0.0.1"), filter),
                mergeBindings(getBinding("127.25.0.1/32", 10), getBinding("2001:0:0:0:0:0:0:1/128", 30)));
    }

    @Test
    public void testFilterDatabaseOnNullNode() {
        List<SxpDatabaseBinding> filtered = SxpDatabaseImpl.filterDatabase(database, null, null);
        assertTrue(filtered.isEmpty());
    }

    @Test
    public void testFilterDatabaseWithNullFilter() {
        NodeId nodeId = NodeId.getDefaultInstance("10.10.10.10");
        List<SxpDatabaseBinding> filtered = SxpDatabaseImpl.filterDatabase(database, nodeId, null);
        assertTrue(filtered.isEmpty());
    }

    @Test
    public void testGetReplaceForBindings() throws Exception {
        database.addBinding(NodeId.getDefaultInstance("10.10.10.10"),
                mergeBindings(getBinding("0.0.0.0/0", 5, "10.10.10.10"), getBinding("2.2.2.2/32", 200, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));

        database.addBinding(NodeId.getDefaultInstance("20.20.20.20"),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("1.1.1.1/32", 10, "20.20.20.20")));

        database.addBinding(NodeId.getDefaultInstance("30.30.30.30"),
                mergeBindings(getBinding("25.2.2.6/32", 20, "30.30.30.30", "20.20.20.20", "10.10.10.10"),
                        getBinding("1.1.1.1/32", 10, "30.30.30.30")));

        sxpConnections.add(mockConnection("10.10.10.10"));
        sxpConnections.add(mockConnection("20.20.20.20"));
        sxpConnections.add(mockConnection("30.30.30.30"));


        database.deleteBindings(NodeId.getDefaultInstance("10.10.10.10"), mergeBindings(getBinding("2.2.2.2/32", 200)));
        assertBindings(SxpDatabase.getReplaceForBindings(mergeBindings(), database,
                SxpDatabase.getInboundFilters(node, "global")), mergeBindings());

        assertBindings(SxpDatabase.getReplaceForBindings(mergeBindings(getBinding("2.2.2.2/32", 200)), database,
                SxpDatabase.getInboundFilters(node, "global")), mergeBindings(getBinding("2.2.2.2/32", 20)));

        database.deleteBindings(NodeId.getDefaultInstance("20.20.20.20"), mergeBindings(getBinding("2.2.2.2/32", 20)));

        assertBindings(SxpDatabase.getReplaceForBindings(mergeBindings(getBinding("2.2.2.2/32", 20)), database,
                SxpDatabase.getInboundFilters(node, "global")), mergeBindings(getBinding("2.2.2.2/32", 200)));
        assertBindings(SxpDatabase.getReplaceForBindings(mergeBindings(getBinding("2.2.2.2/32", 254)), database,
                SxpDatabase.getInboundFilters(node, "global")), mergeBindings(getBinding("2.2.2.2/32", 200)));
        assertBindings(SxpDatabase.getReplaceForBindings(mergeBindings(getBinding("25.2.2.2/32", 20)), database,
                SxpDatabase.getInboundFilters(node, "global")), mergeBindings());
    }

    @Test
    public void testGetReplaceForBindingsWithNullInputs() throws Exception {
        assertTrue(SxpDatabase.getReplaceForBindings(null, null, null).isEmpty());
        assertTrue(SxpDatabase.getReplaceForBindings(Collections.EMPTY_LIST, database, null).isEmpty());
        assertTrue(SxpDatabase.getReplaceForBindings(Collections.EMPTY_LIST, null, null).isEmpty());
        assertTrue(SxpDatabase.getReplaceForBindings(null, database, null).isEmpty());
    }

    @Test
    public void testAddBinding() throws Exception {
        assertEquals(0, database.addBinding(NodeId.getDefaultInstance("1.1.1.1"), mergeBindings()).size());
        assertEquals(0, database.getBindings().size());

        database.addBinding(NodeId.getDefaultInstance("10.10.10.10"),
                mergeBindings(getBinding("0.0.0.0/0", 5, "10.10.10.10"), getBinding("1.1.1.1/32", 10, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));

        database.addBinding(NodeId.getDefaultInstance("20.20.20.20"),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20")));

        assertBindings(database.getBindings(),
                mergeBindings(getBinding("1.1.1.1/32", 10, "10.10.10.10"), getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                        getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20")));

        assertBindings(database.getBindings(NodeId.getDefaultInstance("10.10.10.10")),
                mergeBindings(getBinding("1.1.1.1/32", 10, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));
        assertBindings(database.getBindings(NodeId.getDefaultInstance("20.20.20.20")),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20")));
    }

    @Test
    public void testReconcileBindings() throws Exception {
        database.addBinding(NodeId.getDefaultInstance("10.10.10.10"),
                mergeBindings(getBinding("0.0.0.0/0", 5, "10.10.10.10"), getBinding("1.1.1.1/32", 10, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));

        database.addBinding(NodeId.getDefaultInstance("20.20.20.20"),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20")));

        database.setReconciliation(NodeId.getDefaultInstance("50.50.50.50"));

        assertBindings(database.getBindings(NodeId.getDefaultInstance("10.10.10.10")),
                mergeBindings(getBinding("1.1.1.1/32", 10, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));
        assertBindings(database.getBindings(NodeId.getDefaultInstance("20.20.20.20")),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20")));

        database.setReconciliation(NodeId.getDefaultInstance("10.10.10.10"));
        database.reconcileBindings(NodeId.getDefaultInstance("20.20.20.20"));
        database.reconcileBindings(NodeId.getDefaultInstance("50.50.50.50"));

        assertBindings(database.getBindings(NodeId.getDefaultInstance("10.10.10.10")),
                mergeBindings(getBinding("1.1.1.1/32", 10, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));
        assertBindings(database.getBindings(NodeId.getDefaultInstance("20.20.20.20")),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20")));

        database.reconcileBindings(NodeId.getDefaultInstance("10.10.10.10"));

        assertEquals(0, database.getBindings(NodeId.getDefaultInstance("10.10.10.10")).size());
        assertBindings(database.getBindings(NodeId.getDefaultInstance("20.20.20.20")),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20")));
    }

    @Test
    public void testPutBindings() {
        NodeId nodeId = new NodeId("127.0.0.1");
        boolean resultOfPut = database.putBindings(nodeId, BindingDatabase.BindingType.ActiveBindings, mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                getBinding("2.2.2.2/32", 200, "20.20.20.20")));
        assertTrue(resultOfPut);
        boolean resultOfPut2 = database.putBindings(nodeId, BindingDatabase.BindingType.ActiveBindings, Collections.EMPTY_LIST);
        assertTrue(!resultOfPut2);
    }

    private static class StartTestWatcher extends TestWatcher {
        protected void starting(Description description) {
            LOG.info("Starting test: {}", description.getMethodName());
        }
    }
}

