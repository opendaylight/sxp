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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InetAddressComparatorTest {

        private static NodeId node1, node2, node3;
        private static InetAddress address1, address2, address3;

        @BeforeClass public static void init() throws UnknownHostException {
                node1 = new NodeId(new Ipv4Address("127.0.0.1"));
                node2 = new NodeId(new Ipv4Address("0.0.0.1"));
                node3 = new NodeId(new Ipv4Address("127.124.56.1"));

                address1 = InetAddress.getByName("127.0.0.1");
                address2 = InetAddress.getByName("0.0.0.1");
                address3 = InetAddress.getByName("127.124.56.1");
        }

        @Test public void testGreaterThan() throws Exception {
                assertFalse(InetAddressComparator.greaterThan(address1, address3));
                assertFalse(InetAddressComparator.greaterThan(address2, address3));
        }

}
