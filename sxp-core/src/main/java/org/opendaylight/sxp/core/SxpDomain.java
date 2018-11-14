/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core;

import static org.opendaylight.sxp.util.ArraysUtil.getBitAddress;

import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

/**
 * SxpDomain class represent layer that isolate Connections
 * and Bindings learned from them to only Peers of the same domain.
 * Sharing of Bindings can be achieved by filters between domains.
 */
@SuppressWarnings("all")
public class SxpDomain implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SxpDomain.class.getName());

    private MasterDatabaseInf masterDatabase;
    private SxpDatabaseInf sxpDatabase;
    private final String name;
    private final SxpNode node;
    private final Map<FilterSpecific, Map<String, SxpBindingFilter<?, ? extends SxpDomainFilterFields>>>
            filters =
            new EnumMap<>(FilterSpecific.class);
    private final Map<InetAddress, SxpConnection> connections = new HashMap<>();
    private final Map<IpPrefix, SxpConnectionTemplateFields> templates = new HashMap<>();

    /**
     * @param name           Name of Domain
     * @param sxpDatabase    SxpDatabase that will be use to store incoming Bindings
     * @param masterDatabase MasterDatabase that will be used to store filtered Bindings
     */
    private SxpDomain(SxpNode owner, String name, SxpDatabaseInf sxpDatabase, MasterDatabaseInf masterDatabase) {
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
        SxpDomain instance = new SxpDomain(owner, name, sxpDatabase, masterDatabase);
        masterDatabase.initDBPropagatingListener(owner.getSvcBindingDispatcher(), instance);
        return instance;
    }

    /**
     * @param owner  SxpNode to which Domain belongs
     * @param domain SxpDomain initializer
     * @return Domain consisting of provided values
     */
    public static SxpDomain createInstance(SxpNode owner,
                                           org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain domain,
                                           MasterDatabaseInf masterDB,
                                           SxpDatabaseInf sxpDB) {
        SxpDomain sxpDomain = createInstance(owner, Preconditions.checkNotNull(domain).getDomainName(), sxpDB, masterDB);
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
                if (!sxpDomain.addFilter(SxpBindingFilter.generateFilter(filter, sxpDomain.getName()))) {
                    throw new IllegalArgumentException("Cannot create domain Filters contains invalid values");
                }
            }
        }
        return sxpDomain;
    }

    /**
     * Create a domain with default in-memory databases.
     *
     * @param owner  SxpNode to which Domain belongs
     * @param domain SxpDomain initializer
     * @return Domain consisting of provided values
     */
    public static SxpDomain createInstance(SxpNode owner,
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain domain) {
        return createInstance(owner, domain, new MasterDatabaseImpl(), new SxpDatabaseImpl());
    }

    /**
     * Gets all filters,these filters may be merged and their entries thus wont be accessible
     *
     * @return Filters contained in current domains
     */
    public Map<String, SxpBindingFilter<?, ? extends SxpDomainFilterFields>> getFilters() {
        Map<String, SxpBindingFilter<?, ? extends SxpDomainFilterFields>> filterMap = new HashMap<>();
        synchronized (filters) {
            for (Map<String, SxpBindingFilter<?, ? extends SxpDomainFilterFields>> m : filters.values()) {
                m.values().forEach(filter -> {
                    filter.getSxpFilter().getDomains().getDomain().forEach(d -> {
                        if (Objects.nonNull(filterMap.putIfAbsent(d.getName(), filter))) {
                            // Merge on filter can be executed MAX 2 times per filter type
                            filterMap.put(d.getName(),
                                    SxpBindingFilter.mergeFilters(Arrays.asList(filter, filterMap.get(d.getName()))));
                        }
                    });
                });
            }
        }
        return Collections.unmodifiableMap(filterMap);
    }

    /**
     * Checks if filter fields are correctly set
     *
     * @param filter Filter that will be checked
     */
    private void checkFilter(SxpBindingFilter<?, ? extends SxpDomainFilterFields> filter) {
        Objects.requireNonNull(filter);
        Objects.requireNonNull(filter.getSxpFilter());
        Objects.requireNonNull(filter.getSxpFilter().getFilterEntries());
        Objects.requireNonNull(filter.getSxpFilter().getDomains());
        Objects.requireNonNull(filter.getSxpFilter().getDomains().getDomain());
        Objects.requireNonNull(filter.getSxpFilter().getFilterName());
        Objects.requireNonNull(filter.getSxpFilter().getFilterSpecific());
    }

    /**
     * Adds filter into Domain and propagate changes to other domains
     *
     * @param filter Domain filter that will be added
     * @return If Filter was added
     */
    public boolean addFilter(SxpBindingFilter<?, ? extends SxpDomainFilterFields> filter) {
        checkFilter(filter);
        if (filter.getSxpFilter().getDomains().getDomain().isEmpty() || filter.getSxpFilter()
                .getDomains()
                .getDomain()
                .stream()
                .anyMatch(d -> d.getName().equals(getName())) || checkDomainOverlap(
                filter.getSxpFilter().getFilterSpecific(), filter.getSxpFilter().getDomains())) {
            return false;
        }
        synchronized (filters) {
            if (filters.get(filter.getSxpFilter().getFilterSpecific())
                    .containsKey(filter.getSxpFilter().getFilterName())) {
                return false;
            }
            filters.get(filter.getSxpFilter().getFilterSpecific()).put(filter.getSxpFilter().getFilterName(), filter);
        }
        for (SxpDomain domain : node.getDomains()) {
            if (!domain.getName().equals(getName()) && filter.getSxpFilter()
                    .getDomains()
                    .getDomain()
                    .stream()
                    .anyMatch(f -> f.getName().equals(domain.getName()))) {
                // push changes to MDb
                propagateToSharedMasterDatabases(Collections.emptyList(),
                        getMasterDatabase().getBindings(BindingOriginsConfig.LOCAL_ORIGIN).stream().filter(filter).collect(Collectors.toList()),
                        domain);
                // push changes to SXPDb
                getConnections().stream().filter(SxpConnection::isModeListener).forEach(c -> {
                    propagateToSharedSxpDatabases(Collections.emptyList(), getSxpDatabase().getBindings(c.getId())
                            .stream()
                            .filter(filter)
                            .collect(Collectors.toList()), domain, c.getId(), c.getFilter(FilterType.Inbound));
                });
            }
        }
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
        if (Objects.nonNull(bindingFilter)) {
            for (SxpDomain domain : node.getDomains()) {
                if (!domain.getName().equals(getName()) && bindingFilter.getSxpFilter()
                        .getDomains()
                        .getDomain()
                        .stream()
                        .anyMatch(f -> f.getName().equals(domain.getName()))) {
                    // push changes to MDb
                    propagateToSharedMasterDatabases(getMasterDatabase().getBindings(BindingOriginsConfig.LOCAL_ORIGIN)
                            .stream()
                            .filter(bindingFilter)
                            .collect(Collectors.toList()), Collections.emptyList(), domain);
                    // push changes to SXPDb
                    getConnections().stream().filter(SxpConnection::isModeListener).forEach(c -> {
                        propagateToSharedSxpDatabases(getSxpDatabase().getBindings(c.getId())
                                        .stream()
                                        .filter(bindingFilter)
                                        .collect(Collectors.toList()), Collections.emptyList(), domain, c.getId(),
                                c.getFilter(FilterType.Inbound));
                    });
                }
            }
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
        checkFilter(newFilter);
        SxpBindingFilter<?, ? extends SxpDomainFilterFields> oldFilter;
        synchronized (filters) {
            oldFilter =
                    filters.get(newFilter.getSxpFilter().getFilterSpecific())
                            .get(newFilter.getSxpFilter().getFilterName());
            if (Objects.nonNull(oldFilter)) {
                filters.get(newFilter.getSxpFilter().getFilterSpecific())
                        .put(newFilter.getSxpFilter().getFilterName(), newFilter);
            }
        }
        if (Objects.isNull(oldFilter)) {
            return addFilter(newFilter);
        }
        final List<SxpDomain>
                domains =
                node.getDomains().stream().filter(d -> !d.getName().equals(getName())).collect(Collectors.toList());
        // Propagating changes from domains based on scope of affected binding by current filter
        // domains only in old filter
        domains.stream()
                .filter(d -> oldFilter.getSxpFilter()
                        .getDomains()
                        .getDomain()
                        .stream()
                        .anyMatch(filter_d -> filter_d.getName().equals(d.getName())) && newFilter.getSxpFilter()
                        .getDomains()
                        .getDomain()
                        .stream()
                        .noneMatch(filter_d -> filter_d.getName().equals(d.getName())))
                .forEach(domain -> {
                    propagateToSharedMasterDatabases(getMasterDatabase().getBindings(BindingOriginsConfig.LOCAL_ORIGIN)
                            .stream()
                            .filter(oldFilter)
                            .collect(Collectors.toList()), Collections.emptyList(), domain);
                    getConnections().stream().filter(SxpConnection::isModeListener).forEach(c -> {
                        // push changes to SXPDb
                        propagateToSharedSxpDatabases(getSxpDatabase().getBindings(c.getId())
                                        .stream()
                                        .filter(oldFilter)
                                        .collect(Collectors.toList()), Collections.emptyList(), domain, c.getId(),
                                c.getFilter(FilterType.Inbound));
                    });
                });
        // domains in both old and new filter
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
                    // push changes to SXPDb
                    propagateToSharedMasterDatabases(getMasterDatabase().getBindings(BindingOriginsConfig.LOCAL_ORIGIN)
                            .stream()
                            .filter(b -> oldFilter.test(b) && !newFilter.test(b))
                            .collect(Collectors.toList()), getMasterDatabase().getBindings(BindingOriginsConfig.LOCAL_ORIGIN)
                            .stream()
                            .filter(b -> !oldFilter.test(b) && newFilter.test(b))
                            .collect(Collectors.toList()), domain);
                    getConnections().stream().filter(SxpConnection::isModeListener).forEach(c -> {
                        // push changes to SXPDb
                        propagateToSharedSxpDatabases(getSxpDatabase().getBindings(c.getId())
                                .stream()
                                .filter(b -> oldFilter.test(b) && !newFilter.test(b))
                                .collect(Collectors.toList()), getSxpDatabase().getBindings(c.getId())
                                .stream()
                                .filter(b -> !oldFilter.test(b) && newFilter.test(b))
                                .collect(Collectors.toList()), domain, c.getId(), c.getFilter(FilterType.Inbound));
                    });
                });
        // domains only in new filter
        domains.stream()
                .filter(d -> oldFilter.getSxpFilter()
                        .getDomains()
                        .getDomain()
                        .stream()
                        .noneMatch(filter_d -> filter_d.getName().equals(d.getName())) && newFilter.getSxpFilter()
                        .getDomains()
                        .getDomain()
                        .stream()
                        .anyMatch(filter_d -> filter_d.getName().equals(d.getName())))
                .forEach(domain -> {
                    // push changes to MDb
                    propagateToSharedMasterDatabases(getMasterDatabase().getBindings(BindingOriginsConfig.LOCAL_ORIGIN)
                            .stream()
                            .filter(oldFilter)
                            .collect(Collectors.toList()), Collections.emptyList(), domain);
                    getConnections().stream().filter(SxpConnection::isModeListener).forEach(c -> {
                        // push changes to SXPDb
                        propagateToSharedSxpDatabases(getSxpDatabase().getBindings(c.getId())
                                        .stream()
                                        .filter(oldFilter)
                                        .collect(Collectors.toList()), Collections.emptyList(), domain, c.getId(),
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
        if (Objects.isNull(domain) || this.name.equals(domain.getName()) || (remove.isEmpty() && add.isEmpty())) {
            return;
        }
        List<SxpBindingFields> added = new ArrayList<>();
        List<SxpBindingFields> deleted = new ArrayList<>();
        List<SxpBindingFields> replace = new ArrayList<>(add);
        Map<NodeId, SxpBindingFilter> filterMap = SxpDatabase.getInboundFilters(node, domain.getName());
        synchronized (domain) {
            deleted.addAll(domain.getMasterDatabase().deleteBindings(remove));
            if (!deleted.isEmpty()) {
                replace.addAll(SxpDatabase.getReplaceForBindings(deleted, domain.getSxpDatabase(), filterMap));
                //Fix for specific cases where local bindings are overwritten by shared local bindings
                replace.addAll(domain.getMasterDatabase().getBindings(BindingOriginsConfig.LOCAL_ORIGIN));
            }
            added.addAll(domain.getMasterDatabase().addBindings(replace));
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
        if (Objects.isNull(nodeIdRemote) || Objects.isNull(domain) || this.name.equals(domain.getName()) || (
                remove.isEmpty() && add.isEmpty())) {
            return;
        }
        synchronized (domain) {
            final List<SxpDatabaseBinding> deleted = domain.getSxpDatabase().deleteBindings(nodeIdRemote, remove);
            List<SxpDatabaseBinding> added
                    = domain.getSxpDatabase().addBinding(nodeIdRemote, add);
            if (!added.isEmpty() || !deleted.isEmpty()) {
                if (Objects.isNull(filter)) {
                    propagateToSharedMasterDatabases(deleted, added, domain);
                } else {
                    propagateToSharedMasterDatabases(deleted.stream().filter(filter).collect(Collectors.toList()),
                            added.stream().filter(filter).collect(Collectors.toList()), domain);
                }
            }
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
        if (Objects.isNull(removed) || Objects.isNull(added) || (added.isEmpty() && removed.isEmpty())) {
            return;
        }
        final Map<String, SxpDomain> sxpDomains = new HashMap<>();
        for (SxpDomain d : node.getDomains()) {
            if (!getName().equals(d.getName())) {
                sxpDomains.put(d.getName(), d);
            }
        }
        if (!sxpDomains.isEmpty()) {
            final Map<String, SxpBindingFilter<?, ? extends SxpDomainFilterFields>> filters = getFilters();
            for (Map.Entry<String, SxpBindingFilter<?, ? extends SxpDomainFilterFields>> filter : filters.entrySet()) {
                if (sxpDomains.containsKey(filter.getKey())) {
                    propagateToSharedMasterDatabases(
                            removed.stream().filter(filter.getValue()).collect(Collectors.toList()),
                            added.stream().filter(filter.getValue()).collect(Collectors.toList()),
                            sxpDomains.get(filter.getKey()));
                }
            }
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
        if (Objects.isNull(nodeIdRemote) || Objects.isNull(removed) || Objects.isNull(added) || (added.isEmpty()
                && removed.isEmpty())) {
            return;
        }
        final Map<String, SxpDomain> sxpDomains = new HashMap<>();
        for (SxpDomain d : node.getDomains()) {
            if (!getName().equals(d.getName())) {
                sxpDomains.put(d.getName(), d);
            }
        }
        if (!sxpDomains.isEmpty()) {
            final Map<String, SxpBindingFilter<?, ? extends SxpDomainFilterFields>> filters = getFilters();
            for (Map.Entry<String, SxpBindingFilter<?, ? extends SxpDomainFilterFields>> filter : filters.entrySet()) {
                if (sxpDomains.containsKey(filter.getKey())) {
                    propagateToSharedSxpDatabases(
                            removed.stream().filter(filter.getValue()).collect(Collectors.toList()),
                            added.stream().filter(filter.getValue()).collect(Collectors.toList()),
                            sxpDomains.get(filter.getKey()), nodeIdRemote, sxpFilter);
                }
            }
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
    public List<SxpConnection> getConnections() {
        synchronized (connections) {
            return Collections.unmodifiableList(new ArrayList<>(connections.values()));
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
        if (template.getTemplatePassword() != null && !template.getTemplatePassword().isEmpty()) {
            node.updateMD5keys();
        }
        return true;
    }

    /**
     * @param templatePrefix IpPrefix specifying Template
     * @return Removed Template or null
     */
    public SxpConnectionTemplateFields removeConnectionTemplate(IpPrefix templatePrefix) {
        SxpConnectionTemplateFields template;
        synchronized (templates) {
            template = templates.remove(templatePrefix);
        }
        if (template != null && template.getTemplatePassword() != null && !template.getTemplatePassword().isEmpty()) {
            node.updateMD5keys();
        }
        return template;
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
     * @return ConnectionTemplates associated with current domain
     */
    public Collection<SxpConnectionTemplateFields> getConnectionTemplates() {
        synchronized (templates) {
            return Collections.unmodifiableCollection(templates.values());
        }
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
            if (connections.containsKey(Preconditions.checkNotNull(connection).getDestination().getAddress())) {
                throw new IllegalArgumentException(
                        "Connection " + connection + " with destination " + connection.getDestination() + " exist.");
            }
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

    @Override
    public void close() {
        LOG.info("Closig SXP domain and its connections: {}", this);
        getConnections().forEach(SxpConnection::shutdown);
        try {
            sxpDatabase.close();
        } catch (Exception e) {
            LOG.error("{} Failed to properly close the SXP DB", this);
        }
        try {
            masterDatabase.close();
        } catch (Exception e) {
            LOG.error("{} Failed to properly close the Master DB", this);
        }
    }
}
