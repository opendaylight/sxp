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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class, DatastoreAccess.class})
public class MasterDatastoreImplTest {

    private static MasterDatastoreImpl database;
    private static long time = System.currentTimeMillis();
    private static DatastoreAccess access;
    private static Map<IpPrefix, MasterDatabaseBinding> databaseBindings_Op = new HashMap<>();

    @BeforeClass public static void initClass() {
        access = PowerMockito.mock(DatastoreAccess.class);
        PowerMockito.when(access.mergeSynchronous(any(InstanceIdentifier.class), any(MasterDatabase.class),
                any(LogicalDatastoreType.class))).then(invocation -> {
            ((MasterDatabase) invocation.getArguments()[1]).getMasterDatabaseBinding().stream().forEach(b -> {
                databaseBindings_Op .put(b.getIpPrefix(), b);
            });
            return null;
        });
        PowerMockito.when(access.putSynchronous(any(InstanceIdentifier.class), any(MasterDatabase.class),
                any(LogicalDatastoreType.class))).then(invocation -> {
            databaseBindings_Op.clear();
            ((MasterDatabase) invocation.getArguments()[1]).getMasterDatabaseBinding().stream().forEach(b -> {
                databaseBindings_Op.put(b.getIpPrefix(), b);
            });
            return null;
        });
        PowerMockito.when(access.readSynchronous(any(InstanceIdentifier.class), any(LogicalDatastoreType.class)))
                .then(invocation -> {
                    if (((InstanceIdentifier) invocation.getArguments()[0]).getTargetType() == MasterDatabase.class) {
                        return new MasterDatabaseBuilder().setMasterDatabaseBinding(
                                new ArrayList<>(databaseBindings_Op.values())).build();
                    } else if (((InstanceIdentifier) invocation.getArguments()[0]).getTargetType()
                            == MasterDatabaseBinding.class) {
                        return databaseBindings_Op.get(
                                ((MasterDatabaseBindingKey) ((InstanceIdentifier) invocation.getArguments()[0]).firstKeyOf(
                                        MasterDatabaseBinding.class)).getIpPrefix());
                    }
                    return null;
                });
    }

    @Before public void init() {
        databaseBindings_Op.clear();
        database = new MasterDatastoreImpl(access, "0.0.0.0");
    }

    private <T extends SxpBindingFields> T getBinding(String prefix, int sgt, String... peers) {
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        bindingBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
        bindingBuilder.setSecurityGroupTag(new Sgt(sgt));
        bindingBuilder.setTimestamp(TimeConv.toDt(time += 1000));
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

    @Test public void testAddLocalBindings() throws Exception {
        assertEquals(0, database.addLocalBindings(mergeBindings()).size());
        assertEquals(0, database.getBindings().size());

        List<SxpBindingFields>
                toAdd =
                mergeBindings(getBinding("0.0.0.0/0", 5, "10.10.10.10"), getBinding("1.1.1.1/32", 10, "10.10.10.10"),
                        getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                        getBinding("2.2.2.2/32", 20, "20.20.20.20", "10.10.10.10"),
                        getBinding("2.2.2.2/32", 200, "20.20.20.20"));

        assertBindings(database.addLocalBindings(toAdd), mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 200, "20.20.20.20")));
        assertEquals(2, database.getBindings().size());
        assertBindings(database.getBindings(), mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 200, "20.20.20.20")));

        assertEquals(0, database.addLocalBindings(toAdd).size());
        assertEquals(2, database.getBindings().size());
        assertBindings(database.getBindings(), mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 200, "20.20.20.20")));

        toAdd.clear();
        toAdd =
                mergeBindings(getBinding("15.15.15.15/24", 15, "0.10.10.10"),
                        getBinding("2.2.2.2/32", 2000, "200.200.200.200"));

        assertEquals(2, database.addLocalBindings(toAdd).size());
        assertEquals(3, database.getBindings().size());
        assertBindings(database.getBindings(), mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 2000, "20.20.20.20"), getBinding("15.15.15.15/24", 15, "0.10.10.10"),
                getBinding("2.2.2.2/32", 2000, "200.200.200.200")));
    }

    @Test public void testDeleteBindingsLocal() throws Exception {
        database.addLocalBindings(mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 2000, "20.20.20.20"), getBinding("15.15.15.15/24", 15, "0.10.10.10"),
                getBinding("2.2.2.20/32", 2000, "200.200.200.200")));

        assertEquals(0, database.deleteBindingsLocal(mergeBindings()).size());
        assertEquals(4, database.getBindings().size());

        assertBindings(database.deleteBindingsLocal(mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 2000, "20.20.20.20"))),
                mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                        getBinding("2.2.2.2/32", 2000, "20.20.20.20")));
        assertBindings(database.getBindings(), mergeBindings(getBinding("15.15.15.15/24", 15, "0.10.10.10"),
                getBinding("2.2.2.20/32", 2000, "200.200.200.200")));

        assertBindings(database.deleteBindingsLocal(mergeBindings(getBinding("15.15.15.15/24", 15, "0.10.10.10"),
                getBinding("2.2.2.20/32", 2000, "200.200.200.200"))),
                mergeBindings(getBinding("15.15.15.15/24", 15, "0.10.10.10"),
                        getBinding("2.2.2.20/32", 2000, "200.200.200.200")));
        assertEquals(0, database.getBindings().size());
    }

    @Test public void testAddBindings() throws Exception {
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

    @Test public void testDeleteBindings() throws Exception {
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

    /**
     * This test method verifies otput of toString() after adding several bindings.
     *
     * Currently ignored, since the test expects an exact order of items in the string,
     * but the underlying implementation uses HashMap, which does not guarantee the order of values().
     *
     */
    @Ignore
    @Test public void testToString() throws Exception {
        assertEquals("MasterDatastoreImpl\n", database.toString());

        database.addBindings(mergeBindings(getBinding("1.1.1.1/32", 100, "10.10.10.10"),
                getBinding("2.2.2.2/32", 2000, "20.20.20.20"), getBinding("0:0:0:0:0:0:0:A/32", 15, "0.10.10.10"),
                getBinding("2.2.2.20/32", 2000, "200.200.200.200")));

        assertEquals("MasterDatastoreImpl\n" + "\t2000 2.2.2.2/32\n" + "\t2000 2.2.2.20/32\n" + "\t100 1.1.1.1/32\n"
                + "\t15 0:0:0:0:0:0:0:A/32\n", database.toString());
    }
}
