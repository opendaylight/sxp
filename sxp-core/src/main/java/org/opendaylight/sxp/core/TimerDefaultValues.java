/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

public final class TimerDefaultValues {

    private static final int DELETE_HOLD_DOWN_TIMER = 120;
    private static final int HOLD_TIMER = 90;
    private static final int HOLD_TIMER_MAX = 180;
    private static final int HOLD_TIMER_MIN = 90;
    private static final int HOLD_TIMER_MIN_ACCEPTABLE = 120;
    private static final int KEEP_ALIVE_TIMER = 30;
    private static final int RECONCILIATION_TIMER = 120;
    private static final int RETRY_OPEN_TIMER = 120;

    /**
     * Non-configurable
     */
    public final int getDeleteHoldDownTimer() {
        return DELETE_HOLD_DOWN_TIMER;
    }

    public final int getHoldTimer() {
        return HOLD_TIMER;
    }

    public final int getHoldTimerMax() {
        return HOLD_TIMER_MAX;
    }

    public final int getHoldTimerMin() {
        return HOLD_TIMER_MIN;
    }

    public final int getHoldTimerMinAcceptable() {
        return HOLD_TIMER_MIN_ACCEPTABLE;
    }

    public final int getKeepAliveTimer() {
        return KEEP_ALIVE_TIMER;
    }

    public final int getReconciliationTimer() {
        return RECONCILIATION_TIMER;
    }

    public final int getRetryOpenTimer() {
        return RETRY_OPEN_TIMER;
    }

}
