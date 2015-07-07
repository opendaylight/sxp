/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class SecurityTest {

        @Test public void testGetMD5b() throws Exception {
                assertArrayEquals(
                        new byte[] {-33, -22, -15, 3, -112, -27, 96, -82, -89, 69, -52, -70, 83, -32, 68, -19},
                        Security.getMD5b("cisco"));
                assertArrayEquals(new byte[] {-82, -15, -66, -70, 47, 118, 45, -89, -54, 70, 37, 51, -58, -107, 57, 62},
                        Security.getMD5b("opendaylight"));
        }

        @Test public void testGetMD5s() throws Exception {
                assertEquals("07982c55db2b9985d3391f02e639db9c", Security.getMD5s("cisco123"));
                assertEquals("feced0f4af8ce928a6e3a18db9544cb8", Security.getMD5s("org.opendaylight"));
        }
}
