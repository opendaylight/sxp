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
import java.util.Map;
import java.util.Map.Entry;
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
    public static final Map<OriginType, Integer> DEFAULT_ORIGIN_PRIORITIES =
            Collections.unmodifiableMap(createDefaultPriorities());

    private static Map<OriginType, Integer> createDefaultPriorities() {
        Map<OriginType, Integer> defaultPrios = new HashMap<>();
        defaultPrios.put(LOCAL_ORIGIN, 1);
        defaultPrios.put(NETWORK_ORIGIN, 2);
        return defaultPrios;
    }

    private final Map<OriginType, Integer> bindingOrigins = new ConcurrentHashMap<>();

    public Map<OriginType, Integer> getBindingOrigins() {
        return Collections.unmodifiableMap(bindingOrigins);
    }

    public synchronized boolean addBindingOrigin(OriginType origin, Integer priority) {
        if (bindingOrigins.containsKey(origin)) {
            LOG.warn("Binding origin: {} already exists.", origin.getValue());
            return false;
        }
        if (bindingOrigins.containsValue(priority)) {
            LOG.warn("Priority wanted to be used: {} is already used.", priority);
            return false;
        }

        bindingOrigins.put(origin, priority);
        return true;
    }

    public synchronized void addBindingOrigins(Iterable<BindingOrigin> origins) {
        origins.forEach(bindingOrigin -> addBindingOrigin(bindingOrigin.getOrigin(),
                bindingOrigin.getPriority().intValue()));
    }

    public synchronized void addBindingOrigins(Map<OriginType, Integer> originTypes) {
        for (Entry<OriginType, Integer> origin : originTypes.entrySet()) {
            addBindingOrigin(origin.getKey(), origin.getValue());
        }
    }

    public synchronized boolean updateBindingOrigin(OriginType origin, Integer priority) {
        if (!bindingOrigins.containsKey(origin)) {
            LOG.warn("Binding origin to be updated: {} not found.", origin.getValue());
            return false;
        }
        if (bindingOrigins.containsValue(priority)) {
            LOG.warn("Priority wanted to be used: {} is already used.", priority);
            return false;
        }

        bindingOrigins.put(origin, priority);
        return true;
    }

    public synchronized boolean deleteBindingOrigin(OriginType origin) {
        if (LOCAL_ORIGIN.equals(origin) || NETWORK_ORIGIN.equals(origin)) {
            LOG.warn("Binding origin default value: {} cannot be deleted.", origin.getValue());
            return false;
        }
        if (!bindingOrigins.containsKey(origin)) {
            LOG.warn("Binding origin to be deleted: {} not found.", origin.getValue());
            return false;
        }

        bindingOrigins.remove(origin);
        return true;
    }

    /**
     * Validates provided collection of binding origins.
     * <p>
     * A valid collection of binding origins:
     * <ul>
     *     <li>1. must contain default origins {@link BindingOriginsConfig#LOCAL_ORIGIN}
     *     and {@link BindingOriginsConfig#NETWORK_ORIGIN}</li>
     *     <li>2. must NOT contain any duplicate origin type definition</li>
     *     <li>3. must NOT contain any duplicate priority definition</li>
     * </ul>
     * <p>
     * If any of the conditions is broken an {@link IllegalArgumentException} is thrown.
     *
     * @param origins List of binding origins to be validated
     */
    public static void validateBindingOrigins(Collection<BindingOrigin> origins) {
        LOG.debug("Validating binding origins: {}", origins);
        final Set<OriginType> types = origins.stream().map(BindingOrigin::getOrigin).collect(Collectors.toSet());
        if (!types.contains(LOCAL_ORIGIN) || !types.contains(NETWORK_ORIGIN)) {
            LOG.error("Provided origins do not contain the required defaults.");
            throw new IllegalArgumentException("Provided origins do not contain the required defaults.");
        }
        // check for duplicate origin type definitions
        if (types.size() != origins.size()) {
            final String msg = "Provided origins have duplicate origin type definitions.";
            LOG.error(msg);
            throw new IllegalArgumentException(msg);
        }
        // check for duplicate priority definitions
        final Set<Integer> priorities = origins.stream().map(bindingOrigin -> bindingOrigin.getPriority().intValue())
                .collect(Collectors.toSet());
        if (priorities.size() != origins.size()) {
            final String msg = "Provided origins have duplicate priority definitions.";
            LOG.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public String toString() {
        return "BindingOriginsConfig{" +
                "bindingOrigins=" + bindingOrigins +
                '}';
    }
}
