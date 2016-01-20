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

    /**
     * Converts Byte Array into Integer value
     *
     * @param bytes Array that will be converted
     * @return Integer value of specified Array
     */
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

    /**
     * Merge multiple Byte Arrays into one
     *
     * @param bytes Array to be merged together
     * @return Merged Arrays
     */
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
        for (byte[] aByte : bytes) {
            if (aByte != null) {
                System.arraycopy(aByte, 0, combined, shift, aByte.length);
                shift += aByte.length;
            }
        }
        return combined;
    }

    /**
     * Converts series of Boolean values to byte
     *
     * @param bits Boolean values representing bits
     * @return Byte created with specified bit values
     */
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

    /**
     * Creates a new copy of Array
     *
     * @param source Array that will be copied
     * @return Newly created Array with copied values
     */
    public static byte[] copy(byte[] source) {
        if (source == null) {
            return new byte[0];
        }
        byte[] result = new byte[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }

    /**
     * Gets bit value from Byte at specified position
     *
     * @param _byte    Byte where to look for bit value
     * @param position Position of bit
     * @return Value of bit at specified position
     */
    public static int getBit(byte _byte, int position) {
        position--;
        return _byte >> position & 1;
    }

    /**
     * Converts Integer value to Array of bytes
     *
     * @param value Integer to be transformed
     * @return Array of bytes representing Integer value
     */
    public static byte[] int2bytes(int value) {
        return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
    }

    /**
     * Converts Integer value to Array of bytes and skips first bytes
     *
     * @param value        Integer to be transformed
     * @param nbytes2cropp Number of Bytes to be skipped
     * @return Array of bytes representing Integer value
     */
    public static byte[] int2bytesCropp(int value, int nbytes2cropp) {
        byte[] _value = int2bytes(value);
        return Arrays.copyOfRange(_value, nbytes2cropp, _value.length);
    }

    /**
     * Reads Bytes from Array and return them in new Array
     *
     * @param source Array to be read
     * @param start  Index from which is copying started
     * @return Array with data read from specified Array
     */
    public static byte[] readBytes(byte[] source, int start) {
        if (source == null || source.length == 0) {
            return new byte[0];
        }
        byte[] result = new byte[source.length - start];
        System.arraycopy(source, start, result, 0, source.length - start);
        return result;
    }

    /**
     * Reads Bytes from Array and return them in new Array
     *
     * @param source Array to be read
     * @param start  Index from which is copying started
     * @param length Length of Array where will be stored data
     * @return Array with data read from specified Array
     */
    public static byte[] readBytes(byte[] source, int start, int length) {
        if (source == null || source.length == 0) {
            return new byte[0];
        }
        byte[] result = new byte[length];
        System.arraycopy(source, start, result, 0, length);
        return result;
    }

    /**
     * @param x byte value which is used for reverse
     * @return byte with reversed order of bits
     */
    public static byte reverseBitsByte(byte x) {
        int intSize = 8;
        byte y = 0;
        for (int position = intSize - 1; position >= 0; position--) {
            y += ((x & 1) << position);
            x >>= 1;
        }
        return y;
    }

    /**
     * Sets a bit in the byte
     *
     * @param _byte    Byte where the bit will be changed
     * @param position Position of bit in Byte, i.e. 1 to 8 to specified value
     * @param value    Value to be set as (True=1,False=0)
     * @return Byte with changed bit at specified position
     */
    public static byte setBit(byte _byte, int position, boolean value) {
        position--;
        return (byte) (value ? _byte | 1 << position : _byte & ~(1 << position));
    }
}
