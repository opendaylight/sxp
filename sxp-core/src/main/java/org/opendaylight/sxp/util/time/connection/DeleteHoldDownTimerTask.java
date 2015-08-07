/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time.connection;

import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.util.time.SxpTimerTask;

/**
 * DeleteHoldDownTimerTask is trigger when a connection is on listener side is torn down.
 * The bindings learnt are not deleted immediately but being held off,
 * for the delete hold down timer period. (The default timer period is 120 seconds)
 */
public class DeleteHoldDownTimerTask extends SxpTimerTask<Void> {

    private final SxpConnection connection;

    /**
     * Constructor that sets timer period, and set Connection
     *
     * @param connection SxpConnection that timer belongs to
     * @param period     Value representing time in some Time unit
     */
    public DeleteHoldDownTimerTask(SxpConnection connection, int period) {
        super(period);
        this.connection = connection;
    }

    @Override public Void call() {
        if (connection.isStateDeleteHoldDown() || connection.isStatePendingOn()) {
            LOG.info(connection.getOwner() + " Default{} [{}]", getClass().getSimpleName(), getPeriod());
            connection.purgeBindings();
        }
        return null;
    }
}
