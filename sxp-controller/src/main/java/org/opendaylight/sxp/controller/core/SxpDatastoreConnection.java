/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.capabilities.fields.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.opendaylight.sxp.controller.core.SxpDatastoreNode.getIdentifierBuilder;

public class SxpDatastoreConnection extends org.opendaylight.sxp.core.SxpConnection {

    private final PortNumber port;
    private final IpAddress address;
    private DatastoreAccess datastoreAccess;
    private String nodeId = null;

    public SxpDatastoreConnection(DatastoreAccess datastoreAccess, SxpNode owner, Connection connection)
            throws UnknownVersionException {
        super(Preconditions.checkNotNull(owner), Preconditions.checkNotNull(connection));
        this.address = new IpAddress(Preconditions.checkNotNull(connection.getPeerAddress()));
        this.port = new PortNumber(Preconditions.checkNotNull(connection.getTcpPort()));
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        setCapabilities(Configuration.getCapabilities(
                connection.getVersion() != null ? connection.getVersion() : owner.getVersion()));
    }

    private synchronized String getNodeId() {
        if (nodeId == null)
            nodeId = NodeIdConv.toString(getOwnerId());
        return nodeId;
    }

    private InstanceIdentifier<Connection> getIdentifier() {
        return getIdentifierBuilder(getNodeId()).child(Connections.class)
                .child(Connection.class, new ConnectionKey(address, port))
                .build();
    }

    private Connection getDatastoreConnection() {
        return datastoreAccess != null ? datastoreAccess.readSynchronous(getIdentifier(),
                LogicalDatastoreType.OPERATIONAL) : null;
    }

    @Override protected synchronized void setTimers(ConnectionTimers timers) {
        super.setTimers(timers);
        if (getDatastoreConnection() != null) {
            datastoreAccess.put(getIdentifier().child(ConnectionTimers.class), Preconditions.checkNotNull(timers),
                    LogicalDatastoreType.OPERATIONAL);
        }
    }

    @Override protected synchronized void setCapabilities(Capabilities capabilities) {
        super.setCapabilities(capabilities);
        if (getDatastoreConnection() != null) {
            datastoreAccess.put(getIdentifier().child(Capabilities.class), Preconditions.checkNotNull(capabilities),
                    LogicalDatastoreType.OPERATIONAL);
        }
    }

    @Override protected synchronized void setVersion(Version version) {
        super.setVersion(version);
        if (getDatastoreConnection() != null) {
            datastoreAccess.merge(getIdentifier(),
                    new ConnectionBuilder().setPeerAddress(address).setTcpPort(port).setVersion(version).build(),
                    LogicalDatastoreType.OPERATIONAL);
        }
    }

    @Override protected synchronized void setState(ConnectionState state) {
        super.setState(state);
        if (getDatastoreConnection() != null) {
            datastoreAccess.merge(getIdentifier(),
                    new ConnectionBuilder().setPeerAddress(address).setTcpPort(port).setState(state).build(),
                    LogicalDatastoreType.OPERATIONAL);
        }
    }

    @Override public synchronized void setNodeIdRemote(NodeId remoteNodeId) {
        super.setNodeIdRemote(remoteNodeId);
        if (getDatastoreConnection() != null) {
            datastoreAccess.merge(getIdentifier(),
                    new ConnectionBuilder().setPeerAddress(address).setTcpPort(port).setNodeId(remoteNodeId).build(),
                    LogicalDatastoreType.OPERATIONAL);
        }
    }

    @Override public synchronized void setUpdateOrKeepaliveMessageTimestamp() {
        super.setUpdateOrKeepaliveMessageTimestamp();
        if (getDatastoreConnection() != null) {
            datastoreAccess.merge(getIdentifier(), new ConnectionBuilder().setPeerAddress(address)
                    .setTcpPort(port)
                    .setTimestampUpdateOrKeepAliveMessage(getConnection().getTimestampUpdateOrKeepAliveMessage())
                    .build(), LogicalDatastoreType.OPERATIONAL);
        }
    }

    @Override public void shutdown() {
        super.shutdown();
        Connection connection = getDatastoreConnection();
        if (connection != null)
            setConnection(getDatastoreConnection());
    }
}
