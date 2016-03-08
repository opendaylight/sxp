/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class MasterDatabase implements MasterDatabaseInf {

    protected static final Logger LOG = LoggerFactory.getLogger(MasterDatabase.class.getName());

    /**
     * Pre filter bindings before adding to MasterDatabase
     *
     * @param bindings List of bindings that will be filtered
     * @param function Function that will be used to find existing bindings in MasterDatabase
     * @param <T>      Any type extending SxpBindingFields
     * @return List of bindings that can be added to MasterDatabase
     */
    protected static <T extends SxpBindingFields> Map<IpPrefix, MasterDatabaseBinding> filterIncomingBindings(
            List<T> bindings, Function<IpPrefix, MasterDatabaseBinding> function) {
        Map<IpPrefix, MasterDatabaseBinding> prefixMap = new HashMap<>();
        if (function == null || bindings == null || bindings.isEmpty()) {
            return prefixMap;
        }
        bindings.stream().forEach(b -> {
            if (ignoreBinding(b))
                return;
            MasterDatabaseBinding
                    binding =
                    prefixMap.get(b.getIpPrefix()) == null ? function.apply(b.getIpPrefix()) : prefixMap.get(
                            b.getIpPrefix());
            if (binding == null || b.getPeerSequence().getPeer().size() < binding.getPeerSequence().getPeer().size()
                    || (b.getPeerSequence().getPeer().size() == binding.getPeerSequence().getPeer().size()
                    && TimeConv.toLong(b.getTimestamp()) > TimeConv.toLong(binding.getTimestamp()))) {
                prefixMap.put(b.getIpPrefix(), new MasterDatabaseBindingBuilder(b).build());
            }
        });
        return prefixMap;
    }

    /**
     * Checks if Binding has prefix 0:0:0:0:0:0:0:0/0 or 0.0.0.0/0,
     * if so then it will be ignored
     *
     * @param binding Binding to be checked
     * @return If binding will be ignored
     */
    private static <T extends SxpBindingFields> boolean ignoreBinding(T binding) {
        return binding.getIpPrefix().getIpv6Prefix() != null && "0:0:0:0:0:0:0:0/0".equals(
                binding.getIpPrefix().getIpv6Prefix().getValue()) || (binding.getIpPrefix().getIpv4Prefix() != null
                && "0.0.0.0/0".equals(binding.getIpPrefix().getIpv4Prefix().getValue()));
    }

    @Override public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName() + "\n");
        List<MasterDatabaseBinding> databaseBindings = getBindings();
        if (!databaseBindings.isEmpty()) {
            databaseBindings.stream()
                    .forEach(b -> builder.append("\t")
                            .append(b.getSecurityGroupTag().getValue())
                            .append(" ")
                            .append(b.getIpPrefix().getValue())
                            .append("\n"));
        }
        return builder.toString();
    }
}
