/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionTemplateFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.connection.templates.ConnectionTemplate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.connection.templates.ConnectionTemplateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConnectionDecoderTest {

    private SxpNode sxpNode;
    private ConnectionDecoder decoder;

    @Before public void setUp() {
        sxpNode = mock(SxpNode.class);
        decoder = new ConnectionDecoder(sxpNode);
    }

    private SxpDomain getDomain(SxpConnectionTemplateFields template) {
        SxpDomain domain = mock(SxpDomain.class);
        when(domain.getTemplate(any(InetSocketAddress.class))).thenReturn(template);
        return domain;
    }

    private ChannelHandlerContext getContext() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.5", 65498));
        when(ctx.channel()).thenReturn(channel);
        return ctx;
    }

    private ConnectionTemplate getTemplate(ConnectionMode mode, Version version) {
        ConnectionTemplateBuilder builder = new ConnectionTemplateBuilder();
        builder.setTemplateTcpPort(new PortNumber(64999));
        builder.setTemplatePrefix(new IpPrefix("0.0.0.0/0".toCharArray()));
        builder.setTemplateMode(mode);
        builder.setTemplateVersion(version);
        return builder.build();
    }

    @Test public void testAddConnection() throws Exception {
        decoder.addConnection("domain", mock(Connection.class));
        verify(sxpNode).addConnection(any(Connection.class), anyString());
    }

    @Test public void testChannelActive() throws Exception {
        ChannelHandlerContext ctx = getContext();
        List<SxpDomain> domains = new ArrayList<>();
        when(sxpNode.getDomains()).thenReturn(domains);

        when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(null);
        domains.add(getDomain(mock(SxpConnectionTemplateFields.class)));
        decoder.channelActive(ctx);
        verify(ctx, never()).fireChannelActive();

        domains.clear();
        domains.add(getDomain(null));
        decoder.channelActive(ctx);
        verify(ctx).fireChannelActive();

        when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(mock(SxpConnection.class));
        decoder.channelActive(ctx);
        verify(ctx, times(2)).fireChannelActive();
    }

    private ByteBuf getMessage(MessageType type, Version version, ConnectionMode mode) {
        return Unpooled.copiedBuffer(MessageType.Open.equals(type) ? new byte[] {0, 0, 0, 32, 0, 0, 0, 1, 0, 0, 0,
                (byte) version.getIntValue(), 0, 0, 0, (byte) mode.getIntValue(), 80, 6, 6, 3, 0, 2, 0, 1, 0, 80, 7, 4,
                0, 90, 0, (byte) 180} : new byte[] {0, 0, 0, 28, 0, 0, 0, 2, 0, 0, 0, (byte) version.getIntValue(), 0,
                0, 0, (byte) mode.getIntValue(), 80, 5, 4, 127, 0, 0, 1, 80, 7, 2, 0, 120});
    }

    @Test public void testChannelRead0() throws Exception {
        ChannelHandlerContext ctx = getContext();
        List<SxpDomain> domains = new ArrayList<>();
        when(sxpNode.getDomains()).thenReturn(domains);

        domains.add(getDomain(null));
        decoder.channelRead0(ctx, mock(ByteBuf.class));
        verify(ctx).fireChannelRead(any(ByteBuf.class));

        when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(mock(SxpConnection.class));
        decoder.channelRead0(ctx, mock(ByteBuf.class));
        verify(ctx, times(2)).fireChannelRead(any(ByteBuf.class));
    }

    @Test public void testChannelRead1() throws Exception {
        ChannelHandlerContext ctx = getContext();
        List<SxpDomain> domains = new ArrayList<>();
        when(sxpNode.getDomains()).thenReturn(domains);

        domains.add(getDomain(getTemplate(ConnectionMode.Listener, Version.Version4)));
        decoder.channelRead0(ctx, getMessage(MessageType.OpenResp, Version.Version4, ConnectionMode.Listener));
        verify(sxpNode).addConnection(any(Connection.class), anyString());
        verify(ctx).close();
    }

    @Test public void testChannelRead2() throws Exception {
        ChannelHandlerContext ctx = getContext();
        List<SxpDomain> domains = new ArrayList<>();
        when(sxpNode.getDomains()).thenReturn(domains);

        domains.add(getDomain(getTemplate(ConnectionMode.Listener, null)));
        decoder.channelRead0(ctx, getMessage(MessageType.Open, Version.Version4, ConnectionMode.Listener));
        verify(sxpNode).addConnection(any(Connection.class), anyString());
        verify(ctx).close();
    }

    @Test public void testChannelRead3() throws Exception {
        ChannelHandlerContext ctx = getContext();
        List<SxpDomain> domains = new ArrayList<>();
        when(sxpNode.getDomains()).thenReturn(domains);

        domains.add(getDomain(getTemplate(null, Version.Version4)));
        decoder.channelRead0(ctx, getMessage(MessageType.OpenResp, Version.Version4, ConnectionMode.Listener));
        verify(sxpNode).addConnection(any(Connection.class), anyString());
        verify(ctx).close();
    }

    @Test public void testChannelRead4() throws Exception {
        ChannelHandlerContext ctx = getContext();
        List<SxpDomain> domains = new ArrayList<>();
        when(sxpNode.getDomains()).thenReturn(domains);

        domains.add(getDomain(getTemplate(null, null)));
        decoder.channelRead0(ctx, getMessage(MessageType.Open, Version.Version4, ConnectionMode.Listener));
        verify(sxpNode).addConnection(any(Connection.class), anyString());
        verify(ctx).close();
    }

    @Test public void testChannelRead5() throws Exception {
        ChannelHandlerContext ctx = getContext();
        List<SxpDomain> domains = new ArrayList<>();
        when(sxpNode.getDomains()).thenReturn(domains);

        domains.add(getDomain(getTemplate(ConnectionMode.Listener, Version.Version4)));
        decoder.channelRead0(ctx, getMessage(MessageType.OpenResp, Version.Version3, ConnectionMode.Listener));
        verify(sxpNode, never()).addConnection(any(Connection.class), anyString());
        verify(ctx).close();
    }

    @Test public void testChannelRead6() throws Exception {
        ChannelHandlerContext ctx = getContext();
        List<SxpDomain> domains = new ArrayList<>();
        when(sxpNode.getDomains()).thenReturn(domains);

        domains.add(getDomain(getTemplate(ConnectionMode.Listener, Version.Version4)));
        decoder.channelRead0(ctx, getMessage(MessageType.OpenResp, Version.Version4, ConnectionMode.Speaker));
        verify(sxpNode, never()).addConnection(any(Connection.class), anyString());
        verify(ctx).close();
    }
}
