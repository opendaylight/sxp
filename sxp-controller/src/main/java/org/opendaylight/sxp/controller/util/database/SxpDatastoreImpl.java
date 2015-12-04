/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import java.util.List;

import com.google.common.base.Preconditions;
import org.opendaylight.sxp.util.database.SxpBindingIdentity;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseAccess;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.sxp.util.exception.node.NodeIdNotDefinedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SxpDatastoreImpl extends SxpDatabaseImpl {

    private final SxpDatabaseAccess databaseAccess;

    public SxpDatastoreImpl(SxpDatabaseAccess databaseAccess) {
        this.databaseAccess = Preconditions.checkNotNull(databaseAccess);
    }

    @Override
    public synchronized boolean addBindings(SxpDatabase database) throws DatabaseAccessException {
        this.database = databaseAccess.read();
        boolean result = super.addBindings(database);
        databaseAccess.put(this.database);
        return result;
    }

    @Override
    public synchronized void cleanUpBindings(NodeId nodeId) throws NodeIdNotDefinedException, DatabaseAccessException {
        this.database = databaseAccess.read();
        super.cleanUpBindings(nodeId);
        databaseAccess.put(this.database);
    }

    @Override
    public synchronized List<SxpBindingIdentity> deleteBindings(SxpDatabase database) throws DatabaseAccessException {
        this.database = databaseAccess.read();
        List<SxpBindingIdentity> result = super.deleteBindings(database);
        databaseAccess.put(this.database);
        return result;
    }

    @Override
    public synchronized SxpDatabase get() throws DatabaseAccessException {
        return databaseAccess.read();
    }

    private static final Logger LOG = LoggerFactory.getLogger(SxpDatastoreImpl.class.getName());


    @Override
    public synchronized void purgeBindings(NodeId nodeId) throws DatabaseAccessException, NodeIdNotDefinedException {
        this.database = databaseAccess.read();
        super.purgeBindings(nodeId);
        databaseAccess.put(this.database);
    }

    @Override
    public synchronized List<SxpBindingIdentity> readBindings() throws DatabaseAccessException {
        database = databaseAccess.read();
        return super.readBindings();
    }

    @Override
    public synchronized void setAsCleanUp(NodeId nodeId) throws NodeIdNotDefinedException, DatabaseAccessException {
        database = databaseAccess.read();
        super.setAsCleanUp(nodeId);
        databaseAccess.put(this.database);
    }

    @Override
    public synchronized String toString() {
        try {
            database = databaseAccess.read();
            return super.toString();
        } catch (DatabaseAccessException e) {
            return "[error]";
        }
    }
}
