/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.behavior;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.legacy.LegacyMessageFactory;
import org.opendaylight.sxp.util.exception.ErrorMessageReceivedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageConnectionStateException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.ErrorMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.PurgeAllMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessageLegacy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class, Context.class}) public class SxpLegacyTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static SxpLegacy sxpLegacy;
        private static ChannelHandlerContext channelHandlerContext;
        private static SxpConnection connection;
        private static SxpNode sxpNode;

        @Before public void init() throws UnknownHostException {
                sxpLegacy = new SxpLegacy(PowerMockito.mock(Context.class));
                channelHandlerContext = mock(ChannelHandlerContext.class);
                connection = mock(SxpConnection.class);
                when(connection.getVersion()).thenReturn(Version.Version1);
                when(connection.getMode()).thenReturn(ConnectionMode.None);
                when(connection.getNodeIdRemote()).thenReturn(NodeId.getDefaultInstance("0.0.0.0"));
                when(connection.getDestination()).thenReturn(
                        new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 5));
                PowerMockito.mockStatic(LegacyMessageFactory.class);
                sxpNode = PowerMockito.mock(SxpNode.class);
                Context context = PowerMockito.mock(Context.class);
                PowerMockito.when(context.getOwner()).thenReturn(sxpNode);
                when(connection.getContext()).thenReturn(context);
        }

        @Test public void testOnChannelActivation() throws Exception {
                byte i = 1;
                for (ConnectionMode mode : ConnectionMode.values()) {
                        when(connection.getMode()).thenReturn(mode);

                        sxpLegacy.onChannelActivation(channelHandlerContext, connection);
                        verify(channelHandlerContext, times(i)).writeAndFlush(any(ByteBuf.class));
                        verify(connection, times(i++)).setStatePendingOn();
                }

                when(connection.isStateDeleteHoldDown()).thenReturn(true);
                sxpLegacy.onChannelActivation(channelHandlerContext, connection);
                verify(channelHandlerContext, times(i)).writeAndFlush(any(ByteBuf.class));
                verify(connection, times(i++)).setStatePendingOn();
                verify(connection).setReconciliationTimer();
        }

        @Test public void testOnChannelInactivation() throws Exception {
                when(connection.isStateOn(Matchers.<SxpConnection.ChannelHandlerContextType>any())).thenReturn(true);
                when(connection.getContextType(any(ChannelHandlerContext.class))).thenReturn(
                        SxpConnection.ChannelHandlerContextType.ListenerContext);

                when(connection.isPurgeAllMessageReceived()).thenReturn(false);
                sxpLegacy.onChannelInactivation(channelHandlerContext, connection);
                verify(connection).setDeleteHoldDownTimer();

                when(connection.isPurgeAllMessageReceived()).thenReturn(true);
                sxpLegacy.onChannelInactivation(channelHandlerContext, connection);
                verify(connection).setStateOff(any(ChannelHandlerContext.class));

                when(connection.getContextType(any(ChannelHandlerContext.class))).thenReturn(
                        SxpConnection.ChannelHandlerContextType.SpeakerContext);
                sxpLegacy.onChannelInactivation(channelHandlerContext, connection);
                verify(channelHandlerContext).writeAndFlush(any(ByteBuf.class));
                verify(connection, times(2)).setStateOff(any(ChannelHandlerContext.class));

        }

        @Test public void testOnInputMessageOpen() throws Exception {
                OpenMessageLegacy message = mock(OpenMessageLegacy.class);
                when(message.getVersion()).thenReturn(Version.Version1);
                when(message.getType()).thenReturn(MessageType.Open);
                when(message.getPayload()).thenReturn(new byte[] {});

                when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
                when(connection.isModeListener()).thenReturn(true);
                sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).setStateOff(any(ChannelHandlerContext.class));
                verify(connection, times(0)).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
                verify(connection, times(0)).setStateOn();
                verify(channelHandlerContext).writeAndFlush(any(getClass()));

                when(connection.isModeListener()).thenReturn(false);
                when(connection.isModeSpeaker()).thenReturn(true);
                sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
                verify(connection, times(2)).setMode(ConnectionMode.Speaker);
                verify(connection).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
                verify(connection).setStateOn();
                verify(channelHandlerContext, times(2)).writeAndFlush(any(getClass()));

                when(message.getSxpMode()).thenReturn(ConnectionMode.Speaker);
                when(connection.isModeListener()).thenReturn(true);
                when(connection.isModeSpeaker()).thenReturn(false);
                sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
                verify(connection, times(1)).setMode(ConnectionMode.Listener);
                verify(connection, times(2)).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
                verify(connection, times(2)).setStateOn();
                verify(channelHandlerContext, times(3)).writeAndFlush(any(getClass()));

                when(message.getType()).thenReturn(MessageType.OpenResp);
                sxpLegacy.onInputMessage(channelHandlerContext, connection, message);

                when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
                when(connection.isModeListener()).thenReturn(true);
                sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
                verify(connection, times(2)).setStateOff(any(ChannelHandlerContext.class));
                verify(connection, times(3)).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
                verify(connection, times(3)).setStateOn();
                verify(channelHandlerContext, times(4)).writeAndFlush(any(getClass()));

                when(connection.getVersion()).thenReturn(Version.Version4);
                exception.expect(ErrorMessageException.class);
                sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
        }

        @Test public void testOnInputMessageUpdate() throws Exception {
                UpdateMessageLegacy messageLegacy = mock(UpdateMessageLegacy.class);
                when(messageLegacy.getType()).thenReturn(MessageType.Update);
                when(messageLegacy.getPayload()).thenReturn(new byte[] {});

                when(connection.isStateOn(SxpConnection.ChannelHandlerContextType.ListenerContext)).thenReturn(true);
                sxpLegacy.onInputMessage(channelHandlerContext, connection, messageLegacy);
                verify(connection).setUpdateOrKeepaliveMessageTimestamp();
                verify(sxpNode).processUpdateMessage(any(UpdateMessageLegacy.class), any(SxpConnection.class));

                when(connection.getState()).thenReturn(ConnectionState.Off);
                when(connection.isStateOn(SxpConnection.ChannelHandlerContextType.ListenerContext)).thenReturn(false);
                exception.expect(UpdateMessageConnectionStateException.class);
                sxpLegacy.onInputMessage(channelHandlerContext, connection, messageLegacy);
        }

        @Test public void testOnInputMessageError() throws Exception {
                ErrorMessage message = mock(ErrorMessage.class);
                when(message.getInformation()).thenReturn("");
                when(message.getType()).thenReturn(MessageType.Error);
                when(message.getPayload()).thenReturn(new byte[] {});

                exception.expect(ErrorMessageReceivedException.class);
                sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
        }

        @Test public void testOnInputMessagePurgeAll() throws Exception {
                PurgeAllMessage message = mock(PurgeAllMessage.class);
                when(message.getType()).thenReturn(MessageType.PurgeAll);

                sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).setPurgeAllMessageReceived();
                verify(sxpNode).purgeBindings(any(NodeId.class));
                verify(sxpNode).setSvcBindingManagerNotify();
        }

}
