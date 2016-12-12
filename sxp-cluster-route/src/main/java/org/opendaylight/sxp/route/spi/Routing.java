/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.spi;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

public interface Routing {

    String getInterface();

    Routing setInterface(String inf);

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

    boolean isActive();

    boolean removeRouteForCurrentService();

    boolean addRouteForCurrentService();
}
