/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging.legacy;

import io.netty.buffer.ByteBuf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.OpenMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LegacyMessageFactoryTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private byte[] toBytes(ByteBuf message) {
                byte[] _message = new byte[message.readableBytes()];
                message.readBytes(_message);
                message.release();
                return _message;
        }

        @Test public void testCreateError() throws Exception {
                ByteBuf message = LegacyMessageFactory.createError(ErrorCodeNonExtended.NoError, null);
                assertNotNull(message);
                message.release();

                message = LegacyMessageFactory.createError(ErrorCodeNonExtended.MessageParseError);
                byte[] result = new byte[] {0, 0, 0, 12, 0, 0, 0, 4, 0, 0, 0, 2};
                assertArrayEquals(result, toBytes(message));

                message = LegacyMessageFactory.createError(ErrorCodeNonExtended.NoError);
                result = new byte[] {0, 0, 0, 12, 0, 0, 0, 4, 0, 0, 0, 0};
                assertArrayEquals(result, toBytes(message));

                message = LegacyMessageFactory.createError(ErrorCodeNonExtended.VersionMismatch);
                result = new byte[] {0, 0, 0, 12, 0, 0, 0, 4, 0, 0, 0, 1};
                assertArrayEquals(result, toBytes(message));
        }

        @Test public void testCreateOpen() throws Exception {
                //VERSION 1
                ByteBuf message = LegacyMessageFactory.createOpen(Version.Version1, ConnectionMode.Listener);
                byte[] result = new byte[] {0, 0, 0, 16, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 2};
                assertArrayEquals(result, toBytes(message));

                message = LegacyMessageFactory.createOpen(Version.Version1, ConnectionMode.Speaker);
                result = new byte[] {0, 0, 0, 16, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1};
                assertArrayEquals(result, toBytes(message));

                //VERSION 2
                message = LegacyMessageFactory.createOpen(Version.Version2, ConnectionMode.Listener);
                result = new byte[] {0, 0, 0, 16, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 2};
                assertArrayEquals(result, toBytes(message));

                message = LegacyMessageFactory.createOpen(Version.Version2, ConnectionMode.Speaker);
                result = new byte[] {0, 0, 0, 16, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 1};
                assertArrayEquals(result, toBytes(message));

                //VERSION 3
                message = LegacyMessageFactory.createOpen(Version.Version3, ConnectionMode.Listener);
                result = new byte[] {0, 0, 0, 16, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 2};
                assertArrayEquals(result, toBytes(message));

                message = LegacyMessageFactory.createOpen(Version.Version3, ConnectionMode.Speaker);
                result = new byte[] {0, 0, 0, 16, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 1};
                assertArrayEquals(result, toBytes(message));
        }

        @Test public void testCreateOpenResp() throws Exception {
                //VERSION 1
                ByteBuf message = LegacyMessageFactory.createOpenResp(Version.Version1, ConnectionMode.Speaker);
                byte[] result = new byte[] {0, 0, 0, 16, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 1};
                assertArrayEquals(result, toBytes(message));

                message = LegacyMessageFactory.createOpenResp(Version.Version1, ConnectionMode.Listener);
                result = new byte[] {0, 0, 0, 16, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 2};
                assertArrayEquals(result, toBytes(message));

                //VERSION 2
                message = LegacyMessageFactory.createOpenResp(Version.Version2, ConnectionMode.Speaker);
                result = new byte[] {0, 0, 0, 16, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 1};
                assertArrayEquals(result, toBytes(message));

                message = LegacyMessageFactory.createOpenResp(Version.Version2, ConnectionMode.Listener);
                result = new byte[] {0, 0, 0, 16, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 2};
                assertArrayEquals(result, toBytes(message));

                //VERSION 3
                message = LegacyMessageFactory.createOpenResp(Version.Version3, ConnectionMode.Speaker);
                result = new byte[] {0, 0, 0, 16, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0, 1};
                assertArrayEquals(result, toBytes(message));

                message = LegacyMessageFactory.createOpenResp(Version.Version3, ConnectionMode.Listener);
                result = new byte[] {0, 0, 0, 16, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0, 2};
                assertArrayEquals(result, toBytes(message));
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

                prefixGroups.add(createPrefixGroup(DatabaseAction.Delete, 10, "192.168.10.1/32"));
                prefixGroups.add(createPrefixGroup(DatabaseAction.Delete, 30, "2000::1/128"));
                prefixGroups.add(createPrefixGroup(DatabaseAction.Add, 10000, "192.168.0.1/32"));
                prefixGroups.add(createPrefixGroup(DatabaseAction.Add, 20000, "2001::1/64", "10.10.10.10/30"));
                prefixGroups.add(createPrefixGroup(DatabaseAction.Add, 30000, "2002::1/128"));
                prefixGroups.add(createPrefixGroup(DatabaseAction.Add, 40000, "11.11.11.0/29"));
                prefixGroups.add(createPrefixGroup(DatabaseAction.Add, 65000, "172.168.1.0/28"));

                sourceBuilder.setPrefixGroup(prefixGroups);
                sourceList.add(sourceBuilder.build());
                when(database.getSource()).thenReturn(sourceList);

                ByteBuf message = LegacyMessageFactory.createUpdate(database, false, Version.Version1);
                //192.168.10.1/32, 192.168.0.1/32
                byte[]
                        result =
                        new byte[] {0, 0, 0, 42, 0, 0, 0, 3, 0, 0, 0, 3, 0, 0, 0, 4, -64, -88, 10, 1, 0, 0, 0, 1, 0, 0,
                                0, 14, -64, -88, 0, 1, 0, 0, 0, 1, 0, 0, 0, 2, 39, 16};
                assertArrayEquals(result, toBytes(message));

                message = LegacyMessageFactory.createUpdate(database, false, Version.Version2);
                //2000::1/128, 192.168.10.1/32, 192.168.0.1/32, 2002::1/128
                result =
                        new byte[] {0, 0, 0, 100, 0, 0, 0, 3, 0, 0, 0, 3, 0, 0, 0, 4, -64, -88, 10, 1, 0, 0, 0, 4, 0, 0,
                                0, 16, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 14, -64,
                                -88, 0, 1, 0, 0, 0, 1, 0, 0, 0, 2, 39, 16, 0, 0, 0, 2, 0, 0, 0, 26, 32, 2, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 2, 117, 48};
                assertArrayEquals(result, toBytes(message));

                message = LegacyMessageFactory.createUpdate(database, false, Version.Version3);
                //ALL
                result =
                        new byte[] {0, 0, 0, -20, 0, 0, 0, 3, 0, 0, 0, 3, 0, 0, 0, 4, -64, -88, 10, 1, 0, 0, 0, 4, 0, 0,
                                0, 16, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 14, -64,
                                -88, 0, 1, 0, 0, 0, 1, 0, 0, 0, 2, 39, 16, 0, 0, 0, 2, 0, 0, 0, 35, 32, 1, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 1, 64, 0, 0, 0, 1, 0, 0, 0, 2, 78,
                                32, 0, 0, 0, 1, 0, 0, 0, 23, 10, 10, 10, 10, 0, 0, 0, 2, 0, 0, 0, 1, 30, 0, 0, 0, 1, 0,
                                0, 0, 2, 78, 32, 0, 0, 0, 2, 0, 0, 0, 26, 32, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                1, 0, 0, 0, 1, 0, 0, 0, 2, 117, 48, 0, 0, 0, 1, 0, 0, 0, 23, 11, 11, 11, 0, 0, 0, 0, 2,
                                0, 0, 0, 1, 29, 0, 0, 0, 1, 0, 0, 0, 2, -100, 64, 0, 0, 0, 1, 0, 0, 0, 23, -84, -88, 1,
                                0, 0, 0, 0, 2, 0, 0, 0, 1, 28, 0, 0, 0, 1, 0, 0, 0, 2, -3, -24};
                assertArrayEquals(result, toBytes(message));
        }

        @Test public void testDecodeOpen() throws Exception {
                //VERSION 1
                OpenMessageLegacy
                        message =
                        (OpenMessageLegacy) LegacyMessageFactory.decodeOpen(new byte[] {0, 0, 0, 1, 0, 0, 0, 2});
                assertEquals(ConnectionMode.Listener, message.getSxpMode());
                assertEquals(Version.Version1, message.getVersion());
                message = (OpenMessageLegacy) LegacyMessageFactory.decodeOpen(new byte[] {0, 0, 0, 1, 0, 0, 0, 1});
                assertEquals(ConnectionMode.Speaker, message.getSxpMode());
                assertEquals(Version.Version1, message.getVersion());

                //VERSION 2
                message = (OpenMessageLegacy) LegacyMessageFactory.decodeOpen(new byte[] {0, 0, 0, 2, 0, 0, 0, 2});
                assertEquals(ConnectionMode.Listener, message.getSxpMode());
                assertEquals(Version.Version2, message.getVersion());

                message = (OpenMessageLegacy) LegacyMessageFactory.decodeOpen(new byte[] {0, 0, 0, 2, 0, 0, 0, 1});
                assertEquals(ConnectionMode.Speaker, message.getSxpMode());
                assertEquals(Version.Version2, message.getVersion());

                //VERSION 3
                message = (OpenMessageLegacy) LegacyMessageFactory.decodeOpen(new byte[] {0, 0, 0, 3, 0, 0, 0, 2});
                assertEquals(ConnectionMode.Listener, message.getSxpMode());
                assertEquals(Version.Version3, message.getVersion());

                message = (OpenMessageLegacy) LegacyMessageFactory.decodeOpen(new byte[] {0, 0, 0, 3, 0, 0, 0, 1});
                assertEquals(ConnectionMode.Speaker, message.getSxpMode());
                assertEquals(Version.Version3, message.getVersion());

        }

        @Test public void testDecodeOpenResp() throws Exception {
                //VERSION 1
                OpenMessageLegacy
                        message =
                        (OpenMessageLegacy) LegacyMessageFactory.decodeOpenResp(new byte[] {0, 0, 0, 1, 0, 0, 0, 2});
                assertEquals(ConnectionMode.Listener, message.getSxpMode());
                assertEquals(Version.Version1, message.getVersion());
                message = (OpenMessageLegacy) LegacyMessageFactory.decodeOpenResp(new byte[] {0, 0, 0, 1, 0, 0, 0, 1});
                assertEquals(ConnectionMode.Speaker, message.getSxpMode());
                assertEquals(Version.Version1, message.getVersion());

                //VERSION 2
                message = (OpenMessageLegacy) LegacyMessageFactory.decodeOpenResp(new byte[] {0, 0, 0, 2, 0, 0, 0, 2});
                assertEquals(ConnectionMode.Listener, message.getSxpMode());
                assertEquals(Version.Version2, message.getVersion());

                message = (OpenMessageLegacy) LegacyMessageFactory.decodeOpenResp(new byte[] {0, 0, 0, 2, 0, 0, 0, 1});
                assertEquals(ConnectionMode.Speaker, message.getSxpMode());
                assertEquals(Version.Version2, message.getVersion());

                //VERSION 3
                message = (OpenMessageLegacy) LegacyMessageFactory.decodeOpenResp(new byte[] {0, 0, 0, 3, 0, 0, 0, 2});
                assertEquals(ConnectionMode.Listener, message.getSxpMode());
                assertEquals(Version.Version3, message.getVersion());

                message = (OpenMessageLegacy) LegacyMessageFactory.decodeOpenResp(new byte[] {0, 0, 0, 3, 0, 0, 0, 1});
                assertEquals(ConnectionMode.Speaker, message.getSxpMode());
                assertEquals(Version.Version3, message.getVersion());

                exception.expect(ErrorMessageException.class);
                LegacyMessageFactory.decodeOpenResp(new byte[] {0, 0, 0, 4, 0, 0, 0, 1});
        }

        @Test public void testDecodeUpdate() throws Exception {
                UpdateMessageLegacy
                        message =
                        (UpdateMessageLegacy) LegacyMessageFactory.decodeUpdate(
                                new byte[] {0, 0, 0, 1, 0, 0, 0, 14, -64, -88, 0, 1, 0, 0, 0, 1, 0, 0, 0, 2, 39, 16, 0,
                                        0, 0, 3, 0, 0, 0, 4, -64, -88, 0, 2});
                assertEquals(2, message.getMappingRecord().size());
                org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord
                        record =
                        message.getMappingRecord().get(0);
                assertEquals(record.getOperationCode(), AttributeType.AddIpv4);
                assertEquals(record.getAddress(), new IpPrefix("192.168.0.1/32".toCharArray()));

                record = message.getMappingRecord().get(1);
                assertEquals(record.getOperationCode(), AttributeType.DelIpv4);
                assertEquals(record.getAddress(), new IpPrefix("192.168.0.2/32".toCharArray()));

        }
}
