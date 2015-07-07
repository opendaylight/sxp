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
import org.opendaylight.sxp.util.exception.node.NodeIdNotDefinedException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.sxp.database.Vpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SxpDatabaseImplTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static SxpDatabase sxpDatabase;
        private static SxpDatabaseImpl database;
        private static List<PathGroup> pathGroupList;
        private static boolean bindingsAsCleanUp;

        @Before public void init() {
                pathGroupList = new ArrayList<>();
                sxpDatabase = mock(SxpDatabase.class);
                when(sxpDatabase.getVpn()).thenReturn(new ArrayList<Vpn>());
                when(sxpDatabase.getAttribute()).thenReturn(new ArrayList<Attribute>());
                when(sxpDatabase.getPathGroup()).thenReturn(pathGroupList);
                database = new SxpDatabaseImpl(sxpDatabase);
                bindingsAsCleanUp = false;
        }

        private Binding getBinding(String prefix) {
                BindingBuilder bindingBuilder = new BindingBuilder();
                bindingBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
                bindingBuilder.setTimestamp(DateAndTime.getDefaultInstance("2015-06-30T12:00:00Z"));
                bindingBuilder.setCleanUp(bindingsAsCleanUp);
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

        private PathGroup getPathGroup(List<NodeId> nodeIds, PrefixGroup... prefixGroups_) {
                PathGroupBuilder pathGroupBuilder = new PathGroupBuilder();
                List<PrefixGroup> prefixGroups = new ArrayList<>();
                pathGroupBuilder.setPrefixGroup(prefixGroups);
                for (PrefixGroup group : prefixGroups_) {
                        prefixGroups.add(group);
                }
                pathGroupBuilder.setPeerSequence(NodeIdConv.createPeerSequence(nodeIds));
                pathGroupBuilder.setPathHash(nodeIds.hashCode() + 25);
                return pathGroupBuilder.build();
        }

        private List<NodeId> getNodeIds(String... peerSeq) {
                List<NodeId> nodeIds = new ArrayList<>();
                for (String s : peerSeq) {
                        nodeIds.add(NodeId.getDefaultInstance(s));
                }
                return nodeIds;
        }

        private SxpBindingIdentity getSxpBindingIdentity(int sgt, String prefix, String... peerSeq) {
                SxpBindingIdentity identity;
                PrefixGroup prefixGroup = getPrefixGroup(sgt, prefix);
                List<NodeId> nodeIds = getNodeIds(peerSeq);
                identity =
                        SxpBindingIdentity.create(getBinding(prefix), prefixGroup, getPathGroup(nodeIds, prefixGroup));
                return identity;
        }

        @Test public void testAddBindings() throws Exception {
                SxpDatabase sxpDatabase_ = mock(SxpDatabase.class);
                List<PathGroup> pathGroups = new ArrayList<>();
                pathGroups.add(getPathGroup(getNodeIds("127.0.0.1", "127.0.0.2"), getPrefixGroup(10, "127.0.0.1/32")));
                pathGroups.add(getPathGroup(getNodeIds("127.0.0.1", "127.0.0.2"), getPrefixGroup(10, "127.0.0.1/32")));
                when(sxpDatabase_.getPathGroup()).thenReturn(pathGroups);
                database.addBindings(sxpDatabase_);
                pathGroups.remove(0);
                assertEquals(pathGroups, pathGroupList);
                bindingsAsCleanUp = true;
                pathGroups.add(getPathGroup(getNodeIds("127.0.0.5", "127.0.0.10"),
                        getPrefixGroup(10, "2001:0:0:0:0:0:0:1/128")));
                pathGroups.add(getPathGroup(getNodeIds("127.0.0.1", "127.0.0.2"), getPrefixGroup(10, "127.0.0.1/32")));
                database.addBindings(sxpDatabase_);
                pathGroups.remove(2);
                assertEquals(pathGroups, pathGroupList);
                database.addBindings(null);
                assertEquals(pathGroups, pathGroupList);
        }

        private boolean containsPrefixGroup(PrefixGroup group, List<PathGroup> pathGroups) {
                boolean found = false;
                for (PathGroup pathGroup : pathGroups) {
                        found = found || pathGroup.getPrefixGroup().contains(group);
                }
                return found;
        }

        @Test public void testCleanUpBindings() throws Exception {
                bindingsAsCleanUp = true;
                pathGroupList.add(getPathGroup(getNodeIds("127.0.0.1"), getPrefixGroup(10, "127.0.0.1/32")));
                pathGroupList.add(
                        getPathGroup(getNodeIds("127.0.0.1", "127.0.1.0"), getPrefixGroup(10, "127.0.0.15/32")));
                pathGroupList.add(getPathGroup(getNodeIds("127.0.0.5", "127.0.0.10"),
                        getPrefixGroup(10, "2001:0:0:0:0:0:0:1/128")));

                database.cleanUpBindings(NodeId.getDefaultInstance("127.0.0.1"));
                assertFalse(containsPrefixGroup(getPrefixGroup(10, "127.0.0.1/32"), pathGroupList));
                assertFalse(containsPrefixGroup(getPrefixGroup(10, "127.0.0.15/32"), pathGroupList));

                database.cleanUpBindings(NodeId.getDefaultInstance("127.0.0.10"));
                assertTrue(containsPrefixGroup(getPrefixGroup(10, "2001:0:0:0:0:0:0:1/128"), pathGroupList));

                bindingsAsCleanUp = false;
                pathGroupList.add(
                        getPathGroup(getNodeIds("127.0.0.8", "127.0.0.20"), getPrefixGroup(10, "80.2.2.1/32")));
                database.cleanUpBindings(NodeId.getDefaultInstance("127.0.0.8"));
                assertTrue(containsPrefixGroup(getPrefixGroup(10, "80.2.2.1/32"), pathGroupList));

                exception.expect(NodeIdNotDefinedException.class);
                database.cleanUpBindings(null);
        }

        @Test public void testDeleteBindings() throws Exception {
                //Fill DB
                pathGroupList.add(getPathGroup(getNodeIds("127.0.0.1"), getPrefixGroup(10, "127.0.0.1/32")));
                pathGroupList.add(
                        getPathGroup(getNodeIds("127.0.0.1", "127.0.1.0"), getPrefixGroup(20, "127.0.0.15/32")));
                pathGroupList.add(getPathGroup(getNodeIds("127.0.0.5", "127.0.0.10"),
                        getPrefixGroup(30, "2001:0:0:0:0:0:0:1/128")));

                //DB to be removed
                SxpDatabase sxpDatabase_ = mock(SxpDatabase.class);
                when(sxpDatabase_.getPathGroup()).thenReturn(pathGroupList);
                database.deleteBindings(sxpDatabase_);
                assertTrue(pathGroupList.isEmpty());

                List<PathGroup> pathGroups = new ArrayList<>();
                pathGroups.add(getPathGroup(getNodeIds("127.0.0.1"), getPrefixGroup(10, "127.0.0.1/32")));
                pathGroups.add(getPathGroup(getNodeIds("127.0.0.1", "127.0.1.0"), getPrefixGroup(20, "127.0.0.15/32")));
                pathGroups.add(getPathGroup(getNodeIds("127.0.0.5", "127.0.0.10"),
                        getPrefixGroup(30, "2001:0:0:0:0:0:0:1/128")));
                when(sxpDatabase_.getPathGroup()).thenReturn(pathGroups);

                //Fill DB
                pathGroupList.add(getPathGroup(getNodeIds("127.0.0.1"), getPrefixGroup(10, "127.0.0.1/32")));
                pathGroupList.add(
                        getPathGroup(getNodeIds("0.0.25.1", "0.0.10.0"), getPrefixGroup(200, "128.0.0.15/32")));
                pathGroupList.add(getPathGroup(getNodeIds("0.0.0.50", "0.0.0.100"),
                        getPrefixGroup(300, "2001:1:0:0:0:0:0:1/128")));

                database.deleteBindings(sxpDatabase_);
                assertFalse(containsPrefixGroup(getPrefixGroup(10, "127.0.0.1/32"), pathGroupList));
                assertTrue(containsPrefixGroup(getPrefixGroup(200, "128.0.0.15/32"), pathGroupList));
                assertTrue(containsPrefixGroup(getPrefixGroup(300, "2001:1:0:0:0:0:0:1/128"), pathGroupList));
        }

        @Test public void testPurgeBindings() throws Exception {
                //Fill DB
                pathGroupList.add(getPathGroup(getNodeIds("127.0.0.1"), getPrefixGroup(10, "127.0.0.1/32")));
                pathGroupList.add(
                        getPathGroup(getNodeIds("127.0.0.1", "127.0.1.0"), getPrefixGroup(20, "127.0.0.15/32")));
                pathGroupList.add(getPathGroup(getNodeIds("127.0.0.5", "127.0.0.10"),
                        getPrefixGroup(30, "2001:0:0:0:0:0:0:1/128")));

                List<PathGroup> pathGroups = new ArrayList<>();
                pathGroups.add(getPathGroup(getNodeIds("127.0.0.1"), getPrefixGroup(10, "127.0.0.1/32")));
                pathGroups.add(getPathGroup(getNodeIds("127.0.0.1", "127.0.1.0"), getPrefixGroup(20, "127.0.0.15/32")));

                database.purgeBindings(NodeId.getDefaultInstance("127.0.0.5"));
                assertEquals(pathGroups, pathGroupList);

                database.purgeBindings(NodeId.getDefaultInstance("127.127.0.5"));
                assertEquals(pathGroups, pathGroupList);

                database.purgeBindings(NodeId.getDefaultInstance("127.0.0.1"));
                assertTrue(pathGroupList.isEmpty());

                exception.expect(NodeIdNotDefinedException.class);
                database.purgeBindings(null);
        }

        @Test public void testReadBindings() throws Exception {
                //Fill DB
                pathGroupList.add(getPathGroup(getNodeIds("127.0.0.1"), getPrefixGroup(10, "127.0.0.1/32")));
                pathGroupList.add(
                        getPathGroup(getNodeIds("127.0.0.2", "127.0.2.2"), getPrefixGroup(100, "127.0.1.1/32")));
                List<SxpBindingIdentity> sxpBindingIdentities = new ArrayList<>();
                sxpBindingIdentities.add(getSxpBindingIdentity(10, "127.0.0.1/32", "127.0.0.1"));
                sxpBindingIdentities.add(getSxpBindingIdentity(100, "127.0.1.1/32", "127.0.0.2", "127.0.2.2"));
                assertEquals(sxpBindingIdentities, database.readBindings());

                sxpBindingIdentities.clear();
                SxpDatabaseImpl database_ = new SxpDatabaseImpl();
                assertEquals(sxpBindingIdentities, database_.readBindings());
        }

        @Test public void testSetAsCleanUp() throws Exception {
                pathGroupList.add(getPathGroup(getNodeIds("127.0.0.1"), getPrefixGroup(10, "127.0.0.1/32")));
                pathGroupList.add(
                        getPathGroup(getNodeIds("127.0.1.1", "127.0.2.2"), getPrefixGroup(100, "127.0.1.1/32")));
                database.setAsCleanUp(NodeId.getDefaultInstance("127.0.0.1"));
                bindingsAsCleanUp = true;
                assertTrue(containsPrefixGroup(getPrefixGroup(10, "127.0.0.1/32"), pathGroupList));
                assertFalse(containsPrefixGroup(getPrefixGroup(100, "127.0.1.1/32"), pathGroupList));
                database.setAsCleanUp(NodeId.getDefaultInstance("127.0.1.1"));
                assertTrue(containsPrefixGroup(getPrefixGroup(10, "127.0.0.1/32"), pathGroupList));
                assertTrue(containsPrefixGroup(getPrefixGroup(100, "127.0.1.1/32"), pathGroupList));

                assertFalse(containsPrefixGroup(getPrefixGroup(100, "0.0.1.1/32"), pathGroupList));

                exception.expect(NodeIdNotDefinedException.class);
                database.setAsCleanUp(null);
        }

        @Test public void testToString() throws Exception {
                pathGroupList.add(getPathGroup(getNodeIds("127.0.0.1"), getPrefixGroup(10, "127.0.0.1/32")));
                pathGroupList.add(
                        getPathGroup(getNodeIds("127.0.0.1", "127.0.1.0"), getPrefixGroup(10, "127.0.0.15/32")));
                pathGroupList.add(getPathGroup(getNodeIds("127.0.0.5", "127.0.0.10"),
                        getPrefixGroup(10, "2001:0:0:0:0:0:0:1/128")));

                assertEquals("SxpDatabaseImpl\n" + " 127.0.0.1\n" + "  10 127.0.0.1/32 [2015-06-30T12:00:00Z]\n"
                                + " 127.0.0.1,127.0.1.0\n" + "  10 127.0.0.15/32 [2015-06-30T12:00:00Z]\n"
                                + " 127.0.0.5,127.0.0.10\n" + "  10 2001:0:0:0:0:0:0:1/128 [2015-06-30T12:00:00Z]",
                        database.toString());
        }

}
