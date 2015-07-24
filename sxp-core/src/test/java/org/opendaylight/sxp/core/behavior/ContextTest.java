/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.behavior;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.OpenMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.SocketAddress;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class, StrategyFactory.class}) public class ContextTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static SxpConnection connection;
        private static SxpNode sxpNode;
        private static ChannelHandlerContext channelHandlerContext;
        private static Context context;
        private static Strategy strategy;

        @Before public void init() throws Exception {
                connection = mock(SxpConnection.class);
                sxpNode = PowerMockito.mock(SxpNode.class);
                when(connection.getOwner()).thenReturn(sxpNode);
                PowerMockito.when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(connection);
                channelHandlerContext = mock(ChannelHandlerContext.class);
                Channel channel = mock(Channel.class);
                when(channel.localAddress()).thenReturn(mock(SocketAddress.class));
                when(channel.remoteAddress()).thenReturn(mock(SocketAddress.class));
                when(channelHandlerContext.channel()).thenReturn(channel);
                strategy = mock(Strategy.class);
                PowerMockito.mockStatic(StrategyFactory.class);
                PowerMockito.when(StrategyFactory.getStrategy(any(Context.class), any(Version.class)))
                        .thenReturn(strategy);
        }

        @Test public void testExecuteInputMessageStrategy() throws Exception {
                context = new Context(sxpNode, Version.Version3);
                Context context_ = new Context(sxpNode, Version.Version4);
                when(connection.getContext()).thenReturn(context_);

                OpenMessage message = mock(OpenMessage.class);
                when(message.getVersion()).thenReturn(Version.Version4);
                when(message.getType()).thenReturn(MessageType.Open);
                when(connection.getVersion()).thenReturn(Version.Version3);

                exception.expect(IllegalStateException.class);
                context.executeInputMessageStrategy(channelHandlerContext, connection, message);

                when(message.getType()).thenReturn(MessageType.OpenResp);
                exception.expect(IllegalStateException.class);
                context.executeInputMessageStrategy(channelHandlerContext, connection, message);
        }

        @Test public void testExecuteInputMessageStrategyLegacy() throws Exception {
                context = new Context(sxpNode, Version.Version4);
                Context context_ = new Context(sxpNode, Version.Version1);
                when(connection.getContext()).thenReturn(context_);

                OpenMessageLegacy message = mock(OpenMessageLegacy.class);
                when(message.getVersion()).thenReturn(Version.Version1);
                when(message.getType()).thenReturn(MessageType.Open);
                when(connection.getVersion()).thenReturn(Version.Version1);

                context.executeInputMessageStrategy(channelHandlerContext, connection, message);
                verify(channelHandlerContext).writeAndFlush(any(ByteBuf.class));
                verify(sxpNode).openConnection(any(SxpConnection.class));

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
}
