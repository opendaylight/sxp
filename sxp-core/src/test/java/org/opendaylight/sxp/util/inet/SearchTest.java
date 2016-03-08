/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.inet;

import org.junit.Test;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class SearchTest {

        @Test public void testGetBestLocalDeviceAddress() throws Exception {
                assertNotEquals(null, Search.getBestLocalDeviceAddress());
        }

        @Test public void testGetAddress() throws Exception {
                assertEquals("50.23.21.5", Search.getAddress(new IpAddress(Ipv4Address.getDefaultInstance("50.23.21.5"))));
                assertNotEquals("50.23.21.5", Search.getAddress(new IpAddress(Ipv4Address.getDefaultInstance("0.0.21.0"))));
                assertEquals("2001:0:0:0:0:0:0:0",
                        Search.getAddress(new IpAddress(Ipv6Address.getDefaultInstance("2001:0:0:0:0:0:0:0"))));
                assertNotEquals("2001:0:0:0:0:0:0:0",
                        Search.getAddress(new IpAddress(Ipv6Address.getDefaultInstance("2001:0:5:0:0:0:fe:0"))));
        }
}
