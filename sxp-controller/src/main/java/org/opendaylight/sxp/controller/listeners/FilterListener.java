/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class FilterListener extends SxpDataChangeListener<SxpFilter> {

    public FilterListener(DatastoreAccess datastoreAccess, LogicalDatastoreType datastoreType) {
        super(datastoreAccess, datastoreType);
    }

    @Override protected DataTreeIdentifier<SxpFilter> getDataTreeIdentifier() {
        return new DataTreeIdentifier<>(datastoreType, SUBSCRIBED_PATH.child(Node.class)
                .augmentation(SxpNodeIdentity.class)
                .child(SxpPeerGroups.class)
                .child(SxpPeerGroup.class)
                .child(SxpFilter.class));
    }

    @Override protected void handleConfig(DataTreeModification<SxpFilter> c, String nodeId) {
    }

    @Override protected void handleNonConfig(DataTreeModification<SxpFilter> c, String nodeId) {
        SxpNode sxpNode = Configuration.getRegisteredNode(nodeId);
        if (sxpNode == null)
            return;
        String groupName = c.getRootPath().getRootIdentifier().firstKeyOf(SxpPeerGroup.class).getName();
        switch (c.getRootNode().getModificationType()) {
            case SUBTREE_MODIFIED:
                sxpNode.removeFilterFromPeerGroup(groupName, c.getRootNode().getDataBefore().getFilterType());
                sxpNode.addFilterToPeerGroup(groupName, c.getRootNode().getDataAfter());
                break;
            case WRITE:
                if (c.getRootNode().getDataBefore() == null) {
                    sxpNode.addFilterToPeerGroup(groupName, c.getRootNode().getDataAfter());
                }
                break;
            case DELETE:
                if (c.getRootNode().getDataAfter() == null) {
                    sxpNode.removeFilterFromPeerGroup(groupName, c.getRootNode().getDataBefore().getFilterType());
                }
                break;
        }
    }
}
