/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.capabilities.fields.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.capabilities.fields.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

public final class Configuration {
    public static final String TOPOLOGY_NAME = "sxp";
    public static final int DEFAULT_PREFIX_GROUP = 0;
    public static final int NETTY_CONNECT_TIMEOUT_MILLIS = 15000;
    public static final boolean NETTY_LOGGER_HANDLER = false;
    public static final boolean SET_COMPOSITION_ATTRIBUTE_COMPACT_NO_RESERVED_FIELDS = true;

    /**
     * Retrieve SXP peer {@link Capabilities} according to SXP {@link Version}.
     *
     * @param version Version according which Capabilities are generated
     * @return Capabilities supported by provided version of SXP peer
     */
    public static Capabilities getCapabilities(final Version version) {
        final CapabilitiesBuilder capabilitiesBuilder = new CapabilitiesBuilder();
        final List<CapabilityType> capabilities = new ArrayList<>();

        if (version.getIntValue() >= Version.Version1.getIntValue()) {
            capabilities.add(CapabilityType.Ipv4Unicast);
            if (version.getIntValue() >= Version.Version2.getIntValue()) {
                capabilities.add(CapabilityType.Ipv6Unicast);
                if (version.getIntValue() >= Version.Version3.getIntValue()) {
                    capabilities.add(CapabilityType.SubnetBindings);
                    if (version.getIntValue() >= Version.Version4.getIntValue()) {
                        capabilities.add(CapabilityType.LoopDetection);
                        capabilities.add(CapabilityType.SxpCapabilityExchange);
                    }
                }
            }
        }

        if (capabilities.isEmpty()) {
            capabilities.add(CapabilityType.None);
        }

        capabilitiesBuilder.setCapability(capabilities);
        return capabilitiesBuilder.build();
    }
}
