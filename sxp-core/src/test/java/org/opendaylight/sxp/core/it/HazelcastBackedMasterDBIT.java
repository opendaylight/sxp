/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.hazelcast.config.Config;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.HazelcastBackedMasterDB;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Copy-paste from {@link org.opendaylight.sxp.util.database.MasterDatabaseImplTest}
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SxpNode.class})
@PowerMockIgnore("javax.management.*")
public class HazelcastBackedMasterDBIT {

    private HazelcastBackedMasterDB database;
    private static long time = System.currentTimeMillis();
    @Rule
    public Timeout globalTimeout = new Timeout(60_000);

    @BeforeClass
    public static void initClass() {
        BindingOriginsConfig.INSTANCE.addBindingOrigins(BindingOriginsConfig.DEFAULT_ORIGIN_PRIORITIES);
    }

    @Before
    public void init() {
        Config hcConfig = new Config();
        hcConfig.getGroupConfig().setName(UUID.randomUUID().toString());
        database = new HazelcastBackedMasterDB("MASTER_DB", hcConfig);
    }

    @After
    public void shutdown() {
        database.close();
    }

    private <T extends SxpBindingFields> T getBinding(String prefix, int sgt, String... peers) {
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        bindingBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
        bindingBuilder.setSecurityGroupTag(new Sgt(sgt));
        bindingBuilder.setTimestamp(TimeConv.toDt(time += 1000));
        bindingBuilder.setOrigin(BindingOriginsConfig.LOCAL_ORIGIN);
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

}