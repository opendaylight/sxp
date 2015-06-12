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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.sxp.core.behavior.Context;
import org.opendaylight.sxp.core.messaging.AttributeList;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.core.service.UpdateExportTask;
import org.opendaylight.sxp.util.exception.ErrorCodeDataLengthException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.exception.connection.IncompatiblePeerModeException;
import org.opendaylight.sxp.util.exception.connection.SocketAddressNotRecognizedException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeNotFoundException;
import org.opendaylight.sxp.util.exception.unknown.UnknownConnectionModeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownErrorCodeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownErrorSubCodeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpNodeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.SxpTimerTask;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.sxp.util.time.connection.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.PasswordType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.HoldTimeAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.SxpNodeIdAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SxpConnection {

    public enum ChannelHandlerContextType {
        ListenerContext, None, SpeakerContext
    }

    protected static final Logger LOG = LoggerFactory.getLogger(SxpConnection.class.getName());

    public static SxpConnection create(SxpNode owner, Connection connection) throws Exception {
        return new SxpConnection(owner, connection);
    }

    private ConnectionBuilder connectionBuilder;

    private Context context;

    protected HashMap<ChannelHandlerContext, ChannelHandlerContextType> ctxs = new HashMap<>(4);

    protected InetSocketAddress destination;

    protected InetSocketAddress localAddress, remoteAddress;

    protected SxpNode owner;

    protected HashMap<TimerType, ListenableScheduledFuture<?>> timers = new HashMap<>(5);

    private final Deque<Callable<?>> inboundUpdateMessageQueue = new ArrayDeque<>(10),
            outboundUpdateMessageQueue =
                    new ArrayDeque<>(10);
    private final AtomicLong inboundMonitor = new AtomicLong(0), outboundMonitor = new AtomicLong(0);

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

    private SxpConnection(SxpNode owner, Connection connection) throws Exception {
        this.owner = owner;
        this.connectionBuilder = new ConnectionBuilder(connection);

        if (connectionBuilder.getVersion() == null) {
            connectionBuilder.setVersion(Version.Version4);
        }
        setStateOff();

        int port = Configuration.getConstants().getPort();
        if (connection.getTcpPort() != null && connection.getTcpPort().getValue() > 0) {
            port = connection.getTcpPort().getValue();
        }
        this.destination = IpPrefixConv.parseInetPrefix(connection.getPeerAddress().getValue());
        this.destination = new InetSocketAddress(destination.getAddress(), port);

        if (connection.getVersion() != null) {
            setBehaviorContexts(connection.getVersion());
        } else {
            setBehaviorContexts(owner.getVersion());
        }
    }

    public void addChannelHandlerContext(ChannelHandlerContext ctx) throws Exception {
        synchronized (ctxs) {
            this.ctxs.put(ctx, ChannelHandlerContextType.None);
            LOG.debug(this + " Add channel context {}/{}", ctx, ctxs);
        }
    }

    public void cleanUpBindings() throws Exception {
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
        } catch (Exception e) {
            LOG.warn(this + " Unknown message relevant peer node ID | {} | {}", e.getClass().getSimpleName(),
                    e.getMessage());
            return;
        }

        context.getOwner().cleanUpBindings(peerId);
        context.getOwner().notifyService();
    }

    public void closeChannelHandlerContext(ChannelHandlerContext ctx) {
        synchronized (ctxs) {
            for (ChannelHandlerContext _ctx : ctxs.keySet()) {
                if (_ctx.equals(ctx)) {
                    _ctx.close();
                    ctxs.remove(_ctx);
                    return;
                }
            }
        }
    }

    public void closeChannelHandlerContextComplements(ChannelHandlerContext ctx) {
        synchronized (ctxs) {
            List<ChannelHandlerContext> complements = new ArrayList<ChannelHandlerContext>();
            for (ChannelHandlerContext _ctx : ctxs.keySet()) {
                if (!_ctx.equals(ctx)) {
                    complements.add(_ctx);
                }
            }
            for (ChannelHandlerContext _ctx : complements) {
                LOG.info(this + " Dual channel closed {}", _ctx);
                _ctx.close();
            }

            // Complements should be removed here. In a very slow environment,
            // the delay between ctx.close() performing and the following
            // removing channel context after the successful closing from the
            // ctxs register causes ChannelHandlerContextDiscrepancyException
            // in parallel Binding Dispatcher processes.
            for (ChannelHandlerContext complement : complements) {
                ctxs.remove(complement);
            }
        }
    }

    //on inactive clear content
    public void closeChannelHandlerContexts() {
        synchronized (ctxs) {
            if (ctxs != null) {
                for (ChannelHandlerContext _ctx : ctxs.keySet()) {
                    _ctx.close();
                }
                ctxs.clear();
            }
        }
    }

    private void disableKeepAliveMechanism(String log) throws UnknownErrorCodeException, UnknownErrorSubCodeException,
            ErrorCodeDataLengthException, ChannelHandlerContextNotFoundException,
            ChannelHandlerContextDiscrepancyException {
        if (isModeListener()) {
            setHoldTime(0);
        } else if (isModeSpeaker()) {
            setKeepaliveTime(0);
        }
        LOG.info("{} Connection keep-alive mechanism is disabled | {}", toString(), log);
    }

    private void disableKeepAliveMechanismConnectionTermination(int holdTimeMin, int holdTimeMinAcc, int holdTimeMax)
            throws Exception {
        disableKeepAliveMechanism("Unacceptable hold time [min=" + holdTimeMin + " acc=" + holdTimeMinAcc + " max="
                + holdTimeMax + "] | Connection termination");
        ByteBuf error = MessageFactory.createError(ErrorCode.OpenMessageError, ErrorSubCode.UnacceptableHoldTime, null);
        try {
            ChannelHandlerContext ctx = getChannelHandlerContext(ChannelHandlerContextType.SpeakerContext);
            LOG.info("{} Sent ERROR {}", toString(), error.toString());
            ctx.writeAndFlush(error);
            closeChannelHandlerContext(ctx);
        } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
            error.release();
        }
    }

    public List<CapabilityType> getCapabilities() {
        if (connectionBuilder.getCapabilities() == null || connectionBuilder.getCapabilities().getCapability() == null) {
            return new ArrayList<CapabilityType>();
        }
        return connectionBuilder.getCapabilities().getCapability();
    }

    public ChannelHandlerContext getChannelHandlerContext(ChannelHandlerContextType channelHandlerContextType)
            throws ChannelHandlerContextNotFoundException, ChannelHandlerContextDiscrepancyException {
        synchronized (ctxs) {
            if (!isModeBoth()) {
                channelHandlerContextType = ChannelHandlerContextType.None;
            }

            if (ctxs.isEmpty()) {
                throw new ChannelHandlerContextNotFoundException();
            } else if ((isModeBoth() && ctxs.size() > 2) || (!isModeBoth() && ctxs.size() > 1)) {
                LOG.warn(this + " Registered contexts: " + ctxs);
                throw new ChannelHandlerContextDiscrepancyException();
            }

            for (Map.Entry<ChannelHandlerContext, ChannelHandlerContextType> e : ctxs.entrySet()) {
                if (e.getValue().equals(channelHandlerContextType) && !e.getKey().isRemoved()) {
                    return e.getKey();
                }
            }
            throw new ChannelHandlerContextNotFoundException();
        }
    }

    public Connection getConnection() {
        return connectionBuilder.build();
    }

    public Context getContext() {
        return context;
    }

    public InetSocketAddress getDestination() {
        return destination;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public int getHoldTime() {
        if (connectionBuilder.getConnectionTimers() == null
                || connectionBuilder.getConnectionTimers().getHoldTime() == null
                || connectionBuilder.getConnectionTimers().getHoldTime() == null) {
            return 0;
        }
        return connectionBuilder.getConnectionTimers().getHoldTime();
    }

    public int getHoldTimeMax() {
        if (connectionBuilder.getConnectionTimers() == null
                || connectionBuilder.getConnectionTimers().getHoldTimeMax() == null
                || connectionBuilder.getConnectionTimers().getHoldTimeMax() == null) {
            return 0;
        }
        return connectionBuilder.getConnectionTimers().getHoldTimeMax();
    }

    public int getHoldTimeMin() {
        if (connectionBuilder.getConnectionTimers() == null
                || connectionBuilder.getConnectionTimers().getHoldTimeMin() == null
                || connectionBuilder.getConnectionTimers().getHoldTimeMin() == null) {
            return 0;
        }
        return connectionBuilder.getConnectionTimers().getHoldTimeMin();
    }

    public int getHoldTimeMinAcceptable() {
        if (connectionBuilder.getConnectionTimers() == null
                || connectionBuilder.getConnectionTimers().getHoldTimeMinAcceptable() == null
                || connectionBuilder.getConnectionTimers().getHoldTimeMinAcceptable() == null) {
            return 0;
        }
        return connectionBuilder.getConnectionTimers().getHoldTimeMinAcceptable();
    }

    public int getKeepaliveTime() {
        if (connectionBuilder.getConnectionTimers() == null
                || connectionBuilder.getConnectionTimers().getKeepAliveTime() == null
                || connectionBuilder.getConnectionTimers().getKeepAliveTime() == null) {
            return 0;
        }
        return connectionBuilder.getConnectionTimers().getKeepAliveTime();
    }

    public ConnectionMode getMode() {
        if (connectionBuilder.getMode() == null) {
            return ConnectionMode.None;
        }
        return connectionBuilder.getMode();
    }

    public ConnectionMode getModeRemote() {
        if (connectionBuilder.getModeRemote() == null) {
            return ConnectionMode.None;
        }
        return connectionBuilder.getModeRemote();
    }

    public NodeId getNodeIdRemote() {
        return connectionBuilder.getNodeId();
    }

    public ListenableScheduledFuture<?> getNodeTimer(TimerType timerType) {
        return context.getOwner().getTimer(timerType);
    }

    public SxpNode getOwner() {
        return owner;
    }

    public NodeId getOwnerId() {
        return owner.getNodeId();
    }

    public PasswordType getPasswordType() {
        if (connectionBuilder.getPassword() == null) {
            return PasswordType.None;
        }
        return connectionBuilder.getPassword();
    }

    public int getReconciliationTime() {
        if (connectionBuilder.getConnectionTimers() == null
                || connectionBuilder.getConnectionTimers().getReconciliationTime() == null
                || connectionBuilder.getConnectionTimers().getReconciliationTime() == null) {
            return 0;
        }
        return connectionBuilder.getConnectionTimers().getReconciliationTime();
    }

    public ConnectionState getState() {
        return connectionBuilder.getState();
    }

    public ListenableScheduledFuture<?> getTimer(TimerType timerType) {
        return timers.get(timerType);
    }

    public long getTimestampUpdateMessageExport() throws Exception {
        return TimeConv.toLong(connectionBuilder.getTimestampUpdateMessageExport());
    }

    public long getTimestampUpdateOrKeepAliveMessage() throws Exception {
        return TimeConv.toLong(connectionBuilder.getTimestampUpdateOrKeepAliveMessage());
    }

    public Version getVersion() {
        return connectionBuilder.getVersion();
    }

    public Version getVersionRemote() {
        return connectionBuilder.getVersionRemote();
    }

    public boolean hasCapability(CapabilityType capability) {
        if (connectionBuilder.getCapabilities() == null || connectionBuilder.getCapabilities().getCapability() == null) {
            return false;
        }
        return connectionBuilder.getCapabilities().getCapability().contains(capability);
    }

    private void initializeTimers(ConnectionMode connectionMode) throws Exception {

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

    public boolean isBidirectionalBoth() {
        boolean speaker = false;
        boolean listener = false;
        synchronized (ctxs) {
            for (ChannelHandlerContext ctx : ctxs.keySet()) {
                switch (ctxs.get(ctx)) {
                    case SpeakerContext:
                        speaker = true;
                        continue;
                    case ListenerContext:
                        listener = true;
                        continue;
                    default:
                        break;
                }
            }
            return speaker && listener;
        }
    }

    public boolean isModeBoth() {
        return getMode().equals(ConnectionMode.Both);
    }

    public boolean isModeListener() {
        return getMode().equals(ConnectionMode.Listener) || isModeBoth();
    }

    public boolean isModeSpeaker() {
        return getMode().equals(ConnectionMode.Speaker) || isModeBoth();
    }

    public boolean isPurgeAllMessageReceived() {
        return connectionBuilder.isPurgeAllMessageReceived() == null ? false : connectionBuilder
                .isPurgeAllMessageReceived();
    }

    public boolean isStateDeleteHoldDown() {
        return getState().equals(ConnectionState.DeleteHoldDown);
    }

    public boolean isStateOff() {
        return getState().equals(ConnectionState.Off);
    }

    public boolean isStateOn() {
        return getState().equals(ConnectionState.On);
    }

    public boolean isStatePendingOn() {
        return getState().equals(ConnectionState.PendingOn);
    }

    public boolean isUpdateAllExported() {
        return connectionBuilder.isUpdateAllExported() == null ? false : connectionBuilder.isUpdateAllExported();
    }

    public boolean isUpdateExported() {
        return connectionBuilder.isUpdateExported() == null ? false : connectionBuilder.isUpdateExported();
    }

    public boolean isVersion123() {
        return getVersion().equals(Version.Version1) || getVersion().equals(Version.Version2)
                || getVersion().equals(Version.Version3);
    }

    public boolean isVersion4() {
        return getVersion().equals(Version.Version4);
    }

    public void markChannelHandlerContext(ChannelHandlerContext ctx, ChannelHandlerContextType channelHandlerContextType)
            throws Exception {
        synchronized (ctxs) {
            ctxs.remove(ctx);
            ctxs.put(ctx, channelHandlerContextType);
        }
    }

    public void purgeBindings() throws Exception {
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
        } catch (Exception e) {
            LOG.warn(this + " Unknown message relevant peer node ID | {} | {}", e.getClass().getSimpleName(),
                    e.getMessage());
            return;
        }

        context.getOwner().purgeBindings(peerId);
        context.getOwner().notifyService();
        setStateOff();
    }

    public void resetUpdateExported() {
        connectionBuilder.setUpdateExported(false);
    }

    public void setBehaviorContexts(Version version) throws UnknownSxpNodeException, UnknownVersionException {
        connectionBuilder.setCapabilities(Configuration.getCapabilities(version));
        connectionBuilder.setVersion(version);
        context = new Context(owner, version);
    }

    // Bidirectional uses separated Speaker part and Listener part connection
    // setup.
    public void setConnection(OpenMessage message) throws Exception {
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

    public void setConnectionListenerPart(OpenMessage message) throws Exception {

        // Node modes compatibility.
        if (getMode().equals(ConnectionMode.Listener) && !message.getSxpMode().equals(ConnectionMode.Speaker)
                || getMode().equals(ConnectionMode.Speaker) && !message.getSxpMode().equals(ConnectionMode.Listener)) {
            throw new IncompatiblePeerModeException(getMode(), message.getSxpMode());
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
        } catch (Exception e) {
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

    public void setConnectionSpeakerPart(OpenMessage message) throws Exception {
        // Node modes compatibility.
        if (getMode().equals(ConnectionMode.Listener) && !message.getSxpMode().equals(ConnectionMode.Speaker)
                || getMode().equals(ConnectionMode.Speaker) && !message.getSxpMode().equals(ConnectionMode.Listener)) {
            throw new IncompatiblePeerModeException(getMode(), message.getSxpMode());
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
        } catch (Exception e) {
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

    public void setDeleteHoldDownTimer() throws Exception {
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

    public void setDestinationPort(int port) {
        this.destination = new InetSocketAddress(destination.getAddress(), port);
    }

    public void setHoldTime(int value) {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder(
                connectionBuilder.getConnectionTimers());
        connectionTimersBuilder.setHoldTime(value);
        connectionBuilder.setConnectionTimers(connectionTimersBuilder.build());
    }

    public void setHoldTimeMin(int value) {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder(
                connectionBuilder.getConnectionTimers());
        connectionTimersBuilder.setHoldTimeMin(value);
        connectionBuilder.setConnectionTimers(connectionTimersBuilder.build());
    }

    public void setHoldTimeMinAcceptable(int value) {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder(
                connectionBuilder.getConnectionTimers());
        connectionTimersBuilder.setHoldTimeMinAcceptable(value);
        connectionBuilder.setConnectionTimers(connectionTimersBuilder.build());
    }

    public void setInetSocketAddresses(SocketAddress localAddress, SocketAddress remoteAddress) throws Exception {
        if (!(localAddress instanceof InetSocketAddress)) {
            throw new SocketAddressNotRecognizedException(localAddress);
        } else if (!(remoteAddress instanceof InetSocketAddress)) {
            throw new SocketAddressNotRecognizedException(remoteAddress);
        }

        this.localAddress = (InetSocketAddress) localAddress;
        this.remoteAddress = (InetSocketAddress) remoteAddress;
    }

    public void setKeepaliveTime(int value) {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder(
                connectionBuilder.getConnectionTimers());
        connectionTimersBuilder.setKeepAliveTime(value);
        connectionBuilder.setConnectionTimers(connectionTimersBuilder.build());
    }

    public void setMode(ConnectionMode mode) {
        connectionBuilder.setMode(mode);
    }

    public void setModeRemote(ConnectionMode mode) {
        connectionBuilder.setModeRemote(mode);
    }

    public void setNodeIdRemote(NodeId nodeId) {
        connectionBuilder.setNodeId(nodeId);
    }

    public void setOwner(SxpNode owner) {
        this.owner = owner;
    }

    public void setPurgeAllMessageReceived() {
        connectionBuilder.setPurgeAllMessageReceived(true);
    }

    public void setReconciliationTimer() throws Exception {
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

    public void setStateAdminDown() {
        connectionBuilder.setState(ConnectionState.AdministrativelyDown);
    }

    public void setStateDeleteHoldDown() throws Exception {
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
        } catch (Exception e) {
            LOG.warn(this + " Unknown message relevant peer node ID | {} | {}", e.getClass().getSimpleName(),
                    e.getMessage());
            return;
        }
        context.getOwner().setAsCleanUp(peerId);
    }

    public void setStateOff() {
        closeChannelHandlerContexts();
        connectionBuilder.setState(ConnectionState.Off);
        connectionBuilder.setUpdateAllExported(false);
        connectionBuilder.setUpdateExported(false);
        connectionBuilder.setPurgeAllMessageReceived(false);
        stopTimers();
        clearMessages();
    }

    public void setStateOff(ChannelHandlerContext ctx) {
        closeChannelHandlerContext(ctx);
        if (ctxs.isEmpty()) {
            connectionBuilder.setState(ConnectionState.Off);
        }
        connectionBuilder.setUpdateAllExported(false);
        connectionBuilder.setUpdateExported(false);
        connectionBuilder.setPurgeAllMessageReceived(false);
        stopTimers();
        clearMessages();
    }

    private void stopTimers() {
        try {
            setTimer(TimerType.DeleteHoldDownTimer, null);
            setTimer(TimerType.ReconciliationTimer, null);
            setTimer(TimerType.HoldTimer, 0);
            setTimer(TimerType.KeepAliveTimer, 0);
        } catch (UnknownTimerTypeException e) {
            LOG.warn("{} Error stopping Timers ", this, e);
        }
    }

    public void setStateOn() {
        if (isModeSpeaker() || isModeBoth()) {
            owner.setSvcBindingDispatcherNotify();
        }
        connectionBuilder.setState(ConnectionState.On);
    }

    public void setStatePendingOn() {
        connectionBuilder.setState(ConnectionState.PendingOn);
    }

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

    public ListenableScheduledFuture<?> setTimer(TimerType timerType, ListenableScheduledFuture<?> timer) {
        ListenableScheduledFuture<?> t = this.timers.put(timerType, timer);
        if (t != null && !t.isDone()) {
            t.cancel(false);
        }
        return timer;
    }

    public void setUpdateAllExported() {
        connectionBuilder.setUpdateAllExported(true);
    }

    public void setUpdateExported() {
        connectionBuilder.setUpdateExported(true);
    }

    public void setUpdateMessageExportTimestamp() {
        connectionBuilder.setTimestampUpdateMessageExport(TimeConv.toDt(System.currentTimeMillis()));
    }

    public void setUpdateOrKeepaliveMessageTimestamp() {
        connectionBuilder.setTimestampUpdateOrKeepAliveMessage(TimeConv.toDt(System.currentTimeMillis()));
    }

    public void setVersionRemote(Version versionRemote) {
        connectionBuilder.setVersionRemote(versionRemote);
    }

    public void shutdown() {
        if (isModeListener()){
            try {
                LOG.info("{} PURGE bindings ", this);
                purgeBindings();
            } catch (Exception e) {
                LOG.error(this + " Shutdown connection | {} | ", e.getClass().getSimpleName(), e);
            }
        }
        if (isModeSpeaker()) {
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
