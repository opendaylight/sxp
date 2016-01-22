/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.inet;

import java.net.InetAddress;
import java.util.Comparator;


/**
 * InetAddressComparator class used for comparing InetAddresses
 */
public class InetAddressComparator implements Comparator<InetAddress> {

    private static final InetAddressComparator inetAddressComparator = new InetAddressComparator();

    /**
     * Check if InetAddress is greater than other one
     *
     * @param address1 InetAddress to compare
     * @param address2 InetAddress to compare
     * @return If first address is greater than second
     */
    public static boolean greaterThan(InetAddress address1, InetAddress address2) {
        return inetAddressComparator.compare(address1, address2) == 1;
    }

    @Override
    public int compare(InetAddress address1, InetAddress address2) {
        if (address1 == null && address2 == null) {
            return 0;
        } else if (address1 == null) {
            return -1;
        } else if (address2 == null) {
            return 1;
        }

        byte[] addr1 = address1.getAddress();
        byte[] addr2 = address2.getAddress();

        // Prefer ipv6 before ipv4.
        if (addr1.length < addr2.length) {
            return -1;
        } else if (addr1.length > addr2.length) {
            return 1;
        }

        // 2 IPs of the same type
        for (int i = 0; i < addr1.length; i++) {
            int b1 = ubyte2int(addr1[i]);
            int b2 = ubyte2int(addr2[i]);
            if (b1 < b2) {
                return -1;
            } else if (b1 > b2) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Converts Unsigned Byte to Integer
     *
     * @param b Byte to convert
     * @return Integer value representing specified Byte
     */
    private int ubyte2int(byte b) {
        return b & 0xFF;
    }
}
