/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.io;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * <pre>
 *  ODL-Karaf @see http://localhost:8181/restconf/config/network-topology:network-topology/topology/sxp
 * </pre>
 */
public final class ConfigLoader implements AutoCloseable {

    private final DatastoreAccess datastoreAccess;

    public ConfigLoader(DatastoreAccess datastoreAccess) {
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
    }

    /**
     * @param nodeName             NodeId used for initialization
     * @param logicalDatastoreType Logical datastore type where topology will be initialized
     * @param datastoreAccess      Datastore access used for initialization
     * @return If Topology was successfully initialized
     */
    public static boolean initTopologyNode(final String nodeName, final LogicalDatastoreType logicalDatastoreType,
            final DatastoreAccess datastoreAccess) {
        InstanceIdentifier<Node>
                nodeIdentifier =
                NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                nodeName)));
        return datastoreAccess.checkAndPut(nodeIdentifier, new NodeBuilder().withKey(new NodeKey(
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                        nodeName))).build(), logicalDatastoreType, false);
    }

    /**
     * @param connections Connection containing data about peer
     * @return Connection data wrapper
     */
    public static org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections parseConnections(
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections connections) {
        ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder().setConnection(new ArrayList<>());
        if (connections != null && connections.getConnection() != null) {
            connectionsBuilder.setConnection(connections.getConnection());
        }
        return connectionsBuilder.build();
    }

    @Override
    public void close() {
        datastoreAccess.close();
    }
}
