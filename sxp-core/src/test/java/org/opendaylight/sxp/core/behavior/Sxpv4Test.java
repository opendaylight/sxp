/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.legacy.LegacyMessageFactory;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.core.service.BindingHandler;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.exception.ErrorMessageReceivedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageCompositionException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageConnectionStateException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.CapabilitiesAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.CapabilitiesAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.HoldTimeAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.capabilities.attribute.CapabilitiesAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.capabilities.attribute.CapabilitiesAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.capabilities.attribute.capabilities.attributes.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.hold.time.attribute.HoldTimeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.ErrorMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.PurgeAllMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessage;

public class Sxpv4Test {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Sxpv4 sxpv4;
    private ChannelHandlerContext channelHandlerContext;
    private SxpConnection connection;
    private SxpNode sxpNode;

    @Before
    public void init() throws Exception {
        channelHandlerContext = mock(ChannelHandlerContext.class);
        connection = mock(SxpConnection.class);
        when(connection.getVersion()).thenReturn(Version.Version4);
        when(connection.getMode()).thenReturn(ConnectionMode.Speaker);
        mock(LegacyMessageFactory.class);
        sxpNode = mock(SxpNode.class);
        when(sxpNode.getWorker()).thenReturn(new ThreadsWorker());
        BindingHandler handler = new BindingHandler(sxpNode, new BindingDispatcher(sxpNode));
        when(sxpNode.getSvcBindingHandler()).thenReturn(handler);
        Context context = new Context(sxpNode, Version.Version4);
        sxpv4 = new Sxpv4(sxpNode);
        when(connection.getContext()).thenReturn(context);
        when(connection.getOwnerId()).thenReturn(new NodeId("0.0.0.0"));
        when(connection.getOwner()).thenReturn(sxpNode);
        when(connection.getDestination()).thenReturn(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0));
        when(connection.getLocalAddress()).thenReturn(new InetSocketAddress(InetAddress.getByName("0.0.0.1"), 0));
        when(sxpNode.getWorker()).thenReturn(mock(ThreadsWorker.class));
    }

    @Test
    public void testOnChannelActivation() throws Exception {
        when(connection.isModeBoth()).thenReturn(false);
        sxpv4.onChannelActivation(channelHandlerContext, connection);
        verify(channelHandlerContext).writeAndFlush(any(ByteBuf.class));
        verify(connection).setStatePendingOn();

        when(connection.isModeBoth()).thenReturn(true);
        when(connection.isBidirectionalBoth()).thenReturn(false);
        sxpv4.onChannelActivation(channelHandlerContext, connection);
        verify(channelHandlerContext, times(2)).writeAndFlush(any(ByteBuf.class));
        verify(connection, times(2)).setStatePendingOn();

        when(connection.getMode()).thenReturn(ConnectionMode.None);
        when(connection.isBidirectionalBoth()).thenReturn(true);
        sxpv4.onChannelActivation(channelHandlerContext, connection);
        verify(channelHandlerContext, times(3)).writeAndFlush(any(ByteBuf.class));
        verify(connection, times(3)).setStatePendingOn();

        when(connection.isStateDeleteHoldDown()).thenReturn(true);
        when(connection.isStateOn()).thenReturn(true);
        sxpv4.onChannelActivation(channelHandlerContext, connection);
        verify(channelHandlerContext, times(4)).writeAndFlush(any(ByteBuf.class));
        verify(connection, times(3)).setStatePendingOn();
        verify(connection).setReconciliationTimer();
    }

    private List<Attribute> getHoldTime() {
        HoldTimeAttributeBuilder holdTimeAttributeBuilder = new HoldTimeAttributeBuilder();
        HoldTimeAttributesBuilder holdTimeAttributesBuilder = new HoldTimeAttributesBuilder();
        holdTimeAttributesBuilder.setHoldTimeMaxValue(120);
        holdTimeAttributesBuilder.setHoldTimeMinValue(60);
        holdTimeAttributeBuilder.setHoldTimeAttributes(holdTimeAttributesBuilder.build());
        AttributeBuilder builder = new AttributeBuilder();
        builder.setAttributeOptionalFields(holdTimeAttributeBuilder.build());
        builder.setType(AttributeType.HoldTime);
        return Collections.singletonList(builder.build());
    }

    private List<Attribute> getCapabilities() {
        CapabilitiesAttributes attributes = new CapabilitiesAttributesBuilder()
                .setCapabilities(Collections.singletonList(new CapabilitiesBuilder()
                        .setCode(CapabilityType.None)
                        .build()))
                .build();
        CapabilitiesAttribute attribute = new CapabilitiesAttributeBuilder()
                .setCapabilitiesAttributes(attributes)
                .build();
        AttributeBuilder builder = new AttributeBuilder();
        builder.setAttributeOptionalFields(attribute);
        builder.setType(AttributeType.Capabilities);
        return Collections.singletonList(builder.build());
    }

    @Test
    public void testOnInputMessageOpenListenerConnection() throws Exception {
        OpenMessage message = mock(OpenMessage.class);
        when(message.getVersion()).thenReturn(Version.Version4);
        when(message.getType()).thenReturn(MessageType.Open);
        when(message.getPayload()).thenReturn(new byte[]{});
        when(message.getAttribute()).thenReturn(getHoldTime());
        when(connection.getMode()).thenReturn(ConnectionMode.Listener);

        when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
        verify(connection).setConnection(any(OpenMessage.class));
        verify(connection, never()).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
        verify(channelHandlerContext).writeAndFlush(isNotNull());

        when(message.getSxpMode()).thenReturn(ConnectionMode.Speaker);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
        verify(connection, times(2)).setConnection(any(OpenMessage.class));
        verify(connection, never()).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
        verify(channelHandlerContext, times(2)).writeAndFlush(isNotNull());
    }

    @Test
    public void testOnInputMessageOpenBothModeConnectionErrHandling() throws Exception {
        OpenMessage message = mock(OpenMessage.class);
        when(message.getVersion()).thenReturn(Version.Version4);
        when(message.getType()).thenReturn(MessageType.Open);
        when(message.getPayload()).thenReturn(new byte[]{});
        when(message.getAttribute()).thenReturn(getHoldTime());
        when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
        when(connection.getMode()).thenReturn(ConnectionMode.Both);
        when(connection.isModeBoth()).thenReturn(true);

        sxpv4.onInputMessage(channelHandlerContext, connection, message);

        verify(connection, never()).setConnection(eq(message));
        verify(connection, never()).setStateOff(channelHandlerContext);
    }

    @Test
    public void testOnInputMessageOpen() throws Exception {
        List<Attribute> attributes = new ArrayList<>();
        attributes.addAll(getHoldTime());
        attributes.addAll(getCapabilities());

        OpenMessage message = mock(OpenMessage.class);
        when(message.getVersion()).thenReturn(Version.Version4);
        when(message.getType()).thenReturn(MessageType.Open);
        when(message.getPayload()).thenReturn(new byte[]{});
        when(message.getAttribute()).thenReturn(attributes);

        when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
        verify(connection).setConnection(any(OpenMessage.class));
        verify(connection, never()).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
        verify(channelHandlerContext).writeAndFlush(isNotNull());

        when(message.getSxpMode()).thenReturn(ConnectionMode.Speaker);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
        verify(connection, times(2)).setConnection(any(OpenMessage.class));
        verify(connection, never()).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
        verify(channelHandlerContext, times(2)).writeAndFlush(isNotNull());

        //DeleteHoldDown/PendingOn/IPDropdown
        when(connection.isStateDeleteHoldDown()).thenReturn(true);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
        verify(connection).closeChannelHandlerContextComplements(any(ChannelHandlerContext.class));
        verify(connection, times(3)).setConnection(any(OpenMessage.class));
        verify(channelHandlerContext, times(3)).writeAndFlush(isNotNull());

        when(connection.isStateDeleteHoldDown()).thenReturn(false);
        when(connection.isStatePendingOn()).thenReturn(true);
        Channel channel = mock(Channel.class);
        InetSocketAddress localAddress = new InetSocketAddress("0.0.0.20", 0);
        InetSocketAddress remoteAddress = new InetSocketAddress("0.0.0.10", 0);
        when(channel.localAddress()).thenReturn(localAddress);
        when(channel.remoteAddress()).thenReturn(remoteAddress);
        when(channelHandlerContext.channel()).thenReturn(channel);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
        verify(connection).closeInitContextWithRemote(any(InetSocketAddress.class));
        verify(channelHandlerContext, times(3)).writeAndFlush(isNotNull());

        when(connection.getDestination()).thenReturn(remoteAddress);
        when(connection.isStateDeleteHoldDown()).thenReturn(false);
        when(connection.isStatePendingOn()).thenReturn(false);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
        verify(connection,times(4)).markChannelHandlerContext(any(ChannelHandlerContext.class));
        verify(connection, times(4)).setConnection(any(OpenMessage.class));
        verify(channelHandlerContext, times(4)).writeAndFlush(isNotNull());

        //BOTH mode
        when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
        when(connection.isModeBoth()).thenReturn(true);
        when(connection.isBidirectionalBoth()).thenReturn(false);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
        verify(connection).setConnectionSpeakerPart(any(OpenMessage.class));
        verify(connection).markChannelHandlerContext(any(ChannelHandlerContext.class),
                any(SxpConnection.ChannelHandlerContextType.class));
        verify(channelHandlerContext, times(5)).writeAndFlush(isNotNull());

        when(message.getSxpMode()).thenReturn(ConnectionMode.Speaker);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
        verify(channelHandlerContext, times(6)).writeAndFlush(isNotNull());
        verify(connection).setStateOff(any(ChannelHandlerContext.class));

        when(connection.isBidirectionalBoth()).thenReturn(true);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
        verify(connection, times(5)).setConnection(any(OpenMessage.class));
    }

    @Test
    public void testOnInputMessageOpenResp() throws Exception {
        OpenMessage message = mock(OpenMessage.class);
        when(message.getVersion()).thenReturn(Version.Version4);
        when(message.getType()).thenReturn(MessageType.OpenResp);
        when(message.getPayload()).thenReturn(new byte[]{});
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

    @Test
    public void testOnInputMessageUpdate() throws Exception {
        UpdateMessage message = mock(UpdateMessage.class);
        when(message.getType()).thenReturn(MessageType.Update);
        when(message.getPayload()).thenReturn(new byte[]{});

        when(connection.isStateOn(SxpConnection.ChannelHandlerContextType.LISTENER_CNTXT)).thenReturn(true);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
        verify(connection).setUpdateOrKeepaliveMessageTimestamp();
        verify(connection).processUpdateMessage(any(UpdateMessage.class));

        when(connection.getState()).thenReturn(ConnectionState.Off);
        when(connection.isStateOn(SxpConnection.ChannelHandlerContextType.LISTENER_CNTXT)).thenReturn(false);
        exception.expect(UpdateMessageConnectionStateException.class);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
    }

    @Test
    public void testOnInputMessageError() throws Exception {
        ErrorMessage message = mock(ErrorMessage.class);
        when(message.getInformation()).thenReturn("");
        when(message.getType()).thenReturn(MessageType.Error);
        when(message.getPayload()).thenReturn(new byte[]{});

        exception.expect(ErrorMessageReceivedException.class);
        sxpv4.onInputMessage(channelHandlerContext, connection, message);
    }

    @Test
    public void testOnInputMessagePurgeAll() throws Exception {
        PurgeAllMessage message = mock(PurgeAllMessage.class);
        when(message.getType()).thenReturn(MessageType.PurgeAll);

        when(connection.getDestination()).thenReturn(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 5));
        sxpv4.onInputMessage(channelHandlerContext, connection, message);

        verify(sxpNode.getWorker()).executeTaskInSequence(any(Callable.class), eq(ThreadsWorker.WorkerType.INBOUND),
                any(SxpConnection.class));
    }

    @Test
    public void testOnInputMessageKeepAlive() throws Exception {
        KeepaliveMessage message = mock(KeepaliveMessage.class);
        when(message.getType()).thenReturn(MessageType.Error);

        sxpv4.onInputMessage(channelHandlerContext, connection, message);
        verify(connection).setUpdateOrKeepaliveMessageTimestamp();
    }

    @Test
    public void testOnParseInputGoldenPath() throws Exception {
        Assert.assertNotNull(sxpv4.onParseInput(LegacyMessageFactory.createOpen(Version.Version4, ConnectionMode.Both)));
    }

    @Test(expected = ErrorMessageException.class)
    public void testOnParseInputExceptionHandling() throws Exception {
        sxpv4.onParseInput(Unpooled.buffer());
    }

    @Test(expected = UpdateMessageCompositionException.class)
    public void testOnUpdateMessageIllegalSqt() throws Exception {
        MasterDatabaseBinding binding = new MasterDatabaseBindingBuilder()
                .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.10.10.10/24")))
                .setSecurityGroupTag(new Sgt(1))
                .setOrigin(BindingOriginsConfig.NETWORK_ORIGIN)
                .setPeerSequence(new PeerSequenceBuilder().setPeer(Collections.emptyList()).build())
                .build();
        sxpv4.onUpdateMessage(connection, Collections.emptyList(), Collections.singletonList(binding), null);
    }
}
