/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinitionBuilder;

/**
 * Purpose: house of dummy route creator
 */
public class RouteTestFactory {

    /**
     * @param idSeed    magic1 (part of key)
     * @param valueSeed magic2
     * @return dummy routing definition
     */
    static RoutingDefinition createDummyRoutingDef(final int idSeed, final int valueSeed) {
        final IpAddress virtualIp = IpAddressBuilder.getDefaultInstance("1.2.3." + idSeed);
        final IpAddress netmask = IpAddressBuilder.getDefaultInstance("255.255.255." + valueSeed);

        return new RoutingDefinitionBuilder().setInterface("eth" + valueSeed)
                .setIpAddress(virtualIp)
                .setNetmask(netmask)
                .build();
    }
}
