/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.sxp.util.time.ManagedTimer;
import org.opendaylight.sxp.util.time.node.TimerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Connector extends Service {

    protected static final Logger LOG = LoggerFactory.getLogger(Connector.class.getName());

    public Connector(SxpNode owner) {
        super(owner);
    }

    @Override
    public void run() {
        LOG.debug(owner + " Starting {}", Connector.class.getSimpleName());

        ManagedTimer ntRetryOpen = owner.getTimer(TimerType.RetryOpenTimer);
        while (!finished) {
            try {
                Thread.sleep(THREAD_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }

            if (!owner.isEnabled()) {
                continue;
            } else if (owner.getAllOffConnections().isEmpty()) {
                continue;
            }
            // Timer not defined.
            if (ntRetryOpen == null) {
                try {
                    ntRetryOpen = owner.setTimer(TimerType.RetryOpenTimer, owner.getRetryOpenTime());
                } catch (UnknownTimerTypeException e) {
                    LOG.warn(owner + " {} {} | {}", Connector.class.getSimpleName(), e.getClass().getSimpleName(),
                            e.getMessage());
                    cancel();
                    continue;
                }
            }
            if (!ntRetryOpen.isRunning()) {
                if (ntRetryOpen.isDone()) {
                    try {
                        ntRetryOpen = TimerFactory.copyTimer(ntRetryOpen);
                    } catch (UnknownTimerTypeException e) {
                        LOG.error(owner + " {}", e.getClass());
                    }
                }
                try {
                    ntRetryOpen.start();
                } catch (Exception e) {
                    LOG.warn(owner + " Node retry open start | {} | [done='{}']", e.getClass().getSimpleName(),
                            ntRetryOpen.isDone());
                }
            }
        }
        ntRetryOpen.stopForce();
        LOG.info(owner + " Shutdown {}", getClass().getSimpleName());
    }
}
