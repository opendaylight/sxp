/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.*;

import java.util.concurrent.*;

/**
 * ThreadsWorker class is used for executing and scheduling tasks inside SxpNode and SxpConnection
 */
public class ThreadsWorker {

        /**
         * WorkerType enum is used for running task on specific executor
         */
        public enum WorkerType {
                INBOUND, OUTBOUND, DEFAULT
        }


        private final ListeningScheduledExecutorService scheduledExecutorService;
        private final ListeningExecutorService executorService, executorServiceOutbound, executorServiceInbound;

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
         * ScheduledExecutorService contains 10 threads
         * Default executorService contains 5 threads and
         * executorService for inbound and outbound communication
         * have 10 threads both
         */
        public ThreadsWorker() {
                this(Executors.newScheduledThreadPool(10), Executors.newFixedThreadPool(10),
                        Executors.newFixedThreadPool(5), Executors.newFixedThreadPool(10));
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
                return scheduledExecutorService.schedule(Preconditions.checkNotNull(task), period, unit);
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
                return getExecutor(type).submit(Preconditions.checkNotNull(task));
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
                return getExecutor(type).submit(Preconditions.checkNotNull(task));
        }

        /**
         * Adds execution listener to specified task
         *
         * @param task     Task on which listener will be added.
         * @param listener Task which will be executed after Listenable future is done.
         * @throws NullPointerException If task or listener is null
         */
        public void addListener(ListenableFuture task, Runnable listener) {
                Preconditions.checkNotNull(task).addListener(Preconditions.checkNotNull(listener), executorService);
        }
}
