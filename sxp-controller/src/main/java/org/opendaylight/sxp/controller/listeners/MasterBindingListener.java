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
import org.opendaylight.sxp.controller.core.SxpDatastoreNode;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class MasterBindingListener extends SxpDataChangeListener<MasterDatabaseBinding> {

    public MasterBindingListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess);
    }

    @Override
    protected DataTreeIdentifier<MasterDatabaseBinding> getDataTreeIdentifier(LogicalDatastoreType datastoreType) {
        return new DataTreeIdentifier<>(datastoreType, SUBSCRIBED_PATH.child(Node.class)
                .augmentation(SxpNodeIdentity.class)
                .child(MasterDatabase.class)
                .child(MasterDatabaseBinding.class));
    }

    private boolean isNonLocal(MasterDatabaseBinding binding) {
        return !(binding == null || binding.getPeerSequence() == null || binding.getPeerSequence().getPeer() == null
                || binding.getPeerSequence().getPeer().isEmpty());
    }

    @Override protected void handleNonConfig(DataTreeModification<MasterDatabaseBinding> c, String nodeId) {
        SxpDatastoreNode sxpNode = (SxpDatastoreNode) Configuration.getRegisteredNode(nodeId);
        if (sxpNode == null || isNonLocal(c.getRootNode().getDataBefore()) || isNonLocal(
                c.getRootNode().getDataAfter()))
            return;
        LOG.trace("Operational Modification MasterDatabase {}", c.getRootNode().getModificationType());
        // TODO re-implement to handle direct adding to MASTER-DB and finding replaces bindings when something was deleted
        switch (c.getRootNode().getModificationType()) {
            case WRITE:
            case SUBTREE_MODIFIED:
                if (c.getRootNode().getDataBefore() != null && c.getRootNode().getDataAfter() == null) {
                    //Binding update
                    break;
                } else if (c.getRootNode().getDataBefore() == null) {
                    //Binding add
                    break;
                }
            case DELETE:
                //Binding update
                break;
        }
    }
}
