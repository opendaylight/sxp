/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        InstanceIdentifier<Node>
                nodeIdentifier =
                InstanceIdentifier.builder(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                        .child(Node.class, new NodeKey(new NodeId(nodeName)))
                        .build();

        if (datastoreAccess.readSynchronous(nodeIdentifier, logicalDatastoreType) == null) {
            datastoreAccess.putSynchronous(nodeIdentifier,
                    new NodeBuilder().setKey(new NodeKey(new NodeId(nodeName))).build(), logicalDatastoreType);
        }
    }
}
