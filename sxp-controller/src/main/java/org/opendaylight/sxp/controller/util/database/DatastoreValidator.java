/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.sxp.controller.util.database.access.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentityBuilder;
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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public final class DatastoreValidator {

    private static DatastoreValidator instance = null;

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
            }
        }
    }
}
