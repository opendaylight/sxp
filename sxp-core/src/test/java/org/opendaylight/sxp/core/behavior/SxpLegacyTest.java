/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.behavior;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpConnection.ChannelHandlerContextType;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.core.service.BindingHandler;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
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

public class SxpLegacyTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    private SxpLegacy sxpLegacy;
    private ChannelHandlerContext channelHandlerContext;
    private SxpConnection connection;
    private SxpNode sxpNode;

    @Before
    public void init() throws Exception {
        sxpLegacy = new SxpLegacy(Version.Version4);
        channelHandlerContext = mock(ChannelHandlerContext.class);
        sxpNode = mock(SxpNode.class);
        connection = mock(SxpConnection.class);
        when(connection.getVersion()).thenReturn(Version.Version1);
        when(connection.getMode()).thenReturn(ConnectionMode.None);
        when(connection.getNodeIdRemote()).thenReturn(NodeId.getDefaultInstance("0.0.0.0"));
        when(connection.getDestination()).thenReturn(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 5));
        when(sxpNode.getWorker()).thenReturn(mock(ThreadsWorker.class));
        BindingHandler handler = new BindingHandler(sxpNode, new BindingDispatcher(sxpNode));
        when(sxpNode.getSvcBindingHandler()).thenReturn(handler);
        when(connection.getOwner()).thenReturn(sxpNode);
        Context context = new Context(sxpNode, Version.Version4);
        when(connection.getContext()).thenReturn(context);
    }

    @Test
    public void testOnChannelActivation() throws Exception {
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

    @Test
    public void testOnChannelInactivation() throws Exception {
        when(connection.isStateOn(ArgumentMatchers.any(ChannelHandlerContextType.class))).thenReturn(true);
        when(connection.getContextType(any(ChannelHandlerContext.class))).thenReturn(SxpConnection.ChannelHandlerContextType.LISTENER_CNTXT);

        sxpLegacy.onChannelInactivation(channelHandlerContext, connection);
        verify(connection).setDeleteHoldDownTimer();

        when(connection.getContextType(any(ChannelHandlerContext.class))).thenReturn(SxpConnection.ChannelHandlerContextType.SPEAKER_CNTXT);
        sxpLegacy.onChannelInactivation(channelHandlerContext, connection);
        verify(channelHandlerContext).writeAndFlush(any(ByteBuf.class));
        verify(connection, times(1)).setStateOff(any(ChannelHandlerContext.class));
    }

    @Test
    public void testOnInputMessageOpen() throws Exception {
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
        verify(channelHandlerContext).writeAndFlush(isNotNull());

        when(connection.isModeListener()).thenReturn(false);
        when(connection.isModeSpeaker()).thenReturn(true);
        sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
        verify(connection).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
        verify(connection).setStateOn();
        verify(channelHandlerContext, times(2)).writeAndFlush(isNotNull());

        when(message.getSxpMode()).thenReturn(ConnectionMode.Speaker);
        when(connection.isModeListener()).thenReturn(true);
        when(connection.isModeSpeaker()).thenReturn(false);
        sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
        verify(connection, times(2)).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
        verify(connection, times(2)).setStateOn();
        verify(channelHandlerContext, times(3)).writeAndFlush(isNotNull());

        when(message.getType()).thenReturn(MessageType.OpenResp);
        sxpLegacy.onInputMessage(channelHandlerContext, connection, message);

        when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
        when(connection.isModeListener()).thenReturn(true);
        sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
        verify(connection, times(2)).setStateOff(any(ChannelHandlerContext.class));
        verify(connection, times(3)).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
        verify(connection, times(3)).setStateOn();
        verify(channelHandlerContext, times(4)).writeAndFlush(isNotNull());

        when(connection.getVersion()).thenReturn(Version.Version4);
        exception.expect(ErrorMessageException.class);
        sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
    }

    @Test
    public void testOnInputMessageUpdate() throws Exception {
        UpdateMessageLegacy messageLegacy = mock(UpdateMessageLegacy.class);
        when(messageLegacy.getType()).thenReturn(MessageType.Update);
        when(messageLegacy.getPayload()).thenReturn(new byte[] {});

        when(connection.isStateOn(SxpConnection.ChannelHandlerContextType.LISTENER_CNTXT)).thenReturn(true);
        sxpLegacy.onInputMessage(channelHandlerContext, connection, messageLegacy);
        verify(connection).setUpdateOrKeepaliveMessageTimestamp();
        verify(connection).processUpdateMessage(any(UpdateMessageLegacy.class));

        when(connection.getState()).thenReturn(ConnectionState.Off);
        when(connection.isStateOn(SxpConnection.ChannelHandlerContextType.LISTENER_CNTXT)).thenReturn(false);
        exception.expect(UpdateMessageConnectionStateException.class);
        sxpLegacy.onInputMessage(channelHandlerContext, connection, messageLegacy);
    }

    @Test
    public void testOnInputMessageError() throws Exception {
        ErrorMessage message = mock(ErrorMessage.class);
        when(message.getInformation()).thenReturn("");
        when(message.getType()).thenReturn(MessageType.Error);
        when(message.getPayload()).thenReturn(new byte[] {});

        exception.expect(ErrorMessageReceivedException.class);
        sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
    }

    @Test
    public void testOnInputMessagePurgeAll() throws Exception {
        PurgeAllMessage message = mock(PurgeAllMessage.class);
        when(message.getType()).thenReturn(MessageType.PurgeAll);

        sxpLegacy.onInputMessage(channelHandlerContext, connection, message);
        verify(sxpNode.getWorker()).executeTaskInSequence(any(Callable.class), eq(ThreadsWorker.WorkerType.INBOUND),
                any(SxpConnection.class));
    }

    @Test
    public void testOnException() {
        sxpLegacy.onException(channelHandlerContext, connection);
    }

}
