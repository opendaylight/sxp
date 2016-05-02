/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.opendaylight.sxp.controller.listeners.SxpDataChangeListener.checkChange;

public class FilterListener {

    protected final DatastoreAccess datastoreAccess;

    public FilterListener(DatastoreAccess datastoreAccess) {
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);

    }

    public void handleOperational(DataObjectModification<SxpFilter> c, InstanceIdentifier<SxpPeerGroup> identifier) {
        final String nodeId = identifier.firstKeyOf(Node.class).getNodeId().getValue();
        SxpNode sxpNode = Configuration.getRegisteredNode(nodeId);
        if (sxpNode == null)
            return;
        String groupName = identifier.firstKeyOf(SxpPeerGroup.class).getName();
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

    public void handleConfig(DataObjectModification<SxpFilter> c, final InstanceIdentifier<SxpPeerGroup> identifier) {
        switch (c.getModificationType()) {
            case WRITE:
                if (c.getDataAfter() != null)
                    datastoreAccess.putSynchronous(getIdentifier(c.getDataAfter(), identifier), c.getDataAfter(),
                            LogicalDatastoreType.OPERATIONAL);
            case SUBTREE_MODIFIED:
                if (c.getDataAfter() != null)
                    break;
            case DELETE:
                datastoreAccess.checkAndDelete(getIdentifier(c.getDataBefore(), identifier),
                        LogicalDatastoreType.OPERATIONAL);
                break;
        }
    }

    private InstanceIdentifier<SxpFilter> getIdentifier(SxpFilter d, InstanceIdentifier<SxpPeerGroup> identifier) {
        return identifier.child(SxpFilter.class, new SxpFilterKey(d.getFilterSpecific(), d.getFilterType()));
    }

    private void addFilterToGroup(final SxpNode sxpNode, DataObjectModification<SxpFilter> c, final String groupName,
            final InstanceIdentifier<SxpFilter> identifier) {
        if (!sxpNode.addFilterToPeerGroup(groupName, c.getDataAfter())) {
            if (!datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION))
                datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL);
        }
    }
}
