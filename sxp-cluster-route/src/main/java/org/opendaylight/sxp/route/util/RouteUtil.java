/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.route.spi.Routing;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinitionBuilder;

/**
 * Purpose: utility for routing tasks
 */
public class RouteUtil {

    /**
     * Address to string with null check
     *
     * @param address any address
     * @return string form of given address
     */
    public static String addressToString(IpAddress address) {
        return String.valueOf(address.getValue());
    }

    /**
     * Filter out all sxp nodes which involve given virtual interface
     *
     * @param virtualIp virtual interface
     * @param sxpNodes  all available sxp nodes
     * @return sxp nodes which use given virtualIp iface
     */
    public static Collection<SxpNode> findSxpNodesOnVirtualIp(final IpAddress virtualIp,
            final Collection<SxpNode> sxpNodes) {
        final Collection<SxpNode> affectedNodes;
        if (Objects.isNull(virtualIp)) {
            affectedNodes = Collections.emptyList();
        } else {
            affectedNodes =
                    sxpNodes.stream()
                            .filter(node -> Objects.nonNull(node) && Objects.nonNull(node.getSourceIp())
                                    && addressToString(virtualIp).equals(node.getSourceIp().getHostAddress()))
                            .collect(Collectors.toList());
        }
        return affectedNodes;
    }

    /**
     * Create copy of given routing definition and add timestamp, consistency and detailed info
     *
     * @param originalDefinition source
     * @param consistent         true if routing is consistent with system
     * @param info               explanation / cause of state
     * @return rich routing definition
     */
    public static RoutingDefinition createOperationalRouteDefinition(
            @Nonnull final RoutingDefinition originalDefinition, final boolean consistent, final String info) {
        return new RoutingDefinitionBuilder(originalDefinition).setTimestamp(TimeConv.toDt(System.currentTimeMillis()))
                .setConsistent(consistent)
                .setInfo(info)
                .build();
    }

    /**
     * Extract all parts of routing definition contained in routing service
     *
     * @param existingRouting routing service containing all essential parts of routing definition
     * @return extracted definition
     */
    public static RoutingDefinition extractRoutingDefinition(final Routing existingRouting) {
        return new RoutingDefinitionBuilder().setIpAddress(existingRouting.getVirtualIp())
                .setNetmask(existingRouting.getNetmask())
                .setInterface(existingRouting.getInterface())
                .build();
    }
}
