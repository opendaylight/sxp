/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.spi.ListListener;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroups;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.opendaylight.sxp.controller.listeners.spi.Listener.Differences.checkDifference;

public class PeerGroupListener extends ListListener<SxpNodeIdentity, SxpPeerGroups, SxpPeerGroup> {

    public PeerGroupListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess, SxpPeerGroups.class);
    }

    @Override protected void handleOperational(DataObjectModification<SxpPeerGroup> c,
            InstanceIdentifier<SxpNodeIdentity> identifier, SxpNode sxpNode) {
        LOG.trace("Operational Modification {} {}", getClass(), c.getModificationType());
        switch (c.getModificationType()) {
            case WRITE:
                if (c.getDataBefore() == null) {
                    addGroupToNode(sxpNode, c, getIdentifier(c.getDataAfter(), identifier));
                    break;
                } else if (c.getDataAfter() == null) {
                    sxpNode.removePeerGroup(Preconditions.checkNotNull(c.getDataBefore()).getName());
                    break;
                }
            case SUBTREE_MODIFIED:
                if (checkDifference(
                        c.getDataBefore() == null || c.getDataBefore().getSxpPeers() == null ? null : c.getDataBefore()
                                .getSxpPeers()
                                .getSxpPeer(),
                        c.getDataAfter() == null || c.getDataAfter().getSxpPeers() == null ? null : c.getDataAfter()
                                .getSxpPeers()
                                .getSxpPeer())) {
                    sxpNode.removePeerGroup(Preconditions.checkNotNull(c.getDataBefore()).getName());
                    addGroupToNode(sxpNode, c, getIdentifier(c.getDataAfter(), identifier));
                }
                break;
            case DELETE:
                sxpNode.removePeerGroup(Preconditions.checkNotNull(c.getDataBefore()).getName());
                break;
        }
    }

    @Override protected InstanceIdentifier<SxpPeerGroup> getIdentifier(SxpPeerGroup d,
            InstanceIdentifier<SxpNodeIdentity> parentIdentifier) {
        Preconditions.checkNotNull(d);
        Preconditions.checkNotNull(parentIdentifier);
        return parentIdentifier.child(SxpPeerGroups.class).child(SxpPeerGroup.class, new SxpPeerGroupKey(d.getName()));
    }

    /**
     * @param sxpNode    Node where group will be added
     * @param c          Object modification containing necessary data
     * @param identifier InstanceIdentifier pointing to specified group
     */
    private void addGroupToNode(final SxpNode sxpNode, final DataObjectModification<SxpPeerGroup> c,
            final InstanceIdentifier<SxpPeerGroup> identifier) {
        final DatastoreAccess datastoreAccess = getDatastoreAccess(NodeIdConv.toString(sxpNode.getNodeId()));
        if (!sxpNode.addPeerGroup(new SxpPeerGroupBuilder(c.getDataAfter()).build())) {
            if (!datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION))
                datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL);
        }
    }
}
