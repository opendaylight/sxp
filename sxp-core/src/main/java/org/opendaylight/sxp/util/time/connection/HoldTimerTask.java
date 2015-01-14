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
import org.opendaylight.sxp.util.time.ManagedTimer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;

public class HoldTimerTask extends ConnectionTimerTask {

    protected HoldTimerTask(SxpNode owner, int period, SxpConnection connection, ChannelHandlerContext ctx)
            throws ChannelHandlerContextNotFoundException, ChannelHandlerContextDiscrepancyException {
        super(TimerType.HoldTimer, owner, period, connection, ctx);
    }

    @Override
    protected void performAction() {
        LOG.debug(connection + " {} [{}]", getClass().getSimpleName(), getPeriod());

        if (connection.isStateOn() && connection.isModeListener() && connection.isVersion4()) {
            try {
                if (connection.getTimestampUpdateOrKeepAliveMessage() < System.currentTimeMillis() - getPeriod()
                        * ManagedTimer.TIME_FACTOR) {
                    connection.setStateOff(ctx);
                    LOG.info("{} State to Off", connection);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        done();
    }
}