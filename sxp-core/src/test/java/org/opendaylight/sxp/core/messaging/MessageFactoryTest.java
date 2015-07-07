/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.sxp.util.exception.ErrorCodeDataLengthException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.*;
import org.opendaylight.yangtools.yang.binding.Notification;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageFactoryTest {

        @Rule public ExpectedException exception = ExpectedException.none();
        private static NodeId nodeId;

        private byte[] toBytes(ByteBuf message) {
                byte[] _message = new byte[message.readableBytes()];
                message.readBytes(_message);
                message.release();
                return _message;
        }

        @BeforeClass public static void init() throws Exception {
                nodeId = NodeIdConv.createNodeId("192.168.0.1");
        }

        @Test public void testCreateError() throws Exception {
                ByteBuf
                        message =
                        MessageFactory.createError(ErrorCode.MessageHeaderError, ErrorSubCode.MalformedAttributeList,
                                null);
                assertNotNull(message);
                assertArrayEquals(new byte[] {0, 0, 0, 12, 0, 0, 0, 4, -127, 1, 0, 0}, toBytes(message));
                message =
                        MessageFactory.createError(ErrorCode.OpenMessageError,
                                ErrorSubCode.UnrecognizedWellKnownAttribute, null);
                assertArrayEquals(new byte[] {0, 0, 0, 12, 0, 0, 0, 4, -126, 2, 0, 0}, toBytes(message));
                message =
                        MessageFactory.createError(ErrorCode.UpdateMessageError, ErrorSubCode.MissingWellKnownAttribute,
                                null);
                assertArrayEquals(new byte[] {0, 0, 0, 12, 0, 0, 0, 4, -125, 3, 0, 0}, toBytes(message));
                for (byte i = 4; i < 11; i++) {
                        message =
                                MessageFactory.createError(ErrorCode.UpdateMessageError, ErrorSubCode.forValue(i),
                                        null);
                        assertArrayEquals(new byte[] {0, 0, 0, 12, 0, 0, 0, 4, -125, i, 0, 0}, toBytes(message));
                }

                exception.expect(ErrorCodeDataLengthException.class);
                MessageFactory.createError(ErrorCode.MessageHeaderError, ErrorSubCode.MalformedAttributeList,
                        new byte[11]);
        }

        @Test public void testCreateKeepalive() throws Exception {
                ByteBuf message = MessageFactory.createKeepalive();
                assertArrayEquals(new byte[] {0, 0, 0, 8, 0, 0, 0, 6}, toBytes(message));
        }

        @Test public void testCreateOpen() throws Exception {
                ByteBuf
                        message =
                        MessageFactory.createOpen(Version.Version4, ConnectionMode.Listener, nodeId, 120, 150);
                byte[]
                        result =
                        new byte[] {0, 0, 0, 32, 0, 0, 0, 1, 0, 0, 0, 4, 0, 0, 0, 2, 80, 6, 6, 3, 0, 2, 0, 1, 0, 80, 7,
                                4, 0, 120, 0, -106};
                assertArrayEquals(result, toBytes(message));
                message = MessageFactory.createOpen(Version.Version4, ConnectionMode.Speaker, nodeId, 120, 150);

                result =
                        new byte[] {0, 0, 0, 30, 0, 0, 0, 1, 0, 0, 0, 4, 0, 0, 0, 1, 80, 5, 4, -64, -88, 0, 1, 80, 7, 4,
                                0, 120, 0, -106};
                assertArrayEquals(result, toBytes(message));
        }

        @Test public void testCreateOpenResp() throws Exception {
                ByteBuf
                        message =
                        MessageFactory.createOpenResp(Version.Version4, ConnectionMode.Listener, nodeId, 120, 150);
                byte[]
                        result =
                        new byte[] {0, 0, 0, 32, 0, 0, 0, 2, 0, 0, 0, 4, 0, 0, 0, 2, 80, 6, 6, 3, 0, 2, 0, 1, 0, 80, 7,
                                4, 0, 120, 0, -106};
                assertArrayEquals(result, toBytes(message));

                message = MessageFactory.createOpenResp(Version.Version4, ConnectionMode.Speaker, nodeId, 120, 150);
                result =
                        new byte[] {0, 0, 0, 30, 0, 0, 0, 2, 0, 0, 0, 4, 0, 0, 0, 1, 80, 5, 4, -64, -88, 0, 1, 80, 7, 4,
                                0, 120, 0, -106};
                assertArrayEquals(result, toBytes(message));
        }

        @Test public void testCreatePurgeAll() throws Exception {
                ByteBuf message = MessageFactory.createPurgeAll();
                assertArrayEquals(new byte[] {0, 0, 0, 8, 0, 0, 0, 5}, toBytes(message));
        }

        private PrefixGroup createPrefixGroup(DatabaseAction action, int sgt, String... bindings) {
                PrefixGroup prefixGroup = mock(PrefixGroup.class);
                when(prefixGroup.getSgt()).thenReturn(new Sgt(sgt));
                List<Binding> bindingList = new ArrayList<>();
                for (String binding : bindings) {
                        BindingBuilder bindingBuilder = new BindingBuilder();
                        bindingBuilder.setAction(action);
                        bindingBuilder.setIpPrefix(new IpPrefix(binding.toCharArray()));
                        bindingList.add(bindingBuilder.build());
                }
                when(prefixGroup.getBinding()).thenReturn(bindingList);
                return prefixGroup;
        }

        @Test public void testCreateUpdate() throws Exception {
                MasterDatabase database = mock(MasterDatabase.class);
                SourceBuilder sourceBuilder = new SourceBuilder();
                List<PrefixGroup> prefixGroups = new ArrayList<>();
                List<Source> sourceList = new ArrayList<>();

                prefixGroups.add(createPrefixGroup(DatabaseAction.Delete, 10000, "192.168.0.1/32"));
                prefixGroups.add(createPrefixGroup(DatabaseAction.Add, 20000, "2001::1/64", "10.10.10.10/30"));
                prefixGroups.add(createPrefixGroup(DatabaseAction.Delete, 30000, "2002::1/128"));
                prefixGroups.add(createPrefixGroup(DatabaseAction.Add, 40000, "11.11.11.0/29"));
                prefixGroups.add(createPrefixGroup(DatabaseAction.Add, 65000, "172.168.1.0/28"));

                sourceBuilder.setBindingSource(DatabaseBindingSource.Local);
                sourceBuilder.setPrefixGroup(prefixGroups);
                sourceList.add(sourceBuilder.build());
                when(database.getSource()).thenReturn(sourceList);
                ByteBuf message = MessageFactory.createUpdate(database, nodeId, false);

                byte[]
                        result =
                        new byte[] {0, 0, 0, 108, 0, 0, 0, 3, 16, 13, 5, 32, -64, -88, 0, 1, 16, 14, 17, -128, 32, 2, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 16, 16, 4, -64, -88, 0, 1, 16, 17, 2, 78, 32, 16,
                                11, 5, 30, 10, 10, 10, 10, 16, 12, 9, 64, 32, 1, 0, 0, 0, 0, 0, 0, 16, 16, 4, -64, -88,
                                0, 1, 16, 17, 2, -100, 64, 16, 11, 5, 29, 11, 11, 11, 0, 16, 16, 4, -64, -88, 0, 1, 16,
                                17, 2, -3, -24, 16, 11, 5, 28, -84, -88, 1, 0};
                assertArrayEquals(result, toBytes(message));
        }

        @Test public void testCreateUpdateAddPrefixes() throws Exception {
                List<PrefixGroup> prefixGroups = new ArrayList<>();
                prefixGroups.add(createPrefixGroup(DatabaseAction.Add, 20000, "2001::1/64", "10.10.10.10/30"));

                List<NodeId> peerSequence = new ArrayList<>();
                peerSequence.add(NodeIdConv.createNodeId("192.168.5.1"));

                ByteBuf message = MessageFactory.createUpdateAddPrefixes(nodeId, peerSequence, prefixGroups);
                assertArrayEquals(
                        new byte[] {0, 0, 0, 44, 0, 0, 0, 3, 16, 16, 8, -64, -88, 0, 1, -64, -88, 5, 1, 16, 17, 2, 78,
                                32, 16, 11, 5, 30, 10, 10, 10, 10, 16, 12, 9, 64, 32, 1, 0, 0, 0, 0, 0, 0},
                        toBytes(message));
        }

        @Test public void testCreateUpdateDeletePrefixes() throws Exception {
                List<PrefixGroup> prefixGroups = new ArrayList<>();

                prefixGroups.add(createPrefixGroup(DatabaseAction.Delete, 10000, "192.168.0.1/32"));
                prefixGroups.add(createPrefixGroup(DatabaseAction.Delete, 30000, "2002::1/128"));

                ByteBuf message = MessageFactory.createUpdateDeletePrefixes(prefixGroups);
                assertArrayEquals(
                        new byte[] {0, 0, 0, 36, 0, 0, 0, 3, 16, 13, 5, 32, -64, -88, 0, 1, 16, 14, 17, -128, 32, 2, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1}, toBytes(message));
        }

        @Test public void testDecodeErrorMessage() throws Exception {
                ErrorMessage message = (ErrorMessage) MessageFactory.decodeErrorMessage(new byte[] {-127, 1, 0, 0});
                assertNotNull(message);
                assertEquals(ErrorType.Extended, message.getErrorType());
                assertEquals(ErrorCode.MessageHeaderError, message.getErrorCode());
                assertEquals(ErrorSubCode.MalformedAttributeList, message.getErrorSubCode());

                message = (ErrorMessage) MessageFactory.decodeErrorMessage(new byte[] {-126, 2, 0, 0});
                assertNotNull(message);
                assertEquals(ErrorType.Extended, message.getErrorType());
                assertEquals(ErrorCode.OpenMessageError, message.getErrorCode());
                assertEquals(ErrorSubCode.UnrecognizedWellKnownAttribute, message.getErrorSubCode());

                message = (ErrorMessage) MessageFactory.decodeErrorMessage(new byte[] {-125, 3, 0, 0});
                assertNotNull(message);
                assertEquals(ErrorType.Extended, message.getErrorType());
                assertEquals(ErrorCode.UpdateMessageError, message.getErrorCode());
                assertEquals(ErrorSubCode.MissingWellKnownAttribute, message.getErrorSubCode());

                for (byte i = 4; i < 11; i++) {
                        message = (ErrorMessage) MessageFactory.decodeErrorMessage(new byte[] {-125, i, 0, 0});
                        assertNotNull(message);
                        assertEquals(ErrorType.Extended, message.getErrorType());
                        assertEquals(ErrorCode.UpdateMessageError, message.getErrorCode());
                        assertEquals(ErrorSubCode.forValue(i), message.getErrorSubCode());
                }
        }

        @Test public void testDecodeKeepalive() throws Exception {
                KeepaliveMessage
                        message =
                        (KeepaliveMessage) MessageFactory.decodeKeepalive(new byte[] {0, 0, 0, 8, 0, 0, 0, 6});
                assertNotNull(message);
                assertEquals(MessageType.Keepalive, message.getType());
        }

        @Test public void testDecodeOpen() throws Exception {
                OpenMessage
                        message =
                        (OpenMessage) MessageFactory.decodeOpen(
                                new byte[] {0, 0, 0, 4, 0, 0, 0, 2, 80, 6, 6, 3, 0, 2, 0, 1, 0, 80, 7, 4, 0, 120, 0,
                                        -106});
                assertNotNull(message);
                assertEquals(ConnectionMode.Listener, message.getSxpMode());
                assertEquals(Version.Version4, message.getVersion());
                assertEquals(MessageType.Open, message.getType());

                message =
                        (OpenMessage) MessageFactory.decodeOpen(
                                new byte[] {0, 0, 0, 4, 0, 0, 0, 1, 80, 5, 4, -64, -88, 0, 1, 80, 7, 4, 0, 120, 0,
                                        -106});
                assertNotNull(message);
                assertEquals(ConnectionMode.Speaker, message.getSxpMode());
                assertEquals(Version.Version4, message.getVersion());
                assertEquals(MessageType.Open, message.getType());
        }

        @Test public void testDecodeOpenResp() throws Exception {
                OpenMessage
                        message =
                        (OpenMessage) MessageFactory.decodeOpenResp(
                                new byte[] {0, 0, 0, 4, 0, 0, 0, 2, 80, 6, 6, 3, 0, 2, 0, 1, 0, 80, 7, 4, 0, 120, 0,
                                        -106});
                assertNotNull(message);
                assertEquals(ConnectionMode.Listener, message.getSxpMode());
                assertEquals(Version.Version4, message.getVersion());
                assertEquals(MessageType.OpenResp, message.getType());

                message =
                        (OpenMessage) MessageFactory.decodeOpenResp(
                                new byte[] {0, 0, 0, 4, 0, 0, 0, 1, 80, 5, 4, -64, -88, 0, 1, 80, 7, 4, 0, 120, 0,
                                        -106});
                assertNotNull(message);
                assertEquals(ConnectionMode.Speaker, message.getSxpMode());
                assertEquals(Version.Version4, message.getVersion());
                assertEquals(MessageType.OpenResp, message.getType());
        }

        @Test public void testExtractVersion() throws Exception {
                for (byte i = 1; i < 4; i++) {
                        assertEquals(Version.forValue(i), MessageFactory.extractVersion(new byte[] {0, 0, 0, i}));
                }
        }

        @Test public void testDecodePurgeAll() throws Exception {
                PurgeAllMessage
                        message =
                        (PurgeAllMessage) MessageFactory.decodePurgeAll(new byte[] {0, 0, 0, 8, 0, 0, 0, 6});
                assertNotNull(message);
                assertEquals(MessageType.PurgeAll, message.getType());
        }

        @Test public void testDecodeUpdate() throws Exception {
                UpdateMessage
                        message =
                        (UpdateMessage) MessageFactory.decodeUpdate(
                                new byte[] {16, 13, 5, 32, -64, -88, 0, 1, 16, 14, 17, -128, 32, 2, 0, 0, 0, 0, 0, 0, 0,
                                        0, 0, 0, 0, 0, 0, 1, 16, 16, 4, -64, -88, 0, 1, 16, 17, 2, 78, 32, 16, 11, 5,
                                        30, 10, 10, 10, 10, 16, 12, 9, 64, 32, 1, 0, 0, 0, 0, 0, 0});

                Ipv4AddPrefixAttribute
                        ipv4AddPrefixAttribute =
                        (Ipv4AddPrefixAttribute) AttributeList.get(message.getAttribute(), AttributeType.Ipv4AddPrefix);
                assertTrue(ipv4AddPrefixAttribute.getIpv4AddPrefixAttributes()
                        .getIpPrefix()
                        .contains(new IpPrefix("10.10.10.10/30".toCharArray())));

                Ipv6AddPrefixAttribute
                        ipv6AddPrefixAttribute =
                        (Ipv6AddPrefixAttribute) AttributeList.get(message.getAttribute(), AttributeType.Ipv6AddPrefix);
                assertTrue(ipv6AddPrefixAttribute.getIpv6AddPrefixAttributes()
                        .getIpPrefix()
                        .contains(new IpPrefix("2001:0:0:0:0:0:0:0/64".toCharArray())));

                Ipv4DeletePrefixAttribute
                        ipv4DeletePrefixAttribute =
                        (Ipv4DeletePrefixAttribute) AttributeList.get(message.getAttribute(),
                                AttributeType.Ipv4DeletePrefix);
                assertTrue(ipv4DeletePrefixAttribute.getIpv4DeletePrefixAttributes()
                        .getIpPrefix()
                        .contains(new IpPrefix("192.168.0.1/32".toCharArray())));

                Ipv6DeletePrefixAttribute
                        ipv6DeletePrefixAttribute =
                        (Ipv6DeletePrefixAttribute) AttributeList.get(message.getAttribute(),
                                AttributeType.Ipv6DeletePrefix);
                assertTrue(ipv6DeletePrefixAttribute.getIpv6DeletePrefixAttributes()
                        .getIpPrefix()
                        .contains(new IpPrefix("2002:0:0:0:0:0:0:1/128".toCharArray())));

        }

        @Test public void testIsLegacy() throws Exception {
                assertTrue(MessageFactory.isLegacy(Version.Version1));
                assertTrue(MessageFactory.isLegacy(Version.Version2));
                assertTrue(MessageFactory.isLegacy(Version.Version3));
                assertFalse(MessageFactory.isLegacy(Version.Version4));
        }

        @Test public void testParseOpen() throws Exception {
                byte[]
                        msg =
                        new byte[] {0, 0, 0, 32, 0, 0, 0, 1, 0, 0, 0, 4, 0, 0, 0, 2, 80, 6, 6, 3, 0, 2, 0, 1, 0, 80, 7,
                                4, 0, 120, 0, -106};
                ByteBuf message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                Notification notification = MessageFactory.parse(Version.Version4, message);
                message.release();
                assertTrue(notification instanceof OpenMessage);
                assertEquals(MessageType.Open, ((OpenMessage) notification).getType());
                msg =
                        new byte[] {0, 0, 0, 32, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 2, 80, 6, 6, 3, 0, 2, 0, 1, 0, 80, 7,
                                4, 0, 120, 0, -106};
                message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                notification = MessageFactory.parse(Version.Version4, message);
                message.release();
                assertTrue(notification instanceof OpenMessageLegacy);
                assertEquals(MessageType.Open, ((OpenMessageLegacy) notification).getType());

        }

        @Test public void testParseResp() throws Exception {
                byte[]
                        msg =
                        new byte[] {0, 0, 0, 32, 0, 0, 0, 2, 0, 0, 0, 4, 0, 0, 0, 2, 80, 6, 6, 3, 0, 2, 0, 1, 0, 80, 7,
                                4, 0, 120, 0, -106};
                ByteBuf message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                Notification notification = MessageFactory.parse(Version.Version4, message);
                message.release();
                assertTrue(notification instanceof OpenMessage);
                assertEquals(MessageType.OpenResp, ((OpenMessage) notification).getType());
                msg =
                        new byte[] {0, 0, 0, 32, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0, 2, 80, 6, 6, 3, 0, 2, 0, 1, 0, 80, 7,
                                4, 0, 120, 0, -106};
                message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                notification = MessageFactory.parse(Version.Version3, message);
                message.release();
                assertTrue(notification instanceof OpenMessageLegacy);
                assertEquals(MessageType.OpenResp, ((OpenMessageLegacy) notification).getType());

        }

        @Test public void testParseUpdate() throws Exception {
                byte[]
                        msg =
                        new byte[] {0, 0, 0, 68, 0, 0, 0, 3, 16, 13, 5, 32, -64, -88, 0, 1, 16, 14, 17, -128, 32, 2, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 16, 16, 4, -64, -88, 0, 1, 16, 17, 2, 78, 32, 16,
                                11, 5, 30, 10, 10, 10, 10, 16, 12, 9, 64, 32, 1, 0, 0, 0, 0, 0, 0};
                ByteBuf message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                Notification notification = MessageFactory.parse(Version.Version4, message);
                message.release();
                assertTrue(notification instanceof UpdateMessage);
                assertEquals(MessageType.Update, ((UpdateMessage) notification).getType());
                msg =
                        new byte[] {0, 0, 0, 42, 0, 0, 0, 3, 0, 0, 0, 1, 0, 0, 0, 14, -64, -88, 0, 1, 0, 0, 0, 1, 0, 0,
                                0, 2, 39, 16, 0, 0, 0, 3, 0, 0, 0, 4, -64, -88, 0, 2};
                message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                notification = MessageFactory.parse(Version.Version3, message);
                assertTrue(notification instanceof UpdateMessageLegacy);
                assertEquals(MessageType.Update, ((UpdateMessageLegacy) notification).getType());

        }

        @Test public void testParseError() throws Exception {
                byte[] msg = new byte[] {0, 0, 0, 12, 0, 0, 0, 4, -125, 3, 0, 0};
                ByteBuf message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                Notification notification = MessageFactory.parse(Version.Version4, message);
                message.release();
                assertTrue(notification instanceof ErrorMessage);
                assertEquals(MessageType.Error, ((ErrorMessage) notification).getType());
                msg = new byte[] {0, 0, 0, 12, 0, 0, 0, 4, -125, 3, 0, 0};
                message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                notification = MessageFactory.parse(Version.Version3, message);
                message.release();
                assertTrue(notification instanceof ErrorMessage);
                assertEquals(MessageType.Error, ((ErrorMessage) notification).getType());
        }

        @Test public void testParsePurgeAll() throws Exception {
                byte[] msg = new byte[] {0, 0, 0, 8, 0, 0, 0, 5};
                ByteBuf message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                Notification notification = MessageFactory.parse(Version.Version4, message);
                message.release();
                assertTrue(notification instanceof PurgeAllMessage);
                assertEquals(MessageType.PurgeAll, ((PurgeAllMessage) notification).getType());
                msg = new byte[] {0, 0, 0, 8, 0, 0, 0, 5};
                message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                notification = MessageFactory.parse(Version.Version3, message);
                message.release();
                assertTrue(notification instanceof PurgeAllMessage);
                assertEquals(MessageType.PurgeAll, ((PurgeAllMessage) notification).getType());
        }

        @Test public void testParse() throws Exception {
                //KEEPALIVE
                byte[] msg = new byte[] {0, 0, 0, 8, 0, 0, 0, 6};
                ByteBuf message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                Notification notification = MessageFactory.parse(Version.Version4, message);
                message.release();
                assertTrue(notification instanceof KeepaliveMessage);
                assertEquals(MessageType.Keepalive, ((KeepaliveMessage) notification).getType());

                //ERRORS
                msg = new byte[] {0, 0};
                message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                exception.expect(ErrorMessageException.class);
                MessageFactory.parse(Version.Version4, message);

                msg = new byte[] {0, 0, 0, 5, 0, 0, 0, 0};
                message = PooledByteBufAllocator.DEFAULT.buffer(msg.length);
                message.writeBytes(msg);
                exception.expect(ErrorMessageException.class);
                MessageFactory.parse(Version.Version4, message);

        }
}
