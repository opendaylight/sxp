/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util;

import com.google.common.hash.Hashing;

import java.math.BigInteger;
import java.nio.charset.Charset;

public final class Security {

    private static final String CHARSET_NAME = "US-ASCII";

    /**
     * Encrypt String value by MD5 message-digest algorithm
     *
     * @param str String to be encrypted by MD5
     * @return Encrypted data saved in Byte Array
     */
    public static byte[] getMD5b(String str) {
        return Hashing.md5().hashString(str, Charset.forName(CHARSET_NAME)).asBytes();
    }

    /**
     * Encrypt String value by MD5 message-digest algorithm
     *
     * @param str String to be encrypted by MD5
     * @return Encrypted data saved in String
     */
    public static String getMD5s(String str) {
        byte[] digest = getMD5b(str);

        String md5hash = new BigInteger(1, digest).toString(16);
        while (md5hash.length() < 32) {
            md5hash = "0" + md5hash;
        }
        return md5hash;
    }
}
