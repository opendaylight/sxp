/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import static org.opendaylight.sxp.controller.listeners.spi.Listener.Differences.checkFilterEntries;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.spi.ContainerListener;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FilterListener extends ContainerListener<SxpPeerGroup, SxpFilter> {

    public FilterListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess, SxpFilter.class);
    }

    @Override
    protected void handleOperational(DataObjectModification<SxpFilter> c, InstanceIdentifier<SxpPeerGroup> identifier,
            SxpNode sxpNode) {
        final String groupName = identifier.firstKeyOf(SxpPeerGroup.class).getName();
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
                if (checkFilterEntries(c.getDataBefore() == null ? null : c.getDataBefore().getFilterEntries(),
                        c.getDataAfter() == null ? null : c.getDataAfter().getFilterEntries())) {
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

    @Override
    protected InstanceIdentifier<SxpFilter> getIdentifier(SxpFilter d,
            InstanceIdentifier<SxpPeerGroup> parentIdentifier) {
        return parentIdentifier.child(SxpFilter.class, new SxpFilterKey(d.getFilterSpecific(), d.getFilterType()));
    }

    /**
     * @param sxpNode    Node where filter will be added
     * @param c          Object modification containing necessary data
     * @param groupName  Group name specifying where filter will be added
     * @param identifier InstanceIdentifier pointing to provided filter
     */
    private void addFilterToGroup(final SxpNode sxpNode, DataObjectModification<SxpFilter> c, final String groupName,
            final InstanceIdentifier<SxpFilter> identifier) {
        final DatastoreAccess datastoreAccess = getDatastoreAccess(NodeIdConv.toString(sxpNode.getNodeId()));
        if (!sxpNode.addFilterToPeerGroup(groupName, c.getDataAfter())) {
            if (!datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION))
                datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL);
        }
    }

}
