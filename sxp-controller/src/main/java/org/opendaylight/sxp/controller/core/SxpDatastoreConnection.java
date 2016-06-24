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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SxpDatastoreConnection extends org.opendaylight.sxp.core.SxpConnection {

    private final PortNumber port;
    private final IpAddress address;
    private final DatastoreAccess datastoreAccess;
    private final String nodeId;

    /**
     * Creates SxpDatastoreConnection using provided values
     *
     * @param datastoreAccess Handle used to read and write from Datastore
     * @param owner           SxpNode to be set as owner
     * @param connection      Connection that contains settings
     * @param domain          Sxp Domain where Connections contains
     * @return SxpConnection created by specified values
     * @throws UnknownVersionException If version in provided values isn't supported
     */
    public static SxpDatastoreConnection create(DatastoreAccess datastoreAccess, SxpNode owner, Connection connection,
            String domain) throws UnknownVersionException {
        SxpDatastoreConnection
                datastoreConnection =
                new SxpDatastoreConnection(datastoreAccess, owner, connection, domain);
        datastoreConnection.setCapabilities(Configuration.getCapabilities(datastoreConnection.getVersion()));
        return datastoreConnection;
    }

    private SxpDatastoreConnection(DatastoreAccess datastoreAccess, SxpNode owner, Connection connection, String domain)
            throws UnknownVersionException {
        super(Preconditions.checkNotNull(owner), Preconditions.checkNotNull(connection),
                Preconditions.checkNotNull(domain));
        this.address = new IpAddress(Preconditions.checkNotNull(connection.getPeerAddress()));
        this.port = new PortNumber(Preconditions.checkNotNull(connection.getTcpPort()));
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        this.nodeId = NodeIdConv.toString(getOwnerId());
    }

    /**
     * @return InstanceIdentifier pointing to specific Connection
     */
    private InstanceIdentifier<Connection> getIdentifier() {
        return SxpDatastoreNode.getIdentifier(nodeId)
                .child(SxpDomains.class)
                .child(SxpDomain.class, new SxpDomainKey(domain))
                .child(Connections.class)
                .child(Connection.class, new ConnectionKey(address, port));
    }

    @Override protected void setTimers(ConnectionTimers timers) {
        super.setTimers(timers);
        if (datastoreAccess != null) {
            datastoreAccess.checkAndPut(getIdentifier().child(ConnectionTimers.class),
                    Preconditions.checkNotNull(timers), LogicalDatastoreType.OPERATIONAL, true);
        }
    }

    @Override protected void setCapabilities(Capabilities capabilities) {
        super.setCapabilities(capabilities);
        if (datastoreAccess != null) {
            datastoreAccess.checkAndPut(getIdentifier().child(Capabilities.class),
                    Preconditions.checkNotNull(capabilities), LogicalDatastoreType.OPERATIONAL, true);
        }
    }

    @Override protected void setVersion(Version version) {
        super.setVersion(version);
        if (datastoreAccess != null) {
            datastoreAccess.checkAndMerge(getIdentifier(),
                    new ConnectionBuilder().setPeerAddress(address).setTcpPort(port).setVersion(version).build(),
                    LogicalDatastoreType.OPERATIONAL, true);
        }
    }

    @Override protected void setState(ConnectionState state) {
        super.setState(state);
        if (datastoreAccess != null) {
            datastoreAccess.checkAndMerge(getIdentifier(),
                    new ConnectionBuilder().setPeerAddress(address).setTcpPort(port).setState(state).build(),
                    LogicalDatastoreType.OPERATIONAL, true);
        }
    }

    @Override public void setNodeIdRemote(NodeId remoteNodeId) {
        super.setNodeIdRemote(remoteNodeId);
        if (datastoreAccess != null) {
            datastoreAccess.checkAndMerge(getIdentifier(),
                    new ConnectionBuilder().setPeerAddress(address).setTcpPort(port).setNodeId(remoteNodeId).build(),
                    LogicalDatastoreType.OPERATIONAL, true);
        }
    }

    @Override public void setUpdateOrKeepaliveMessageTimestamp() {
        super.setUpdateOrKeepaliveMessageTimestamp();
        if (datastoreAccess != null) {
            datastoreAccess.checkAndMerge(getIdentifier(), new ConnectionBuilder().setPeerAddress(address)
                    .setTcpPort(port)
                    .setTimestampUpdateOrKeepAliveMessage(getConnection().getTimestampUpdateOrKeepAliveMessage())
                    .build(), LogicalDatastoreType.OPERATIONAL, true);
        }
    }

    @Override public void shutdown() {
        super.shutdown();
        Connection
                connection =
                datastoreAccess != null ? datastoreAccess.readSynchronous(getIdentifier(),
                        LogicalDatastoreType.OPERATIONAL) : null;
        if (connection != null)
            setConnection(connection);
    }
}
