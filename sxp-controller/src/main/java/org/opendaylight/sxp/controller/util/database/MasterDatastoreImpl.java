/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import java.net.UnknownHostException;
import java.util.List;

import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.MasterBindingIdentity;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseAccess;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.sxp.util.exception.node.NodeIdNotDefinedException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

public final class MasterDatastoreImpl extends MasterDatabaseImpl {

    private String controllerName;

    public MasterDatastoreImpl(String controllerName, MasterDatabaseAccess databaseAccess) {
        super(databaseAccess);
        this.controllerName = controllerName;
    }

    @Override
    public void addBindings(NodeId owner, List<MasterBindingIdentity> contributedBindingIdentities)
            throws DatabaseAccessException, NodeIdNotDefinedException {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            super.addBindings(owner, contributedBindingIdentities);
            databaseAccess.put(database);
        }
    }

    @Override
    public void addBindingsLocal(SxpNode sxpNode, List<PrefixGroup> prefixGroups) throws DatabaseAccessException {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            super.addBindingsLocal(sxpNode, prefixGroups);
            databaseAccess.put(database);
        }
    }

    @Override
    public void expandBindings(int quantity)
            throws DatabaseAccessException, UnknownPrefixException, UnknownHostException {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            super.expandBindings(quantity);
            databaseAccess.put(database);
        }
    }

    @Override
    public MasterDatabase get() throws DatabaseAccessException {
        synchronized (databaseAccess) {
            return databaseAccess.read();
        }
    }

    @Override
    public List<MasterDatabase> partition(int quantity, boolean onlyChanged, SxpBindingFilter filter) throws DatabaseAccessException {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            return super.partition(quantity, onlyChanged, filter);
        }
    }

    @Override
    public void purgeAllDeletedBindings() throws DatabaseAccessException {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            super.purgeAllDeletedBindings();
            databaseAccess.put(database);
        }
    }

    @Override
    public void purgeBindings(NodeId nodeId) throws DatabaseAccessException, NodeIdNotDefinedException {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            super.purgeBindings(nodeId);
            databaseAccess.put(database);
        }
    }

    @Override
    public List<MasterBindingIdentity> readBindings() throws DatabaseAccessException {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            return super.readBindings();
        }
    }

    @Override
    public List<PrefixGroup> readBindingsLocal() throws DatabaseAccessException {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            return super.readBindingsLocal();
        }
    }

    @Override
    public void resetModified() throws DatabaseAccessException {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            super.resetModified();
            databaseAccess.put(database);
        }
    }

    @Override
    public boolean setAsDeleted(SxpNode sxpNode, List<PrefixGroup> prefixGroups) throws DatabaseAccessException {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            boolean result = super.setAsDeleted(sxpNode, prefixGroups);
            databaseAccess.put(database);
            return result;
        }
    }

    @Override
    public String toString() {
        synchronized (databaseAccess) {
            try {
                database = databaseAccess.read();
                return super.toString();
            } catch (Exception e) {
                LOG.warn(controllerName + " {} | {}", e.getClass().getSimpleName(), e.getMessage());
                return "[error]";
            }
        }
    }
}
