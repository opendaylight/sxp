/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

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

    private static final QName NODE_KEY_QNAME = QName.create(Node.QNAME, "node-id").intern();
    private static final QName TOPO_KEY_QNAME = QName.create(Topology.QNAME, "topology-id").intern();

    public static final YangInstanceIdentifier TOPO_LIST = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME)
            .node(Topology.QNAME)
            .build();

    public static final YangInstanceIdentifier SXP_NODE_LIST = YangInstanceIdentifier.builder(TOPO_LIST)
            .nodeWithKey(Topology.QNAME, TOPO_KEY_QNAME, "sxp")
            .node(Node.QNAME)
            .build();

    public static YangInstanceIdentifier createAbsoluteNodeIdentifier(String nodeId) {
        return YangInstanceIdentifier.builder(SXP_NODE_LIST)
                .nodeWithKey(Node.QNAME, NODE_KEY_QNAME, nodeId)
                .build();
    }

}
