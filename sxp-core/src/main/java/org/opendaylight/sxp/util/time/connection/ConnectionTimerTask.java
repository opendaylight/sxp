/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time.connection;

import io.netty.channel.ChannelHandlerContext;

import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.time.SyncTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;

public abstract class ConnectionTimerTask extends SyncTimerTask {

    protected SxpConnection connection;

    protected ChannelHandlerContext ctx;

    protected ConnectionTimerTask(TimerType timerType, SxpNode owner, int period, SxpConnection connection,
            ChannelHandlerContext ctx) throws ChannelHandlerContextNotFoundException,
            ChannelHandlerContextDiscrepancyException {
        super(timerType, owner, period);

        this.connection = connection;
        this.ctx = ctx;
    }

    @Override
    protected void performAction() {
        LOG.info(owner + " {} done", ConnectionTimerTask.class.getSimpleName());
        done();
    }
}