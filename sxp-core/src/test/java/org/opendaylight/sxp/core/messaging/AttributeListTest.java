/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeNotFoundException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeVariantException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeVariant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.FlagsFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.UnrecognizedAttribute;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class AttributeListTest {

        private static AttributeList attributes;
        @Rule public ExpectedException exception = ExpectedException.none();

        private Attribute getAttribute(AttributeType type, AttributeVariant variant, FlagsFields.Flags flags) {
                AttributeBuilder builder = new AttributeBuilder();
                builder.setAttributeVariant(variant);
                builder.setFlags(flags);
                builder.setType(type);
                builder.setLength(0);
                builder.setValue(new byte[] {});
                builder.setAttributeOptionalFields(mock(UnrecognizedAttribute.class));
                return builder.build();
        }

        @Before public void init() throws Exception {
                attributes = AttributeList.create(new ArrayList<Attribute>());
                attributes.add(getAttribute(AttributeType.HoldTime, AttributeVariant.Compact,
                        new FlagsFields.Flags(false, true, false, false, false)));
                attributes.add(getAttribute(AttributeType.Capabilities, AttributeVariant.CompactExtendedLength,
                        new FlagsFields.Flags(true, true, false, false, false)));
                attributes.add(getAttribute(AttributeType.Unspecified, AttributeVariant.NonCompact,
                        new FlagsFields.Flags(false, false, false, false, false)));
        }

        @Test public void testDecode() throws Exception {
                AttributeList
                        attributes =
                        AttributeList.decode(new byte[] {80, 6, 4, 2, 0, 1, 0, 80, 7, 4, 0, 10, 0, 20});
                assertNotNull(attributes.get(AttributeType.Capabilities));
                assertNotNull(attributes.get(AttributeType.HoldTime));
        }

        @Test public void testGet() throws Exception {
                assertNotNull(attributes.get(AttributeType.HoldTime));
                assertNotNull(attributes.get(AttributeType.Capabilities));
        }

        @Test public void testGetException0() throws Exception {
                exception.expect(AttributeNotFoundException.class);
                attributes.get(AttributeType.AddIpv6);
        }

        @Test public void testGetException1() throws Exception {
                exception.expect(AttributeNotFoundException.class);
                attributes.get(AttributeType.Ipv4DeletePrefix);
        }

        @Test public void testToBytes() throws Exception {
                assertArrayEquals(new byte[] {8, 7, 0, 24, 6, 0, 0, -1, -1, -1, 0, 0, 0, 0}, attributes.toBytes());

                attributes.clear();
                attributes.add(getAttribute(AttributeType.Unspecified, AttributeVariant.None,
                        new FlagsFields.Flags(false, false, false, false, false)));
                exception.expect(AttributeVariantException.class);
                attributes.toBytes();
        }
}
