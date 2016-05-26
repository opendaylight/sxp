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
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FilterListener extends ContainerListener<SxpPeerGroups, SxpFilter> {

    private String groupName;

    public FilterListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess);
    }

    @Override protected void handleOperational(DataObjectModification<SxpFilter> c,
            InstanceIdentifier<SxpNodeIdentity> identifier) {
        final String nodeId = identifier.firstKeyOf(Node.class).getNodeId().getValue();
        SxpNode sxpNode = Configuration.getRegisteredNode(nodeId);
        if (sxpNode == null) {
            LOG.error("Operational Modification {} {} could not get SXPNode {}", getClass(), c.getModificationType(),
                    nodeId);
            return;
        }
        LOG.trace("Operational Modification {} {}", getClass(), c.getModificationType());
        switch (c.getModificationType()) {
            case WRITE:
                if (c.getDataBefore() == null) {
                    addFilterToGroup(sxpNode, c, groupName, getIdentifier(c.getDataAfter(), identifier));
                    break;
                } else if (c.getDataAfter() == null) {
                    sxpNode.removeFilterFromPeerGroup(groupName, c.getDataBefore().getFilterType(),
                            Preconditions.checkNotNull(c.getDataBefore()).getFilterSpecific());
                    break;
                }
            case SUBTREE_MODIFIED:
                if (checkChange(c, SxpFilterFields::getFilterEntries)) {
                    sxpNode.removeFilterFromPeerGroup(groupName,
                            Preconditions.checkNotNull(c.getDataBefore()).getFilterType(),
                            Preconditions.checkNotNull(c.getDataBefore()).getFilterSpecific());
                    addFilterToGroup(sxpNode, c, groupName, getIdentifier(c.getDataAfter(), identifier));
                }
                break;
            case DELETE:
                sxpNode.removeFilterFromPeerGroup(groupName,
                        Preconditions.checkNotNull(c.getDataBefore()).getFilterType(),
                        Preconditions.checkNotNull(c.getDataBefore()).getFilterSpecific());
                break;
        }
    }

    @Override public void handleChange(DataObjectModification<SxpPeerGroups> modifiedChildContainer_,
            LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<SxpNodeIdentity> identifier) {
        if (modifiedChildContainer_ != null) {
            modifiedChildContainer_.getModifiedChildren().forEach(m -> {
                if (m.getDataType().equals(SxpPeerGroup.class)) {
                    //noinspection unchecked
                    DataObjectModification<SxpPeerGroup> g = (DataObjectModification<SxpPeerGroup>) m;
                    groupName =
                            g.getDataAfter() != null ? g.getDataAfter().getName() :
                                    g.getDataBefore() != null ? g.getDataBefore().getName() : null;
                    g.getModifiedChildren().forEach(sm -> {
                        if (sm.getDataType().equals(SxpFilter.class)) {
                            switch (logicalDatastoreType) {
                                case OPERATIONAL:
                                    //noinspection unchecked
                                    handleOperational((DataObjectModification<SxpFilter>) sm, identifier);
                                    break;
                                case CONFIGURATION:
                                    //noinspection unchecked
                                    handleConfig((DataObjectModification<SxpFilter>) sm, identifier);
                                    break;
                            }
                        }
                    });
                }
            });
        }
    }

    @Override protected InstanceIdentifier<SxpFilter> getIdentifier(SxpFilter d,
            InstanceIdentifier<SxpNodeIdentity> parentIdentifier) {
        return parentIdentifier.child(SxpPeerGroups.class)
                .child(SxpPeerGroup.class, new SxpPeerGroupKey(groupName))
                .child(SxpFilter.class, new SxpFilterKey(d.getFilterSpecific(), d.getFilterType()));
    }

    @Override public DataObjectModification<SxpPeerGroups> getModifications(
            DataTreeModification<SxpNodeIdentity> treeModification) {
        return treeModification.getRootNode().getModifiedChildContainer(SxpPeerGroups.class);
    }

    private void addFilterToGroup(final SxpNode sxpNode, DataObjectModification<SxpFilter> c, final String groupName,
            final InstanceIdentifier<SxpFilter> identifier) {
        if (!sxpNode.addFilterToPeerGroup(groupName, c.getDataAfter())) {
            if (!datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION))
                datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL);
        }
    }
}
