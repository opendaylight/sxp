/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;

public final class Security {

    private static final String ALGORITHM = "MD5";

    private static final String CHARSET_NAME = "US-ASCII";

    public static byte[] getMD5b(String str) throws Exception {
        byte[] byteArray = str.getBytes(Charset.forName(CHARSET_NAME));
        MessageDigest md = MessageDigest.getInstance(ALGORITHM);
        return md.digest(byteArray);
    }

    /**
     * The MD5 message-digest algorithm is a widely used cryptographic hash
     * function producing a 128-bit hash value, typically expressed in text
     * format as a 32 digit hexadecimal number.
     * 
     * @throws Exception
     */
    public static String getMD5s(String str) throws Exception {
        byte[] digest = getMD5b(str);

        String md5hash = new BigInteger(1, digest).toString(16);
        while (md5hash.length() < 32) {
            md5hash = "0" + md5hash;
        }
        return md5hash;
    }
}
