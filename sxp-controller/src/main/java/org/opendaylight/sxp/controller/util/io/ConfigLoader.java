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
import java.util.List;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabaseBuilder;
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
        return datastoreAccess.checkAndPut(nodeIdentifier, new NodeBuilder().setKey(new NodeKey(
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                        nodeName))).build(), logicalDatastoreType, false);
    }

    /**
     * @param database MasterDatabase containing bindings
     * @return MasterDatabase data wrapper
     */
    public static MasterDatabase parseMasterDatabase(
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.MasterDatabase database) {
        List<MasterDatabaseBinding> bindings = new ArrayList<>();
        MasterDatabaseBuilder databaseBuilder = new MasterDatabaseBuilder();
        databaseBuilder.setMasterDatabaseBinding(bindings);
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        bindingBuilder.setTimestamp(TimeConv.toDt(System.currentTimeMillis()));
        bindingBuilder.setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());
        if (database != null && database.getBinding() != null) {
            database.getBinding().forEach(b -> {
                bindingBuilder.setSecurityGroupTag(b.getSgt());
                b.getIpPrefix().forEach(p -> bindings.add(bindingBuilder.setIpPrefix(p).build()));
            });
        }
        return databaseBuilder.build();
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
