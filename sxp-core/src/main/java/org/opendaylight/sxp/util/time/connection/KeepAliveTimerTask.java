/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time.connection;

import io.netty.buffer.ByteBuf;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.time.SxpTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.TimerType;

/**
 * KeepAliveTimerTask is used by SXP speaker to send KEEPALIVE message
 * in order to indicate to the listener that the connection remains live.
 * (The default timer period is 1/3 of HoldTimerTask period)
 */
public class KeepAliveTimerTask extends SxpTimerTask<Void> {

    private final SxpConnection connection;

    /**
     * Constructor that sets timer period, and set Connection
     *
     * @param connection SxpConnection that timer belongs to
     * @param period     Value representing time in some Time unit
     */
    public KeepAliveTimerTask(SxpConnection connection, int period) {
        super(period);
        this.connection = connection;
    }

    @Override
    public Void call() {
        LOG.debug(connection + " {} [{}]", getClass().getSimpleName(), getPeriod());

        if (connection.isStateOn(SxpConnection.ChannelHandlerContextType.SpeakerContext) && connection.isModeSpeaker()
                && connection.isVersion4()) {
            ByteBuf keepAlive = MessageFactory.createKeepalive();
            try {
                LOG.info("{} Sent KEEPALIVE {}", connection, MessageFactory.toString(keepAlive));
                connection.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.SpeakerContext)
                        .writeAndFlush(keepAlive);
            } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
                LOG.warn("{} ERROR sending KEEPALIVE ", connection, e);
                keepAlive.release();
            }
            connection.setTimer(TimerType.KeepAliveTimer, getPeriod());
        }
        return null;
    }
}
