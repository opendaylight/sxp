/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.behavior;

import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.sxp.core.Constants.MESSAGE_HEADER_LENGTH_LENGTH;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.net.SocketAddress;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.Notification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessageLegacy;

public class ContextTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    private static SxpConnection connection;
    private static SxpNode sxpNode;
    private static ChannelHandlerContext channelHandlerContext;
    private static Context context;
    private static Strategy strategy;

    @Before
    public void init() throws Exception {
        connection = mock(SxpConnection.class);
        sxpNode = mock(SxpNode.class);
        when(connection.getOwner()).thenReturn(sxpNode);
        when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(connection);
        channelHandlerContext = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(channel.localAddress()).thenReturn(mock(SocketAddress.class));
        when(channel.remoteAddress()).thenReturn(mock(SocketAddress.class));
        when(channelHandlerContext.channel()).thenReturn(channel);
        strategy = mock(Strategy.class);
    }

    @Test
    public void testExecuteInputMessageStrategy() throws Exception {
        context = new Context(sxpNode, Version.Version3);
        Context context_ = new Context(sxpNode, Version.Version4);
        when(connection.getContext()).thenReturn(context_);

        OpenMessage message = mock(OpenMessage.class);
        when(message.getVersion()).thenReturn(Version.Version4);
        when(message.getType()).thenReturn(MessageType.Open);
        when(connection.getVersion()).thenReturn(Version.Version3);

        context.executeInputMessageStrategy(channelHandlerContext, connection, message);
        verify(strategy).onInputMessage(any(ChannelHandlerContext.class), any(SxpConnection.class),
                any(OpenMessageLegacy.class));

        when(message.getType()).thenReturn(MessageType.OpenResp);
        exception.expect(IllegalStateException.class);
        context.executeInputMessageStrategy(channelHandlerContext, connection, message);
    }

    @Test
    public void testExecuteInputMessageStrategyLegacy() throws Exception {
        context = new Context(sxpNode, Version.Version4);
        Context context_ = new Context(sxpNode, Version.Version1);
        when(connection.getContext()).thenReturn(context_);

        OpenMessageLegacy message = mock(OpenMessageLegacy.class);
        when(message.getVersion()).thenReturn(Version.Version1);
        when(message.getType()).thenReturn(MessageType.Open);
        when(connection.getVersion()).thenReturn(Version.Version1);

        context.executeInputMessageStrategy(channelHandlerContext, connection, message);
        verify(channelHandlerContext).writeAndFlush(any(ByteBuf.class));
        verify(connection).setStateOff(channelHandlerContext);

        when(connection.getVersion()).thenReturn(Version.Version4, Version.Version1);
        context.executeInputMessageStrategy(channelHandlerContext, connection, message);
        verify(connection).setBehaviorContexts(Version.Version1);

        OpenMessage message1 = mock(OpenMessage.class);
        when(message1.getVersion()).thenReturn(Version.Version4);
        when(message1.getType()).thenReturn(MessageType.Open);
        when(connection.getVersion()).thenReturn(Version.Version2);
        context.executeInputMessageStrategy(channelHandlerContext, connection, message1);
        verify(strategy).onInputMessage(channelHandlerContext, connection, message1);

        when(message.getType()).thenReturn(MessageType.OpenResp);
        context.executeInputMessageStrategy(channelHandlerContext, connection, message);
        verify(connection, times(2)).setBehaviorContexts(Version.Version1);
    }

    @Test
    public void testExecuteChannelActivationStrategy() {
        Context ctxt = new Context(sxpNode, Version.Version4);
        ctxt.executeChannelActivationStrategy(channelHandlerContext, connection);
    }

    @Test
    public void testExecuteChannelInactivationStrategy() {
        Context ctxt = new Context(sxpNode, Version.Version4);
        ctxt.executeChannelInactivationStrategy(channelHandlerContext, connection);
    }

    @Test
    public void testExecuteExceptionCaughtStrategy() {
        Context ctxt = new Context(sxpNode, Version.Version4);
        ctxt.executeExceptionCaughtStrategy(channelHandlerContext, connection);
    }

    @Test
    public void testExecuteParseInput() throws Exception {
        Context ctxt = new Context(sxpNode, Version.Version4);
        Notification notificationMock = mock(Notification.class);
        when(strategy.onParseInput(any())).thenReturn(notificationMock);
        ByteBuf mock = mock(ByteBuf.class);
        when(mock.readBytes(new byte[MESSAGE_HEADER_LENGTH_LENGTH])).thenReturn(mock);
        Assert.assertNotNull(ctxt.executeParseInput(mock));
        // Assert.assertNotNull(ctxt.executeParseInput(Unpooled.copiedBuffer(new byte[] {1, 0, 1, 0, 1, 0, 1, 0})));
    }

    @Test
    public void testGetVersion() {
        Context ctxt = new Context(sxpNode, Version.Version4);
        Assert.assertEquals(Version.Version4, ctxt.getVersion());
    }

    @Test
    public void testExecuteUpdateMessageStrategy() throws Exception {
        Context ctxt = new Context(sxpNode, Version.Version4);
        ByteBuf bbufMock = mock(ByteBuf.class);
        when(strategy.onUpdateMessage(any(), any(), any(), any())).thenReturn(bbufMock);
        Assert.assertNotNull(ctxt.executeUpdateMessageStrategy(connection, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null));
    }
}
