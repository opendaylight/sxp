package org.opendaylight.sxp.jst.util;

import com.google.common.base.Optional;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

/**
 *
 * @author Martin Dindoffer
 */
public class NormalizedNodeUtils {

    private static final QName NODE_KEY_QNAME = QName.create(Node.QNAME, "node-id").intern();
    private static final QName TOPO_KEY_QNAME = QName.create(Topology.QNAME, "topology-id").intern();

    public static Optional<NormalizedNode<?, ?>> pickNode(NormalizedNode<?, ?> nodeList, String nodeId) {
        YangInstanceIdentifier id = YangInstanceIdentifier.builder()
                .nodeWithKey(Node.QNAME, NODE_KEY_QNAME, nodeId)
                .build();
        return NormalizedNodes.findNode(nodeList, id);
    }

    public static Optional<NormalizedNode<?, ?>> pickTopology(NormalizedNode<?, ?> topologyList, String topoId) {
        YangInstanceIdentifier id = YangInstanceIdentifier.builder()
                .nodeWithKey(Topology.QNAME, TOPO_KEY_QNAME, topoId)
                .build();
        return NormalizedNodes.findNode(topologyList, id);
    }
}
