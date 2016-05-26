/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.io;

import com.google.common.util.concurrent.AbstractCheckedFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class) @PrepareForTest({DatastoreAccess.class, org.opendaylight.sxp.core.SxpNode.class,
        org.opendaylight.sxp.util.inet.Search.class, Configuration.class}) public class ConfigLoaderTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    private static DatastoreAccess access;

    @Before public void init() throws Exception {
        access = PowerMockito.mock(DatastoreAccess.class);
        PowerMockito.when(
                access.put(any(InstanceIdentifier.class), any(SxpNodeIdentity.class), any(LogicalDatastoreType.class)))
                .thenReturn(mock(AbstractCheckedFuture.class));
        PowerMockito.mockStatic(org.opendaylight.sxp.core.SxpNode.class, RETURNS_MOCKS);
        PowerMockito.mockStatic(org.opendaylight.sxp.util.inet.Search.class);
    }

    @Test public void testCreate() throws Exception {
    }
}
