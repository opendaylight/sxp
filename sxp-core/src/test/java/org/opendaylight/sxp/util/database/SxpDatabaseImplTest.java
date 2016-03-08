/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.sxp.util.filtering.PrefixListFilter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.PrefixListFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.prefix.list.filter.entries.PrefixListEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.prefix.list.filter.entries.PrefixListEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SxpDatabaseImplTest {

    private static SxpDatabaseImpl database;

    @Before public void init() {
        database = new SxpDatabaseImpl();
    }

    private <T extends SxpBindingFields> T getBinding(String prefix, int sgt, String... peers) {
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        bindingBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
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

    private <T extends SxpBindingFields, R extends SxpBindingFields> void assertBindings(List<T> bindings1,
            List<R> bindings2) {
        bindings1.stream()
                .forEach(b -> assertTrue(bindings2.stream()
                        .anyMatch(r -> r.getSecurityGroupTag().getValue().equals(b.getSecurityGroupTag().getValue())
                                && Arrays.equals(r.getIpPrefix().getValue(), b.getIpPrefix().getValue()))));
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

    @Test public void testDeleteBindings() throws Exception {
        assertEquals(0, database.deleteBindings(NodeId.getDefaultInstance("10.10.10.10")).size());
        assertEquals(0,
                database.deleteBindings(NodeId.getDefaultInstance("10.10.10.10"), new ArrayList<>())
                        .size());

        database.addBinding(NodeId.getDefaultInstance("10.10.10.10"),
                mergeBindings(getBinding("0.0.0.0/0", 5, "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));

        database.addBinding(NodeId.getDefaultInstance("20.20.20.20"),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("1.1.1.1/32", 10, "20.20.20.20")));

        database.addBinding(NodeId.getDefaultInstance("30.30.30.30"),
                mergeBindings(getBinding("25.2.2.6/32", 20, "30.30.30.30", "20.20.20.20", "10.10.10.10"),
                        getBinding("1.1.1.1/32", 10, "30.30.30.30")));

        assertEquals(0,
                database.deleteBindings(NodeId.getDefaultInstance("10.10.10.10"), new ArrayList<>())
                        .size());
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

    @Test public void testFilterDatabase() throws Exception {
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

    @Test public void testGetReplaceForBindings() throws Exception {
        database.addBinding(NodeId.getDefaultInstance("10.10.10.10"),
                mergeBindings(getBinding("0.0.0.0/0", 5, "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));

        database.addBinding(NodeId.getDefaultInstance("20.20.20.20"),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("1.1.1.1/32", 10, "20.20.20.20")));

        database.addBinding(NodeId.getDefaultInstance("30.30.30.30"),
                mergeBindings(getBinding("25.2.2.6/32", 20, "30.30.30.30", "20.20.20.20", "10.10.10.10"),
                        getBinding("1.1.1.1/32", 10, "30.30.30.30")));

        database.deleteBindings(NodeId.getDefaultInstance("10.10.10.10"), mergeBindings(getBinding("2.2.2.2/32", 200)));
        assertBindings(SxpDatabase.getReplaceForBindings(database, mergeBindings()), mergeBindings());

        assertBindings(SxpDatabase.getReplaceForBindings(database, mergeBindings(getBinding("2.2.2.2/32", 200))),
                mergeBindings(getBinding("2.2.2.2/32", 20)));

        database.deleteBindings(NodeId.getDefaultInstance("20.20.20.20"), mergeBindings(getBinding("2.2.2.2/32", 20)));

        assertBindings(SxpDatabase.getReplaceForBindings(database, mergeBindings(getBinding("2.2.2.2/32", 20))),
                mergeBindings(getBinding("2.2.2.2/32", 200)));
        assertBindings(SxpDatabase.getReplaceForBindings(database, mergeBindings(getBinding("2.2.2.2/32", 254))),
                mergeBindings(getBinding("2.2.2.2/32", 200)));
        assertBindings(SxpDatabase.getReplaceForBindings(database, mergeBindings(getBinding("25.2.2.2/32", 20))),
                mergeBindings());
    }

    @Test public void testAddBinding() throws Exception {
        assertEquals(0, database.addBinding(NodeId.getDefaultInstance("1.1.1.1"), mergeBindings()).size());
        assertEquals(0, database.getBindings().size());

        database.addBinding(NodeId.getDefaultInstance("10.10.10.10"),
                mergeBindings(getBinding("0.0.0.0/0", 5, "10.10.10.10"), getBinding("1.1.1.1/32", 10, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));

        database.addBinding(NodeId.getDefaultInstance("20.20.20.20"),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20")));

        assertBindings(database.getBindings(), mergeBindings(getBinding("1.1.1.1/32", 10, "10.10.10.10"),
                getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                getBinding("2.2.2.2/32", 200, "20.20.20.20")));

        assertBindings(database.getBindings(NodeId.getDefaultInstance("10.10.10.10")),
                mergeBindings(getBinding("1.1.1.1/32", 10, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));
        assertBindings(database.getBindings(NodeId.getDefaultInstance("20.20.20.20")),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20")));
    }

    @Test public void testReconcileBindings() throws Exception {
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

    @Test public void testToString() throws Exception {
        assertEquals("SxpDatabaseImpl\n", database.toString());

        database.addBinding(NodeId.getDefaultInstance("10.10.10.10"),
                mergeBindings(getBinding("0.0.0.0/0", 5, "10.10.10.10"), getBinding("1.1.1.1/32", 10, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));

        database.addBinding(NodeId.getDefaultInstance("20.20.20.20"),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20")));
        assertEquals("SxpDatabaseImpl\n" + "\t10 1.1.1.1/32\n" + "\t100 1.1.1.1/32\n" + "\t20 2.2.2.2/32\n"
                + "\t200 2.2.2.2/32\n", database.toString());
    }
}
