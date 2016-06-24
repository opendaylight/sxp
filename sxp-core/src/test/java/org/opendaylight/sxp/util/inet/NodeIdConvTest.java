/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.inet;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class NodeIdConvTest {

        private static NodeId node1, node2, node3;
        private static List<NodeId> nodeIds1, nodeIds2;

        @BeforeClass public static void init() {
                node1 = new NodeId(new Ipv4Address("127.0.0.1"));
                node2 = new NodeId(new Ipv4Address("0.0.0.1"));
                node3 = new NodeId(new Ipv4Address("127.124.56.1"));

                nodeIds1 = new ArrayList<>();
                nodeIds1.add(node1);
                nodeIds1.add(node2);
                nodeIds1.add(node3);

                nodeIds2 = new ArrayList<>();
                nodeIds2.add(node1);
                nodeIds2.add(node3);
                nodeIds2.add(node2);
        }

        @Test public void test_decode() throws Exception {
                assertEquals(node1, NodeIdConv._decode(new byte[] {127, 0, 0, 1}));
                assertEquals(node2, NodeIdConv._decode(new byte[] {0, 0, 0, 1}));
                assertEquals(node3, NodeIdConv._decode(new byte[] {127, 124, 56, 1}));
        }

        @Test public void testCreate() throws Exception {
                assertEquals(node1, NodeIdConv.create(2130706433));
                assertEquals(node2, NodeIdConv.create(1));
                assertEquals(node3, NodeIdConv.create(2138847233));
        }

        @Test public void testCreateNodeId() throws Exception {
                assertEquals(node1, NodeIdConv.createNodeId(Inet4Address.getByName("127.0.0.1")));
                assertEquals(node2, NodeIdConv.createNodeId(Inet4Address.getByName("0.0.0.1")));
                assertEquals(node3, NodeIdConv.createNodeId(Inet4Address.getByName("127.124.56.1")));

                assertEquals(node1, NodeIdConv.createNodeId("127.0.0.1"));
                assertEquals(node2, NodeIdConv.createNodeId("0.0.0.1"));
                assertEquals(node3, NodeIdConv.createNodeId("127.124.56.1"));
        }

        @Test public void testCreatePeerSequence() throws Exception {
                assertNotNull(NodeIdConv.createPeerSequence(nodeIds1));
                assertNotNull(NodeIdConv.createPeerSequence(new ArrayList<NodeId>()));
                assertNotNull(NodeIdConv.createPeerSequence(null));
                assertEquals(3, NodeIdConv.createPeerSequence(nodeIds1).getPeer().size());
                assertTrue(NodeIdConv.createPeerSequence(null).getPeer().isEmpty());
        }

        @Test public void testDecode() throws Exception {
                assertNotNull(NodeIdConv.decode(null));
                assertNotNull(NodeIdConv.decode(new byte[] {}));
                assertEquals(nodeIds1, NodeIdConv.decode(new byte[] {127, 0, 0, 1, 0, 0, 0, 1, 127, 124, 56, 1}));
                assertNotEquals(nodeIds2, NodeIdConv.decode(new byte[] {127, 0, 0, 1, 0, 0, 0, 1, 127, 124, 56, 1}));
        }

        @Test public void testEqualTo() throws Exception {
                assertTrue(NodeIdConv.equalTo(node1, node1));
                assertFalse(NodeIdConv.equalTo(node1, node3));
                assertFalse(NodeIdConv.equalTo(node2, node3));
        }

        @Test public void testGetPeerSequence() throws Exception {
                assertNotNull(NodeIdConv.getPeerSequence(null));
                assertNotNull(NodeIdConv.getPeerSequence(NodeIdConv.createPeerSequence(null)));
                assertEquals(nodeIds1, NodeIdConv.getPeerSequence(NodeIdConv.createPeerSequence(nodeIds1)));
                assertNotEquals(nodeIds1, NodeIdConv.getPeerSequence(NodeIdConv.createPeerSequence(nodeIds2)));
        }

        @Test public void testToBytes() throws Exception {
                assertNotNull(NodeIdConv.toBytes(new ArrayList<NodeId>()));
                assertNotNull(NodeIdConv.toBytes(nodeIds1));
                assertArrayEquals(new byte[] {127, 0, 0, 1, 0, 0, 0, 1, 127, 124, 56, 1}, NodeIdConv.toBytes(nodeIds1));

                assertNotNull(NodeIdConv.toBytes(node1));
                assertArrayEquals(new byte[] {127, 0, 0, 1}, NodeIdConv.toBytes(node1));
        }

        @Test public void testToString() throws Exception {
                assertEquals("127.0.0.1", NodeIdConv.toString(node1));
                assertEquals("0.0.0.1", NodeIdConv.toString(node2));
                assertEquals("127.124.56.1", NodeIdConv.toString(node3));

                List<Peer> peerList = new ArrayList<>();
                PeerSequenceBuilder peerSequenceBuilder = new PeerSequenceBuilder();
                peerSequenceBuilder.setPeer(peerList);

                PeerBuilder peerBuilder = new PeerBuilder();
                peerBuilder.setNodeId(node1);
                peerBuilder.setSeq(0);
                peerList.add(peerBuilder.build());
                peerBuilder = new PeerBuilder();
                peerBuilder.setNodeId(node2);
                peerBuilder.setSeq(1);
                peerList.add(peerBuilder.build());

                assertEquals("127.0.0.1,0.0.0.1", NodeIdConv.toString(peerSequenceBuilder.build()));
        }
}
