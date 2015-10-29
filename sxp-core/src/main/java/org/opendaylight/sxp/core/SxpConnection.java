/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.opendaylight.sxp.core.behavior.Context;
import org.opendaylight.sxp.core.handler.MessageDecoder;
import org.opendaylight.sxp.core.messaging.AttributeList;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.core.service.UpdateExportTask;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.exception.connection.IncompatiblePeerModeException;
import org.opendaylight.sxp.util.exception.connection.SocketAddressNotRecognizedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeNotFoundException;
import org.opendaylight.sxp.util.exception.unknown.UnknownConnectionModeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownNodeIdException;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.PasswordType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorSubCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.HoldTimeAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.SxpNodeIdAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

    private List<ChannelHandlerContext> initCtxs = new ArrayList<>(2);
    protected HashMap<ChannelHandlerContextType, ChannelHandlerContext> ctxs = new HashMap<>(2);

    protected InetSocketAddress destination;

    protected InetSocketAddress localAddress, remoteAddress;

    protected SxpNode owner;

    protected HashMap<TimerType, ListenableScheduledFuture<?>> timers = new HashMap<>(5);

    private final Deque<Callable<?>> inboundUpdateMessageQueue = new ArrayDeque<>(10),
            outboundUpdateMessageQueue =
                    new ArrayDeque<>(10);
    private final AtomicLong inboundMonitor = new AtomicLong(0), outboundMonitor = new AtomicLong(0);
    private final Map<FilterType, SxpBindingFilter> bindingFilterMap = new HashMap<>();

    /**
     * @param filterType Type of SxpBindingFilter to look for
     * @return Filter with specified type or null if connection doesnt have one
     */
    public SxpBindingFilter getFilter(FilterType filterType) {
        synchronized (bindingFilterMap) {
            return bindingFilterMap.get(filterType);
        }
    }

    /**
     * Defines how to setup flags after filter of specific type is set in SxpConnection
     *
     * @param filterType Type of SxpBindingFilter that was set
     */
    private void updateFlagsForDatabase(FilterType filterType) {
        if(!isStateOn()){
            return;
        }
        if (filterType.equals(FilterType.Inbound) && (isModeListener() || isModeBoth())) {
            owner.setSvcBindingManagerNotify();
        } else if (filterType.equals(FilterType.Outbound) && (isModeSpeaker() || isModeBoth())) {
            try {
                getChannelHandlerContext(ChannelHandlerContextType.SpeakerContext).writeAndFlush(
                        MessageFactory.createPurgeAll());
            } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
                LOG.error(this + " Cannot send PURGE ALL message to set new filter| {} | ",
                        e.getClass().getSimpleName());
            }
            connectionBuilder.setUpdateAllExported(false);
            connectionBuilder.setUpdateExported(false);
            owner.setSvcBindingDispatcherNotify();
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
                FilterType filterType = filter.getSxpFilter().getFilterType();
                bindingFilterMap.put(filterType, filter);
                updateFlagsForDatabase(filterType);
            }
        }
    }

    /**
     * @param filterType Type of SxpBindingFilter for which looks for its PeerGroup
     * @return PeerGroup name associated with filter of specified type
     */
    public String getGroupName(FilterType filterType) {
        synchronized (bindingFilterMap) {
            return bindingFilterMap.get(filterType) != null ? bindingFilterMap.get(filterType)
                    .getPeerGroupName() : null;
        }
    }

    /**
     * Removed SxpBindingFilter from SxpConnection and reset appropriate flags
     *
     * @param filterType Type of SxpBindingFilter to be removed
     * @return Removed SxpBindingFilter
     */
    public SxpBindingFilter removeFilter(FilterType filterType) {
        synchronized (bindingFilterMap) {
            SxpBindingFilter filter = bindingFilterMap.remove(filterType);
            if(filter!=null) {
                updateFlagsForDatabase(filterType);
            }
            return filter;
        }
    }

    /**
     * Gets AtomicLong used for notification of incoming messages that will be needed to proceed
     *
     * @return AtomicLong used for counting of incoming messages
     */
    public AtomicLong getInboundMonitor() {
        return inboundMonitor;
    }

    /**
     * Gets AtomicLong used for notification of outgoing messages that will be needed to proceed
     *
     * @return AtomicLong used for counting of outgoing messages
     */
    public AtomicLong getOutboundMonitor() {
        return outboundMonitor;
    }

    /**
     * Poll Task representing export of Update Message on this connection.
     *
     * @return Task exporting specific Update Message
     */
    public Callable<?> pollUpdateMessageOutbound() {
        synchronized (outboundUpdateMessageQueue) {
            return outboundUpdateMessageQueue.poll();
        }
    }

    /**
     * Push new Update Message task into export queue.
     *
     * @param task Task containing process information of Update Message
     */
    public void pushUpdateMessageOutbound(Callable<?> task) {
        synchronized (outboundUpdateMessageQueue) {
            outboundUpdateMessageQueue.push(task);
        }
    }

    /**
     * Poll Task representing import of Update Message on this connection.
     *
     * @return Task importing specific Update Message
     */
    public Callable<?> pollUpdateMessageInbound() {
        synchronized (inboundUpdateMessageQueue) {
            return inboundUpdateMessageQueue.poll();
        }
    }

    /**
     * Push new Update Message task into import queue.
     *
     * @param task Task containing process information of Update Message
     */
    public void pushUpdateMessageInbound(Callable<?> task) {
        synchronized (inboundUpdateMessageQueue) {
            inboundUpdateMessageQueue.push(task);
        }
    }

    /**
     * Clears queue of inbound and outbound Update Messages.
     */
    private void clearMessages() {
        synchronized (inboundUpdateMessageQueue) {
            inboundUpdateMessageQueue.clear();
        }
        synchronized (outboundUpdateMessageQueue) {
            for (Callable t : outboundUpdateMessageQueue) {
                if (t instanceof UpdateExportTask) {
                    ((UpdateExportTask) t).freeReferences();
                }
            }
            outboundUpdateMessageQueue.clear();
        }
    }

    /**
     * Default constructor that creates SxpConnection using provided values
     *
     * @param owner      SxpNode to be set as owner
     * @param connection Connection that contains settings
     * @throws UnknownVersionException If version in provided values isn't supported
     */
    private SxpConnection(SxpNode owner, Connection connection) throws UnknownVersionException {
        this.owner = owner;
        this.connectionBuilder = new ConnectionBuilder(connection);

        if (connectionBuilder.getVersion() == null) {
            connectionBuilder.setVersion(Version.Version4);
        }
        if (connectionBuilder.getState() == null || connectionBuilder.getState().equals(ConnectionState.Off)) {
            setStateOff();
        }
        int port = Configuration.getConstants().getPort();
        if (connection.getTcpPort() != null && connection.getTcpPort().getValue() > 0) {
            port = connection.getTcpPort().getValue();
        }
        this.destination = new InetSocketAddress(Search.getAddress(connection.getPeerAddress()), port);

        if (connection.getVersion() != null) {
            setBehaviorContexts(connection.getVersion());
        } else {
            setBehaviorContexts(owner.getVersion());
        }
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

        // Get message relevant peer node ID.
        NodeId peerId;
        try {
            if (isVersion123()) {
                peerId = NodeIdConv.createNodeId(getDestination().getAddress());
            } else if (isVersion4()) {
                peerId = getNodeIdRemote();
            } else {
                LOG.warn(this + " Unknown message relevant peer node ID | Version not recognized [\""
                        + connectionBuilder.getVersion() + "\"]");
                return;
            }
        } catch (UnknownNodeIdException e) {
            LOG.warn(this + " Unknown message relevant peer node ID | {} | {}", e.getClass().getSimpleName(),
                    e.getMessage());
            return;
        }

        context.getOwner().cleanUpBindings(peerId);
        context.getOwner().notifyService();
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
    private void disableKeepAliveMechanismConnectionTermination(int holdTimeMin, int holdTimeMinAcc, int holdTimeMax) {
        disableKeepAliveMechanism("Unacceptable hold time [min=" + holdTimeMin + " acc=" + holdTimeMinAcc + " max="
                + holdTimeMax + "] | Connection termination");
        MessageDecoder.sendErrorMessage(null,
                new ErrorMessageException(ErrorCode.OpenMessageError, ErrorSubCode.UnacceptableHoldTime, null), this);
    }

    /**
     * @return Gets all supported getCapabilities
     */
    public List<CapabilityType> getCapabilities() {
        if (connectionBuilder.getCapabilities() == null || connectionBuilder.getCapabilities().getCapability() == null) {
            return new ArrayList<CapabilityType>();
        }
        return connectionBuilder.getCapabilities().getCapability();
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
     * @return Gets Connection containing all setting o SxpConnection
     */
    public Connection getConnection() {
        return connectionBuilder.build();
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
        return destination;
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
        if (connectionBuilder.getConnectionTimers() == null
                || connectionBuilder.getConnectionTimers().getHoldTime() == null
                || connectionBuilder.getConnectionTimers().getHoldTime() == null) {
            return 0;
        }
        return connectionBuilder.getConnectionTimers().getHoldTime();
    }

    /**
     * @return Gets HoldTimeMax value or zero if disabled
     */
    public int getHoldTimeMax() {
        if (connectionBuilder.getConnectionTimers() == null
                || connectionBuilder.getConnectionTimers().getHoldTimeMax() == null
                || connectionBuilder.getConnectionTimers().getHoldTimeMax() == null) {
            return 0;
        }
        return connectionBuilder.getConnectionTimers().getHoldTimeMax();
    }

    /**
     * @return Gets HoldTimeMin value or zero if disabled
     */
    public int getHoldTimeMin() {
        if (connectionBuilder.getConnectionTimers() == null
                || connectionBuilder.getConnectionTimers().getHoldTimeMin() == null
                || connectionBuilder.getConnectionTimers().getHoldTimeMin() == null) {
            return 0;
        }
        return connectionBuilder.getConnectionTimers().getHoldTimeMin();
    }

    /**
     * @return Gets HoldTimeMinAcceptable value or zero if disabled
     */
    public int getHoldTimeMinAcceptable() {
        if (connectionBuilder.getConnectionTimers() == null
                || connectionBuilder.getConnectionTimers().getHoldTimeMinAcceptable() == null
                || connectionBuilder.getConnectionTimers().getHoldTimeMinAcceptable() == null) {
            return 0;
        }
        return connectionBuilder.getConnectionTimers().getHoldTimeMinAcceptable();
    }

    /**
     * @return Gets KeepAlive value or zero if disabled
     */
    public int getKeepaliveTime() {
        if (connectionBuilder.getConnectionTimers() == null
                || connectionBuilder.getConnectionTimers().getKeepAliveTime() == null
                || connectionBuilder.getConnectionTimers().getKeepAliveTime() == null) {
            return 0;
        }
        return connectionBuilder.getConnectionTimers().getKeepAliveTime();
    }

    /**
     * @return Gets Mode of connection
     */
    public ConnectionMode getMode() {
        if (connectionBuilder.getMode() == null) {
            return ConnectionMode.None;
        }
        return connectionBuilder.getMode();
    }

    /**
     * @return Gets Mode of Peer
     */
    public ConnectionMode getModeRemote() {
        if (connectionBuilder.getModeRemote() == null) {
            return ConnectionMode.None;
        }
        return connectionBuilder.getModeRemote();
    }

    /**
     * @return Gets NodeId of Peer Node
     */
    public NodeId getNodeIdRemote() {
        return connectionBuilder.getNodeId();
    }

    /**
     * Gets SxpNode specific Timer of owner
     *
     * @param timerType Type of Timer
     * @return TimerType or null if not present
     */
    public ListenableScheduledFuture<?> getNodeTimer(TimerType timerType) {
        return context.getOwner().getTimer(timerType);
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
        if (connectionBuilder.getPassword() == null) {
            return PasswordType.None;
        }
        return connectionBuilder.getPassword();
    }

    /**
     * @return Gets Reconciliation timer period or zero if disabled
     */
    public int getReconciliationTime() {
        if (connectionBuilder.getConnectionTimers() == null
                || connectionBuilder.getConnectionTimers().getReconciliationTime() == null
                || connectionBuilder.getConnectionTimers().getReconciliationTime() == null) {
            return 0;
        }
        return connectionBuilder.getConnectionTimers().getReconciliationTime();
    }

    /**
     * @return Gets current state of connection
     */
    public ConnectionState getState() {
        return connectionBuilder.getState();
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
     * @return Gets Update message timestamp
     */
    public long getTimestampUpdateMessageExport() {
        return TimeConv.toLong(connectionBuilder.getTimestampUpdateMessageExport());
    }

    /**
     * @return Gets KeepAlive timestamp
     */
    public long getTimestampUpdateOrKeepAliveMessage() {
        return TimeConv.toLong(connectionBuilder.getTimestampUpdateOrKeepAliveMessage());
    }

    /**
     * @return Gets connection version
     */
    public Version getVersion() {
        return connectionBuilder.getVersion();
    }

    /**
     * @return Gets Peer version
     */
    public Version getVersionRemote() {
        return connectionBuilder.getVersionRemote();
    }

    /**
     * Check if connection support capability
     *
     * @param capability Type of capability
     * @return If connection supports it
     */
    public boolean hasCapability(CapabilityType capability) {
        if (connectionBuilder.getCapabilities() == null || connectionBuilder.getCapabilities().getCapability() == null) {
            return false;
        }
        return connectionBuilder.getCapabilities().getCapability().contains(capability);
    }

    /**
     * Initialize timers according to specified mode and timer values
     *
     * @param connectionMode ConnectionMode used for setup
     */
    private void initializeTimers(ConnectionMode connectionMode) {

        // Listener connection specific.
        if (connectionMode.equals(ConnectionMode.Listener)) {
            // Set reconciliation timer per connection.
            setReconciliationTimer();
            if (getHoldTime() > 0) {
                setTimer(TimerType.HoldTimer, getHoldTime());
            }
        }
        // Speaker connection specific. According to the initial negotiation in
        // the Sxpv4 behavior, we can't use Speaker configuration that is
        // related to channel context, i.e.
        // ChannelHandlerContextDiscrepancyException. This timer will be setup
        // during Binding Dispatcher runtime.
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
     * @return If PurgeAll message was received
     */
    public boolean isPurgeAllMessageReceived() {
        return connectionBuilder.isPurgeAllMessageReceived() == null ? false : connectionBuilder
                .isPurgeAllMessageReceived();
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
     * @return If Update message was exported at least once
     */
    public boolean isUpdateAllExported() {
        return connectionBuilder.isUpdateAllExported() == null ? false : connectionBuilder.isUpdateAllExported();
    }

    /**
     * @return If Update message was exported
     */
    public boolean isUpdateExported() {
        return connectionBuilder.isUpdateExported() == null ? false : connectionBuilder.isUpdateExported();
    }

    /**
     * @return If connection is legacy version
     */
    public boolean isVersion123() {
        return getVersion().equals(Version.Version1) || getVersion().equals(Version.Version2)
                || getVersion().equals(Version.Version3);
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
            LOG.error("{} Cannot automatically mark ChannelHandlerContext {}", this, ctx);
        } else if (isModeListener()) {
            markChannelHandlerContext(ctx, ChannelHandlerContextType.ListenerContext);
        } else if (isModeSpeaker()) {
            markChannelHandlerContext(ctx, ChannelHandlerContextType.SpeakerContext);
        }
    }

    public void purgeBindings() {
        // Get message relevant peer node ID.
        NodeId peerId;
        try {
            if (isVersion123()) {
                peerId = NodeIdConv.createNodeId(getDestination().getAddress());
            } else if (isVersion4()) {
                peerId = getNodeIdRemote();
            } else {
                LOG.warn(this + " Unknown message relevant peer node ID | Version not recognized [\"" + getVersion()
                        + "\"]");
                return;
            }
        } catch (UnknownNodeIdException e) {
            LOG.warn(this + " Unknown message relevant peer node ID | {} | {}", e.getClass().getSimpleName(),
                    e.getMessage());
            setStateOff();
            return;
        }

        context.getOwner().purgeBindings(peerId);
        context.getOwner().notifyService();
        try {
            setStateOff(getChannelHandlerContext(ChannelHandlerContextType.ListenerContext));
        } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
            LOG.warn("{} Error setting Off connection", this, e);
            setStateOff();
        }
    }

    /**
     * Reset Flag UpdateExported
     */
    public void resetUpdateExported() {
        connectionBuilder.setUpdateExported(false);
    }

    /**
     * Set behaviour context according to specified version
     *
     * @param version Version to be set
     * @throws UnknownVersionException If Version isn't supported
     */
    public void setBehaviorContexts(Version version) throws UnknownVersionException {
        connectionBuilder.setCapabilities(Configuration.getCapabilities(version));
        connectionBuilder.setVersion(version);
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
        setModeRemote(message.getSxpMode());

        // Set version.
        Version newVersion = getVersion().getIntValue() <= message.getVersion().getIntValue() ? getVersion() : message
                .getVersion();
        setVersionRemote(message.getVersion());
        setBehaviorContexts(newVersion);

        // Set counter-part NodeID [Purge-All purpose: remove bindings learned
        // from a specific node].
        try {
            SxpNodeIdAttribute attNodeId = (SxpNodeIdAttribute) AttributeList.get(message.getAttribute(),
                    AttributeType.SxpNodeId);
            if (attNodeId != null) {
                setNodeIdRemote(attNodeId.getSxpNodeIdAttributes().getNodeId());
            }
        } catch (AttributeNotFoundException e) {
        }

        // Keep-alive and hold-time negotiation.
        HoldTimeAttribute attHoldTime;
        try {
            attHoldTime = (HoldTimeAttribute) AttributeList.get(message.getAttribute(), AttributeType.HoldTime);
        } catch (AttributeNotFoundException e) {
            disableKeepAliveMechanism("Hold time attribute not present in received message");
            return;
        }

        if (isModeBoth()) {
            setModeRemote(ConnectionMode.Both);
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
            holdTimeMin = getOwner().getHoldTimeMin();
            holdTimeMax = getOwner().getHoldTimeMax();

            // Globally disabled
            if (holdTimeMin == 0 || holdTimeMax == 0) {
                disableKeepAliveMechanism("Minimum and maximum hold time values are globally disabled");
                return;
            }
        }
        // The negotiation succeeds?
        if (message.getType().equals(MessageType.Open)) {
            if (holdTimeMinAcc < holdTimeMin) {
                setHoldTimeMin(holdTimeMinAcc);
            }
            if (holdTimeMinAcc > holdTimeMax) {
                disableKeepAliveMechanismConnectionTermination(holdTimeMin, holdTimeMinAcc, holdTimeMax);
                return;
            } else if (holdTimeMinAcc >= holdTimeMin && holdTimeMinAcc <= holdTimeMax) {
                setHoldTime(holdTimeMinAcc);
                setHoldTimeMin(holdTimeMinAcc);
            }
        } else if (message.getType().equals(MessageType.OpenResp)) {
            if (holdTimeMinAcc <= holdTimeMax && holdTimeMinAcc >= holdTimeMin) {
                setHoldTime(holdTimeMinAcc);
            } else {
                disableKeepAliveMechanismConnectionTermination(holdTimeMin, holdTimeMinAcc, holdTimeMax);
                return;
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
        setModeRemote(message.getSxpMode());

        // Set version.
        Version newVersion = getVersion().getIntValue() <= message.getVersion().getIntValue() ? getVersion() : message
                .getVersion();
        setVersionRemote(message.getVersion());
        setBehaviorContexts(newVersion);

        // Set counter-part NodeID [Purge-All purpose: remove bindings learned
        // from a specific node].
        try {
            SxpNodeIdAttribute attNodeId = (SxpNodeIdAttribute) AttributeList.get(message.getAttribute(),
                    AttributeType.SxpNodeId);
            if (attNodeId != null) {
                setNodeIdRemote(attNodeId.getSxpNodeIdAttributes().getNodeId());
            }
        } catch (AttributeNotFoundException e) {
        }

        // Keep-alive and hold-time negotiation.
        HoldTimeAttribute attHoldTime;
        try {
            attHoldTime = (HoldTimeAttribute) AttributeList.get(message.getAttribute(), AttributeType.HoldTime);
        } catch (AttributeNotFoundException e) {
            disableKeepAliveMechanism("Hold time attribute not present in received message");
            return;
        }

        if (isModeBoth()) {
            setModeRemote(ConnectionMode.Both);
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
        // Global time settings.
        if (holdTimeMinAcc == 0) {
            holdTimeMinAcc = getOwner().getHoldTimeMinAcceptable();

            // Globally disabled
            if (holdTimeMinAcc == 0) {
                disableKeepAliveMechanism("Minimum acceptable hold time value is globally disabled");
                return;
            }
        }
        // The negotiation succeeds?
        if (message.getType().equals(MessageType.Open)) {
            if (holdTimeMinAcc > holdTimeMax) {
                disableKeepAliveMechanismConnectionTermination(holdTimeMin, holdTimeMinAcc, holdTimeMax);
                return;
            } else {
                int holdTimeSelected = Math.max(holdTimeMin, holdTimeMinAcc);
                setKeepaliveTime((int) (1.0 / 3.0 * holdTimeSelected));
                setHoldTimeMinAcceptable(holdTimeSelected);
            }
        } else if (message.getType().equals(MessageType.OpenResp)) {
            if (holdTimeMin < holdTimeMinAcc) {
                disableKeepAliveMechanismConnectionTermination(holdTimeMin, holdTimeMinAcc, holdTimeMax);
                return;
            } else {
                setKeepaliveTime((int) (1.0 / 3.0 * holdTimeMin));
            }
        }

        initializeTimers(ConnectionMode.Speaker);
    }

    /**
     * Start DeleteHoldDown timer and if Reconciliation timer is started stop it
     */
    public void setDeleteHoldDownTimer() {
        // Non configurable.
        setTimer(TimerType.DeleteHoldDownTimer,Configuration.getTimerDefault()
                .getDeleteHoldDownTimer());

        ListenableScheduledFuture<?> ctReconciliation = getTimer(TimerType.ReconciliationTimer);
        if (ctReconciliation != null && !ctReconciliation.isDone()) {
            LOG.info("{} Stopping Reconciliation timer cause | Connection DOWN.", this);
            setTimer(TimerType.ReconciliationTimer, null);
        }

        setStateDeleteHoldDown();
    }

    /**
     * Sets port of Peer
     *
     * @param port Port to be set
     */
    public void setDestinationPort(int port) {
        this.destination = new InetSocketAddress(destination.getAddress(), port);
    }

    /**
     * Sets HoldTimer timer period
     *
     * @param value Time to be set
     */
    public void setHoldTime(int value) {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder(
                connectionBuilder.getConnectionTimers());
        connectionTimersBuilder.setHoldTime(value);
        connectionBuilder.setConnectionTimers(connectionTimersBuilder.build());
    }

    /**
     * Sets HoldTimeMin used for HoldTime Negotiation
     *
     * @param value Time to be set
     */
    public void setHoldTimeMin(int value) {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder(
                connectionBuilder.getConnectionTimers());
        connectionTimersBuilder.setHoldTimeMin(value);
        connectionBuilder.setConnectionTimers(connectionTimersBuilder.build());
    }

    /**
     * Sets HoldTimeMinAcceptable used for HoldTime Negotiation
     *
     * @param value Time to be set
     */
    public void setHoldTimeMinAcceptable(int value) {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder(
                connectionBuilder.getConnectionTimers());
        connectionTimersBuilder.setHoldTimeMinAcceptable(value);
        connectionBuilder.setConnectionTimers(connectionTimersBuilder.build());
    }

    /**
     * Sets addresses used to communicate
     *
     * @param localAddress  SocketAddress of local connection
     * @param remoteAddress SocketAddress of Peer
     * @throws SocketAddressNotRecognizedException If parameters aren't instance of InetSocketAddress
     */
    public void setInetSocketAddresses(SocketAddress localAddress, SocketAddress remoteAddress)
            throws SocketAddressNotRecognizedException {
        if (!(localAddress instanceof InetSocketAddress)) {
            throw new SocketAddressNotRecognizedException(localAddress);
        } else if (!(remoteAddress instanceof InetSocketAddress)) {
            throw new SocketAddressNotRecognizedException(remoteAddress);
        }

        this.localAddress = (InetSocketAddress) localAddress;
        this.remoteAddress = (InetSocketAddress) remoteAddress;
    }

    /**
     * Sets KeepAlive timer period
     *
     * @param value Time to be set
     */
    public void setKeepaliveTime(int value) {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder(
                connectionBuilder.getConnectionTimers());
        connectionTimersBuilder.setKeepAliveTime(value);
        connectionBuilder.setConnectionTimers(connectionTimersBuilder.build());
    }

    /**
     * Sets Mode of Connection
     *
     * @param mode ConnectionMode to be set
     */
    public void setMode(ConnectionMode mode) {
        connectionBuilder.setMode(mode);
    }

    /**
     * Sets Mode of Peer
     *
     * @param mode ConnectionMode to be set
     */
    public void setModeRemote(ConnectionMode mode) {
        connectionBuilder.setModeRemote(mode);
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
     * Sets Node that Connection belong to
     *
     * @param owner SxpNode to be set as owner
     */
    public void setOwner(SxpNode owner) {
        this.owner = owner;
    }

    /**
     * Set Flag PurgeAllReceived
     */
    public void setPurgeAllMessageReceived() {
        connectionBuilder.setPurgeAllMessageReceived(true);
    }

    public void setReconciliationTimer() {
        if (getReconciliationTime() > 0) {
            ListenableScheduledFuture<?> ctDeleteHoldDown = getTimer(TimerType.DeleteHoldDownTimer);
            if (ctDeleteHoldDown != null && !ctDeleteHoldDown.isDone()) {
                LOG.info("{} Starting Reconciliation timer.", this);
                setTimer(TimerType.ReconciliationTimer, getReconciliationTime());

                LOG.info("{} Stopping Delete Hold Down timer.", this);
                setTimer(TimerType.DeleteHoldDownTimer, null);
            }
        }
    }

    /**
     * Set State to AdministrativelyDown
     */
    public void setStateAdminDown() {
        connectionBuilder.setState(ConnectionState.AdministrativelyDown);
    }

    /**
     * Set State to DeleteHoldDown and triggers cleanUp of Database
     */
    public void setStateDeleteHoldDown() {
        connectionBuilder.setUpdateAllExported(false);
        connectionBuilder.setUpdateExported(false);
        connectionBuilder.setPurgeAllMessageReceived(false);
        connectionBuilder.setState(ConnectionState.DeleteHoldDown);

        NodeId peerId;
        try {
            if (isVersion123()) {
                peerId = NodeIdConv.createNodeId(getDestination().getAddress());
            } else if (isVersion4()) {
                peerId = getNodeIdRemote();
            } else {
                LOG.warn(this + " Unknown message relevant peer node ID | Version not recognized [\"" + getVersion()
                        + "\"]");
                return;
            }
        } catch (UnknownNodeIdException e) {
            LOG.warn(this + " Unknown message relevant peer node ID | {} | {}", e.getClass().getSimpleName(),
                    e.getMessage());
            return;
        }
        context.getOwner().setAsCleanUp(peerId);
    }

    /**
     * Set State to Off resets all flags, stop timers,
     * clear Handling of messages and close ChannelHandlerContexts
     */
    public void setStateOff() {
        closeChannelHandlerContexts();
        connectionBuilder.setState(ConnectionState.Off);
        connectionBuilder.setUpdateAllExported(false);
        connectionBuilder.setUpdateExported(false);
        connectionBuilder.setPurgeAllMessageReceived(false);
        stopTimers();
        clearMessages();
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
                    connectionBuilder.setPurgeAllMessageReceived(false);
                    setTimer(TimerType.DeleteHoldDownTimer, 0);
                    setTimer(TimerType.ReconciliationTimer, 0);
                    setTimer(TimerType.HoldTimer, 0);
                    synchronized (inboundUpdateMessageQueue) {
                        inboundUpdateMessageQueue.clear();
                    }
                    break;
                case SpeakerContext:
                    connectionBuilder.setUpdateAllExported(false);
                    connectionBuilder.setUpdateExported(false);
                    setTimer(TimerType.KeepAliveTimer, 0);
                    synchronized (outboundUpdateMessageQueue) {
                        for (Callable t : outboundUpdateMessageQueue) {
                            if (t instanceof UpdateExportTask) {
                                ((UpdateExportTask) t).freeReferences();
                            }
                        }
                        outboundUpdateMessageQueue.clear();
                    }
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
        connectionBuilder.setState(ConnectionState.On);
        if (isModeSpeaker() || isModeBoth()) {
            owner.setSvcBindingDispatcherNotify();
        }
    }

    /**
     * Set State to PendingOn
     */
    public void setStatePendingOn() {
        connectionBuilder.setState(ConnectionState.PendingOn);
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
     * Sets that Update was exported at least once
     */
    public void setUpdateAllExported() {
        connectionBuilder.setUpdateAllExported(true);
    }

    /**
     * Sets that Update was exported and connections should have all Bindings up to date
     */
    public void setUpdateExported() {
        connectionBuilder.setUpdateExported(true);
    }

    /**
     * Update Export TimeStamp
     */
    public void setUpdateMessageExportTimestamp() {
        connectionBuilder.setTimestampUpdateMessageExport(TimeConv.toDt(System.currentTimeMillis()));
    }

    /**
     * Update KeepAlive TimeStamp
     */
    public void setUpdateOrKeepaliveMessageTimestamp() {
        connectionBuilder.setTimestampUpdateOrKeepAliveMessage(TimeConv.toDt(System.currentTimeMillis()));
    }

    /**
     * Sets Connections version of Peer
     *
     * @param versionRemote Version to be set
     */
    public void setVersionRemote(Version versionRemote) {
        connectionBuilder.setVersionRemote(versionRemote);
    }

    /**
     * Shutdown Connection and send PurgeAll if Speaker mode,
     * or purge learned Bindings if Listener mode
     */
    public void shutdown() {
        if (isModeListener() && isStateOn()){
            LOG.info("{} PURGE bindings ", this);
            purgeBindings();
        }
        if (isModeSpeaker() && isStateOn()) {
            ByteBuf message = MessageFactory.createPurgeAll();
            LOG.info("{} Sending PURGEALL {}", this, MessageFactory.toString(message));
            try {
                getChannelHandlerContext(ChannelHandlerContextType.SpeakerContext).writeAndFlush(message);
            } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
                LOG.error(this + " Shutdown connection | {} | ", e.getClass().getSimpleName());
                message.release();
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
        if (getNodeIdRemote() != null) {
            result += "/" + getModeRemote().toString().charAt(0) + "v" + getVersionRemote().getIntValue() + " "
                    + NodeIdConv.toString(getNodeIdRemote()) + "]";
        } else {
            result += "]";
        }
        return result;
    }
}
