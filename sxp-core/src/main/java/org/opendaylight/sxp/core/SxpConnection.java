/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import io.netty.channel.ChannelHandlerContext;
import org.opendaylight.sxp.core.behavior.Context;
import org.opendaylight.sxp.core.messaging.AttributeList;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.core.service.BindingHandler;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.SxpDatabase;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.exception.connection.IncompatiblePeerModeException;
import org.opendaylight.sxp.util.exception.connection.SocketAddressNotRecognizedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeNotFoundException;
import org.opendaylight.sxp.util.exception.unknown.UnknownConnectionModeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.sxp.util.time.SxpTimerTask;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.sxp.util.time.connection.DeleteHoldDownTimerTask;
import org.opendaylight.sxp.util.time.connection.HoldTimerTask;
import org.opendaylight.sxp.util.time.connection.KeepAliveTimerTask;
import org.opendaylight.sxp.util.time.connection.ReconcilationTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.PasswordType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.capabilities.fields.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorSubCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.HoldTimeAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.SxpNodeIdAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessageLegacy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * SxpConnection class represent SxpPeer and contains logic for maintaining communication
 */
public class SxpConnection {

    /**
     * ChannelHandlerContextType enum specifies role of ChannelHandlerContext
     */
    public enum ChannelHandlerContextType {
        ListenerContext, None, SpeakerContext
    }

    protected static final Logger LOG = LoggerFactory.getLogger(SxpConnection.class.getName());

    /**
     * Creates SxpConnection using provided values
     *
     * @param owner      SxpNode to be set as owner
     * @param connection Connection that contains settings
     * @return SxpConnection created by specified values
     * @throws UnknownVersionException If version in provided values isn't supported
     */
    public static SxpConnection create(SxpNode owner, Connection connection) throws UnknownVersionException {
        return new SxpConnection(owner, connection);
    }

    private ConnectionBuilder connectionBuilder;
    private Context context;

    private final List<ChannelHandlerContext> initCtxs = new ArrayList<>(2);
    private final HashMap<ChannelHandlerContextType, ChannelHandlerContext> ctxs = new HashMap<>(2);
    private final List<CapabilityType> remoteCapabilityTypes = new ArrayList<>();

    protected InetSocketAddress localAddress, remoteAddress;

    private final SxpNode owner;

    protected HashMap<TimerType, ListenableScheduledFuture<?>> timers = new HashMap<>(5);
    private final Map<FilterType, Map<FilterSpecific, SxpBindingFilter>> bindingFilterMap =
        new HashMap<>(FilterType.values().length);

    /**
     * @param filterType Type of SxpBindingFilter to look for
     * @return Filter with specified type or null if connection doesnt have one
     */
    public SxpBindingFilter getFilter(FilterType filterType) {
        synchronized (bindingFilterMap) {
            return SxpBindingFilter.mergeFilters(bindingFilterMap.get(filterType).values());
        }
    }

    /**
     * Defines how to setup flags after filter of specific type is set in SxpConnection
     *
     * @param filterType Type of SxpBindingFilter that was set
     */
    private void updateFlagsForDatabase(final FilterType filterType, boolean filterRemoved) {
        if (!isStateOn() || filterType.equals(FilterType.Outbound) && !isModeSpeaker() ||
                (filterType.equals(FilterType.Inbound) || filterType.equals(FilterType.InboundDiscarding))
                        && !isModeListener()) {
            return;
        }
        final SxpDatabaseInf sxpDatabase = owner.getBindingSxpDatabase();
        final MasterDatabaseInf masterDatabase = owner.getBindingMasterDatabase();
        if (filterType.equals(FilterType.Outbound)) {
            //Sends PurgeAll, All bindings in this order
            List<SxpConnection> connections = new ArrayList<>();
            connections.add(this);
            owner.getWorker()
                    .addListener(BindingDispatcher.sendPurgeAllMessage(this), () -> getOwner().getSvcBindingDispatcher()
                            .propagateUpdate(null, masterDatabase.getBindings(), connections));
        } else if (filterRemoved && filterType.equals(FilterType.Inbound)) {
            //Adds all Bindings learned from peer to MasterDB and sends it to All Listeners
            owner.getWorker().executeTaskInSequence(() -> {
                synchronized (sxpDatabase) {
                    List<SxpDatabaseBinding> bindingsAdd = sxpDatabase.getBindings(getNodeIdRemote());
                    if (getFilter(filterType) != null) {
                        bindingsAdd.removeIf(getFilter(filterType));
                    }
                    getOwner().getSvcBindingDispatcher()
                            .propagateUpdate(null, masterDatabase.addBindings(bindingsAdd),
                                    owner.getAllOnSpeakerConnections());
                }
                return null;
            }, ThreadsWorker.WorkerType.INBOUND, this);
        } else if (!filterRemoved) {
            //Filters out Bindings from SXP database, removes it from Master and send update to all Listeners
            owner.getWorker().executeTaskInSequence(() -> {
                synchronized (sxpDatabase) {
                    List<SxpDatabaseBinding>
                            bindingsDelete =
                            SxpDatabase.filterDatabase(sxpDatabase, getNodeIdRemote(), getFilter(filterType));

                    if (filterType.equals(FilterType.InboundDiscarding)) {
                        sxpDatabase.deleteBindings(getNodeIdRemote(), bindingsDelete);
                    }
                    getOwner().getSvcBindingDispatcher()
                            .propagateUpdate(masterDatabase.deleteBindings(bindingsDelete), masterDatabase.addBindings(
                                    SxpDatabase.getReplaceForBindings(bindingsDelete, getOwner())),
                                    owner.getAllOnSpeakerConnections());
                }
                return null;
            }, ThreadsWorker.WorkerType.INBOUND, this);
        }
    }

    /**
     * Puts SxpBindingFilter into SxpConnection and sets appropriate flags
     *
     * @param filter SxpBindingFilter to be set
     */
    public void putFilter(SxpBindingFilter filter) {
        if (filter != null) {
            synchronized (bindingFilterMap) {
                FilterType filterType = Preconditions.checkNotNull(filter.getSxpFilter()).getFilterType();
                bindingFilterMap.get(filterType)
                        .put(Preconditions.checkNotNull(filter.getSxpFilter().getFilterSpecific()), filter);
                updateFlagsForDatabase(filterType, false);
            }
        }
    }

    /**
     * @param filterType Type of SxpBindingFilter for which looks for its PeerGroup
     * @return PeerGroup name associated with filter of specified type
     */
    public String getGroupName(FilterType filterType) {
        synchronized (bindingFilterMap) {
            SxpBindingFilter filter = getFilter(filterType);
            return filter != null ? filter.getPeerGroupName() : null;
        }
    }

    /**
     * Removed SxpBindingFilter from SxpConnection and reset appropriate flags
     *
     * @param filterType Type of SxpBindingFilter to be removed
     * @return Removed SxpBindingFilter
     */
    public SxpBindingFilter removeFilter(FilterType filterType , FilterSpecific specific) {
        synchronized (bindingFilterMap) {
            SxpBindingFilter
                    filter =
                    bindingFilterMap.get(Preconditions.checkNotNull(filterType))
                            .remove(Preconditions.checkNotNull(specific));
            if (filter != null) {
                updateFlagsForDatabase(filterType, true);
            }
            return filter;
        }
    }

    /**
     * Default constructor that creates SxpConnection using provided values
     *
     * @param owner      SxpNode to be set as owner
     * @param connection Connection that contains settings
     * @throws UnknownVersionException If version in provided values isn't supported
     */
    protected SxpConnection(SxpNode owner, Connection connection) throws UnknownVersionException {
        this.owner = Preconditions.checkNotNull(owner);
        this.connectionBuilder = new ConnectionBuilder(Preconditions.checkNotNull(connection));
        this.remoteAddress =
                new InetSocketAddress(Search.getAddress(connectionBuilder.getPeerAddress()),
                        connectionBuilder.getTcpPort() != null ? connectionBuilder.getTcpPort()
                                .getValue() : Configuration.getConstants().getPort());
        for (FilterType filterType : FilterType.values()) {
            bindingFilterMap.put(filterType, new HashMap<>());
        }
        this.context =
                new Context(owner,
                        connectionBuilder.getVersion() != null ? connectionBuilder.getVersion() : owner.getVersion());
        connectionBuilder.setCapabilities(Configuration.getCapabilities(connectionBuilder.getVersion()));
    }

    protected synchronized void setTimers(ConnectionTimers build) {
        connectionBuilder.setConnectionTimers(Preconditions.checkNotNull(build));
    }

    protected synchronized void setState(ConnectionState state) {
        connectionBuilder.setState(Preconditions.checkNotNull(state));
    }

    protected synchronized void setCapabilities(Capabilities capabilities) {
        connectionBuilder.setCapabilities(Preconditions.checkNotNull(capabilities));
    }

    protected synchronized void setVersion(Version version) {
        connectionBuilder.setVersion(Preconditions.checkNotNull(version));
    }

    public synchronized Connection getConnection() {
        return connectionBuilder.build();
    }

    protected synchronized void setConnection(Connection connection){
        connectionBuilder = new ConnectionBuilder(Preconditions.checkNotNull(connection));
    }

    /**
     * Update KeepAlive TimeStamp
     */
    public synchronized void setUpdateOrKeepaliveMessageTimestamp() {
        connectionBuilder.setTimestampUpdateOrKeepAliveMessage(TimeConv.toDt(System.currentTimeMillis()));
    }

    /**
     * Sets Id of Peer
     *
     * @param nodeId NodeId to be set
     */
    public void setNodeIdRemote(NodeId nodeId) {
        connectionBuilder.setNodeId(nodeId);
    }

    /**
     * Propagate changes learned from network to SxpDatabase
     *
     * @param message UpdateMessage containing changes
     */
    public void processUpdateMessage(UpdateMessage message) {
        if (getNodeIdRemote() == null) {
            LOG.warn(getOwner() + " Unknown message relevant peer node ID");
            return;
        }
        owner.getWorker().executeTaskInSequence(() -> {
            owner.getSvcBindingHandler()
                    .processUpdate(BindingHandler.processMessageDeletion(message),
                            BindingHandler.processMessageAddition(message, getFilter(FilterType.InboundDiscarding)),
                            this);
            return null;
        }, ThreadsWorker.WorkerType.INBOUND, this);
    }

    /**
     * Propagate changes learned from network to SxpDatabase
     *
     * @param message UpdateMessage containing changes
     */
    public void processUpdateMessage(UpdateMessageLegacy message) {
        if (getNodeIdRemote() == null) {
            LOG.warn(getOwner() + " Unknown message relevant peer node ID");
            return;
        }
        owner.getWorker().executeTaskInSequence(() -> {
            owner.getSvcBindingHandler()
                    .processUpdate(BindingHandler.processMessageDeletion(message),
                            BindingHandler.processMessageAddition(message, getFilter(FilterType.InboundDiscarding),
                                    getNodeIdRemote()), this);
            return null;
        }, ThreadsWorker.WorkerType.INBOUND, this);
    }

    public <T extends SxpBindingFields> void propagateUpdate(List<T> deleteBindings, List<T> addBindings) {
        owner.getSvcBindingDispatcher()
                .propagateUpdate(deleteBindings, addBindings, owner.getAllOnSpeakerConnections());
    }

    /**
     * Adds ChannelHandlerContext into init queue
     *
     * @param ctx ChannelHandlerContext to be added
     */
    public void addChannelHandlerContext(ChannelHandlerContext ctx) {
        synchronized (initCtxs) {
            initCtxs.add(ctx);
            LOG.debug(this + " Add init channel context {}/{}", ctx, initCtxs);
        }
    }

    /**
     * Notifies to Delete Bindings with Flag CleanUp learned from this connection
     * and propagate this change to other connections
     */
    public void cleanUpBindings() {
        // Clean-up bindings within reconciliation: If the connection recovers
        // before the delete hold down timer expiration, a reconcile timer is
        // started to clean up old mappings that didn’t get informed to be
        // removed because of the loss of connectivity.
        context.getOwner().getBindingSxpDatabase().reconcileBindings(getNodeIdRemote());
    }

    /**
     * Close specified ChannelHandlerContext and remove it from connection
     *
     * @param ctx ChannelHandlerContext to be closed
     */
    public ChannelHandlerContextType closeChannelHandlerContext(ChannelHandlerContext ctx) {
        ChannelHandlerContextType type = ChannelHandlerContextType.None;
        synchronized (initCtxs) {
            initCtxs.remove(ctx);
        }
        synchronized (ctxs) {
            for (Map.Entry<ChannelHandlerContextType, ChannelHandlerContext> e : ctxs.entrySet()) {
                if (e.getValue().equals(ctx)) {
                    type = e.getKey();
                }
            }
            if (type != ChannelHandlerContextType.None) {
                ctxs.remove(type);
            }
        }
        ctx.close();
        return type;
    }

    /**
     * Close all init ChannelHandlerContext and mark specified ChannelHandlerContext
     * according to connection mode
     *
     * @param ctx ChannelHandlerContext ChannelHandlerContext to be marked
     */
    public void closeChannelHandlerContextComplements(ChannelHandlerContext ctx) {
        synchronized (initCtxs) {
            initCtxs.remove(ctx);
            for (ChannelHandlerContext _ctx : initCtxs) {
                _ctx.close();
            }
            initCtxs.clear();
        }
    }

    /**
     * Close and remove all ChannelHandlerContext associated with this connection
     */
    public void closeChannelHandlerContexts() {
        synchronized (initCtxs) {
            for (ChannelHandlerContext _ctx : initCtxs) {
                _ctx.close();
            }
            initCtxs.clear();
        }
        synchronized (ctxs) {
            for (ChannelHandlerContext _ctx : ctxs.values()) {
                _ctx.close();
            }
            ctxs.clear();
        }
    }

    /**
     * Disable KeepAlive mechanism
     *
     * @param log Log message
     */
    private void disableKeepAliveMechanism(String log) {
        if (isModeListener()) {
            setHoldTime(0);
        } else if (isModeSpeaker()) {
            setKeepaliveTime(0);
        }
        LOG.info("{} Connection keep-alive mechanism is disabled | {}", toString(), log);
    }

    /**
     * Disable KeepAlive mechanism and send Peer error message
     *
     * @param holdTimeMin    Negotiated value
     * @param holdTimeMinAcc Negotiated value
     * @param holdTimeMax    Negotiated value
     */
    private void disableKeepAliveMechanismConnectionTermination(int holdTimeMin, int holdTimeMinAcc, int holdTimeMax)
            throws ErrorMessageException {
        disableKeepAliveMechanism("Unacceptable hold time [min=" + holdTimeMin + " acc=" + holdTimeMinAcc + " max="
                + holdTimeMax + "] | Connection termination");
        throw new ErrorMessageException(ErrorCode.OpenMessageError, ErrorSubCode.UnacceptableHoldTime, null);
    }

    /**
     * @return Gets all supported getCapabilities
     */
    public List<CapabilityType> getCapabilities() {
        if (getConnection().getCapabilities() == null || getConnection().getCapabilities().getCapability() == null) {
            return new ArrayList<>();
        }
        return getConnection().getCapabilities().getCapability();
    }

    /**
     * @return Gets all supported getCapabilities of Remote Peer
     */
    public List<CapabilityType> getCapabilitiesRemote() {
        synchronized (remoteCapabilityTypes) {
            return Collections.unmodifiableList(remoteCapabilityTypes);
        }
    }

    /**
     * Clears and afterwards sets Capabilities of Remote Peer
     *
     * @param capabilityTypeList List of Capabilities that Remote Peer supports
     */
    public void setCapabilitiesRemote(List<CapabilityType> capabilityTypeList) {
        synchronized (remoteCapabilityTypes) {
            remoteCapabilityTypes.clear();
            remoteCapabilityTypes.addAll(capabilityTypeList);
        }
    }

    /**
     * Gets ChannelHandlerContext according to specified type
     *
     * @param channelHandlerContextType Type of ChannelHandlerContext
     * @return ChannelHandlerContext marked with specified type
     * @throws ChannelHandlerContextNotFoundException    If ChannelHandlerContext isn't present
     * @throws ChannelHandlerContextDiscrepancyException If there are more ChannelHandlerContext,
     *                                                   that it used to be
     */
    public ChannelHandlerContext getChannelHandlerContext(ChannelHandlerContextType channelHandlerContextType)
            throws ChannelHandlerContextNotFoundException, ChannelHandlerContextDiscrepancyException {
        synchronized (ctxs) {
            if ((isModeBoth() && ctxs.size() > 2) || (!isModeBoth() && ctxs.size() > 1)) {
                LOG.warn(this + " Registered contexts: " + ctxs);
                throw new ChannelHandlerContextDiscrepancyException();
            }
            ChannelHandlerContext ctx = ctxs.get(channelHandlerContextType);
            if (ctx == null || ctx.isRemoved()) {
                throw new ChannelHandlerContextNotFoundException();
            }
            return ctx;
        }
    }

    /**
     * @return Gets Context selecting logic
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return Gets destination address
     */
    public InetSocketAddress getDestination() {
        return remoteAddress;
    }

    /**
     * @return Gets local address
     */
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * @return Gets HoldTime value or zero if disabled
     */
    public int getHoldTime() {
        if (getConnection().getConnectionTimers() == null
                || getConnection().getConnectionTimers().getHoldTime() == null) {
            return getOwner().getHoldTime();
        }
        return getConnection().getConnectionTimers().getHoldTime();
    }

    /**
     * @return Gets HoldTimeMax value or zero if disabled
     */
    public int getHoldTimeMax() {
        if (getConnection().getConnectionTimers() == null
                || getConnection().getConnectionTimers().getHoldTimeMax() == null) {
            return getOwner().getHoldTimeMax();
        }
        return getConnection().getConnectionTimers().getHoldTimeMax();
    }

    /**
     * @return Gets HoldTimeMin value or zero if disabled
     */
    public int getHoldTimeMin() {
        if (getConnection().getConnectionTimers() == null
                || getConnection().getConnectionTimers().getHoldTimeMin() == null) {
            return getOwner().getHoldTimeMin();
        }
        return getConnection().getConnectionTimers().getHoldTimeMin();
    }

    /**
     * @return Gets HoldTimeMinAcceptable value or zero if disabled
     */
    public int getHoldTimeMinAcceptable() {
        if (getConnection().getConnectionTimers() == null
                || getConnection().getConnectionTimers().getHoldTimeMinAcceptable() == null) {
            return getOwner().getHoldTimeMinAcceptable();
        }
        return getConnection().getConnectionTimers().getHoldTimeMinAcceptable();
    }

    /**
     * @return Gets KeepAlive value or zero if disabled
     */
    public int getKeepaliveTime() {
        if (getConnection().getConnectionTimers() == null
                || getConnection().getConnectionTimers().getKeepAliveTime() == null) {
            return getOwner().getKeepAliveTime();
        }
        return getConnection().getConnectionTimers().getKeepAliveTime();
    }

    /**
     * @return Gets Mode of connection
     */
    public ConnectionMode getMode() {
        return getConnection().getMode() != null ? getConnection().getMode() : ConnectionMode.None;
    }

    /**
     * @return Gets Mode of Peer
     */
    public ConnectionMode getModeRemote() {
        ConnectionMode mode = getMode();
        switch (mode) {
            case Speaker:
                return ConnectionMode.Listener;
            case Listener:
                return ConnectionMode.Speaker;
        }
        return mode;
    }

    /**
     * @return Gets NodeId of Peer Node
     */
    public NodeId getNodeIdRemote() {
        return getConnection().getNodeId();
    }

    /**
     * @return Gets Node that connections belongs to
     */
    public SxpNode getOwner() {
        return owner;
    }

    /**
     * @return Gets NodeId of Node that connection belongs to
     */
    public NodeId getOwnerId() {
        return owner.getNodeId();
    }

    /**
     * @return Gets Type of password used by connection
     */
    public PasswordType getPasswordType() {
        return getConnection().getPassword() != null ? getConnection().getPassword() : PasswordType.None;
    }

    /**
     * @return Gets Reconciliation timer period or zero if disabled
     */
    public int getReconciliationTime() {
        if (getConnection().getConnectionTimers() == null
                || getConnection().getConnectionTimers().getReconciliationTime() == null) {
            return 0;
        }
        return getConnection().getConnectionTimers().getReconciliationTime();
    }

    /**
     * @return Gets current state of connection
     */
    public ConnectionState getState() {
        return getConnection().getState();
    }

    /**
     * Gets SxpConnection specific Timer
     *
     * @param timerType Type of Timer
     * @return TimerType or null if not present
     */
    public ListenableScheduledFuture<?> getTimer(TimerType timerType) {
        return timers.get(timerType);
    }

    /**
     * @return Gets KeepAlive timestamp
     */
    public long getTimestampUpdateOrKeepAliveMessage() {
        return TimeConv.toLong(getConnection().getTimestampUpdateOrKeepAliveMessage());
    }

    /**
     * @return Gets connection version
     */
    public Version getVersion() {
        return getConnection().getVersion();
    }

    /**
     * Initialize timers according to specified mode and timer values
     *
     * @param connectionMode ConnectionMode used for setup
     */
    private void initializeTimers(ConnectionMode connectionMode) {
        if (connectionMode.equals(ConnectionMode.Listener)) {
            if (getHoldTime() > 0) {
                setTimer(TimerType.HoldTimer, getHoldTime());
            }
        }
        if (connectionMode.equals(ConnectionMode.Speaker)) {
            if (getKeepaliveTime() > 0) {
                setTimer(TimerType.KeepAliveTimer, getKeepaliveTime());
            }
        }
    }

    /**
     * @return If both ChannelHandlerContext are properly on
     */
    public boolean isBidirectionalBoth() {
        synchronized (ctxs) {
            return ctxs.containsKey(ChannelHandlerContextType.ListenerContext) && !ctxs.get(
                    ChannelHandlerContextType.ListenerContext).isRemoved() && ctxs.containsKey(
                    ChannelHandlerContextType.SpeakerContext) && !ctxs.get(ChannelHandlerContextType.SpeakerContext)
                    .isRemoved();
        }
    }

    /**
     * @return If connection is in mode Both
     */
    public boolean isModeBoth() {
        return getMode().equals(ConnectionMode.Both);
    }

    /**
     * @return If connection is in mode Listener
     */
    public boolean isModeListener() {
        return getMode().equals(ConnectionMode.Listener) || isModeBoth();
    }

    /**
     * @return If connection is in mode Speaker
     */
    public boolean isModeSpeaker() {
        return getMode().equals(ConnectionMode.Speaker) || isModeBoth();
    }

    /**
     * @return If State is DeleteHoldDown
     */
    public boolean isStateDeleteHoldDown() {
        return getState().equals(ConnectionState.DeleteHoldDown);
    }

    /**
     * @return If State is Off or in Both mode connections isn't in bidirectional mode
     */
    public boolean isStateOff() {
        return getState().equals(ConnectionState.Off) || (isModeBoth() && !isBidirectionalBoth());
    }

    /**
     * @return If State is On
     */
    public boolean isStateOn() {
        return getState().equals(ConnectionState.On);
    }

    /**
     * Test if specified function of Connection is On
     *
     * @param type Specifies function (Speaker/Listener)
     * @return if functionality is active on Connection
     */
    public boolean isStateOn(ChannelHandlerContextType type) {
        if (isModeBoth() && !type.equals(ChannelHandlerContextType.None)) {
            synchronized (ctxs) {
                return ctxs.containsKey(type) && !ctxs.get(type).isRemoved();
            }
        }
        return isStateOn();
    }

    public boolean isStatePendingOn() {
        return getState().equals(ConnectionState.PendingOn);
    }

    /**
     * @return If connection is version 4
     */
    public boolean isVersion4() {
        return getVersion().equals(Version.Version4);
    }

    /**
     * Mark ChannelHandlerContext with specified Role Speaker or Listener,
     * removes ChannelHandlerContext from init queue
     *
     * @param ctx                       ChannelHandlerContext to be marked
     * @param channelHandlerContextType Type of mark
     */
    public void markChannelHandlerContext(ChannelHandlerContext ctx,
            ChannelHandlerContextType channelHandlerContextType) {
        synchronized (initCtxs) {
            initCtxs.remove(ctx);
        }
        synchronized (ctxs) {
            ctxs.put(channelHandlerContextType, ctx);
        }
    }

    /**
     * Mark ChannelHandlerContext with Role Speaker or Listener,
     * removes ChannelHandlerContext from init queue
     * Aware that this method is only used for Non Both mode
     *
     * @param ctx ChannelHandlerContext to be marked
     */
    public void markChannelHandlerContext(ChannelHandlerContext ctx) {
        if (isModeBoth()) {
            LOG.warn("{} Cannot automatically mark ChannelHandlerContext {}", this, ctx);
        } else if (isModeListener()) {
            markChannelHandlerContext(ctx, ChannelHandlerContextType.ListenerContext);
        } else if (isModeSpeaker()) {
            markChannelHandlerContext(ctx, ChannelHandlerContextType.SpeakerContext);
        }
    }

    public void purgeBindings() {
        // Get message relevant peer node ID.
        BindingHandler.processPurgeAllMessage(this);
        try {
            setStateOff(getChannelHandlerContext(ChannelHandlerContextType.ListenerContext));
        } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
            setStateOff();
        }
    }

    /**
     * Set behaviour context according to specified version
     *
     * @param version Version to be set
     * @throws UnknownVersionException If Version isn't supported
     */
    public void setBehaviorContexts(Version version) throws UnknownVersionException {
        setCapabilities(Configuration.getCapabilities(version));
        setVersion(version);
        context = new Context(owner, version);
    }

    /**
     * Setup Connection by parsing received OpenMessage
     *
     * @param message OpenMessage containing necessary data
     * @throws UnknownConnectionModeException If connection mode isn't compatible
     * @throws ErrorMessageException          If da in OpenMessage are incorrect
     */
    public void setConnection(OpenMessage message) throws ErrorMessageException, UnknownConnectionModeException {
        if (isModeListener() && message.getSxpMode().equals(ConnectionMode.Speaker)) {
            setConnectionListenerPart(message);
        } else if (isModeSpeaker() && message.getSxpMode().equals(ConnectionMode.Listener)) {
            setConnectionSpeakerPart(message);
            try {
                setCapabilitiesRemote(MessageFactory.decodeCapabilities(message));
            } catch (AttributeNotFoundException e) {
                LOG.warn("{} No Capabilities received by remote peer.", this);
            }
        } else {
            throw new UnknownConnectionModeException();
        }
        // Set connection state.
        setStateOn();
    }

    /**
     * Setup Listener mode Connection by parsing received OpenMessage
     *
     * @param message OpenMessage containing necessary data
     * @throws ErrorMessageException If data in OpenMessage are incorrect
     */
    public void setConnectionListenerPart(OpenMessage message) throws ErrorMessageException {

        // Node modes compatibility.
        if (getMode().equals(ConnectionMode.Listener) && !message.getSxpMode().equals(ConnectionMode.Speaker)
                || getMode().equals(ConnectionMode.Speaker) && !message.getSxpMode().equals(ConnectionMode.Listener)) {
            throw new ErrorMessageException(ErrorCode.OpenMessageError,
                    new IncompatiblePeerModeException(getMode(), message.getSxpMode()));
        }
        // Set counter-part NodeID [Purge-All purpose: remove bindings learned
        // from a specific node].
        try {
            SxpNodeIdAttribute attNodeId = (SxpNodeIdAttribute) AttributeList.get(message.getAttribute(),
                    AttributeType.SxpNodeId);
            if (attNodeId != null) {
                setNodeIdRemote(attNodeId.getSxpNodeIdAttributes().getNodeId());
            }
        } catch (AttributeNotFoundException e) {
            if (isVersion4()) {
                throw new ErrorMessageException(ErrorCode.OpenMessageError, ErrorSubCode.OptionalAttributeError, e);
            }
        }

        // Keep-alive and hold-time negotiation.
        HoldTimeAttribute attHoldTime;
        try {
            attHoldTime = (HoldTimeAttribute) AttributeList.get(message.getAttribute(), AttributeType.HoldTime);
        } catch (AttributeNotFoundException e) {
            disableKeepAliveMechanism("Hold time attribute not present in received message");
            return;
        }

        // Peer parameters.
        int holdTimeMinAcc = attHoldTime.getHoldTimeAttributes().getHoldTimeMinValue();
        // Keep-alive mechanism is not used.
        if (holdTimeMinAcc == 0) {
            disableKeepAliveMechanism("Minimum acceptable hold time value is not used in peer");
            return;
        }

        // Local parameters.
        // Per-connection time settings: User (not)defined.
        int holdTimeMin = getHoldTimeMin();
        int holdTimeMax = getHoldTimeMax();
        // Global time settings: Default values have been pulled during node
        // creation TimeSettings.pullDefaults().
        if (holdTimeMin == 0 || holdTimeMax == 0) {
            disableKeepAliveMechanism("Minimum and maximum hold time values are globally disabled");
            return;
        }
        // The negotiation succeeds?
        if (message.getType().equals(MessageType.Open)) {
            if (holdTimeMinAcc < holdTimeMin) {
                setHoldTimeMin(holdTimeMinAcc);
            }
            if (holdTimeMinAcc > holdTimeMax) {
                disableKeepAliveMechanismConnectionTermination(holdTimeMin, holdTimeMinAcc, holdTimeMax);
            } else if (holdTimeMinAcc >= holdTimeMin && holdTimeMinAcc <= holdTimeMax) {
                setHoldTime(holdTimeMinAcc);
                setHoldTimeMin(holdTimeMinAcc);
            }
        } else if (message.getType().equals(MessageType.OpenResp)) {
            if (holdTimeMinAcc <= holdTimeMax) {
                setHoldTime(holdTimeMin);
            } else {
                disableKeepAliveMechanismConnectionTermination(holdTimeMin, holdTimeMinAcc, holdTimeMax);
            }
        }

        initializeTimers(ConnectionMode.Listener);
    }

    /**
     * Setup Speaker mode Connection by parsing received OpenMessage
     *
     * @param message OpenMessage containing necessary data
     * @throws ErrorMessageException If data in OpenMessage are incorrect
     */
    public void setConnectionSpeakerPart(OpenMessage message) throws ErrorMessageException {
        // Node modes compatibility.
        if (getMode().equals(ConnectionMode.Listener) && !message.getSxpMode().equals(ConnectionMode.Speaker)
                || getMode().equals(ConnectionMode.Speaker) && !message.getSxpMode().equals(ConnectionMode.Listener)) {
            throw new ErrorMessageException(ErrorCode.OpenMessageError,
                    new IncompatiblePeerModeException(getMode(), message.getSxpMode()));
        }
        // Set counter-part NodeID [Purge-All purpose: remove bindings learned
        // from a specific node].
        try {
            SxpNodeIdAttribute attNodeId = (SxpNodeIdAttribute) AttributeList.get(message.getAttribute(),
                    AttributeType.SxpNodeId);
            if (attNodeId != null && isVersion4()) {
                throw new ErrorMessageException(ErrorCode.OpenMessageError, ErrorSubCode.OptionalAttributeError, null);
            }
        } catch (AttributeNotFoundException e) {
            //NOP
        }

        // Keep-alive and hold-time negotiation.
        HoldTimeAttribute attHoldTime;
        try {
            attHoldTime = (HoldTimeAttribute) AttributeList.get(message.getAttribute(), AttributeType.HoldTime);
        } catch (AttributeNotFoundException e) {
            disableKeepAliveMechanism("Hold time attribute not present in received message");
            return;
        }

        // Peer parameters.
        int holdTimeMin = attHoldTime.getHoldTimeAttributes().getHoldTimeMinValue();
        int holdTimeMax = attHoldTime.getHoldTimeAttributes().getHoldTimeMaxValue();
        // Keep-alive mechanism is not used.
        if (holdTimeMin == 0) {
            disableKeepAliveMechanism("Minimum hold time value set to ZERO ");
            return;
        }

        // Local parameters.
        // Per-connection time settings: User (not)defined.
        int holdTimeMinAcc = getHoldTimeMinAcceptable();
        // Globally disabled
        if (holdTimeMinAcc == 0) {
            disableKeepAliveMechanism("Minimum acceptable hold time value is globally disabled");
            return;
        }
        // The negotiation succeeds?
        if (message.getType().equals(MessageType.Open)) {
            if (holdTimeMinAcc > holdTimeMax) {
                disableKeepAliveMechanismConnectionTermination(holdTimeMin, holdTimeMinAcc, holdTimeMax);
            } else {
                int holdTimeSelected = Math.max(holdTimeMin, holdTimeMinAcc);
                setKeepaliveTime((int) (1.0 / 3.0 * holdTimeSelected));
                setHoldTimeMinAcceptable(holdTimeSelected);
            }
        } else if (message.getType().equals(MessageType.OpenResp)) {
            setKeepaliveTime((int) (1.0 / 3.0 * holdTimeMin));
        }

        initializeTimers(ConnectionMode.Speaker);
    }

    /**
     * Start DeleteHoldDown timer and if Reconciliation timer is started stop it
     */
    public void setDeleteHoldDownTimer() {
        setTimer(TimerType.DeleteHoldDownTimer, getConnection().getConnectionTimers().getDeleteHoldDownTime());
        ListenableScheduledFuture<?> ctReconciliation = getTimer(TimerType.ReconciliationTimer);
        if (ctReconciliation != null && !ctReconciliation.isDone()) {
            LOG.info("{} Stopping Reconciliation timer cause | Connection DOWN.", this);
            setTimer(TimerType.ReconciliationTimer, null);
        }
        setStateDeleteHoldDown();
    }

    /**
     * Sets HoldTimer timer period
     *
     * @param value Time to be set
     */
    public void setHoldTime(int value) {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder(
                getConnection().getConnectionTimers());
        connectionTimersBuilder.setHoldTime(value);
        setTimers(connectionTimersBuilder.build());
    }

    /**
     * Sets HoldTimeMin used for HoldTime Negotiation
     *
     * @param value Time to be set
     */
    public void setHoldTimeMin(int value) {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder(
                getConnection().getConnectionTimers());
        connectionTimersBuilder.setHoldTimeMin(value);
        setTimers(connectionTimersBuilder.build());
    }

    /**
     * Sets HoldTimeMinAcceptable used for HoldTime Negotiation
     *
     * @param value Time to be set
     */
    public void setHoldTimeMinAcceptable(int value) {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder(
                getConnection().getConnectionTimers());
        connectionTimersBuilder.setHoldTimeMinAcceptable(value);
        setTimers(connectionTimersBuilder.build());
    }

    /**
     * Sets addresses used to communicate
     *
     * @param localAddress  SocketAddress of local connection
     * @throws SocketAddressNotRecognizedException If parameters aren't instance of InetSocketAddress
     */
    public void setInetSocketAddresses(SocketAddress localAddress)
            throws SocketAddressNotRecognizedException {
        if (!(localAddress instanceof InetSocketAddress)) {
            throw new SocketAddressNotRecognizedException(localAddress);
        }
        this.localAddress = (InetSocketAddress) localAddress;
    }

    /**
     * Sets KeepAlive timer period
     *
     * @param value Time to be set
     */
    public void setKeepaliveTime(int value) {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder(
                getConnection().getConnectionTimers());
        connectionTimersBuilder.setKeepAliveTime(value);
        setTimers(connectionTimersBuilder.build());
    }

    public void setReconciliationTimer() {
        if (getReconciliationTime() > 0) {
            ListenableScheduledFuture<?> ctDeleteHoldDown = getTimer(TimerType.DeleteHoldDownTimer);
            if (ctDeleteHoldDown != null && !ctDeleteHoldDown.isDone()) {
                LOG.info("{} Stopping Delete Hold Down timer.", this);
                setTimer(TimerType.DeleteHoldDownTimer, null);
            }
            LOG.info("{} Starting Reconciliation timer.", this);
            setTimer(TimerType.ReconciliationTimer, getReconciliationTime());
        }
    }

    /**
     * Set State to DeleteHoldDown and triggers cleanUp of Database
     */
    public void setStateDeleteHoldDown() {
        setState(ConnectionState.DeleteHoldDown);
        owner.getBindingSxpDatabase().setReconciliation(getNodeIdRemote());
    }

    /**
     * Set State to Off resets all flags, stop timers,
     * clear Handling of messages and close ChannelHandlerContexts
     */
    public void setStateOff() {
        stopTimers();
        setState(ConnectionState.Off);
        getOwner().getWorker().cancelTasksInSequence(true, ThreadsWorker.WorkerType.INBOUND, this);
        getOwner().getWorker().cancelTasksInSequence(true, ThreadsWorker.WorkerType.OUTBOUND, this);
        closeChannelHandlerContexts();
    }

    /**
     * Gets type of ChannelHandlerContext in current connection,
     * if ChannelHandlerContext is not used in this connection return None
     *
     * @param ctx ChannelHandlerContext  to be tested
     * @return Type of ChannelHandlerContext in this connection
     */
    public ChannelHandlerContextType getContextType(ChannelHandlerContext ctx){
        ChannelHandlerContextType type = ChannelHandlerContextType.None;
        synchronized (ctxs) {
            for (Map.Entry<ChannelHandlerContextType, ChannelHandlerContext> e : ctxs.entrySet()) {
                if (e.getValue().equals(ctx)) {
                    type = e.getKey();
                    break;
                }
            }
        }
        return type;
    }

    /**
     * Set State to Off and close ChannelHandlerContext,
     * if Connection is in Both mode disable appropriate
     * functionality based on ChannelHandlerContext type
     *
     * @param ctx ChannelHandlerContext to be closed
     */
    public void setStateOff(ChannelHandlerContext ctx) {
        ChannelHandlerContextType type = closeChannelHandlerContext(ctx);
        if (ctxs.isEmpty()) {
            setStateOff();
        } else {
            switch (type) {
                case ListenerContext:
                    setTimer(TimerType.DeleteHoldDownTimer, 0);
                    setTimer(TimerType.ReconciliationTimer, 0);
                    setTimer(TimerType.HoldTimer, 0);
                    getOwner().getWorker().cancelTasksInSequence(true, ThreadsWorker.WorkerType.INBOUND, this);
                    break;
                case SpeakerContext:
                    setTimer(TimerType.KeepAliveTimer, 0);
                    getOwner().getWorker().cancelTasksInSequence(true, ThreadsWorker.WorkerType.OUTBOUND, this);
                    break;
            }
        }
    }

    /**
     * Stops all supported timers till version 4
     */
    private void stopTimers() {
        setTimer(TimerType.DeleteHoldDownTimer, 0);
        setTimer(TimerType.ReconciliationTimer, 0);
        setTimer(TimerType.HoldTimer, 0);
        setTimer(TimerType.KeepAliveTimer, 0);
    }

    /**
     * Set State to On and if Connection is mode speaker
     * notifies export of Bindings
     */
    public void setStateOn() {
        setState(ConnectionState.On);
        if (isModeSpeaker() || isModeBoth()) {
            getOwner().getWorker().executeTaskInSequence(() -> {
                List<SxpConnection> sxpConnections = new ArrayList<>();
                sxpConnections.add(this);
                owner.getSvcBindingDispatcher()
                        .propagateUpdate(null, getOwner().getBindingMasterDatabase().getBindings(), sxpConnections);
                return null;
            }, ThreadsWorker.WorkerType.OUTBOUND, this);
        }
    }

    /**
     * Set State to PendingOn
     */
    public void setStatePendingOn() {
        setState(ConnectionState.PendingOn);
    }

    /**
     * Sets SxpConnection specific Timer
     *
     * @param timerType Type of Timer that will be set
     * @param period    Time period to wait till execution in Seconds
     * @return ListenableScheduledFuture callback
     * @throws UnknownTimerTypeException If current TimerType isn't supported
     */
    public synchronized ListenableScheduledFuture<?> setTimer(TimerType timerType, int period) throws UnknownTimerTypeException {
        SxpTimerTask timer;
        switch (timerType) {
            case DeleteHoldDownTimer:
                timer = new DeleteHoldDownTimerTask(this, period);
                break;
            case HoldTimer:
                timer = new HoldTimerTask(this, period);
                break;
            case KeepAliveTimer:
                timer = new KeepAliveTimerTask(this, period);
                break;
            case ReconciliationTimer:
                timer = new ReconcilationTimerTask(this, period);
                break;
            default:
                throw new UnknownTimerTypeException(timerType);
        }
        ListenableScheduledFuture<?> timer_ = getTimer(timerType);
        if (period > 0 && (timer_ == null || !timer_.isCancelled())) {
            return this.setTimer(timerType, owner.getWorker().scheduleTask(timer, period, TimeUnit.SECONDS));
        } else {
            return this.setTimer(timerType, null);
        }
    }

    /**
     * Sets SxpConnection specific Timer
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
     * Shutdown Connection and send PurgeAll if Speaker mode,
     * or purge learned Bindings if Listener mode
     */
    public void shutdown() {
        if (isModeListener()) {
            LOG.info("{} PURGE bindings ", this);
            purgeBindings();
        } else if (isModeSpeaker() && isStateOn()) {
            try {
                BindingDispatcher.sendPurgeAllMessage(this).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error(this + " Shutdown connection | {} | ", e.getClass().getSimpleName());
            }
        }
        setStateOff();
    }

    @Override
    public String toString() {
        String localAddress = this.localAddress != null ? this.localAddress.toString() : "";
        if (localAddress.startsWith("/")) {
            localAddress = localAddress.substring(1);
        }
        String remoteAddress = this.remoteAddress != null ? this.remoteAddress.toString() : "";
        if (remoteAddress.startsWith("/")) {
            remoteAddress = remoteAddress.substring(1);
        }
        String result = owner.toString() + "[" + localAddress + "/" + remoteAddress + "]";

        result += "[" + (getState().equals(ConnectionState.Off) ? "X" : getState().toString().charAt(0)) + "|"
                + getMode().toString().charAt(0) + "v" + getVersion().getIntValue();
        if (getModeRemote() != null) {
            result += "/" + getModeRemote().toString().charAt(0);
        }

        if (getNodeIdRemote() != null) {
            result += " " + NodeIdConv.toString(getNodeIdRemote());
        }
        result += "]";
        return result;
    }
}
