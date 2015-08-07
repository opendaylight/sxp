/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import java.util.List;

import org.opendaylight.sxp.util.database.SxpBindingIdentity;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseAccess;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.sxp.util.exception.node.NodeIdNotDefinedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

public final class SxpDatastoreImpl extends SxpDatabaseImpl {

    private String controllerName;

    public SxpDatastoreImpl(String controllerName, SxpDatabaseAccess databaseAccess) {
        super(databaseAccess);
        this.controllerName = controllerName;
    }

    @Override
    public boolean addBindings(SxpDatabase database) throws DatabaseAccessException {
        synchronized (databaseAccess) {
            this.database = databaseAccess.read();
            synchronized (this.database) {
                boolean result = super.addBindings(database);
                databaseAccess.put(this.database);
                return result;
            }
        }
    }

    @Override
    public List<SxpBindingIdentity> deleteBindings(SxpDatabase database) throws DatabaseAccessException {
        synchronized (databaseAccess) {
            this.database = databaseAccess.read();
            synchronized (this.database) {
                List<SxpBindingIdentity> result = super.deleteBindings(database);
                databaseAccess.put(this.database);
                return result;
            }
        }
    }

    @Override
    public SxpDatabase get() throws DatabaseAccessException {
        synchronized (databaseAccess) {
            return databaseAccess.read();
        }
    }

    @Override
    public void purgeBindings(NodeId nodeId) throws DatabaseAccessException, NodeIdNotDefinedException {
        synchronized (databaseAccess) {
            this.database = databaseAccess.read();
            synchronized (this.database) {
                super.purgeBindings(nodeId);
                databaseAccess.put(this.database);
            }
        }
    }

    @Override
    public List<SxpBindingIdentity> readBindings() throws DatabaseAccessException {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            synchronized (this.database) {
                return super.readBindings();
            }
        }
    }

    @Override
    public String toString() {
        synchronized (databaseAccess) {
            try {
                database = databaseAccess.read();
            } catch (Exception e) {
                LOG.warn(controllerName + " {} | {}", e.getClass().getSimpleName(), e.getMessage());
                return "[error]";
            }
            synchronized (this.database) {
                return super.toString();
            }
        }
    }
}
