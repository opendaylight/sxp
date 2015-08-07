/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * SxpTimerTask representing Abstract parent for all timers used in SXP
 *
 * @param <T> Type used as return value that each timer returns
 */
public abstract class SxpTimerTask<T> implements Callable<T> {

    protected static final Logger LOG = LoggerFactory.getLogger(SxpTimerTask.class.getName());
    private final int period;

    /**
     * Default constructor that only sets period after which timer will be triggered
     *
     * @param period Value representing time in some Time unit
     */
    protected SxpTimerTask(int period) {
        this.period = period;
    }

    /**
     * Gets period of Timer after which it will be executed
     *
     * @return Value representing time in some Time unit
     */
    public int getPeriod() {
        return period;
    }
}
