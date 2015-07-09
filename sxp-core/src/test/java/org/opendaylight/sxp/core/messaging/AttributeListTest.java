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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class AttributeListTest {

        private static AttributeList attributes;
        @Rule public ExpectedException exception = ExpectedException.none();

        @Before public void init() throws Exception {
                attributes = AttributeList.create(new ArrayList<Attribute>());
                attributes.add(AttributeFactory.createCapabilities(Version.Version2));
                attributes.add(AttributeFactory.createHoldTime(10, 20));
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

                exception.expect(AttributeNotFoundException.class);
                attributes.get(AttributeType.AddIpv6);
                exception.expect(AttributeNotFoundException.class);
                attributes.get(AttributeType.Ipv4DeletePrefix);
        }

        @Test public void testToBytes() throws Exception {

                assertArrayEquals(new byte[] {80, 6, 4, 2, 0, 1, 0, 80, 7, 4, 0, 10, 0, 20}, attributes.toBytes());
        }
}
