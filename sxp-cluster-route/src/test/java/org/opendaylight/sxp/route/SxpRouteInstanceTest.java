/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sxp.route.core.SxpClusterRouteManager;

/**
 * Test for {@link SxpRouteInstance}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SxpRouteInstanceTest {

    @Mock private BindingAwareBroker bindingBroker;
    @Mock private BindingAwareBroker.ProviderContext context;
    @Mock private SxpClusterRouteManager sxpClusterRouteManager;
    private SxpRouteInstance routeInstance;

    @Before
    public void setUp() throws Exception {
        routeInstance = new SxpRouteInstance(bindingBroker, sxpClusterRouteManager);
    }

    @Test
    public void onSessionInitiated() throws Exception {
        routeInstance.onSessionInitiated(context);
        Mockito.verify(sxpClusterRouteManager).init();
    }

}
