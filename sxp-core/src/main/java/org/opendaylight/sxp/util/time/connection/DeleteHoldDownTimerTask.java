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

public class DeleteHoldDownTimerTask extends SxpTimerTask<Void> {

    private final SxpConnection connection;

    public DeleteHoldDownTimerTask(SxpConnection connection, int period) {
        super(period);
        this.connection = connection;
    }

    @Override public Void call() throws Exception {
        if (connection.isStateDeleteHoldDown()) {
            LOG.info(connection.getOwner() + " Default{} [{}]", getClass().getSimpleName(), getPeriod());
            try {
                connection.purgeBindings();
            } catch (Exception e) {
                LOG.warn(connection.getOwner() + " {} {} | {}", getClass().getSimpleName(),
                        e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return null;
    }
}
