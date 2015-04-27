/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time.node;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpConnection.ChannelHandlerContextType;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.util.time.ManagedTimer;
import org.opendaylight.sxp.util.time.SyncTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;

public class KeepAliveTimerTask extends SyncTimerTask {

    protected KeepAliveTimerTask(SxpNode owner, int period) {
        super(TimerType.KeepAliveTimer, owner, period);
    }

    @Override
    public void performAction() {
        if (owner.getServerPort() < 1) {
            done();
            return;
        }
        LOG.debug(owner + " Default{} [{}]", getClass().getSimpleName(), getPeriod());

        List<SxpConnection> connections = owner.getAllOnConnections();
        for (SxpConnection connection : connections) {
            if (!connection.isModeSpeaker()) {
                continue;
            } else if (connection.isVersion123()) {
                continue;
            }
            // Connection specific timer is used.
            // TODO: Find out if we need this stuff
            if (connection.getTimer(TimerType.KeepAliveTimer) != null) {
                continue;
            }
            ChannelHandlerContext ctx;
            try {
                if (connection.isModeBoth()) {
                    ctx = connection.getChannelHandlerContext(ChannelHandlerContextType.SpeakerContext);
                } else {
                    ctx = connection.getChannelHandlerContext();
                }
            } catch (Exception e) {
                LOG.warn(owner + " Default{} {} | {}", getClass().getSimpleName(), e.getClass().getSimpleName(),
                        e.getMessage());
                continue;
            }
            try {
                if (connection.getTimestampUpdateMessageExport() + getPeriod() * ManagedTimer.TIME_FACTOR > System
                        .currentTimeMillis()) {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            ByteBuf keepalive = MessageFactory.createKeepalive();
            if(!ctx.isRemoved()){
                LOG.info("{} Sent KEEPALIVE {}", connection, MessageFactory.toString(keepalive));
                ctx.writeAndFlush(keepalive);
            }else LOG.warn("{} Can not send KEEPALIVE {}", connection, MessageFactory.toString(keepalive));
        }
        done();
    }
}