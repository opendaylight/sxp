/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.util.exception.connection.SocketAddressNotRecognizedException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpConnectionException;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import java.net.InetSocketAddress;

public class ConnectionsListener extends SxpDataChangeListener<Connection> {

    public ConnectionsListener(DatastoreAccess datastoreAccess, LogicalDatastoreType datastoreType) {
        super(datastoreAccess, datastoreType);
    }

    @Override protected DataTreeIdentifier<Connection> getDataTreeIdentifier() {
        return new DataTreeIdentifier<>(datastoreType, SUBSCRIBED_PATH.child(Node.class)
                .augmentation(SxpNodeIdentity.class)
                .child(Connections.class)
                .child(Connection.class));
    }

    private InetSocketAddress getConnection(DataTreeModification<Connection> c) {
        return new InetSocketAddress(
                Search.getAddress(Preconditions.checkNotNull(c.getRootNode().getDataBefore()).getPeerAddress()),
                Preconditions.checkNotNull(c.getRootNode().getDataBefore().getTcpPort()).getValue());
    }

    @Override protected void handleNonConfig(DataTreeModification<Connection> c, final String nodeId) {
        switch (c.getRootNode().getModificationType()) {
            case SUBTREE_MODIFIED:
                if (checkChange(c, SxpConnectionFields::getTcpPort) != null
                        || checkChange(c, SxpConnectionFields::getVersion) != null
                        || checkChange(c, SxpConnectionFields::getPassword) != null
                        || checkChange(c, SxpConnectionFields::getPeerAddress) != null
                        || checkChange(c, SxpConnectionFields::getConnectionTimers) != null) {
                    Configuration.getRegisteredNode(nodeId).getConnection(getConnection(c)).shutdown();
                }
                break;
            case WRITE:
                if (c.getRootNode().getDataBefore() == null) {
                    Configuration.getRegisteredNode(nodeId).addConnection(c.getRootNode().getDataAfter());
                }
                break;
            case DELETE:
                Configuration.getRegisteredNode(nodeId).removeConnection(getConnection(c));
                break;
        }
    }
}
