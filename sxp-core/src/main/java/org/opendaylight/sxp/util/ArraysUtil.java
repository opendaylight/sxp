/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util;

import java.util.Arrays;

public final class ArraysUtil {
    public static int bytes2int(byte[] bytes) {
        if (bytes == null) {
            return 0;
        }
        switch (bytes.length) {
        case 1:
            return bytes[0] & 0xFF;
        case 2:
            return (bytes[0] & 0xFF) << 8 | bytes[1] & 0xFF;
        case 3:
            return (bytes[0] & 0xFF) << 16 | (bytes[1] & 0xFF) << 8 | bytes[2] & 0xFF;
        case 4:
            return (bytes[0] & 0xFF) << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | bytes[3] & 0xFF;
        default:
            return 0;
        }
    }

    public static byte[] combine(byte[]... bytes) {

        if (bytes == null) {
            return null;
        } else if (bytes.length == 1) {
            return bytes[0];
        }

        int combinedLength = 0;
        for (byte[] _bytes : bytes) {
            if (_bytes != null) {
                combinedLength += _bytes.length;
            }
        }

        byte[] combined = new byte[combinedLength];
        int shift = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != null) {
                System.arraycopy(bytes[i], 0, combined, shift, bytes[i].length);
                shift += bytes[i].length;
            }
        }
        return combined;
    }

    public static byte convertBits(Boolean... bits) {
        String number = "";
        int i = 0;
        for (boolean bit : bits) {
            number += bit ? "1" : "0";
            if (i == 7) {
                break;
            }
            i++;
        }
        return (byte) Integer.parseInt(number, 2);
    }

    public static byte[] copy(byte[] source) {
        if (source == null) {
            return new byte[0];
        }
        byte[] result = new byte[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }

    public static int getBit(byte _byte, int position) {
        position--;
        return _byte >> position & 1;
    }

    public static byte[] int2bytes(int value) {
        return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
    }

    public static byte[] int2bytesCropp(int value, int nbytes2cropp) {
        byte[] _value = int2bytes(value);
        return Arrays.copyOfRange(_value, nbytes2cropp, _value.length);
    }

    public static byte[] readBytes(byte[] source, int start) {
        if (source == null || source.length == 0) {
            return new byte[0];
        }
        byte[] result = new byte[source.length - start];
        System.arraycopy(source, start, result, 0, source.length - start);
        return result;
    }

    public static byte[] readBytes(byte[] source, int start, int length) {
        if (source == null || source.length == 0) {
            return new byte[0];
        }
        byte[] result = new byte[length];
        System.arraycopy(source, start, result, 0, length);
        return result;
    }

    /**
     * @param _byte
     * @param position
     *            position of a bit in the byte, i.e. 1 to 8
     * @param value
     * @return
     */
    public static byte setBit(byte _byte, int position, boolean value) {
        position--;
        return (byte) (value ? _byte | 1 << position : _byte & ~(1 << position));
    }

    public static byte setBit(int position, boolean value) {
        return setBit((byte) 0x00, position, value);
    }

    public static byte[] trimZerosPrime(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new byte[0];
        }
        int i = 0;
        while (i < bytes.length && bytes[i] == 0) {
            i++;
        }
        return Arrays.copyOfRange(bytes, i, bytes.length);
    }

    public static byte[] trimZerosTail(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new byte[0];
        }
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            i--;
        }
        return Arrays.copyOf(bytes, i + 1);
    }
}
