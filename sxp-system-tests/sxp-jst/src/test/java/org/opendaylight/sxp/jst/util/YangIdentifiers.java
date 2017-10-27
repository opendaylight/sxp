package org.opendaylight.sxp.jst.util;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 *
 * @author Martin Dindoffer
 */
public final class YangIdentifiers {

    public static final YangInstanceIdentifier TOPO_LIST = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME)
            .node(Topology.QNAME)
            .build();

    public static final YangInstanceIdentifier NODE_LIST = YangInstanceIdentifier.builder(TOPO_LIST)
            .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"), "sxp")
            .node(Node.QNAME)
            .build();

    public static YangInstanceIdentifier createAbsoluteNodeIdentifier(String nodeId) {
        return YangInstanceIdentifier.builder(NODE_LIST)
                .nodeWithKey(Node.QNAME, QName.create(Node.QNAME, "node-id"), nodeId)
                .build();
    }

}
