/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.BindingDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class SxpDatabase implements SxpDatabaseInf {

    protected static final Logger LOG = LoggerFactory.getLogger(SxpDatabase.class.getName());

    protected abstract boolean putBindings(NodeId nodeId, BindingDatabase.BindingType bindingType,
            List<SxpDatabaseBinding> bindings);

    protected abstract List<SxpDatabaseBinding> getBindings(BindingDatabase.BindingType bindingType);

    protected abstract List<SxpDatabaseBinding> getBindings(BindingDatabase.BindingType bindingType, NodeId nodeId);

    protected abstract boolean deleteBindings(NodeId nodeId, BindingDatabase.BindingType bindingType);

    protected abstract List<SxpDatabaseBinding> deleteBindings(NodeId nodeId, Set<IpPrefix> bindings,
            BindingDatabase.BindingType bindingType);

    @Override public synchronized List<SxpDatabaseBinding> deleteBindings(NodeId nodeId) {
        if (nodeId == null)
            return new ArrayList<>();
        List<SxpDatabaseBinding>
                bindings =
                new ArrayList<>(getBindings(BindingDatabase.BindingType.ActiveBindings, nodeId));
        bindings.addAll(getBindings(BindingDatabase.BindingType.ReconciledBindings, nodeId));
        deleteBindings(nodeId, BindingDatabase.BindingType.ActiveBindings);
        deleteBindings(nodeId, BindingDatabase.BindingType.ReconciledBindings);
        return bindings;
    }

    @Override public synchronized <T extends SxpBindingFields> List<SxpDatabaseBinding> deleteBindings(NodeId nodeId,
            List<T> bindings) {
        if (nodeId == null || bindings == null || bindings.isEmpty())
            return new ArrayList<>();
        Set<IpPrefix> ipPrefices = bindings.stream().map(SxpBindingFields::getIpPrefix).collect(Collectors.toSet());
        List<SxpDatabaseBinding>
                databaseBindings =
                new ArrayList<>(deleteBindings(nodeId, ipPrefices, BindingDatabase.BindingType.ActiveBindings));
        databaseBindings.addAll(deleteBindings(nodeId, ipPrefices, BindingDatabase.BindingType.ReconciledBindings));
        return databaseBindings;
    }

    @Override public synchronized List<SxpDatabaseBinding> getBindings() {
        List<SxpDatabaseBinding> bindings = new ArrayList<>(getBindings(BindingDatabase.BindingType.ActiveBindings));
        bindings.addAll(getBindings(BindingDatabase.BindingType.ReconciledBindings));
        return bindings;
    }

    @Override public synchronized List<SxpDatabaseBinding> getBindings(NodeId nodeId) {
        List<SxpDatabaseBinding> bindings = new ArrayList<>();
        if (nodeId != null) {
            bindings.addAll(getBindings(BindingDatabase.BindingType.ReconciledBindings, nodeId));
            bindings.addAll(getBindings(BindingDatabase.BindingType.ActiveBindings, nodeId));
        }
        return bindings;
    }

    @Override public synchronized <T extends SxpBindingFields> List<SxpDatabaseBinding> addBinding(NodeId nodeId,
            List<T> bindings) {
        List<SxpDatabaseBinding> databaseBindings = new ArrayList<>();
        if (nodeId == null || bindings == null || bindings.isEmpty()) {
            return databaseBindings;
        }
        bindings.stream()
                .filter(t -> !ignoreBinding(t))
                .forEach(t -> databaseBindings.add(new SxpDatabaseBindingBuilder(t).build()));
        putBindings(nodeId, BindingDatabase.BindingType.ActiveBindings, databaseBindings);
        deleteBindings(nodeId, bindings.stream().map(SxpBindingFields::getIpPrefix).collect(Collectors.toSet()),
                BindingDatabase.BindingType.ReconciledBindings);
        return databaseBindings;
    }

    @Override public synchronized void setReconciliation(NodeId nodeId) {
        if (nodeId != null) {
            putBindings(nodeId, BindingDatabase.BindingType.ReconciledBindings,
                    getBindings(BindingDatabase.BindingType.ActiveBindings, nodeId));
            deleteBindings(nodeId, BindingDatabase.BindingType.ActiveBindings);
        }
    }

    @Override public synchronized List<SxpDatabaseBinding> reconcileBindings(NodeId nodeId) {
        if (nodeId != null) {
            List<SxpDatabaseBinding> bindings = getBindings(BindingDatabase.BindingType.ReconciledBindings, nodeId);
            deleteBindings(nodeId, BindingDatabase.BindingType.ReconciledBindings);
            return bindings;
        }
        return new ArrayList<>();
    }

    @Override public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName() + "\n");
        List<SxpDatabaseBinding> databaseBindings = getBindings();
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

    /**
     * Checks if Binding has prefix 0:0:0:0:0:0:0:0/0 or 0.0.0.0/0,
     * if so then it will be ignored
     *
     * @param binding Binding to be checked
     * @return If binding will be ignored
     */
    private static <T extends SxpBindingFields> boolean ignoreBinding(T binding) {
        if (binding == null)
            return true;
        return binding.getIpPrefix().getIpv6Prefix() != null && "0:0:0:0:0:0:0:0/0".equals(
                binding.getIpPrefix().getIpv6Prefix().getValue()) || (binding.getIpPrefix().getIpv4Prefix() != null
                && "0.0.0.0/0".equals(binding.getIpPrefix().getIpv4Prefix().getValue()));
    }

    /**
     * Create Map consisting of NodeIds of remoter peers associated with Inbound filters applied to them
     *
     * @param node SxpNode containing Connections and filters
     * @return Map of NodeId of remote peers and Inbound filters
     */
    public static Map<NodeId, SxpBindingFilter> getInboundFilters(SxpNode node) {
        Map<NodeId, SxpBindingFilter> map = new HashMap<>();
        node.getAllConnections().stream().forEach(c -> {
            if (c.isModeListener()) {
                map.put(c.getNodeIdRemote(), c.getFilter(FilterType.Inbound));
            }
        });
        return map;
    }

    /**
     * Finds replace for specified bindings from specified SxpNode
     *
     * @param bindings List of bindings that needs replace
     * @param <T>      Any type extending SxpBindingFields
     * @return List of replacements
     */
    public static <T extends SxpBindingFields> List<SxpDatabaseBinding> getReplaceForBindings(List<T> bindings,
            final SxpDatabaseInf database,final Map<NodeId, SxpBindingFilter> filters) {
        if (bindings == null || database == null || bindings.isEmpty())
            return new ArrayList<>();
        Set<IpPrefix>
                prefixesForReplace =
                bindings.stream().map(SxpBindingFields::getIpPrefix).collect(Collectors.toSet());
        Map<IpPrefix, SxpDatabaseBinding> prefixMap = new HashMap<>(bindings.size());
        synchronized (database) {
            for (Map.Entry<NodeId, SxpBindingFilter> entry : filters.entrySet()) {
                Preconditions.checkNotNull(database).getBindings(entry.getKey()).stream().forEach(b -> {
                    if (!prefixesForReplace.contains(b.getIpPrefix()) || entry.getValue() != null && entry.getValue()
                            .apply(b)) {
                        return;
                    }
                    SxpDatabaseBinding binding = prefixMap.get(b.getIpPrefix());
                    if (binding == null || b.getPeerSequence().getPeer().size() < binding.getPeerSequence()
                            .getPeer()
                            .size() || (
                            b.getPeerSequence().getPeer().size() == binding.getPeerSequence().getPeer().size()
                                    && TimeConv.toLong(b.getTimestamp()) > TimeConv.toLong(binding.getTimestamp()))) {
                        prefixMap.put(b.getIpPrefix(), b);
                    }
                });
            }
        }
        return new ArrayList<>(prefixMap.values());
    }

    /**
     * Filter SxpDatabase and return all bindings that do not match specified filter
     *
     * @param database SxpDatabase that will be filtered
     * @param nodeId   Peer specifying bindings  that will be filtered
     * @param filter   Filter according to which filtering will be held
     * @return List of bindings that do not match filter criteria
     */
    public static List<SxpDatabaseBinding> filterDatabase(SxpDatabaseInf database, NodeId nodeId,
            SxpBindingFilter filter) {
        if (nodeId == null || filter == null) {
            return new ArrayList<>();
        }
        List<SxpDatabaseBinding> active = database.getBindings(nodeId),
                filtered = new ArrayList<>();
        if (!active.isEmpty()) {
            filtered.addAll(Collections2.filter(active, filter::apply));
        }
        return filtered;
    }

}
