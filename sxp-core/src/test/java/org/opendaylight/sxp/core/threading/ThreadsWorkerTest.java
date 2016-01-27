/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.threading;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class) public class ThreadsWorkerTest {

        private static ThreadsWorker worker;
        private static Callable callable;
        private static Runnable runnable;

        private static ListeningScheduledExecutorService scheduledExecutorService;
        private static ListeningExecutorService executorService, executorServiceOutbound, executorServiceInbound;

        @Before public void init() {
                callable = mock(Callable.class);
                runnable = mock(Runnable.class);

                scheduledExecutorService = mock(ListeningScheduledExecutorService.class);
                executorService = mock(ListeningExecutorService.class);
                executorServiceInbound = mock(ListeningExecutorService.class);
                executorServiceOutbound = mock(ListeningExecutorService.class);
                worker = new ThreadsWorker();
        }

        @Test public void testScheduleTask() throws Exception {
                worker.scheduleTask(callable, 0, TimeUnit.SECONDS);
                verify(scheduledExecutorService).schedule(any(Callable.class), anyInt(), any(TimeUnit.class));
        }

        @Test public void testExecuteTaskCallable() throws Exception {
                worker.executeTask(callable, ThreadsWorker.WorkerType.DEFAULT);
                verify(executorService).submit(any(Callable.class));

                worker.executeTask(callable, ThreadsWorker.WorkerType.OUTBOUND);
                verify(executorServiceOutbound).submit(any(Callable.class));

                worker.executeTask(callable, ThreadsWorker.WorkerType.INBOUND);
                verify(executorServiceInbound).submit(any(Callable.class));
        }

        @Test public void testExecuteTaskRunnable() throws Exception {
                worker.executeTask(runnable, ThreadsWorker.WorkerType.DEFAULT);
                verify(executorService).submit(any(Runnable.class));

                worker.executeTask(runnable, ThreadsWorker.WorkerType.OUTBOUND);
                verify(executorServiceOutbound).submit(any(Runnable.class));

                worker.executeTask(runnable, ThreadsWorker.WorkerType.INBOUND);
                verify(executorServiceInbound).submit(any(Runnable.class));
        }

        @Test public void testAddListener() throws Exception {
                ListenableFuture future = mock(ListenableFuture.class);
                worker.addListener(future, runnable);
                verify(future).addListener(runnable, executorService);
        }

        @Test public void testExecuteTaskInSequence() throws Exception {
                final int[] counter = {10};

            worker.addListener(worker.executeTaskInSequence(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    for (int i = 0; i < 500000; i++) {
                    }
                    counter[0] += 10;
                    System.out.println("DONE_1 " + counter[0]);
                    return null;
                }
            }, ThreadsWorker.WorkerType.DEFAULT), new Runnable() {
                @Override public void run() {
                    System.out.println("DONE_1_1 " + counter[0]);
                }
            });
            worker.addListener(worker.executeTaskInSequence(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    counter[0] *= 5;
                    while (counter[0] > 0) {

                    }
                    System.out.println("DONE_2 " + counter[0]);
                    return null;
                }
            }, ThreadsWorker.WorkerType.DEFAULT), new Runnable() {
                @Override public void run() {
                    System.out.println("DONE_2:2 " + counter[0]);
                }
            });

            worker.executeTaskInSequence(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    counter[0] -= 5;
                    System.out.println("DONE_3 " + counter[0]);
                    return null;
                }
            }, ThreadsWorker.WorkerType.DEFAULT);

            worker.executeTaskInSequence(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    counter[0] *= 2;
                    System.out.println("DONE_4 " + counter[0]);
                    return null;
                }
            }, ThreadsWorker.WorkerType.INBOUND);
            Thread.sleep(15000);

        }
}
