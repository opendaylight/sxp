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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class FilterListener extends SxpDataChangeListener<SxpFilter> {

    public FilterListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess);
    }

    @Override protected DataTreeIdentifier<SxpFilter> getDataTreeIdentifier(LogicalDatastoreType datastoreType) {
        return new DataTreeIdentifier<>(datastoreType, SUBSCRIBED_PATH.child(Node.class)
                .augmentation(SxpNodeIdentity.class)
                .child(SxpPeerGroups.class)
                .child(SxpPeerGroup.class)
                .child(SxpFilter.class));
    }

    private void addFilterToGroup(final SxpNode sxpNode, DataTreeModification<SxpFilter> c, final String groupName) {
        if (!sxpNode.addFilterToPeerGroup(groupName, c.getRootNode().getDataAfter())) {
            if (!datastoreAccess.checkAndDelete(c.getRootPath().getRootIdentifier(),
                    LogicalDatastoreType.CONFIGURATION))
                datastoreAccess.checkAndDelete(c.getRootPath().getRootIdentifier(), LogicalDatastoreType.OPERATIONAL);
        }
    }

    @Override protected void handleNonConfig(DataTreeModification<SxpFilter> c, String nodeId) {
        SxpNode sxpNode = Configuration.getRegisteredNode(nodeId);
        if (sxpNode == null)
            return;
        String groupName = c.getRootPath().getRootIdentifier().firstKeyOf(SxpPeerGroup.class).getName();
        LOG.trace("Operational Modification Filters {}", c.getRootNode().getModificationType());
        switch (c.getRootNode().getModificationType()) {
            case WRITE:
                if (c.getRootNode().getDataBefore() == null) {
                    addFilterToGroup(sxpNode, c, groupName);
                    break;
                } else if (c.getRootNode().getDataAfter() == null) {
                    sxpNode.removeFilterFromPeerGroup(groupName, c.getRootNode().getDataBefore().getFilterType(),
                            Preconditions.checkNotNull(c.getRootNode().getDataBefore()).getFilterSpecific());
                    break;
                }
            case SUBTREE_MODIFIED:
                if (checkChange(c, SxpFilterFields::getFilterEntries)) {
                    sxpNode.removeFilterFromPeerGroup(groupName,
                            Preconditions.checkNotNull(c.getRootNode().getDataBefore()).getFilterType(),
                            Preconditions.checkNotNull(c.getRootNode().getDataBefore()).getFilterSpecific());
                    addFilterToGroup(sxpNode, c, groupName);
                }
                break;
            case DELETE:
                sxpNode.removeFilterFromPeerGroup(groupName,
                        Preconditions.checkNotNull(c.getRootNode().getDataBefore()).getFilterType(),
                        Preconditions.checkNotNull(c.getRootNode().getDataBefore()).getFilterSpecific());
                break;
        }
    }
}
