/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route;

import java.util.Objects;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.sxp.route.core.SxpClusterRouteManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: registers {@link SxpClusterRouteManager} when all necessary services are ready
 */
public class SxpRouteInstance implements BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SxpRouteInstance.class);

    private final SxpClusterRouteManager sxpClusterRouteManager;

    public SxpRouteInstance(final BindingAwareBroker broker, final SxpClusterRouteManager sxpClusterRouteManager) {
        LOG.info("Starting {} ..", getClass().getSimpleName());
        this.sxpClusterRouteManager = Objects.requireNonNull(sxpClusterRouteManager);
        broker.registerProvider(this);
        LOG.info("Sxp-controller lifecycle listener registered - routing ready");
    }

    @Override
    public void onSessionInitiated(final BindingAwareBroker.ProviderContext providerContext) {
        sxpClusterRouteManager.init();
    }
}
