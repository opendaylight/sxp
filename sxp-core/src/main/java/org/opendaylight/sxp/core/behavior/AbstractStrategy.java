/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.behavior;

import io.netty.channel.ChannelHandlerContext;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStrategy implements Strategy {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractStrategy.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void onChannelInactivation(ChannelHandlerContext ctx, SxpConnection connection) {
        LOG.info("{} Handling channel inactivation on context {}", connection, ctx);
        SxpConnection.ChannelHandlerContextType type = connection.getContextType(ctx);
        if (connection.isStateOn(type)) {
            switch (type) {
                case LISTENER_CNTXT:
                    connection.setDeleteHoldDownTimer();
                    return;
                case SPEAKER_CNTXT:
                    LOG.trace("Context status on channel inactivation: {}, {}, writing purgeall", ctx.isRemoved(), ctx.channel());
                    ctx.writeAndFlush(MessageFactory.createPurgeAll());
                    break;
            }
            connection.setStateOff(ctx);
            if (connection.getInitCtxs().isEmpty()) {
                connection.scheduleRetryOpen();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onException(ChannelHandlerContext ctx, SxpConnection connection) {
        LOG.warn("{} onException", connection);
    }
}
