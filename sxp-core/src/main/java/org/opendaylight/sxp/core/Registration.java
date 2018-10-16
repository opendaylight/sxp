/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.sxp.util.inet.NodeIdConv;

public final class Registration {
    private static final Map<String, SxpNode> NODES = new ConcurrentHashMap<>();

    /**
     * Get all registered nodes.
     *
     * @return Currently added SxpNodes
     */
    public static Collection<SxpNode> getNodes() {
        return Collections.unmodifiableCollection(NODES.values());
    }

    /**
     * Retrieve a registered node.
     *
     * @param nodeId NodeId specifying Node
     * @return SxpNode with provided NodeId
     */
    public static SxpNode getRegisteredNode(final String nodeId) {
        return NODES.get(nodeId);
    }

    /**
     * Unregister a given node.
     *
     * @param nodeId NodeId specifying Node
     * @return Removed SxpNode
     */
    public static SxpNode unRegister(final String nodeId) {
        return NODES.remove(Preconditions.checkNotNull(nodeId));
    }

    /**
     * Register a given node.
     *
     * @param node SxpNode that will be registered
     * @return Registered SxpNode
     */
    public static SxpNode register(final SxpNode node) {
        NODES.put(NodeIdConv.toString(Preconditions.checkNotNull(node).getNodeId()), node);
        return node;
    }
}
