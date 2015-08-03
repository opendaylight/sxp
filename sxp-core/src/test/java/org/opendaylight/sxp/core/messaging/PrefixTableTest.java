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
import org.opendaylight.sxp.util.exception.message.attribute.PrefixTableAttributeIsNotCompactException;
import org.opendaylight.sxp.util.exception.message.attribute.PrefixTableColumnsSizeException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.FlagsFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrefixTableTest {

        @Rule public ExpectedException exception = ExpectedException.none();
        private static PrefixTable prefixTable;

        @Before public void init() throws PrefixTableColumnsSizeException {
                prefixTable = new PrefixTable(1);
        }

        @Test public void testAddItem() throws Exception {
                IpPrefix prefix = new IpPrefix(Ipv4Prefix.getDefaultInstance("0.0.0.0/32"));
                prefixTable.addItem(prefix, AttributeFactory.createCapabilities(Version.Version4));
                assertNotNull(prefixTable.get(prefix));
                assertNull(prefixTable.get(new IpPrefix(Ipv4Prefix.getDefaultInstance("1.1.1.1/32"))));
        }

        @Test public void testAddItemException0() throws Exception {
                exception.expect(PrefixTableColumnsSizeException.class);
                prefixTable.addItem(new IpPrefix(Ipv4Prefix.getDefaultInstance("0.0.0.0/32")));
        }

        @Test public void testAddItemException1() throws Exception {
                Attribute attribute = mock(Attribute.class);
                FlagsFields.Flags flags = new FlagsFields.Flags(false, false, false, false, false);
                when(attribute.getFlags()).thenReturn(flags);
                exception.expect(PrefixTableAttributeIsNotCompactException.class);
                prefixTable.addItem(new IpPrefix(Ipv4Prefix.getDefaultInstance("0.0.0.0/32")), attribute);
        }

        @Test public void testAddItemException2() throws Exception {
                exception.expect(IllegalArgumentException.class);
                prefixTable.addItem(null);
        }

        @Test public void testToBytes() throws Exception {
                IpPrefix prefix = new IpPrefix(Ipv4Prefix.getDefaultInstance("0.0.0.0/32"));
                prefixTable.addItem(prefix, AttributeFactory.createCapabilities(Version.Version4));

                prefix = new IpPrefix(Ipv4Prefix.getDefaultInstance("1.2.3.4/32"));
                prefixTable.addItem(prefix, AttributeFactory.createSourceGroupTag(10));

                assertArrayEquals(
                        new byte[] {1, 0, 0, 0, 80, 0, 0, 16, 0, 0, 0, 0, 3, 0, 2, 0, 1, 0, 48, 46, 48, 46, 48, 46, 48,
                                47, 51, 50, 0, 10, 49, 46, 50, 46, 51, 46, 52, 47, 51, 50}, prefixTable.toBytes());

                exception.expect(PrefixTableColumnsSizeException.class);
                prefixTable.addItem(prefix, AttributeFactory.createCapabilities(Version.Version3),
                        AttributeFactory.createHoldTime(10));
        }
}
