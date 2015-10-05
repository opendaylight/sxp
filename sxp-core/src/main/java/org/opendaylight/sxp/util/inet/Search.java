/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.inet;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.connection.NoNetworkInterfacesException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Search {

    /**
     * Class representing Ip Address used for subnet conversion of binding
     */
    private static final class IpAddress {

        private byte[] address, mask;

        private byte[] firstAddress, lastAddress;

        private int icol;

        private long n = 0;

        private byte[] subnetNumber, broadcastAddress;

        /**
         * Constructor used to set Address and prefix of Ip address
         *
         * @param inetAddress Ip Address of Binding
         * @param prefix      Prefix of binding
         * @throws UnknownHostException   If invalid Ip address was specified
         */
        private IpAddress(InetAddress inetAddress, int prefix) throws UnknownHostException {
            address = inetAddress.getAddress();

            mask = new BigInteger("-1").shiftLeft(address.length * 8 - prefix).toByteArray();
            while (mask.length < address.length) {
                mask = ArraysUtil.combine(new byte[] { (byte) 255 }, mask);
            }

            subnetNumber = new byte[address.length];
            broadcastAddress = new byte[address.length];
            firstAddress = new byte[address.length];
            lastAddress = new byte[address.length];

            int i = 0;
            icol = -1;
            for (byte b : mask) {
                if ((b & 0xFF) == 255) {
                    subnetNumber[i] = address[i];
                    broadcastAddress[i] = address[i];
                    firstAddress[i] = address[i];
                    lastAddress[i] = address[i];
                } else if (b == 0) {
                    subnetNumber[i] = 0;
                    broadcastAddress[i] = (byte) 255;
                } else {
                    icol = i;
                }
                i++;
            }

            // Classfull.
            if (icol < 0) {
                for (int j = 0; j < subnetNumber.length; j++) {
                    firstAddress[j] = subnetNumber[j];
                }
                firstAddress[firstAddress.length - 1] = 1;

                for (int j = 0; j < broadcastAddress.length; j++) {
                    lastAddress[j] = broadcastAddress[j];
                }
                lastAddress[lastAddress.length - 1] = (byte) ((lastAddress[lastAddress.length - 1] & 0xFF) - 1);

                LOG.info("<" + InetAddress.getByAddress(firstAddress) + " .. " + InetAddress.getByAddress(lastAddress)
                        + ">");
                return;
            }

            // Classless.
            int mgn = 256 - (mask[icol] & 0xFF);
            int mul = (address[icol] & 0xFF) / mgn;

            subnetNumber[icol] = (byte) (mul * mgn);
            broadcastAddress[icol] = (byte) ((subnetNumber[icol] & 0xFF) + mgn - 1);

            firstAddress[icol] = subnetNumber[icol];
            firstAddress[firstAddress.length - 1] = 1;

            for (int j = icol; j < broadcastAddress.length; j++) {
                lastAddress[j] = broadcastAddress[j];
            }
            lastAddress[lastAddress.length - 1] = (byte) ((lastAddress[lastAddress.length - 1] & 0xFF) - 1);

            LOG.info("<" + InetAddress.getByAddress(firstAddress) + " .. " + InetAddress.getByAddress(lastAddress)
                    + ">");

        }

        /**
         * Recursively expands Bindings into subnet
         *
         * @param expanded List of Bindings where will be expanded Bindings added
         * @param ipAddress Ip Address that will be expanded
         * @param depth Depth of recursion which is used as recursion stop
         * @param quantity Max number to limit the expansion
         * @throws UnknownHostException   If invalid Ip address was specified
         * @throws UnknownPrefixException If invalid prefix was specified
         */
        private void _expand(List<Binding> expanded, byte[] ipAddress, int depth, AtomicInteger quantity)
                throws UnknownHostException, UnknownPrefixException {
            byte[] _ipAddress = ArraysUtil.copy(ipAddress);
            if (depth >= _ipAddress.length - 1) {
                for (int i = firstAddress[depth] & 0xFF; i <= (lastAddress[depth] & 0xFF); i++) {
                    if (quantity.get() <= 0) {
                        return;
                    }

                    _ipAddress[depth] = (byte) i;
                    InetAddress inetAddress = InetAddress.getByAddress(_ipAddress);

                    BindingBuilder bindingBuilder = new BindingBuilder();
                    bindingBuilder.setAction(DatabaseAction.Add);
                    String _ipPrefix = inetAddress.toString() + (ipAddress.length > 4 ? "/128" : "/32");
                    bindingBuilder.setIpPrefix(IpPrefixConv.createPrefix(_ipPrefix));

                    LOG.debug(++n + " " + inetAddress);
                    expanded.add(bindingBuilder.build());
                    quantity.decrementAndGet();
                }
            } else if (firstAddress[depth] == lastAddress[depth]) {
                if (quantity.get() <= 0) {
                    return;
                }

                _expand(expanded, _ipAddress, depth + 1, quantity);
            } else {
                for (int i = firstAddress[depth] & 0xFF; i <= (lastAddress[depth] & 0xFF); i++) {
                    if (quantity.get() <= 0) {
                        return;
                    }

                    _ipAddress[depth] = (byte) i;
                    _expand(expanded, _ipAddress, depth + 1, quantity);
                }
            }
        }

        /**
         * Expand InetAddress into subnet bindings
         *
         * @param quantity Max number to limit the expansion
         * @return List of bindings that were created by expansion into subnet
         * @throws UnknownHostException   If invalid Ip address was specified
         * @throws UnknownPrefixException If invalid prefix was specified
         */
        private List<Binding> expand(AtomicInteger quantity) throws UnknownPrefixException, UnknownHostException {
            List<Binding> expanded = new ArrayList<>();
            _expand(expanded, firstAddress, 0, quantity);
            return expanded;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(Search.class.getName());
    private static int bestAddresPointer = 1;

    public static List<?> getAllSxpNodes() {

        return new ArrayList<InetAddress>();
    }

    /**
     * Gets Local address selected by heuristic
     *
     * @return InetAddress that isn't virtual and is Up
     * @throws NoNetworkInterfacesException If there is no NetworkInterface available
     * @throws SocketException              If an I/O error occurs
     */
    public static InetAddress getBestLocalDeviceAddress() throws NoNetworkInterfacesException, SocketException {

        List<InetAddress> inetAddresses = new ArrayList<InetAddress>();
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
     * Expands specified Binding into subnet.
     * Amount of subnet that will be expanded is limited by quantity.
     *
     * @param binding Bindings that will be expanded
     * @param quantity Max number to limit the expansion
     * @return List of bindings that were created by expansion into subnet
     * @throws UnknownHostException   If invalid Ip address was specified
     * @throws UnknownPrefixException If invalid prefix was specified
     */
    public static List<Binding> getExpandedBindings(Binding binding, AtomicInteger quantity)
            throws UnknownHostException, UnknownPrefixException {
        String[] ipPrefix = new String(binding.getIpPrefix().getValue()).split("/");

        List<NodeId> _peerSequence = NodeIdConv.getPeerSequence(binding.getPeerSequence());
        List<NodeId> _sources = NodeIdConv.getSources(binding.getSources());
        List<Binding> _expandedBindings = new ArrayList<Binding>();

        for (Binding expadedBinding : new IpAddress(InetAddress.getByName(ipPrefix[0]), Integer.parseInt(ipPrefix[1]))
                .expand(quantity)) {
            BindingBuilder _expandedBinding = new BindingBuilder(expadedBinding);
            _expandedBinding.setPeerSequence(NodeIdConv.createPeerSequence(_peerSequence));
            _expandedBinding.setSources(NodeIdConv.createSources(_sources));
            _expandedBinding.setTimestamp(new DateAndTime(binding.getTimestamp()));
            _expandedBindings.add(_expandedBinding.build());
        }

        return _expandedBindings;
    }

    /**
     * Expands Binding specified by Ip address and prefix into subnet.
     * Amount of subnet that will be expanded is limited by quantity.
     *
     * @param inetAddress Ip Address of Binding
     * @param prefix      Prefix of binding
     * @param quantity    Max number to limit the expansion
     * @return List of bindings that were created by expansion into subnet
     * @throws UnknownHostException   If invalid Ip address was specified
     * @throws UnknownPrefixException If invalid prefix was specified
     */
    public static List<Binding> getExpandedBindings(String inetAddress, int prefix, AtomicInteger quantity)
            throws UnknownHostException, UnknownPrefixException {
        return new IpAddress(InetAddress.getByName(inetAddress), prefix).expand(quantity);
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
