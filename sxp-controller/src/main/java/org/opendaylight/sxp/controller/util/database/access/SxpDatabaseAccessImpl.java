/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database.access;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseAccess;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public final class SxpDatabaseAccessImpl implements SxpDatabaseAccess {

    protected static final Logger LOG = LoggerFactory.getLogger(SxpDatabaseAccessImpl.class.getName());

    private final String controllerName;

    private final InstanceIdentifier<SxpDatabase> databaseIdentifier;

    private final DatastoreAccess datastoreAccess;

    private final LogicalDatastoreType logicalDatastoreType;

    public SxpDatabaseAccessImpl(String controllerName, DatastoreAccess datastoreAccess,
            LogicalDatastoreType logicalDatastoreType) {
        this.controllerName = controllerName;
        this.datastoreAccess = datastoreAccess;
        this.logicalDatastoreType = logicalDatastoreType;
        this.databaseIdentifier = InstanceIdentifier
                .builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class,
                        new NodeKey(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                        controllerName))).augmentation(SxpNodeIdentity.class).child(SxpDatabase.class)
                .build();
    }

    @Override
    public void delete(SxpDatabase database) throws DatabaseAccessException {
        for (PathGroup pathGroup : database.getPathGroup()) {
            InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup> pathGroupIdentifier = InstanceIdentifier
                    .builder(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                    .child(Node.class,
                            new NodeKey(
                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                            controllerName)))
                    .augmentation(SxpNodeIdentity.class)
                    .child(SxpDatabase.class)
                    .child(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup.class,
                            new PathGroupKey(pathGroup.getPathHash()));

            try {
                datastoreAccess.delete(pathGroupIdentifier.build(), logicalDatastoreType).get();
            } catch (Exception e) {
                throw new DatabaseAccessException(controllerName, SxpDatabaseAccessImpl.class.getSimpleName()
                        + " deletation", e);
            }
        }
        LOG.trace("[{}] {} items deleted", controllerName, SxpDatabaseAccessImpl.class.getSimpleName());
    }

    @Override
    public void merge(SxpDatabase database) throws DatabaseAccessException {
        try {
            datastoreAccess.merge(databaseIdentifier, database, logicalDatastoreType).get();
        } catch (Exception e) {
            throw new DatabaseAccessException(controllerName, SxpDatabaseAccessImpl.class.getSimpleName() + " merging",
                    e);
        }
        LOG.trace("[{}] {} items merged", controllerName, SxpDatabaseAccessImpl.class.getSimpleName());
    }

    @Override
    public void put(SxpDatabase database) throws DatabaseAccessException {
        try {
            datastoreAccess.put(databaseIdentifier, database, logicalDatastoreType).get();
        } catch (Exception e) {
            throw new DatabaseAccessException(controllerName, SxpDatabaseAccessImpl.class.getSimpleName() + " putting",
                    e);
        }
        LOG.trace("[{}] {} items put", controllerName, SxpDatabaseAccessImpl.class.getSimpleName());
    }

    @Override
    public SxpDatabase read() throws DatabaseAccessException {
        Optional<SxpDatabase> database;
        try {
            database = datastoreAccess.read(databaseIdentifier, logicalDatastoreType).get();
        } catch (Exception e) {
            throw new DatabaseAccessException(controllerName, SxpDatabaseAccessImpl.class.getSimpleName() + " reading",
                    e);
        }
        if (!database.isPresent()) {
            throw new DatabaseAccessException(controllerName, SxpDatabaseAccessImpl.class.getSimpleName()
                    + " not exists in operational datastore");
        }
        LOG.trace("[{}] {} items read", controllerName, SxpDatabaseAccessImpl.class.getSimpleName());

        return database.get();
    }
}
