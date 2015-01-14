/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.inet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.unknown.UnknownNodeIdException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.peer.sequence.fields.PeerSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.peer.sequence.fields.peer.sequence.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sources.fields.Sources;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sources.fields.SourcesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

public final class NodeIdConv {

    public static NodeId _decode(byte[] array) throws Exception {
        return createNodeId(InetAddress.getByAddress(ArraysUtil.readBytes(array, 0, 4)).toString());
    }

    public static NodeId create(int nodeId) throws Exception {
        return NodeIdConv._decode(ArraysUtil.int2bytes(nodeId));
    }

    public static NodeId createNodeId(InetAddress inetAddress) throws UnknownNodeIdException {
        if (inetAddress instanceof Inet4Address) {
            return new NodeId(new Ipv4Address(inetAddress.getHostAddress()));
        }
        throw new UnknownNodeIdException("Not IPv4 format [\"" + inetAddress.toString() + "\"]");
    }

    public static NodeId createNodeId(String prefix) throws Exception {
        if (prefix == null || prefix.isEmpty()) {
            throw new UnknownNodeIdException("Not defined [\"" + prefix + "\"]");
        }
        return createNodeId(IpPrefixConv.parseInetPrefix(prefix).getAddress());
    }

    public static PeerSequence createPeerSequence(List<NodeId> nodeIds) {
        if (nodeIds == null) {
            nodeIds = new ArrayList<NodeId>();
        }
        List<Peer> peers = new ArrayList<Peer>();
        for (NodeId nodeId : nodeIds) {
            PeerBuilder peerBuilder = new PeerBuilder();
            peerBuilder.setSeq(nodeIds.indexOf(nodeId));
            peerBuilder.setNodeId(new NodeId(nodeId));
            peers.add(peerBuilder.build());
        }
        PeerSequenceBuilder peerSequenceBuilder = new PeerSequenceBuilder();
        peerSequenceBuilder.setPeer(peers);
        return peerSequenceBuilder.build();
    }

    public static Sources createSources(List<NodeId> nodeIds) {
        if (nodeIds == null) {
            nodeIds = new ArrayList<NodeId>();
        }
        SourcesBuilder sourcesBuilder = new SourcesBuilder();
        sourcesBuilder.setSource(new ArrayList<>(nodeIds));
        return sourcesBuilder.build();
    }

    public static List<NodeId> decode(byte[] array) throws Exception {
        List<NodeId> nodesIds = new ArrayList<NodeId>();
        do {
            NodeId nodeId = _decode(array);
            nodesIds.add(nodeId);
            array = ArraysUtil.readBytes(array, IpPrefixConv.getBytesLength(getPrefixLength(nodeId)));
        } while (array.length != 0);
        return nodesIds;
    }

    public static boolean equalTo(NodeId nodeId1, NodeId nodeId2) {
        return toString(nodeId1).equals(toString(nodeId2));
    }

    public static List<NodeId> getPeerSequence(PeerSequence peerSequence) {
        if (peerSequence == null || peerSequence.getPeer() == null || peerSequence.getPeer().isEmpty()) {
            return new ArrayList<NodeId>();
        }
        List<NodeId> nodeIds = new ArrayList<NodeId>();
        int i = 0;
        while (true) {
            boolean contain = false;
            for (Peer peer : peerSequence.getPeer()) {
                if (i == peer.getKey().getSeq()) {
                    nodeIds.add(peer.getNodeId());
                    i++;
                    contain = true;
                    break;
                }
            }
            if (!contain) {
                break;
            }
        }
        return nodeIds;
    }

    public static int getPrefixLength(NodeId nodeId) {
        return 32;
    }

    public static List<NodeId> getSources(Sources sources) {
        if (sources == null || sources.getSource() == null || sources.getSource().isEmpty()) {
            return new ArrayList<NodeId>();
        }
        List<NodeId> nodeIds = new ArrayList<NodeId>();
        for (NodeId source : sources.getSource()) {
            nodeIds.add(source);
        }
        return nodeIds;
    }

    public static int hashCode(List<NodeId> nodeIds) {
        final int prime = 31;

        int result = 0;
        for (NodeId nodeId : nodeIds) {
            result = prime * result + nodeId.hashCode();
        }
        return result;
    }

    public static byte[] toBytes(List<NodeId> nodesIds) throws Exception {
        byte[] array = new byte[0];
        for (NodeId nodeId : nodesIds) {
            array = ArraysUtil.combine(array, toBytes(nodeId));
        }
        return array;
    }

    public static byte[] toBytes(NodeId nodeId) throws Exception {
        String _prefix = new String(nodeId.getValue());
        if (_prefix.startsWith("/")) {
            _prefix = _prefix.substring(1);
        }
        int i = _prefix.lastIndexOf("/");
        if (i != -1) {
            _prefix = _prefix.substring(0, i);
        }
        return InetAddress.getByName(_prefix).getAddress();
    }

    private static String toString(List<NodeId> nodeIds) {
        String result = "";
        if (nodeIds != null) {
            for (NodeId nodeId : nodeIds) {
                result += toString(nodeId) + " ";
            }
        }
        result = result.trim();
        return result.replaceAll(" ", ",");
    }

    public static String toString(NodeId nodeId) {
        if (nodeId == null) {
            return "";
        }
        String result = new String(nodeId.getValue());
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        int i = result.lastIndexOf("/");
        if (i != -1) {
            return result.substring(0, i);
        }
        return result;
    }

    public static String toString(PeerSequence peerSequence) {
        return toString(getPeerSequence(peerSequence));
    }

    public static String toString(Sources sources) {
        return toString(getSources(sources));
    }
}