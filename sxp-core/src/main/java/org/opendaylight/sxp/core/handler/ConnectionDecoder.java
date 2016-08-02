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
import java.net.UnknownHostException;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.util.exception.ErrorCodeDataLengthException;
import org.opendaylight.sxp.util.exception.connection.SocketAddressNotRecognizedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.attribute.AddressLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeVariantException;
import org.opendaylight.sxp.util.exception.message.attribute.TlvNotFoundException;
import org.opendaylight.sxp.util.exception.unknown.UnknownNodeIdException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpMessageTypeException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionTemplateFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.Notification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessageLegacy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.sxp.core.SxpConnection.invertMode;

/**
 * Handles generation of new SxpConnections based on templates.
 */
@Sharable public class ConnectionDecoder extends SimpleChannelInboundHandler<ByteBuf> {

    protected static final Logger LOG = LoggerFactory.getLogger(ConnectionDecoder.class.getName());
    protected SxpNode owner;

    /**
     * Constructor that sets predefined values
     *
     * @param owner SxpNode associated with decoder
     */
    public ConnectionDecoder(SxpNode owner) {
        this.owner = owner;
    }

    /**
     * @param domain     Domain name
     * @param connection Connection that will be added
     */
    protected void addConnection(String domain, Connection connection) {
        owner.addConnection(connection, domain);
    }

    /**
     * @param address Address to look for
     * @return SxpDomain containing template with provided address
     */
    private SxpDomain getTemplateDomain(InetSocketAddress address) {
        for (SxpDomain sxpDomain : owner.getDomains()) {
            if (sxpDomain.getTemplate(address) != null) {
                return sxpDomain;
            }
        }
        return null;
    }

    /**
     * @param ctx Context containing remote address
     * @return Address of remote peer
     */
    private InetSocketAddress getAddress(ChannelHandlerContext ctx) {
        return ctx.channel().remoteAddress() instanceof InetSocketAddress ? (InetSocketAddress) ctx.channel()
                .remoteAddress() : null;
    }

    @Override public void channelActive(ChannelHandlerContext ctx) throws SocketAddressNotRecognizedException {
        InetSocketAddress address = getAddress(ctx);
        final SxpConnection connection = owner.getConnection(address);
        final SxpDomain domain = getTemplateDomain(address);
        if (connection != null || domain == null) {
            ctx.fireChannelActive();
        }
    }

    @Override public void channelRead0(ChannelHandlerContext ctx, ByteBuf message)
            throws ErrorCodeDataLengthException, AttributeLengthException, TlvNotFoundException, AddressLengthException,
            UnknownNodeIdException, ErrorMessageException, UnknownSxpMessageTypeException, AttributeVariantException,
            UnknownHostException, UnknownPrefixException {
        InetSocketAddress address = getAddress(ctx);
        final SxpConnection connection = owner.getConnection(address);
        final SxpDomain domain = getTemplateDomain(address);

        if (address != null && connection == null && domain != null) {
            final SxpConnectionTemplateFields template = domain.getTemplate(address);
            Notification notification = MessageFactory.parse(null, message);
            Version version = getVersion(notification);
            ConnectionMode mode = getMode(notification);
            if ((template.getTemplateMode() == null || template.getTemplateMode().equals(mode)) && (
                    template.getTemplateVersion() == null || template.getTemplateVersion().equals(version))) {
                LOG.info("{} Adding new SxpConnection from template {}", owner, template);
                addConnection(domain.getName(), new ConnectionBuilder().setMode(invertMode(mode))
                        .setTcpPort(template.getTemplateTcpPort())
                        .setPeerAddress(new IpAddress(address.getAddress().getHostAddress().toCharArray()))
                        .setPassword(template.getTemplatePassword())
                        .setDescription("AutoGenerated connection.")
                        .setState(ConnectionState.Off)
                        .setConnectionTimers(new ConnectionTimersBuilder().build())
                        .setVersion(version)
                        .setCapabilities(Configuration.getCapabilities(version))
                        .build());
            }
            ctx.close();
        } else {
            ReferenceCountUtil.retain(message);
            ctx.fireChannelRead(message);
        }
    }

    /**
     * @param notification Notification to be parsed
     * @return Mode of remotePeer or null if message is of unsupported type
     */
    private ConnectionMode getMode(Notification notification) {
        if (notification instanceof OpenMessage)
            return ((OpenMessage) notification).getSxpMode();
        else if (notification instanceof OpenMessageLegacy)
            return ((OpenMessageLegacy) notification).getSxpMode();
        throw new IllegalArgumentException("Not supported message received");
    }

    /**
     * @param notification Notification to be parsed
     * @return Version of remotePeer or null if message is of unsupported type
     */
    private Version getVersion(Notification notification) {
        if (notification instanceof OpenMessage)
            return ((OpenMessage) notification).getVersion();
        else if (notification instanceof OpenMessageLegacy)
            return ((OpenMessageLegacy) notification).getVersion();
        throw new IllegalArgumentException("Not supported message received");
    }
}
