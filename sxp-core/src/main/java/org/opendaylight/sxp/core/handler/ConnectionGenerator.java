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
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.core.messaging.legacy.LegacyMessageFactory;
import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.ErrorCodeDataLengthException;
import org.opendaylight.sxp.util.exception.connection.SocketAddressNotRecognizedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionTemplateFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles server and client sides of a channel.
 */
@Sharable public class ConnectionGenerator extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionGenerator.class.getName());
    private SxpNode owner;

    /**
     * Constructor that sets predefined values
     *
     * @param owner SxpNode associated with decoder
     */
    public ConnectionGenerator(SxpNode owner) {
        super();
        this.owner = owner;
    }

    private SxpConnectionTemplateFields getTemplate(SocketAddress address) {
        SxpConnectionTemplateFields template = null;
        if (address instanceof InetSocketAddress) {
            for (SxpDomain sxpDomain : owner.getDomains()) {
                template = sxpDomain.getTemplate((InetSocketAddress) address);
                if (template != null) {
                    break;
                }
            }
        }
        return template;
    }

    @Override public void channelActive(ChannelHandlerContext ctx) throws SocketAddressNotRecognizedException {
        final SxpConnection connection = owner.getConnection(ctx.channel().remoteAddress());
        final SxpConnectionTemplateFields template = getTemplate(ctx.channel().remoteAddress());
        if (connection == null && template != null) {
            ctx.writeAndFlush(LegacyMessageFactory.createOpen(
                    template.getTemplateVersion() == null ? Version.Version4 : template.getTemplateVersion(),
                    template.getTemplateMode() == null ? ConnectionMode.Listener : template.getTemplateMode()));
        } else
            ctx.fireChannelActive();
    }

    @Override public void channelRead0(ChannelHandlerContext ctx, ByteBuf message) throws ErrorCodeDataLengthException {
        final SxpConnection connection = owner.getConnection(ctx.channel().remoteAddress());
        final SxpConnectionTemplateFields template = getTemplate(ctx.channel().remoteAddress());
        if (connection == null && template != null) {
            if (message.isReadable(
                    MessageFactory.MESSAGE_HEADER_TYPE_LENGTH + MessageFactory.MESSAGE_HEADER_LENGTH_LENGTH)) {
                byte[] headerType = new byte[MessageFactory.MESSAGE_HEADER_TYPE_LENGTH];
                message.readBytes(MessageFactory.MESSAGE_HEADER_LENGTH_LENGTH);
                message.readBytes(headerType);
                switch (MessageType.forValue(ArraysUtil.bytes2int(headerType))) {
                    case Open:
                        break;
                    case OpenResp:
                        //setMode listener
                        break;
                    case Error:
                        //set mode speaker
                        break;
                }
            }
            ctx.write(LegacyMessageFactory.createError(ErrorCodeNonExtended.NoError, null));
            ctx.close();
        } else {
            ReferenceCountUtil.retain(message);
            ctx.fireChannelRead(message);
        }
    }
}
