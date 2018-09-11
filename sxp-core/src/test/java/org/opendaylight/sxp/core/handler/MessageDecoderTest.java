/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.handler;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import java.net.SocketAddress;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.behavior.Context;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

public class MessageDecoderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static SxpConnection connection;
    private static SxpNode sxpNode;
    private static ChannelInboundHandler decoder;
    private static ChannelHandlerContext channelHandlerContext;
    private static Context context;

    @Before
    public void init() throws Exception {
        sxpNode = mock(SxpNode.class);
        NodeId nodeId = new NodeId("10.10.10.10");
        when(sxpNode.getNodeId()).thenReturn(nodeId);
        context = new Context(sxpNode, Version.Version4);
        connection = mock(SxpConnection.class);
        when(connection.getOwner()).thenReturn(sxpNode);
        when(connection.getContext()).thenReturn(context);
        when(connection.getOwnerId()).thenReturn(nodeId);
        when(connection.getVersion()).thenReturn(Version.Version4);
        when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(connection);
        decoder = MessageDecoder.createClientProfile(sxpNode);
        channelHandlerContext = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(channel.localAddress()).thenReturn(mock(SocketAddress.class));
        when(channel.remoteAddress()).thenReturn(mock(SocketAddress.class));
        when(channelHandlerContext.channel()).thenReturn(channel);
    }

    @Test
    public void testChannelActive() throws Exception {
        when(connection.isStateOn()).thenReturn(false);
        when(connection.getMode()).thenReturn(ConnectionMode.Speaker);
        decoder.channelActive(channelHandlerContext);
        verify(connection).addChannelHandlerContext(any(ChannelHandlerContext.class));

        when(connection.getMode()).thenReturn(ConnectionMode.Both);
        when(connection.isStateOn(ArgumentMatchers.any())).thenReturn(true);
        decoder.channelActive(channelHandlerContext);
        verify(connection, times(2)).addChannelHandlerContext(any(ChannelHandlerContext.class));

        when(connection.isStateOn(ArgumentMatchers.any())).thenReturn(false);
        decoder.channelActive(channelHandlerContext);
        verify(connection, times(3)).addChannelHandlerContext(any(ChannelHandlerContext.class));

        decoder = MessageDecoder.createServerProfile(sxpNode);
        decoder.channelActive(channelHandlerContext);
        verify(connection, times(4)).addChannelHandlerContext(any(ChannelHandlerContext.class));

        when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(null);
        decoder.channelActive(channelHandlerContext);
        verify(channelHandlerContext, times(1)).close();

        when(connection.isStateOn()).thenReturn(true);
        decoder.channelActive(channelHandlerContext);
        verify(channelHandlerContext, times(2)).close();
    }

    @Test
    public void testChannelReadWithNullConnection() throws Exception {
        when(sxpNode.getConnection(any())).thenReturn(null);
        decoder.channelRead(channelHandlerContext, null);
    }

    @Test
    public void testSendErrorMessage() throws Exception {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        when(connection.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.SPEAKER_CNTXT)).thenReturn(
                context);
        MessageDecoder.sendErrorMessage(null,
                new ErrorMessageException(ErrorCodeNonExtended.NoError, new Exception("")), connection);

        verify(context).writeAndFlush(any(ByteBuf.class));
    }

    @Test
    public void testChannelReadComplete() throws Exception {
        decoder.channelReadComplete(channelHandlerContext);
        verify(channelHandlerContext).flush();
    }
}
