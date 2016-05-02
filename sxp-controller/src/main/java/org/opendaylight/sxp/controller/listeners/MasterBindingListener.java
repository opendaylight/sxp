/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MasterBindingListener extends SxpDataChangeListener<MasterDatabaseBinding> {

    public MasterBindingListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess);
    }

    @Override protected void handleOperational(DataObjectModification<MasterDatabaseBinding> c,
            InstanceIdentifier<SxpNodeIdentity> identifier) {
        // TODO re-implement to handle direct adding to MASTER-DB and finding replaces bindings when something was deleted
    }

    @Override protected InstanceIdentifier<MasterDatabaseBinding> getIdentifier(MasterDatabaseBinding d,
            InstanceIdentifier<SxpNodeIdentity> parentIdentifier) {
        return parentIdentifier.child(MasterDatabase.class)
                .child(MasterDatabaseBinding.class, new MasterDatabaseBindingKey(d.getIpPrefix()));
    }

    @Override public <R extends DataObject> Class<R> getWatchedContainer() {
        //noinspection unchecked
        return (Class<R>) MasterDatabase.class;
    }
}
