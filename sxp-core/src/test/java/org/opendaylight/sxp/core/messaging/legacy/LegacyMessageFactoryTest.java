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
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessageLegacy;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class LegacyMessageFactoryTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private byte[] toBytes(ByteBuf message) {
                byte[] _message = new byte[message.readableBytes()];
                message.readBytes(_message);
                message.release();
                return _message;
        }

        private MasterDatabaseBinding getBinding(int sgt, String prefix) throws UnknownPrefixException {
                MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
                bindingBuilder.setSecurityGroupTag(new Sgt(sgt));
                bindingBuilder.setTimestamp(TimeConv.toDt(System.currentTimeMillis()));
                bindingBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
                return bindingBuilder.build();
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

        @Test public void testCreateUpdate() throws Exception {
                List<SxpBindingFields> add = new ArrayList<>(), dell = new ArrayList<>();

                dell.add(getBinding(10, "192.168.10.1/32"));
                dell.add(getBinding(30, "2000::1/128"));
                add.add(getBinding(10000, "192.168.0.1/32"));
                add.add(getBinding(20000, "2001::1/64"));
                add.add(getBinding(20000, "10.10.10.10/30"));
                add.add(getBinding(30000, "2002::1/128"));
                add.add(getBinding(40000, "11.11.11.0/29"));
                add.add(getBinding(65000, "172.168.1.0/28"));

                ByteBuf message = LegacyMessageFactory.createUpdate(dell, add, Version.Version1, null);
                //192.168.10.1/32, 192.168.0.1/32
                byte[]
                        result =
                        new byte[] {0, 0, 0, 42, 0, 0, 0, 3, 0, 0, 0, 3, 0, 0, 0, 4, -64, -88, 10, 1, 0, 0, 0, 1, 0, 0,
                                0, 14, -64, -88, 0, 1, 0, 0, 0, 1, 0, 0, 0, 2, 39, 16};
                assertArrayEquals(result, toBytes(message));

                message = LegacyMessageFactory.createUpdate(dell, add, Version.Version2, null);
                //2000::1/128, 192.168.10.1/32, 192.168.0.1/32, 2002::1/128
                result =
                        new byte[] {0, 0, 0, 100, 0, 0, 0, 3, 0, 0, 0, 3, 0, 0, 0, 4, -64, -88, 10, 1, 0, 0, 0, 4, 0, 0,
                                0, 16, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 14, -64,
                                -88, 0, 1, 0, 0, 0, 1, 0, 0, 0, 2, 39, 16, 0, 0, 0, 2, 0, 0, 0, 26, 32, 2, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 2, 117, 48};
                assertArrayEquals(result, toBytes(message));

                message = LegacyMessageFactory.createUpdate(dell, add, Version.Version3, null);
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
                                        0, 0, 3, 0, 0, 0, 13, -64, -88, 0, 2, 0, 0, 0, 2, 0, 0, 0, 1, 32});
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
