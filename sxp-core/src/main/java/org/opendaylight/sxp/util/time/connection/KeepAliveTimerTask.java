/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.util.time.SxpTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;

import java.util.concurrent.TimeUnit;

public class KeepAliveTimerTask extends SxpTimerTask<Void> {

    private final SxpConnection connection;

    public KeepAliveTimerTask(SxpConnection connection, int period) {
        super(period);
        this.connection = connection;
    }

    @Override public Void call() throws Exception {
        LOG.debug(connection + " {} [{}]", getClass().getSimpleName(), getPeriod());

        if (connection.isStateOn() && connection.isModeSpeaker() && connection.isVersion4()) {
            try {
                if (connection.getTimestampUpdateMessageExport() + TimeUnit.SECONDS.toMillis(getPeriod())
                        <= System.currentTimeMillis()) {
                    ChannelHandlerContext
                            ctx =
                            connection.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.SpeakerContext);
                    ByteBuf keepalive = MessageFactory.createKeepalive();
                    if (!ctx.isRemoved()) {
                        LOG.info("{} Sent KEEPALIVE {}", connection, MessageFactory.toString(keepalive));
                        ctx.writeAndFlush(keepalive);
                    } else
                        LOG.warn("{} Can not send KEEPALIVE {}", connection, MessageFactory.toString(keepalive));
                }
            } catch (Exception e) {
                LOG.warn("{} ERROR sending KEEPALIVE ", connection, e);
            }
            connection.setTimer(TimerType.KeepAliveTimer, getPeriod());
        }
        return null;
    }
}
