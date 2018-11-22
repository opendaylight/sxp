/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.jst.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 *
 * @author Martin Dindoffer
 */
public final class SxpSchemaPaths {

    public static final SchemaPath NT_PATH
            = SchemaPath.create(true, NetworkTopology.QNAME);

    public static final SchemaPath TOPOLOGY_PATH
            = SchemaPath.create(true, NetworkTopology.QNAME, Topology.QNAME);

    public static final SchemaPath NT_NODE_PATH
            = SchemaPath.create(true, NetworkTopology.QNAME, Topology.QNAME, Node.QNAME);

    public static final SchemaPath SXP_NODE_PATH
            = SchemaPath.create(true, NetworkTopology.QNAME, Topology.QNAME, Node.QNAME, SxpNodeIdentity.QNAME);

    private SxpSchemaPaths() {
    }

}
