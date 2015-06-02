/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import io.netty.channel.Channel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.core.handler.MessageDecoder;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.core.service.BindingHandler;
import org.opendaylight.sxp.core.service.BindingManager;
import org.opendaylight.sxp.core.service.ConnectFacade;
import org.opendaylight.sxp.core.service.Service;
import org.opendaylight.sxp.util.Security;
import org.opendaylight.sxp.util.database.Database;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseProvider;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseProvider;
import org.opendaylight.sxp.util.exception.connection.SocketAddressNotRecognizedException;
import org.opendaylight.sxp.util.exception.node.DatabaseNotFoundException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpConnectionException;
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.sxp.util.time.SxpTimerTask;
import org.opendaylight.sxp.util.time.node.RetryOpenTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The source-group tag exchange protocol (SXP) aware node implementation. SXP
 * is a control protocol to propagate IP address to Source Group Tag (SGT)
 * binding information across network devices.
 * <p>
 * Source groups are the endpoints connecting to the network that have common
 * network policies. Each source group is identified by a unique SGT value. The
 * SGT to which an endpoint belongs can be assigned statically or dynamically,
 * and the SGT can be used as a classifier in network policies.
 */
public final class SxpNode extends ConcurrentHashMap<InetSocketAddress, SxpConnection> {

    private static final Logger LOG = LoggerFactory.getLogger(SxpNode.class.getName());

    /** */
    private static final long serialVersionUID = 5347662502940140082L;

    protected static final long THREAD_DELAY = 10;

    /**
     * Create new instance of SxpNode with empty databases
     * and default ThreadWorkers
     *
     * @param nodeId ID of newly created Node
     * @param node   Node setup data
     * @return New instance of SxpNode
     * @throws Exception
     */
    public static SxpNode createInstance(NodeId nodeId, SxpNodeIdentity node) throws Exception {
        return createInstance(nodeId, node, new MasterDatabaseImpl(), new SxpDatabaseImpl());
    }

    /**
     * Create new instance of SxpNode containing provided database data
     * and default ThreadWorkers
     *
     * @param nodeId         ID of newly created Node
     * @param node           Node setup data
     * @param masterDatabase Data which will be added to Master-DB
     * @param sxpDatabase    Data which will be added to SXP-DB
     * @return New instance of SxpNode
     * @throws Exception
     */
    public static SxpNode createInstance(NodeId nodeId, SxpNodeIdentity node, MasterDatabaseProvider masterDatabase,
            SxpDatabaseProvider sxpDatabase) throws Exception {
        return createInstance(nodeId, node, masterDatabase, sxpDatabase, new ThreadsWorker());
    }

    /**
     * Create new instance of SxpNode containing provided database data
     * and custom ThreadWorkers
     *
     * @param nodeId         ID of newly created Node
     * @param node           Node setup data
     * @param masterDatabase Data which will be added to Master-DB
     * @param sxpDatabase    Data which will be added to SXP-DB
     * @param worker         Thread workers which will be executing task inside SxpNode
     * @return New instance of SxpNode
     * @throws Exception
     */
    public static SxpNode createInstance(NodeId nodeId, SxpNodeIdentity node, MasterDatabaseProvider masterDatabase,
            SxpDatabaseProvider sxpDatabase, ThreadsWorker worker) throws Exception {
        return new SxpNode(nodeId, node, masterDatabase, sxpDatabase, worker);
    }

    protected volatile MasterDatabaseProvider _masterDatabase = null;

    protected volatile SxpDatabaseProvider _sxpDatabase = null;

    private final HandlerFactory handlerFactoryClient = new HandlerFactory(MessageDecoder.createClientProfile(this));

    private final HandlerFactory handlerFactoryServer = new HandlerFactory(MessageDecoder.createServerProfile(this));

    private SxpNodeIdentityBuilder nodeBuilder;

    private NodeId nodeId;

    private Channel serverChannel;

    protected InetAddress sourceIp;

    private final Service svcBindingManager, svcBindingDispatcher;
    private final ThreadsWorker worker;

    /** Common timers setup. */
    private HashMap<TimerType, ListenableScheduledFuture<?>> timers = new HashMap<>(6);

    private SxpNode(NodeId nodeId, SxpNodeIdentity node, MasterDatabaseProvider masterDatabase,
            SxpDatabaseProvider sxpDatabase,ThreadsWorker worker) throws Exception {
        super(Configuration.getConstants().getNodeConnectionsInitialSize());
        this.worker = worker;
        this.nodeId = nodeId;
        this.nodeBuilder = new SxpNodeIdentityBuilder(node);

        if (nodeBuilder.getSourceIp() == null) {
            this.sourceIp = Search.getBestLocalDeviceAddress();
            LOG.debug(toString() + " Setting-up the best local device IP address [sourceIp=\"" + sourceIp + "\"]");
        } else {
            this.sourceIp = IpPrefixConv.parseInetPrefix(IpPrefixConv.toString(nodeBuilder.getSourceIp())).getAddress();
        }

        this.nodeBuilder.setSecurity(setPassword(nodeBuilder.getSecurity()));
        this._masterDatabase = masterDatabase;
        this._sxpDatabase = sxpDatabase;

        addConnections(nodeBuilder.getConnections());

        svcBindingManager = new BindingManager(this);
        svcBindingDispatcher = new BindingDispatcher(this);

        // Start services.
        if (isEnabled()) {
            start();
        }
    }

    public void addConnection(Connection connection) throws Exception {
        if (connection == null) {
            return;
        }

        SxpConnection _connection = SxpConnection.create(this, connection);
        put(_connection.getDestination(), _connection);
    }

    public void addConnections(Connections connections) throws Exception {
        if (connections == null || connections.getConnection() == null || connections.getConnection().isEmpty()) {
            return;
        }
        for (Connection connection : connections.getConnection()) {
            addConnection(connection);
        }
    }

    public void cleanUpBindings(NodeId nodeID) throws Exception {
        if (svcBindingManager instanceof BindingManager) {
            ((BindingManager) svcBindingManager).cleanUpBindings(nodeID);
        }
    }

    @Override
    public SxpConnection get(Object key) {
        if (!(key instanceof InetSocketAddress)) {
            return null;
        }
        return super.get(key);
    }

    public List<SxpConnection> getAllDeleteHoldDownConnections() {
        List<SxpConnection> connections = new ArrayList<SxpConnection>();
        for (InetSocketAddress inetAddress : keySet()) {
            if (!(get(inetAddress) instanceof SxpConnection)) {
                continue;
            }
            SxpConnection connection = get(inetAddress);
            if (connection.isStateDeleteHoldDown()) {
                connections.add(connection);
            }
        }
        return connections;
    }

    public List<SxpConnection> getAllOffConnections() {
        List<SxpConnection> connections = new ArrayList<SxpConnection>();
        for (InetSocketAddress inetAddress : keySet()) {
            if (!(get(inetAddress) instanceof SxpConnection)) {
                continue;
            }
            SxpConnection connection = get(inetAddress);
            if (connection.isStateOff()) {
                connections.add(connection);
            }
        }
        return connections;
    }

    public List<SxpConnection> getAllOnConnections() {
        List<SxpConnection> connections = new ArrayList<SxpConnection>();
        for (InetSocketAddress inetAddress : keySet()) {
            if (!(get(inetAddress) instanceof SxpConnection)) {
                continue;
            }
            SxpConnection connection = get(inetAddress);
            if (connection.isStateOn()) {
                connections.add(connection);
            }
        }
        return connections;
    }

    public List<SxpConnection> getAllOnListenerConnections() {
        List<SxpConnection> connections = new ArrayList<SxpConnection>();
        for (InetSocketAddress inetAddress : keySet()) {
            if (!(get(inetAddress) instanceof SxpConnection)) {
                continue;
            }
            SxpConnection connection = get(inetAddress);
            if (connection.isStateOn()
                    && (connection.getMode().equals(ConnectionMode.Listener) || connection.isModeBoth())) {
                connections.add(connection);
            }
        }
        return connections;
    }

    public List<SxpConnection> getAllOnSpeakerConnections() {
        List<SxpConnection> connections = new ArrayList<SxpConnection>();
        for (InetSocketAddress inetAddress : keySet()) {
            if (!(get(inetAddress) instanceof SxpConnection)) {
                continue;
            }
            SxpConnection connection = get(inetAddress);
            if (connection.isStateOn()
                    && (connection.getMode().equals(ConnectionMode.Speaker) || connection.isModeBoth())) {
                connections.add(connection);
            }
        }
        return connections;
    }

    public synchronized MasterDatabaseProvider getBindingMasterDatabase() throws Exception {
        if (_masterDatabase == null) {
            throw new DatabaseNotFoundException(DatabaseType.MasterDatabase);
        }
        return _masterDatabase;
    }

    public synchronized SxpDatabaseProvider getBindingSxpDatabase() throws Exception {
        if (_sxpDatabase == null) {
            throw new DatabaseNotFoundException(DatabaseType.SxpBindingDatabase);
        }
        return _sxpDatabase;
    }

    public SxpConnection getByAddress(InetSocketAddress inetSocketAddress) {
        for (InetSocketAddress _inetSocketAddress : keySet()) {
            if (_inetSocketAddress.getAddress().equals(inetSocketAddress.getAddress())) {
                return get(_inetSocketAddress);
            }
        }
        return null;
    }

    public SxpConnection getByPort(int port) {
        for (InetSocketAddress inetSocketAddress : keySet()) {
            if (inetSocketAddress.getPort() == port) {
                return get(inetSocketAddress);
            }
        }
        return null;
    }

    public SxpConnection getConnection(SocketAddress socketAddress) throws Exception {
        if (!(socketAddress instanceof InetSocketAddress)) {
            throw new SocketAddressNotRecognizedException(socketAddress);
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;

        SxpConnection connection = get(inetSocketAddress);
        // Local registered ports.
        if (connection == null) {
            InetSocketAddress clientAddress = ConnectFacade.getClientUsedAddress(inetSocketAddress.getPort());
            if (clientAddress != null) {
                connection = getByPort(clientAddress.getPort());
            }
        }
        // Devices addresses.
        if (connection == null) {
            connection = getByAddress(inetSocketAddress);
        }
        if (connection == null || !(connection instanceof SxpConnection)) {
            throw new UnknownSxpConnectionException("InetSocketAddress: " + inetSocketAddress);
        }
        return connection;
    }

    public int getExpansionQuantity() {
        if (nodeBuilder.getMappingExpanded() == null) {
            return 0;
        }
        return nodeBuilder.getMappingExpanded();
    }

    public int getHoldTime() {
        if (nodeBuilder.getTimers() == null || nodeBuilder.getTimers().getListenerProfile() == null
                || nodeBuilder.getTimers().getListenerProfile().getHoldTime() == null) {
            return 0;
        }
        return nodeBuilder.getTimers().getListenerProfile().getHoldTime();
    }

    public int getHoldTimeMax() {
        if (nodeBuilder.getTimers() == null || nodeBuilder.getTimers().getListenerProfile() == null
                || nodeBuilder.getTimers().getListenerProfile().getHoldTimeMax() == null) {
            return 0;
        }
        return nodeBuilder.getTimers().getListenerProfile().getHoldTimeMax();
    }

    public int getHoldTimeMin() {
        if (nodeBuilder.getTimers() == null || nodeBuilder.getTimers().getListenerProfile() == null
                || nodeBuilder.getTimers().getListenerProfile().getHoldTimeMin() == null) {
            return 0;
        }
        return nodeBuilder.getTimers().getListenerProfile().getHoldTimeMin();
    }

    public int getHoldTimeMinAcceptable() {
        if (nodeBuilder.getTimers() == null || nodeBuilder.getTimers().getSpeakerProfile() == null
                || nodeBuilder.getTimers().getSpeakerProfile().getHoldTimeMinAcceptable() == null) {
            return 0;
        }
        return nodeBuilder.getTimers().getSpeakerProfile().getHoldTimeMinAcceptable();
    }

    public int getKeepAliveTime() {
        if (nodeBuilder.getTimers() == null || nodeBuilder.getTimers().getSpeakerProfile() == null
                || nodeBuilder.getTimers().getSpeakerProfile().getKeepAliveTime() == null) {
            return 0;
        }
        return nodeBuilder.getTimers().getSpeakerProfile().getKeepAliveTime();
    }

    public String getName() {
        return nodeBuilder.getName() == null || nodeBuilder.getName().isEmpty() ? NodeIdConv.toString(nodeId)
                : nodeBuilder.getName();
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public String getPassword() {
        if (nodeBuilder.getSecurity() == null) {
            return null;
        }

        return nodeBuilder.getSecurity().getPassword();
    }

    public int getRetryOpenTime() {
        if (nodeBuilder.getTimers() == null || nodeBuilder.getTimers().getRetryOpenTime() == null) {
            return 0;
        }
        return nodeBuilder.getTimers().getRetryOpenTime();
    }

    public int getServerPort() {
        if (nodeBuilder.getTcpPort() == null || nodeBuilder.getTcpPort().getValue() == null) {
            return -1;
        }

        return nodeBuilder.getTcpPort().getValue();
    }

    public ListenableScheduledFuture<?> getTimer(TimerType timerType) {
        return timers.get(timerType);
    }

    public Version getVersion() {
        if (nodeBuilder.getVersion() == null) {
            return Version.Version4;
        }
        return nodeBuilder.getVersion();
    }

    public boolean isEnabled() {
        return nodeBuilder.isEnabled() == null ? false : nodeBuilder.isEnabled();
    }

    public boolean isSvcBindingDispatcherStarted() {
        return svcBindingDispatcher != null;
    }

    public boolean isSvcBindingManagerStarted() {
        return svcBindingManager != null;
    }

    public void notifyService() {
        if (isSvcBindingManagerStarted()) {
            setSvcBindingManagerNotify();
        } else if (isSvcBindingDispatcherStarted()) {
            setSvcBindingDispatcherDispatch();
        }
    }

    public synchronized void openConnections() throws Exception {
        // Server not created yet.
        if (serverChannel == null) {
            return;
        }

        final SxpNode node = this;

        int connectionsAllSize = size();
        int connectionsOnSize = getAllOnConnections().size();
        List<SxpConnection> connections = getAllOffConnections();
        int connectionsOffSize = connections.size();

        for (final SxpConnection connection : connections) {
            LOG.info(connection + " Open connection thread [Id/X/O/All=\"" + (connections.indexOf(connection) + 1)
                    + "/" + connectionsOffSize + "/" + connectionsOnSize + "/" + connectionsAllSize + "\"]");

            worker.executeTask(new Runnable() {

                @Override public void run() {
                    try {
                        connection.setStatePendingOn();
                        ConnectFacade.createClient(node, connection, handlerFactoryClient);

                    } catch (Exception e) {
                        // Connection is not established by adverse party
                        // before.
                        if (connection.isStatePendingOn()) {
                            connection.setStateOff();
                            LOG.warn(connection + " {}", e.getMessage());
                        }
                    }
                }
            }, ThreadsWorker.WorkerType.Default);
        }
    }

    public void processUpdateMessage(UpdateMessage message, SxpConnection connection) throws InterruptedException {
        BindingHandler.processUpdateMessage(message, connection);
    }

    public void processUpdateMessage(UpdateMessageLegacy message, SxpConnection connection) throws InterruptedException {
        BindingHandler.processUpdateMessage(message, connection);
    }

    public void purgeBindings(NodeId nodeID) throws Exception {
        if (svcBindingManager instanceof BindingManager) {
            ((BindingManager) svcBindingManager).purgeBindings(nodeID);
        }
    }

    public void putLocalBindingsMasterDatabase(MasterDatabase masterDatabaseConfiguration) throws Exception {
        Source source = null;
        if (masterDatabaseConfiguration.getSource() != null) {
            for (Source _source : masterDatabaseConfiguration.getSource()) {
                if (_source.getBindingSource().equals(DatabaseBindingSource.Local)) {
                    source = _source;
                    break;
                }
            }
        }

        if (source != null && source.getPrefixGroup() != null && !source.getPrefixGroup().isEmpty()) {
            getBindingMasterDatabase().addBindingsLocal(Database.assignPrefixGroups(nodeId, source.getPrefixGroup()));
            notifyService();
        }
    }

    public SxpConnection removeConnection(InetSocketAddress destination) throws Exception {
        SxpConnection connection = remove(destination);
        connection.shutdown();
        return connection;
    }

    public void setAsCleanUp(NodeId nodeID) throws Exception {
        if (svcBindingManager instanceof BindingManager) {
            ((BindingManager) svcBindingManager).setAsCleanUp(nodeID);
        }
    }

    protected org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.node.fields.Security setPassword(
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.node.fields.Security security)
            throws Exception {
        SecurityBuilder securityBuilder = new SecurityBuilder();
        if (security == null || security.getPassword() == null || security.getPassword().isEmpty()) {
            securityBuilder.setPassword("");
            return securityBuilder.build();
        }

        if (nodeBuilder.getSecurity() != null && nodeBuilder.getSecurity().getPassword() != null
                && !nodeBuilder.getSecurity().getPassword().isEmpty()
                && !nodeBuilder.getSecurity().getPassword().equals(security.getPassword())) {
            shutdownConnections();
        }
        securityBuilder.setPassword(security.getPassword());
        securityBuilder.setMd5Digest(Security.getMD5s(security.getPassword()));
        return securityBuilder.build();
    }

    public void setServerChannel(Channel serverChannel) throws Exception {
        this.serverChannel = serverChannel;

        // Open connections (preliminarily)
        openConnections();

        this.serverChannelInit.set(false);
    }

    public void setSvcBindingDispatcherDispatch() {
        if (isSvcBindingDispatcherStarted() && svcBindingDispatcher instanceof BindingDispatcher) {
            ((BindingDispatcher) svcBindingDispatcher).dispatch();
        }
    }

    /**
     * Notify BindingDispatcher to execute dispatch of bindings on reconnected connections
     */
    public void setSvcBindingDispatcherNotify() {
        if (isSvcBindingDispatcherStarted()) {
            svcBindingDispatcher.notifyChange();
        }
    }

    public void setSvcBindingManagerNotify() {
        if (isSvcBindingManagerStarted()) {
            svcBindingManager.notifyChange();
        }
    }

    public synchronized ListenableScheduledFuture<?> setTimer(TimerType timerType, int period)
            throws UnknownTimerTypeException {
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

    public ListenableScheduledFuture<?> setTimer(TimerType timerType, ListenableScheduledFuture<?> timer) {
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
        if (svcBindingDispatcher != null && svcBindingDispatcher instanceof BindingDispatcher) {
            ((BindingDispatcher) svcBindingDispatcher).setPartitionSize(size);
        }
    }

    /**
     * Gets Execution handler of current Node
     *
     * @return ThreadsWorker reference
     */
    public ThreadsWorker getWorker(){
        return worker;
    }

    /**
     * Administratively shutdown.
     */
    public synchronized void shutdown() {
        // Wait until server channel ends its own initialization.
        while (serverChannelInit.get()) {
            try {
                wait(THREAD_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            setTimer(TimerType.RetryOpenTimer, 0);
        } catch (UnknownTimerTypeException e) {
            LOG.warn("{} Error stopping Timers ", this, e);
        }
        shutdownConnections();

        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }
        if (svcBindingDispatcher != null) {
            svcBindingDispatcher.cancel();
        }
        if (svcBindingManager != null) {
            svcBindingManager.cancel();
        }
        nodeBuilder.setEnabled(false);
    }

    public synchronized void shutdownConnections() {
        for (SxpConnection connection : values()) {
            connection.shutdown();

            if (connection.isModeListener()) {
                try {
                    getBindingMasterDatabase().purgeBindings(connection.getNodeIdRemote());
                    getBindingSxpDatabase().purgeBindings(connection.getNodeIdRemote());
                } catch (Exception e) {
                    LOG.error(connection + " Shutdown connections | {} | {}", e.getClass().getSimpleName(),
                            e.getMessage());
                }
            }
        }
    }

    private AtomicBoolean serverChannelInit = new AtomicBoolean(false);

    public void start() throws Exception {
        // Put local bindings before services startup.
        MasterDatabase masterDatabaseConfiguration = nodeBuilder.getMasterDatabase();
        if (masterDatabaseConfiguration != null) {
            putLocalBindingsMasterDatabase(masterDatabaseConfiguration);
            // LOG.info(this + " " + getBindingMasterDatabase().toString());
        }

        final SxpNode node = this;
        worker.executeTask(new Runnable() {

            @Override public void run() {
                try {
                    serverChannelInit.set(true);
                    ConnectFacade.createServer(node, getServerPort(), handlerFactoryServer);

                } catch (Exception e) {
                    LOG.warn(node + " {}", e.getMessage());
                }
            }
        }, ThreadsWorker.WorkerType.Default);
        if (getRetryOpenTime() > 0) {
            setTimer(TimerType.RetryOpenTimer, getRetryOpenTime());
        }
        nodeBuilder.setEnabled(true);
    }

    @Override
    public String toString() {
        return "["
                + (nodeBuilder.getName() != null && !nodeBuilder.getName().isEmpty() ? nodeBuilder.getName() + ":" : "")
                + NodeIdConv.toString(nodeId) + "]";
    }
}
