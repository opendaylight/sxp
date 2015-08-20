/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time.node;

import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.time.SxpTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;

/**
 * RetryOpenTimerTask is triggered as long as there is one SXP connection on the device that is not up
 * and try to bring it up. (The default timer period is 120 seconds)
 */
public class RetryOpenTimerTask extends SxpTimerTask<Void> {

    private final SxpNode owner;

    /**
     * Constructor that sets timer period, and set node on which it will try to bring up Connections
     *
     * @param owner  SxpNode that timer belongs to
     * @param period Value representing time in some Time unit
     */
    public RetryOpenTimerTask(SxpNode owner, int period) {
        super(period);
        this.owner = owner;
    }

    @Override public Void call() {
        if (owner.isEnabled()) {
            LOG.debug(owner + " Default{} [{}]", getClass().getSimpleName(), getPeriod());
            owner.openConnections();
            owner.setTimer(TimerType.RetryOpenTimer, getPeriod());
        }
        return null;
    }
}
