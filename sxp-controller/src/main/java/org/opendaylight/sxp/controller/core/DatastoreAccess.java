/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.sxp.controller.listeners.TransactionChainListenerImpl;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public final class DatastoreAccess {

    private final BindingTransactionChain bindingTransactionChain;
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreAccess.class.getName());

    // TODO: this effectively prevents us from releasing the DataBroker, which may cause problems
    private static volatile DatastoreAccess INSTANCE = null;

    public static DatastoreAccess getInstance(DataBroker dataBroker) {
        // Storing in local variable prevents multiple volatile reads
        DatastoreAccess local = INSTANCE;
        if (local == null) {
            synchronized (DatastoreAccess.class) {
                local = INSTANCE;
                if (local == null) {
                    local = new DatastoreAccess(dataBroker);
                    INSTANCE = local;
                }
            }
        }

        return local;
    }

    private DatastoreAccess(DataBroker dataBroker) {
        Preconditions.checkNotNull(dataBroker);
        bindingTransactionChain = dataBroker.createTransactionChain(new TransactionChainListenerImpl());
    }

    public <T extends DataObject> void checkParams(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        Preconditions.checkNotNull(bindingTransactionChain);
        Preconditions.checkNotNull(logicalDatastoreType);
        Preconditions.checkNotNull(path);
    }

    public <T extends DataObject> ListenableFuture<Void> delete(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        checkParams(path, logicalDatastoreType);
        synchronized (bindingTransactionChain) {
            //LOG.warn("Delete {}", path);
            WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
            transaction.delete(logicalDatastoreType, path);
            return transaction.submit();
        }
    }

    public <T extends DataObject> CheckedFuture<Void, TransactionCommitFailedException> merge(
            InstanceIdentifier<T> path, T data, LogicalDatastoreType logicalDatastoreType) {
        checkParams(path, logicalDatastoreType);
        Preconditions.checkNotNull(data);
        synchronized (bindingTransactionChain) {
            //LOG.warn("Merge {}", path);
            WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
            transaction.merge(logicalDatastoreType, path, data);
            return transaction.submit();
        }
    }

    public <T extends DataObject> CheckedFuture<Void, TransactionCommitFailedException> put(InstanceIdentifier<T> path,
            T data, LogicalDatastoreType logicalDatastoreType) {
        checkParams(path, logicalDatastoreType);
        Preconditions.checkNotNull(data);
        synchronized (bindingTransactionChain) {
            //LOG.warn("Put {}", path);
            WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
            transaction.put(logicalDatastoreType, path, data);
            return transaction.submit();
        }
    }

    public <T extends DataObject> ListenableFuture<Void> putListenable(InstanceIdentifier<T> path, T data,
            LogicalDatastoreType logicalDatastoreType) {
        checkParams(path, logicalDatastoreType);
        Preconditions.checkNotNull(data);
        synchronized (bindingTransactionChain) {
            WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
            transaction.put(logicalDatastoreType, path, data);
            return transaction.submit();
        }
    }

    public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        checkParams(path, logicalDatastoreType);
        synchronized (bindingTransactionChain) {
            try (ReadOnlyTransaction transaction = bindingTransactionChain.newReadOnlyTransaction()) {
                return transaction.read(logicalDatastoreType, path);
            }
        }
    }

    @Deprecated public <T extends DataObject> boolean deleteSynchronous(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        try {
            delete(path, logicalDatastoreType).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error deleting {}", path, e);
            return false;
        }
        return true;
    }

    public <T extends DataObject> boolean mergeSynchronous(InstanceIdentifier<T> path, T data,
            LogicalDatastoreType logicalDatastoreType) {
        try {
            merge(path, data, logicalDatastoreType).get();
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
        return true;
    }

    public <T extends DataObject> boolean putSynchronous(InstanceIdentifier<T> path, T data,
            LogicalDatastoreType logicalDatastoreType) {
        try {
            put(path, data, logicalDatastoreType).get();
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
        return true;
    }

    public <T extends DataObject> T readSynchronous(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        try {
            Optional<T> result = read(path, logicalDatastoreType).get();
            return result.isPresent() ? result.get() : null;
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    public <T extends DataObject> boolean checkAndPut(InstanceIdentifier<T> identifier, T data,
            LogicalDatastoreType datastoreType, final boolean mustContains) {
        Preconditions.checkNotNull(identifier);
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(datastoreType);
        //TODO add checks for parents
        synchronized (bindingTransactionChain) {
            final boolean
                    check =
                    mustContains ?
                            readSynchronous(identifier, datastoreType) != null :
                            readSynchronous(identifier, datastoreType) == null;
            if (check) {
                return putSynchronous(identifier, data, datastoreType);
            }
            return false;
        }
    }

    public <T extends DataObject> boolean checkAndDelete(InstanceIdentifier<T> identifier,
            LogicalDatastoreType datastoreType) {
        Preconditions.checkNotNull(identifier);
        Preconditions.checkNotNull(datastoreType);
        synchronized (bindingTransactionChain) {
            if (readSynchronous(identifier, datastoreType) != null) {
                try {
                    delete(identifier, datastoreType).get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Error deleting {}", identifier, e);
                    return false;
                }
                return true;
            }
            return false;
        }
    }
}
