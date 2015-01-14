/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time.node;

import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpConnection.ChannelHandlerContextType;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.time.ManagedTimer;
import org.opendaylight.sxp.util.time.SyncTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;

public class HoldTimerTask extends SyncTimerTask {

    protected HoldTimerTask(SxpNode owner, int period) {
        super(TimerType.HoldTimer, owner, period);
    }

    @Override
    protected void performAction() {
        if (owner.getServerPort() < 1) {
            done();
            return;
        }
        LOG.debug(owner + " Default{} [{}]", getClass().getSimpleName(), getPeriod());

        long currentTime = System.currentTimeMillis();
        List<SxpConnection> connections = owner.getAllOnConnections();
        for (SxpConnection connection : connections) {
            if (!connection.isModeListener()) {
                continue;
            } else if (connection.isVersion123()) {
                continue;
            }
            // Connection specific timer is used.
            if (connection.getTimer(TimerType.HoldTimer) != null) {
                continue;
            }
            try {
                if (currentTime - getPeriod() * ManagedTimer.TIME_FACTOR <= connection
                        .getTimestampUpdateOrKeepAliveMessage()) {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            ChannelHandlerContext ctx;
            try {
                if (connection.isModeBoth()) {
                    ctx = connection.getChannelHandlerContext(ChannelHandlerContextType.ListenerContext);
                } else {
                    ctx = connection.getChannelHandlerContext();
                }
            } catch (Exception e) {
                LOG.warn(owner + " Default{} {} | {}", getClass().getSimpleName(), e.getClass().getSimpleName(),
                        e.getMessage());
                continue;
            }
            connection.setStateOff(ctx);
            LOG.info("{} State to Off", connection);
        }
        done();
    }
}