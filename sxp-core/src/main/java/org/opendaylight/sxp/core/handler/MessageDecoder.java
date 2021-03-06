/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import java.io.IOException;
import java.net.SocketAddress;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles server and client sides of a channel.
 */
@Sharable
@SuppressWarnings("all")
public class MessageDecoder extends SimpleChannelInboundHandler<ByteBuf> {//NOSONAR

    private enum Profile {
        CLIENT, SERVER
    }


    private static final Logger LOG = LoggerFactory.getLogger(MessageDecoder.class.getName());

    /**
     * Creates MessageDecoder for Connection
     *
     * @param owner SxpNode that will be parent of connection
     * @return MessageDecoder
     */
    public static ChannelInboundHandler createClientProfile(SxpNode owner) {
        return new MessageDecoder(owner, Profile.CLIENT);
    }

    /**
     * Creates MessageDecoder for Node
     *
     * @param owner SxpNode that will own this decoder
     * @return MessageDecoder
     */
    public static ChannelInboundHandler createServerProfile(SxpNode owner) {
        return new MessageDecoder(owner, Profile.SERVER);
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
        String localAddressString = localAddress.toString();
        if (localAddressString.startsWith("/")) {
            localAddressString = localAddressString.substring(1);
        }
        String remoteAddressString = remoteAddress.toString();
        if (remoteAddressString.startsWith("/")) {
            remoteAddressString = remoteAddressString.substring(1);
        }
        return owner.toString() + "[" + localAddressString + "/" + remoteAddressString + "]";
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
                message =
                        MessageFactory.createError(messageValidationException.getErrorCode(),
                                messageValidationException.getErrorSubCode(), messageValidationException.getData());
            }
            if (ctx == null) {
                ctx = connection.getChannelHandlerContext(ChannelHandlerContextType.SPEAKER_CNTXT);
            }
        } catch (ErrorCodeDataLengthException e) {
            LOG.info("{} ERROR sending Error {}", connection, messageValidationException, e);
            return;
        } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
            LOG.info("{} ERROR sending Error {}", connection, messageValidationException, e);
            if (message != null) {
                message.release();
            }
            return;
        }
        LOG.info("{} Sent ERROR {}", connection, MessageFactory.toString(message));
        ctx.writeAndFlush(message);
        ctx.close();
    }

    private final SxpNode owner;

    private final Profile profile;

    /**
     * Constructor that sets predefined values
     *
     * @param owner   SxpNode associated with decoder
     * @param profile Profile that defines Server or Client
     */
    private MessageDecoder(SxpNode owner, Profile profile) {
        super();
        this.owner = owner;
        this.profile = profile;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws SocketAddressNotRecognizedException {
        LOG.info("Channel active for context {}", ctx);
        Channel channel = ctx.channel();
        SocketAddress remoteAddr = channel.remoteAddress();
        SxpConnection connection = owner.getConnection(remoteAddr);
        if (connection == null) {
            LOG.warn(getLogMessage(owner, ctx, "Channel activation"));
            ctx.close();
            return;
        }
        if (profile.equals(Profile.SERVER)) {
            connection.setInetSocketAddresses(channel.localAddress());
            connection.addChannelHandlerContext(ctx);
            return;
        }
        connection.setInetSocketAddresses(channel.localAddress());
        connection.addChannelHandlerContext(ctx);
        connection.getContext().executeChannelActivationStrategy(ctx, connection);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOG.warn(getLogMessage(owner, ctx, "Channel inactivation"));
        final SxpConnection connection = owner.getConnection(ctx.channel().remoteAddress());
        if (connection == null) {
            return;
        }
        connection.getContext().executeChannelInactivationStrategy(ctx, connection);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf message) {
        final SxpConnection connection = owner.getConnection(ctx.channel().remoteAddress());
        if (connection == null) {
            LOG.warn(getLogMessage(owner, ctx, "Channel read0"));
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
            } catch (Exception e) {
                LOG.warn(getLogMessage(owner, ctx, "Channel read") + ": {}", MessageFactory.toString(message), e);
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
        final SxpConnection connection = owner.getConnection(ctx.channel().remoteAddress());
        if (connection == null) {
            LOG.warn(getLogMessage(owner, ctx, "Channel exception"));
            return;
        }
        if (cause instanceof DecoderException) {
            if (cause.getCause() instanceof SSLHandshakeException) {
                LOG.warn("{} SSL invalid certificate {}", connection, cause.getCause().getMessage());
            } else if (cause.getCause() instanceof SSLException) {
                LOG.error("{} SSLException caught, shutting down", connection, cause);
            } else {
                LOG.warn("{} Decoder error {} shutting down", connection, cause);
            }
            connection.setStateOff(ctx);
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
