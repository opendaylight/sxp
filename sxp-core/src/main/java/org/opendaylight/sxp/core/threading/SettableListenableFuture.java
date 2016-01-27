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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public final class SettableListenableFuture<T> implements ListenableFuture<T> {

    private boolean canceled = false;
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
            listeners.add(new ListenerTuple(Preconditions.checkNotNull(listener),
                Preconditions.checkNotNull(executor)));
        } else {
            future.addListener(listener, executor);
        }
    }

    @Override synchronized public boolean cancel(boolean b) {
        if (future == null) {
            for (ListenerTuple listenerTuple : listeners) {
                listenerTuple.execute();
            }
        }
        return future == null ? (canceled = true) : future.cancel(b);
    }

    @Override synchronized public boolean isCancelled() {
        return future == null ? canceled : future.isCancelled();
    }

    @Override synchronized public boolean isDone() {
        return future == null ? result != null || canceled : future.isDone();
    }

    @Override synchronized public T get() throws InterruptedException, ExecutionException {
        if (result == null) {
            result = future == null ? executor.submit(task).get() : future.get();
            for (ListenerTuple listenerTuple : listeners) {
                listenerTuple.execute();
            }
        }
        return result;
    }

    @Override synchronized public T get(long l, TimeUnit timeUnit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (result == null) {
            result =
                future == null ? executor.submit(task).get(l, timeUnit) : future.get(l, timeUnit);
            for (ListenerTuple listenerTuple : listeners) {
                listenerTuple.execute();
            }
        }
        return result;
    }

    public synchronized Callable<T> getTask() {
        return task;
    }

    public synchronized ListeningExecutorService getExecutor() {
        return executor;
    }

    public synchronized ListenableFuture<T> setFuture(ListenableFuture<T> listenableFuture) {
        if (future == null && !isDone()) {
            future = Preconditions.checkNotNull(listenableFuture);
            for (ListenerTuple listenerTuple : listeners) {
                Preconditions.checkNotNull(listenableFuture)
                    .addListener(listenerTuple.listener, listenerTuple.executor);
            }
        }
        return listenableFuture;
    }

    private final static class ListenerTuple {
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
