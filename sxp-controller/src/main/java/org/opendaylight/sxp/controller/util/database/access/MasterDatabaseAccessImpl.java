/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database.access;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseAccess;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
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

public final class MasterDatabaseAccessImpl implements MasterDatabaseAccess {

    protected static final Logger LOG = LoggerFactory.getLogger(MasterDatabaseAccessImpl.class.getName());

    private final String controllerName;

    private final InstanceIdentifier<MasterDatabase> databaseIdentifier;

    private final DatastoreAccess datastoreAccess;

    private final LogicalDatastoreType logicalDatastoreType;

    public MasterDatabaseAccessImpl(String controllerName, DatastoreAccess datastoreAccess,
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
                                        controllerName))).augmentation(SxpNodeIdentity.class)
                .child(MasterDatabase.class).build();

    }

    @Override
    public void delete(MasterDatabase database) throws DatabaseAccessException {
        for (Source source : database.getSource()) {
            InstanceIdentifierBuilder<Source> pathGroupIdentifier = InstanceIdentifier
                    .builder(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                    .child(Node.class,
                            new NodeKey(
                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                            controllerName))).augmentation(SxpNodeIdentity.class)
                    .child(MasterDatabase.class).child(Source.class, new SourceKey(source.getBindingSource()));

            try {
                datastoreAccess.delete(pathGroupIdentifier.build(), logicalDatastoreType).get();
            } catch (Exception e) {
                throw new DatabaseAccessException(controllerName, MasterDatabaseAccessImpl.class.getSimpleName()
                        + " deletation", e);
            }
        }
        LOG.trace("[{}] {} items deleted", controllerName, MasterDatabaseAccessImpl.class.getSimpleName());
    }

    @Override
    public void merge(MasterDatabase database) throws DatabaseAccessException {
        try {
            datastoreAccess.merge(databaseIdentifier, database, logicalDatastoreType).get();
        } catch (Exception e) {
            throw new DatabaseAccessException(controllerName, MasterDatabaseAccessImpl.class.getSimpleName()
                    + " merging", e);
        }
        LOG.trace("[{}] {} items merged", controllerName, MasterDatabaseAccessImpl.class.getSimpleName());
    }

    @Override
    public void put(MasterDatabase database) throws DatabaseAccessException {
        try {
            datastoreAccess.put(databaseIdentifier, database, logicalDatastoreType).get();
        } catch (Exception e) {
            throw new DatabaseAccessException(controllerName, MasterDatabaseAccessImpl.class.getSimpleName()
                    + " putting", e);
        }
        LOG.trace("[{}] {} items put", controllerName, MasterDatabaseAccessImpl.class.getSimpleName());
    }

    @Override
    public MasterDatabase read() throws DatabaseAccessException {
        Optional<MasterDatabase> database;
        try {
            database = datastoreAccess.read(databaseIdentifier, logicalDatastoreType).get();
        } catch (Exception e) {
            throw new DatabaseAccessException(controllerName, MasterDatabaseAccessImpl.class.getSimpleName()
                    + " reading", e);
        }
        if (!database.isPresent()) {
            throw new DatabaseAccessException(controllerName, MasterDatabaseAccessImpl.class.getSimpleName()
                    + " not exists in operational datastore");
        }
        LOG.trace("[{}] {} items read", controllerName, MasterDatabaseAccessImpl.class.getSimpleName());

        return database.get();
    }
}
