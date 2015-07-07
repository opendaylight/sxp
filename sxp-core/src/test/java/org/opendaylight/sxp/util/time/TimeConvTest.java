/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;

import static org.junit.Assert.assertEquals;

public class TimeConvTest {

        @Test public void testToDt() throws Exception {
                DateAndTime time = TimeConv.toDt(954216546);
                assertEquals("1970-01-12T02:03:36Z", time.getValue());
        }

        @Test public void testToLong() throws Exception {
                DateAndTime time = new DateAndTime("1970-01-08T15:33:35Z");
                assertEquals(657215000, TimeConv.toLong(time));
                assertEquals(-1, TimeConv.toLong(null));
        }
}
