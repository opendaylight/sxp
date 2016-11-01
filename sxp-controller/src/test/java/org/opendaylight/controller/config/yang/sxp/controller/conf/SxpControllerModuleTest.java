/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.sxp.controller.conf;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.core.*;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class) @PrepareForTest({DatastoreAccess.class}) public class SxpControllerModuleTest {

    private SxpControllerModule controllerModule;
    private DatastoreAccess datastoreAccess;
    private ModuleIdentifier moduleIdentifier;
    private DependencyResolver dependencyResolver;

    @Before public void init() {
        PowerMockito.mockStatic(DatastoreAccess.class);
        datastoreAccess = mock(DatastoreAccess.class);
        dependencyResolver = mock(DependencyResolver.class);
        moduleIdentifier = mock(ModuleIdentifier.class);
        PowerMockito.when(DatastoreAccess.getInstance(any(DataBroker.class))).thenReturn(datastoreAccess);
    }

    @Test public void createInstance_1() throws Exception {
        controllerModule = new SxpControllerModule(moduleIdentifier, dependencyResolver);

        AutoCloseable result = controllerModule.createInstance();
        assertNotNull(result);
        verify(datastoreAccess).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.CONFIGURATION));
        verify(datastoreAccess).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
    }

    @Test public void createInstance_2() throws Exception {
        controllerModule =
                new SxpControllerModule(moduleIdentifier, dependencyResolver,
                        new SxpControllerModule(moduleIdentifier, dependencyResolver), () -> {
                });

        AutoCloseable result = controllerModule.createInstance();
        assertNotNull(result);
        verify(datastoreAccess).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.CONFIGURATION));
        verify(datastoreAccess).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
    }

}
