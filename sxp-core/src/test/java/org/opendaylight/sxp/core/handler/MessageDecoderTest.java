/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import java.net.SocketAddress;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.behavior.Context;
import org.opendaylight.sxp.util.exception.ErrorMessageReceivedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageConnectionStateException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.Notification;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class, Context.class}) public class MessageDecoderTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static SxpConnection connection;
        private static SxpNode sxpNode;
        private static ChannelInboundHandler decoder;
        private static ChannelHandlerContext channelHandlerContext;
        private static Context context;

        @Before public void init() throws Exception {
                connection = mock(SxpConnection.class);
                context = PowerMockito.mock(Context.class);
                when(connection.getContext()).thenReturn(context);
                sxpNode = PowerMockito.mock(SxpNode.class);
                PowerMockito.when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(connection);
                decoder = MessageDecoder.createClientProfile(sxpNode);
                channelHandlerContext = mock(ChannelHandlerContext.class);
                Channel channel = mock(Channel.class);
                when(channel.localAddress()).thenReturn(mock(SocketAddress.class));
                when(channel.remoteAddress()).thenReturn(mock(SocketAddress.class));
                when(channelHandlerContext.channel()).thenReturn(channel);
        }

        @Test public void testChannelActive() throws Exception {
                when(connection.isStateOn()).thenReturn(false);
                when(connection.isModeBoth()).thenReturn(false);
                decoder.channelActive(channelHandlerContext);
                verify(connection).addChannelHandlerContext(any(ChannelHandlerContext.class));
                verify(context).executeChannelActivationStrategy(any(ChannelHandlerContext.class),
                        any(SxpConnection.class));

                when(connection.isModeBoth()).thenReturn(true);
                when(connection.isStateOn(Matchers.<SxpConnection.ChannelHandlerContextType>any())).thenReturn(true);
                decoder.channelActive(channelHandlerContext);
                verify(connection, times(2)).addChannelHandlerContext(any(ChannelHandlerContext.class));
                verify(context, times(2)).executeChannelActivationStrategy(any(ChannelHandlerContext.class),
                        any(SxpConnection.class));

                when(connection.isStateOn(Matchers.<SxpConnection.ChannelHandlerContextType>any())).thenReturn(false);
                decoder.channelActive(channelHandlerContext);
                verify(connection, times(3)).addChannelHandlerContext(any(ChannelHandlerContext.class));
                verify(context, times(3)).executeChannelActivationStrategy(any(ChannelHandlerContext.class),
                        any(SxpConnection.class));

                decoder = MessageDecoder.createServerProfile(sxpNode);
                decoder.channelActive(channelHandlerContext);
                verify(connection, times(4)).addChannelHandlerContext(any(ChannelHandlerContext.class));
                verify(context, times(3)).executeChannelActivationStrategy(any(ChannelHandlerContext.class),
                        any(SxpConnection.class));

                when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(null);
                decoder.channelActive(channelHandlerContext);
                verify(channelHandlerContext, times(1)).close();

                when(connection.isStateOn()).thenReturn(true);
                decoder.channelActive(channelHandlerContext);
                verify(channelHandlerContext, times(2)).close();
        }

        @Test public void testChannelInactive() throws Exception {
                decoder.channelInactive(channelHandlerContext);
                verify(context).executeChannelInactivationStrategy(any(ChannelHandlerContext.class),
                        any(SxpConnection.class));

                when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(null);
                decoder.channelInactive(channelHandlerContext);
                verify(context).executeChannelInactivationStrategy(any(ChannelHandlerContext.class),
                        any(SxpConnection.class));
        }

        @Test public void testChannelRead0() throws Exception {
                ByteBuf byteBuf = mock(ByteBuf.class);
                when(byteBuf.readableBytes()).thenReturn(1, 0);
                PowerMockito.when(context.executeParseInput(any(ByteBuf.class))).thenReturn(mock(Notification.class));

                decoder.channelRead(channelHandlerContext, byteBuf);
                verify(context).executeParseInput(any(ByteBuf.class));
                verify(context).executeInputMessageStrategy(any(ChannelHandlerContext.class), any(SxpConnection.class),
                        any(Notification.class));
                Exception[]
                        classes =
                        new Exception[] {new ErrorMessageException(ErrorCodeNonExtended.NoError, null),
                                new ErrorMessageReceivedException(""),
                                new UpdateMessageConnectionStateException(ConnectionState.AdministrativelyDown)};
                for (byte i = 0; i < classes.length; i++) {
                        doThrow(classes[i]).when(context)
                                .executeInputMessageStrategy(any(ChannelHandlerContext.class), any(SxpConnection.class),
                                        any(Notification.class));
                        when(byteBuf.readableBytes()).thenReturn(1, 0);
                        decoder.channelRead(channelHandlerContext, byteBuf);
                        verify(connection, times(i + 1)).setStateOff(any(ChannelHandlerContext.class));
                }
        }

        @Test public void testExceptionCaught() throws Exception {
                decoder.exceptionCaught(channelHandlerContext, null);
                verify(context).executeExceptionCaughtStrategy(any(ChannelHandlerContext.class),
                        any(SxpConnection.class));
        }

        @Test public void testSendErrorMessage() throws Exception {
                ChannelHandlerContext context = mock(ChannelHandlerContext.class);
                when(connection.getChannelHandlerContext(
                        SxpConnection.ChannelHandlerContextType.SpeakerContext)).thenReturn(context);
                MessageDecoder.sendErrorMessage(null,
                        new ErrorMessageException(ErrorCodeNonExtended.NoError, new Exception("")), connection);

                verify(context).writeAndFlush(any(ByteBuf.class));
        }

        @Test public void testChannelReadComplete() throws Exception {
                decoder.channelReadComplete(channelHandlerContext);
                verify(channelHandlerContext).flush();
        }
}
