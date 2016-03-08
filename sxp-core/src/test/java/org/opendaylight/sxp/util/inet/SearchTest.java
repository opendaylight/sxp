/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.inet;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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

        private <T extends SxpBindingFields> List<T> getBindings(String... strings) {
                List<T> bindings = new ArrayList<>();
                MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
                for (String s : strings) {
                        bindings.add((T) bindingBuilder.setIpPrefix(new IpPrefix(s.toCharArray())).build());
                }
                return bindings;
        }

        @Test public void testExpandBindings() throws Exception {
                assertEquals(2, Search.expandBindings(getBindings("127.0.0.5/24"), 2).size());
                assertEquals(1, Search.expandBindings(getBindings("127.0.0.5/32"), 20).size());
                assertEquals(21, Search.expandBindings(getBindings("127.0.0.5/32","8.8.8.8/24"), 20).size());
        }

        @Test public void testExpandBinding() throws Exception {
                assertEquals(256, Search.expandBinding(
                        new MasterDatabaseBindingBuilder().setIpPrefix(IpPrefixConv.createPrefix("130.4.102.1/24")).build(),
                    Short.MAX_VALUE).size());
                assertEquals(256, Search.expandBinding(new MasterDatabaseBindingBuilder().setIpPrefix(
                        IpPrefixConv.createPrefix("2001:db8::ff00:42:8329/120")).build(), Short.MAX_VALUE).size());
        }
}
