/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.MasterBindingIdentity;
import org.opendaylight.sxp.util.database.SxpBindingIdentity;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.peer.sequence.fields.peer.sequence.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class}) public class BindingManagerTest {

        private static SxpNode sxpNode;
        private static SxpDatabaseImpl sxpDatabase;
        private static MasterDatabaseProvider databaseProvider;
        private static int time;

        @Before public void init() throws Exception {
                time = 10;
                sxpNode = PowerMockito.mock(SxpNode.class);
                PowerMockito.when(sxpNode.isEnabled()).thenReturn(true);
                sxpDatabase = mock(SxpDatabaseImpl.class);
                databaseProvider = mock(MasterDatabaseProvider.class);
                PowerMockito.when(sxpNode.getBindingSxpDatabase()).thenReturn(sxpDatabase);
                PowerMockito.when(sxpNode.getBindingMasterDatabase()).thenReturn(databaseProvider);
        }

        private Peer getPeer(String id, int seq) {
                PeerBuilder peerBuilder = new PeerBuilder();
                peerBuilder.setNodeId(new NodeId(id));
                peerBuilder.setSeq(seq);
                return peerBuilder.build();
        }

        private PathGroup getPathGroup(String... id) {
                List<Peer> peerList = new ArrayList<>();
                int seq = 0;
                for (String s : id) {
                        peerList.add(getPeer(s, seq++));
                }
                PathGroupBuilder pathGroupBuilder = new PathGroupBuilder();
                PeerSequenceBuilder peerSequenceBuilder = new PeerSequenceBuilder();
                peerSequenceBuilder.setPeer(peerList);
                pathGroupBuilder.setPeerSequence(peerSequenceBuilder.build());
                return pathGroupBuilder.build();
        }

        private Binding getBinding(String prefix) {
                BindingBuilder bindingBuilder = new BindingBuilder();
                bindingBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
                bindingBuilder.setTimestamp(DateAndTime.getDefaultInstance("2015-06-30T12:" + (time++) + ":00Z"));

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

        @Test public void testDatabaseArbitration() throws Exception {
                BindingManager manager = new BindingManager(sxpNode);
                Class<List<MasterBindingIdentity>> listClass = (Class<List<MasterBindingIdentity>>) (Class) List.class;
                ArgumentCaptor<List<MasterBindingIdentity>> argument = ArgumentCaptor.forClass(listClass);
                //SHORTEST PATH
                List<SxpBindingIdentity> sxpBindingIdentities = new ArrayList<>();
                when(sxpDatabase.readBindings()).thenReturn(sxpBindingIdentities);
                sxpBindingIdentities.add(
                        SxpBindingIdentity.create(getBinding("127.0.0.3/32"), getPrefixGroup(100, "127.0.0.3/32"),
                                getPathGroup("127.0.0.3")));
                sxpBindingIdentities.add(
                        SxpBindingIdentity.create(getBinding("127.0.0.4/32"), getPrefixGroup(10, "127.0.0.4/32"),
                                getPathGroup("127.0.0.4")));
                sxpBindingIdentities.add(
                        SxpBindingIdentity.create(getBinding("127.0.0.4/32"), getPrefixGroup(10, "127.0.0.4/32"),
                                getPathGroup("127.0.0.3", "127.0.0.4")));

                manager.call();
                verify(databaseProvider).addBindings(any(NodeId.class), argument.capture());
                List<MasterBindingIdentity> masterBindingIdentities = argument.getValue();
                assertMasterBindingIdentities(masterBindingIdentities, sxpBindingIdentities.get(0), true);
                assertMasterBindingIdentities(masterBindingIdentities, sxpBindingIdentities.get(1), true);
                assertMasterBindingIdentities(masterBindingIdentities, sxpBindingIdentities.get(2), false);

                //MOST RECENT
                sxpBindingIdentities.clear();
                sxpBindingIdentities.add(
                        SxpBindingIdentity.create(getBinding("127.0.0.3/32"), getPrefixGroup(10, "127.0.0.3/32"),
                                getPathGroup("127.0.0.3")));
                sxpBindingIdentities.add(
                        SxpBindingIdentity.create(getBinding("127.0.0.3/32"), getPrefixGroup(10, "127.0.0.3/32"),
                                getPathGroup("127.0.0.4")));

                manager.call();
                verify(databaseProvider, times(2)).addBindings(any(NodeId.class), argument.capture());
                masterBindingIdentities = argument.getValue();
                assertMasterBindingIdentities(masterBindingIdentities, sxpBindingIdentities.get(0), false);
                assertMasterBindingIdentities(masterBindingIdentities, sxpBindingIdentities.get(1), true);
        }

        private void assertMasterBindingIdentities(List<MasterBindingIdentity> masterBindingIdentities,
                SxpBindingIdentity group, boolean not) {
                assertNotNull(masterBindingIdentities);
                boolean sgt = false, bindings = false, sequence = false;
                for (MasterBindingIdentity identity : masterBindingIdentities) {
                        if (identity.getPrefixGroup().getSgt().equals(group.getPrefixGroup().getSgt())) {
                                sgt = true;
                                if (identity.getBinding().getIpPrefix().equals(group.getBinding().getIpPrefix())) {
                                        bindings = true;
                                }
                                if (identity.getBinding()
                                        .getPeerSequence()
                                        .equals(group.getPathGroup().getPeerSequence())) {
                                        sequence = true;
                                }
                        }
                }
                if (!not) {
                        assertFalse(sgt && bindings && sequence);
                } else {
                        assertTrue(sgt);
                        assertTrue(bindings);
                        assertTrue(sequence);
                }
        }

        @Test public void testCall() throws Exception {
                BindingManager manager = new BindingManager(sxpNode);

                manager.call();
                verify(sxpDatabase).readBindings();
                verify(databaseProvider).addBindings(any(NodeId.class), anyListOf(MasterBindingIdentity.class));
                verify(sxpNode).setSvcBindingDispatcherDispatch();
        }

        @Test public void testCleanUpBindings() throws Exception {
                BindingManager manager = new BindingManager(sxpNode);
                manager.cleanUpBindings(NodeId.getDefaultInstance("0.0.0.0"));
                verify(sxpDatabase).cleanUpBindings(NodeId.getDefaultInstance("0.0.0.0"));
        }

        @Test public void testPurgeBindings() throws Exception {
                BindingManager manager = new BindingManager(sxpNode);
                manager.purgeBindings(NodeId.getDefaultInstance("0.0.0.0"));
                verify(sxpDatabase).purgeBindings(NodeId.getDefaultInstance("0.0.0.0"));
        }

        @Test public void testSetAsCleanUp() throws Exception {
                BindingManager manager = new BindingManager(sxpNode);
                manager.setAsCleanUp(NodeId.getDefaultInstance("0.0.0.0"));
                verify(sxpDatabase).setAsCleanUp(NodeId.getDefaultInstance("0.0.0.0"));
        }
}
