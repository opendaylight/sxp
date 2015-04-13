/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import io.netty.channel.Channel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.core.handler.MessageDecoder;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.core.service.BindingHandler;
import org.opendaylight.sxp.core.service.BindingManager;
import org.opendaylight.sxp.core.service.ConnectFacade;
import org.opendaylight.sxp.core.service.Connector;
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
import org.opendaylight.sxp.util.time.ManagedTimer;
import org.opendaylight.sxp.util.time.node.TimerFactory;
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
public final class SxpNode extends HashMap<InetSocketAddress, SxpConnection> {

    // private static final ExecutorService executor =
    // Executors.newCachedThreadPool();

    private static final Logger LOG = LoggerFactory.getLogger(SxpNode.class.getName());

    /** */
    private static final long serialVersionUID = 5347662502940140082L;

    protected static final long THREAD_DELAY = 10;

    public static SxpNode createInstance(NodeId nodeId, SxpNodeIdentity node) throws Exception {
        return new SxpNode(nodeId, node, new MasterDatabaseImpl(), new SxpDatabaseImpl());
    }

    public static SxpNode createInstance(NodeId nodeId, SxpNodeIdentity node, MasterDatabaseProvider masterDatabase,
            SxpDatabaseProvider sxpDatabase) throws Exception {
        return new SxpNode(nodeId, node, masterDatabase, sxpDatabase);
    }

    protected volatile MasterDatabaseProvider _masterDatabase = null;

    protected volatile SxpDatabaseProvider _sxpDatabase = null;

    private final HandlerFactory handlerFactoryClient = new HandlerFactory(MessageDecoder.createClientProfile(this));

    private final HandlerFactory handlerFactoryServer = new HandlerFactory(MessageDecoder.createServerProfile(this));

    private SxpNodeIdentityBuilder nodeBuilder;

    private NodeId nodeId;

    private Channel serverChannel;

    protected InetAddress sourceIp;

    private Service svcBindingHandler, svcBindingDispatcher;

    private boolean svcBindingHandlerStarted, svcBindingManagerStarted, svcBindingDispatcherStarted,
            svcConnectorStarted;

    private final Service svcBindingManager, svcConnector;

    /** Common timers setup. */
    private HashMap<TimerType, ManagedTimer> timers = new HashMap<TimerType, ManagedTimer>(6);

    public SxpNode(NodeId nodeId, SxpNodeIdentity node, MasterDatabaseProvider masterDatabase,
            SxpDatabaseProvider sxpDatabase) throws Exception {
        super(Configuration.getConstants().getNodeConnectionsInitialSize());
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
        svcConnector = new Connector(this);

        if (nodeBuilder.getTimers() != null && nodeBuilder.getTimers().getRetryOpenTime() != null
                && nodeBuilder.getTimers().getRetryOpenTime() > 0) {
            setTimer(TimerType.RetryOpenTimer, nodeBuilder.getTimers().getRetryOpenTime());
        }
        // Listener global.
        if (nodeBuilder.getTimers() != null && nodeBuilder.getTimers().getListenerProfile() != null
                && nodeBuilder.getTimers().getListenerProfile().getHoldTime() != null
                && nodeBuilder.getTimers().getListenerProfile().getHoldTime() > 0) {
            setTimer(TimerType.HoldTimer, nodeBuilder.getTimers().getListenerProfile().getHoldTime());
        }
        // Speaker global.
        if (nodeBuilder.getTimers() != null && nodeBuilder.getTimers().getSpeakerProfile() != null
                && nodeBuilder.getTimers().getSpeakerProfile().getKeepAliveTime() != null
                && nodeBuilder.getTimers().getSpeakerProfile().getKeepAliveTime() > 0) {
            setTimer(TimerType.KeepAliveTimer, nodeBuilder.getTimers().getSpeakerProfile().getKeepAliveTime());
        }

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

        // At least 1 Listener is specified.
        if (svcBindingHandler == null
                && (connection.getMode().equals(ConnectionMode.Listener) || connection.getMode().equals(
                        ConnectionMode.Both))) {
            svcBindingHandler = new BindingHandler(this);

            if (svcBindingHandler != null && !svcBindingHandlerStarted) {
                ExecutorService executor = Executors.newFixedThreadPool(2);
                svcBindingHandler.reset();
                executor.execute(svcBindingHandler);
                svcBindingHandlerStarted = true;
                executor.shutdown();
            }
        }
        // At least 1 Speaker is specified
        if (svcBindingDispatcher == null
                && (connection.getMode().equals(ConnectionMode.Speaker) || connection.getMode().equals(
                        ConnectionMode.Both))) {
            svcBindingDispatcher = new BindingDispatcher(this);

            if (svcBindingDispatcher != null && !svcBindingDispatcherStarted) {
                ExecutorService executor = Executors.newFixedThreadPool(2);
                svcBindingDispatcher.reset();
                executor.execute(svcBindingDispatcher);
                svcBindingDispatcherStarted = true;
                executor.shutdown();
            }
        }
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

    public ManagedTimer getTimer(TimerType timerType) {
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
        return svcBindingDispatcherStarted;
    }

    public boolean isSvcBindingHandlerStartedStarted() {
        return svcBindingHandlerStarted;
    }

    public boolean isSvcBindingManagerStarted() {
        return svcBindingManagerStarted;
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

        ExecutorService executor = Executors.newCachedThreadPool();
        final SxpNode node = this;

        int connectionsAllSize = size();
        int connectionsOnSize = getAllOnConnections().size();
        List<SxpConnection> connections = getAllOffConnections();
        int connectionsOffSize = connections.size();

        for (final SxpConnection connection : connections) {
            LOG.info(connection + " Open connection thread [Id/X/O/All=\"" + (connections.indexOf(connection) + 1)
                    + "/" + connectionsOffSize + "/" + connectionsOnSize + "/" + connectionsAllSize + "\"]");

            executor.execute(new Runnable() {
                @Override
                public void run() {
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
            });

            try {
                Thread.sleep(THREAD_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
    }

    public void processUpdateMessage(UpdateMessage message, SxpConnection connection) throws InterruptedException {
        if (svcBindingHandler instanceof BindingHandler) {
            ((BindingHandler) svcBindingHandler).processUpdateMessage(message, connection);
        }
    }

    public void processUpdateMessage(UpdateMessageLegacy message, SxpConnection connection) throws InterruptedException {
        if (svcBindingHandler instanceof BindingHandler) {
            ((BindingHandler) svcBindingHandler).processUpdateMessage(message, connection);
        }
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
        if (svcBindingDispatcher instanceof BindingDispatcher && isSvcBindingDispatcherStarted()) {
            ((BindingDispatcher) svcBindingDispatcher).dispatch();
        }
    }

    public void setSvcBindingManagerNotify() {
        if (isSvcBindingManagerStarted()) {
            svcBindingManager.notifyChange();
        }
    }

    public ManagedTimer setTimer(TimerType timerType, int period) throws UnknownTimerTypeException {
        ManagedTimer timer = TimerFactory.createTimer(timerType, period, this);
        this.timers.put(timerType, timer);
        return timer;
    }

    public ManagedTimer setTimer(TimerType timerType, ManagedTimer timer) {
        this.timers.put(timerType, timer);
        return timer;
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

        shutdownConnections();

        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }
        if (svcBindingDispatcher != null) {
            svcBindingDispatcher.cancel();
            svcBindingDispatcherStarted = false;
        }
        if (svcBindingHandler != null) {
            svcBindingHandler.cancel();
            svcBindingHandlerStarted = false;
        }
        if (svcBindingManager != null) {
            svcBindingManager.cancel();
            svcBindingManagerStarted = false;
        }
        if (svcConnector != null) {
            svcConnector.cancel();
            svcConnectorStarted = false;
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

            try {
                Thread.sleep(THREAD_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private AtomicBoolean serverChannelInit = new AtomicBoolean(false);

    public void start() throws Exception {
        ExecutorService executor = Executors.newCachedThreadPool();

        // Put local bindings before services startup.
        MasterDatabase masterDatabaseConfiguration = nodeBuilder.getMasterDatabase();
        if (masterDatabaseConfiguration != null) {
            putLocalBindingsMasterDatabase(masterDatabaseConfiguration);
            // LOG.info(this + " " + getBindingMasterDatabase().toString());
        }

        // Speaker: Binding dispatcher service (as 1st).
        if (svcBindingDispatcher != null && !svcBindingDispatcherStarted) {
            svcBindingDispatcher.reset();
            executor.execute(svcBindingDispatcher);
            svcBindingDispatcherStarted = true;
        }
        // Listener: Binding handler service.
        if (svcBindingHandler != null && !svcBindingHandlerStarted) {
            svcBindingHandler.reset();
            executor.execute(svcBindingHandler);
            svcBindingHandlerStarted = true;
        }
        // Common: Binding manager service.
        if (svcBindingManager != null && !svcBindingManagerStarted) {
            svcBindingManager.reset();
            executor.execute(svcBindingManager);
            svcBindingManagerStarted = true;
        }

        final SxpNode node = this;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    serverChannelInit.set(true);
                    ConnectFacade.createServer(node, getServerPort(), handlerFactoryServer);

                } catch (Exception e) {
                    LOG.warn(node + " {}", e.getMessage());
                }
            }
        });

        // Connector service.
        if (svcConnector != null && !svcConnectorStarted) {
            svcConnector.reset();
            executor.execute(svcConnector);
            svcConnectorStarted = true;
        }

        executor.shutdown();
        nodeBuilder.setEnabled(true);
    }

    @Override
    public String toString() {
        return "["
                + (nodeBuilder.getName() != null && !nodeBuilder.getName().isEmpty() ? nodeBuilder.getName() + ":" : "")
                + NodeIdConv.toString(nodeId) + "]";
    }
}
