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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SxpBindingIdentityTest {

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

        private PathGroup getPathGroup(List<NodeId> nodeIds,
                org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup... prefixGroups_) {
                PathGroupBuilder pathGroupBuilder = new PathGroupBuilder();
                List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup>
                        prefixGroups =
                        new ArrayList<>();
                pathGroupBuilder.setPrefixGroup(prefixGroups);
                for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup group : prefixGroups_) {
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

        @Test public void testCreate() throws Exception {
                SxpBindingIdentity
                        identity =
                        SxpBindingIdentity.create(getBinding("127.0.0.1/32"), getPrefixGroup(10, "127.0.0.1/32"),
                                getPathGroup(getNodeIds("0.0.0.0"), getPrefixGroup(10, "127.0.0.1/32")));
                assertEquals(getBinding("127.0.0.1/32"), identity.getBinding());
                assertEquals(getPrefixGroup(10, "127.0.0.1/32"), identity.getPrefixGroup());
                assertEquals(getPathGroup(getNodeIds("0.0.0.0"), getPrefixGroup(10, "127.0.0.1/32")),
                        identity.getPathGroup());
        }

        @Test public void testEquals() throws Exception {
                SxpBindingIdentity
                        identity =
                        SxpBindingIdentity.create(getBinding("127.0.0.1/32"), getPrefixGroup(10, "127.0.0.1/32"),
                                getPathGroup(getNodeIds("0.0.0.0"), getPrefixGroup(10, "127.0.0.1/32")));
                SxpBindingIdentity
                        identity2 =
                        SxpBindingIdentity.create(getBinding("127.0.0.2/32"), getPrefixGroup(20, "127.0.0.2/32"),
                                getPathGroup(getNodeIds("0.0.0.0"), getPrefixGroup(20, "127.0.0.2/32")));

                assertTrue(identity.equals(identity));
                assertFalse(identity.equals(identity2));
                assertFalse(identity2.equals(identity));
                assertFalse(identity.equals(null));
        }

        @Test public void testToString() throws Exception {
                List<SxpBindingIdentity> identities = new ArrayList<>();
                identities.add(SxpBindingIdentity.create(getBinding("127.0.0.1/32"), getPrefixGroup(10, "127.0.0.1/32"),
                        getPathGroup(getNodeIds("0.0.0.0"), getPrefixGroup(10, "127.0.0.1/32"))));
                assertEquals("0.0.0.0 10 127.0.0.1/32", identities.get(0).toString());
                identities.add(SxpBindingIdentity.create(getBinding("127.0.0.2/32"), getPrefixGroup(20, "127.0.0.2/32"),
                        getPathGroup(getNodeIds("0.0.0.0"), getPrefixGroup(20, "127.0.0.2/32"))));
                assertEquals("0.0.0.0 10 127.0.0.1/32\n0.0.0.0 20 127.0.0.2/32\n",
                        SxpBindingIdentity.toString(identities));
        }
}
