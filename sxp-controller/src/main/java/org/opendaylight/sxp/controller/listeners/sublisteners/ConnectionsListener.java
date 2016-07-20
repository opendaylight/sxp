/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.spi.ListListener;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionPeerFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.opendaylight.sxp.controller.listeners.spi.Listener.Differences.checkDifference;

public class ConnectionsListener extends ListListener<SxpDomain, Connections, Connection> {

    public ConnectionsListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess, Connections.class);
    }

    @Override
    protected void handleOperational(DataObjectModification<Connection> c, InstanceIdentifier<SxpDomain> identifier,
            SxpNode sxpNode) {
        final String domainName = identifier.firstKeyOf(SxpDomain.class).getDomainName();
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
                if (checkDifference(c, con -> con.getTcpPort().getValue()) || (
                        checkDifference(c, SxpConnectionFields::getVersion) && !checkDifference(c,
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

    /**
     * @param connection Connection that will be parsed
     * @return InetSocket address of provided connection
     */
    private InetSocketAddress getConnection(Connection connection) {
        return new InetSocketAddress(Search.getAddress(Preconditions.checkNotNull(connection).getPeerAddress()),
                Preconditions.checkNotNull(connection.getTcpPort()).getValue());
    }

}
