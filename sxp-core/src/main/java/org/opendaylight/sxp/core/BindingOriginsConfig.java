/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;

public enum BindingOriginsConfig {
    INSTANCE;

    private BindingOriginsConfig() {}

    public static final OriginType NETWORK_ORIGIN = OriginType.getDefaultInstance("NETWORK");
    public static final OriginType LOCAL_ORIGIN = OriginType.getDefaultInstance("LOCAL");

    public static final Map<OriginType, Integer> DEFAULT_ORIGIN_PRIORITIES = BindingOriginsConfig.initDefaultPrioritiesMap();
    private static Map<OriginType, Integer> initDefaultPrioritiesMap(){
        Map<OriginType, Integer> defaultPrios = new HashMap<>();
        defaultPrios.put(LOCAL_ORIGIN, 1);
        defaultPrios.put(NETWORK_ORIGIN, 2);
        return Collections.unmodifiableMap(defaultPrios);
    }

    private Map<OriginType, Integer> originPriorities;

    /**
     * Add initial priorities to map on start up.
     * @param priorities Initial priorities
     */
    public void addInitialProrities(Map<OriginType, Integer> priorities) {
        if (!priorities.containsKey(LOCAL_ORIGIN) || !priorities.containsKey(NETWORK_ORIGIN)) {
            throw new IllegalArgumentException("Provided origin types do not contain the required defaults.");
        }
        Collection<Integer> uniquePriorities = new HashSet<>(priorities.values());
        if (uniquePriorities.size() != priorities.size()) {
            throw new IllegalArgumentException("Provided origin types have conflicting priorities.");
        }
        this.originPriorities = new HashMap<>(priorities);
    }

    public Map<OriginType, Integer> getOriginPriorities() {
        return originPriorities;
    }

    public Integer putOriginType(OriginType originType, Integer priority) {
        return originPriorities.put(originType, priority);
    }
}
