/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time.node;

import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.time.SyncTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;

public class RetryOpenTimerTask extends SyncTimerTask {
    public static RetryOpenTimerTask create(SxpNode owner, int period) {
        return new RetryOpenTimerTask(owner, period);
    }

    protected RetryOpenTimerTask(SxpNode owner, int period) {
        super(TimerType.RetryOpenTimer, owner, period);
    }

    @Override
    public void performAction() {
        if (owner.getServerPort() < 1) {
            done();
            return;
        }
        LOG.debug(owner + " Default{} [{}]", getClass().getSimpleName(), getPeriod());
        try {
            owner.openConnections();
        } catch (Exception e) {
            e.printStackTrace();
        }
        done();
    }
}