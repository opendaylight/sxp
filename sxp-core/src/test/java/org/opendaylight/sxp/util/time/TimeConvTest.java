/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeConvTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        @Test public void testToDt() throws Exception {
                TimeConv.setTimeZone("GMT+1");
                DateAndTime time = TimeConv.toDt(660815000);
                assertEquals("1970-01-08T16:33:35Z", time.getValue());

                TimeConv.setTimeZone("GMT+2");
                time = TimeConv.toDt(660815000);
                assertEquals("1970-01-08T17:33:35Z", time.getValue());
        }

        @Test public void testToLong() throws Exception {
                assertEquals(-1, TimeConv.toLong(null));

                TimeConv.setTimeZone("GMT+1");
                DateAndTime time = new DateAndTime("1970-01-08T16:33:35Z");
                assertEquals(660815000, TimeConv.toLong(time));

                TimeConv.setTimeZone("GMT+2");
                time = new DateAndTime("1970-01-08T17:33:35Z");
                assertEquals(660815000, TimeConv.toLong(time));

                time = mock(DateAndTime.class);
                when(time.getValue()).thenReturn("temp");
                exception.expect(Exception.class);
                assertEquals(-1, TimeConv.toLong(time));
        }

        @Test public void testSetTimeZone() throws Exception {
                try {
                        TimeConv.setTimeZone("GMT+5");
                        TimeConv.setTimeZone("");
                } catch (Exception e) {
                        fail();
                }
                exception.expect(IllegalArgumentException.class);
                TimeConv.setTimeZone(null);
        }
}
