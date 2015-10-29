/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import org.junit.Test;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DatabaseTest {

        private Binding getBinding(String prefix) {
                BindingBuilder bindingBuilder = new BindingBuilder();
                bindingBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
                bindingBuilder.setTimestamp(DateAndTime.getDefaultInstance("2015-06-30T12:00:00Z"));

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

        private void assertPrefixGroups(List<PrefixGroup> prefixGroups_, List<NodeId> nodeIds) {
                for (PrefixGroup prefixGroup : prefixGroups_) {
                        for (Binding binding : prefixGroup.getBinding()) {
                                assertEquals(NodeIdConv.createPeerSequence(nodeIds), binding.getPeerSequence());
                        }
                }
        }

        @Test public void testAssignPrefixGroups() throws Exception {
                List<PrefixGroup> prefixGroups = new ArrayList<>();
                prefixGroups.add(getPrefixGroup(20, "127.0.0.0/32"));
                prefixGroups.add(getPrefixGroup(200, "127.0.0.10/32"));
                List<PrefixGroup> prefixGroups_ = Database.assignPrefixGroups(null, prefixGroups);
                assertNotNull(prefixGroups_);
                prefixGroups_ = Database.assignPrefixGroups(null, null);
                assertNotNull(prefixGroups_);

                List<NodeId> nodeIds = new ArrayList<>();
                nodeIds.add(NodeId.getDefaultInstance("0.0.0.0"));
                prefixGroups_ = Database.assignPrefixGroups(nodeIds.get(0), prefixGroups);

                assertPrefixGroups(prefixGroups_, nodeIds);

                nodeIds.clear();
                nodeIds.add(NodeId.getDefaultInstance("1.1.1.1"));
                prefixGroups_ = Database.assignPrefixGroups(nodeIds.get(0), prefixGroups_);

                assertPrefixGroups(prefixGroups_, nodeIds);
        }

        @Test public void testCreatePrefixGroup() throws Exception {
                PrefixGroup group = Database.createPrefixGroup(100, "127.0.0.0/32");
                assertEquals(100, (long) group.getSgt().getValue());
                assertEquals(1, group.getBinding().size());
                assertArrayEquals("127.0.0.0/32".toCharArray(), group.getBinding().get(0).getIpPrefix().getValue());

                group = Database.createPrefixGroup(100, "127.0.0.0/32", "0.0.0.0/24");
                assertEquals(100, (long) group.getSgt().getValue());
                assertEquals(2, group.getBinding().size());
                assertArrayEquals("127.0.0.0/32".toCharArray(), group.getBinding().get(0).getIpPrefix().getValue());
                assertArrayEquals("0.0.0.0/24".toCharArray(), group.getBinding().get(1).getIpPrefix().getValue());
        }
}
