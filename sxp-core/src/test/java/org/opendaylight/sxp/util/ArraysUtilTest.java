/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ArraysUtilTest {

        @Test public void testBytes2int() throws Exception {
                assertEquals(0, ArraysUtil.bytes2int(null));
                assertEquals(960488, ArraysUtil.bytes2int(new byte[] {0, 14, -89, -24}));
                assertEquals(127, ArraysUtil.bytes2int(new byte[] {0, 0, 0, 127}));
                assertEquals(2560488, ArraysUtil.bytes2int(new byte[] {0, 39, 17, -24}));
        }

        @Test public void testCombine() throws Exception {
                assertNull(ArraysUtil.combine(null));
                assertArrayEquals(new byte[] {}, ArraysUtil.combine(new byte[] {}));
                assertArrayEquals(new byte[] {1, 3, 2}, ArraysUtil.combine(new byte[] {1, 3, 2}, new byte[] {}));
                assertArrayEquals(new byte[] {1, 5, 9, 3, 5, 7},
                        ArraysUtil.combine(new byte[] {1, 5, 9}, new byte[] {3, 5, 7}));
                assertArrayEquals(new byte[] {1, 3, 2}, ArraysUtil.combine(new byte[] {1, 3, 2}));
        }

        @Test public void testConvertBits() throws Exception {
                assertEquals(1, ArraysUtil.convertBits(false, false, false, true));
                assertEquals(127, ArraysUtil.convertBits(true, true, true, true, true, true, true));
                assertEquals(74, ArraysUtil.convertBits(true, false, false, true, false, true, false));
        }

        @Test public void testCopy() throws Exception {
                assertArrayEquals(new byte[] {56, 98, 23}, ArraysUtil.copy(new byte[] {56, 98, 23}));
                assertArrayEquals(new byte[] {}, ArraysUtil.copy(new byte[] {}));
                assertArrayEquals(new byte[] {}, ArraysUtil.copy(null));
        }

        @Test public void testGetBit() throws Exception {
                assertEquals(0, ArraysUtil.getBit((byte) 126, 0));
                assertEquals(0, ArraysUtil.getBit((byte) 119, 4));
                assertEquals(1, ArraysUtil.getBit((byte) 126, 6));
        }

        @Test public void testInt2bytes() throws Exception {
                assertArrayEquals(new byte[] {0, 14, -89, -24}, ArraysUtil.int2bytes(960488));
                assertArrayEquals(new byte[] {0, 0, 0, 127}, ArraysUtil.int2bytes(127));
                assertArrayEquals(new byte[] {0, 39, 17, -24}, ArraysUtil.int2bytes(2560488));
        }

        @Test public void testInt2bytesCropp() throws Exception {
                assertArrayEquals(new byte[] {}, ArraysUtil.int2bytesCropp(960488, 4));
                assertArrayEquals(new byte[] {0, 127}, ArraysUtil.int2bytesCropp(127, 2));
                assertArrayEquals(new byte[] {0, 39, 17, -24}, ArraysUtil.int2bytesCropp(2560488, 0));
        }

        @Test public void testReadBytes() throws Exception {
                assertArrayEquals(new byte[] {10, 4, -89, -24}, ArraysUtil.readBytes(new byte[] {10, 4, -89, -24}, 0));
                assertArrayEquals(new byte[] {-89, -24}, ArraysUtil.readBytes(new byte[] {10, 4, -89, -24}, 2));
                assertArrayEquals(new byte[] {}, ArraysUtil.readBytes(new byte[] {10, 4, -89, -24}, 4));

                assertArrayEquals(new byte[] {10}, ArraysUtil.readBytes(new byte[] {10, 4, -89, -24}, 0, 1));
                assertArrayEquals(new byte[] {-89, -24}, ArraysUtil.readBytes(new byte[] {10, 4, -89, -24}, 2, 2));
                assertArrayEquals(new byte[] {10, 4, -89, -24},
                        ArraysUtil.readBytes(new byte[] {10, 4, -89, -24}, 0, 4));
                assertArrayEquals(new byte[] {}, ArraysUtil.readBytes(new byte[] {}, 0, 4));
                assertArrayEquals(new byte[] {}, ArraysUtil.readBytes(null, 0, 4));
        }

        @Test public void testSetBit() throws Exception {
                assertEquals(1, ArraysUtil.setBit(1, true));
                assertEquals(4, ArraysUtil.setBit(3, true));

                assertEquals(81, ArraysUtil.setBit((byte) 80, 1, true));
                assertEquals(119, ArraysUtil.setBit((byte) 127, 4, false));
        }

        @Test public void testTrimZerosPrime() throws Exception {
                assertArrayEquals(new byte[] {}, ArraysUtil.trimZerosPrime(null));
                assertArrayEquals(new byte[] {}, ArraysUtil.trimZerosPrime(new byte[] {}));
                assertArrayEquals(new byte[] {12, 56}, ArraysUtil.trimZerosPrime(new byte[] {0, 0, 12, 56}));
                assertArrayEquals(new byte[] {52, 0, 12, 56, 0},
                        ArraysUtil.trimZerosPrime(new byte[] {0, 52, 0, 12, 56, 0}));
        }

        @Test public void testTrimZerosTail() throws Exception {
                assertArrayEquals(new byte[] {}, ArraysUtil.trimZerosTail(null));
                assertArrayEquals(new byte[] {}, ArraysUtil.trimZerosTail(new byte[] {}));
                assertArrayEquals(new byte[] {0, 0, 12, 56}, ArraysUtil.trimZerosTail(new byte[] {0, 0, 12, 56}));
                assertArrayEquals(new byte[] {0, 52, 0, 12, 56},
                        ArraysUtil.trimZerosTail(new byte[] {0, 52, 0, 12, 56, 0}));
        }
}
