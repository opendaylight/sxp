/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.core.SxpDatastoreNode;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.util.exception.connection.NoNetworkInterfacesException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NodeIdentityListener extends SxpDataChangeListener<SxpNodeIdentity> {

    public NodeIdentityListener(DatastoreAccess datastoreAccess, LogicalDatastoreType datastoreType) {
        super(datastoreAccess, datastoreType);
    }

    @Override protected DataTreeIdentifier<SxpNodeIdentity> getDataTreeIdentifier() {
        return new DataTreeIdentifier<>(datastoreType,
                SUBSCRIBED_PATH.child(Node.class).augmentation(SxpNodeIdentity.class));
    }

    @Override protected void handleNonConfig(DataTreeModification<SxpNodeIdentity> c, final String nodeId) {
        switch (c.getRootNode().getModificationType()) {
            case SUBTREE_MODIFIED:
                if (!Preconditions.checkNotNull(c.getRootNode().getDataBefore())
                        .getSecurity()
                        .equals(Preconditions.checkNotNull(c.getRootNode().getDataAfter()).getSecurity())) {
                    Configuration.getRegisteredNode(nodeId).shutdown();
                    try {
                        Configuration.getRegisteredNode(nodeId).start();
                    } catch (NoNetworkInterfacesException e) {
                        LOG.error("Cannot bind address TODO", e);
                    }
                }
                break;
            case WRITE:
                if (c.getRootNode().getDataBefore() == null) {
                    try {
                        Configuration.register(
                                new SxpDatastoreNode(NodeId.getDefaultInstance(Preconditions.checkNotNull(nodeId)),
                                        datastoreAccess, c.getRootNode().getDataAfter()));
                        Configuration.getRegisteredNode(nodeId).start();
                    } catch (NoNetworkInterfacesException e) {
                        LOG.error("Cannot bind address TODO", e);
                    }
                }
                break;
            case DELETE:
                Configuration.getRegisteredNode(Preconditions.checkNotNull(nodeId)).shutdown();
                break;
        }
    }
}
