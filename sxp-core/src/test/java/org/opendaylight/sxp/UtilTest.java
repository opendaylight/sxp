/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.inet.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtilTest {

    private static int expansionQuantity;

    protected static final Logger LOG;

    static {
        Configuration.initializeLogger();
        LOG = LoggerFactory.getLogger(UtilTest.class.getName());
    }

    @BeforeClass
    public static void init() {
        expansionQuantity = Short.MAX_VALUE;
    }

    @Test
    public void UtililtiesTest() throws Exception {
        assertNotEquals(null, Search.getBestLocalDeviceAddress());

        for (int value = 0; value < 2 * Short.MAX_VALUE; value++) {
            assertEquals(value, ArraysUtil.bytes2int(ArraysUtil.int2bytes(value)));
        }

        AtomicInteger expansionQuantity = new AtomicInteger(UtilTest.expansionQuantity);
        assertEquals(1016, Search.getExpandedBindings("130.4.102.1", 22, expansionQuantity).size());
        assertEquals(254, Search.getExpandedBindings("2001:db8::ff00:42:8329", 120, expansionQuantity).size());
    }
}
