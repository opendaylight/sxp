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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.binding.origins.BindingOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum BindingOriginsConfig {
    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(BindingOriginsConfig.class);

    public static final OriginType LOCAL_ORIGIN = OriginType.getDefaultInstance("LOCAL");
    public static final OriginType NETWORK_ORIGIN = OriginType.getDefaultInstance("NETWORK");
    public static final Map<OriginType, Integer> DEFAULT_ORIGIN_PRIORITIES;
    static {
        Map<OriginType, Integer> defaultPrios = new HashMap<>();
        defaultPrios.put(LOCAL_ORIGIN, 1);
        defaultPrios.put(NETWORK_ORIGIN, 2);
        DEFAULT_ORIGIN_PRIORITIES = Collections.unmodifiableMap(defaultPrios);
    }

    public static final String MISSING_REQUIRED_DEFAULTS = "Provided origins do not contain the required defaults.";
    public static final String DUPLICATE_ORIGIN_DEFINITIONS = "Provided origins have duplicate origin type definitions.";
    public static final String DUPLICATE_PRIORITY_DEFINITIONS = "Provided origins have duplicate priority definitions.";

    private final Map<OriginType, Integer> bindingOrigins = new ConcurrentHashMap<>();

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

    /**
     * Validates provided list of binding origins.
     * </p>
     * A valid list of binding origins must:
     * <ul>
     *     <li>1. contains default origins {@link BindingOriginsConfig#LOCAL_ORIGIN}
     *     and {@link BindingOriginsConfig#NETWORK_ORIGIN}</li>
     *     <li>2. must NOT contain any duplicate origin type definition</li>
     *     <li>3. must NOT contain any duplicate priority definition</li>
     * </ul>
     * </p>
     * If any of the condition is broken an {@link IllegalArgumentException} is thrown.
     *
     * @param origins List og binding origins to be validated
     */
    public static void validateBindingOrigins(List<BindingOrigin> origins) {
        LOG.debug("Validating binding origins {}", origins);
        final Set<OriginType> types = origins.stream().map(BindingOrigin::getOrigin).collect(Collectors.toSet());
        if (!types.contains(LOCAL_ORIGIN) || !types.contains(NETWORK_ORIGIN)) {
            LOG.error(MISSING_REQUIRED_DEFAULTS);
            throw new IllegalArgumentException(MISSING_REQUIRED_DEFAULTS);
        }
        // check for duplicate origin type definitions
        if (types.size() != origins.size()) {
            LOG.error(DUPLICATE_ORIGIN_DEFINITIONS);
            throw new IllegalArgumentException(DUPLICATE_ORIGIN_DEFINITIONS);
        }
        // check for duplicate priority definitions
        final Set<Integer> priorities = origins.stream().map(bindingOrigin -> bindingOrigin.getPriority().intValue())
                .collect(Collectors.toSet());
        if (priorities.size() != origins.size()) {
            LOG.error(DUPLICATE_PRIORITY_DEFINITIONS);
            throw new IllegalArgumentException(DUPLICATE_PRIORITY_DEFINITIONS);
        }
    }
}
