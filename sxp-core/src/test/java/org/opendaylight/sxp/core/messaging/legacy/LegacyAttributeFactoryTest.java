/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging.legacy;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.AddIpv4Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.AddIpv6Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.DeleteIpv4Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.DeleteIpv6Attribute;

import static org.junit.Assert.assertEquals;

public class LegacyAttributeFactoryTest {

        @Test public void testDecodeAddIPv4() throws Exception {
                AddIpv4Attribute
                        ipv4Attribute =
                        (AddIpv4Attribute) LegacyAttributeFactory.decodeAddIPv4(AttributeType.AddIpv4, 14,
                                new byte[] {0, 0, 0, 1, 0, 0, 0, 14, -64, -88, 0, 1, 0, 0, 0, 1, 0, 0, 0, 2, 39, 16});
                assertEquals(10000, ipv4Attribute.getAddIpv4Attributes().getSgt().intValue());
                assertEquals(new IpPrefix("192.168.0.1/32".toCharArray()),
                        ipv4Attribute.getAddIpv4Attributes().getIpPrefix());
        }

        @Test public void testDecodeAddIPv6() throws Exception {
                AddIpv6Attribute
                        ipv4Attribute =
                        (AddIpv6Attribute) LegacyAttributeFactory.decodeAddIPv6(AttributeType.AddIpv6, 26,
                                new byte[] {0, 0, 0, 2, 0, 0, 0, 26, 32, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0,
                                        0, 0, 1, 0, 0, 0, 2, 117, 48});
                assertEquals(30000, ipv4Attribute.getAddIpv6Attributes().getSgt().intValue());
                assertEquals(new IpPrefix("2002:0:0:0:0:0:0:1/128".toCharArray()),
                        ipv4Attribute.getAddIpv6Attributes().getIpPrefix());
        }

        @Test public void testDecodeDeleteIPv4() throws Exception {
                DeleteIpv4Attribute
                        ipv4Attribute =
                        (DeleteIpv4Attribute) LegacyAttributeFactory.decodeDeleteIPv4(AttributeType.DelIpv4, 13,
                                new byte[] {0, 0, 0, 3, 0, 0, 0, 13, -64, -88, 0, 0, 0, 0, 0, 2, 0, 0, 0, 1, 16});
                assertEquals(new IpPrefix("192.168.0.0/16".toCharArray()),
                        ipv4Attribute.getDeleteIpv4Attributes().getIpPrefix());
        }

        @Test public void testDecodeDeleteIPv6() throws Exception {
                DeleteIpv6Attribute
                        ipv4Attribute =
                        (DeleteIpv6Attribute) LegacyAttributeFactory.decodeDeleteIPv6(AttributeType.DelIpv6, 25,
                                new byte[] {0, 0, 0, 2, 0, 0, 0, 25, 32, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0,
                                        0, 0, 2, 0, 0, 0, 1, -128});
                assertEquals(new IpPrefix("2002:0:0:0:0:0:0:2/128".toCharArray()),
                        ipv4Attribute.getDeleteIpv6Attributes().getIpPrefix());
        }
}
