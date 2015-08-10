/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.inet;

import com.google.common.net.InetAddresses;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

    /**
     * Decode Node specific identification from byte array
     *
     * @param array Byte Array that will be decoded
     * @return NodeId decoded from specified array
     * @throws UnknownHostException   If address has illegal format
     * @throws UnknownNodeIdException If address isn't in IPv4 format
     */
    public static NodeId _decode(byte[] array) throws UnknownHostException, UnknownNodeIdException {
        return createNodeId(InetAddress.getByAddress(ArraysUtil.readBytes(array, 0, 4)).toString());
    }

    /**
     * Decode Node specific identification from Integer
     *
     * @param nodeId Integer value that will be decoded
     * @return NodeId decoded from specified integer
     * @throws UnknownHostException   If address has illegal format
     * @throws UnknownNodeIdException If address isn't in IPv4 format
     */
    public static NodeId create(int nodeId) throws UnknownHostException, UnknownNodeIdException {
        return NodeIdConv._decode(ArraysUtil.int2bytes(nodeId));
    }

    /**
     * Creates Node specific identification
     *
     * @param inetAddress IPv4 address used as identification
     * @return NodeId created with specified values
     * @throws UnknownNodeIdException If address isn't in IPv4 format
     */
    public static NodeId createNodeId(InetAddress inetAddress) throws UnknownNodeIdException {
        if (inetAddress instanceof Inet4Address) {
            return new NodeId(new Ipv4Address(inetAddress.getHostAddress()));
        }
        throw new UnknownNodeIdException("Not IPv4 format [\"" + inetAddress.toString() + "\"]");
    }

    /**
     * Creates Node specific identification
     *
     * @param prefix Prefix used to create ID
     * @return NodeId created with specified values
     * @throws UnknownNodeIdException If prefix is null or empty
     */
    public static NodeId createNodeId(String prefix) throws UnknownNodeIdException {
        if (prefix == null || prefix.isEmpty()) {
            throw new UnknownNodeIdException("Not defined [\"" + prefix + "\"]");
        }
        return createNodeId(IpPrefixConv.parseInetPrefix(prefix).getAddress());
    }

    /**
     * Creates PeerSequence from NodeIds,
     * that have the same sequence as in provided list
     *
     * @param nodeIds List of NodeIds used for PeerSequence
     * @return PeerSequence generated from NodeId's
     */
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

    /**
     * Creates Sources that consist of specified NodeIds
     *
     * @param nodeIds List of NodeIds used for Sources
     * @return Sources generated from NodeIds
     */
    public static Sources createSources(List<NodeId> nodeIds) {
        if (nodeIds == null) {
            nodeIds = new ArrayList<NodeId>();
        }
        SourcesBuilder sourcesBuilder = new SourcesBuilder();
        sourcesBuilder.setSource(new ArrayList<>(nodeIds));
        return sourcesBuilder.build();
    }

    /**
     * Decode Node specific identifications from byte array
     *
     * @param array Byte Array that will be decoded
     * @return List of NodeIds decoded from specified array
     * @throws UnknownHostException   If one of addresses has illegal format
     * @throws UnknownNodeIdException If one of addresses isn't in IPv4 format
     */
    public static List<NodeId> decode(byte[] array) throws UnknownHostException, UnknownNodeIdException {
        List<NodeId> nodesIds = new ArrayList<NodeId>();
        while (array != null && array.length != 0) {
            NodeId nodeId = _decode(array);
            nodesIds.add(nodeId);
            array = ArraysUtil.readBytes(array, IpPrefixConv.getBytesLength(getPrefixLength(nodeId)));
        }
        return nodesIds;
    }

    /**
     * Equality check of two NodeIds based on their String representation
     *
     * @param nodeId1 NodeId to compare
     * @param nodeId2 NodeId to compare
     * @return If NodeIds are equal
     */
    public static boolean equalTo(NodeId nodeId1, NodeId nodeId2) {
        return toString(nodeId1).equals(toString(nodeId2));
    }

    /**
     * Creates List of NodeIds from PeerSequence preserving the same order
     *
     * @param peerSequence PeerSequence used for generation
     * @return List of NodeIds contained by PeerSequence
     */
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

    /**
     * Gets Prefix length of NodeId, return 32 since all NodeIds must have it
     *
     * @param nodeId NodeId where to check for Prefix length
     * @return Length of prefix in NodeID
     */
    public static int getPrefixLength(NodeId nodeId) {
        return 32;
    }

    /**
     * Creates List of NodeIds from Sources
     *
     * @param sources Sources used for generation
     * @return List of NodeIds contained in Sources
     */
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

    /**
     * Creates HashCode for multiple NodeIds
     *
     * @param nodeIds NodeIds used
     * @return HashCode of specified NodeIds
     */
    public static int hashCode(List<NodeId> nodeIds) {
        final int prime = 31;

        int result = 0;
        for (NodeId nodeId : nodeIds) {
            result = prime * result + nodeId.hashCode();
        }
        return result;
    }

    /**
     * Converts multiple NodeIds into Byte Array
     *
     * @param nodesIds List of NodeIds that will be converted
     * @return Byte Array representing specified NodeIds
     */
    public static byte[] toBytes(List<NodeId> nodesIds) {
        byte[] array = new byte[0];
        for (NodeId nodeId : nodesIds) {
            array = ArraysUtil.combine(array, toBytes(nodeId));
        }
        return array;
    }

    /**
     * Converts NodeId into Byte Array
     *
     * @param nodeId NodeId that will be converted
     * @return ByteArray representing specified NodeId
     */
    public static byte[] toBytes(NodeId nodeId) {
        String _prefix = new String(nodeId.getValue());
        if (_prefix.startsWith("/")) {
            _prefix = _prefix.substring(1);
        }
        int i = _prefix.lastIndexOf("/");
        if (i != -1) {
            _prefix = _prefix.substring(0, i);
        }
        return InetAddresses.forString(_prefix).getAddress();
    }

    /**
     * Create String representation of multiple NodeIds
     *
     * @param nodeIds NodeIds used
     * @return String representation of specified NodeIds
     */
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

    /**
     * Create String representations of NodeId
     *
     * @param nodeId NodeId used
     * @return String representation of specified NodeId
     */
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

    /**
     * Create String representation of PeerSequence
     *
     * @param peerSequence PeerSequence used
     * @return String representation of specified PeerSequence
     */
    public static String toString(PeerSequence peerSequence) {
        return toString(getPeerSequence(peerSequence));
    }

    /**
     * Create String representation of Sources
     *
     * @param sources Sources used
     * @return String representation of specified Sources
     */
    public static String toString(Sources sources) {
        return toString(getSources(sources));
    }
}
