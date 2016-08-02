/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.SxpDatabase;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpDomainFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.fields.Domains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.fields.domains.Domain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionTemplateFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.sxp.util.ArraysUtil.getBitAddress;

/**
 * SxpDomain class represent layer that isolate Connections
 * and Bindings learned from them to only Peers of the same domain.
 * Sharing of Bindings can be achieved by filters between domains.
 */
public class SxpDomain implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SxpDomain.class.getName());

    private final MasterDatabaseInf masterDatabase;
    private final SxpDatabaseInf sxpDatabase;
    private final String name;
    private final SxpNode node;
    private final Map<FilterSpecific, Map<String, SxpBindingFilter<?, ? extends SxpDomainFilterFields>>>
            filters =
            new HashMap<>(FilterSpecific.values().length);
    private final Map<InetAddress, SxpConnection> connections = new HashMap<>();
    private final Map<IpPrefix, SxpConnectionTemplateFields> templates = new HashMap<>();

    /**
     * @param name           Name of Domain
     * @param sxpDatabase    SxpDatabase that will be use to store incoming Bindings
     * @param masterDatabase MasterDatabase that will be used to store filtered Bindings
     */
    protected SxpDomain(SxpNode owner, String name, SxpDatabaseInf sxpDatabase, MasterDatabaseInf masterDatabase) {
        for (FilterSpecific filterSpecific : FilterSpecific.values()) {
            filters.put(filterSpecific, new HashMap<>());
        }
        this.masterDatabase = Preconditions.checkNotNull(masterDatabase);
        this.sxpDatabase = Preconditions.checkNotNull(sxpDatabase);
        this.name = Preconditions.checkNotNull(name);
        this.node = owner;
    }

    /**
     * @param owner          SxpNode to which Domain belongs
     * @param name           Name of Domains
     * @param sxpDatabase    SxpDatabase used in this domain
     * @param masterDatabase MasterDatabase used in this domain
     * @return Domain consisting of provided values
     */
    public static SxpDomain createInstance(SxpNode owner, String name, SxpDatabaseInf sxpDatabase,
            MasterDatabaseInf masterDatabase) {
        return new SxpDomain(owner, name, sxpDatabase, masterDatabase);
    }

    /**
     * @param owner  SxpNode to which Domain belongs
     * @param domain SxpDomain initializer
     * @return Domain consisting of provided values
     */
    public static SxpDomain createInstance(SxpNode owner,
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain domain) {
        SxpDomain
                sxpDomain =
                createInstance(owner, Preconditions.checkNotNull(domain).getDomainName(), new SxpDatabaseImpl(),
                        new MasterDatabaseImpl());
        if (domain.getMasterDatabase() != null) {
            sxpDomain.getMasterDatabase().addBindings(domain.getMasterDatabase().getMasterDatabaseBinding());
        }
        if (domain.getConnections() != null && domain.getConnections().getConnection() != null) {
            domain.getConnections()
                    .getConnection()
                    .forEach(c -> sxpDomain.putConnection(new SxpConnection(owner, c, sxpDomain.getName())));
        }
        if (domain.getDomainFilters() != null && domain.getDomainFilters().getDomainFilter() != null) {
            for (DomainFilter filter : domain.getDomainFilters().getDomainFilter()) {
                if (!sxpDomain.addFilter(SxpBindingFilter.generateFilter(filter, sxpDomain.getName())))
                    throw new IllegalArgumentException("Cannot create domain Filters contains invalid values");
            }
        }
        return sxpDomain;
    }

    /**
     * Gets all filters,these filters may be merged and their entries thus wont be accessible
     *
     * @return Filters contained in current domains
     */
    public Map<String, SxpBindingFilter<?, ? extends SxpDomainFilterFields>> getFilters() {
        Map<String, List<SxpBindingFilter<?, ? extends SxpDomainFilterFields>>> filterMap = new HashMap<>();
        synchronized (filters) {
            filters.values()
                    .forEach(m -> m.values().forEach(f -> f.getSxpFilter().getDomains().getDomain().forEach(d -> {
                        if (!filterMap.containsKey(d.getName())) {
                            filterMap.put(d.getName(), new ArrayList<>());
                        }
                        filterMap.get(d.getName()).add(f);
                    })));
        }
        return Collections.unmodifiableMap(filterMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        (Function<Map.Entry<String, List<SxpBindingFilter<?, ? extends SxpDomainFilterFields>>>, SxpBindingFilter<?, ? extends SxpDomainFilterFields>>) stringListEntry -> SxpBindingFilter
                                .mergeFilters(stringListEntry.getValue()))));
    }

    /**
     * Adds filter into Domain and propagate changes to other domains
     *
     * @param filter Domain filter that will be added
     * @return If Filter was added
     */
    public boolean addFilter(SxpBindingFilter<?, ? extends SxpDomainFilterFields> filter) {
        synchronized (filters) {
            if (filter == null || filters.get(filter.getSxpFilter().getFilterSpecific())
                    .containsKey(filter.getSxpFilter().getFilterName()) || filter.getSxpFilter() == null
                    || filter.getSxpFilter().getDomains() == null
                    || filter.getSxpFilter().getDomains().getDomain() == null || filter.getSxpFilter()
                    .getDomains()
                    .getDomain()
                    .isEmpty() || filter.getSxpFilter()
                    .getDomains()
                    .getDomain()
                    .stream()
                    .anyMatch(d -> d.getName().equals(getName())) || filter.getSxpFilter().getFilterEntries() == null
                    || checkDomainOverlap(filter.getSxpFilter().getFilterSpecific(),
                    filter.getSxpFilter().getDomains()))
                return false;
            filters.get(filter.getSxpFilter().getFilterSpecific()).put(filter.getSxpFilter().getFilterName(), filter);
        }
        node.getDomains()
                .stream()
                .filter(d -> !d.getName().equals(getName()) && filter.getSxpFilter()
                        .getDomains()
                        .getDomain()
                        .stream()
                        .anyMatch(filter_d -> filter_d.getName().equals(d.getName())))
                .forEach(domain -> {
                    propagateToSharedMasterDatabases(Collections.emptyList(),
                            getMasterDatabase().getLocalBindings().stream().filter(filter).collect(Collectors.toList()),
                            domain);
                    getConnections().stream().filter(SxpConnection::isModeListener).forEach(c -> {
                        propagateToSharedSxpDatabases(Collections.emptyList(),
                                getSxpDatabase().getBindings(c.getNodeIdRemote())
                                        .stream()
                                        .filter(filter)
                                        .collect(Collectors.toList()), domain, c.getNodeIdRemote(),
                                c.getFilter(FilterType.Inbound));
                    });
                });
        return true;
    }

    /**
     * Overlap filter are filters that has the same FilterSpecific and is operating on same domain
     *
     * @param filterSpecific Filter type specification
     * @param domains        Domains that will be checked on overlap
     * @return If Overlapping filters are in present
     */
    private boolean checkDomainOverlap(FilterSpecific filterSpecific, Domains domains) {
        return domains.getDomain()
                .stream()
                .map(Domain::getName)
                .anyMatch(d -> filters.get(filterSpecific)
                        .values()
                        .stream()
                        .map(f -> f.getSxpFilter().getDomains().getDomain())
                        .collect(Collectors.toList())
                        .stream()
                        .anyMatch(dom -> dom.stream().anyMatch(domain -> domain.getName().equals(d))));
    }

    /**
     * Removed Domain filter from current domain
     *
     * @param specific Filter type specification
     * @param name     Name of filter that will be removed
     * @return Removed filter
     */
    public SxpBindingFilter<?, ? extends SxpDomainFilterFields> removeFilter(FilterSpecific specific, String name) {
        SxpBindingFilter<?, ? extends SxpDomainFilterFields> bindingFilter;
        synchronized (filters) {
            bindingFilter = filters.get(Preconditions.checkNotNull(specific)).remove(Preconditions.checkNotNull(name));
        }
        if (bindingFilter != null) {
            node.getDomains()
                    .stream()
                    .filter(d -> !d.getName().equals(getName()) && bindingFilter.getSxpFilter()
                            .getDomains()
                            .getDomain()
                            .stream()
                            .anyMatch(filter_d -> filter_d.getName().equals(d.getName())))
                    .forEach(domain -> {
                        propagateToSharedMasterDatabases(getMasterDatabase().getLocalBindings()
                                .stream()
                                .filter(bindingFilter)
                                .collect(Collectors.toList()), Collections.emptyList(), domain);
                        getConnections().stream().filter(SxpConnection::isModeListener).forEach(c -> {
                            propagateToSharedSxpDatabases(getSxpDatabase().getBindings(c.getNodeIdRemote())
                                            .stream()
                                            .filter(bindingFilter)
                                            .collect(Collectors.toList()), Collections.emptyList(), domain, c.getNodeIdRemote(),
                                    c.getFilter(FilterType.Inbound));
                        });
                    });
        }
        return bindingFilter;
    }

    /**
     * Update Filter entries
     *
     * @param newFilter New filter that will replace old one
     * @return If filter update was successful
     */
    public boolean updateFilter(SxpBindingFilter<?, ? extends SxpDomainFilterFields> newFilter) {
        SxpBindingFilter<?, ? extends SxpDomainFilterFields> oldFilter;
        synchronized (filters) {
            oldFilter =
                    filters.get(newFilter.getSxpFilter().getFilterSpecific())
                            .put(newFilter.getSxpFilter().getFilterName(), newFilter);
        }
        if (oldFilter == null)
            return false;
        List<SxpDomain>
                domains =
                node.getDomains().stream().filter(d -> !d.getName().equals(getName())).collect(Collectors.toList());
        //Domains only in old filter
        domains.stream()
                .filter(d -> oldFilter.getSxpFilter()
                        .getDomains()
                        .getDomain()
                        .stream()
                        .anyMatch(filter_d -> filter_d.getName().equals(d.getName())) && !newFilter.getSxpFilter()
                        .getDomains()
                        .getDomain()
                        .stream()
                        .anyMatch(filter_d -> filter_d.getName().equals(d.getName())))
                .forEach(domain -> {
                    propagateToSharedMasterDatabases(getMasterDatabase().getLocalBindings()
                            .stream()
                            .filter(oldFilter)
                            .collect(Collectors.toList()), Collections.emptyList(), domain);
                    getConnections().stream().filter(SxpConnection::isModeListener).forEach(c -> {
                        propagateToSharedSxpDatabases(getSxpDatabase().getBindings(c.getNodeIdRemote())
                                        .stream()
                                        .filter(oldFilter)
                                        .collect(Collectors.toList()), Collections.emptyList(), domain, c.getNodeIdRemote(),
                                c.getFilter(FilterType.Inbound));
                    });
                });
        //domains in both old and new filter
        domains.stream()
                .filter(d -> oldFilter.getSxpFilter()
                        .getDomains()
                        .getDomain()
                        .stream()
                        .anyMatch(filter_d -> filter_d.getName().equals(d.getName())) && newFilter.getSxpFilter()
                        .getDomains()
                        .getDomain()
                        .stream()
                        .anyMatch(filter_d -> filter_d.getName().equals(d.getName())))
                .forEach(domain -> {
                    propagateToSharedMasterDatabases(getMasterDatabase().getLocalBindings()
                            .stream()
                            .filter(b -> oldFilter.test(b) && !newFilter.test(b))
                            .collect(Collectors.toList()), getMasterDatabase().getLocalBindings()
                            .stream()
                            .filter(b -> !oldFilter.test(b) && newFilter.test(b))
                            .collect(Collectors.toList()), domain);
                    getConnections().stream().filter(SxpConnection::isModeListener).forEach(c -> {
                        propagateToSharedSxpDatabases(getSxpDatabase().getBindings(c.getNodeIdRemote())
                                        .stream()
                                        .filter(b -> oldFilter.test(b) && !newFilter.test(b))
                                        .collect(Collectors.toList()), getSxpDatabase().getBindings(c.getNodeIdRemote())
                                        .stream()
                                        .filter(b -> !oldFilter.test(b) && newFilter.test(b))
                                        .collect(Collectors.toList()), domain, c.getNodeIdRemote(),
                                c.getFilter(FilterType.Inbound));
                    });
                });
        //domains only in new filter
        domains.stream()
                .filter(d -> !oldFilter.getSxpFilter()
                        .getDomains()
                        .getDomain()
                        .stream()
                        .anyMatch(filter_d -> filter_d.getName().equals(d.getName())) && newFilter.getSxpFilter()
                        .getDomains()
                        .getDomain()
                        .stream()
                        .anyMatch(filter_d -> filter_d.getName().equals(d.getName())))
                .forEach(domain -> {
                    propagateToSharedMasterDatabases(getMasterDatabase().getLocalBindings()
                            .stream()
                            .filter(oldFilter)
                            .collect(Collectors.toList()), Collections.emptyList(), domain);
                    getConnections().stream().filter(SxpConnection::isModeListener).forEach(c -> {
                        propagateToSharedSxpDatabases(getSxpDatabase().getBindings(c.getNodeIdRemote())
                                        .stream()
                                        .filter(oldFilter)
                                        .collect(Collectors.toList()), Collections.emptyList(), domain, c.getNodeIdRemote(),
                                c.getFilter(FilterType.Inbound));
                    });
                });
        return true;
    }

    /**
     * Routine that send Bindings changes to other domains and find replacement for deleted bindings
     *
     * @param remove Bindings that will be removed
     * @param add    Bindings that will be added
     * @param domain Destination domain
     */
    private <T extends SxpBindingFields> void propagateToSharedMasterDatabases(final List<T> remove, final List<T> add,
            final SxpDomain domain) {
        if (domain == null || this.name.equals(domain.getName()) || (remove.isEmpty() && add.isEmpty()))
            return;
        List<SxpBindingFields> added = new ArrayList<>(), deleted = new ArrayList<>(), replace = new ArrayList<>(add);
        Map<NodeId, SxpBindingFilter> filterMap = SxpDatabase.getInboundFilters(node, domain.getName());
        synchronized (domain) {
            deleted.addAll(domain.getMasterDatabase().deleteBindings(remove));
            if (!deleted.isEmpty()) {
                replace.addAll(SxpDatabase.getReplaceForBindings(deleted, domain.getSxpDatabase(), filterMap));
                //Fix for specific cases where local bindings are overwritten by shared local bindings
                replace.addAll(domain.getMasterDatabase().getLocalBindings());
            }
            added.addAll(domain.getMasterDatabase().addBindings(replace));
            if (!added.isEmpty() || !deleted.isEmpty())
                node.getSvcBindingDispatcher()
                        .propagateUpdate(deleted, added, domain.getConnections()
                                .stream()
                                .filter(c -> c.isModeSpeaker() && c.isStateOn())
                                .collect(Collectors.toList()));
        }
    }

    /**
     * @param remove       Bindings that were removed
     * @param add          Bindings that will be added
     * @param domain       Domain to which data wil be propagated
     * @param nodeIdRemote NodeId from which Bindings came from
     * @param filter       Inbound SxpFilter applied to transfer
     * @param <T>          Unified type for Bindings
     */
    private <T extends SxpBindingFields> void propagateToSharedSxpDatabases(List<T> remove, List<T> add,
            SxpDomain domain, NodeId nodeIdRemote, SxpBindingFilter<?, ? extends SxpFilterFields> filter) {
        if (nodeIdRemote == null || domain == null || this.name.equals(domain.getName()) || (remove.isEmpty()
                && add.isEmpty()))
            return;
        synchronized (domain) {
            List<SxpDatabaseBinding> deleted = domain.getSxpDatabase().deleteBindings(nodeIdRemote, remove),
                    added = domain.getSxpDatabase().addBinding(nodeIdRemote, add);
            if (!added.isEmpty() || !deleted.isEmpty())
                propagateToSharedMasterDatabases(
                        filter == null ? deleted : deleted.stream().filter(filter).collect(Collectors.toList()),
                        filter == null ? added : added.stream().filter(filter).collect(Collectors.toList()), domain);
        }
    }

    /**
     * Add binding changes to all other domains Master-DBs and send changes to remote peers of Domains.
     * This data that will be proceed to domains only if filters assigned to current Domain allows it.
     *
     * @param removed Bindings that were removed
     * @param added   Bindings that will be added
     * @param <T>     Unified type for Bindings
     */
    public <T extends SxpBindingFields> void pushToSharedMasterDatabases(final List<T> removed, final List<T> added) {
        if (removed == null || added == null || (added.isEmpty() && removed.isEmpty()))
            return;
        Map<String, SxpDomain>
                sxpDomains =
                node.getDomains()
                        .stream()
                        .filter(d -> !d.getName().equals(getName()))
                        .collect(Collectors.toMap(SxpDomain::getName, Function.identity()));
        if (sxpDomains.isEmpty())
            return;
        for (Map.Entry<String, SxpBindingFilter<?, ? extends SxpDomainFilterFields>> filter : getFilters().entrySet()) {
            if (sxpDomains.containsKey(filter.getKey()))
                propagateToSharedMasterDatabases(
                        removed.stream().filter(filter.getValue()).collect(Collectors.toList()),
                        added.stream().filter(filter.getValue()).collect(Collectors.toList()),
                        sxpDomains.get(filter.getKey()));
        }
    }

    /**
     * @param nodeIdRemote NodeId from which Bindings came from
     * @param sxpFilter    Inbound SxpFilter applied to transfer
     * @param removed      Bindings that were removed
     * @param added        Bindings that will be added
     */
    public void pushToSharedSxpDatabases(NodeId nodeIdRemote, SxpBindingFilter<?, ? extends SxpFilterFields> sxpFilter,
            List<SxpDatabaseBinding> removed, List<SxpDatabaseBinding> added) {
        if (nodeIdRemote == null || removed == null || added == null || (added.isEmpty() && removed.isEmpty()))
            return;
        Map<String, SxpDomain>
                sxpDomains =
                node.getDomains()
                        .stream()
                        .filter(d -> !d.getName().equals(getName()))
                        .collect(Collectors.toMap(SxpDomain::getName, Function.identity()));
        if (sxpDomains.isEmpty())
            return;
        for (Map.Entry<String, SxpBindingFilter<?, ? extends SxpDomainFilterFields>> filter : getFilters().entrySet()) {
            if (sxpDomains.containsKey(filter.getKey()))
                propagateToSharedSxpDatabases(removed.stream().filter(filter.getValue()).collect(Collectors.toList()),
                        added.stream().filter(filter.getValue()).collect(Collectors.toList()),
                        sxpDomains.get(filter.getKey()), nodeIdRemote, sxpFilter);
        }
    }

    /**
     * @return MasterDatabase assigned to this domain
     */
    public MasterDatabaseInf getMasterDatabase() {
        return masterDatabase;
    }

    /**
     * @return SxpDatabase assigned into this domain
     */
    public SxpDatabaseInf getSxpDatabase() {
        return sxpDatabase;
    }

    /**
     * @return Name of this domain
     */
    public String getName() {
        return name;
    }

    /**
     * @return Unmodifiable collection of SxpConnections in this domain
     */
    public Collection<SxpConnection> getConnections() {
        synchronized (connections) {
            return Collections.unmodifiableCollection(connections.values());
        }
    }

    /**
     * Gets SxpConnection from Domain
     *
     * @param address Address of remote peer
     * @return SxpConnection specified by address or null
     */
    public SxpConnection getConnection(InetSocketAddress address) {
        synchronized (connections) {
            return connections.get(Preconditions.checkNotNull(address).getAddress());
        }
    }

    /**
     * @param template ConnectionTemplate to be added
     * @param <T>      Any type extending SxpConnectionTemplateFields
     * @return if Template was successfully added
     */
    public <T extends SxpConnectionTemplateFields> boolean addConnectionTemplate(T template) {
        synchronized (templates) {
            if (templates.containsKey(Preconditions.checkNotNull(template).getTemplatePrefix())) {
                return false;
            }
            templates.put(template.getTemplatePrefix(), template);
        }
        return true;
    }

    /**
     * @param templatePrefix IpPrefix specifying Template
     * @return Removed Template or null
     */
    public SxpConnectionTemplateFields removeConnectionTemplate(IpPrefix templatePrefix) {
        synchronized (templates) {
            if (templates.containsKey(templatePrefix)) {
                return templates.get(templatePrefix);
            }
        }
        return null;
    }

    /**
     * @param address InetSocketAddress as corresponding substance of Template prefix
     * @return Associated Template
     */
    public SxpConnectionTemplateFields getTemplate(InetSocketAddress address) {
        final BitSet bitAddress = getBitAddress(Preconditions.checkNotNull(address).getAddress().getHostAddress());
        synchronized (templates) {
            for (IpPrefix ipPrefix : templates.keySet()) {
                boolean found = true;
                final BitSet prefixMatch = getBitAddress(IpPrefixConv.toString(ipPrefix).split("/")[0]);
                final int prefixMask = Integer.parseInt(IpPrefixConv.toString(ipPrefix).split("/")[1]);
                for (int i = 0; i < prefixMask; i++) {
                    if (bitAddress.get(i) != prefixMatch.get(i)) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return templates.get(ipPrefix);
                }
            }
        }
        return null;
    }

    /**
     * Check if Domain contains SxpConnection
     *
     * @param address Address of Peer to look for
     * @return If Domain contains remote peer with specified address
     */
    public boolean hasConnection(InetSocketAddress address) {
        synchronized (connections) {
            return connections.containsKey(Preconditions.checkNotNull(address).getAddress());
        }
    }

    /**
     * Adds SxpConnection into domain
     *
     * @param connection SxpConnection that will be added
     * @return Added SxpConnection
     */
    public SxpConnection putConnection(SxpConnection connection) {
        synchronized (connections) {
            if (connections.containsKey(Preconditions.checkNotNull(connection).getDestination().getAddress()))
                throw new IllegalArgumentException(
                        "Connection " + connection + " with destination " + connection.getDestination() + " exist.");
            connections.put(Preconditions.checkNotNull(connection).getDestination().getAddress(), connection);
            return connection;
        }
    }

    /**
     * @param address InetSocketAddress of remote peer that will be removed
     * @return Removed SxpConnection or null if there was no connection with specified address
     */
    public SxpConnection removeConnection(InetSocketAddress address) {
        synchronized (connections) {
            return connections.remove(Preconditions.checkNotNull(address).getAddress());
        }
    }

    @Override public void close() {
        synchronized (connections) {
            connections.values().forEach(SxpConnection::shutdown);
        }
    }
}
