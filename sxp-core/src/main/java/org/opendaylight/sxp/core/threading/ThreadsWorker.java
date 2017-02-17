/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.threading;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.opendaylight.sxp.core.SxpConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ThreadsWorker class is used for executing and scheduling tasks inside SxpNode and SxpConnection
 */
public class ThreadsWorker implements AutoCloseable {

    /**
     * WorkerType enum is used for running task on specific executor
     */
    public enum WorkerType {
        INBOUND, OUTBOUND, DEFAULT
    }


    private final class QueueKey {

        private final WorkerType workerType;
        private final SxpConnection connection;

        private QueueKey(WorkerType workerType) {
            this.workerType = Preconditions.checkNotNull(workerType);
            this.connection = null;
        }

        private QueueKey(WorkerType workerType, SxpConnection connection) {
            this.workerType = Preconditions.checkNotNull(workerType);
            this.connection = Preconditions.checkNotNull(connection);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            QueueKey queueKey = (QueueKey) o;
            return workerType == queueKey.workerType && Objects.equals(connection, queueKey.connection);
        }

        @Override
        public int hashCode() {
            return Objects.hash(workerType, connection);
        }
    }


    private static final Logger LOG = LoggerFactory.getLogger(ThreadsWorker.class.getName());

    private final ListeningScheduledExecutorService scheduledExecutorService;
    private final ListeningExecutorService executorService, executorServiceOutbound, executorServiceInbound;
    private final Map<QueueKey, Deque<SettableListenableFuture>> dequeMap = new HashMap<>(WorkerType.values().length);

    /**
     * Custom ThreadsWorker constructor
     *
     * @param scheduledExecutorService ScheduledExecutorService which will be used for scheduling tasks like SXP timers
     * @param executorService          ExecutorService which will be used for executing tasks
     * @param executorServiceInbound   ExecutorService which will be used for executing inbound messaging behaviour
     * @param executorServiceOutbound  ExecutorService which will be used for executing outbound messaging behaviour
     */
    public ThreadsWorker(ScheduledExecutorService scheduledExecutorService, ExecutorService executorService,
            ExecutorService executorServiceInbound, ExecutorService executorServiceOutbound) {
        for (WorkerType workerType : WorkerType.values()) {
            dequeMap.put(new QueueKey(workerType), new ArrayDeque<>());
        }
        this.scheduledExecutorService =
                MoreExecutors.listeningDecorator(Preconditions.checkNotNull(scheduledExecutorService));
        this.executorService = MoreExecutors.listeningDecorator(Preconditions.checkNotNull(executorService));
        this.executorServiceInbound =
                MoreExecutors.listeningDecorator(Preconditions.checkNotNull(executorServiceInbound));
        this.executorServiceOutbound =
                MoreExecutors.listeningDecorator(Preconditions.checkNotNull(executorServiceOutbound));
    }

    /**
     * Default ThreadsWorker constructor with these threads pools
     * ScheduledExecutorService contains 2 threads
     * Default executorService contains 2 threads and
     * executorService for inbound and outbound communication
     * have 2 threads both
     */
    public ThreadsWorker() {
        this(2, 2, 2, 1);
    }

    /**
     * @param inPool      Inbound executor poll size
     * @param defaultPool Default executor poll size
     * @param outPool     Outbound executor poll size
     * @param timers      Timer executor poll size
     */
    public ThreadsWorker(int inPool, int defaultPool, int outPool, int timers) {
        this(generateScheduledExecutor(timers, "TIMERS"), generateExecutor(defaultPool, "DEFAULT"),
                generateExecutor(inPool, "INBOUND"), generateExecutor(outPool, "OUTBOUND"));
    }

    /**
     * Gets specified executor
     *
     * @param type Type specifying executor service
     * @return execution service specified by type
     */
    private ListeningExecutorService getExecutor(WorkerType type) {
        switch (type) {
            case INBOUND:
                return executorServiceInbound;
            case OUTBOUND:
                return executorServiceOutbound;
            default:
                return executorService;
        }
    }

    /**
     * Schedule and execute task after specified period in ListeningScheduledExecutorService
     *
     * @param task   Callable task which will be scheduled
     * @param period Time after which task will be executed
     * @param unit   Time unit of period
     * @return ListenableScheduledFuture that can be used to extract result or cancel
     * @throws NullPointerException If task is null
     */
    public <T> ListenableScheduledFuture<T> scheduleTask(Callable<T> task, int period, TimeUnit unit) {
        LOG.debug("Scheduled task {} wit period {} {}", Objects.requireNonNull(task).getClass(), period, unit);
        return scheduledExecutorService.schedule(task, period, unit);
    }

    /**
     * Adds and execute task in ListeningExecutorService
     *
     * @param task Callable task which will be added to execution queue
     * @param type Specifies execution queue used
     * @return ListenableFuture that can be used to extract result or cancel
     * @throws NullPointerException If task is null
     */
    public <T> ListenableFuture<T> executeTask(Callable<T> task, WorkerType type) {
        LOG.debug("Execute task {}", Objects.requireNonNull(task).getClass());
        return getExecutor(type).submit(Objects.requireNonNull(task));
    }

    /**
     * Execute tasks preserving their order, and execution will on specified executor
     *
     * @param task Callable task that will be executed
     * @param type WorkerType specifying type of executor
     * @return ListenableFuture that can be used to extract result or cancel
     */
    public <T> ListenableFuture<T> executeTaskInSequence(final Callable<T> task, final WorkerType type) {
        LOG.debug("Execute in sequence task {}", Objects.requireNonNull(task).getClass());
        return executeTaskInSequence(Objects.requireNonNull(task), new QueueKey(type));
    }

    /**
     * Execute tasks preserving their order, and execution will on specified executor
     *
     * @param task       Callable task that will be executed
     * @param type       WorkerType specifying type of executor
     * @param connection SxpConnection specified as additional key
     * @return ListenableFuture that can be used to extract result or cancel
     */
    public <T> ListenableFuture<T> executeTaskInSequence(final Callable<T> task, final WorkerType type,
            final SxpConnection connection) {
        return executeTaskInSequence(Objects.requireNonNull(task), new QueueKey(type, connection));
    }

    /**
     * Cancel all task queued in sequence of specified worker
     *
     * @param mayInterruptIfRunning if the thread executing this
     *                              task should be interrupted; otherwise, in-progress tasks are allowed
     *                              to complete
     * @param type                  WorkerType specifying type of executor
     */
    public void cancelTasksInSequence(final boolean mayInterruptIfRunning, final WorkerType type) {
        cancelTasksInSequence(mayInterruptIfRunning, new QueueKey(type));
    }

    /**
     * Cancel all task queued in sequence of specified worker
     *
     * @param mayInterruptIfRunning if the thread executing this
     *                              task should be interrupted; otherwise, in-progress tasks are allowed
     *                              to complete
     * @param type                  WorkerType specifying type of executor
     * @param connection            SxpConnection specified as additional key
     */
    public void cancelTasksInSequence(final boolean mayInterruptIfRunning, final WorkerType type,
            final SxpConnection connection) {
        cancelTasksInSequence(mayInterruptIfRunning, new QueueKey(type, connection));
    }

    /**
     * Adds and execute task in ListeningExecutorService
     *
     * @param task Runnable task which will be added to execution queue
     * @param type Specifies execution queue used
     * @return ListenableFuture that can be used to extract result or cancel
     * @throws NullPointerException If task is null
     */
    public ListenableFuture executeTask(Runnable task, WorkerType type) {
        return getExecutor(type).submit(Objects.requireNonNull(task));
    }

    /**
     * Adds execution listener to specified task
     *
     * @param task     Task on which listener will be added.
     * @param listener Task which will be executed after Listenable future is done.
     * @throws NullPointerException If task or listener is null
     */
    public void addListener(ListenableFuture task, Runnable listener) {
        Preconditions.checkNotNull(task).addListener(Objects.requireNonNull(listener), executorService);
    }

    /**
     * Execute task in specified worker or if queue is not empty add it there
     *
     * @param task Task on which listener will be added.
     * @param key  QueueKey specifying que where task will be executed
     * @return ListenableFuture that can be used to extract result or cancel
     */
    private <T> ListenableFuture<T> executeTaskInSequence(final Callable<T> task, final QueueKey key) {
        synchronized (dequeMap) {
            if (!dequeMap.containsKey(key)) {
                dequeMap.put(key, new ArrayDeque<>());
            }
        }
        synchronized (dequeMap.get(key)) {
            SettableListenableFuture<T> future = new SettableListenableFuture<>(task, getExecutor(key.workerType));
            dequeMap.get(key).addLast(future);
            if (dequeMap.get(key).size() == 1) {
                ListenableFuture<T> callback = future.getExecutor().submit(future.getTask());
                callback.addListener(() -> {
                    synchronized (dequeMap.get(key)) {
                        dequeMap.get(key).pollFirst();
                        sequenceRecursion(key);
                    }
                }, getExecutor(key.workerType));
                return callback;
            } else {
                return future;
            }
        }
    }

    /**
     * Recursively call next task in queue
     *
     * @param key QueueKey specifying que where task will be executed
     */
    private void sequenceRecursion(final QueueKey key) {
        SettableListenableFuture future = dequeMap.get(key).peekFirst();
        if (future != null) {
            if (!future.isDone()) {
                future.setFuture(future.getExecutor().submit(future.getTask())).addListener(() -> {
                    synchronized (dequeMap.get(key)) {
                        dequeMap.get(key).pollFirst();
                        sequenceRecursion(key);
                    }
                }, future.getExecutor());
            } else {
                dequeMap.get(key).pollFirst();
                sequenceRecursion(key);
            }
        }
    }

    /**
     * Cancel all remaining tasks in queue
     *
     * @param mayInterruptIfRunning if the thread executing this
     *                              task should be interrupted; otherwise, in-progress tasks are allowed
     *                              to complete
     * @param key                   QueueKey specifying que where task will be executed
     */
    private void cancelTasksInSequence(final boolean mayInterruptIfRunning, QueueKey key) {
        synchronized (dequeMap) {
            if (dequeMap.get(key) == null) {
                return;
            }
        }
        synchronized (dequeMap.get(key)) {
            dequeMap.get(key)
                    .stream()
                    .filter(task -> !task.isDone())
                    .forEach(task -> task.cancel(mayInterruptIfRunning));
            dequeMap.get(key).clear();
        }
    }

    /**
     * @param threads  num of uses threads
     * @param poolName Pool name displayed in logs
     * @return Customized thread poll
     */
    public static ExecutorService generateExecutor(int threads, final String poolName) {
        final ThreadFactory threadFactory;
        if (poolName == null) {
            threadFactory = Executors.defaultThreadFactory();
        } else {
            threadFactory = new ThreadFactoryBuilder().setNameFormat(poolName + "-%d").build();
        }
        return new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                threadFactory) {

            @Override
            protected void afterExecute(Runnable runnable, Throwable throwable) {
                super.afterExecute(runnable, throwable);
                if (Objects.nonNull(throwable)) {
                    LOG.debug("Task {} failed with {}", runnable, throwable);
                }
            }
        };
    }

    /**
     * @param threads  num of uses threads
     * @param poolName Pool name displayed in logs
     * @return Customized thread poll
     */
    public static ScheduledExecutorService generateScheduledExecutor(int threads, final String poolName) {
        final ThreadFactory threadFactory;
        if (poolName == null) {
            threadFactory = Executors.defaultThreadFactory();
        } else {
            threadFactory = new ThreadFactoryBuilder().setNameFormat(poolName + "-%d").build();
        }
        return new ScheduledThreadPoolExecutor(threads, threadFactory) {

            @Override
            protected void afterExecute(Runnable runnable, Throwable throwable) {
                super.afterExecute(runnable, throwable);
                if (Objects.nonNull(throwable)) {
                    LOG.info("Task {} failed with {}", runnable, throwable);
                }
            }
        };
    }

    @Override
    public void close() {
        scheduledExecutorService.shutdown();
        executorService.shutdown();
        executorServiceInbound.shutdown();
        executorServiceOutbound.shutdown();
    }
}
