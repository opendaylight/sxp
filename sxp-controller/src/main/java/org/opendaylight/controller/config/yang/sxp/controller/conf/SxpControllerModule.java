/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.sxp.controller.conf;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.common.util.NoopAutoCloseable;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.util.io.ConfigLoader;

import static org.opendaylight.controller.config.yang.sxp.controller.conf.SxpControllerInstance.initTopology;

public class SxpControllerModule
        extends org.opendaylight.controller.config.yang.sxp.controller.conf.AbstractSxpControllerModule {

    public SxpControllerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SxpControllerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.sxp.controller.conf.SxpControllerModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override public java.lang.AutoCloseable createInstance() {
        try (DatastoreAccess datastoreAccess = DatastoreAccess.getInstance(getDataBrokerDependency())) {
            initTopology(datastoreAccess, LogicalDatastoreType.OPERATIONAL);
            initTopology(datastoreAccess, LogicalDatastoreType.CONFIGURATION);
            new ConfigLoader(datastoreAccess).load(getSxpController());
        }
        return NoopAutoCloseable.INSTANCE;
    }

    @Override public void customValidation() {
        // Add custom validation form module attributes here.
    }
}
