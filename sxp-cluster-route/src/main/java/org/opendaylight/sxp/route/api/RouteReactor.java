/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.api;

import com.google.common.util.concurrent.FluentFuture;
import javax.annotation.Nullable;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinition;

/**
 * Purpose: process changes in {@link SxpClusterRoute} and  {@link RoutingDefinition}
 */
public interface RouteReactor {

    /**
     * Propagate change of managed virtual interface to system
     *
     * @param oldRoute previous value of configured {@link SxpClusterRoute}
     * @param newRoute next value of configured {@link SxpClusterRoute}
     * @return route update async outcome
     */
    FluentFuture<? extends CommitInfo> updateRouting(@Nullable SxpClusterRoute oldRoute, @Nullable SxpClusterRoute newRoute);

    /**
     * Remove all managed virtual interfaces
     *
     * @return route update async outcome
     */
    FluentFuture<? extends CommitInfo> wipeRouting();
}
