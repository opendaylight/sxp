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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SxpNode.class, DatastoreAccess.class, BindingDispatcher.class})
public class MasterDatastoreImplTest {

    private static MasterDatastoreImpl database;
    private static long time = System.currentTimeMillis();
    private static DatastoreAccess access;
    private static Map<IpPrefix, MasterDatabaseBinding> databaseBindings_Op = new HashMap<>();
    @Mock private BindingDispatcher dispatcherMock;
    @Mock private SxpDomain domainMock;
    @Mock private SxpNode nodeMock;

    @BeforeClass
    public static void initClass() {
        BindingOriginsConfig.INSTANCE.addBindingOrigins(BindingOriginsConfig.DEFAULT_ORIGIN_PRIORITIES);

        access = PowerMockito.mock(DatastoreAccess.class);
        PowerMockito.when(
                access.merge(any(InstanceIdentifier.class), any(MasterDatabase.class), any(LogicalDatastoreType.class)))
                .then(invocation -> {
                    ((MasterDatabase) invocation.getArguments()[1]).getMasterDatabaseBinding().stream().forEach(b -> {
                        databaseBindings_Op.put(b.getIpPrefix(), b);
                    });
                    return Futures.immediateCheckedFuture(null);
                });
        PowerMockito.when(
                access.put(any(InstanceIdentifier.class), any(MasterDatabase.class), any(LogicalDatastoreType.class)))
                .then(invocation -> {
                    databaseBindings_Op.clear();
                    ((MasterDatabase) invocation.getArguments()[1]).getMasterDatabaseBinding().stream().forEach(b -> {
                        databaseBindings_Op.put(b.getIpPrefix(), b);
                    });
                    return Futures.immediateCheckedFuture(null);
                });
        PowerMockito.when(access.readSynchronous(any(InstanceIdentifier.class), any(LogicalDatastoreType.class)))
                .then(invocation -> {
                    if (((InstanceIdentifier) invocation.getArguments()[0]).getTargetType() == MasterDatabase.class) {
                        return new MasterDatabaseBuilder().setMasterDatabaseBinding(
                                new ArrayList<>(databaseBindings_Op.values())).build();
                    } else if (((InstanceIdentifier) invocation.getArguments()[0]).getTargetType()
                            == MasterDatabaseBinding.class) {
                        return databaseBindings_Op.get(
                                ((MasterDatabaseBindingKey) ((InstanceIdentifier) invocation.getArguments()[0])
                                        .firstKeyOf(
                                                MasterDatabaseBinding.class)).getIpPrefix());
                    }
                    return null;
                });
    }

    @Before
    public void init() {
        databaseBindings_Op.clear();
        when(dispatcherMock.getOwner()).thenReturn(nodeMock);
        database = new MasterDatastoreImpl(access, "0.0.0.0", "DOMAIN");
        database.initDBPropagatingListener(dispatcherMock, domainMock);
    }

    private <T extends SxpBindingFields> T getBinding(String prefix, int sgt, String... peers) {
        return getBinding(prefix, sgt, BindingOriginsConfig.LOCAL_ORIGIN, peers);
    }

    private <T extends SxpBindingFields> T getNetworkBinding(String prefix, int sgt, String... peers) {
        return getBinding(prefix, sgt, BindingOriginsConfig.NETWORK_ORIGIN, peers);
    }

    private <T extends SxpBindingFields> T getBinding(String prefix, int sgt, OriginType origin, String... peers) {
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        bindingBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
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

    private <T extends SxpBindingFields, R extends SxpBindingFields> void assertBindings(List<T> bindings1,
            List<R> bindings2) {
        bindings1.forEach(b -> assertTrue(bindings2.stream().anyMatch(
                r -> r.getSecurityGroupTag().getValue().equals(b.getSecurityGroupTag().getValue())
                        && Arrays.equals(r.getIpPrefix().getValue(), b.getIpPrefix().getValue())
                        && r.getOrigin().equals(b.getOrigin()))));
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

    @Test
    public void testAddBindingsNotDefinedPriority() {
        final String cluster = "CLUSTER";
        final SxpBindingFields binding = getBinding("1.1.1.1/32", 10, new OriginType(cluster));

        try {
            database.addBindings(Collections.singletonList(binding));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Cannot find binding priority: " + cluster, e.getMessage());
        }
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
}