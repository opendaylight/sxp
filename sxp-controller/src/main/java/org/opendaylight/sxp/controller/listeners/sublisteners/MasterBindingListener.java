/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.spi.ListListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.List;

public class MasterBindingListener extends ListListener<SxpNodeIdentity, MasterDatabase, MasterDatabaseBinding> {

    public MasterBindingListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess, MasterDatabase.class);
    }

    @Override protected void handleOperational(DataObjectModification<MasterDatabaseBinding> c,
            InstanceIdentifier<SxpNodeIdentity> identifier) {
        //TODO implement Binding handling
    }

    @Override protected InstanceIdentifier<MasterDatabaseBinding> getIdentifier(MasterDatabaseBinding d,
            InstanceIdentifier<SxpNodeIdentity> parentIdentifier) {
        return parentIdentifier.child(MasterDatabase.class)
                .child(MasterDatabaseBinding.class, new MasterDatabaseBindingKey(d.getIpPrefix()));
    }

    @Override public void handleChange(List<DataObjectModification<MasterDatabase>> modifiedChildContainer,
            LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<SxpNodeIdentity> identifier) {
        //TODO implement Binding handling
        super.handleChange(modifiedChildContainer, logicalDatastoreType, identifier);
    }

}
