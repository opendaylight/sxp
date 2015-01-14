/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.sxp.controller.util.database.access.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.sxp.database.Vpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public final class DatastoreValidator {

    protected static DatastoreValidator instance = null;

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreValidator.class.getName());

    public static synchronized DatastoreValidator getInstance(DatastoreAccess datastoreAccess) {
        if (instance == null) {
            instance = new DatastoreValidator(datastoreAccess);
        }
        return instance;
    }

    private DatastoreAccess datastoreAccess;

    private DatastoreValidator(DatastoreAccess datastoreAccess) {
        this.datastoreAccess = datastoreAccess;
    }

    public DatastoreAccess getDatastoreAccess() {
        return datastoreAccess;
    }

    public void validateSxpNodeDatabases(String nodeName, LogicalDatastoreType logicalDatastoreType)
            throws InterruptedException, ExecutionException {
        // SXP database.
        InstanceIdentifier<SxpDatabase> sxpDatabaseIdentifier = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class, new NodeKey(new NodeId(nodeName))).augmentation(SxpNodeIdentity.class)
                .child(SxpDatabase.class).build();

        CheckedFuture<Optional<SxpDatabase>, ReadFailedException> sxpDatabase = datastoreAccess.read(
                sxpDatabaseIdentifier, LogicalDatastoreType.OPERATIONAL);

        if (!sxpDatabase.get().isPresent()) {
            try {
                SxpDatabaseBuilder databaseBuilder = new SxpDatabaseBuilder();
                databaseBuilder.setAttribute(new ArrayList<Attribute>());
                databaseBuilder.setPathGroup(new ArrayList<PathGroup>());
                databaseBuilder.setVpn(new ArrayList<Vpn>());
                datastoreAccess.put(sxpDatabaseIdentifier, databaseBuilder.build(), logicalDatastoreType).get();
            } catch (CancellationException | ExecutionException | InterruptedException e) {
                LOG.error("SXP node '{}' sxp-database creation failed: '{}'", nodeName, e.getMessage());
                return;
            }
        }

        // Master database.
        InstanceIdentifier<MasterDatabase> masterDatabaseIdentifier = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class, new NodeKey(new NodeId(nodeName))).augmentation(SxpNodeIdentity.class)
                .child(MasterDatabase.class).build();

        CheckedFuture<Optional<MasterDatabase>, ReadFailedException> masterDatabase = datastoreAccess.read(
                masterDatabaseIdentifier, LogicalDatastoreType.OPERATIONAL);

        if (!masterDatabase.get().isPresent()) {
            try {
                MasterDatabaseBuilder databaseBuilder = new MasterDatabaseBuilder();
                databaseBuilder.setAttribute(new ArrayList<Attribute>());
                databaseBuilder.setSource(new ArrayList<Source>());
                databaseBuilder
                        .setVpn(new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.master.database.Vpn>());
                datastoreAccess.put(masterDatabaseIdentifier, databaseBuilder.build(), logicalDatastoreType).get();
            } catch (CancellationException | ExecutionException | InterruptedException e) {
                LOG.error("SXP node '{}' master-database creation failed: '{}'", nodeName, e.getMessage());
                return;
            }
        }
    }

    public void validateSxpNodePath(String nodeName, LogicalDatastoreType logicalDatastoreType)
            throws InterruptedException, ExecutionException {

        InstanceIdentifier<NetworkTopology> networkTopologyIndentifier = InstanceIdentifier.builder(
                NetworkTopology.class).build();
        CheckedFuture<Optional<NetworkTopology>, ReadFailedException> networkTopology = datastoreAccess.read(
                networkTopologyIndentifier, logicalDatastoreType);
        if (!networkTopology.get().isPresent()) {
            try {
                datastoreAccess.put(networkTopologyIndentifier, new NetworkTopologyBuilder().build(),
                        logicalDatastoreType).get();
            } catch (CancellationException | ExecutionException | InterruptedException e) {
                LOG.error("NetworkTopology parent creation failed: '{}'", e.getMessage());
                return;
            }
        }

        InstanceIdentifier<Topology> nodesIndentifier = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME))).build();
        CheckedFuture<Optional<Topology>, ReadFailedException> nodes = datastoreAccess.read(nodesIndentifier,
                logicalDatastoreType);
        if (!nodes.get().isPresent()) {
            try {
                TopologyBuilder topologyBuilder = new TopologyBuilder();
                topologyBuilder.setKey(new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)));
                datastoreAccess.put(nodesIndentifier, topologyBuilder.build(), logicalDatastoreType).get();
            } catch (CancellationException | ExecutionException | InterruptedException e) {
                LOG.error("NetworkTopology/Topology parent creation failed: '{}'", e.getMessage());
                return;
            }
        }

        InstanceIdentifier<Node> nodeIdentifier = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class, new NodeKey(new NodeId(nodeName))).build();
        CheckedFuture<Optional<Node>, ReadFailedException> node = datastoreAccess.read(nodeIdentifier,
                logicalDatastoreType);
        if (!node.get().isPresent()) {
            try {
                NodeBuilder nodeBuilder = new NodeBuilder();
                nodeBuilder.setKey(new NodeKey(new NodeId(nodeName)));
                datastoreAccess.put(nodeIdentifier, nodeBuilder.build(), logicalDatastoreType).get();
            } catch (CancellationException | ExecutionException | InterruptedException e) {
                LOG.error("NetworkTopology/Topology/Node '{}' parent creation failed: '{}'", nodeName, e.getMessage());
                return;
            }
        }

        InstanceIdentifier<SxpNodeIdentity> sxpNodeIdentityIdentifier = InstanceIdentifier
                .builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class, new NodeKey(new NodeId(nodeName))).augmentation(SxpNodeIdentity.class).build();
        CheckedFuture<Optional<SxpNodeIdentity>, ReadFailedException> sxpNodeIdentity = datastoreAccess.read(
                sxpNodeIdentityIdentifier, logicalDatastoreType);
        if (!sxpNodeIdentity.get().isPresent()) {
            try {
                datastoreAccess.put(sxpNodeIdentityIdentifier, new SxpNodeIdentityBuilder().build(),
                        logicalDatastoreType).get();
            } catch (CancellationException | ExecutionException | InterruptedException e) {
                LOG.error("SXP node identity'{}' parent creation failed: '{}'", nodeName, e.getMessage());
                return;
            }
        }
    }
}
