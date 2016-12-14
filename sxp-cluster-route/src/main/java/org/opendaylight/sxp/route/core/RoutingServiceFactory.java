/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import org.apache.commons.lang3.SystemUtils;
import org.opendaylight.sxp.route.spi.Routing;
import org.opendaylight.sxp.route.spi.SystemCall;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinition;

/**
 * Purpose: provides instance of {@link Routing} based on OS type
 */
public class RoutingServiceFactory {

    private SystemCall processFunction = s -> Runtime.getRuntime().exec(s);

    /**
     * @param initializer configured {@link RoutingDefinition}
     * @return OS dependent routing instance
     */
    public Routing instantiateRoutingService(RoutingDefinition initializer) {
        if (SystemUtils.IS_OS_LINUX) {
            return new LinuxRoutingService(processFunction, initializer);
        }
        throw new UnsupportedOperationException("OS is not supported by service");
    }

}
