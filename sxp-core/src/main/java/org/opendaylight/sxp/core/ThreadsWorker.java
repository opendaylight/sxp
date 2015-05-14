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

        private final ListeningScheduledExecutorService scheduledExecutorService;
        private final ListeningExecutorService executorService;

        /**
         * Custom ThreadsWorker constructor
         *
         * @param scheduledExecutorService ScheduledExecutorService which will be used for scheduling tasks
         * @param executorService          ExecutorService which will be used for executing tasks
         * @throws NullPointerException If scheduledExecutorService or executorService is null
         */
        public ThreadsWorker(ScheduledExecutorService scheduledExecutorService, ExecutorService executorService) {
                this.scheduledExecutorService =
                        MoreExecutors.listeningDecorator(Preconditions.checkNotNull(scheduledExecutorService));
                this.executorService = MoreExecutors.listeningDecorator(Preconditions.checkNotNull(executorService));
        }

        /**
         * Default ThreadsWorker constructor with these threads pools
         * ScheduledExecutorService contains 10 threads
         * ExecutorService contains 25 threads
         */
        public ThreadsWorker() {
                this(Executors.newScheduledThreadPool(10), Executors.newFixedThreadPool(25));
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
        public ListenableScheduledFuture<?> scheduleTask(Callable task, int period, TimeUnit unit) {
                return scheduledExecutorService.schedule(Preconditions.checkNotNull(task), period, unit);
        }

        /**
         * Adds and execute task in ListeningExecutorService
         *
         * @param task Callable task which will be added to execution queue
         * @return ListenableFuture that can be used to extract result or cancel
         * @throws NullPointerException If task is null
         */
        public ListenableFuture<?> executeTask(Callable task) {
                return executorService.submit(Preconditions.checkNotNull(task));
        }

        /**
         * Adds and execute task in ListeningExecutorService
         *
         * @param task Runnable task which will be added to execution queue
         * @return ListenableFuture that can be used to extract result or cancel
         * @throws NullPointerException If task is null
         */
        public ListenableFuture<?> executeTask(Runnable task) {
                return executorService.submit(Preconditions.checkNotNull(task));
        }
}
