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
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.sxp.util.time.ManagedTimer;
import org.opendaylight.sxp.util.time.SyncTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;

public final class TimerFactory {

    public static ManagedTimer copyTimer(ManagedTimer timer) throws UnknownTimerTypeException,
            ChannelHandlerContextNotFoundException, ChannelHandlerContextDiscrepancyException {
        if (!(timer.getTimerTask() instanceof ConnectionTimerTask)) {
            throw new UnknownTimerTypeException("Timer task class not supported '"
                    + timer.getTimerTask().getClass().getSimpleName() + "'");
        }
        return createTimer(timer.getTimerType(), timer.getPeriod() / ManagedTimer.TIME_FACTOR, timer.getOwner(),
                ((ConnectionTimerTask) timer.getTimerTask()).connection,
                ((ConnectionTimerTask) timer.getTimerTask()).ctx);
    }

    private static ManagedTimer create(SyncTimerTask timerTask) {
        return new ManagedTimer(timerTask, timerTask.getPeriod() * ManagedTimer.TIME_FACTOR);
    }

    public static ManagedTimer createTimer(TimerType timerType, int period, SxpNode owner, SxpConnection connection,
            ChannelHandlerContext ctx) throws ChannelHandlerContextNotFoundException,
            ChannelHandlerContextDiscrepancyException, UnknownTimerTypeException {
        switch (timerType) {
        case DeleteHoldDownTimer:
            return create(new DeleteHoldDownTimerTask(owner, period, connection, ctx));
        case HoldTimer:
            return create(new HoldTimerTask(owner, period, connection, ctx));
        case KeepAliveTimer:
            return create(new KeepAliveTimerTask(owner, period, connection, ctx));
        case ReconciliationTimer:
            return create(new ReconcilationTimerTask(owner, period, connection, ctx));
        default:
            throw new UnknownTimerTypeException(timerType);
        }
    }
}
