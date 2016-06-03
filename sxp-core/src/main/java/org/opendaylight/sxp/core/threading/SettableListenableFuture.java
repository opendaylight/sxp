/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.threading;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * SettableListenableFuture class represent temporally ListenableFuture that serves as forwarder of requests, or if no ListenableFuture was set handle requests on its own.
 *
 * @param <T> Type of result
 */
public final class SettableListenableFuture<T> implements ListenableFuture<T> {

    private boolean canceled = false, done = false;
    private final List<ListenerTuple> listeners = new ArrayList<>();
    private final Callable<T> task;
    private final ListeningExecutorService executor;
    private ListenableFuture<T> future = null;
    private T result = null;

    public SettableListenableFuture(Callable<T> task, ListeningExecutorService executor) {
        this.task = Preconditions.checkNotNull(task);
        this.executor = Preconditions.checkNotNull(executor);
    }

    @Override synchronized public void addListener(Runnable listener, Executor executor) {
        if (future == null) {
            listeners.add(
                    new ListenerTuple(Preconditions.checkNotNull(listener), Preconditions.checkNotNull(executor)));
        } else {
            future.addListener(listener, executor);
        }
    }

    @Override synchronized public boolean cancel(boolean b) {
        boolean result = future == null ? (canceled = true) : future.cancel(b);
        if (future == null) {
            for (ListenerTuple listenerTuple : listeners) {
                listenerTuple.execute();
            }
        }
        return result;
    }

    @Override synchronized public boolean isCancelled() {
        return future == null ? canceled : future.isCancelled();
    }

    @Override synchronized public boolean isDone() {
        return future == null ? done || isCancelled() : future.isDone();
    }

    @Override synchronized public T get() throws InterruptedException, ExecutionException {
        if (future == null) {
            if (isDone()) {
                return result;
            }
            done = true;
            result = executor.submit(task).get();
            for (ListenerTuple listenerTuple : listeners) {
                listenerTuple.execute();
            }
            return result;
        }
        return future.get();
    }

    @Override synchronized public T get(long l, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (future == null) {
            if (isDone()) {
                return result;
            }
            done = true;
            result = executor.submit(task).get(l, timeUnit);
            for (ListenerTuple listenerTuple : listeners) {
                listenerTuple.execute();
            }
            return result;
        }
        return future.get(l, timeUnit);
    }

    /**
     * @return Task linked to this ListenableFuture
     */
    public synchronized Callable<T> getTask() {
        return task;
    }

    /**
     * @return Executor on which task linked with this ListenableFuture was/will be executed
     */
    public synchronized ListeningExecutorService getExecutor() {
        return executor;
    }

    /**
     * Sets forwarding to other ListenableFuture
     *
     * @param listenableFuture ListenableFuture that will be set for forwarding
     * @return this ListenableFuture
     */
    public synchronized ListenableFuture<T> setFuture(ListenableFuture<T> listenableFuture) {
        if (future == null && !isDone()) {
            future = Preconditions.checkNotNull(listenableFuture);
            for (ListenerTuple listenerTuple : listeners) {
                Preconditions.checkNotNull(listenableFuture)
                        .addListener(listenerTuple.listener, listenerTuple.executor);
            }
        }
        return this;
    }

    private final class ListenerTuple {

        private final Runnable listener;
        private final Executor executor;

        public ListenerTuple(Runnable listener, Executor executor) {
            this.listener = listener;
            this.executor = executor;
        }

        public void execute() {
            executor.execute(listener);
        }

    }
}
