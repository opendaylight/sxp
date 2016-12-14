/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.spi;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

/**
 * Purpose: reflect configuration of virtual address routing to network
 */
public interface Routing {

    /**
     * @return Name virtual interface used in routing
     */
    String getInterface();

    /**
     * @param inf Interface name that will be used for routing
     * @return Current instance
     */
    Routing setInterface(String inf);

    /**
     * @return Virtual address used for routing
     */
    IpAddress getVirtualIp();

    /**
     * @param netmask network mask
     * @return self
     */
    Routing setNetmask(IpAddress netmask);

    /**
     * @return assigned network mask
     */
    IpAddress getNetmask();

    /**
     * @return If current routing as active
     */
    boolean isActive();

    /**
     * Removes virtual interface and ip-address via ifconfig,
     * operation requires administrator privileges
     *
     * @return If operation was successful
     */
    boolean removeRouteForCurrentService();

    /**
     * Creates new virtual interface and ip-address via ifconfig,
     * operation requires administrator privileges
     *
     * @return If operation was successful
     */
    boolean addRouteForCurrentService();

    /**
     * Propagate change of virtual ip-address owner via arping
     *
     * @return If operation was successful
     */
    boolean updateArpTableForCurrentService();
}
