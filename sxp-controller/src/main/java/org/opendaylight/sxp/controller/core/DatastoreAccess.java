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
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.sxp.controller.listeners.TransactionChainListenerImpl;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DatastoreAccess implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreAccess.class);

    private final TransactionChainListener chainListener;
    private BindingTransactionChain bindingTransactionChain;
    private final DataBroker dataBroker;
    private boolean closed = false;
    private final List<DatastoreAccess> childDatastoreAccesses = new ArrayList<>();

    /**
     * @param dataBroker DataBroker used for accessing data store
     * @return DataAccess used to access data store
     */
    public static DatastoreAccess getInstance(DataBroker dataBroker) {
        return new DatastoreAccess(Preconditions.checkNotNull(dataBroker));
    }

    /**
     * Creates Datastore access with Parent-child dependency,
     * when Parent is closed all its children are also closed.
     *
     * @param datastoreAccess DataAccess used to access data store
     * @return DataAccess used to access data store
     */
    public static DatastoreAccess getInstance(DatastoreAccess datastoreAccess) {
        DatastoreAccess child = new DatastoreAccess(Preconditions.checkNotNull(datastoreAccess).dataBroker);
        datastoreAccess.childDatastoreAccesses.add(child);
        return child;
    }

    /**
     * @param dataBroker DataBroker that will be used to access data store
     */
    private DatastoreAccess(DataBroker dataBroker) {
        this.chainListener = new TransactionChainListenerImpl(this);
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        bindingTransactionChain = this.dataBroker.createTransactionChain(chainListener);
    }

    /**
     * @param path                 Identifier path to be checked
     * @param logicalDatastoreType DataStore type to be checked
     * @param <T>                  Any type extending DataObject
     */
    private <T extends DataObject> boolean checkParams(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        if (closed) {
            return false;
        }
        Preconditions.checkNotNull(bindingTransactionChain);
        Preconditions.checkNotNull(logicalDatastoreType);
        Preconditions.checkNotNull(path);
        return true;
    }

    /**
     * Delete data at specified path.
     *
     * @param <T>                  Any type extending DataObject
     * @param path                 InstanceIdentifier path specifying data
     * @param logicalDatastoreType Type of datastore where operation will be held
     * @return ListenableFuture callback of operation
     */
    public synchronized <T extends DataObject> FluentFuture<? extends CommitInfo> delete(InstanceIdentifier<T> path,
                                                                                         LogicalDatastoreType logicalDatastoreType) {
        if (!checkParams(path, logicalDatastoreType)) {
            return FluentFutures.immediateCancelledFluentFuture();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Delete {} {}", logicalDatastoreType, path.getTargetType());
        }
        synchronized (dataBroker) {
            WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
            transaction.delete(logicalDatastoreType, path);
            return transaction.commit();
        }
    }

    /**
     * @param <T>                  Any type extending DataObject
     * @param path                 InstanceIdentifier path specifying data
     * @param data                 Data that will be used in operation
     * @param logicalDatastoreType Type of datastore where operation will be held
     * @return Future holding the results of the datastore operation
     */
    public synchronized <T extends DataObject> FluentFuture<? extends CommitInfo> merge(
            InstanceIdentifier<T> path, T data, LogicalDatastoreType logicalDatastoreType) {
        if (!checkParams(path, logicalDatastoreType)) {
            return FluentFutures.immediateCancelledFluentFuture();
        }
        Preconditions.checkNotNull(data);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Merge {} {}", logicalDatastoreType, path.getTargetType());
        }
        WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
        transaction.merge(logicalDatastoreType, path, data);
        return transaction.commit();
    }

    /**
     * @param <T>                  Any type extending DataObject
     * @param path                 InstanceIdentifier path specifying data
     * @param data                 Data that will be used in operation
     * @param logicalDatastoreType Type of datastore where operation will be held
     * @return Future holding the results of the datastore operation
     */
    public synchronized <T extends DataObject> FluentFuture<? extends CommitInfo> put(
            InstanceIdentifier<T> path, T data, LogicalDatastoreType logicalDatastoreType) {
        if (!checkParams(path, logicalDatastoreType)) {
            return FluentFutures.immediateCancelledFluentFuture();
        }
        Preconditions.checkNotNull(data);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Put {} {}", logicalDatastoreType, path.getTargetType());
        }
        WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
        transaction.put(logicalDatastoreType, path, data);
        return transaction.commit();
    }

    /**
     * @param path                 InstanceIdentifier path specifying data
     * @param logicalDatastoreType Type of datastore where operation will be held
     * @param <T>                  Any type extending DataObject
     * @return CheckedFuture callback of operation
     */
    public synchronized <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(
            InstanceIdentifier<T> path, LogicalDatastoreType logicalDatastoreType) {
        if (!checkParams(path, logicalDatastoreType)) {
            return Futures.makeChecked(Futures.immediateCancelledFuture(), input ->
                    new ReadFailedException("Datastore was closed"));
        }
        try (ReadOnlyTransaction transaction = bindingTransactionChain.newReadOnlyTransaction()) {
            return transaction.read(logicalDatastoreType, path);
        }
    }

    /**
     * @param path                 InstanceIdentifier path specifying data
     * @param data                 Data that will be used in operation
     * @param logicalDatastoreType Type of datastore where operation will be held
     * @param <T>                  Any type extending DataObject
     * @return If operation was successful
     */
    public synchronized <T extends DataObject> boolean mergeSynchronous(InstanceIdentifier<T> path, T data,
            LogicalDatastoreType logicalDatastoreType) {
        try {
            merge(path, data, logicalDatastoreType).get();
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Failed to merge {} to {}", path, logicalDatastoreType, e);
            return false;
        }
        return true;
    }

    /**
     * Put data at specified path and wait till operation ends.
     *
     * @param path                 InstanceIdentifier path specifying data
     * @param data                 Data that will be used in operation
     * @param logicalDatastoreType Type of datastore where operation will be held
     * @param <T>                  Any type extending DataObject
     * @return If operation was successful
     */
    public synchronized <T extends DataObject> boolean putSynchronous(InstanceIdentifier<T> path, T data,
            LogicalDatastoreType logicalDatastoreType) {
        try {
            put(path, data, logicalDatastoreType).get();
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Failed to put {} to {}", path, logicalDatastoreType, e);
            return false;
        }
        return true;
    }

    /**
     * Delete data at specified path and wait till operation ends.
     *
     * @param path Path to node to be deleted
     * @param logicalDatastoreType data-store type
     * @return {@code true} if data node was successfully deleted, {@code false} otherwise
     */
    public synchronized <T extends DataObject> boolean deleteSynchronous(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        try {
            delete(path, logicalDatastoreType).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Failed to delete {} from {}, exception: {}", path, logicalDatastoreType, e);
            return false;
        } catch (ExecutionException e) {
            LOG.error("Failed to delete {} from {}, exception: {}", path, logicalDatastoreType, e);
            return false;
        }
        return true;
    }

    /**
     * @param path                 InstanceIdentifier path specifying data
     * @param logicalDatastoreType Type of datastore where operation will be held
     * @param <T>                  Any type extending DataObject
     * @return Data read from datastore or null if operation was interrupted
     */
    public synchronized <T extends DataObject> T readSynchronous(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        try {
            Optional<T> result = read(path, logicalDatastoreType).checkedGet();
            return result.isPresent() ? result.get() : null;
        } catch (ReadFailedException e) {
            return null;
        }
    }

    /**
     * @param identifier    InstanceIdentifier that will be checked
     * @param datastoreType Datastore type where datastore will be checked
     * @param <T>           Any type extending DataObject
     * @return If All parents of provided path exists
     */
    private <T extends DataObject> boolean checkParentExist(final InstanceIdentifier<T> identifier,
            final LogicalDatastoreType datastoreType) {
        final InstanceIdentifier.PathArgument[]
                arguments =
                Iterables.toArray(identifier.getPathArguments(), InstanceIdentifier.PathArgument.class);
        return arguments.length < 2 ||
                readSynchronous(identifier.firstIdentifierOf(arguments[arguments.length - 2].getType()), datastoreType)
                        != null;
    }

    /**
     * @param identifier    InstanceIdentifier path specifying data
     * @param data          Data that will be used in operation
     * @param datastoreType Type of datastore where operation will be held
     * @param mustContains  Specifying if object mus preexist or not
     * @param <T>           Any type extending DataObject
     * @return If operation was successful
     */
    public synchronized <T extends DataObject> boolean checkAndPut(InstanceIdentifier<T> identifier, T data,
            LogicalDatastoreType datastoreType, final boolean mustContains) {
        final boolean
                check =
                checkParentExist(identifier, datastoreType) && (mustContains ?
                        readSynchronous(identifier, datastoreType) != null :
                        readSynchronous(identifier, datastoreType) == null);
        return check && !put(identifier, data, datastoreType).isCancelled();
    }

    /**
     * @param identifier    InstanceIdentifier path specifying data
     * @param data          Data that will be used in operation
     * @param datastoreType Type of datastore where operation will be held
     * @param mustContains  Specifying if object mus preexist or not
     * @param <T>           Any type extending DataObject
     * @return If operation was successful
     */
    public synchronized <T extends DataObject> boolean checkAndMerge(InstanceIdentifier<T> identifier, T data,
            LogicalDatastoreType datastoreType, final boolean mustContains) {
        final boolean
                check =
                checkParentExist(identifier, datastoreType) && (mustContains ?
                        readSynchronous(identifier, datastoreType) != null :
                        readSynchronous(identifier, datastoreType) == null);
        if (check) {
            return !merge(identifier, data, datastoreType).isCancelled();
        }
        return false;
    }

    /**
     * @param identifier    InstanceIdentifier path specifying data
     * @param datastoreType Type of datastore where operation will be held
     * @param <T>           Any type extending DataObject
     * @return If operation was successful
     */
    public synchronized <T extends DataObject> boolean checkAndDelete(InstanceIdentifier<T> identifier,
            LogicalDatastoreType datastoreType) {
        if (readSynchronous(identifier, datastoreType) != null) {
            return !delete(identifier, datastoreType).isCancelled();
        }
        return false;
    }

    /**
     * Create node synchronously in data-store only if it has NOT previously exist.
     * <p>
     * This method does not check for existence of parent nodes. It is
     * responsibility of its caller to create all parent nodes.
     *
     * @param identifier Path to node to be created
     * @param data Node data to be created
     * @param datastoreType data-store type
     * @return {@code true} if node was successfully created, {@code false} otherwise
     */
    public synchronized <T extends DataObject> boolean putIfNotExists(InstanceIdentifier<T> identifier, T data,
            LogicalDatastoreType datastoreType) {
        if (readSynchronous(identifier, datastoreType) != null) {
            LOG.warn("Node to be created {} has already exist", identifier);
            return false;
        }

        return putSynchronous(identifier, data, datastoreType);
    }

    /**
     * Recreate transaction chain if error occurred and decrement allowed error rate counter
     */
    public synchronized void reinitializeChain() {
        if (!closed) {
            bindingTransactionChain.close();
            bindingTransactionChain = dataBroker.createTransactionChain(chainListener);
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            bindingTransactionChain.close();
            childDatastoreAccesses.forEach(DatastoreAccess::close);
        }
    }
}
