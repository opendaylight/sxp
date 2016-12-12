/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import java.util.Objects;
import org.opendaylight.sxp.controller.core.DatastoreAccess;

/**
 * Purpose: create follower synchronization status tasks
 */
public class FollowerSyncStatusTaskFactory {

    private final int period;
    private final int consequentFailLimit;

    /**
     * @param period              repeat period [s]
     * @param consequentFailLimit max.amount of consequent DS read failures
     */
    public FollowerSyncStatusTaskFactory(final int period, final int consequentFailLimit) {
        this.period = period;
        this.consequentFailLimit = consequentFailLimit;
    }

    public FollowerSyncStatusTask createFollowerSyncStatusTask(final DatastoreAccess datastoreAccess) {
        return new FollowerSyncStatusTask(period, Objects.requireNonNull(datastoreAccess), consequentFailLimit);
    }
}
