/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.database;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of a MasterDatabse
 */
public abstract class MasterDatabase implements MasterDatabaseInf {

    public static final Map<OriginType, Integer> DEFAULT_ORIGIN_PRIORITIES = initDefaultPrioritiesMap();
    private static Map<OriginType, Integer> initDefaultPrioritiesMap(){
        Map<OriginType, Integer> defaultPrios = new HashMap<>();
        defaultPrios.put(LOCAL_ORIGIN, 1);
        defaultPrios.put(NETWORK_ORIGIN, 2);
        return Collections.unmodifiableMap(defaultPrios);
    }

    private static final Logger LOG = LoggerFactory.getLogger(MasterDatabase.class.getName());
    private static final Map<OriginType, Integer> originPriorities = new HashMap<>();

    public static Map<OriginType, Integer> getOriginPriorities() {
        return originPriorities;
    }

    public static Integer putOriginType(final OriginType originType, final Integer priority) {
        // todo checks and validation?
        return MasterDatabase.originPriorities.put(originType, priority);
    }

    public MasterDatabase(Map<OriginType, Integer> originPriorities) {
        if (!originPriorities.containsKey(NETWORK_ORIGIN) || !originPriorities.containsKey(LOCAL_ORIGIN)) {
            throw new IllegalArgumentException("Provided origin types do not contain the required defaults.");
        }
        Collection<Integer> uniquePriorities = new HashSet<>(originPriorities.values());
        if (uniquePriorities.size() != originPriorities.size()) {
            throw new IllegalArgumentException("Provided origin types have conflicting priorities.");
        }
        MasterDatabase.originPriorities.putAll(originPriorities);
    }

    /**
     * Pre filter bindings before adding to MasterDatabase
     *
     * @param bindings List of bindings that will be filtered
     * @param get      Function that will be used to find existing bindings in MasterDatabase
     * @param remove   Function that will be used to remove existing bindings from MasterDatabase
     * @param <T>      Any type extending SxpBindingFields
     * @return List of bindings that can be added to MasterDatabase
     */
    protected static <T extends SxpBindingFields> Map<IpPrefix, MasterDatabaseBinding> filterIncomingBindings(
            List<T> bindings, Function<IpPrefix, MasterDatabaseBinding> get, Function<IpPrefix, Boolean> remove,
            OriginType bindingType) {
        Map<IpPrefix, MasterDatabaseBinding> prefixMap = new HashMap<>();
        if (get == null || remove == null || bindings == null || bindings.isEmpty()) {
            return prefixMap;
        }
        bindings.forEach(b -> {
            if (ignoreBinding(b)) {
                return;
            }
            MasterDatabaseBinding
                    binding =
                    !prefixMap.containsKey(b.getIpPrefix()) ? get.apply(b.getIpPrefix()) : prefixMap.get(
                            b.getIpPrefix());
            if (binding == null || getPeerSequenceLength(b) < getPeerSequenceLength(binding) || (
                    getPeerSequenceLength(b) == getPeerSequenceLength(binding)
                            && TimeConv.toLong(b.getTimestamp()) > TimeConv.toLong(binding.getTimestamp()))) {
                prefixMap.put(b.getIpPrefix(), new MasterDatabaseBindingBuilder(b)
                        .setOrigin(bindingType)
                        .build());
                remove.apply(b.getIpPrefix());
            }
        });
        return prefixMap;
    }

    /**
     * Get the length of the peer sequence.
     */
    public static <T extends SxpBindingFields> int getPeerSequenceLength(T b) {
        return b == null || b.getPeerSequence() == null
                || b.getPeerSequence().getPeer() == null ? 0 : b.getPeerSequence().getPeer().size();
    }

    /**
     * Checks if Binding has prefix 0:0:0:0:0:0:0:0/0 or 0.0.0.0/0,
     * if so then it will be ignored
     *
     * @param binding Binding to be checked
     * @return If binding will be ignored
     */
    @SuppressWarnings("all")
    protected static <T extends SxpBindingFields> boolean ignoreBinding(T binding) {
        if (binding == null) {
            return true;
        }
        return binding.getIpPrefix().getIpv6Prefix() != null && "0:0:0:0:0:0:0:0/0".equals(
                binding.getIpPrefix().getIpv6Prefix().getValue()) || (binding.getIpPrefix().getIpv4Prefix() != null
                && "0.0.0.0/0".equals(binding.getIpPrefix().getIpv4Prefix().getValue()));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName() + "\n");
        Collection<MasterDatabaseBinding> databaseBindings = getBindings();
        if (!databaseBindings.isEmpty()) {
            databaseBindings.forEach(b -> builder.append("\t")
                    .append(b.getSecurityGroupTag().getValue())
                    .append(" ")
                    .append(b.getIpPrefix().getValue())
                    .append("\n"));
        }
        return builder.toString();
    }
}
