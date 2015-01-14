/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time.node;

import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.sxp.util.time.ManagedTimer;
import org.opendaylight.sxp.util.time.SyncTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;

public final class TimerFactory {

    public static ManagedTimer copyTimer(ManagedTimer timer) throws UnknownTimerTypeException {
        return createTimer(timer.getTimerType(), timer.getPeriod() / ManagedTimer.TIME_FACTOR, timer.getOwner());
    }

    private static ManagedTimer create(SyncTimerTask timerTask) {
        return new ManagedTimer(timerTask, timerTask.getPeriod() * ManagedTimer.TIME_FACTOR);
    }

    /**
     * @param timerType
     * @param period
     *            [seconds]
     * @return
     * @throws UnknownTimerTypeException
     */
    public static ManagedTimer createTimer(TimerType timerType, int period, SxpNode owner)
            throws UnknownTimerTypeException {

        switch (timerType) {
        case HoldTimer:
            return create(new HoldTimerTask(owner, period));
        case KeepAliveTimer:
            return create(new KeepAliveTimerTask(owner, period));
        case RetryOpenTimer:
            return create(new RetryOpenTimerTask(owner, period));
        default:
            throw new UnknownTimerTypeException(timerType);
        }
    }
}
