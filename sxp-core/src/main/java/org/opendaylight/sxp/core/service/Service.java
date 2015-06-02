/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.ThreadsWorker;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseProvider;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseProvider;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Service<T> implements Callable<T> {

    protected SxpNode owner;
    private boolean notified = false;
    private AtomicReference<ListenableFuture<?>> change = new AtomicReference<>(null);

    protected Service(SxpNode owner) {
        this.owner = owner;
    }

    public void cancel() {
        if(!change.get().isDone()){
            change.get().cancel(false);
        }
    }

    public synchronized MasterDatabaseProvider getBindingMasterDatabase() throws Exception {
        return owner.getBindingMasterDatabase();
    }

    public synchronized SxpDatabaseProvider getBindingSxpDatabase() throws Exception {
        return owner.getBindingSxpDatabase();
    }

    public void notifyChange() {
        synchronized (change) {
            if (change.get() == null || change.get().isDone()) {
                executeChange(this);
            } else {
                notified = true;
            }
        }
    }

    /**
     * Execute new task and recursively check,
     * if specified task was notified, if so start again.
     *
     * @param task Task which contains logic.
     */
    private void executeChange(final Callable<?> task) {
        change.set(owner.getWorker().executeTask(task, ThreadsWorker.WorkerType.Default));
        owner.getWorker().addListener(change.get(), new Runnable() {

            @Override public void run() {
                synchronized (change) {
                    if (notified) {
                        notified = false;
                        executeChange(task);
                    }
                }
            }
        });
    }
}
