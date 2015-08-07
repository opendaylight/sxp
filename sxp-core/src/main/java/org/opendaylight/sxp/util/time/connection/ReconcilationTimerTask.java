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

public class ReconcilationTimerTask extends SxpTimerTask<Void> {

    private final SxpConnection connection;

    public ReconcilationTimerTask(SxpConnection connection, int period) {
        super(period);
        this.connection = connection;
    }

    @Override public Void call() {
        if (connection.isStateOn()) {
            LOG.info(connection.getOwner() + " Default{} [{}]", getClass().getSimpleName(), getPeriod());
            connection.cleanUpBindings();
        }
        return null;
    }
}
