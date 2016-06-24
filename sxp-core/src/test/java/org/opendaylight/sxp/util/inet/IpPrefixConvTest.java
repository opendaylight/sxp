/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.inet;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IpPrefixConvTest {

        private static IpPrefix ipPrefix1, ipPrefix2, ipPrefix3, ipPrefix4;
        private static List<IpPrefix> ipPrefixes;

        @BeforeClass public static void init() {
                ipPrefix1 = IpPrefixBuilder.getDefaultInstance("127.0.0.1/32");
                ipPrefix2 = IpPrefixBuilder.getDefaultInstance("0.0.0.0/16");
                ipPrefix3 = IpPrefixBuilder.getDefaultInstance("2001:db8:0:0:0:0:0:1/128");
                ipPrefix4 = IpPrefixBuilder.getDefaultInstance("2001:d8:0:0:0:0:0:0/32");

                ipPrefixes = new ArrayList<>();
                ipPrefixes.add(ipPrefix1);
                ipPrefixes.add(ipPrefix2);
                ipPrefixes.add(ipPrefix3);
                ipPrefixes.add(ipPrefix4);
        }

        @Test public void testCreatePrefix() throws Exception {
                assertEquals(ipPrefix1, IpPrefixConv.createPrefix("127.0.0.1/32"));
                assertEquals(ipPrefix2, IpPrefixConv.createPrefix("0.0.0.0/16"));
                assertEquals(ipPrefix3, IpPrefixConv.createPrefix("2001:db8:0:0:0:0:0:1/128"));
                assertEquals(ipPrefix4, IpPrefixConv.createPrefix("2001:d8:0:0:0:0:0:0/32"));
        }

        @Test public void testDecodeIpv4() throws Exception {
                assertNotNull(IpPrefixConv.decodeIpv4(null, true));
                assertNotNull(IpPrefixConv.decodeIpv4(new byte[] {}, true));
                List<IpPrefix> cmp = new ArrayList<>();
                cmp.add(ipPrefix1);
                cmp.add(ipPrefix2);
                assertEquals(cmp, IpPrefixConv.decodeIpv4(new byte[] {32, 127, 0, 0, 1, 16, 0, 0}, true));
        }

        @Test public void testDecodeIpv6() throws Exception {
                assertNotNull(IpPrefixConv.decodeIpv6(null, true));
                assertNotNull(IpPrefixConv.decodeIpv6(new byte[] {}, true));
                List<IpPrefix> cmp = new ArrayList<>();
                cmp.add(ipPrefix3);
                cmp.add(ipPrefix4);
                assertEquals(cmp, IpPrefixConv.decodeIpv6(
                        new byte[] {-128, 32, 1, 13, -72, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 32, 32, 1, 0, -40},
                        true));
        }

        @Test public void testEqualTo() throws Exception {
                assertTrue(IpPrefixConv.equalTo(ipPrefix1, ipPrefix1));
                assertFalse(IpPrefixConv.equalTo(ipPrefix1, ipPrefix3));
                assertFalse(IpPrefixConv.equalTo(ipPrefix3, ipPrefix4));
        }

        @Test public void testGetBytesLength() throws Exception {
                assertEquals(1, IpPrefixConv.getBytesLength(1));
                assertEquals(16, IpPrefixConv.getBytesLength(127));
        }

        @Test public void testGetPrefixLength() throws Exception {
                assertEquals(32, IpPrefixConv.getPrefixLength(ipPrefix1));
                assertEquals(16, IpPrefixConv.getPrefixLength(ipPrefix2));
                assertEquals(128, IpPrefixConv.getPrefixLength(ipPrefix3));
                assertEquals(32, IpPrefixConv.getPrefixLength(ipPrefix4));
        }

        @Test public void testParseInetPrefix() throws Exception {
                assertEquals(new InetSocketAddress(Inet4Address.getByName("122.23.56.2"), 32),
                        IpPrefixConv.parseInetPrefix("122.23.56.2/32"));
                assertEquals(new InetSocketAddress(Inet4Address.getByName("122.0.56.2"), 24),
                        IpPrefixConv.parseInetPrefix("122.0.56.2/24"));

                assertEquals(new InetSocketAddress(Inet6Address.getByName("2001:db8::1"), 128),
                        IpPrefixConv.parseInetPrefix("2001:db8::1/128"));
                assertEquals(new InetSocketAddress(Inet6Address.getByName("201:d8::1"), 32),
                        IpPrefixConv.parseInetPrefix("201:d8::1/32"));
        }

        @Test public void testToBytes() throws Exception {
                assertArrayEquals(new byte[] {32, 127, 0, 0, 1}, IpPrefixConv.toBytes(ipPrefix1));
                assertArrayEquals(new byte[] {16, 0, 0}, IpPrefixConv.toBytes(ipPrefix2));
                assertArrayEquals(new byte[] {-128, 32, 1, 13, -72, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                        IpPrefixConv.toBytes(ipPrefix3));
                assertArrayEquals(new byte[] {32, 32, 1, 0, -40}, IpPrefixConv.toBytes(ipPrefix4));

                assertNotNull(IpPrefixConv.toBytes(new ArrayList<IpPrefix>()));
                assertArrayEquals(
                        new byte[] {32, 127, 0, 0, 1, 16, 0, 0, -128, 32, 1, 13, -72, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                1, 32, 32, 1, 0, -40}, IpPrefixConv.toBytes(ipPrefixes));
        }

        @Test public void testToString() throws Exception {
                assertEquals("127.0.0.1/32", IpPrefixConv.toString(ipPrefix1));
                assertEquals("0.0.0.0/16", IpPrefixConv.toString(ipPrefix2));
                assertEquals("2001:db8:0:0:0:0:0:1/128", IpPrefixConv.toString(ipPrefix3));
                assertEquals("2001:d8:0:0:0:0:0:0/32", IpPrefixConv.toString(ipPrefix4));

                assertEquals("127.0.0.1/32,0.0.0.0/16,2001:db8:0:0:0:0:0:1/128,2001:d8:0:0:0:0:0:0/32",
                        IpPrefixConv.toString(ipPrefixes));
        }

}
