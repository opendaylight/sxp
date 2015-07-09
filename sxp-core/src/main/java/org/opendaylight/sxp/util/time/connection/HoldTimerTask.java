/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time.connection;

import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.handler.MessageDecoder;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.time.SxpTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorSubCode;

import java.util.concurrent.TimeUnit;

public class HoldTimerTask extends SxpTimerTask<Void> {

    private final SxpConnection connection;

    public HoldTimerTask(SxpConnection connection, int period) {
        super(period);
        this.connection = connection;
    }

    @Override public Void call() throws Exception {
        LOG.debug(connection + " {} [{}]", getClass().getSimpleName(), getPeriod());

        if (connection.isStateOn() && connection.isModeListener() && connection.isVersion4()) {
            try {
                if (connection.getTimestampUpdateOrKeepAliveMessage()
                        < System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(getPeriod())) {
                        MessageDecoder.sendErrorMessage(connection.getChannelHandlerContext(
                                        SxpConnection.ChannelHandlerContextType.ListenerContext),
                                new ErrorMessageException(null, ErrorSubCode.UnacceptableHoldTime, null), connection);
                        connection.setDeleteHoldDownTimer();
                        LOG.info("{} State to DeleteHoldDown", connection);
                        return null;
                }
            } catch (Exception e) {
                LOG.warn(connection.getOwner() + " {} {} | {}", getClass().getSimpleName(),
                        e.getClass().getSimpleName(), e.getMessage());
            }
            connection.setTimer(TimerType.HoldTimer, getPeriod());
        }
        return null;
    }
}
