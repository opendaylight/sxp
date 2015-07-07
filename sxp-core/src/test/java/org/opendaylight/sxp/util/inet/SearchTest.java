/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.inet;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class SearchTest {

        @Test public void testGetAllSxpNodes() throws Exception {
                assertNotNull(Search.getAllSxpNodes());
                assertEquals(0, Search.getAllSxpNodes().size());
        }

        @Test public void testGetBestLocalDeviceAddress() throws Exception {
                assertNotEquals(null, Search.getBestLocalDeviceAddress());
        }

        @Test public void testGetExpandedBindings() throws Exception {
                AtomicInteger expansionQuantity = new AtomicInteger(Short.MAX_VALUE);
                assertEquals(1016, Search.getExpandedBindings("130.4.102.1", 22, expansionQuantity).size());
                assertEquals(254, Search.getExpandedBindings("2001:db8::ff00:42:8329", 120, expansionQuantity).size());
        }

}
