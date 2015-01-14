/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import java.util.List;

import org.opendaylight.sxp.util.database.MasterBindingIdentity;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseAccess;
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
    public void addBindings(NodeId owner, List<MasterBindingIdentity> contributedBindingIdentities) throws Exception {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            synchronized (database) {
                super.addBindings(owner, contributedBindingIdentities);
                databaseAccess.put(database);
            }
        }
    }

    @Override
    public void addBindingsLocal(List<PrefixGroup> prefixGroups) throws Exception {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            super.addBindingsLocal(prefixGroups);
            databaseAccess.put(database);
        }
    }

    @Override
    public void expandBindings(int quantity) throws Exception {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            synchronized (database) {
                super.expandBindings(quantity);
                databaseAccess.put(database);
            }
        }
    }

    @Override
    public MasterDatabase get() throws Exception {
        synchronized (databaseAccess) {
            return databaseAccess.read();
        }
    }

    @Override
    public List<MasterDatabase> partition(int quantity, boolean onlyChanged) throws Exception {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            synchronized (database) {
                return super.partition(quantity, onlyChanged);
            }
        }
    }

    @Override
    public void purgeAllDeletedBindings() throws Exception {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            synchronized (database) {
                super.purgeAllDeletedBindings();
                databaseAccess.put(database);
            }
        }
    }

    @Override
    public void purgeBindings(NodeId nodeId) throws Exception {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            synchronized (database) {
                super.purgeBindings(nodeId);
                databaseAccess.put(database);
            }
        }
    }

    @Override
    public List<MasterBindingIdentity> readBindings() throws Exception {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            synchronized (database) {
                return super.readBindings();
            }
        }
    }

    @Override
    public List<PrefixGroup> readBindingsLocal() throws Exception {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            synchronized (database) {
                return super.readBindingsLocal();
            }
        }
    }

    @Override
    public void resetModified() throws Exception {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            synchronized (database) {
                super.resetModified();
                databaseAccess.put(database);
            }
        }
    }

    @Override
    public boolean setAsDeleted(List<PrefixGroup> prefixGroups) throws Exception {
        synchronized (databaseAccess) {
            database = databaseAccess.read();
            synchronized (database) {
                boolean result = super.setAsDeleted(prefixGroups);
                databaseAccess.put(database);
                return result;
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
            synchronized (database) {
                return super.toString();
            }
        }
    }
}
