/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionPeerFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.net.InetSocketAddress;

public class ConnectionsListener extends ContainerListener<Connections, Connection> {

    public ConnectionsListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess);
    }

    @Override protected void handleOperational(DataObjectModification<Connection> c,
            InstanceIdentifier<SxpNodeIdentity> identifier) {
        final String nodeId = identifier.firstKeyOf(Node.class).getNodeId().getValue();
        SxpNode sxpNode = Configuration.getRegisteredNode(nodeId);
        if (sxpNode == null) {
            LOG.error("Operational Modification {} {} could not get SXPNode {}", getClass(), c.getModificationType(),
                    nodeId);
            return;
        }
        LOG.trace("Operational Modification {} {}", getClass(), c.getModificationType());
        switch (c.getModificationType()) {
            case WRITE:
                if (c.getDataBefore() == null) {
                    sxpNode.addConnection(c.getDataAfter());
                    break;
                } else if (c.getDataAfter() == null) {
                    sxpNode.removeConnection(getConnection(c.getDataBefore()));
                    break;
                }
            case SUBTREE_MODIFIED:
                if (checkChange(c, SxpConnectionFields::getTcpPort) || (
                        (checkChange(c, SxpConnectionFields::getVersion) || checkChange(c,
                                SxpConnectionFields::getConnectionTimers)) && !checkChange(c,
                                SxpConnectionPeerFields::getState) && ConnectionState.On.equals(
                                c.getDataAfter().getState())) || checkChange(c, SxpConnectionFields::getPassword)
                        || checkChange(c, SxpConnectionFields::getPeerAddress)) {
                    sxpNode.getConnection(getConnection(c.getDataBefore())).shutdown();
                }
                break;
            case DELETE:
                sxpNode.removeConnection(getConnection(c.getDataBefore()));
                break;
        }
    }

    @Override protected InstanceIdentifier<Connection> getIdentifier(Connection d,
            InstanceIdentifier<SxpNodeIdentity> parentIdentifier) {
        Preconditions.checkNotNull(d);
        Preconditions.checkNotNull(parentIdentifier);
        return parentIdentifier.child(Connections.class)
                .child(Connection.class, new ConnectionKey(d.getPeerAddress(), d.getTcpPort()));
    }

    @Override public DataObjectModification<Connections> getModifications(
            DataTreeModification<SxpNodeIdentity> treeModification) {
        return treeModification.getRootNode().getModifiedChildContainer(Connections.class);
    }

    private InetSocketAddress getConnection(Connection connection) {
        return new InetSocketAddress(Search.getAddress(Preconditions.checkNotNull(connection).getPeerAddress()),
                Preconditions.checkNotNull(connection.getTcpPort()).getValue());
    }

}
