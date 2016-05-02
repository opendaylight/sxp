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
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionPeerFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import java.net.InetSocketAddress;

public class ConnectionsListener extends SxpDataChangeListener<Connection> {

    public ConnectionsListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess);
    }

    @Override protected DataTreeIdentifier<Connection> getDataTreeIdentifier(LogicalDatastoreType datastoreType) {
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
        SxpNode sxpNode = Configuration.getRegisteredNode(nodeId);
        if (sxpNode == null)
            return;
        switch (c.getRootNode().getModificationType()) {
            case WRITE:
                if (c.getRootNode().getDataBefore() == null) {
                    sxpNode.addConnection(c.getRootNode().getDataAfter());
                    break;
                } else if (c.getRootNode().getDataAfter() == null) {
                    sxpNode.removeConnection(getConnection(c));
                    break;
                }
            case SUBTREE_MODIFIED:
                if (checkChange(c, SxpConnectionFields::getTcpPort) || (
                        (checkChange(c, SxpConnectionFields::getVersion) || checkChange(c,
                                SxpConnectionFields::getConnectionTimers)) && !checkChange(c,
                                SxpConnectionPeerFields::getState) && ConnectionState.On.equals(
                                c.getRootNode().getDataAfter().getState())) || checkChange(c,
                        SxpConnectionFields::getPassword) || checkChange(c, SxpConnectionFields::getPeerAddress)) {
                    sxpNode.getConnection(getConnection(c)).shutdown();
                }
                break;
            case DELETE:
                sxpNode.removeConnection(getConnection(c));
                break;
        }
    }
}
