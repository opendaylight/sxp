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

public class RetryOpenTimerTask extends SxpTimerTask<Void> {

    private final SxpNode owner;

    public RetryOpenTimerTask(SxpNode owner, int period) {
        super(period);
        this.owner = owner;
    }

    @Override public Void call() throws Exception {
        if (owner.getServerPort() >= 1) {
            LOG.debug(owner + " Default{} [{}]", getClass().getSimpleName(), getPeriod());
            try {
                owner.openConnections();
            } catch (Exception e) {
                LOG.debug(owner + " Default{} [{}]", getClass().getSimpleName(), getPeriod());
            }
        }
        owner.setTimer(TimerType.RetryOpenTimer, getPeriod());
        return null;
    }
}
