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
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.sxp.controller.listeners.TransactionChainListenerImpl;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DatastoreAccess implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreAccess.class.getName());
    private static final TransactionChainListener chainListener = new TransactionChainListenerImpl();

    private BindingTransactionChain bindingTransactionChain;
    private final DataBroker dataBroker;
    private boolean closed = false;

    public static DatastoreAccess getInstance(DataBroker dataBroker) {
        return new DatastoreAccess(Preconditions.checkNotNull(dataBroker));
    }

    public static DatastoreAccess getInstance(DatastoreAccess datastoreAccess) {
        return new DatastoreAccess(Preconditions.checkNotNull(datastoreAccess).dataBroker);
    }

    private DatastoreAccess(DataBroker dataBroker) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        bindingTransactionChain = this.dataBroker.createTransactionChain(chainListener);
    }

    private <T extends DataObject> void checkParams(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        Preconditions.checkNotNull(bindingTransactionChain);
        Preconditions.checkNotNull(logicalDatastoreType);
        Preconditions.checkNotNull(path);
    }

    public <T extends DataObject> ListenableFuture<Void> delete(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        checkParams(path, logicalDatastoreType);
        if (LOG.isDebugEnabled())
            LOG.warn("Delete {} {}", logicalDatastoreType, path.getTargetType());
        synchronized (dataBroker) {
            WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
            transaction.delete(logicalDatastoreType, path);
            return transaction.submit();
        }
    }

    public <T extends DataObject> CheckedFuture<Void, TransactionCommitFailedException> merge(
            InstanceIdentifier<T> path, T data, LogicalDatastoreType logicalDatastoreType) {
        checkParams(path, logicalDatastoreType);
        Preconditions.checkNotNull(data);
        if (LOG.isDebugEnabled())
            LOG.warn("Merge {} {}", logicalDatastoreType, path.getTargetType());
        synchronized (dataBroker) {
            WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
            transaction.merge(logicalDatastoreType, path, data);
            return transaction.submit();
        }
    }

    public <T extends DataObject> CheckedFuture<Void, TransactionCommitFailedException> put(InstanceIdentifier<T> path,
            T data, LogicalDatastoreType logicalDatastoreType) {
        checkParams(path, logicalDatastoreType);
        Preconditions.checkNotNull(data);
        if (LOG.isDebugEnabled())
            LOG.warn("Put {} {}", logicalDatastoreType, path.getTargetType());
        synchronized (dataBroker) {
            WriteTransaction transaction = bindingTransactionChain.newWriteOnlyTransaction();
            transaction.put(logicalDatastoreType, path, data);
            return transaction.submit();
        }
    }

    public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        checkParams(path, logicalDatastoreType);
        synchronized (dataBroker) {
            try (ReadOnlyTransaction transaction = bindingTransactionChain.newReadOnlyTransaction()) {
                return transaction.read(logicalDatastoreType, path);
            }
        }
    }

    @Deprecated public synchronized <T extends DataObject> boolean deleteSynchronous(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        if (closed)
            return false;
        synchronized (dataBroker) {
            try {
                delete(path, logicalDatastoreType).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error deleting {}", path, e);
                return false;
            } catch (Exception e) {
                bindingTransactionChain.close();
                bindingTransactionChain = dataBroker.createTransactionChain(chainListener);
                return deleteSynchronous(path,logicalDatastoreType);
            }
            return true;
        }
    }

    public synchronized <T extends DataObject> boolean mergeSynchronous(InstanceIdentifier<T> path, T data,
            LogicalDatastoreType logicalDatastoreType) {
        if (closed)
            return false;
        synchronized (dataBroker) {
            try {
                merge(path, data, logicalDatastoreType).get();
            } catch (InterruptedException | ExecutionException e) {
                return false;
            } catch (Exception e) {
                bindingTransactionChain.close();
                bindingTransactionChain = dataBroker.createTransactionChain(chainListener);
                return mergeSynchronous(path,data,logicalDatastoreType);
            }
            return true;
        }
    }

    public synchronized <T extends DataObject> boolean putSynchronous(InstanceIdentifier<T> path, T data,
            LogicalDatastoreType logicalDatastoreType) {
        if (closed)
            return false;
        synchronized (dataBroker) {
            try {
                put(path, data, logicalDatastoreType).get();
            } catch (InterruptedException | ExecutionException e) {
                return false;
            } catch (Exception e) {
                bindingTransactionChain.close();
                bindingTransactionChain = dataBroker.createTransactionChain(chainListener);
                return putSynchronous(path,data,logicalDatastoreType);
            }
            return true;
        }
    }

    public synchronized <T extends DataObject> T readSynchronous(InstanceIdentifier<T> path,
            LogicalDatastoreType logicalDatastoreType) {
        if (closed)
            return null;
        synchronized (dataBroker) {
            try {
                Optional<T> result = read(path, logicalDatastoreType).get();
                return result.isPresent() ? result.get() : null;
            } catch (InterruptedException | ExecutionException e) {
                return null;
            } catch (Exception e) {
                bindingTransactionChain.close();
                bindingTransactionChain = dataBroker.createTransactionChain(chainListener);
                return readSynchronous(path,logicalDatastoreType);
            }
        }
    }

    private <T extends DataObject> boolean checkParentExist(final InstanceIdentifier<T> identifier,
            final LogicalDatastoreType datastoreType) {
        final InstanceIdentifier.PathArgument[]
                arguments =
                Iterables.toArray(identifier.getPathArguments(), InstanceIdentifier.PathArgument.class);
        if (arguments.length < 2)
            return true;
        return readSynchronous(identifier.firstIdentifierOf(arguments[arguments.length - 2].getType()), datastoreType)
                != null;
    }

    public synchronized <T extends DataObject> boolean checkAndPut(InstanceIdentifier<T> identifier, T data,
            LogicalDatastoreType datastoreType, final boolean mustContains) {
        Preconditions.checkNotNull(data);
        if (closed)
            return false;
        synchronized (dataBroker) {
            checkParams(identifier, datastoreType);
            final boolean
                    check =
                    checkParentExist(identifier, datastoreType) && (mustContains ?
                            readSynchronous(identifier, datastoreType) != null :
                            readSynchronous(identifier, datastoreType) == null);
            if (check) {
                return putSynchronous(identifier, data, datastoreType);
            }
            return false;
        }
    }

    public synchronized <T extends DataObject> boolean checkAndMerge(InstanceIdentifier<T> identifier, T data,
            LogicalDatastoreType datastoreType, final boolean mustContains) {
        if (closed)
            return false;
        synchronized (dataBroker) {
            Preconditions.checkNotNull(data);
            checkParams(identifier, datastoreType);
            final boolean
                    check =
                    checkParentExist(identifier, datastoreType) && (mustContains ?
                            readSynchronous(identifier, datastoreType) != null :
                            readSynchronous(identifier, datastoreType) == null);
            if (check) {
                return mergeSynchronous(identifier, data, datastoreType);
            }
            return false;
        }
    }

    public synchronized <T extends DataObject> boolean checkAndDelete(InstanceIdentifier<T> identifier,
            LogicalDatastoreType datastoreType) {
        if (closed)
            return false;
        synchronized (dataBroker) {
            checkParams(identifier, datastoreType);
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

    @Override public synchronized void close() {
        closed = true;
        bindingTransactionChain.close();
    }
}
