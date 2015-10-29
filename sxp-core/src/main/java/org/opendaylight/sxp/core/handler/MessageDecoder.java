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
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpConnection.ChannelHandlerContextType;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.core.messaging.legacy.LegacyMessageFactory;
import org.opendaylight.sxp.util.exception.ErrorCodeDataLengthException;
import org.opendaylight.sxp.util.exception.ErrorMessageReceivedException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.exception.connection.SocketAddressNotRecognizedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageConnectionStateException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpConnectionException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;

/** Handles server and client sides of a channel. */
@Sharable
public class MessageDecoder extends SimpleChannelInboundHandler<ByteBuf> {

    private static enum Profile {
        Client, Server
    }

    private static final Logger LOG = LoggerFactory.getLogger(MessageDecoder.class.getName());

    /**
     * Creates MessageDecoder for Connection
     *
     * @param owner SxpNode that will be parent of connection
     * @return MessageDecoder
     */
    public static ChannelInboundHandler createClientProfile(SxpNode owner) {
        return new MessageDecoder(owner, Profile.Client);
    }

    /**
     * Creates MessageDecoder for Node
     *
     * @param owner SxpNode that will own this decoder
     * @return MessageDecoder
     */
    public static ChannelInboundHandler createServerProfile(SxpNode owner) {
        return new MessageDecoder(owner, Profile.Server);
    }

    /**
     * Generate String representation of Node
     *
     * @param owner         SxpNode
     * @param localAddress  SocketAddress representing local address of Node
     * @param remoteAddress SocketAddress representing remote address of Node
     * @return String representing specified values
     */
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

    /**
     * Generate log message with provided values
     *
     * @param owner   SxpNode used
     * @param ctx     ChannelHandlerContext used
     * @param message String message with additional info
     * @return String representation of these values used for Log
     */
    private static String getLogMessage(SxpNode owner, ChannelHandlerContext ctx, String message) {
        return getLogMessage(owner, ctx, message, null);
    }

    /**
     * Generate log message with provided values
     *
     * @param owner   SxpNode used
     * @param ctx     ChannelHandlerContext used
     * @param message String message with additional info
     * @param e       Exception used
     * @return String representation of these values used for Log
     */
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

    /**
     * Send predefined Error message to peer and afterwards close
     * appropriate ChannelHandlerContext
     *
     * @param ctx                        ChannelHandlerContext used for communication, if is null
     *                                   use SpeakerContext ChannelHandler from connection fot communication
     * @param messageValidationException ErrorMessageException that will be send
     * @param connection                 SxpConnection associated with peer
     */
    public static void sendErrorMessage(ChannelHandlerContext ctx, ErrorMessageException messageValidationException,
                                        SxpConnection connection) {
        ByteBuf message = null;
        try {
            if (messageValidationException.isLegacy()) {
                message = LegacyMessageFactory.createError(messageValidationException.getErrorCodeNonExtended(), null);
            } else {
                message = MessageFactory.createError(messageValidationException.getErrorCode(),
                        messageValidationException.getErrorSubCode(), messageValidationException.getData());
            }
            if (ctx == null) {
                ctx = connection.getChannelHandlerContext(ChannelHandlerContextType.SpeakerContext);
            }
        } catch (ErrorCodeDataLengthException e) {
            LOG.info("{} ERROR sending Error {}", connection, messageValidationException, e);
            return;
        } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
            LOG.info("{} ERROR sending Error {}", connection, messageValidationException, e);
            message.release();
            return;
        }
        LOG.info("{} Sent ERROR {}", connection, MessageFactory.toString(message));
        ctx.writeAndFlush(message);
        ctx.close();
    }

    private SxpNode owner;

    private Profile profile;

    /**
     * Constructor that sets predefined values
     *
     * @param owner SxpNode associated with decoder
     * @param profile Profile that defines Server or Client
     */
    private MessageDecoder(SxpNode owner, Profile profile) {
        super();
        this.owner = owner;
        this.profile = profile;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws SocketAddressNotRecognizedException {
        SxpConnection connection;
        try {
            connection = owner.getConnection(ctx.channel().remoteAddress());
        } catch (SocketAddressNotRecognizedException | UnknownSxpConnectionException e) {
            LOG.warn(getLogMessage(owner, ctx, "Channel activation", e));
            ctx.close();
            return;
        }
        // Connection is already functional, do not add new channel context.
        if (connection.isStateOn() && (!connection.isModeBoth() || connection.isBidirectionalBoth())) {
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
        if (!connection.isModeBoth() || !connection.isStateOn(ChannelHandlerContextType.ListenerContext))
        {
            connection.getContext().executeChannelActivationStrategy(ctx, connection);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SxpConnection connection;
        try {
            connection = owner.getConnection(ctx.channel().remoteAddress());
        } catch (SocketAddressNotRecognizedException | UnknownSxpConnectionException e) {
            LOG.warn(getLogMessage(owner, ctx, "Channel inactivation", e));
            return;
        }
        connection.getContext().executeChannelInactivationStrategy(ctx, connection);
        LOG.warn(getLogMessage(owner, ctx, "Channel inactivation"));
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf message) {
       // LOG.debug(getLogMessage(owner, ctx, "Input received", null) + ": {}", MessageFactory.toString(message));

        SxpConnection connection;
        try {
            connection = owner.getConnection(ctx.channel().remoteAddress());
        } catch (SocketAddressNotRecognizedException | UnknownSxpConnectionException e) {
            LOG.warn(getLogMessage(owner, ctx, "Channel read0", e));
            return;
        }
        while (message.readableBytes() != 0) {
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
            } catch (ErrorMessageReceivedException | UpdateMessageConnectionStateException e) {
                // Filter of error messages.
                LOG.warn(getLogMessage(owner, ctx, "", e));
                connection.setStateOff(ctx);
                break;
            } catch (IndexOutOfBoundsException e) {
                LOG.info(getLogMessage(owner, ctx, "Unsupported message input: " + MessageFactory.toString(message),
                        null));
                break;
            } catch (Exception e) {
                LOG.warn(getLogMessage(owner, ctx, "Channel read", e) + ": {}", MessageFactory.toString(message));
                break;
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        SxpConnection connection;
        try {
            connection = owner.getConnection(ctx.channel().remoteAddress());
        } catch (SocketAddressNotRecognizedException | UnknownSxpConnectionException e) {
            LOG.warn(getLogMessage(owner, ctx, "Channel exception", e));
            return;
        }
        if (cause instanceof IOException) {
            LOG.debug("IO error {} shutting down connection {}", cause, connection);
            connection.setStateOff(ctx);
            return;
        }
        connection.getContext().executeExceptionCaughtStrategy(ctx, connection);
        LOG.warn(getLogMessage(owner, ctx, "Channel exception"), cause);
    }
}
