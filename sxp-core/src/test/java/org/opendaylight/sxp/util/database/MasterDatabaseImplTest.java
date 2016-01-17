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
import org.junit.runner.RunWith;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.exception.node.NodeIdNotDefinedException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.master.database.Vpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class}) public class MasterDatabaseImplTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static MasterDatabase masterDatabase;
        private static MasterDatabaseImpl database;
        private static List<Source> sources;
        private static DatabaseBindingSource databaseBindingSource;
        private static DatabaseAction databaseAction;

        @Before public void init() {
                sources = new ArrayList<>();
                masterDatabase = mock(MasterDatabase.class);
                when(masterDatabase.getVpn()).thenReturn(new ArrayList<Vpn>());
                when(masterDatabase.getSource()).thenReturn(sources);
                database = new MasterDatabaseImpl(masterDatabase);
                databaseBindingSource = DatabaseBindingSource.Sxp;
                databaseAction = DatabaseAction.Add;
        }

        private Binding getBinding(String prefix) {
                BindingBuilder bindingBuilder = new BindingBuilder();
                bindingBuilder.setAction(databaseAction);
                bindingBuilder.setChanged(true);
                bindingBuilder.setTimestamp(DateAndTime.getDefaultInstance("2015-06-30T12:00:00Z"));
                List<NodeId> nodeIds = new ArrayList<>();
                if (prefix.contains(":")) {
                        bindingBuilder.setIpPrefix(new IpPrefix(Ipv6Prefix.getDefaultInstance(prefix)));
                } else {
                        bindingBuilder.setIpPrefix(new IpPrefix(Ipv4Prefix.getDefaultInstance(prefix)));
                        nodeIds.add(NodeId.getDefaultInstance(prefix.split("/")[0]));
                }
                bindingBuilder.setKey(new BindingKey(bindingBuilder.getIpPrefix()));
                bindingBuilder.setPeerSequence(NodeIdConv.createPeerSequence(nodeIds));
                bindingBuilder.setSources(null);
                return bindingBuilder.build();
        }

        private PrefixGroup getPrefixGroup(int sgt, String... prefixes) {
                PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder();
                List<Binding> bindings = new ArrayList<>();
                for (String s : prefixes) {
                        bindings.add(getBinding(s));
                }
                prefixGroupBuilder.setBinding(bindings);
                prefixGroupBuilder.setSgt(new Sgt(sgt));
                return prefixGroupBuilder.build();
        }

        private Source getSource(PrefixGroup... prefixGroups_) {
                SourceBuilder sourceBuilder = new SourceBuilder();
                List<PrefixGroup> prefixGroups = new ArrayList<>();
                sourceBuilder.setPrefixGroup(prefixGroups);
                for (PrefixGroup group : prefixGroups_) {
                        prefixGroups.add(group);
                }
                sourceBuilder.setBindingSource(databaseBindingSource);
                sourceBuilder.setKey(new SourceKey(sourceBuilder.getBindingSource()));
                return sourceBuilder.build();
        }

        private MasterBindingIdentity getMasterBindingIdentity(int sgt, String... peerSeq) {
                MasterBindingIdentity identity;
                PrefixGroup prefixGroup = getPrefixGroup(sgt, peerSeq);
                identity = MasterBindingIdentity.create(getBinding(peerSeq[0]), prefixGroup, getSource(prefixGroup));
                return identity;
        }

        private boolean hasMasterBindingIdentity(MasterBindingIdentity identity, List<Source> sources) {
                boolean found = false;
                for (Source source : sources) {
                        if (identity.getSource().getBindingSource().equals(source.getBindingSource())) {
                                found = found || source.getPrefixGroup().contains(identity.getPrefixGroup());
                        }
                }
                return found;
        }

        @Test public void testAddBindings() throws Exception {
                database.addBindings(NodeId.getDefaultInstance("0.0.0.0"), null);
                assertTrue(sources.isEmpty());
                database.addBindings(NodeId.getDefaultInstance("0.0.0.0"), new ArrayList<MasterBindingIdentity>());
                assertTrue(sources.isEmpty());

                List<MasterBindingIdentity> masterBindingIdentities = new ArrayList<>();
                masterBindingIdentities.add(getMasterBindingIdentity(10, "127.0.0.0/32", "127.0.0.1/32", "0.0.0.0/0"));
                database.addBindings(NodeId.getDefaultInstance("0.0.0.0"), masterBindingIdentities);
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(10, "127.0.0.0/32", "127.0.0.1/32"),
                        sources));
                assertFalse(hasMasterBindingIdentity(getMasterBindingIdentity(10, "0.0.0.0/0"), sources));
                assertFalse(hasMasterBindingIdentity(getMasterBindingIdentity(100, "127.0.0.0/32", "127.0.0.1/32"),
                        sources));

                masterBindingIdentities.clear();
                masterBindingIdentities.add(getMasterBindingIdentity(20, "128.0.0.0/32", "128.0.0.1/32"));
                database.addBindings(NodeId.getDefaultInstance("1.0.0.0"), masterBindingIdentities);
                assertFalse(hasMasterBindingIdentity(getMasterBindingIdentity(10, "127.0.0.0/32", "127.0.0.1/32"),
                        sources));
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(20, "128.0.0.0/32", "128.0.0.1/32"),
                        sources));

                databaseBindingSource = DatabaseBindingSource.Local;
                masterBindingIdentities.add(getMasterBindingIdentity(200, "129.0.0.0/32", "129.0.0.1/32"));
                database.addBindings(NodeId.getDefaultInstance("0.0.0.0"), masterBindingIdentities);
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(200, "129.0.0.0/32", "129.0.0.1/32"),
                        sources));

                databaseAction = DatabaseAction.Delete;
                databaseBindingSource = DatabaseBindingSource.Sxp;
                masterBindingIdentities.remove(0);
                masterBindingIdentities.add(getMasterBindingIdentity(5, "129.5.0.0/32", "129.0.0.5/32"));
                database.addBindings(NodeId.getDefaultInstance("0.0.0.0"), masterBindingIdentities);
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(20, "128.0.0.0/32", "128.0.0.1/32"),
                        sources));
                assertTrue(
                        hasMasterBindingIdentity(getMasterBindingIdentity(5, "129.5.0.0/32", "129.0.0.5/32"), sources));

                exception.expect(NodeIdNotDefinedException.class);
                database.addBindings(null, masterBindingIdentities);
        }

        @Test public void testAddBindingsLocal() throws Exception {
                databaseBindingSource = DatabaseBindingSource.Local;
                List<PrefixGroup> list = new ArrayList<>();
                SxpNode sxpNode = PowerMockito.mock(SxpNode.class);
                database.addBindingsLocal(sxpNode, null);
                assertTrue(sources.isEmpty());
                database.addBindingsLocal(sxpNode, new ArrayList<PrefixGroup>());
                assertTrue(sources.isEmpty());

                list.add(getPrefixGroup(10, "0.0.0.0/32"));
                list.add(getPrefixGroup(50, "0.0.0.0/0"));
                database.addBindingsLocal(sxpNode, list);
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(10, "0.0.0.0/32"), sources));
                assertFalse(hasMasterBindingIdentity(getMasterBindingIdentity(10, "0.0.0.1/32"), sources));
                assertFalse(hasMasterBindingIdentity(getMasterBindingIdentity(50, "0.0.0.0/0"), sources));

                list.add(getPrefixGroup(100, "0.0.0.1/32"));
                database.addBindingsLocal(sxpNode, list);
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(10, "0.0.0.0/32"), sources));
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(100, "0.0.0.1/32"), sources));
        }

        private boolean containsBinding(Binding binding, PrefixGroup group) {
                for (Binding binding1 : group.getBinding()) {
                        if (binding.getIpPrefix().equals(binding1.getIpPrefix())) {
                                return true;
                        }
                }
                return false;
        }

        @Test public void testExpandBindings() throws Exception {
                PrefixGroup prefixGroup = getPrefixGroup(10, "127.0.0.0/24");
                sources.add(getSource(prefixGroup));
                database.expandBindings(3);
                assertTrue(containsBinding(getBinding("127.0.0.1/32"), prefixGroup));
                assertTrue(containsBinding(getBinding("127.0.0.2/32"), prefixGroup));
                assertTrue(containsBinding(getBinding("127.0.0.3/32"), prefixGroup));
                assertFalse(containsBinding(getBinding("127.0.0.4/32"), prefixGroup));

                prefixGroup = getPrefixGroup(100, "2001:0:0:0:0:0:0:0/64");
                sources.add(getSource(prefixGroup));
                database.expandBindings(2);
                assertTrue(containsBinding(getBinding("2001:0:0:0:0:0:0:1/128"), prefixGroup));
                assertTrue(containsBinding(getBinding("2001:0:0:0:0:0:0:2/128"), prefixGroup));
                assertFalse(containsBinding(getBinding("2001:0:0:0:0:0:0:3/128"), prefixGroup));
        }

        @Test public void testPartition() throws Exception {
                sources.add(getSource(getPrefixGroup(10, "127.0.0.0/32")));
                sources.add(getSource(getPrefixGroup(100, "127.0.0.1/32")));
                sources.add(getSource(getPrefixGroup(1000, "127.0.0.2/32")));
                sources.add(getSource(getPrefixGroup(20, "2001:0:0:0:0:0:0:0/128")));
                sources.add(getSource(getPrefixGroup(200, "2001:0:0:0:0:0:0:1/128")));
                List<MasterDatabase> masterDatabases = database.partition(3, false, null);
                assertEquals(2, masterDatabases.size());
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(10, "127.0.0.0/32"),
                        masterDatabases.get(0).getSource()));
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(100, "127.0.0.1/32"),
                        masterDatabases.get(0).getSource()));
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(1000, "127.0.0.2/32"),
                        masterDatabases.get(0).getSource()));

                assertFalse(hasMasterBindingIdentity(getMasterBindingIdentity(20, "2001:0:0:0:0:0:0:0/128"),
                        masterDatabases.get(0).getSource()));

                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(20, "2001:0:0:0:0:0:0:0/128"),
                        masterDatabases.get(1).getSource()));
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(200, "2001:0:0:0:0:0:0:1/128"),
                        masterDatabases.get(1).getSource()));
        }

        @Test public void testPurgeAllDeletedBindings() throws Exception {
                sources.add(getSource(getPrefixGroup(10, "127.0.0.0/32")));
                sources.add(getSource(getPrefixGroup(20, "2001:0:0:0:0:0:0:0/128")));
                databaseAction = DatabaseAction.Delete;
                sources.add(getSource(getPrefixGroup(100, "127.0.0.1/32")));
                sources.add(getSource(getPrefixGroup(200, "2001:0:0:0:0:0:0:1/128")));
                database.purgeAllDeletedBindings();

                assertFalse(hasMasterBindingIdentity(getMasterBindingIdentity(100, "127.0.0.1/32"), sources));
                assertFalse(hasMasterBindingIdentity(getMasterBindingIdentity(200, "2001:0:0:0:0:0:0:1/128"), sources));

                databaseAction = DatabaseAction.Add;
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(10, "127.0.0.0/32"), sources));
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(20, "2001:0:0:0:0:0:0:0/128"), sources));
        }

        @Test public void testPurgeBindings() throws Exception {
                sources.add(getSource(getPrefixGroup(10, "127.0.0.0/32")));
                sources.add(getSource(getPrefixGroup(100, "127.0.0.1/32")));
                sources.add(getSource(getPrefixGroup(200, "2001:0:0:0:0:0:0:1/128")));
                database.purgeBindings(NodeId.getDefaultInstance("127.0.0.1"));

                assertFalse(hasMasterBindingIdentity(getMasterBindingIdentity(100, "127.0.0.1/32"), sources));
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(200, "2001:0:0:0:0:0:0:1/128"), sources));
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(10, "127.0.0.0/32"), sources));

                database.purgeBindings(NodeId.getDefaultInstance("127.0.0.0"));
                assertTrue(hasMasterBindingIdentity(getMasterBindingIdentity(200, "2001:0:0:0:0:0:0:1/128"), sources));
                assertFalse(hasMasterBindingIdentity(getMasterBindingIdentity(10, "127.0.0.0/32"), sources));

                exception.expect(NodeIdNotDefinedException.class);
                database.purgeBindings(null);
        }

        @Test public void testReadBindings() throws Exception {
                sources.add(getSource(getPrefixGroup(10, "127.0.0.0/32")));
                sources.add(getSource(getPrefixGroup(100, "127.0.0.1/32")));

                List<MasterBindingIdentity> masterBindingIdentities = new ArrayList<>();
                masterBindingIdentities.add(getMasterBindingIdentity(10, "127.0.0.0/32"));
                masterBindingIdentities.add(getMasterBindingIdentity(100, "127.0.0.1/32"));
                assertEquals(masterBindingIdentities, database.readBindings());

                databaseAction = DatabaseAction.Delete;
                databaseBindingSource = DatabaseBindingSource.Local;
                sources.add(getSource(getPrefixGroup(200, "2001:0:0:0:0:0:0:1/128")));
                masterBindingIdentities.add(getMasterBindingIdentity(200, "2001:0:0:0:0:0:0:1/128"));
                assertEquals(masterBindingIdentities, database.readBindings());
        }

        @Test public void testReadBindingsLocal() throws Exception {
                sources.add(getSource(getPrefixGroup(10, "127.0.0.0/32")));
                sources.add(getSource(getPrefixGroup(100, "127.0.0.1/32")));

                List<PrefixGroup> masterBindingIdentities = new ArrayList<>();
                assertEquals(masterBindingIdentities, database.readBindingsLocal());

                databaseAction = DatabaseAction.Delete;
                databaseBindingSource = DatabaseBindingSource.Local;
                sources.add(getSource(getPrefixGroup(200, "2001:0:0:0:0:0:0:1/128")));
                masterBindingIdentities.add(getPrefixGroup(200, "2001:0:0:0:0:0:0:1/128"));
                assertEquals(masterBindingIdentities, database.readBindingsLocal());
        }

        @Test public void testResetModified() throws Exception {
                sources.add(getSource(getPrefixGroup(10, "127.0.0.0/32")));
                databaseAction = DatabaseAction.Delete;
                databaseBindingSource = DatabaseBindingSource.Local;
                sources.add(getSource(getPrefixGroup(100, "127.0.0.1/32")));
                sources.add(getSource(getPrefixGroup(200, "2001:0:0:0:0:0:0:1/128")));
                database.resetModified();
                for (Source source : sources) {
                        for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                                for (Binding binding : prefixGroup.getBinding()) {
                                        assertFalse(binding.isChanged());
                                }
                        }
                }
        }

        @Test public void testSetAsDeleted() throws Exception {
                sources.add(getSource(getPrefixGroup(10, "127.0.0.0/32", "0.0.0.0/32")));
                sources.add(getSource(getPrefixGroup(10, "127.5.0.0/32", "0.0.0.50/32")));
                databaseBindingSource = DatabaseBindingSource.Local;
                sources.add(getSource(getPrefixGroup(100, "127.0.0.1/32")));
                sources.add(getSource(getPrefixGroup(200, "2001:0:0:0:0:0:0:1/128")));

                List<PrefixGroup> prefixGroups = new ArrayList<>();
                prefixGroups.add(getPrefixGroup(100, "127.0.0.1/32"));
                prefixGroups.add(getPrefixGroup(200, "2001:0:0:0:0:0:0:1/128"));
                databaseBindingSource = DatabaseBindingSource.Sxp;
                databaseAction = DatabaseAction.Delete;
                prefixGroups.add(getPrefixGroup(10, "127.0.0.0/32", "0.0.0.0/32"));
                PrefixGroup group = getPrefixGroup(10, "127.5.0.0/32", "0.0.0.50/32");
                group.getBinding().clear();
                prefixGroups.add(group);

                database.setAsDeleted(mock(SxpNode.class), prefixGroups);

                assertTrue(sources.contains(getSource(getPrefixGroup(10, "127.0.0.0/32", "0.0.0.0/32"))));
                assertTrue(sources.contains(getSource(getPrefixGroup(10, "127.5.0.0/32", "0.0.0.50/32"))));
                databaseAction = DatabaseAction.Add;
                assertFalse(sources.contains(getSource(getPrefixGroup(10, "127.0.0.0/32", "0.0.0.0/32"))));
                assertFalse(sources.contains(getSource(getPrefixGroup(10, "127.5.0.0/32", "0.0.0.50/32"))));

        }

        @Test public void testToString() throws Exception {
                sources.add(getSource(getPrefixGroup(10, "127.0.0.0/32", "0.0.0.0/32")));
                sources.add(getSource(getPrefixGroup(10, "127.5.0.0/32", "0.0.0.50/32")));
                databaseBindingSource = DatabaseBindingSource.Local;
                sources.add(getSource(getPrefixGroup(100, "127.0.0.1/32")));
                sources.add(getSource(getPrefixGroup(200, "2001:0:0:0:0:0:0:1/128")));
                assertEquals("MasterDatabaseImpl\n" + " Sxp\n"
                                + "  10 127.0.0.0/32 [2015-06-30T12:00:00Z|*Add|Path:127.0.0.0|Src:] 0.0.0.0/32 [2015-06-30T12:00:00Z|*Add|Path:0.0.0.0|Src:]\n"
                                + " Sxp\n"
                                + "  10 127.5.0.0/32 [2015-06-30T12:00:00Z|*Add|Path:127.5.0.0|Src:] 0.0.0.50/32 [2015-06-30T12:00:00Z|*Add|Path:0.0.0.50|Src:]\n"
                                + " Local\n" + "  100 127.0.0.1/32 [2015-06-30T12:00:00Z|*Add|Path:127.0.0.1|Src:]\n"
                                + " Local\n" + "  200 2001:0:0:0:0:0:0:1/128 [2015-06-30T12:00:00Z|*Add|Path:|Src:]",
                        database.toString());
        }
}
