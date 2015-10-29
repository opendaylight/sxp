/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.behavior;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.core.messaging.legacy.LegacyMessageFactory;
import org.opendaylight.sxp.util.exception.ErrorMessageReceivedException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageConnectionStateException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.HoldTimeAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.hold.time.attribute.HoldTimeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.ErrorMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.PurgeAllMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessage;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class, Context.class, MessageFactory.class})
public class Sxpv4Test {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static Sxpv4 sxpv4;
        private static ChannelHandlerContext channelHandlerContext;
        private static SxpConnection connection;
        private static SxpNode sxpNode;

        @Before public void init() throws Exception {
                channelHandlerContext = mock(ChannelHandlerContext.class);
                connection = mock(SxpConnection.class);
                when(connection.getVersion()).thenReturn(Version.Version4);
                when(connection.getMode()).thenReturn(ConnectionMode.Speaker);
                PowerMockito.mockStatic(LegacyMessageFactory.class);
                sxpNode = PowerMockito.mock(SxpNode.class);
                Context context = PowerMockito.mock(Context.class);
                sxpv4 = new Sxpv4(context);
                PowerMockito.when(context.getOwner()).thenReturn(sxpNode);
                when(connection.getContext()).thenReturn(context);
                when(connection.getOwner()).thenReturn(sxpNode);
                when(connection.getDestination()).thenReturn(
                        new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0));
                when(connection.getLocalAddress()).thenReturn(
                        new InetSocketAddress(InetAddress.getByName("0.0.0.1"), 0));

                PowerMockito.mockStatic(MessageFactory.class);
        }

        @Test public void testOnChannelActivation() throws Exception {
                when(connection.isModeBoth()).thenReturn(false);
                sxpv4.onChannelActivation(channelHandlerContext, connection);
                verify(channelHandlerContext).writeAndFlush(any(ByteBuf.class));
                verify(connection).setStatePendingOn();

                when(connection.isModeBoth()).thenReturn(true);
                when(connection.isBidirectionalBoth()).thenReturn(false);
                sxpv4.onChannelActivation(channelHandlerContext, connection);
                verify(channelHandlerContext, times(2)).writeAndFlush(any(ByteBuf.class));
                verify(connection, times(2)).setStatePendingOn();
        }

        private List<Attribute> getHoldTime() {
                List<Attribute> attributes = new ArrayList<>();
                HoldTimeAttributeBuilder holdTimeAttributeBuilder = new HoldTimeAttributeBuilder();
                HoldTimeAttributesBuilder holdTimeAttributesBuilder = new HoldTimeAttributesBuilder();
                holdTimeAttributesBuilder.setHoldTimeMaxValue(120);
                holdTimeAttributesBuilder.setHoldTimeMinValue(60);
                holdTimeAttributeBuilder.setHoldTimeAttributes(holdTimeAttributesBuilder.build());
                AttributeBuilder builder = new AttributeBuilder();
                builder.setAttributeOptionalFields(holdTimeAttributeBuilder.build());
                builder.setType(AttributeType.HoldTime);
                attributes.add(builder.build());
                return attributes;
        }

        @Test public void testOnInputMessageOpen() throws Exception {
                OpenMessage message = mock(OpenMessage.class);
                when(message.getVersion()).thenReturn(Version.Version4);
                when(message.getType()).thenReturn(MessageType.Open);
                when(message.getPayload()).thenReturn(new byte[] {});
                when(message.getAttribute()).thenReturn(getHoldTime());

                when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).setConnection(any(OpenMessage.class));
                verify(connection, never()).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
                verify(channelHandlerContext).writeAndFlush(any(getClass()));

                when(message.getSxpMode()).thenReturn(ConnectionMode.Speaker);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection, times(2)).setConnection(any(OpenMessage.class));
                verify(connection, never()).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
                verify(channelHandlerContext, times(2)).writeAndFlush(any(getClass()));

                //DeleteHoldDown/PendingOn/IPDropdown
                when(connection.isStateDeleteHoldDown()).thenReturn(true);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
                verify(connection, times(3)).setConnection(any(OpenMessage.class));
                verify(channelHandlerContext, times(3)).writeAndFlush(any(getClass()));

                when(connection.isStateDeleteHoldDown()).thenReturn(false);
                when(connection.isStatePendingOn()).thenReturn(true);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).closeChannelHandlerContext(any(ChannelHandlerContext.class));
                verify(channelHandlerContext, times(3)).writeAndFlush(any(getClass()));

                when(connection.getDestination()).thenReturn(
                        new InetSocketAddress(InetAddress.getByName("0.0.0.10"), 0));
                when(connection.isStateDeleteHoldDown()).thenReturn(false);
                when(connection.isStatePendingOn()).thenReturn(false);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).closeChannelHandlerContext(any(ChannelHandlerContext.class));
                verify(connection, times(4)).setConnection(any(OpenMessage.class));
                verify(channelHandlerContext, times(4)).writeAndFlush(any(getClass()));

                //BOTH mode
                when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
                when(connection.isModeBoth()).thenReturn(true);
                when(connection.isBidirectionalBoth()).thenReturn(false);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).setConnectionSpeakerPart(any(OpenMessage.class));
                verify(connection).markChannelHandlerContext(any(ChannelHandlerContext.class),
                        any(SxpConnection.ChannelHandlerContextType.class));
                verify(channelHandlerContext, times(5)).writeAndFlush(any(getClass()));

                when(message.getSxpMode()).thenReturn(ConnectionMode.Speaker);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(channelHandlerContext, times(6)).writeAndFlush(any(getClass()));
                verify(connection).setStateOff(any(ChannelHandlerContext.class));

                when(connection.isBidirectionalBoth()).thenReturn(true);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection, times(5)).setConnection(any(OpenMessage.class));

        }

        @Test public void testOnInputMessageOpenResp() throws Exception {
                OpenMessage message = mock(OpenMessage.class);
                when(message.getVersion()).thenReturn(Version.Version4);
                when(message.getType()).thenReturn(MessageType.OpenResp);
                when(message.getPayload()).thenReturn(new byte[] {});
                when(message.getAttribute()).thenReturn(getHoldTime());

                when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).setConnection(any(OpenMessage.class));

                when(message.getSxpMode()).thenReturn(ConnectionMode.Speaker);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection, times(2)).setConnection(any(OpenMessage.class));

                //DeleteHoldDown/On
                when(connection.isStateDeleteHoldDown()).thenReturn(true);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
                verify(connection, times(3)).setConnection(any(OpenMessage.class));

                when(connection.isStateDeleteHoldDown()).thenReturn(false);
                when(connection.isStateOn()).thenReturn(true);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).closeChannelHandlerContext(any(ChannelHandlerContext.class));

                //BOTH mode
                when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
                when(connection.isModeBoth()).thenReturn(true);
                when(connection.isBidirectionalBoth()).thenReturn(false);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).setConnectionSpeakerPart(any(OpenMessage.class));
                verify(connection).markChannelHandlerContext(any(ChannelHandlerContext.class),
                        any(SxpConnection.ChannelHandlerContextType.class));
                verify(connection, times(3)).setConnection(any(OpenMessage.class));

                when(message.getSxpMode()).thenReturn(ConnectionMode.Speaker);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).setConnectionListenerPart(any(OpenMessage.class));
                verify(connection, times(2)).markChannelHandlerContext(any(ChannelHandlerContext.class),
                        any(SxpConnection.ChannelHandlerContextType.class));
                verify(connection, times(3)).setConnection(any(OpenMessage.class));

                when(connection.isBidirectionalBoth()).thenReturn(true);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection, times(4)).setConnection(any(OpenMessage.class));
        }

        @Test public void testOnInputMessageUpdate() throws Exception {
                UpdateMessage message = mock(UpdateMessage.class);
                when(message.getType()).thenReturn(MessageType.Update);
                when(message.getPayload()).thenReturn(new byte[] {});

                when(connection.isStateOn(SxpConnection.ChannelHandlerContextType.ListenerContext)).thenReturn(true);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).setUpdateOrKeepaliveMessageTimestamp();
                verify(sxpNode).processUpdateMessage(any(UpdateMessage.class), any(SxpConnection.class));

                when(connection.getState()).thenReturn(ConnectionState.Off);
                when(connection.isStateOn(SxpConnection.ChannelHandlerContextType.ListenerContext)).thenReturn(false);
                exception.expect(UpdateMessageConnectionStateException.class);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
        }

        @Test public void testOnInputMessageError() throws Exception {
                ErrorMessage message = mock(ErrorMessage.class);
                when(message.getInformation()).thenReturn("");
                when(message.getType()).thenReturn(MessageType.Error);
                when(message.getPayload()).thenReturn(new byte[] {});

                exception.expect(ErrorMessageReceivedException.class);
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
        }

        @Test public void testOnInputMessagePurgeAll() throws Exception {
                PurgeAllMessage message = mock(PurgeAllMessage.class);
                when(message.getType()).thenReturn(MessageType.PurgeAll);

                when(connection.getDestination()).thenReturn(
                        new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 5));
                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).setPurgeAllMessageReceived();
                verify(sxpNode).purgeBindings(any(NodeId.class));
                verify(sxpNode).notifyService();
        }

        @Test public void testOnInputMessageKeepAlive() throws Exception {
                KeepaliveMessage message = mock(KeepaliveMessage.class);
                when(message.getType()).thenReturn(MessageType.Error);

                sxpv4.onInputMessage(channelHandlerContext, connection, message);
                verify(connection).setUpdateOrKeepaliveMessageTimestamp();
        }
}
