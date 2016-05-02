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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import static org.opendaylight.sxp.controller.util.io.ConfigLoader.initTopologyNode;

public class NodeIdentityListener extends SxpDataChangeListener<SxpNodeIdentity> {

    public NodeIdentityListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess);
    }

    @Override protected DataTreeIdentifier<SxpNodeIdentity> getDataTreeIdentifier(LogicalDatastoreType datastoreType) {
        return new DataTreeIdentifier<>(datastoreType,
                SUBSCRIBED_PATH.child(Node.class).augmentation(SxpNodeIdentity.class));
    }

    @Override protected void handleConfig(DataTreeModification<SxpNodeIdentity> c, String nodeId) {
        initTopologyNode(nodeId, LogicalDatastoreType.OPERATIONAL, datastoreAccess);
        switch (c.getRootNode().getModificationType()) {
            case WRITE:
            case SUBTREE_MODIFIED:
                SxpNodeIdentityBuilder identityBuilder = new SxpNodeIdentityBuilder(c.getRootNode().getDataAfter());
                identityBuilder.setMasterDatabase(null).setSxpDatabase(null);
                datastoreAccess.merge(c.getRootPath().getRootIdentifier(), identityBuilder.build(),
                        LogicalDatastoreType.OPERATIONAL);
                break;
            case DELETE:
                datastoreAccess.checkAndDelete(c.getRootPath().getRootIdentifier(), LogicalDatastoreType.OPERATIONAL);
                break;
        }
    }

    @Override protected void handleNonConfig(DataTreeModification<SxpNodeIdentity> c, final String nodeId) {
        LOG.trace("Operational Modification NodeIdentity {}", c.getRootNode().getModificationType());
        switch (c.getRootNode().getModificationType()) {
            case WRITE:
                if (c.getRootNode().getDataBefore() == null) {
                    Configuration.register(
                            new SxpDatastoreNode(NodeId.getDefaultInstance(Preconditions.checkNotNull(nodeId)),
                                    datastoreAccess, c.getRootNode().getDataAfter())).start();
                    break;
                } else if (c.getRootNode().getDataAfter() == null) {
                    Configuration.unregister(Preconditions.checkNotNull(nodeId)).shutdown();
                    break;
                }
            case SUBTREE_MODIFIED:
                if (checkChange(c, SxpNodeFields::isEnabled)) {
                    if (Preconditions.checkNotNull(c.getRootNode().getDataAfter()).isEnabled()) {
                        Configuration.getRegisteredNode(nodeId).start();
                    } else {
                        Configuration.getRegisteredNode(nodeId).shutdown();
                    }
                } else if (checkChange(c, d -> d.getSecurity().getPassword()) || checkChange(c,
                        SxpNodeFields::getVersion) || checkChange(c, SxpNodeFields::getTcpPort) || checkChange(c,
                        SxpNodeFields::getSourceIp)) {
                    Configuration.getRegisteredNode(nodeId).shutdown().start();
                } else if (checkChange(c, SxpNodeIdentityFields::getTimers)) {
                    Configuration.getRegisteredNode(nodeId).shutdownConnections();
                }
                break;
            case DELETE:
                Configuration.unregister(Preconditions.checkNotNull(nodeId)).shutdown();
                break;
        }
    }
}
