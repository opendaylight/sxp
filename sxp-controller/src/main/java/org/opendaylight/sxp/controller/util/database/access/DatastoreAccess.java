/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database.access;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;

public final class DatastoreAccess {

    private final BindingTransactionChain bindingTransactionChain;

    private static DatastoreAccess instance = null;

    public static synchronized DatastoreAccess getInstance(DataBroker dataBroker) {
        if (instance == null) {
            instance = new DatastoreAccess(dataBroker);
        }
        return instance;
    }

    private DatastoreAccess(DataBroker dataBroker) {
        Preconditions.checkNotNull(dataBroker);
        bindingTransactionChain = dataBroker.createTransactionChain(new TransactionChainListenerImpl());
    }

    public <T extends DataObject> ListenableFuture<Void> delete(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        Preconditions.checkNotNull(bindingTransactionChain);
        Preconditions.checkNotNull(logicalDatastoreType);
        Preconditions.checkNotNull(path);
        synchronized (bindingTransactionChain) {
            WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
            transaction.delete(logicalDatastoreType, path);
            return transaction.submit();
        }
    }

    public <T extends DataObject> CheckedFuture<Void, TransactionCommitFailedException> merge(
            InstanceIdentifier<T> path, T data, LogicalDatastoreType logicalDatastoreType) {
        Preconditions.checkNotNull(bindingTransactionChain);
        Preconditions.checkNotNull(logicalDatastoreType);
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(data);
        synchronized (bindingTransactionChain) {
            WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
            transaction.merge(logicalDatastoreType, path, data);
            return transaction.submit();
        }
    }

    public <T extends DataObject> CheckedFuture<Void, TransactionCommitFailedException> put(InstanceIdentifier<T> path,
            T data, LogicalDatastoreType logicalDatastoreType) {
        Preconditions.checkNotNull(bindingTransactionChain);
        Preconditions.checkNotNull(logicalDatastoreType);
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(data);
        synchronized (bindingTransactionChain) {
            WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
            transaction.put(logicalDatastoreType, path, data);
            return transaction.submit();
        }
    }

    public <T extends DataObject> ListenableFuture<Void> putListenable(InstanceIdentifier<T> path, T data,
            LogicalDatastoreType logicalDatastoreType) {
        Preconditions.checkNotNull(bindingTransactionChain);
        Preconditions.checkNotNull(logicalDatastoreType);
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(data);
        synchronized (bindingTransactionChain) {
            WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
            transaction.put(logicalDatastoreType, path, data);
            return transaction.submit();
        }
    }

    public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        Preconditions.checkNotNull(bindingTransactionChain);
        Preconditions.checkNotNull(logicalDatastoreType);
        Preconditions.checkNotNull(path);
        synchronized (bindingTransactionChain) {
            try (ReadOnlyTransaction transaction = bindingTransactionChain.newReadOnlyTransaction()) {
                return transaction.read(logicalDatastoreType, path);
            }
        }
    }
}
