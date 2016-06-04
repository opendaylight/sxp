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
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.controller.util.database.SxpDatastoreImpl;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.Security;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SxpDatastoreNode extends org.opendaylight.sxp.core.SxpNode {

    public static InstanceIdentifier<SxpNodeIdentity> getIdentifier(final String nodeId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class, new NodeKey(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                Preconditions.checkNotNull(nodeId))))
                .augmentation(SxpNodeIdentity.class)
                .build();
    }

    private final DatastoreAccess datastoreAccess;
    private final String nodeId;

    /**
     * Default constructor that creates and start SxpDatastoreNode using provided values
     *
     * @param node Node setup data
     */
    public SxpDatastoreNode(NodeId nodeId, DatastoreAccess datastoreAccess, SxpNodeIdentity node) {
        super(Preconditions.checkNotNull(nodeId), Preconditions.checkNotNull(node),
                new MasterDatastoreImpl(Preconditions.checkNotNull(datastoreAccess), NodeIdConv.toString(nodeId)),
                new SxpDatastoreImpl(datastoreAccess, NodeIdConv.toString(nodeId)), new ThreadsWorker());
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        this.nodeId = NodeIdConv.toString(nodeId);
    }

    @Override protected void initConfiguration(SxpNodeIdentityBuilder identity) {
        // This is handled by DataStore Listeners
    }

    @Override protected SxpNodeIdentity getNodeIdentity() {
        SxpNodeIdentity
                identity =
                datastoreAccess == null ? null : datastoreAccess.readSynchronous(getIdentifier(nodeId),
                        LogicalDatastoreType.OPERATIONAL);
        return identity != null ? identity : super.getNodeIdentity();
    }

    @Override public void addConnection(Connection connection) {
        addConnection(new SxpDatastoreConnection(datastoreAccess, this, connection));
    }

    @Override protected Security setPassword(final Security security) {
        Security nodeSecurity = super.setPassword(security);
        if (datastoreAccess != null) {
            datastoreAccess.checkAndMerge(getIdentifier(nodeId).child(Security.class), nodeSecurity,
                    LogicalDatastoreType.OPERATIONAL, true);
        }
        return nodeSecurity;
    }
}
