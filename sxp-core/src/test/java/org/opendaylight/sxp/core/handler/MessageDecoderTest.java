/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.handler;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpConnection.ChannelHandlerContextType;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.behavior.Context;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorSubCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessage;

public class MessageDecoderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private SxpConnection connection;
    private SxpNode sxpNode;
    private ChannelInboundHandler decoder;
    private ChannelHandlerContext channelHandlerContext;

    @Before
    public void init() throws Exception {
        sxpNode = mock(SxpNode.class);
        NodeId nodeId = new NodeId("10.10.10.10");
        when(sxpNode.getNodeId()).thenReturn(nodeId);
        Context context = new Context(sxpNode, Version.Version4);
        connection = mock(SxpConnection.class);
        when(connection.getOwner()).thenReturn(sxpNode);
        when(connection.getContext()).thenReturn(context);
        when(connection.getOwnerId()).thenReturn(nodeId);
        when(connection.getVersion()).thenReturn(Version.Version4);
        when(connection.getLocalAddress()).thenReturn(new InetSocketAddress("20.20.20.20", 0));
        when(connection.getDestination()).thenReturn(new InetSocketAddress("10.10.10.10", 0));
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
        when(connection.isStateOn(ArgumentMatchers.any(ChannelHandlerContextType.class))).thenReturn(true);
        decoder.channelActive(channelHandlerContext);
        verify(connection, times(2)).addChannelHandlerContext(any(ChannelHandlerContext.class));

        when(connection.isStateOn(ArgumentMatchers.any(ChannelHandlerContextType.class))).thenReturn(false);
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
    public void testChannelInactive() throws Exception {
        decoder.channelInactive(channelHandlerContext);
        verify(connection).setStateOff(eq(channelHandlerContext));

        when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(null);
        decoder.channelInactive(channelHandlerContext);
        verify(connection).setStateOff(eq(channelHandlerContext));
    }

    @Test
    public void testChannelRead0() throws Exception {
        when(connection.getMode()).thenReturn(ConnectionMode.Speaker);

        ByteBuf byteBuf = MessageFactory.createOpen(Version.Version4, ConnectionMode.Speaker, sxpNode.getNodeId(), 0);
        decoder.channelRead(channelHandlerContext, byteBuf);

        // connection is set up
        verify(connection).setConnection(any(OpenMessage.class));
    }

    @Test
    public void testChannelRead0ErrMsg() throws Exception {
        when(connection.getMode()).thenReturn(ConnectionMode.Speaker);

        ByteBuf byteBuf = Unpooled.copyInt(4, 0, 0);
        decoder.channelRead(channelHandlerContext, byteBuf);

        // error to decode msg - connection is shut down
        verify(connection).setStateOff(eq(channelHandlerContext));
    }

    @Test
    public void testChannelRead0ErrMsgReceived() throws Exception {
        when(connection.getMode()).thenReturn(ConnectionMode.Speaker);

        ByteBuf byteBuf = MessageFactory
                .createError(ErrorCode.MessageHeaderError, ErrorSubCode.MalformedAttributeList, new byte[] {});
        decoder.channelRead(channelHandlerContext, byteBuf);

        // error msg received - connection is shut down
        verify(connection).setStateOff(eq(channelHandlerContext));
    }

    @Test
    public void testChannelRead0UpdateMsgConnectionState() throws Exception {
        when(connection.getMode()).thenReturn(ConnectionMode.Speaker);
        when(connection.getState()).thenReturn(ConnectionState.Off);

        ByteBuf byteBuf = MessageFactory
                .createUpdate(Collections.emptyList(), Collections.emptyList(), sxpNode.getNodeId(),
                        Collections.emptyList(), null);
        decoder.channelRead(channelHandlerContext, byteBuf);

        // update msg received when connection is not on - connection is shut down
        verify(connection).setStateOff(eq(channelHandlerContext));
    }

    @Test
    public void testChannelReadWithNullConnection() throws Exception {
        when(sxpNode.getConnection(any())).thenReturn(null);
        decoder.channelRead(channelHandlerContext, null);
    }

    @Test
    public void testIOExceptionCaught() throws Exception {
        decoder.exceptionCaught(channelHandlerContext, new IOException("error"));
        verify(connection).setStateOff(eq(channelHandlerContext));
    }

    @Test
    public void testDecoderExceptionCaught() throws Exception {
        decoder.exceptionCaught(channelHandlerContext, new DecoderException("error"));
        verify(connection).setStateOff(eq(channelHandlerContext));
    }

    @Test
    public void testNullExceptionCaught() throws Exception {
        decoder.exceptionCaught(channelHandlerContext, null);
        verify(connection, never()).setStateOff(any());
    }

    @Test
    public void testSendErrorMessage() throws Exception {
        when(connection.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.SPEAKER_CNTXT))
                .thenReturn(channelHandlerContext);
        MessageDecoder.sendErrorMessage(null,
                new ErrorMessageException(ErrorCodeNonExtended.NoError, new Exception()), connection);
        verify(channelHandlerContext).close();
    }

    @Test
    public void testSendErrorMessageExceptionHandling() throws Exception {
        when(connection.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.SPEAKER_CNTXT))
                .thenThrow(ChannelHandlerContextNotFoundException.class);
        MessageDecoder.sendErrorMessage(null,
                new ErrorMessageException(ErrorCodeNonExtended.NoError, new Exception()), connection);
        verify(channelHandlerContext, never()).close();
    }

    @Test
    public void testChannelReadComplete() throws Exception {
        decoder.channelReadComplete(channelHandlerContext);
        verify(channelHandlerContext).flush();
    }
}
