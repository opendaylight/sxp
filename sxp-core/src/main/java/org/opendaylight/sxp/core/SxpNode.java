/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.core.handler.MessageDecoder;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.core.service.BindingHandler;
import org.opendaylight.sxp.core.service.ConnectFacade;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.SxpDatabase;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.sxp.util.time.SxpTimerTask;
import org.opendaylight.sxp.util.time.node.RetryOpenTimerTask;
import org.opendaylight.tcpmd5.jni.NativeSupportUnavailableException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.sxp.peers.SxpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final Logger LOG = LoggerFactory.getLogger(SxpNode.class.getName());

    protected static final long THREAD_DELAY = 10;

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
     *
     * @param nodeId         ID of newly created Node
     * @param node           Node setup data
     * @param masterDatabase Data which will be added to Master-DB
     * @param sxpDatabase    Data which will be added to SXP-DB
     * @param worker         Thread workers which will be executing task inside SxpNode
     * @return New instance of SxpNode
     */
    public static SxpNode createInstance(NodeId nodeId, SxpNodeIdentity node, MasterDatabaseInf masterDatabase,
            SxpDatabaseInf sxpDatabase, ThreadsWorker worker) {
        return new SxpNode(nodeId, node, masterDatabase, sxpDatabase, worker);
    }

    private final Map<InetSocketAddress, SxpConnection>
            addressToSxpConnection =
            new HashMap<>(Configuration.getConstants().getNodeConnectionsInitialSize());

    private final MasterDatabaseInf _masterDatabase;
    private final SxpDatabaseInf _sxpDatabase;

    private final HandlerFactory handlerFactoryClient = new HandlerFactory(MessageDecoder.createClientProfile(this));
    private final HandlerFactory handlerFactoryServer = new HandlerFactory(MessageDecoder.createServerProfile(this));

    private SxpNodeIdentityBuilder nodeBuilder;
    private NodeId nodeId;

    private Channel serverChannel;
    protected InetAddress sourceIp;

    private final BindingDispatcher svcBindingDispatcher;
    private final BindingHandler svcBindingHandler;
    private final ThreadsWorker worker;

    /**
     * Common timers setup.
     */
    private HashMap<TimerType, ListenableScheduledFuture<?>> timers = new HashMap<>(6);
    private final Map<String, SxpPeerGroupBuilder> peerGroupMap = new HashMap<>();

    /**
     * Default constructor that creates and start SxpNode using provided values
     *
     * @param nodeId         ID of newly created Node
     * @param node           Node setup data
     * @param masterDatabase Data which will be added to Master-DB
     * @param sxpDatabase    Data which will be added to SXP-DB
     * @param worker         Thread workers which will be executing task inside SxpNode
     */
    protected SxpNode(NodeId nodeId, SxpNodeIdentity node, MasterDatabaseInf masterDatabase, SxpDatabaseInf sxpDatabase,
            ThreadsWorker worker) {
        this.nodeBuilder = new SxpNodeIdentityBuilder(Preconditions.checkNotNull(node));
        this.nodeId = Preconditions.checkNotNull(nodeId);
        this.worker = Preconditions.checkNotNull(worker);
        this._masterDatabase = Preconditions.checkNotNull(masterDatabase);
        this._sxpDatabase = Preconditions.checkNotNull(sxpDatabase);
        this.svcBindingDispatcher = new BindingDispatcher(this);
        this.svcBindingHandler = new BindingHandler(this, this.svcBindingDispatcher);
    }

    protected SxpNodeIdentity getNodeIdentity() {
        return nodeBuilder.build();
    }

    protected void addConnection(SxpConnection connection) {
        synchronized (peerGroupMap) {
            for (SxpPeerGroup peerGroup : getPeerGroup(connection)) {
                for (SxpFilter filter : peerGroup.getSxpFilter()) {
                    connection.putFilter(SxpBindingFilter.generateFilter(filter, peerGroup.getName()));
                }
            }
        }
        synchronized (addressToSxpConnection) {
            if (addressToSxpConnection.containsKey(connection.getDestination())) {
                throw new IllegalArgumentException(
                        "Connection " + connection + " with destination " + connection.getDestination() + " exist.");
            }
            addressToSxpConnection.put(connection.getDestination(), connection);
        }
        updateMD5keys(connection);
        openConnection(connection);
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
                            sxpPeer.getPeerPort() == null ? 64999 : sxpPeer.getPeerPort().getValue()));
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
                Set<SxpBindingFilter> filters = new HashSet<>();
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
                    for (SxpBindingFilter filter : filters) {
                        connection.putFilter(filter);
                    }
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
            return Collections2.transform(peerGroupMap.values(), new Function<SxpPeerGroupBuilder, SxpPeerGroup>() {

                @Nullable @Override public SxpPeerGroup apply(SxpPeerGroupBuilder input) {
                    return input != null ? input.build() : null;
                }
            });
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
            SxpBindingFilter bindingFilter = SxpBindingFilter.generateFilter(sxpFilter, peerGroupName);
            List<SxpFilter> sxpFilters = peerGroup.getSxpFilter();
            for (SxpFilter filter : sxpFilters) {
                if (SxpBindingFilter.checkInCompatibility(filter, sxpFilter)) {
                    LOG.warn("{} Filter of type {} already defined", this, sxpFilter.getFilterType());
                    return false;
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
                if (SxpBindingFilter.checkInCompatibility(filter, newFilter) && filter.getFilterEntries()
                        .getClass()
                        .equals(filter.getFilterEntries().getClass())) {
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
                                f.getFilterType().equals(filterType) && specific.equals(f.getFilterSpecific());
                if (remove) {
                    connections.forEach(c -> c.removeFilter(filterType, f.getFilterSpecific()));
                }
                return remove;
            });
        }
    }

    /**
     * Adds and afterward start new Connection
     *
     * @param connection Connection to be added
     * @throws IllegalArgumentException If Connection exist in Node
     */
    public void addConnection(Connection connection) {
        addConnection(SxpConnection.create(this, Preconditions.checkNotNull(connection)));
    }

    /**
     * Adds and afterwards starts new Connections
     *
     * @param connections Connections to be added
     */
    public void addConnections(Connections connections) {
        if (connections == null || connections.getConnection() == null || connections.getConnection().isEmpty()) {
            return;
        }
        for (Connection connection : connections.getConnection()) {
            addConnection(connection);
        }
    }

    private List<SxpConnection> filterConnections(Predicate<SxpConnection> predicate) {
        Collection<SxpConnection> connections;
        synchronized (addressToSxpConnection) {
            connections = Collections2.filter(addressToSxpConnection.values(), predicate);
        }
        return Collections.unmodifiableList(new ArrayList<>(connections));
    }

    /**
     * @return All SxpConnections on Node
     */
    public List<SxpConnection> getAllConnections() {
        synchronized (addressToSxpConnection) {
            return Collections.unmodifiableList(new ArrayList<>(addressToSxpConnection.values()));
        }
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
        return filterConnections(new Predicate<SxpConnection>() {

            @Override public boolean apply(SxpConnection sxpConnection) {
                return sxpConnection.isStateDeleteHoldDown();
            }
        });
    }

    /**
     * @return Gets all SxpConnections with state set to Off
     */
    public List<SxpConnection> getAllOffConnections() {
        return filterConnections(new Predicate<SxpConnection>() {

            @Override public boolean apply(SxpConnection sxpConnection) {
                return sxpConnection.isStateOff();
            }
        });
    }

    /**
     * @return Gets all SxpConnections with state set to On
     */
    public List<SxpConnection> getAllOnConnections() {
        return filterConnections(new Predicate<SxpConnection>() {

            @Override public boolean apply(SxpConnection sxpConnection) {
                return sxpConnection.isStateOn();
            }
        });
    }

    /**
     * @return Gets all SxpConnections with state set to On and mode Listener or Both
     */
    public List<SxpConnection> getAllOnListenerConnections() {
        return filterConnections(new Predicate<SxpConnection>() {

            @Override public boolean apply(SxpConnection connection) {
                return connection.isStateOn(SxpConnection.ChannelHandlerContextType.ListenerContext) && (
                        connection.getMode().equals(ConnectionMode.Listener) || connection.isModeBoth());
            }
        });
    }

    /**
     * @return Gets all SxpConnections with state set to On and mode Speaker or Both
     */
    public List<SxpConnection> getAllOnSpeakerConnections() {
        return filterConnections(new Predicate<SxpConnection>() {

            @Override public boolean apply(SxpConnection connection) {
                return connection.isStateOn(SxpConnection.ChannelHandlerContextType.SpeakerContext) && (
                        connection.getMode().equals(ConnectionMode.Speaker) || connection.isModeBoth());
            }
        });
    }

    /**
     * @return Gets MasterDatabase that is used in Node
     */
    public MasterDatabaseInf getBindingMasterDatabase() {
        return _masterDatabase;
    }

    /**
     * @return Gets SxpDatabase that is used in Node
     */
    public SxpDatabaseInf getBindingSxpDatabase() {
        return _sxpDatabase;
    }

    /**
     * Gets SxpConnection by its address
     *
     * @param inetSocketAddress InetSocketAddress that is used by SxpConnection
     * @return SxpConnection or null if Node doesn't contains specified address
     * @throws IllegalStateException If found more than 1 SxpConnection
     */
    public SxpConnection getByAddress(final InetSocketAddress inetSocketAddress) {
        List<SxpConnection> sxpConnections = filterConnections(new Predicate<SxpConnection>() {

            @Override public boolean apply(SxpConnection connection) {
                return inetSocketAddress.getAddress().equals(connection.getDestination().getAddress());
            }
        });
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
     * @return SxpConnection or null if Node doesn't contains address with specified port
     * @throws IllegalStateException If found more than 1 SxpConnection
     */
    public SxpConnection getByPort(final int port) {
        List<SxpConnection> sxpConnections = filterConnections(new Predicate<SxpConnection>() {

            @Override public boolean apply(SxpConnection connection) {
                return port == connection.getDestination().getPort();
            }
        });
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
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        synchronized (addressToSxpConnection) {
            SxpConnection connection = addressToSxpConnection.get(inetSocketAddress);
            if (connection == null) {
                connection = getByAddress(inetSocketAddress);
            }
            return connection;
        }
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
        if (getNodeIdentity().getTimers() == null || getNodeIdentity().getTimers() == null
                || getNodeIdentity().getTimers().getHoldTime() == null) {
            return 0;
        }
        return getNodeIdentity().getTimers().getHoldTime();
    }

    /**
     * @return Gets HoldTimeMax value or zero if disabled
     */
    public int getHoldTimeMax() {
        if (getNodeIdentity().getTimers() == null || getNodeIdentity().getTimers() == null
                || getNodeIdentity().getTimers().getHoldTimeMax() == null) {
            return 0;
        }
        return getNodeIdentity().getTimers().getHoldTimeMax();
    }

    /**
     * @return Gets HoldTimeMin value or zero if disabled
     */
    public int getHoldTimeMin() {
        if (getNodeIdentity().getTimers() == null || getNodeIdentity().getTimers() == null
                || getNodeIdentity().getTimers().getHoldTimeMin() == null) {
            return 0;
        }
        return getNodeIdentity().getTimers().getHoldTimeMin();
    }

    /**
     * @return Gets HoldTimeMinAcceptable value or zero if disabled
     */
    public int getHoldTimeMinAcceptable() {
        if (getNodeIdentity().getTimers() == null || getNodeIdentity().getTimers() == null
                || getNodeIdentity().getTimers().getHoldTimeMinAcceptable() == null) {
            return 0;
        }
        return getNodeIdentity().getTimers().getHoldTimeMinAcceptable();
    }

    /**
     * @return Gets KeepAlive value or zero if disabled
     */
    public int getKeepAliveTime() {
        if (getNodeIdentity().getTimers() == null || getNodeIdentity().getTimers() == null
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
        return getNodeIdentity().getPassword();
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
        return serverChannel != null && serverChannel.isActive();
    }

    /**
     * Start all Connections that are in state Off
     */
    public void openConnections() {
        // Server not created yet.
        if (serverChannel == null) {
            return;
        }

        final SxpNode node = this;

        final int connectionsAllSize = getAllConnections().size();
        final int connectionsOnSize = getAllOnConnections().size();
        final List<SxpConnection> connections = getAllOffConnections();

        worker.executeTask(new Runnable() {

            @Override public void run() {
                LOG.info(node + " Open connections [X/O/All=\"" + connections.size() + "/" + connectionsOnSize + "/"
                        + connectionsAllSize + "\"]");
                for (final SxpConnection connection : connections) {
                    openConnection(connection);
                }
            }
        }, ThreadsWorker.WorkerType.DEFAULT);
    }

    /**
     * Connect specified connection to remote peer
     *
     * @param connection Connection containing necessary information for connecting to peer
     */
    public void openConnection(final SxpConnection connection) {
        if (Preconditions.checkNotNull(connection).isStateOff()) {
            try {
                ConnectFacade.createClient(this, connection, handlerFactoryClient);
            } catch (NativeSupportUnavailableException e) {
                LOG.warn(connection + " {}", e.getMessage());
            }
        }
    }

    BindingDispatcher getSvcBindingDispatcher() {
        return svcBindingDispatcher;
    }

    BindingHandler getSvcBindingHandler() {
        return svcBindingHandler;
    }

    /**
     * Adds Bindings to database as Local bindings
     *
     * @param bindings MasterDatabase containing bindings that will be added
     */
    public List<MasterDatabaseBinding> putLocalBindingsMasterDatabase(List<MasterDatabaseBinding> bindings) {
        List<MasterDatabaseBinding> addedBindings = getBindingMasterDatabase().addLocalBindings(bindings);
        svcBindingDispatcher.propagateUpdate(null, addedBindings, getAllOnSpeakerConnections());
        return addedBindings;
    }

    /**
     * Removes Local Bindings from database
     *
     * @param bindings MasterDatabase containing bindings that will be removed
     */
    public List<MasterDatabaseBinding> removeLocalBindingsMasterDatabase(List<MasterDatabaseBinding> bindings) {
        Map<NodeId, SxpBindingFilter> filterMap = SxpDatabase.getInboundFilters(this);
        synchronized (getBindingSxpDatabase()) {
            List<MasterDatabaseBinding> deletedBindings = getBindingMasterDatabase().deleteBindingsLocal(bindings);
            svcBindingDispatcher.propagateUpdate(deletedBindings, getBindingMasterDatabase().addBindings(
                    SxpDatabase.getReplaceForBindings(deletedBindings, getBindingSxpDatabase(), filterMap)),
                    getAllOnSpeakerConnections());
            return deletedBindings;
        }
    }

    /**
     * Remove and afterwards shutdown connection
     *
     * @param destination InetSocketAddress that is used by SxpConnection
     * @return Removed SxpConnection
     */
    public SxpConnection removeConnection(InetSocketAddress destination) {
        SxpConnection connection;
        synchronized (addressToSxpConnection) {
            connection = addressToSxpConnection.remove(destination);
        }
        if (connection != null) {
            connection.shutdown();
            updateMD5keys(connection);
        }
        return connection;
    }

    /**
     * Sets SxpNode specific Timer
     *
     * @param timerType Type of Timer that will be set
     * @param period    Time period to wait till execution in Seconds
     * @return ListenableScheduledFuture callback
     * @throws UnknownTimerTypeException If current TimerType isn't supported
     */
    public ListenableScheduledFuture<?> setTimer(TimerType timerType, int period)
            throws UnknownTimerTypeException {
        synchronized (timers) {
            SxpTimerTask timer;
            switch (timerType) {
                case RetryOpenTimer:
                    timer = new RetryOpenTimerTask(this, period);
                    break;
                default:
                    throw new UnknownTimerTypeException(timerType);
            }
            ListenableScheduledFuture<?> timer_ = getTimer(timerType);
            if (period > 0 && (timer_ == null || !timer_.isCancelled())) {
                return this.setTimer(timerType, getWorker().scheduleTask(timer, period, TimeUnit.SECONDS));
            } else {
                return this.setTimer(timerType, null);
            }
        }
    }

    /**
     * Sets SxpNode specific Timer
     *
     * @param timerType Type of Timer that will be set
     * @param timer     Timer logic
     * @return ListenableScheduledFuture callback
     */
    private ListenableScheduledFuture<?> setTimer(TimerType timerType, ListenableScheduledFuture<?> timer) {
        ListenableScheduledFuture<?> t = this.timers.put(timerType, timer);
        if (t != null && !t.isDone()) {
            t.cancel(false);
        }
        return timer;
    }

    /**
     * Set max number of attributes exported in each Update Message.
     *
     * @param size Size which will be used for partitioning
     * @throws IllegalArgumentException If size of partitioning is bellow 2 or above 150
     */
    public void setMessagePartitionSize(int size) throws IllegalArgumentException {
        svcBindingDispatcher.setPartitionSize(size);
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
     * Administratively shutdown.
     */
    public synchronized SxpNode shutdown() {
        // Wait until server channel ends its own initialization.
        while (serverChannelInit.get()) {
            try {
                wait(THREAD_DELAY);
            } catch (InterruptedException e) {
                LOG.warn("{} Error while shut down ", this, e);
            }
        }
        setTimer(TimerType.RetryOpenTimer, 0);
        shutdownConnections();

        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }
        return this;
    }

    /**
     * Shutdown all Connections
     */
    public void shutdownConnections() {
        synchronized (addressToSxpConnection) {
            for (SxpConnection connection : addressToSxpConnection.values()) {
                if (!connection.isStateOff()) {
                    connection.shutdown();
                }
            }
        }
    }

    private final AtomicBoolean serverChannelInit = new AtomicBoolean(false);

    /**
     * Start SxpNode
     */
    public synchronized SxpNode start() {
        if (isEnabled() || serverChannelInit.getAndSet(true)) {
            return this;
        }
        this.sourceIp = InetAddresses.forString(Search.getAddress(getNodeIdentity().getSourceIp()));
        final SxpNode node = this;
        worker.executeTask(() -> {
            SxpNodeIdentity identity = getNodeIdentity();
            if (identity.getMasterDatabase() != null && getBindingMasterDatabase().getLocalBindings().isEmpty()) {
                putLocalBindingsMasterDatabase(identity.getMasterDatabase().getMasterDatabaseBinding());
            }
            if (identity.getSxpPeerGroups() != null && identity.getSxpPeerGroups().getSxpPeerGroup() != null) {
                identity.getSxpPeerGroups()
                        .getSxpPeerGroup()
                        .forEach(g -> addPeerGroup(new SxpPeerGroupBuilder(g).build()));
            }
            ConnectFacade.createServer(node, handlerFactoryServer).addListener(new ChannelFutureListener() {

                @Override public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        serverChannel = channelFuture.channel();
                        LOG.info(node + " Server created [" + getSourceIp().getHostAddress() + ":" + getServerPort()
                                + "]");
                        addConnections(getNodeIdentity().getConnections());
                        node.setTimer(TimerType.RetryOpenTimer, node.getRetryOpenTime());
                    } else {
                        LOG.info(node + " Server [" + node.getSourceIp().getHostAddress() + ":" + getServerPort()
                                + "] Could not be created " + channelFuture.cause());
                    }
                    serverChannelInit.set(false);
                }
            });
        }, ThreadsWorker.WorkerType.DEFAULT);
        return this;
    }

    private final AtomicInteger updateMD5counter = new AtomicInteger();

    private void updateMD5keys(final SxpConnection connection) {
        if (serverChannel == null || connection.getPassword() == null || connection.getPassword().isEmpty()) {
            return;
        }
        updateMD5counter.incrementAndGet();
        final SxpNode sxpNode = this;
        if (isEnabled() && !serverChannelInit.getAndSet(true)) {
            LOG.info("{} Updating MD5 keys", this);
            serverChannel.close().addListener(createMD5updateListener(sxpNode));
        }
    }

    private ChannelFutureListener createMD5updateListener(final SxpNode sxpNode) {
        return future -> ConnectFacade.createServer(sxpNode, handlerFactoryServer)
                .addListener(new ChannelFutureListener() {

                    @Override public void operationComplete(ChannelFuture future) throws Exception {
                        serverChannel = future.channel();
                        serverChannelInit.set(false);
                        if (updateMD5counter.decrementAndGet() > 0) {
                            serverChannel.close().addListener(createMD5updateListener(sxpNode));
                        }
                    }
                });
    }

    @Override public String toString() {
        return "[" + (
                getNodeIdentity().getName() != null && !getNodeIdentity().getName().isEmpty() ?
                        getNodeIdentity().getName() + ":" : "") + NodeIdConv.toString(nodeId) + "]";
    }
}
