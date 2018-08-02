/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.util.time.SxpTimerTask;

/**
 * Purpose: provides logic for checking if cluster nodes are healthy
 */
public class FollowerSyncStatusTask extends SxpTimerTask<Boolean> {

    private final DatastoreAccess datastoreAccess;
    private int datastoreConsequentFailCounter;
    private final int consequentFailLimit;

    /**
     * Default constructor that only sets period after which timer will be triggered
     *
     * @param period          Value representing time in some Time unit
     * @param datastoreAccess way to readDS synchronously
     */
    protected FollowerSyncStatusTask(int period, final DatastoreAccess datastoreAccess, final int consequentFailLimit) {
        super(period);
        this.datastoreAccess = Objects.requireNonNull(datastoreAccess);
        this.consequentFailLimit = consequentFailLimit;
    }

    @Override
    public Boolean call() throws Exception {
        LOG.debug("Timer tick: check for cluster health");
        Preconditions.checkState(datastoreConsequentFailCounter < consequentFailLimit,
                "Last tick detected cluster isolated condition");

        boolean datastoreFailed = false;
        try {
            datastoreFailed =
                    Objects.isNull(datastoreAccess.readSynchronous(NodeIdentityListener.SUBSCRIBED_PATH,
                            LogicalDatastoreType.CONFIGURATION));

        } catch (IllegalStateException e) {
            datastoreFailed = true;
        }
        if (datastoreFailed) {
            datastoreConsequentFailCounter++;
        } else {
            datastoreConsequentFailCounter = 0;
        }

        return datastoreConsequentFailCounter < consequentFailLimit;
    }

    @Override
    public int getPeriod() {
        if (datastoreConsequentFailCounter != 0) {
            return 1;
        } else {
            return super.getPeriod();
        }
    }
}
