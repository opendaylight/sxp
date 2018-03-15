/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.time.connection;

import io.netty.util.concurrent.Future;
import java.util.concurrent.Callable;
import org.opendaylight.sxp.core.SxpConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryOpenTimerTask implements Callable<Future<Void>> {

    private static final Logger LOG = LoggerFactory.getLogger(RetryOpenTimerTask.class);

    private final SxpConnection sxpConnection;

    public RetryOpenTimerTask(SxpConnection sxpConnection) {
        this.sxpConnection = sxpConnection;
    }

    @Override
    public Future<Void> call() {
        LOG.info("Retry Open timer has expired, about to reconnect {}", sxpConnection);
        return sxpConnection.openConnection();
    }
}