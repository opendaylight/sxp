/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketAddress;

import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpConnection.ChannelHandlerContextType;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.core.messaging.legacy.LegacyMessageFactory;
import org.opendaylight.sxp.util.exception.ErrorMessageReceivedException;
import org.opendaylight.sxp.util.exception.connection.IncompatiblePeerModeException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageConnectionStateException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpConnectionException;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles server and client sides of a channel. */
@Sharable
public class MessageDecoder extends SimpleChannelInboundHandler<ByteBuf> {

    private static enum Profile {
        Client, Server
    }

    private static final Logger LOG = LoggerFactory.getLogger(MessageDecoder.class.getName());

    public static ChannelInboundHandler createClientProfile(SxpNode owner) {
        return new MessageDecoder(owner, Profile.Client);
    }

    public static ChannelInboundHandler createServerProfile(SxpNode owner) {
        return new MessageDecoder(owner, Profile.Server);
    }

    private static String getChannelHandlerContextId(SxpNode owner, SocketAddress localAddress,
            SocketAddress remoteAddress) {
        String _localAddress = localAddress.toString();
        if (_localAddress.startsWith("/")) {
            _localAddress = _localAddress.substring(1);
        }
        String _remoteAddress = remoteAddress.toString();
        if (_remoteAddress.startsWith("/")) {
            _remoteAddress = _remoteAddress.substring(1);
        }
        return owner.toString() + "[" + _localAddress + "/" + _remoteAddress + "]";
    }

    private static String getLogMessage(SxpNode owner, ChannelHandlerContext ctx, String message) {
        return getLogMessage(owner, ctx, message, null);
    }

    private static String getLogMessage(SxpNode owner, ChannelHandlerContext ctx, String message, Exception e) {
        SocketAddress localAddress = ctx.channel().localAddress();
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        String result = getChannelHandlerContextId(owner, localAddress, remoteAddress);
        if (message != null && !message.isEmpty()) {
            result += " " + message;
        }

        if (e != null && e instanceof ErrorMessageException) {
            e = ((ErrorMessageException) e).getCarriedException();
        }

        if (e != null) {
            if (message != null) {
                result += " | ";
            }

            result += e.getClass().getSimpleName();
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                result += " | " + e.getMessage();
            }
        }
        return result;
    }

    public static void sendErrorMessage(ChannelHandlerContext ctx, ErrorMessageException messageValidationException,
            SxpConnection connection) throws Exception {
        ByteBuf message = null;
        if (messageValidationException.isLegacy()) {
            message = LegacyMessageFactory.createError(messageValidationException.getErrorCodeNonExtended(), null);
        } else {
            message = MessageFactory.createError(messageValidationException.getErrorCode(),
                    messageValidationException.getErrorSubCode(), messageValidationException.getData());
        }
        if (ctx == null) {
            ctx = connection.getChannelHandlerContext(ChannelHandlerContextType.SpeakerContext);
        }

        LOG.info("{} Sent ERROR {}", connection, MessageFactory.toString(message));
        ctx.writeAndFlush(message);
        ctx.close();
    }

    private SxpNode owner;

    private Profile profile;

    private MessageDecoder(SxpNode owner, Profile profile) {
        super();
        this.owner = owner;
        this.profile = profile;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SxpConnection connection;
        try {
            connection = owner.getConnection(ctx.channel().remoteAddress());
        } catch (Exception e) {
            LOG.warn(getLogMessage(owner, ctx, "Channel activation", e));
            ctx.close();
            return;
        }
        // Connection is already functional, do not add new channel context.
        if (connection.isStateOn()) {
            ctx.close();
            return;
        }
        if (profile.equals(Profile.Server)) {
            // System.out.println("L" + ctx.channel().remoteAddress());
            connection.setInetSocketAddresses(ctx.channel().localAddress(), ctx.channel().remoteAddress());
            connection.addChannelHandlerContext(ctx);
            return;
        }

        // System.out.println("R" + ctx.channel().remoteAddress());
        connection.setInetSocketAddresses(ctx.channel().localAddress(), ctx.channel().remoteAddress());
        connection.addChannelHandlerContext(ctx);
        connection.getContext().executeChannelActivationStrategy(ctx, connection);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SxpConnection connection;
        try {
            connection = owner.getConnection(ctx.channel().remoteAddress());
        } catch (UnknownSxpConnectionException e) {
            LOG.warn(getLogMessage(owner, ctx, "Channel inactivation", e));
            return;
        }
        connection.getContext().executeChannelInactivationStrategy(ctx, connection);
        LOG.warn(getLogMessage(owner, ctx, "Channel inactivation"));
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf message) throws Exception {
       // LOG.debug(getLogMessage(owner, ctx, "Input received", null) + ": {}", MessageFactory.toString(message));

        SxpConnection connection;
        try {
            connection = owner.getConnection(ctx.channel().remoteAddress());
        } catch (UnknownSxpConnectionException e) {
            LOG.warn(getLogMessage(owner, ctx, "Channel read0", e));
            return;
        }
        do {
            // Execute selected strategy.
            try {
                Notification notification = connection.getContext().executeParseInput(message);
                connection.getContext().executeInputMessageStrategy(ctx, connection, notification);
            } catch (ErrorMessageException messageValidationException) {
                // Attributes validation: Low-level filter of non-valid
                // messages.
                Exception carriedException = messageValidationException.getCarriedException();
                if (carriedException != null) {
                    LOG.warn(getLogMessage(owner, ctx, "", carriedException));
                }
                sendErrorMessage(ctx, messageValidationException, connection);
                connection.setStateOff(ctx);
                break;
            } catch (IncompatiblePeerModeException | ErrorMessageReceivedException
                    | UpdateMessageConnectionStateException e) {
                // Filter of error messages.
                LOG.warn(getLogMessage(owner, ctx, "", e));
                connection.setStateOff(ctx);
                break;
            } catch (IndexOutOfBoundsException e) {
                LOG.info(getLogMessage(owner, ctx, "Unsupported message input: " + MessageFactory.toString(message),
                        null));
                e.printStackTrace();
                break;
            } catch (Exception e) {
                LOG.warn(getLogMessage(owner, ctx, "Channel read", e) + ": {}", MessageFactory.toString(message));
                e.printStackTrace();
                break;
            }
        } while (message.readableBytes() != 0);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SxpConnection connection;
        try {
            connection = owner.getConnection(ctx.channel().remoteAddress());
        } catch (UnknownSxpConnectionException e) {
            LOG.warn(getLogMessage(owner, ctx, "Channel exception", e));
            cause.printStackTrace();
            return;
        }
        connection.getContext().executeExceptionCaughtStrategy(ctx, connection);
        LOG.warn(getLogMessage(owner, ctx, "Channel exception"));
        cause.printStackTrace();
    }
}
