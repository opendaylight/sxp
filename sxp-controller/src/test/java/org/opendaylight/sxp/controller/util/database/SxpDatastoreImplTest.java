/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.BindingDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.BindingDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.BindingDatabaseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.BindingSourcesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.BindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.BindingSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.BindingSourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.SxpDatabaseBindingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;

@RunWith(PowerMockRunner.class) @PrepareForTest({DatastoreAccess.class}) public class SxpDatastoreImplTest {

    private static SxpDatastoreImpl database;
    private static DatastoreAccess access;
    private static Map<NodeId, List<SxpDatabaseBinding>> databaseBindings_Active = new HashMap<>();
    private static Map<NodeId, List<SxpDatabaseBinding>> databaseBindings_Reconciled = new HashMap<>();

    @BeforeClass public static void initClass() {
        access = PowerMockito.mock(DatastoreAccess.class);
        PowerMockito.when(access.mergeSynchronous(any(InstanceIdentifier.class), any(BindingSource.class),
                any(LogicalDatastoreType.class))).then(invocation -> {
            InstanceIdentifier identifier = (InstanceIdentifier) invocation.getArguments()[0];
            if (identifier.getTargetType() == BindingSource.class) {
                Map<NodeId, List<SxpDatabaseBinding>>
                        map =
                        ((BindingDatabaseKey) identifier.firstKeyOf(BindingDatabase.class)).getBindingType()
                                == BindingDatabase.BindingType.ActiveBindings ? databaseBindings_Active : databaseBindings_Reconciled;
                NodeId nodeId = ((BindingSourceKey) identifier.firstKeyOf(BindingSource.class)).getSourceId();

                if (!map.containsKey(nodeId)) {
                    map.put(nodeId, new ArrayList<>());
                }
                map.get(nodeId)
                        .addAll(((BindingSource) invocation.getArguments()[1]).getSxpDatabaseBindings()
                                .getSxpDatabaseBinding());
            }
            return null;
        });
        PowerMockito.when(access.putSynchronous(any(InstanceIdentifier.class), any(MasterDatabase.class),
                any(LogicalDatastoreType.class))).then(invocation -> {
            InstanceIdentifier identifier = (InstanceIdentifier) invocation.getArguments()[0];
            if (identifier.getTargetType() == BindingSource.class) {
                Map<NodeId, List<SxpDatabaseBinding>>
                        map =
                        ((BindingDatabaseKey) identifier.firstKeyOf(BindingDatabase.class)).getBindingType()
                                == BindingDatabase.BindingType.ActiveBindings ? databaseBindings_Active : databaseBindings_Reconciled;
                NodeId nodeId = ((BindingSourceKey) identifier.firstKeyOf(BindingSource.class)).getSourceId();

                if (!map.containsKey(nodeId)) {
                    map.put(nodeId, new ArrayList<>());
                }
                map.get(nodeId).clear();
                map.get(nodeId)
                        .addAll(((BindingSource) invocation.getArguments()[1]).getSxpDatabaseBindings()
                                .getSxpDatabaseBinding());
            }
            return null;
        });
        PowerMockito.when(access.checkAndDelete(any(InstanceIdentifier.class), any(LogicalDatastoreType.class)))
                .then(invocation -> {
                    InstanceIdentifier identifier = (InstanceIdentifier) invocation.getArguments()[0];
                    if (identifier.getTargetType() == BindingSource.class) {
                        Map<NodeId, List<SxpDatabaseBinding>>
                                map =
                                ((BindingDatabaseKey) identifier.firstKeyOf(BindingDatabase.class)).getBindingType()
                                        == BindingDatabase.BindingType.ActiveBindings ? databaseBindings_Active : databaseBindings_Reconciled;
                        NodeId nodeId = ((BindingSourceKey) identifier.firstKeyOf(BindingSource.class)).getSourceId();

                        if (map.containsKey(nodeId)) {
                            map.get(nodeId).clear();
                        }
                    }
                    return null;
                });
        PowerMockito.when(access.readSynchronous(any(InstanceIdentifier.class), any(LogicalDatastoreType.class)))
                .then(invocation -> {
                    InstanceIdentifier identifier = (InstanceIdentifier) invocation.getArguments()[0];
                    Map<NodeId, List<SxpDatabaseBinding>>
                            map =
                            ((BindingDatabaseKey) identifier.firstKeyOf(BindingDatabase.class)).getBindingType()
                                    == BindingDatabase.BindingType.ActiveBindings ? databaseBindings_Active : databaseBindings_Reconciled;
                    if (identifier.getTargetType() == BindingSource.class) {
                        NodeId nodeId = ((BindingSourceKey) identifier.firstKeyOf(BindingSource.class)).getSourceId();
                        return new BindingSourceBuilder().setSourceId(nodeId)
                                .setSxpDatabaseBindings(new SxpDatabaseBindingsBuilder().setSxpDatabaseBinding(
                                        map.containsKey(nodeId) ? map.get(nodeId) : new ArrayList<SxpDatabaseBinding>())
                                        .build())
                                .build();
                    } else if (identifier.getTargetType() == BindingDatabase.class) {
                        List<BindingSource> bindingSources = new ArrayList<BindingSource>();
                        map.entrySet().stream().forEach(e -> {
                            bindingSources.add(new BindingSourceBuilder().setSourceId(e.getKey())
                                    .setSxpDatabaseBindings(
                                            new SxpDatabaseBindingsBuilder().setSxpDatabaseBinding(e.getValue())
                                                    .build())
                                    .build());
                        });
                        return new BindingDatabaseBuilder().setBindingSources(
                                new BindingSourcesBuilder().setBindingSource(bindingSources).build()).build();
                    }
                    return null;
                });
    }

    @Before public void init() {
        databaseBindings_Reconciled.clear();
        databaseBindings_Active.clear();
        database = new SxpDatastoreImpl(access, "0.0.0.0");
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

    @Test public void testDeleteBindings() throws Exception {
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

    @Test public void testAddBinding() throws Exception {
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
        assertEquals("SxpDatastoreImpl\n", database.toString());

        database.addBinding(NodeId.getDefaultInstance("10.10.10.10"),
                mergeBindings(getBinding("0.0.0.0/0", 5, "10.10.10.10"), getBinding("1.1.1.1/32", 10, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10")));

        database.addBinding(NodeId.getDefaultInstance("20.20.20.20"),
                mergeBindings(getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20")));
        assertEquals("SxpDatastoreImpl\n" + "\t10 1.1.1.1/32\n" + "\t100 1.1.1.1/32\n" + "\t20 2.2.2.2/32\n"
                + "\t200 2.2.2.2/32\n", database.toString());
    }
}
