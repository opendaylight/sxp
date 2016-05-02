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
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpPeerGroupFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpPeers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class PeerGroupListener extends SxpDataChangeListener<SxpPeerGroup> {

    public PeerGroupListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess);
    }

    @Override protected DataTreeIdentifier<SxpPeerGroup> getDataTreeIdentifier(LogicalDatastoreType datastoreType) {
        return new DataTreeIdentifier<>(datastoreType, SUBSCRIBED_PATH.child(Node.class)
                .augmentation(SxpNodeIdentity.class)
                .child(SxpPeerGroups.class)
                .child(SxpPeerGroup.class));
    }

    private boolean checkPeersColapsing(DataTreeModification<SxpPeerGroup> c) {
        SxpPeers before = Preconditions.checkNotNull(c.getRootNode().getDataBefore()).getSxpPeers(),
                after = Preconditions.checkNotNull(c.getRootNode().getDataAfter()).getSxpPeers();
        return before == null && after != null &&
                after.getSxpPeer() != null && !after.getSxpPeer().isEmpty() || after == null && before != null &&
                before.getSxpPeer() != null && !before.getSxpPeer().isEmpty();
    }

    private void addGroupToNode(final SxpNode sxpNode, final DataTreeModification<SxpPeerGroup> c) {
        if (!sxpNode.addPeerGroup(new SxpPeerGroupBuilder(c.getRootNode().getDataAfter()).build())) {
            datastoreAccess.deleteSynchronous(c.getRootPath().getRootIdentifier(), LogicalDatastoreType.OPERATIONAL);
            datastoreAccess.deleteSynchronous(c.getRootPath().getRootIdentifier(), LogicalDatastoreType.CONFIGURATION);
        }
    }

    @Override protected void handleNonConfig(DataTreeModification<SxpPeerGroup> c, String nodeId) {
        SxpNode sxpNode = Configuration.getRegisteredNode(nodeId);
        if (sxpNode == null)
            return;
        switch (c.getRootNode().getModificationType()) {
            case WRITE:
                if (c.getRootNode().getDataBefore() == null) {
                    addGroupToNode(sxpNode, c);
                    break;
                } else if (c.getRootNode().getDataAfter() == null) {
                    sxpNode.removePeerGroup(Preconditions.checkNotNull(c.getRootNode().getDataBefore()).getName());
                    break;
                }
            case SUBTREE_MODIFIED:
                if (checkChange(c, SxpPeerGroupFields::getSxpPeers) && checkPeersColapsing(c)) {
                    sxpNode.removePeerGroup(Preconditions.checkNotNull(c.getRootNode().getDataBefore()).getName());
                    addGroupToNode(sxpNode, c);
                }
                break;
            case DELETE:
                if (c.getRootNode().getDataAfter() == null)
                    sxpNode.removePeerGroup(Preconditions.checkNotNull(c.getRootNode().getDataBefore()).getName());
                break;
        }
    }
}
