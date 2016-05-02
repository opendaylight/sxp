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
import org.opendaylight.sxp.util.time.TimeConv;
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
        setCapabilities(Configuration.getCapabilities(connection.getVersion()));
    }

    private synchronized String getNodeId() {
        if (nodeId == null)
            nodeId = NodeIdConv.toString(getOwnerId());
        return nodeId;
    }

    @Override public synchronized Connection getConnection() {
        if (datastoreAccess != null)
            return datastoreAccess.readSynchronous(getIdentifierBuilder(getNodeId()).child(Connections.class)
                    .child(Connection.class, new ConnectionKey(address, port))
                    .build(), LogicalDatastoreType.OPERATIONAL);
        else
            return null;
    }

    @Override protected synchronized void setTimers(ConnectionTimers timers) {
        if (datastoreAccess != null && datastoreAccess.readSynchronous(
                getIdentifierBuilder(getNodeId()).child(Connections.class)
                        .child(Connection.class, new ConnectionKey(address, port))
                        .build(), LogicalDatastoreType.OPERATIONAL) != null)
            datastoreAccess.mergeSynchronous(getIdentifierBuilder(getNodeId()).child(Connections.class)
                    .child(Connection.class, new ConnectionKey(address, port))
                    .child(ConnectionTimers.class)
                    .build(), Preconditions.checkNotNull(timers), LogicalDatastoreType.OPERATIONAL);
    }

    @Override protected synchronized void setCapabilities(Capabilities capabilities) {
        if (datastoreAccess != null && datastoreAccess.readSynchronous(
                getIdentifierBuilder(getNodeId()).child(Connections.class)
                        .child(Connection.class, new ConnectionKey(address, port))
                        .build(), LogicalDatastoreType.OPERATIONAL) != null)
            datastoreAccess.mergeSynchronous(getIdentifierBuilder(getNodeId()).child(Connections.class)
                    .child(Connection.class, new ConnectionKey(address, port))
                    .child(Capabilities.class)
                    .build(), Preconditions.checkNotNull(capabilities), LogicalDatastoreType.OPERATIONAL);
    }

    @Override protected synchronized void setVersion(Version version) {
        if (datastoreAccess != null && datastoreAccess.readSynchronous(
                getIdentifierBuilder(getNodeId()).child(Connections.class)
                        .child(Connection.class, new ConnectionKey(address, port))
                        .build(), LogicalDatastoreType.OPERATIONAL) != null)
            datastoreAccess.mergeSynchronous(getIdentifierBuilder(getNodeId()).child(Connections.class)
                            .child(Connection.class, new ConnectionKey(address, port))
                            .build(),
                    new ConnectionBuilder(getConnection()).setVersion(Preconditions.checkNotNull(version)).build(),
                    LogicalDatastoreType.OPERATIONAL);
    }

    @Override protected synchronized void setState(ConnectionState state) {
        if (datastoreAccess != null && datastoreAccess.readSynchronous(
                getIdentifierBuilder(getNodeId()).child(Connections.class)
                        .child(Connection.class, new ConnectionKey(address, port))
                        .build(), LogicalDatastoreType.OPERATIONAL) != null)
            datastoreAccess.mergeSynchronous(getIdentifierBuilder(getNodeId()).child(Connections.class)
                            .child(Connection.class, new ConnectionKey(address, port))
                            .build(),
                    new ConnectionBuilder(getConnection()).setState(Preconditions.checkNotNull(state)).build(),
                    LogicalDatastoreType.OPERATIONAL);
    }

    @Override public synchronized void setNodeIdRemote(NodeId remoteNodeId) {
        if (datastoreAccess != null && datastoreAccess.readSynchronous(
                getIdentifierBuilder(getNodeId()).child(Connections.class)
                        .child(Connection.class, new ConnectionKey(address, port))
                        .build(), LogicalDatastoreType.OPERATIONAL) != null)
            datastoreAccess.mergeSynchronous(getIdentifierBuilder(getNodeId()).child(Connections.class)
                            .child(Connection.class, new ConnectionKey(address, port))
                            .build(),
                    new ConnectionBuilder(getConnection()).setNodeId(Preconditions.checkNotNull(remoteNodeId)).build(),
                    LogicalDatastoreType.OPERATIONAL);
    }

    @Override public synchronized void setUpdateOrKeepaliveMessageTimestamp() {
        ConnectionBuilder connectionBuilder = new ConnectionBuilder(getConnection());
        if (datastoreAccess != null && datastoreAccess.readSynchronous(
                getIdentifierBuilder(getNodeId()).child(Connections.class)
                        .child(Connection.class, new ConnectionKey(address, port))
                        .build(), LogicalDatastoreType.OPERATIONAL) != null)
            datastoreAccess.mergeSynchronous(getIdentifierBuilder(getNodeId()).child(Connections.class)
                            .child(Connection.class, new ConnectionKey(address, port))
                            .build(),
                    connectionBuilder.setTimestampUpdateOrKeepAliveMessage(TimeConv.toDt(System.currentTimeMillis()))
                            .build(), LogicalDatastoreType.OPERATIONAL);
    }
}
