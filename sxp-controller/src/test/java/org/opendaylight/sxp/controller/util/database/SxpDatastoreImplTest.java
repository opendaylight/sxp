/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
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
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SxpDatastoreImplTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;

    private SxpDatastoreImpl database;
    private Map<NodeId, List<SxpDatabaseBinding>> databaseBindings_Active = new HashMap<>();
    private Map<NodeId, List<SxpDatabaseBinding>> databaseBindings_Reconciled = new HashMap<>();

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        DatastoreAccess access = prepareDataStore(dataBroker, readTransaction, writeTransaction);
        databaseBindings_Reconciled.clear();
        databaseBindings_Active.clear();
        database = new SxpDatastoreImpl(access, "0.0.0.0", "DOMAIN");
    }

    private <T extends SxpBindingFields> T getBinding(String prefix, int sgt, String... peers) {
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
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
        bindings2.forEach(b -> assertTrue(bindings1.stream().anyMatch(
                r -> Objects.equals(r.getSecurityGroupTag(), b.getSecurityGroupTag())
                        && Objects.equals(r.getIpPrefix(), b.getIpPrefix()))));
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
    public void testToString() throws Exception {
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

    /**
     * Prepare {@link DatastoreAccess} mock instance backed by {@link DataBroker} for tests.
     * <p>
     * {@link ReadTransaction} and {@link WriteTransaction} are assumed to be created by {@link TransactionChain}.
     * <p>
     * {@link WriteTransaction} writes or deletes SXP database bindings into a map.
     * <p>
     * {@link ReadTransaction} reads saved SXP database bindings from a map.
     * <p>
     * {@link WriteTransaction} is committed successfully.
     *
     * @param dataBroker mock of {@link DataBroker}
     * @param readTransaction mock of {@link ReadTransaction}
     * @param writeTransaction mock of {@link WriteTransaction}
     * @return mock of {@link DatastoreAccess}
     */
    private DatastoreAccess prepareDataStore(DataBroker dataBroker, ReadTransaction readTransaction,
            WriteTransaction writeTransaction) {
        TransactionChain transactionChain = mock(TransactionChain.class);
        doReturn(CommitInfo.emptyFluentFuture())
                .when(writeTransaction).commit();
        prepareTransactionsAnswers(readTransaction, writeTransaction);
        when(transactionChain.newReadOnlyTransaction())
                .thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction())
                .thenReturn(writeTransaction);
        when(dataBroker.createTransactionChain(any(TransactionChainListener.class)))
                .thenReturn(transactionChain);

        return DatastoreAccess.getInstance(dataBroker);
    }

    private void prepareTransactionsAnswers(ReadTransaction readTransaction, WriteTransaction writeTransaction) {
        doAnswer(invocation -> {
            InstanceIdentifier identifier = invocation.getArgument(1);
            if (identifier.getTargetType() == BindingSource.class) {
                Map<NodeId, List<SxpDatabaseBinding>> map = ((BindingDatabaseKey) identifier.firstKeyOf(
                        BindingDatabase.class)).getBindingType() == BindingDatabase.BindingType.ActiveBindings
                        ? databaseBindings_Active : databaseBindings_Reconciled;
                NodeId nodeId = ((BindingSourceKey) identifier.firstKeyOf(BindingSource.class)).getSourceId();

                if (!map.containsKey(nodeId)) {
                    map.put(nodeId, new ArrayList<>());
                }
                map.get(nodeId)
                        .addAll(((BindingSource) invocation.getArgument(2)).getSxpDatabaseBindings()
                                .getSxpDatabaseBinding());
            }
            return null;
        }).when(writeTransaction)
                .merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(BindingSource.class));

        doAnswer(invocation -> {
            InstanceIdentifier identifier = invocation.getArgument(1);
            if (identifier.getTargetType() == BindingSource.class) {
                Map<NodeId, List<SxpDatabaseBinding>>
                        map = ((BindingDatabaseKey) identifier.firstKeyOf(
                        BindingDatabase.class)).getBindingType() == BindingDatabase.BindingType.ActiveBindings
                        ? databaseBindings_Active : databaseBindings_Reconciled;
                NodeId nodeId = ((BindingSourceKey) identifier.firstKeyOf(BindingSource.class)).getSourceId();

                if (!map.containsKey(nodeId)) {
                    map.put(nodeId, new ArrayList<>());
                }
                map.get(nodeId).clear();
                map.get(nodeId)
                        .addAll(((BindingSource) invocation.getArgument(2)).getSxpDatabaseBindings()
                                .getSxpDatabaseBinding());
            }
            return null;
        }).when(writeTransaction)
                .put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(MasterDatabase.class));

        doAnswer(invocation -> {
            InstanceIdentifier identifier = invocation.getArgument(1);
            if (identifier.getTargetType() == BindingSource.class) {
                Map<NodeId, List<SxpDatabaseBinding>>
                        map =
                        ((BindingDatabaseKey) identifier.firstKeyOf(BindingDatabase.class))
                                .getBindingType() == BindingDatabase.BindingType.ActiveBindings
                                ? databaseBindings_Active : databaseBindings_Reconciled;
                NodeId nodeId = ((BindingSourceKey) identifier.firstKeyOf(BindingSource.class)).getSourceId();

                if (map.containsKey(nodeId)) {
                    map.get(nodeId).clear();
                }
            }
            return null;
        }).when(writeTransaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));

        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .then(invocation -> {
                    InstanceIdentifier identifier = invocation.getArgument(1);
                    Map<NodeId, List<SxpDatabaseBinding>>
                            map = ((BindingDatabaseKey) identifier.firstKeyOf(
                            BindingDatabase.class)).getBindingType() == BindingDatabase.BindingType.ActiveBindings
                            ? databaseBindings_Active : databaseBindings_Reconciled;
                    if (identifier.getTargetType() == BindingSource.class) {
                        NodeId nodeId = ((BindingSourceKey) identifier.firstKeyOf(BindingSource.class)).getSourceId();
                        Optional<BindingSource> bindingSource = Optional
                                .of(new BindingSourceBuilder().setSourceId(nodeId)
                                        .setSxpDatabaseBindings(new SxpDatabaseBindingsBuilder().setSxpDatabaseBinding(
                                                map.containsKey(nodeId) ? map.get(nodeId) : new ArrayList<>())
                                                .build())
                                        .build());
                        return FluentFutures.immediateFluentFuture(bindingSource);
                    } else if (identifier.getTargetType() == BindingDatabase.class) {
                        List<BindingSource> bindingSources = new ArrayList<>();
                        map.forEach((key, value) -> bindingSources.add(new BindingSourceBuilder()
                                .setSourceId(key)
                                .setSxpDatabaseBindings(
                                        new SxpDatabaseBindingsBuilder().setSxpDatabaseBinding(value)
                                                .build())
                                .build()));
                        Optional<BindingDatabase> bindingDatabase = Optional
                                .of(new BindingDatabaseBuilder().setBindingSources(
                                        new BindingSourcesBuilder().setBindingSource(bindingSources).build()).build());
                        return FluentFutures.immediateFluentFuture(bindingDatabase);
                    }
                    return FluentFutures.immediateFluentFuture(Optional.of(mock(identifier.getTargetType())));
                });
    }
}
