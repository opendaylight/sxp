/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.database;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.opendaylight.sxp.core.BindingOriginsConfig;
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
    protected static final Logger LOG = LoggerFactory.getLogger(MasterDatabase.class);

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
            List<T> bindings, Function<IpPrefix, MasterDatabaseBinding> get, Function<IpPrefix, Boolean> remove) {
        Map<IpPrefix, MasterDatabaseBinding> prefixMap = new HashMap<>();

        if (bindings == null || get == null || remove == null) {
            return prefixMap;
        }

        bindings.forEach(incoming -> {
            if (ignoreBinding(incoming)) {
                return;
            }

            final MasterDatabaseBinding binding = prefixMap.containsKey(incoming.getIpPrefix())
                    ? prefixMap.get(incoming.getIpPrefix()) : get.apply(incoming.getIpPrefix());

            if (binding == null || bindingShouldBeReplaced(binding, incoming)) {
                prefixMap.put(incoming.getIpPrefix(), new MasterDatabaseBindingBuilder(incoming).build());
                remove.apply(incoming.getIpPrefix());
            }
        });

        return prefixMap;
    }

    /**
     * Decide if existing (first coming) binding should be replaced by incoming binding.
     * <p>
     * The binding with the highest priority is always preferred. If priorities of bindings
     * are the same then the binding with shorter peer sequence is preferred. If also peer sequences
     * are the same length then the latest binding according to creation time is preferred.
     *
     * @param binding Existing binding
     * @param incoming Incoming binding
     * @return {@code true} if existing binding should be replaced, {@code false} otherwise
     */
    private static <T extends SxpBindingFields> boolean bindingShouldBeReplaced(
            MasterDatabaseBinding binding, T incoming) {
        final Map<OriginType, Integer> bindingOrigins = BindingOriginsConfig.INSTANCE.getBindingOrigins();
        final Integer incomingPriority = bindingOrigins.get(incoming.getOrigin());
        // incoming binding has unknown priority
        if (incomingPriority == null) {
            throw new IllegalStateException("Cannot find binding priority: " + incoming.getOrigin().getValue());
        }
        final Integer priority = bindingOrigins.get(binding.getOrigin());
        // priority of previous binding was deleted, thus incoming has greater priority
        if (priority == null) {
            return true;
        }

        // first decide to replace binding according to priority
        // lower number = higher priority
        if (incomingPriority > priority) {
            return false;
        }
        if (incomingPriority < priority) {
            return true;
        }

        // if priorities are the same, decide according to peer sequence length and binding creation time
        if (getPeerSequenceLength(incoming) < getPeerSequenceLength(binding)
                || ((getPeerSequenceLength(incoming) == getPeerSequenceLength(binding)
                && TimeConv.toLong(incoming.getTimestamp()) > TimeConv.toLong(binding.getTimestamp())))) {
            return true;
        }

        return false;
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
