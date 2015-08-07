/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.inet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

public class InetAddressComparator implements Comparator<InetAddress> {

    private static final InetAddressComparator inetAddressComparator = new InetAddressComparator();

    public static boolean equalTo(InetAddress address1, InetAddress address2) {
        return inetAddressComparator.compare(address1, address2) == 0;
    }

    public static boolean equalTo(NodeId nodeId1, NodeId nodeId2) throws UnknownHostException {
        return equalTo(InetAddress.getByAddress(NodeIdConv.toBytes(nodeId1)),
                InetAddress.getByAddress(NodeIdConv.toBytes(nodeId2)));
    }

    public static boolean greaterThan(InetAddress address1, InetAddress address2) {
        return inetAddressComparator.compare(address1, address2) == 1;
    }

    public static boolean greaterThan(NodeId nodeId1, NodeId nodeId2) throws UnknownHostException {
        return greaterThan(InetAddress.getByAddress(NodeIdConv.toBytes(nodeId1)),
                InetAddress.getByAddress(NodeIdConv.toBytes(nodeId2)));
    }

    public static boolean lessThan(InetAddress address1, InetAddress address2) {
        return inetAddressComparator.compare(address1, address2) == -1;
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
            if (b1 == b2) {
                continue;
            } else if (b1 < b2) {
                return -1;
            } else {
                return 1;
            }
        }
        return 0;
    }

    private int ubyte2int(byte b) {
        return b & 0xFF;
    }
}
