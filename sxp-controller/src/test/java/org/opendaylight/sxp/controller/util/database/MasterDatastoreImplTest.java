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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MasterDatastoreImplTest {

    @Mock
    private SxpDomain domainMock;
    @Mock
    private SxpNode sxpNode;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;

    private MasterDatastoreImpl database;
    private Map<IpPrefix, MasterDatabaseBinding> databaseBindings_Op = new HashMap<>();

    private long time = System.currentTimeMillis();

    @BeforeClass
    public static void initClass() {
        BindingOriginsConfig.INSTANCE.addBindingOrigins(BindingOriginsConfig.DEFAULT_ORIGIN_PRIORITIES);
    }

    @AfterClass
    public static void tearDown() {
        BindingOriginsConfig.INSTANCE.deleteConfiguration();
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(sxpNode.getWorker()).thenReturn(new ThreadsWorker());
        DatastoreAccess access = prepareDataStore(dataBroker, readTransaction, writeTransaction);
        database = new MasterDatastoreImpl(access, "0.0.0.0", "DOMAIN");
        database.initDBPropagatingListener(new BindingDispatcher(sxpNode), domainMock);
    }

    private <T extends SxpBindingFields> T getBinding(String prefix, int sgt, String... peers) {
        return getBinding(prefix, sgt, BindingOriginsConfig.LOCAL_ORIGIN, peers);
    }

    private <T extends SxpBindingFields> T getNetworkBinding(String prefix, int sgt, String... peers) {
        return getBinding(prefix, sgt, BindingOriginsConfig.NETWORK_ORIGIN, peers);
    }

    private <T extends SxpBindingFields> T getBinding(String prefix, int sgt, OriginType origin, String... peers) {
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        bindingBuilder.setIpPrefix(IpPrefixBuilder.getDefaultInstance(prefix));
        bindingBuilder.setSecurityGroupTag(new Sgt(sgt));
        bindingBuilder.setTimestamp(TimeConv.toDt(time += 1000));
        bindingBuilder.setOrigin(origin);
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
                        && Objects.equals(r.getIpPrefix(), b.getIpPrefix())
                        && Objects.equals(r.getOrigin(), (b.getOrigin())))));
    }

    @Test
    public void testAddBindings() throws Exception {
        assertEquals(0, database.addBindings(mergeBindings()).size());
        assertEquals(0, database.getBindings().size());

        List<SxpBindingFields>
                toAdd =
                mergeBindings(getBinding("0.0.0.0/0", 5, "10.10.10.10"), getBinding("1.1.1.1/32", 10, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                        getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20"));

        assertBindings(database.addBindings(toAdd), mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 200, "20.20.20.20")));
        assertEquals(2, database.getBindings().size());
        assertBindings(database.getBindings(), mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 200, "20.20.20.20")));

        assertEquals(0, database.addBindings(toAdd).size());
        assertEquals(2, database.getBindings().size());
        assertBindings(database.getBindings(), mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 200, "20.20.20.20")));

        toAdd.clear();
        toAdd =
                mergeBindings(getBinding("15.15.15.15/24", 15, "0.10.10.10"),
                        getBinding("2.2.2.2/32", 2000, "200.200.200.200"));

        assertEquals(2, database.addBindings(toAdd).size());
        assertEquals(3, database.getBindings().size());
        assertBindings(database.getBindings(), mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 2000, "20.20.20.20"), getBinding("15.15.15.15/24", 15, "0.10.10.10"),
                getBinding("2.2.2.2/32", 2000, "200.200.200.200")));
    }

    @Test
    public void testAddLowerPriorityBinding() {
        final String prefix = "1.1.1.1/32";
        final int sgt = 20;
        final String peer = "10.10.10.10";

        // add local binding
        final SxpBindingFields localBinding = getBinding(prefix, sgt, peer);
        assertBindings(database.addBindings(Collections.singletonList(localBinding)),
                Collections.singletonList(localBinding));
        assertBindings(database.getBindings(), Collections.singletonList(localBinding));

        // add binding of lower priority - lower priority binding is not added
        final SxpBindingFields networkBinding = getNetworkBinding(prefix, sgt, peer);
        assertTrue(database.addBindings(Collections.singletonList(networkBinding)).isEmpty());

        // the previous binding is still in database
        assertBindings(database.getBindings(), Collections.singletonList(localBinding));
    }

    @Test
    public void testAddHigherPriorityBinding() {
        final String prefix = "1.1.1.1/32";
        final int sgt = 20;
        final String peer = "10.10.10.10";

        // add network binding
        final SxpBindingFields networkBinding = getNetworkBinding(prefix, sgt, peer);
        assertBindings(database.addBindings(Collections.singletonList(networkBinding)),
                Collections.singletonList(networkBinding));
        assertBindings(database.getBindings(), Collections.singletonList(networkBinding));

        // add binding of higher priority - previous lower priority binding is replaced
        final SxpBindingFields localBinding = getBinding(prefix, sgt, peer);
        assertBindings(database.addBindings(Collections.singletonList(localBinding)),
                Collections.singletonList(localBinding));

        // the new binding replaced previous one
        assertBindings(database.getBindings(), Collections.singletonList(localBinding));
    }

    @Test
    public void testAddBindingsWithDifferentPriorities() {
        // add two bindings with different priorities for the same ip prefix at once
        final String prefix = "1.1.1.1/32";
        final int sgt = 20;
        final SxpBindingFields networkBinding = getNetworkBinding(prefix, sgt);
        final SxpBindingFields localBinding = getBinding(prefix, sgt);

        // only the binding with higher priority should be added
        assertEquals(1, database.addBindings(mergeBindings(networkBinding, localBinding)).size());
        assertBindings(database.getBindings(), Collections.singletonList(localBinding));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddBindingsNotDefinedPriority() {
        final SxpBindingFields binding = getBinding("1.1.1.1/32", 10, new OriginType("CLUSTER"));
        database.addBindings(Collections.singletonList(binding));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddBindingsFirstBindingWithNullOrigin() {
        database.addBindings(Collections.singletonList(getBinding("1.1.1.1/32", 10, null, new String[0])));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddBindingsNextBindingWithNullOrigin() {
        final String prefix = "1.1.1.1/32";
        final int sgt = 20;
        final SxpBindingFields localBinding = getBinding(prefix, sgt);
        final SxpBindingFields bindingWithNullOrigin = getBinding(prefix, sgt, null, new String[0]);
        assertEquals(1, database.addBindings(Collections.singletonList(localBinding)).size());
        database.addBindings(Collections.singletonList(bindingWithNullOrigin));
    }

    @Test
    public void testDeleteBindings() throws Exception {
        database.addBindings(mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 2000, "20.20.20.20"), getBinding("15.15.15.15/24", 15, "0.10.10.10"),
                getBinding("2.2.2.20/32", 2000, "200.200.200.200")));

        assertEquals(0, database.deleteBindings(mergeBindings()).size());
        assertEquals(4, database.getBindings().size());

        assertBindings(database.deleteBindings(mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 2000, "20.20.20.20"))),
                mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                        getBinding("2.2.2.2/32", 2000, "20.20.20.20")));
        assertBindings(database.getBindings(), mergeBindings(getBinding("15.15.15.15/24", 15, "0.10.10.10"),
                getBinding("2.2.2.20/32", 2000, "200.200.200.200")));

        assertBindings(database.deleteBindings(mergeBindings(getBinding("15.15.15.15/24", 15, "0.10.10.10"),
                getBinding("2.2.2.20/32", 2000, "200.200.200.200"))),
                mergeBindings(getBinding("15.15.15.15/24", 15, "0.10.10.10"),
                        getBinding("2.2.2.20/32", 2000, "200.200.200.200")));
        assertEquals(0, database.getBindings().size());
    }

    @Test
    public void testDeleteNotEqualPrefix() {
        final String prefix = "1.1.1.1/32";
        final int sgt = 20;
        final String peer = "10.10.10.10";

        // add binding
        final SxpBindingFields localBinding = getBinding(prefix, sgt, peer);
        assertBindings(database.addBindings(Collections.singletonList(localBinding)),
                Collections.singletonList(localBinding));
        assertBindings(database.getBindings(), Collections.singletonList(localBinding));

        // try to delete binding with non existing ip prefix
        assertTrue(database.deleteBindings(Collections.singletonList(getBinding("2.2.2.2/32", sgt, peer))).isEmpty());

        // verify binding was not deleted
        assertBindings(database.getBindings(), Collections.singletonList(localBinding));
    }

    @Test
    public void testDeleteNotEqualSgt() {
        final String prefix = "1.1.1.1/32";
        final int sgt = 20;
        final String peer = "10.10.10.10";

        // add binding
        final SxpBindingFields localBinding = getBinding(prefix, sgt, peer);
        assertBindings(database.addBindings(Collections.singletonList(localBinding)),
                Collections.singletonList(localBinding));
        assertBindings(database.getBindings(), Collections.singletonList(localBinding));

        // try to delete binding with non existing sgt
        assertTrue(database.deleteBindings(Collections.singletonList(getBinding(prefix, 50, peer))).isEmpty());

        // verify binding was not deleted
        assertEquals(1, database.getBindings().size());
        assertBindings(database.getBindings(), Collections.singletonList(localBinding));
    }

    @Test
    public void testGetBindings() {
        final SxpBindingFields localBinding = getBinding("1.1.1.1/32", 10);
        final SxpBindingFields networkBinding = getNetworkBinding("2.2.2.2/32", 20);
        assertBindings(database.addBindings(mergeBindings(localBinding, networkBinding)),
                mergeBindings(localBinding, networkBinding));

        // get all bindings
        assertBindings(database.getBindings(), mergeBindings(localBinding, networkBinding));
        // get local bindings
        assertBindings(database.getBindings(BindingOriginsConfig.LOCAL_ORIGIN),
                Collections.singletonList(localBinding));
        // get network bindings
        assertBindings(database.getBindings(BindingOriginsConfig.NETWORK_ORIGIN),
                Collections.singletonList(networkBinding));
    }

    @Test
    public void testToString() throws Exception {
        assertEquals("MasterDatastoreImpl\n", database.toString());

        database.addBindings(mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 2000, "20.20.20.20"), getBinding("0:0:0:0:0:0:0:A/32", 15, "0.10.10.10"),
                getBinding("2.2.2.20/32", 2000, "200.200.200.200")));

        StringBuilder value = new StringBuilder();
        Arrays.asList(database.toString().split("\n")).stream().sorted().forEach(l -> value.append(l).append("\n"));

        assertEquals("\t100 1.1.1.1/32\n" + "\t15 0:0:0:0:0:0:0:A/32\n" + "\t2000 2.2.2.2/32\n" + "\t2000 2.2.2.20/32\n"
                + "MasterDatastoreImpl\n", value.toString());
    }

    /**
     * Prepare {@link DatastoreAccess} mock instance backed by {@link DataBroker} for tests.
     * <p>
     * {@link ReadTransaction} and {@link WriteTransaction} are assumed to be created by {@link TransactionChain}.
     * <p>
     * {@link WriteTransaction} writes Master database bindings into a map.
     * <p>
     * {@link ReadTransaction} reads saved Master database bindings from a map.
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
        doAnswer(invocation -> {
            Objects.requireNonNull(((MasterDatabase) invocation.getArgument(2)).getMasterDatabaseBinding())
                    .forEach(b -> databaseBindings_Op.put(b.getIpPrefix(), b));
            return null;
        }).when(writeTransaction)
                .merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(MasterDatabase.class));

        doAnswer(invocation -> {
            databaseBindings_Op.clear();
            Objects.requireNonNull(((MasterDatabase) invocation.getArgument(2)).getMasterDatabaseBinding())
                    .forEach(b -> databaseBindings_Op.put(b.getIpPrefix(), b));
            return null;
        }).when(writeTransaction)
                .put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(MasterDatabase.class));

        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .then(invocation -> {
                    InstanceIdentifier<?> identifier = invocation.getArgument(1);
                    if (identifier.getTargetType() == MasterDatabase.class) {
                        Optional<MasterDatabase> masterDatabase = Optional.of(new MasterDatabaseBuilder()
                                .setMasterDatabaseBinding(new ArrayList<>(databaseBindings_Op.values()))
                                .build());
                        return FluentFutures.immediateFluentFuture((masterDatabase));
                    } else if (identifier.getTargetType() == MasterDatabaseBinding.class) {
                        Optional<IpPrefix> ipPrefix = Optional.ofNullable(databaseBindings_Op
                                .get(identifier.firstKeyOf(MasterDatabaseBinding.class)))
                                .map(SxpBindingFields::getIpPrefix);
                        return FluentFutures.immediateFluentFuture(ipPrefix);
                    }
                    return FluentFutures.immediateFluentFuture(Optional.of(mock(identifier.getTargetType())));
                });
        when(transactionChain.newReadOnlyTransaction())
                .thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction())
                .thenReturn(writeTransaction);
        when(dataBroker.createTransactionChain(any(TransactionChainListener.class)))
                .thenReturn(transactionChain);

        return DatastoreAccess.getInstance(dataBroker);
    }
}
