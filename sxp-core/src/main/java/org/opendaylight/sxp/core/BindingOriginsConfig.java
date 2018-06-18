/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.binding.origins.BindingOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum BindingOriginsConfig {
    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(BindingOriginsConfig.class.getName());

    public static final OriginType LOCAL_ORIGIN = OriginType.getDefaultInstance("LOCAL");
    public static final OriginType NETWORK_ORIGIN = OriginType.getDefaultInstance("NETWORK");
    public static final Map<OriginType, Integer> DEFAULT_ORIGIN_PRIORITIES;
    static {
        Map<OriginType, Integer> defaultPrios = new HashMap<>();
        defaultPrios.put(LOCAL_ORIGIN, 1);
        defaultPrios.put(NETWORK_ORIGIN, 2);
        DEFAULT_ORIGIN_PRIORITIES = Collections.unmodifiableMap(defaultPrios);
    }

    private final Map<OriginType, Integer> bindingOrigins = Collections.synchronizedMap(new HashMap<>());

    public boolean addBindingOrigin(OriginType origin, Integer priority) {
        if (bindingOrigins.containsKey(origin)) {
            return false;
        }
        if (bindingOrigins.containsValue(priority)) {
            return false;
        }

        bindingOrigins.put(origin, priority);
        return true;
    }

    public void addBindingOrigins(List<BindingOrigin> origins) {
        origins.forEach(bindingOrigin -> addBindingOrigin(bindingOrigin.getOrigin(),
                bindingOrigin.getPriority().intValue()));
    }

    public void validateOriginBindings(List<BindingOrigin> origins) {
        final Set<OriginType> types = origins.stream().map(BindingOrigin::getOrigin).collect(Collectors.toSet());
        if (!types.contains(LOCAL_ORIGIN) || !types.contains(NETWORK_ORIGIN)) {
            final String msg = "Provided origin types do not contain the required defaults.";
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }
        // check if some priority value is not used for more origins
        if (types.size() != origins.size()) {
            final String msg = "Provided origin types have conflicting priorities.";
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }
    }
}
