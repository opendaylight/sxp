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

import com.google.common.base.Preconditions;
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

    private final MasterDatabaseAccess databaseAccess;

    public MasterDatastoreImpl(MasterDatabaseAccess databaseAccess) {
        this.databaseAccess = Preconditions.checkNotNull(databaseAccess);
    }

    @Override
    public synchronized void addBindings(NodeId owner, List<MasterBindingIdentity> contributedBindingIdentities)
            throws DatabaseAccessException, NodeIdNotDefinedException {
        database = databaseAccess.read();
        super.addBindings(owner, contributedBindingIdentities);
        databaseAccess.put(database);
    }

    @Override
    public synchronized void addBindingsLocal(SxpNode sxpNode, List<PrefixGroup> prefixGroups) throws DatabaseAccessException {
        database = databaseAccess.read();
        super.addBindingsLocal(sxpNode, prefixGroups);
        databaseAccess.put(database);
    }

    @Override
    public synchronized void expandBindings(int quantity)
            throws DatabaseAccessException, UnknownPrefixException, UnknownHostException {
        database = databaseAccess.read();
        super.expandBindings(quantity);
        databaseAccess.put(database);
    }

    @Override
    public synchronized MasterDatabase get() throws DatabaseAccessException {
        return databaseAccess.read();
    }

    @Override
    public synchronized List<MasterDatabase> partition(int quantity, boolean onlyChanged, SxpBindingFilter filter) throws DatabaseAccessException {
        database = databaseAccess.read();
        return super.partition(quantity, onlyChanged, filter);
    }

    @Override
    public synchronized void purgeAllDeletedBindings() throws DatabaseAccessException {
        database = databaseAccess.read();
        super.purgeAllDeletedBindings();
        databaseAccess.put(database);
    }

    @Override
    public synchronized void purgeBindings(NodeId nodeId) throws DatabaseAccessException, NodeIdNotDefinedException {
        database = databaseAccess.read();
        super.purgeBindings(nodeId);
        databaseAccess.put(database);
    }

    @Override
    public synchronized List<MasterBindingIdentity> readBindings() throws DatabaseAccessException {
        database = databaseAccess.read();
        return super.readBindings();
    }

    @Override
    public synchronized List<PrefixGroup> readBindingsLocal() throws DatabaseAccessException {
        database = databaseAccess.read();
        return super.readBindingsLocal();
    }

    @Override
    public synchronized void resetModified() throws DatabaseAccessException {
        database = databaseAccess.read();
        super.resetModified();
        databaseAccess.put(database);
    }

    @Override
    public synchronized boolean setAsDeleted(SxpNode sxpNode, List<PrefixGroup> prefixGroups) throws DatabaseAccessException {
        database = databaseAccess.read();
        boolean result = super.setAsDeleted(sxpNode, prefixGroups);
        databaseAccess.put(database);
        return result;
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
