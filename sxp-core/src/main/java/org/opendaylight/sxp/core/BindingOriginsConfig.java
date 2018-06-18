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

    public static final OriginType LOCAL_ORIGIN = OriginType.getDefaultInstance("LOCAL");
    public static final OriginType NETWORK_ORIGIN = OriginType.getDefaultInstance("NETWORK");
    public static final Map<OriginType, Short> DEFAULT_ORIGIN_PRIORITIES;

    public static final String BINDING_ORIGIN_ALREADY_EXIST = "Binding origin: {} already exist";
    public static final String PRIORITY_WANTED_TO_BE_USED_IS_ALREADY_USED = "Priority wanted to be used: {} is already used";
    public static final String BINDING_ORIGIN_TO_BE_UPDATED_NOT_FOUND = "Binding origin to be updated: {} not found";
    public static final String BINDING_ORIGIN_DEFAULT_VALUE_CANNOT_BE_DELETED = "Binding origin default value: {} cannot be deleted";
    public static final String BINDING_ORIGIN_TO_BE_DELETED_NOT_FOUND = "Binding origin to be deleted: {} not found";
    public static final String PROVIDED_ORIGIN_TYPES_DO_NOT_CONTAIN_THE_REQUIRED_DEFAULTS = "Provided origin types do not contain the required defaults.";
    public static final String PROVIDED_ORIGIN_TYPES_HAVE_CONFLICTING_PRIORITIES = "Provided origin types have conflicting priorities.";

    static {
        Map<OriginType, Short> defaultPrios = new HashMap<>();
        defaultPrios.put(LOCAL_ORIGIN, (short) 1);
        defaultPrios.put(NETWORK_ORIGIN, (short) 2);
        DEFAULT_ORIGIN_PRIORITIES = Collections.unmodifiableMap(defaultPrios);
    }

    private static final Logger LOG = LoggerFactory.getLogger(BindingOriginsConfig.class.getName());
    private final Map<OriginType, Short> bindingOrigins = Collections.synchronizedMap(new HashMap<>());

    public boolean addBindingOrigin(OriginType origin, Short priority) {
        if (bindingOrigins.containsKey(origin)) {
            LOG.warn(BINDING_ORIGIN_ALREADY_EXIST, origin.getValue());
            return false;
        }
        if (bindingOrigins.containsValue(priority)) {
            LOG.warn(PRIORITY_WANTED_TO_BE_USED_IS_ALREADY_USED, priority);
            return false;
        }

        bindingOrigins.put(origin, priority);
        return true;
    }

    public void addBindingOrigins(List<BindingOrigin> origins) {
        origins.forEach(bindingOrigin -> addBindingOrigin(bindingOrigin.getOrigin(), bindingOrigin.getPriority()));
    }

    public boolean updateBindingOrigin(OriginType origin, Short priority) {
        if (!bindingOrigins.containsKey(origin)) {
            LOG.warn(BINDING_ORIGIN_TO_BE_UPDATED_NOT_FOUND, origin.getValue());
            return false;
        }
        if (bindingOrigins.containsValue(priority)) {
            LOG.warn(PRIORITY_WANTED_TO_BE_USED_IS_ALREADY_USED, priority);
            return false;
        }

        bindingOrigins.put(origin, priority);
        return true;
    }

    public boolean deleteBindingOrigin(OriginType origin) {
        if (LOCAL_ORIGIN.equals(origin) || NETWORK_ORIGIN.equals(origin)) {
            LOG.warn(BINDING_ORIGIN_DEFAULT_VALUE_CANNOT_BE_DELETED, origin.getValue());
            return false;
        }
        if (!bindingOrigins.containsKey(origin)) {
            LOG.warn(BINDING_ORIGIN_TO_BE_DELETED_NOT_FOUND, origin.getValue());
            return false;
        }

        bindingOrigins.remove(origin);
        return true;
    }

    public void validateOriginBindings(List<BindingOrigin> origins) {
        final Set<OriginType> types = origins.stream().map(BindingOrigin::getOrigin).collect(Collectors.toSet());
        if (!types.contains(LOCAL_ORIGIN) || !types.contains(NETWORK_ORIGIN)) {
            LOG.error(PROVIDED_ORIGIN_TYPES_DO_NOT_CONTAIN_THE_REQUIRED_DEFAULTS);
            throw new IllegalStateException(PROVIDED_ORIGIN_TYPES_DO_NOT_CONTAIN_THE_REQUIRED_DEFAULTS);
        }
        // check if some priority value is not used for more origins
        if (types.size() != origins.size()) {
            LOG.error(PROVIDED_ORIGIN_TYPES_HAVE_CONFLICTING_PRIORITIES);
            throw new IllegalStateException(PROVIDED_ORIGIN_TYPES_HAVE_CONFLICTING_PRIORITIES);
        }
    }
}
