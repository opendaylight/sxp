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
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.spi.ListListener;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionPeerFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.net.InetSocketAddress;

import static org.opendaylight.sxp.controller.listeners.spi.Listener.Differences.checkDifference;

public class ConnectionsListener extends ListListener<SxpDomain, Connections, Connection> {

    public ConnectionsListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess, Connections.class);
    }

    @Override
    protected void handleOperational(DataObjectModification<Connection> c, InstanceIdentifier<SxpDomain> identifier) {
        final String nodeId = identifier.firstKeyOf(Node.class).getNodeId().getValue(),
                domainName = identifier.firstKeyOf(SxpDomain.class).getDomainName();
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
                    sxpNode.addConnection(c.getDataAfter(), domainName);
                    break;
                } else if (c.getDataAfter() == null) {
                    sxpNode.removeConnection(getConnection(c.getDataBefore()));
                    break;
                }
            case SUBTREE_MODIFIED:
                if (checkDifference(c, SxpConnectionFields::getTcpPort) || (
                        (checkDifference(c, SxpConnectionFields::getVersion) || checkDifference(c,
                                SxpConnectionFields::getConnectionTimers)) && !checkDifference(c,
                                SxpConnectionPeerFields::getState) && ConnectionState.On.equals(
                                c.getDataAfter().getState())) || checkDifference(c, SxpConnectionFields::getPassword)
                        || checkDifference(c, SxpConnectionFields::getPeerAddress)) {
                    sxpNode.getConnection(getConnection(c.getDataBefore())).shutdown();
                }
                break;
            case DELETE:
                sxpNode.removeConnection(getConnection(c.getDataBefore()));
                break;
        }
    }

    @Override protected InstanceIdentifier<Connection> getIdentifier(Connection d,
            InstanceIdentifier<SxpDomain> parentIdentifier) {
        Preconditions.checkNotNull(d);
        Preconditions.checkNotNull(parentIdentifier);
        return parentIdentifier.child(Connections.class)
                .child(Connection.class, new ConnectionKey(d.getPeerAddress(), d.getTcpPort()));
    }

    private InetSocketAddress getConnection(Connection connection) {
        return new InetSocketAddress(Search.getAddress(Preconditions.checkNotNull(connection).getPeerAddress()),
                Preconditions.checkNotNull(connection.getTcpPort()).getValue());
    }

}
