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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpDomainFilterFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Map<FilterSpecific, SxpBindingFilter<?, ? extends SxpDomainFilterFields>>
            filters =
            new HashMap<>(FilterSpecific.values().length);
    private final Map<InetAddress, SxpConnection> connections = new HashMap<>();

    /**
     * @param name           Name of Domain
     * @param sxpDatabase    SxpDatabase that will be use to store incoming Bindings
     * @param masterDatabase MasterDatabase that will be used to store filtered Bindings
     */
    public SxpDomain(SxpNode owner, String name, SxpDatabaseInf sxpDatabase, MasterDatabaseInf masterDatabase) {
        this.masterDatabase = Preconditions.checkNotNull(masterDatabase);
        this.sxpDatabase = Preconditions.checkNotNull(sxpDatabase);
        this.name = Preconditions.checkNotNull(name);
        this.node = owner;
    }

    /**
     * @param owner  SxpNode to which Domain belongs
     * @param domain SxpDomain initializer
     */
    public SxpDomain(SxpNode owner,
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain domain) {
        Preconditions.checkNotNull(domain);
        this.masterDatabase = Preconditions.checkNotNull(new MasterDatabaseImpl());
        this.sxpDatabase = Preconditions.checkNotNull(new SxpDatabaseImpl());
        this.name = Preconditions.checkNotNull(domain.getDomainName());
        this.node = owner;
        if (domain.getMasterDatabase() != null) {
            masterDatabase.addBindings(domain.getMasterDatabase().getMasterDatabaseBinding());
        }
        if (domain.getConnections() != null && domain.getConnections().getConnection() != null) {
            domain.getConnections().getConnection().forEach(c -> putConnection(new SxpConnection(this.node, c, name)));
        }
        if (domain.getDomainFilters() != null && domain.getDomainFilters().getDomainFilter() != null) {
            domain.getDomainFilters()
                    .getDomainFilter()
                    .forEach(f -> addFilter(SxpBindingFilter.generateFilter(f, this.name)));
        }
    }

    public synchronized Collection<SxpBindingFilter<?, ? extends SxpDomainFilterFields>> getFilters() {
        Map<String, List<SxpBindingFilter<?, ? extends SxpDomainFilterFields>>> filterMap = new HashMap<>();
        filters.values().forEach(f -> f.getSxpFilter().getDomains().getDomain().forEach(d -> {
            if (filterMap.containsKey(d.getName())) {
                filterMap.get(d.getName()).add(f);
            } else {
                filterMap.put(d.getName(), Collections.singletonList(f));
            }
        }));
        return Collections.unmodifiableCollection(filterMap.entrySet()
                .stream()
                .map(e -> SxpBindingFilter.mergeFilters(e.getValue()))
                .collect(Collectors.toList()));
    }

    public synchronized boolean addFilter(SxpBindingFilter<?, ? extends SxpDomainFilterFields> filter) {
        if (filters.containsKey(Preconditions.checkNotNull(filter).getSxpFilter().getFilterSpecific())
                || filter.getSxpFilter() == null || filter.getSxpFilter().getDomains() == null
                || filter.getSxpFilter().getDomains().getDomain() == null
                || filter.getSxpFilter().getDomains().getDomain().isEmpty()
                || filter.getSxpFilter().getDomains().getDomain().stream().anyMatch(d->d.getName().equals(name)))
            return false;
        filters.put(filter.getSxpFilter().getFilterSpecific(), filter);
        filter.getSxpFilter()
                .getDomains()
                .getDomain()
                .forEach(d -> propagateToSharedDomains(null, shared(new ArrayList<>(), filter), filter));
        return true;
    }

    public synchronized SxpBindingFilter<?, ? extends SxpDomainFilterFields> removeFilter(FilterSpecific specific) {
        SxpBindingFilter<?, ? extends SxpDomainFilterFields>
                bindingFilter =
                filters.remove(Preconditions.checkNotNull(specific));
        if (bindingFilter != null) {
            bindingFilter.getSxpFilter()
                    .getDomains()
                    .getDomain()
                    .forEach(d -> propagateToSharedDomains(shared(new ArrayList<>(), bindingFilter), null,
                            bindingFilter));
        }
        return bindingFilter;
    }

    private List<SxpBindingFields> shared(final List<SxpBindingFields> shared,
            final SxpBindingFilter<?, ?> filter) {
        Preconditions.checkNotNull(shared);
        Preconditions.checkNotNull(filter);
        sxpDatabase.getBindings().stream().filter(filter).collect(Collectors.toCollection(() -> shared));
        masterDatabase.getLocalBindings().stream().filter(filter).collect(Collectors.toCollection(() -> shared));
        return shared;
    }

    public synchronized List<SxpBindingFields> sharesWithDomain(String domainName) {
        Preconditions.checkNotNull(domainName);
        List<SxpBindingFields> shared = new ArrayList<>();
        getFilters().stream().forEach(f -> shared(shared, f));
        return shared;
    }

    private void propagateToSharedDomain(List<SxpBindingFields> removed, List<SxpBindingFields> added,
            final String domain) {
        if (this.name.equals(Preconditions.checkNotNull(domain)))
            return;
        if (removed == null)
            removed = new ArrayList<>();
        if (added == null)
            added = new ArrayList<>();
        //Find Replace for bindings
        if (!removed.isEmpty()) {
            node.getDomains()
                    .stream()
                    .filter(d -> !d.getName().equals(this.name))
                    .map(d -> d.sharesWithDomain(domain))
                    .forEach(added::addAll);
        }
        //Propagate changes
        if (!removed.isEmpty() || !added.isEmpty()) {
            node.getSvcBindingDispatcher()
                    .propagateUpdate(node.getDomain(domain).getMasterDatabase().deleteBindings(removed),
                            node.getDomain(domain).getMasterDatabase().addBindings(added),
                            node.getAllOnSpeakerConnections(domain));
        }
    }

    private <T extends SxpBindingFields> void propagateToSharedDomains(final List<T> removed, final List<T> added,
            final SxpBindingFilter<?, ?> filter) {
        propagateToSharedDomain(added.stream().filter(filter).collect(Collectors.toList()),
                removed.stream().filter(filter).collect(Collectors.toList()), filter.getIdentifier());
    }

    public synchronized <T extends SxpBindingFields> void propagateToSharedDomains(final List<T> removed, final List<T> added) {
        getFilters().stream().forEach(f -> propagateToSharedDomains(removed, added, f));
    }

    public synchronized void propagateToSharedDomain(String domain) {
        if (!this.name.equals(domain))
            propagateToSharedDomain(null, sharesWithDomain(domain), domain);
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
    public synchronized Collection<SxpConnection> getConnections() {
        return Collections.unmodifiableCollection(connections.values());
    }

    /**
     * Gets SxpConnection from Domain
     *
     * @param address Address of remote peer
     * @return SxpConnection specified by address or null
     */
    public synchronized SxpConnection getConnection(InetSocketAddress address) {
        return connections.get(Preconditions.checkNotNull(address).getAddress());
    }

    /**
     * Check if Domain contains SxpConnection
     *
     * @param address Address of Peer to look for
     * @return If Domain contains remote peer with specified address
     */
    public synchronized boolean hasConnection(InetSocketAddress address) {
        return connections.containsKey(Preconditions.checkNotNull(address).getAddress());
    }

    /**
     * Adds SxpConnection into domain
     *
     * @param connection SxpConnection that will be added
     * @return Added SxpConnection
     */
    public synchronized SxpConnection putConnection(SxpConnection connection) {
        if (connections.containsKey(Preconditions.checkNotNull(connection).getDestination().getAddress()))
            throw new IllegalArgumentException(
                    "Connection " + connection + " with destination " + connection.getDestination() + " exist.");
        connections.put(Preconditions.checkNotNull(connection).getDestination().getAddress(), connection);
        return connection;
    }

    /**
     * @param address InetSocketAddress of remote peer that will be removed
     * @return Removed SxpConnection or null if there was no connection with specified address
     */
    public synchronized SxpConnection removeConnection(InetSocketAddress address) {
        return connections.remove(Preconditions.checkNotNull(address).getAddress());
    }

    @Override public synchronized void close() {
        getFilters().forEach(f -> removeFilter(f.getSxpFilter().getFilterSpecific()));
        connections.values().forEach(SxpConnection::shutdown);
    }
}
