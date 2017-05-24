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
 * ReconciliationTimerTask is trigger when a connection is brought up within the delete hold down timer period,
 * bindings are re-populated from the speaker side. At the same time, the old bindings
 * learnt before the connection goes down still holds. (The default timer period is 120 seconds)
 */
public class ReconcilationTimerTask extends SxpTimerTask<Void> {

    private final SxpConnection connection;

    /**
     * Constructor that sets timer period, and set Connection
     *
     * @param connection SxpConnection that timer belongs to
     * @param period     Value representing time in some Time unit
     */
    public ReconcilationTimerTask(SxpConnection connection, int period) {
        super(period);
        this.connection = connection;
    }

    @Override
    public Void call() {
        LOG.info(connection.getOwner() + " Default{} [{}]", getClass().getSimpleName(), getPeriod());
        if (connection.isStateOn()) {
            LOG.info(connection.getOwner() + " Default{} [{}]", getClass().getSimpleName(), getPeriod());
            connection.cleanUpBindings();
        }
        return null;
    }
}
