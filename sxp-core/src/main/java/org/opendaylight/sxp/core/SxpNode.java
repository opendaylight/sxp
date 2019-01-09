/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opendaylight.sxp.core.handler.ConnectionDecoder;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.core.handler.MessageDecoder;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.core.service.BindingHandler;
import org.opendaylight.sxp.core.service.ConnectFacade;
import org.opendaylight.sxp.core.service.SslContextFactory;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.Security;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.SxpDatabase;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.exception.node.DomainNotFoundException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.sxp.peers.SxpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The source-group tag exchange protocol (SXP) aware node implementation. SXP
 * is a control protocol to propagate IP address to Source Group Tag (SGT)
 * binding information across network devices.
 * Source groups are the endpoints connecting to the network that have common
 * network policies. Each source group is identified by a unique SGT value. The
 * SGT to which an endpoint belongs can be assigned statically or dynamically,
 * and the SGT can be used as a classifier in network policies.
 */
public class SxpNode {

    private static final Logger LOG = LoggerFactory.getLogger(SxpNode.class);
    public static final String DEFAULT_DOMAIN = "global";
    private static final Predicate<SxpConnection> INACTIVE_CONNECTION_FILTER = sxpConnection -> {
        if (!sxpConnection.isModeBoth() && ConnectionState.On == sxpConnection.getState()) {
            return false;
        }
        return !(sxpConnection.isModeBoth() && sxpConnection.isBidirectionalBoth());
    };

    protected final HandlerFactory handlerFactoryServer = HandlerFactory.instanceAddDecoder(
            MessageDecoder.createServerProfile(this), HandlerFactory.Position.END);
    protected final BindingDispatcher svcBindingDispatcher;
    protected final BindingHandler svcBindingHandler;
    protected final Map<String, SxpPeerGroupBuilder> peerGroupMap = new HashMap<>();
    protected final Map<String, SxpDomain> sxpDomains = new HashMap<>();

    private final ReentrantLock md5UpdateLock = new ReentrantLock();
    private volatile boolean processingConnectionData;
    private final ThreadsWorker worker;
    private InetAddress sourceIp;
    private SslContextFactory sslContextFactory;
    private final SxpNodeIdentityBuilder nodeBuilder;
    private final NodeId nodeId;
    private Channel serverChannel;
    private final Map<TimerType, ListenableScheduledFuture<?>> timers = new EnumMap<>(TimerType.class);

    /**
     * Create new instance of SxpNode with empty databases
     * and default ThreadWorkers
     *
     * @param nodeId ID of newly created Node
     * @param node   Node setup data
     * @return New instance of SxpNode
     */
    public static SxpNode createInstance(NodeId nodeId, SxpNodeIdentity node) {
        return createInstance(nodeId, node, new MasterDatabaseImpl(), new SxpDatabaseImpl());
    }

    /**
     * Create new instance of SxpNode containing provided database data
     * and default ThreadWorkers
     * Be aware that sharing of the same DB among multiple SxpNode isn't
     * supported and may cause unexpected behaviour
     *
     * @param nodeId         ID of newly created Node
     * @param node           Node setup data
     * @param masterDatabase Data which will be added to Master-DB
     * @param sxpDatabase    Data which will be added to SXP-DB
     * @return New instance of SxpNode
     */
    public static SxpNode createInstance(NodeId nodeId, SxpNodeIdentity node, MasterDatabaseInf masterDatabase,
            SxpDatabaseInf sxpDatabase) {
        return createInstance(nodeId, node, masterDatabase, sxpDatabase, new ThreadsWorker());
    }

    /**
     * Create new instance of SxpNode containing provided database data
     * and custom ThreadWorkers
     * Be aware that sharing of the same DB among multiple SxpNode isn't
     * supported and may cause unexpected behaviour
     * @param nodeId         ID of newly created Node
     * @param node           Node setup data
     * @param masterDatabase Data which will be added to Master-DB
     * @param sxpDatabase    Data which will be added to SXP-DB
     * @param worker         Thread workers which will be executing task inside SxpNode
     * @return New instance of SxpNode
     */
    public static SxpNode createInstance(NodeId nodeId, SxpNodeIdentity node, MasterDatabaseInf masterDatabase,
            SxpDatabaseInf sxpDatabase, ThreadsWorker worker) {
        Preconditions.checkNotNull(sxpDatabase);
        Preconditions.checkNotNull(masterDatabase);
        SxpNode sxpNode = new SxpNode(nodeId, node, worker);
        if (node.getSxpDomains() != null && node.getSxpDomains().getSxpDomain() != null) {
            node.getSxpDomains().getSxpDomain().forEach(sxpDomain -> sxpNode.addDomain(sxpDomain, masterDatabase, sxpDatabase));
        }
        if (sxpNode.sxpDomains.isEmpty()) {
            sxpNode.sxpDomains.put(DEFAULT_DOMAIN,
                    SxpDomain.createInstance(sxpNode, DEFAULT_DOMAIN, sxpDatabase, masterDatabase));
        }
        if (node.getSxpPeerGroups() != null && node.getSxpPeerGroups().getSxpPeerGroup() != null) {
            node.getSxpPeerGroups()
                    .getSxpPeerGroup()
                    .forEach(g -> sxpNode.addPeerGroup(new SxpPeerGroupBuilder(g).build()));
        }
        sxpNode.handlerFactoryServer.addDecoder(new ConnectionDecoder(sxpNode), HandlerFactory.Position.BEGIN);
        return sxpNode;
    }

    /**
     * Default constructor that creates and start SxpNode using provided values
     *
     * @param nodeId ID of newly created Node
     * @param node   Node setup data
     * @param worker Thread workers which will be executing task inside SxpNode
     */
    protected SxpNode(NodeId nodeId, SxpNodeIdentity node, ThreadsWorker worker) {
        this.nodeBuilder = new SxpNodeIdentityBuilder(Preconditions.checkNotNull(node));
        this.nodeId = Preconditions.checkNotNull(nodeId);
        this.worker = Preconditions.checkNotNull(worker);
        this.svcBindingDispatcher = new BindingDispatcher(this);
        this.sslContextFactory =
                new SslContextFactory(Objects.isNull(node.getSecurity()) ? null : node.getSecurity().getTls());
        if (Objects.nonNull(node.getMessageBuffering()) && Objects.nonNull(node.getMessageBuffering().getInBuffer())
                && Objects.nonNull(node.getMessageBuffering().getOutBuffer())) {
            this.svcBindingHandler =
                    new BindingHandler(this, this.svcBindingDispatcher, node.getMessageBuffering().getInBuffer());
            svcBindingDispatcher.setPartitionSize(node.getMessageBuffering().getOutBuffer());
        } else {
            this.svcBindingHandler = new BindingHandler(this, this.svcBindingDispatcher);
        }
        this.sourceIp = InetAddresses.forString(Search.getAddress(node.getSourceIp()));
    }

    /**
     * @return SxpNodeIdentity containing configuration of current Node
     */
    protected SxpNodeIdentity getNodeIdentity() {
        return nodeBuilder.build();
    }

    /**
     * Add and open a new connection. If the connection contains a password, update the MD5 keys.
     *
     * @param connection connection to add
     */
    public SxpConnection addConnection(SxpConnection connection) {
        synchronized (peerGroupMap) {
            for (SxpPeerGroup peerGroup : getPeerGroup(connection)) {
                for (SxpFilter filter : peerGroup.getSxpFilter()) {
                    connection.putFilter(SxpBindingFilter.generateFilter(filter, peerGroup.getName()));
                }
            }
        }
        synchronized (sxpDomains) {
            if (!sxpDomains.containsKey(connection.getDomainName())) {
                LOG.warn("{} Domain {} does not exist", this, connection.getDomainName());
                throw new DomainNotFoundException(getName(), connection.getDomainName());
            }
            sxpDomains.get(connection.getDomainName()).putConnection(connection);
        }
        if (connection.hasNonEmptyPassword()) {
            updateMD5keys();
        }
        Future<Void> openConnection = connection.openConnection();
        openConnection.addListener((FutureListener<Void>) future -> {
            if (!future.isSuccess() && !future.isCancelled()) {
                LOG.warn("{} Failed to connect to remote peer, scheduling retry", connection);
                connection.scheduleRetryOpen();
            }
        });
        return connection;
    }

    /**
     * Checks if there is another PeerGroup with at least one same peer and Filter with same type
     *
     * @param group SxpPeerGroup to be checked on overlap
     * @return SxpPeerGroup that overlaps the specified one or null if there is no overlap
     */
    private SxpPeerGroup checkPeerGroupOverlap(SxpPeerGroupBuilder group) {
        Set<SxpPeer> peerSet1 = new HashSet<>();
        if (group.getSxpPeers() != null && group.getSxpPeers().getSxpPeer() != null) {
            peerSet1.addAll(group.getSxpPeers().getSxpPeer());
        }
        for (SxpPeerGroup peerGroup : getPeerGroups()) {
            Set<SxpPeer> peerSet2 = new HashSet<>();
            if (peerGroup.getSxpPeers() != null && peerGroup.getSxpPeers().getSxpPeer() != null) {
                peerSet2.addAll(peerGroup.getSxpPeers().getSxpPeer());
            }
            if (peerGroup.getName().equals(group.getName()) || (!peerSet1.isEmpty() && !peerSet2.isEmpty()
                    && Sets.intersection(peerSet1, peerSet2).isEmpty())) {
                continue;
            }
            if (group.getSxpFilter() != null) {
                for (SxpFilter filter1 : group.getSxpFilter()) {
                    for (SxpFilter filter2 : peerGroup.getSxpFilter()) {
                        if (SxpBindingFilter.checkInCompatibility(filter1, filter2)) {
                            return peerGroup;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets all SxpConnections in node contained in SxpPeerGroup,
     * or All connection on node if none peers are specified in SxpPeerGroup
     *
     * @param peerGroup PeerGroup containing peers
     * @return List of Connections
     */
    private List<SxpConnection> getConnections(SxpPeerGroupBuilder peerGroup) {
        if (peerGroup.getSxpPeers() == null || peerGroup.getSxpPeers().getSxpPeer() == null || peerGroup.getSxpPeers()
                .getSxpPeer()
                .isEmpty()) {
            return getAllConnections();
        }
        List<SxpConnection> sxpConnections = new ArrayList<>();
        for (SxpPeer sxpPeer : peerGroup.getSxpPeers().getSxpPeer()) {
            SxpConnection
                    connection =
                    getByAddress(new InetSocketAddress(Search.getAddress(sxpPeer.getPeerAddress()),
                            sxpPeer.getPeerPort() == null ? Constants.SXP_DEFAULT_PORT : sxpPeer.getPeerPort().getValue()));
            if (connection != null) {
                sxpConnections.add(connection);
            }
        }
        return sxpConnections;
    }

    /**
     * Gets SxpPeerGroups in which specified connection is
     *
     * @param connection SxpConnections to look for in PeerGroups
     * @return List of PeerGroups that contain SxpConnection
     */
    private List<SxpPeerGroup> getPeerGroup(SxpConnection connection) {
        List<SxpPeerGroup> sxpPeerGroups = new ArrayList<>();
        synchronized (peerGroupMap) {
            for (SxpPeerGroupBuilder peerGroup : peerGroupMap.values()) {
                if (peerGroup.getSxpPeers().getSxpPeer() == null || peerGroup.getSxpPeers().getSxpPeer().isEmpty()) {
                    sxpPeerGroups.add(peerGroup.build());
                    continue;
                }
                for (SxpPeer peer : peerGroup.getSxpPeers().getSxpPeer()) {
                    InetAddress address = InetAddresses.forString(Search.getAddress(peer.getPeerAddress()));
                    if (address.equals(connection.getDestination().getAddress())) {
                        sxpPeerGroups.add(peerGroup.build());
                    }
                }
            }
        }
        return sxpPeerGroups;
    }

    /**
     * Adds new PeerGroup to Node and add filters to connections if any
     *
     * @param peerGroup PeerGroup to be added
     */
    public boolean addPeerGroup(SxpPeerGroup peerGroup) {
        synchronized (peerGroupMap) {
            if (peerGroup == null || peerGroup.getName() == null || peerGroup.getSxpPeers() == null) {
                LOG.warn("{} Cannot add PeerGroup {} due to missing fields", this, peerGroup);
                return false;
            }
            SxpPeerGroupBuilder groupBuilder = new SxpPeerGroupBuilder(peerGroup);
            if (checkPeerGroupOverlap(groupBuilder) != null) {
                throw new IllegalArgumentException("PeerGroup filter overlaps with filter on other PeerGroup");
            }
            if (groupBuilder.getSxpFilter() == null) {
                groupBuilder.setSxpFilter(new ArrayList<>());
            } else {
                Set<SxpBindingFilter<?, ? extends SxpFilterFields>> filters = new HashSet<>();
                for (SxpFilter filter : peerGroup.getSxpFilter()) {
                    filters.add(SxpBindingFilter.generateFilter(filter, peerGroup.getName()));
                }
                if (filters.size() != peerGroup.getSxpFilter().size()) {
                    LOG.warn("{} Cannot add PeerGroup {} due to multiple declarations of filter with same type", this,
                            peerGroup);
                    return false;
                }
                List<SxpConnection> connections = getConnections(groupBuilder);
                for (SxpConnection connection : connections) {
                    filters.forEach(connection::putFilter);
                }
            }
            peerGroupMap.put(peerGroup.getName(), groupBuilder);
            return true;
        }
    }

    /**
     * @param peerGroupName Name of PeerGroup
     * @return PeerGroup with specified name on this node
     */
    public SxpPeerGroup getPeerGroup(String peerGroupName) {
        synchronized (peerGroupMap) {
            return peerGroupMap.get(peerGroupName) != null ? peerGroupMap.get(peerGroupName).build() : null;
        }
    }

    /**
     * Removes PeerGroup from Node and removes filters from connections if any
     *
     * @param peerGroupName Name of PeerGroup that will be removed
     * @return Removed PeerGroup
     */
    public SxpPeerGroup removePeerGroup(String peerGroupName) {
        synchronized (peerGroupMap) {
            SxpPeerGroupBuilder peerGroup = peerGroupMap.remove(peerGroupName);
            if (peerGroup == null) {
                return null;
            }
            List<SxpConnection> connections = getConnections(peerGroup);
            for (SxpConnection connection : connections) {
                for (SxpFilter filter : peerGroup.getSxpFilter()) {
                    connection.removeFilter(filter.getFilterType(), filter.getFilterSpecific());
                }
            }
            return peerGroup.build();
        }
    }

    /**
     * @return All PeerGroups on this node
     */
    public Collection<SxpPeerGroup> getPeerGroups() {
        synchronized (peerGroupMap) {
            return peerGroupMap
                    .values()
                    .stream()
                    .map(input -> input != null ? input.build() : null)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Adds new Filter to specified PeerGroup
     *
     * @param peerGroupName Name of PeerGroup where SxpFilter will be added
     * @param sxpFilter     SxpFilter that will be used
     */
    public boolean addFilterToPeerGroup(String peerGroupName, SxpFilter sxpFilter) {
        synchronized (peerGroupMap) {
            SxpPeerGroupBuilder peerGroup = peerGroupMap.get(peerGroupName);
            if (peerGroup == null || sxpFilter == null) {
                LOG.warn("{} Cannot add Filter | Due to null parameters", this);
                return false;
            }
            SxpBindingFilter<?, ? extends SxpFilterFields>
                    bindingFilter =
                    SxpBindingFilter.generateFilter(sxpFilter, peerGroupName);
            List<SxpFilter> sxpFilters = peerGroup.getSxpFilter();
            for (SxpFilter filter : sxpFilters) {
                if (SxpBindingFilter.checkInCompatibility(filter, sxpFilter)) {
                    LOG.warn("{} Filter of type {} already defined", this, sxpFilter.getFilterType());
                    return filter.equals(sxpFilter);
                }
            }
            sxpFilters.add(sxpFilter);
            if (checkPeerGroupOverlap(peerGroup) != null) {
                sxpFilters.remove(sxpFilter);
                LOG.warn("{} Filter cannot be added due to overlap of Peer Groups", this, sxpFilter);
                return false;
            }
            List<SxpConnection> connections = getConnections(peerGroup);
            for (SxpConnection connection : connections) {
                connection.putFilter(bindingFilter);
            }
            return true;
        }
    }

    /**
     * Update Filter in PeerGroup of specific type
     *
     * @param peerGroupName Name of PeerGroup that contains filter
     * @param newFilter     SxpFilter with new values that will be used
     * @return Old SxpFilter
     */
    public SxpFilter updateFilterInPeerGroup(String peerGroupName, SxpFilter newFilter) {
        synchronized (peerGroupMap) {
            SxpPeerGroupBuilder peerGroup = peerGroupMap.get(peerGroupName);
            if (peerGroup == null || newFilter == null) {
                LOG.warn("{} Cannot update Filter | Due to null parameters", this);
                return null;
            }
            List<SxpFilter> sxpFilters = peerGroup.getSxpFilter();
            SxpFilter oldFilter = null;
            for (SxpFilter filter : sxpFilters) {
                if (SxpBindingFilter.checkInCompatibility(filter, newFilter)) {
                    oldFilter = filter;
                    break;
                }
            }
            if (oldFilter == null) {
                LOG.warn("{} Cannot update Filter | No previous filter with type {} found", this,
                        newFilter.getFilterType());
                return null;
            }
            sxpFilters.remove(oldFilter);
            sxpFilters.add(newFilter);
            SxpBindingFilter bindingFilter = SxpBindingFilter.generateFilter(newFilter, peerGroupName);
            List<SxpConnection> connections = getConnections(peerGroup);
            for (SxpConnection connection : connections) {
                connection.putFilter(bindingFilter);
            }
            return oldFilter;
        }
    }

    /**
     * Removes last added Filter from specified PeerGroup
     *
     * @param peerGroupName Name of PeerGroup that contains filter
     * @param filterType    Type of Filter that will be removed
     * @return If any filters were removed
     */
    public boolean removeFilterFromPeerGroup(String peerGroupName, FilterType filterType, FilterSpecific specific) {
        synchronized (peerGroupMap) {
            SxpPeerGroupBuilder peerGroup = peerGroupMap.get(peerGroupName);
            if (peerGroup == null || filterType == null) {
                return false;
            }
            List<SxpConnection> connections = getConnections(peerGroup);
            return peerGroup.getSxpFilter().removeIf(f -> {
                boolean
                        remove =
                        specific == null ? f.getFilterType().equals(filterType) :
                                (f.getFilterType().equals(filterType) && specific.equals(f.getFilterSpecific()));
                if (remove) {
                    connections.forEach(c -> c.removeFilter(filterType, f.getFilterSpecific()));
                }
                return remove;
            });
        }
    }

    /**
     * Add a domain to the SxpNode if there is not a domain with the same name already present.
     * Uses default, in-memory implementation of databases.
     *
     * @param domain SXpDomain initializer
     * @return If Domain was added
     */
    public boolean addDomain(
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain domain) {
        Preconditions.checkNotNull(domain);
        Preconditions.checkNotNull(domain.getDomainName());
        synchronized (sxpDomains) {
            if (!sxpDomains.containsKey(domain.getDomainName())) {
                sxpDomains.put(domain.getDomainName(), SxpDomain.createInstance(this, domain));
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Add a domain to the SxpNode if there is not a domain with the same name already present.
     *
     * @param domain SXPDomain configuration
     * @param masterDB Master database to use
     * @param sxpDB SXP database to use
     * @return If Domain was added
     */
    public boolean addDomain(
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain domain,
            MasterDatabaseInf masterDB,
            SxpDatabaseInf sxpDB) {
        Preconditions.checkNotNull(domain);
        Preconditions.checkNotNull(domain.getDomainName());
        synchronized (sxpDomains) {
            if (!sxpDomains.containsKey(domain.getDomainName())) {
                sxpDomains.put(domain.getDomainName(), SxpDomain.createInstance(this, domain, masterDB, sxpDB));
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove Domain from SxpNode if there is not domain with specified name thrown IllegalArgumentException
     *
     * @param domainName Name assigned to domain that will be removed
     * @return SxpDomain that was removed
     */
    public SxpDomain removeDomain(String domainName) {
        synchronized (sxpDomains) {
            if (sxpDomains.containsKey(Preconditions.checkNotNull(domainName)) && sxpDomains.get(domainName)
                    .getConnections()
                    .isEmpty()) {
                sxpDomains.get(domainName).close();
                return sxpDomains.remove(domainName);
            }
            throw new IllegalStateException("Domain " + domainName + "cannot be removed.");
        }
    }

    /**
     * @param name Domain name to look for
     * @return SxpDomain or null if not found
     */
    public SxpDomain getDomain(String name) {
        synchronized (sxpDomains) {
            return sxpDomains.get(name);
        }
    }

    /**
     * @return Collections of All Domains in current Node
     */
    public Collection<SxpDomain> getDomains() {
        synchronized (sxpDomains) {
            return Collections.unmodifiableCollection(sxpDomains.values());
        }
    }

    /**
     * @param domain Name of Domain where filter will be added
     * @param filter Filter that will be added to domain
     * @return If filter was added to domain
     */
    public boolean addFilterToDomain(String domain, DomainFilter filter) {
        synchronized (sxpDomains) {
            return Preconditions.checkNotNull(getDomain(Preconditions.checkNotNull(domain)))
                    .addFilter(SxpBindingFilter.generateFilter(Preconditions.checkNotNull(filter), domain));
        }
    }

    /**
     * @param domain     Name of Domain containing Filter
     * @param specific   Filter type specification
     * @param filterName Name of filter
     * @return removed Filter or null if nothing was removed
     */
    public SxpBindingFilter removeFilterFromDomain(String domain, FilterSpecific specific, String filterName) {
        synchronized (sxpDomains) {
            SxpDomain sxpDomain = getDomain(Preconditions.checkNotNull(domain));
            return sxpDomain == null ? null : sxpDomain.removeFilter(Preconditions.checkNotNull(specific),
                    Preconditions.checkNotNull(filterName));
        }
    }

    /**
     * Updated old Filter with new values
     *
     * @param domain    Domain where filter will be updated
     * @param newFilter Filter that will be used from now on
     * @return If filter was updated
     */
    public boolean updateDomainFilter(String domain, DomainFilter newFilter) {
        synchronized (sxpDomains) {
            SxpDomain sxpDomain = getDomain(Preconditions.checkNotNull(domain));
            return sxpDomain != null && sxpDomain.updateFilter(
                    SxpBindingFilter.generateFilter(Preconditions.checkNotNull(newFilter), domain));
        }
    }

    /**
     * Adds and afterward start new Connection
     *
     * @deprecated replaced by {@link #addConnection(Connection, String)}
     * @param connection Connection to be added
     * @throws IllegalArgumentException If Connection exist in Node
     */
    @Deprecated
    public void addConnection(Connection connection) {
        addConnection(connection, DEFAULT_DOMAIN);
    }

    /**
     * Adds and afterward start new Connection
     *
     * @param connection Connection to be added
     * @param domain     Domain where Connection will be added
     *                   if domain does no exist connection wont be added
     * @throws IllegalArgumentException If Connection exist in Node
     */
    public SxpConnection addConnection(Connection connection, String domain) {
        return addConnection(
                SxpConnection.create(this, Preconditions.checkNotNull(connection), Preconditions.checkNotNull(domain)));
    }

    /**
     * Adds and afterwards starts new Connections
     *
     * @deprecated replaced by {@link #addConnections(Connections, String)}
     * @param connections Connections to be added
     */
    @Deprecated
    public void addConnections(Connections connections) {
        addConnections(connections, DEFAULT_DOMAIN);
    }

    /**
     * Adds and afterwards starts new Connections
     *
     * @param connections Connections to be added
     * @param domain      Domain where Connections will be added
     *                    if domain does no exist connection wont be added
     */
    public void addConnections(Connections connections, String domain) {
        if (connections == null || domain == null || connections.getConnection() == null || connections.getConnection()
                .isEmpty()) {
            return;
        }
        connections.getConnection().forEach(c -> addConnection(c, domain));
    }

    /**
     * @param predicate Predicate used for selection of connections
     * @return List of Connections matching specified criteria
     */
    private List<SxpConnection> filterConnections(Predicate<SxpConnection> predicate) {
        List<SxpConnection> connections = new ArrayList<>();
        synchronized (sxpDomains) {
            sxpDomains.values().forEach(d -> connections.addAll(d.getConnections()
                    .stream()
                    .filter(predicate)
                    .collect(Collectors.toList())));
        }
        return Collections.unmodifiableList(connections);
    }

    /**
     * @param predicate Predicate used for selection of connections
     * @param domain    Domain where to look for Connections
     * @return List of Connections matching specified criteria
     */
    private List<SxpConnection> filterConnections(Predicate<SxpConnection> predicate, String domain) {
        if (domain == null) {
            return filterConnections(predicate);
        }
        List<SxpConnection> connections = new ArrayList<>();
        synchronized (sxpDomains) {
            if (sxpDomains.containsKey(Preconditions.checkNotNull(domain))) {
                connections.addAll(sxpDomains.get(domain).getConnections()
                        .stream()
                        .filter(predicate)
                        .collect(Collectors.toList()));
            }
        }
        return Collections.unmodifiableList(connections);
    }

    /**
     * @return All SxpConnections on Node
     */
    public List<SxpConnection> getAllConnections() {
        List<SxpConnection> connections = new ArrayList<>();
        synchronized (sxpDomains) {
            sxpDomains.values().forEach(d -> connections.addAll(d.getConnections()));
        }
        return Collections.unmodifiableList(connections);
    }

    /**
     * @param domain Domain containing Connections
     * @return All SxpConnections on Node
     */
    public List<SxpConnection> getAllConnections(String domain) {
        List<SxpConnection> connections = new ArrayList<>();
        synchronized (sxpDomains) {
            if (sxpDomains.containsKey(Preconditions.checkNotNull(domain))) {
                connections.addAll(sxpDomains.get(domain).getConnections());
            }
        }
        return Collections.unmodifiableList(connections);
    }

    /**
     * @return Ip used by SxpNode
     */
    public InetAddress getSourceIp() {
        return sourceIp;
    }

    /**
     * @return Gets all SxpConnections with state set to DeleteHoldDown
     */
    public List<SxpConnection> getAllDeleteHoldDownConnections() {
        return getAllDeleteHoldDownConnections(null);
    }

    /**
     * @param domain Domain containing connection,
     *               if null gets from all domains
     * @return Gets all SxpConnections with state set to DeleteHoldDown
     */
    public List<SxpConnection> getAllDeleteHoldDownConnections(String domain) {
        return filterConnections(SxpConnection::isStateDeleteHoldDown, domain);
    }

    /**
     * @return Gets all SxpConnections with state set to Off
     */
    public List<SxpConnection> getAllOffConnections() {
        return getAllOffConnections(null);
    }

    /**
     * @param domain Domain containing connection,
     *               if null gets from all domains
     * @return Gets all SxpConnections with state set to Off
     */
    public List<SxpConnection> getAllOffConnections(String domain) {
        return filterConnections(SxpConnection::isStateOff, domain);
    }

    /**
     * @return Gets all SxpConnections with state set to On
     */
    public List<SxpConnection> getAllOnConnections() {
        return getAllOnConnections(null);
    }

    /**
     * @param domain Domain containing connection,
     *               if null gets from all domains
     * @return Gets all SxpConnections with state set to On
     */
    public List<SxpConnection> getAllOnConnections(String domain) {
        return filterConnections(SxpConnection::isStateOn, domain);
    }

    /**
     * @deprecated replaced by {@link #getAllOnListenerConnections(String)}
     * @return Gets all SxpConnections with state set to On and mode Listener or Both
     */
    @Deprecated
    public List<SxpConnection> getAllOnListenerConnections() {
        return getAllOnListenerConnections(null);
    }

    /**
     * @param domain Domain containing connection,
     *               if null gets from all domains
     * @return Gets all SxpConnections with state set to On and mode Listener or Both
     */
    public List<SxpConnection> getAllOnListenerConnections(String domain) {
        return filterConnections(
                connection -> connection.isStateOn(SxpConnection.ChannelHandlerContextType.LISTENER_CNTXT) && (
                        connection.getMode().equals(ConnectionMode.Listener) || connection.isModeBoth()), domain);
    }

    /**
     * @deprecated replaced by {@link #getAllOnSpeakerConnections(String)}
     * @return Gets all SxpConnections with state set to On and mode Speaker or Both
     */
    @Deprecated
    public List<SxpConnection> getAllOnSpeakerConnections() {
        return getAllOnSpeakerConnections(null);
    }

    /**
     * @param domain Domain containing connection,
     *               if null gets from all domains
     * @return Gets all SxpConnections with state set to On and mode Speaker or Both
     */
    public List<SxpConnection> getAllOnSpeakerConnections(String domain) {
        return filterConnections(
                connection -> connection.isStateOn(SxpConnection.ChannelHandlerContextType.SPEAKER_CNTXT) && (
                        connection.getMode().equals(ConnectionMode.Speaker) || connection.isModeBoth()), domain);
    }

    /**
     * @deprecated replaced by {@link #getBindingMasterDatabase(String)}
     * @return Gets MasterDatabase that is used in Node
     */
    @Deprecated
    public MasterDatabaseInf getBindingMasterDatabase() {
        return getBindingMasterDatabase(DEFAULT_DOMAIN);
    }

    /**
     * @deprecated replaced by {@link #getBindingSxpDatabase(String)}
     * @return Gets SxpDatabase that is used in Node
     */
    @Deprecated
    public SxpDatabaseInf getBindingSxpDatabase() {
        return getBindingSxpDatabase(DEFAULT_DOMAIN);
    }

    /**
     * @param domainName Domain containing MasterDatabase
     * @return Gets MasterDatabase that is used in Node
     * @throws DomainNotFoundException if Domain does not exist
     */
    public MasterDatabaseInf getBindingMasterDatabase(String domainName) {
        synchronized (sxpDomains) {
            if (sxpDomains.containsKey(Preconditions.checkNotNull(domainName))) {
                return sxpDomains.get(domainName).getMasterDatabase();
            }
            throw new DomainNotFoundException(getName(), domainName);
        }
    }

    /**
     * @param domainName Domain containing SxpDatabase
     * @return Gets SxpDatabase that is used in Node
     * @throws DomainNotFoundException if Domain does not exist
     */
    public SxpDatabaseInf getBindingSxpDatabase(String domainName) {
        synchronized (sxpDomains) {
            if (sxpDomains.containsKey(Preconditions.checkNotNull(domainName))) {
                return sxpDomains.get(domainName).getSxpDatabase();
            }
            throw new DomainNotFoundException(getName(), domainName);
        }
    }

    /**
     * Gets SxpConnection by its address
     *
     * @param inetSocketAddress InetSocketAddress that is used by SxpConnection
     * @return SxpConnection or null if Node doesn't contains specified address
     * @throws IllegalStateException If found more than 1 SxpConnection
     */
    public SxpConnection getByAddress(final InetSocketAddress inetSocketAddress) {
        Preconditions.checkNotNull(inetSocketAddress);
        List<SxpConnection> sxpConnections = new ArrayList<>();
        synchronized (sxpDomains) {
            sxpDomains.values().forEach(d -> {
                if (d.hasConnection(inetSocketAddress)) {
                    sxpConnections.add(d.getConnection(inetSocketAddress));
                }
            });
        }
        if (sxpConnections.isEmpty()) {
            return null;
        } else if (sxpConnections.size() == 1) {
            return sxpConnections.get(0);
        }
        throw new IllegalStateException("Found multiple Connections on specified address");
    }

    /**
     * Gets SxpConnection by its port
     *
     * @param port Port that is used by SxpConnection
     * @return SxpConnection or null if the Node doesn't contain an address with the specified port
     * @throws IllegalStateException If more than 1 SxpConnection was found
     */
    @Deprecated
    public SxpConnection getByPort(final int port) {
        List<SxpConnection>
                sxpConnections =
                filterConnections(connection -> port == connection.getDestination().getPort());
        if (sxpConnections.isEmpty()) {
            return null;
        } else if (sxpConnections.size() == 1) {
            return sxpConnections.get(0);
        }
        throw new IllegalStateException("Found multiple Connections on specified address");
    }

    /**
     * Gets SxpConnection by its address
     *
     * @param socketAddress SocketAddress that is used by SxpConnection
     * @return SxpConnection if exists
     */
    public SxpConnection getConnection(SocketAddress socketAddress) {
        if (!(socketAddress instanceof InetSocketAddress)) {
            return null;
        }
        return getByAddress((InetSocketAddress) socketAddress);
    }

    /**
     * @return Gets Bindings expansion quantity or zero if disabled
     */
    public int getExpansionQuantity() {
        return getNodeIdentity().getMappingExpanded() != null ? getNodeIdentity().getMappingExpanded() : 0;
    }

    /**
     * @return Gets HoldTime value or zero if disabled
     */
    public int getHoldTime() {
        if (getNodeIdentity().getTimers() == null
                || getNodeIdentity().getTimers().getHoldTime() == null) {
            return 0;
        }
        return getNodeIdentity().getTimers().getHoldTime();
    }

    /**
     * @return Gets HoldTimeMax value or zero if disabled
     */
    public int getHoldTimeMax() {
        if (getNodeIdentity().getTimers() == null
                || getNodeIdentity().getTimers().getHoldTimeMax() == null) {
            return 0;
        }
        return getNodeIdentity().getTimers().getHoldTimeMax();
    }

    /**
     * @return Gets HoldTimeMin value or zero if disabled
     */
    public int getHoldTimeMin() {
        if (getNodeIdentity().getTimers() == null
                || getNodeIdentity().getTimers().getHoldTimeMin() == null) {
            return 0;
        }
        return getNodeIdentity().getTimers().getHoldTimeMin();
    }

    /**
     * @return Gets HoldTimeMinAcceptable value or zero if disabled
     */
    public int getHoldTimeMinAcceptable() {
        if (getNodeIdentity().getTimers() == null
                || getNodeIdentity().getTimers().getHoldTimeMinAcceptable() == null) {
            return 0;
        }
        return getNodeIdentity().getTimers().getHoldTimeMinAcceptable();
    }

    /**
     * @return Gets KeepAlive value or zero if disabled
     */
    public int getKeepAliveTime() {
        if (getNodeIdentity().getTimers() == null
                || getNodeIdentity().getTimers().getKeepAliveTime() == null) {
            return 0;
        }
        return getNodeIdentity().getTimers().getKeepAliveTime();
    }

    /**
     * @return Gets Name of Node
     */
    public String getName() {
        return getNodeIdentity().getName() == null || getNodeIdentity().getName().isEmpty() ? NodeIdConv.toString(
                nodeId) : getNodeIdentity().getName();
    }

    /**
     * @return Gets NodeId
     */
    public NodeId getNodeId() {
        return nodeId;
    }

    /**
     * @return Gets Password used to connect to peers or null if disabled
     */
    public String getPassword() {
        return getNodeIdentity().getSecurity() != null ? getNodeIdentity().getSecurity().getPassword() : null;
    }

    /**
     * @return Gets RetryOpen value or zero if disabled
     */
    public int getRetryOpenTime() {
        if (getNodeIdentity().getTimers() == null || getNodeIdentity().getTimers().getRetryOpenTime() == null) {
            return 0;
        }
        return getNodeIdentity().getTimers().getRetryOpenTime();
    }

    /**
     * @return Gets Node server port or -1 if dissabled
     */
    public int getServerPort() {
        if (getNodeIdentity().getTcpPort() == null || getNodeIdentity().getTcpPort().getValue() == null) {
            return -1;
        }
        return getNodeIdentity().getTcpPort().getValue();
    }

    /**
     * Gets SxpNode specific Timer
     *
     * @param timerType Type of Timer
     * @return TimerType or null if not present
     */
    public ListenableScheduledFuture<?> getTimer(TimerType timerType) {
        return timers.get(timerType);
    }

    /**
     * @return Gets Version of of Node
     */
    public Version getVersion() {
        return getNodeIdentity().getVersion() != null ? getNodeIdentity().getVersion() : Version.Version4;
    }

    /**
     * @return If Node is enabled
     */
    public boolean isEnabled() {
        return Objects.nonNull(serverChannel) && serverChannel.isActive();
    }

    /**
     * Start all Connections that are in state Off
     */
    public synchronized ListenableFuture openConnections() {
        if (!isEnabled()) {
            return Futures.immediateCancelledFuture();
        }
        final SxpNode node = this;

        return worker.executeTask(() -> {
            int connectionsAllSize = getAllConnections().size();
            int connectionsOnSize = getAllOnConnections().size();
            List<SxpConnection> connections = filterConnections(INACTIVE_CONNECTION_FILTER::test);
            LOG.info("{} Opening connections [X/O/All=\"{}/{}/{}\"]", node, connections.size(), connectionsOnSize,
                    connectionsAllSize);
            for (SxpConnection connection : connections) {
                connection.openConnection();
            }
        }, ThreadsWorker.WorkerType.DEFAULT);
    }

    /**
     * @return BindingDispatcher routine used by current Node
     */
    public BindingDispatcher getSvcBindingDispatcher() {
        return svcBindingDispatcher;
    }

    /**
     * @return BindingHandler routine used by current Node
     */
    public BindingHandler getSvcBindingHandler() {
        return svcBindingHandler;
    }

    /**
     * Adds Bindings to database
     *
     * @param bindings   MasterDatabase containing bindings that will be added
     * @param domainName Domain where bindings wil be added
     * @throws DomainNotFoundException if Domain does not exist
     */
    public List<MasterDatabaseBinding> putBindingsMasterDatabase(List<MasterDatabaseBinding> bindings,
            String domainName) {
        final SxpDomain sxpDomain = getDomain(domainName);
        if (sxpDomain == null) {
            throw new DomainNotFoundException(getName(), domainName);
        }
        List<MasterDatabaseBinding> addedBindings;
        synchronized (sxpDomain) {
            addedBindings = sxpDomain.getMasterDatabase().addBindings(bindings);
            sxpDomain.pushToSharedMasterDatabases(Collections.emptyList(), bindings);
        }
        return addedBindings;
    }

    /**
     * Removes Bindings from database
     *
     * @param bindings   MasterDatabase bindings that will be removed
     * @param domainName Domain from which bindings will be removed
     * @throws DomainNotFoundException if Domain does not exist
     */
    public List<MasterDatabaseBinding> removeBindingsMasterDatabase(List<MasterDatabaseBinding> bindings,
            String domainName) {
        final SxpDomain sxpDomain = getDomain(domainName);
        if (sxpDomain == null) {
            throw new DomainNotFoundException(getName(), domainName);
        }
        Map<NodeId, SxpBindingFilter> filterMap = SxpDatabase.getInboundFilters(this, domainName);
        List<MasterDatabaseBinding> deletedBindings;
        synchronized (sxpDomain) {
            deletedBindings = sxpDomain.getMasterDatabase().deleteBindings(bindings);
            sxpDomain.getMasterDatabase().addBindings(
                            SxpDatabase.getReplaceForBindings(deletedBindings, sxpDomain.getSxpDatabase(), filterMap));
            sxpDomain.pushToSharedMasterDatabases(bindings, Collections.emptyList());
        }
        return deletedBindings;
    }

    /**
     * Remove and afterwards shutdown connection
     *
     * @param destination InetSocketAddress that is used by SxpConnection
     * @return Removed SxpConnection
     */
    public SxpConnection removeConnection(InetSocketAddress destination) {
        SxpConnection connection = null;
        synchronized (sxpDomains) {
            for (SxpDomain domain : sxpDomains.values()) {
                if (domain.hasConnection(destination)) {
                    connection = domain.removeConnection(destination);
                    break;
                }
            }
        }
        if (connection != null) {
            connection.shutdown();
            if (connection.hasNonEmptyPassword()) {
                updateMD5keys();
            }
        }
        return connection;
    }

    /**
     * Sets Security password used to connect, and new SslContext
     *
     * @param security Security to be set
     */
    public void setSecurity(
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.Security security) {
        SecurityBuilder securityBuilder = new SecurityBuilder(security);
        if (Objects.isNull(securityBuilder.getPassword())) {
            securityBuilder.setPassword("");
        }
        securityBuilder.setMd5Digest(Security.getMD5s(securityBuilder.getPassword()));
        this.sslContextFactory = new SslContextFactory(securityBuilder.getTls());
        this.nodeBuilder.setSecurity(securityBuilder.build());
    }

    /**
     * Set the period of retry open timer. Cancels already scheduled retry timer if present.
     *
     * @param retryOpenTimerPeriod a period of retries in seconds
     */
    public void setRetryOpenTimerPeriod(int retryOpenTimerPeriod) {
        nodeBuilder.setTimers(new TimersBuilder(nodeBuilder.getTimers()).setRetryOpenTime(retryOpenTimerPeriod).build());
        for (SxpDomain domain : sxpDomains.values()) {
            for (SxpConnection con : domain.getConnections()) {
                con.stopRetryOpenTimer();
                if (con.isStateOff()) {
                    con.scheduleRetryOpen();
                }
            }
        }
    }

    /**
     * Set max number of attributes exported in each Update Message.
     *
     * @param size Size which will be used for partitioning
     * @throws IllegalArgumentException If size of partitioning is bellow 2 or above 150
     */
    public void setMessagePartitionSize(int size) {
        svcBindingDispatcher.setPartitionSize(size);
    }

    /**
     * Set max number of Add update messages to be merged together.
     *
     * @param size Size which will be used for message joining
     * @throws IllegalArgumentException If size of message merge is bellow 1
     */
    public void setMessageMergeSize(int size) {
        svcBindingHandler.setBufferLimit(size);
    }

    /**
     * Gets Execution handler of current Node
     *
     * @return ThreadsWorker reference
     */
    public ThreadsWorker getWorker() {
        return worker;
    }

    /**
     * Shutdown all Connections
     */
    public void shutdownConnections() {
        getDomains().forEach(SxpDomain::close);
    }

    /**
     * Administratively shutdown.
     */
    public synchronized ListenableFuture<Boolean> shutdown() {
        LOG.info("Shutting down server for SXP node {}", this);
        if (!isEnabled()) {
            LOG.info("{} Server already stopped", this);
            return Futures.immediateFuture(Boolean.FALSE);
        }
        final ChannelFuture close = serverChannel.close();
        close.awaitUninterruptibly();
        if (close.isCancelled()) {
            LOG.warn("{} Server could not be stopped because operation was cancelled", this);
            return Futures.immediateFuture(Boolean.FALSE);
        } else if (!close.isSuccess()) {
            LOG.error("{} Server could not be stopped because {}", this, close.cause());
            return Futures.immediateFuture(Boolean.FALSE);
        } else {
            LOG.info("{} Server successfully stopped", this);
            setRetryOpenTimerPeriod(0);
            shutdownConnections();
            return Futures.immediateFuture(Boolean.TRUE);
        }
    }

    /**
     * Start SxpNode
     */
    public synchronized ListenableFuture<Boolean> start() {
        LOG.info("Starting server for SXP node {}", this);
        this.sourceIp = InetAddresses.forString(Search.getAddress(getNodeIdentity().getSourceIp()));
        if (isEnabled()) {
            LOG.info("{} Server already created [{}:{}]", this, getSourceIp().getHostAddress(), getServerPort());
            return Futures.immediateFuture(Boolean.FALSE);
        }
        ChannelFuture create = ConnectFacade.createServer(
                this, handlerFactoryServer, ConnectFacade.collectAllPasswords(this));
        create.awaitUninterruptibly();
        if (create.isCancelled()) {
            LOG.warn("{} Server could not be created [{}:{}] because operation was cancelled",
                    this, getSourceIp().getHostAddress(), getServerPort());
            return Futures.immediateFuture(Boolean.FALSE);
        } else if (!create.isSuccess()) {
            LOG.error("{} Server could not be created [{}:{}] because {}",
                    this, getSourceIp().getHostAddress(), getServerPort(), create.cause());
            return Futures.immediateFuture(Boolean.FALSE);
        } else {
            serverChannel = create.channel();
            LOG.info("{} Server successfully created [{}:{}]", this, getSourceIp().getHostAddress(),
                    getServerPort());
            return Futures.immediateFuture(Boolean.TRUE);
        }
    }

    /**
     * Updates TCP-MD5 keys of SxpNode.
     * This method is thread safe and uses fast path optimization to avoid unnecessary server restarts in high-load
     * situations.
     * As a result, two situations may occur when calling this method. If the calling thread:
     * 1. is the first to arrive at the critical section, it sets the processing flag on and restarts the server
     *  with new keyset
     * 2. invokes the method while the processing flag is on, it returns side-effect free
     *
     * The server restart is guarded by a separate mutex, so successive invocations of this method resulting in
     * a server restart will block until previous restarts are done. However, please do note, this method does not
     * guarantee fair order of such invocations.
     *
     */
    public void updateMD5keys() {
        worker.executeTask(this::doUpdateMD5keys, ThreadsWorker.WorkerType.DEFAULT);
    }

    /**
     * Updates TCP-MD5 keys of SxpNode.
     * This method is thread safe and uses fast path optimization to avoid unnecessary server restarts in high-load
     * situations.
     * As a result, two situations may occur when calling this method. If the calling thread:
     * 1. is the first to arrive at the critical section, it sets the processing flag on and restarts the server
     *  with new keyset
     * 2. invokes the method while the processing flag is on, it returns side-effect free
     *
     * The server restart is guarded by a separate mutex, so successive invocations of this method resulting in
     * a server restart will block until previous restarts are done. However, please do note, this method does not
     * guarantee fair order of such invocations.
     *
     */
    private void doUpdateMD5keys() {
        LOG.info("{} - Updating MD5 keys", this);
        md5UpdateLock.lock();
        try {
            if (processingConnectionData) {
                return;
            }
            processingConnectionData = true;
        } finally {
            md5UpdateLock.unlock();
        }

        if (!isEnabled()) {
            LOG.info("Skipping MD5 update - SXP node {} is not enabled", this);
            return;
        }

        Map<InetAddress, byte[]> keyMapping;
        md5UpdateLock.lock();
        try { // exclusive password extraction from all node connections
            //revoke processing flag
            processingConnectionData = false;
            //read keyMap
            keyMapping = ConnectFacade.collectAllPasswords(this);
        } finally {
            md5UpdateLock.unlock();
        }

        synchronized (md5UpdateLock) {
            if (serverChannel != null) {
                LOG.debug("{} - shutting down the server", this);
                serverChannel.close().awaitUninterruptibly();
            }
            serverChannel = ConnectFacade.createServer(this, handlerFactoryServer, keyMapping)
                    .awaitUninterruptibly().channel();
            LOG.info("{} Server created", this);
        }
    }

    /**
     * @return ContextFactory used for establishing SSL connections
     */
    public SslContextFactory getSslContextFactory() {
        return sslContextFactory;
    }

    @Override
    public String toString() {
        return '[' + (
                nodeBuilder.getName() != null && !nodeBuilder.getName().isEmpty() ? (nodeBuilder.getName() + ':') : "")
                + NodeIdConv.toString(nodeId) + ']';
    }
}
