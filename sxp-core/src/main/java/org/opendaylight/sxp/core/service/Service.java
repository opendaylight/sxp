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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service class provide logic to execute more complex Services
 * grating that each instance of Service is running only once
 * regardless on how many time was service triggered
 *
 * @param <T> Type of service result if any
 */
public abstract class Service<T> implements Callable<T> {

    protected SxpNode owner;
    private AtomicInteger notified = new AtomicInteger(0);
    private ListenableFuture<?> change = null;

    /**
     * Default constructor
     * @param owner SxpNode that this service belongs to
     */
    protected Service(SxpNode owner) {
        this.owner = owner;
    }

    /**
     * Cancel current running service
     */
    public void cancel() {
        if (change != null) {
            change.cancel(false);
        }
        notified.set(0);
    }

    /**
     * @return Gets MasterDatabaseProvider used by owner Node
     */
    public synchronized MasterDatabaseProvider getBindingMasterDatabase() {
        return owner.getBindingMasterDatabase();
    }

    /**
     * @return Gets SxpDatabaseProvider used by owner Node
     */
    public synchronized SxpDatabaseProvider getBindingSxpDatabase() {
        return owner.getBindingSxpDatabase();
    }

    /**
     * Execute current Service or if Service is running,
     * schedule another run after current run finishes
     */
    public void notifyChange() {
        if (notified.getAndIncrement() == 0) {
            executeChange(this);
        }
    }

    /**
     * Execute new task and recursively check,
     * if specified task was notified, if so start again.
     *
     * @param task Task which contains logic.
     */
    private void executeChange(final Callable<?> task) {
        change = owner.getWorker().executeTask(task, ThreadsWorker.WorkerType.DEFAULT);
        owner.getWorker().addListener(change, new Runnable() {

            @Override public void run() {
                if (notified.decrementAndGet() > 0) {
                    notified.set(1);
                    executeChange(task);
                }
            }
        });
    }
}
