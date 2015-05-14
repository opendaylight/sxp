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

public abstract class SxpTimerTask<T> implements Callable<T> {

    protected static final Logger LOG = LoggerFactory.getLogger(SxpTimerTask.class.getName());
    private final int period;

    protected SxpTimerTask(int period) {
        this.period = period;
    }

    public int getPeriod() {
        return period;
    }
}
