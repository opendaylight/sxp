/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.inet;

import org.opendaylight.sxp.util.exception.connection.NoNetworkInterfacesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Search {

    private static final Logger LOG = LoggerFactory.getLogger(Search.class.getName());
    private static int bestAddresPointer = 1;

    /**
     * Gets Local address selected by heuristic
     *
     * @return InetAddress that isn't virtual and is Up
     * @throws NoNetworkInterfacesException If there is no NetworkInterface available
     * @throws SocketException              If an I/O error occurs
     */
    public static InetAddress getBestLocalDeviceAddress() throws NoNetworkInterfacesException, SocketException {

        List<InetAddress> inetAddresses = new ArrayList<>();
        List<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            throw new NoNetworkInterfacesException();
        }

        for (NetworkInterface networkInterface : networkInterfaces) {
            if (networkInterface.isUp() && !networkInterface.isVirtual()) {
                LOG.debug("[{}] {}", networkInterface.getName(), networkInterface.getDisplayName());
                inetAddresses.addAll(Collections.list(networkInterface.getInetAddresses()));
            }
        }

        Collections.sort(inetAddresses, new InetAddressComparator());
        return inetAddresses.get(inetAddresses.size() > bestAddresPointer + 1 ?
                inetAddresses.size() - bestAddresPointer++ : inetAddresses.size() - (bestAddresPointer = 1));
    }

    /**
     * @param address IpAddress to be represented by string
     * @return String representation of IpAddress
     */
    public static String getAddress(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress address) {
        if (address.getIpv4Address() != null) {
            return address.getIpv4Address().getValue();
        } else if (address.getIpv6Address() != null) {
            return address.getIpv6Address().getValue();
        }
        throw new IllegalArgumentException("Address " + address + " has illegal value.");
    }
}
