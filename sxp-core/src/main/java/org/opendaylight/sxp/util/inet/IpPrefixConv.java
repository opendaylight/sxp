/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.inet;

import com.google.common.net.InetAddresses;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;

public final class IpPrefixConv {

    private enum IpPrefixType {
        Ipv4Prefix, Ipv6Prefix
    }

    private static IpPrefix _decode(IpPrefixType ipPrefixType, byte[] array, boolean compact) throws UnknownHostException, UnknownPrefixException {
        int blength = getBytesLength(array[0]);
        byte[] bprefix = ArraysUtil.readBytes(array, compact ? 1 : 4, blength);

        byte[] prefix = ArraysUtil.copy(bprefix);
        // Add complement bytes.
        if (ipPrefixType.equals(IpPrefixType.Ipv4Prefix) && bprefix.length < 4) {
            prefix = ArraysUtil.combine(prefix, new byte[4 - prefix.length]);
        } else if (ipPrefixType.equals(IpPrefixType.Ipv6Prefix) && bprefix.length < 16) {
            prefix = ArraysUtil.combine(prefix, new byte[16 - prefix.length]);
        }

        InetAddress inetAddress = InetAddress.getByAddress(prefix);
        if (ipPrefixType.equals(IpPrefixType.Ipv4Prefix) && !(inetAddress instanceof Inet4Address)) {
            throw new UnknownPrefixException("Not IPv4 format [\"" + inetAddress + "\"]");
        } else if (ipPrefixType.equals(IpPrefixType.Ipv6Prefix) && !(inetAddress instanceof Inet6Address)) {
            throw new UnknownPrefixException("Not IPv6 format [\"" + inetAddress + "\"]");
        }
        String _prefix = inetAddress.toString();
        if (_prefix.startsWith("/")) {
            _prefix = _prefix.substring(1);
        }
        _prefix += "/" + (0xFF & array[0]);
        return new IpPrefix(_prefix.toCharArray());
    }

    public static IpPrefix createPrefix(String ipPrefix) throws UnknownPrefixException {
        if (ipPrefix == null || ipPrefix.isEmpty()) {
            throw new UnknownPrefixException("Not defined [\"" + ipPrefix + "\"]");
        }
        if (ipPrefix.startsWith("/")) {
            ipPrefix = ipPrefix.substring(1);
        }

        return new IpPrefix(new String(ipPrefix).toCharArray());
    }

    private static List<IpPrefix> decode(IpPrefixConv.IpPrefixType ipPrefixType, byte[] array, boolean compact)
            throws UnknownHostException, UnknownPrefixException {
        List<IpPrefix> prefixes = new ArrayList<IpPrefix>();
        do {
            // Reserved octets (not)presented.
            IpPrefix ipPrefix = _decode(ipPrefixType, array, compact);
            prefixes.add(ipPrefix);
            array = ArraysUtil.readBytes(array, (compact ? 1 : 4) + getBytesLength(getPrefixLength(ipPrefix)));
        } while (array.length != 0);
        return prefixes;
    }

    public static List<IpPrefix> decodeIpv4(byte[] array, boolean compact) throws UnknownHostException, UnknownPrefixException {
        return decode(IpPrefixType.Ipv4Prefix, array, compact);
    }

    public static List<IpPrefix> decodeIpv6(byte[] array, boolean compact) throws UnknownHostException, UnknownPrefixException {
        return decode(IpPrefixType.Ipv6Prefix, array, compact);
    }

    public static boolean equalTo(IpPrefix prefix1, IpPrefix prefix2) {
        return toString(prefix1).equals(toString(prefix2));
    }

    public static int getBytesLength(int prefixLength) {
        return (int) Math.ceil((0xFF & prefixLength) / 8.0);
    }

    public static int getPrefixLength(IpPrefix ipPrefix) {
        String _ipPrefix = new String(ipPrefix.getValue());
        int i = _ipPrefix.lastIndexOf("/");
        if (i == -1) {
            return i;
        }
        return Integer.parseInt(_ipPrefix.substring(i + 1));
    }

    public static int hashCode(List<IpPrefix> IpPrefix) {
        final int prime = 31;

        int result = 0;
        for (IpPrefix ipPrefix : IpPrefix) {
            result = prime * result + ipPrefix.hashCode();
        }
        return result;
    }

    public static InetSocketAddress parseInetPrefix(String ipPrefix) {
        if (ipPrefix.startsWith("/")) {
            ipPrefix = ipPrefix.substring(1);
        }
        InetAddress inetAddress;
        short prefix;

        int i = ipPrefix.indexOf("/");
        if (i != -1) {
            inetAddress = InetAddresses.forString(ipPrefix.substring(0, i));
            prefix = Short.valueOf(ipPrefix.substring(i + 1));
        } else {
            inetAddress = InetAddresses.forString(ipPrefix);
            if (inetAddress instanceof Inet4Address) {
                prefix = 32;
            } else if (inetAddress instanceof Inet6Address) {
                prefix = 128;
            } else {
                prefix = 0;
            }
        }
        return new InetSocketAddress(inetAddress, prefix);
    }

    public static byte[] toBytes(IpPrefix prefix) {
        String _prefix = new String(prefix.getValue());
        if (_prefix.startsWith("/")) {
            _prefix = _prefix.substring(1);
        }
        int i = _prefix.lastIndexOf("/");
        if (i != -1) {
            _prefix = _prefix.substring(0, i);
        }
        int length = getPrefixLength(prefix);
        byte[] bprefix = trimPrefix(InetAddresses.forString(_prefix).getAddress(), getBytesLength(length));
        if (Configuration.SET_COMPOSITION_ATTRIBUTE_COMPACT_NO_RESERVED_FIELDS) {
            return ArraysUtil.combine(new byte[] { ArraysUtil.int2bytes(length)[3] }, bprefix);
        }
        return ArraysUtil.combine(new byte[] { ArraysUtil.int2bytes(length)[3], 0x00, 0x00, 0x00 }, bprefix);
    }

    public static byte[] toBytes(List<IpPrefix> prefixes) {
        byte[] array = new byte[0];
        for (IpPrefix prefix : prefixes) {
            array = ArraysUtil.combine(array, toBytes(prefix));
        }
        return array;
    }

    public static String toString(IpPrefix prefix) {
        if (prefix == null) {
            return "";
        }
        String result = new String(prefix.getValue());
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    public static String toString(List<IpPrefix> prefixes) {
        String result = "";
        if (prefixes != null) {
            for (IpPrefix prefix : prefixes) {
                result += toString(prefix) + " ";
            }
        }
        result = result.trim();
        return result.replaceAll(" ", ",");
    }

    public static byte[] trimPrefix(byte[] prefix, int blength) {
        return ArraysUtil.readBytes(prefix, 0, blength);
    }
}