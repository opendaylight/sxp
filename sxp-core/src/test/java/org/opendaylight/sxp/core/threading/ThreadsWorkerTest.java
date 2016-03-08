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
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.core.SxpConnection;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class) @PrepareForTest({MoreExecutors.class}) public class ThreadsWorkerTest {

    private static ThreadsWorker worker;
    private static Callable callable;
    private static Runnable runnable;
    private static SxpConnection connection;

    private static ListeningScheduledExecutorService scheduledExecutorService;
    private static ListeningExecutorService executorService, executorServiceOutbound, executorServiceInbound;

    @Before public void init() {
        callable = mock(Callable.class);
        runnable = mock(Runnable.class);

        connection = mock(SxpConnection.class);
        scheduledExecutorService = mock(ListeningScheduledExecutorService.class);
        executorService = mock(ListeningExecutorService.class);
        executorServiceInbound = mock(ListeningExecutorService.class);
        executorServiceOutbound = mock(ListeningExecutorService.class);
        PowerMockito.mockStatic(MoreExecutors.class);
        PowerMockito.when(MoreExecutors.listeningDecorator(any(AbstractExecutorService.class)))
                .thenReturn(executorService, executorServiceInbound, executorServiceOutbound);
        PowerMockito.when(MoreExecutors.listeningDecorator(any(ScheduledExecutorService.class)))
                .thenReturn(scheduledExecutorService);
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

    @Test public void testExecuteTaskInSequence_Ordering() throws Exception {
        PowerMockito.when(MoreExecutors.listeningDecorator(any(AbstractExecutorService.class))).thenCallRealMethod();
        PowerMockito.when(MoreExecutors.listeningDecorator(any(ScheduledExecutorService.class))).thenCallRealMethod();
        worker = new ThreadsWorker();
        final int[] monitor = {3, 10};

        worker.addListener(worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                for (int i = 0; i < 5000; i++) {
                }
                monitor[0] *= 5;
                return null;
            }
        }, ThreadsWorker.WorkerType.DEFAULT), new Runnable() {

            @Override public void run() {
                monitor[1] += 5;
            }
        });

        worker.addListener(worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                monitor[0] += 4;
                return null;
            }
        }, ThreadsWorker.WorkerType.DEFAULT), new Runnable() {

            @Override public void run() {
                monitor[1] += 4;
            }
        });

        worker.addListener(worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                for (int i = 0; i < 5000; i++) {
                }
                monitor[0] *= 6;
                return null;
            }
        }, ThreadsWorker.WorkerType.DEFAULT), new Runnable() {

            @Override public void run() {
                monitor[1] += 6;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
            }
        });

        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(114, monitor[0]);
        assertEquals(25, monitor[1]);
    }

    @Test public void testExecuteTaskInSequence_Canceling() throws Exception {
        PowerMockito.when(MoreExecutors.listeningDecorator(any(AbstractExecutorService.class))).thenCallRealMethod();
        worker = new ThreadsWorker();

        final int[] monitor = {0, 0};

        ListenableFuture future = worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                for (; monitor != null; ) {
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.OUTBOUND);

        ListenableFuture future_2 = worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                for (; monitor != null; ) {
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.OUTBOUND);

        worker.addListener(future_2, new Runnable() {

            @Override public void run() {
                monitor[0] += 5;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
            }
        });

        worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                monitor[1] += 40;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.OUTBOUND);

        future_2.cancel(true);
        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(5, monitor[0]);
        future.cancel(true);

        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(40, monitor[1]);
    }

    @Test public void testExecuteTaskInSequence_Get() throws Exception {
        PowerMockito.when(MoreExecutors.listeningDecorator(any(AbstractExecutorService.class))).thenCallRealMethod();
        worker = new ThreadsWorker();

        final int[] monitor = {0, 0};

        ListenableFuture future = worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                for (; monitor != null; ) {
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.INBOUND);

        ListenableFuture future_2 = worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                monitor[0] += 40;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.INBOUND);

        worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                monitor[1] += 80;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.INBOUND);
        future_2.get(1L, TimeUnit.SECONDS);
        future_2.get();
        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(40, monitor[0]);

        future.cancel(true);
        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(80, monitor[1]);
    }

    @Test public void testCancelTasksInSequence() throws Exception {
        PowerMockito.when(MoreExecutors.listeningDecorator(any(AbstractExecutorService.class))).thenCallRealMethod();
        worker = new ThreadsWorker();

        final int[] monitor = {0, 0};

        worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                for (; monitor != null; ) {
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.INBOUND);

        worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                monitor[0] += 40;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.INBOUND);

        worker.cancelTasksInSequence(true, ThreadsWorker.WorkerType.INBOUND);
        assertEquals(0, monitor[0]);

        worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                monitor[1] += 80;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.INBOUND);

        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(0, monitor[0]);
        assertEquals(80, monitor[1]);
    }

    @Test public void testExecuteTaskInSequence_Ordering_Connection() throws Exception {
        PowerMockito.when(MoreExecutors.listeningDecorator(any(AbstractExecutorService.class))).thenCallRealMethod();
        PowerMockito.when(MoreExecutors.listeningDecorator(any(ScheduledExecutorService.class))).thenCallRealMethod();
        worker = new ThreadsWorker();
        final int[] monitor = {3, 10};

        worker.addListener(worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                for (int i = 0; i < 5000; i++) {
                }
                monitor[0] *= 5;
                return null;
            }
        }, ThreadsWorker.WorkerType.DEFAULT, connection), new Runnable() {

            @Override public void run() {
                monitor[1] += 5;
            }
        });

        worker.addListener(worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                monitor[0] += 4;
                return null;
            }
        }, ThreadsWorker.WorkerType.DEFAULT, connection), new Runnable() {

            @Override public void run() {
                monitor[1] += 4;
            }
        });

        worker.addListener(worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                for (int i = 0; i < 5000; i++) {
                }
                monitor[0] *= 6;
                return null;
            }
        }, ThreadsWorker.WorkerType.DEFAULT, connection), new Runnable() {

            @Override public void run() {
                monitor[1] += 6;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
            }
        });

        synchronized (monitor) {
            monitor.wait(10000L);
        }
        assertEquals(114, monitor[0]);
        assertEquals(25, monitor[1]);
    }

    @Test public void testExecuteTaskInSequence_Canceling_Connection() throws Exception {
        PowerMockito.when(MoreExecutors.listeningDecorator(any(AbstractExecutorService.class))).thenCallRealMethod();
        worker = new ThreadsWorker();

        final int[] monitor = {0, 0};

        ListenableFuture future = worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                for (; monitor != null; ) {
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.OUTBOUND, connection);

        ListenableFuture future_2 = worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                for (; monitor != null; ) {
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.OUTBOUND, connection);

        worker.addListener(future_2, new Runnable() {

            @Override public void run() {
                monitor[0] += 5;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
            }
        });

        worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                monitor[1] += 40;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.OUTBOUND, connection);

        future_2.cancel(true);
        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(5, monitor[0]);
        future.cancel(true);

        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(40, monitor[1]);
    }

    @Test public void testExecuteTaskInSequence_Get_Connection() throws Exception {
        PowerMockito.when(MoreExecutors.listeningDecorator(any(AbstractExecutorService.class))).thenCallRealMethod();
        worker = new ThreadsWorker();

        final int[] monitor = {0, 0};

        ListenableFuture future = worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                for (; monitor != null; ) {
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.INBOUND, connection);

        ListenableFuture future_2 = worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                monitor[0] += 40;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.INBOUND, connection);

        worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                monitor[1] += 80;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.INBOUND, connection);
        future_2.get();
        future_2.get(1L, TimeUnit.SECONDS);
        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(40, monitor[0]);

        future.cancel(true);
        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(80, monitor[1]);
    }

    @Test public void testCancelTasksInSequence_Connection() throws Exception {
        PowerMockito.when(MoreExecutors.listeningDecorator(any(AbstractExecutorService.class))).thenCallRealMethod();
        worker = new ThreadsWorker();

        final int[] monitor = {0, 0};

        worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                for (; monitor != null; ) {
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.INBOUND, connection);

        worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                monitor[0] += 40;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.INBOUND, connection);

        worker.cancelTasksInSequence(true, ThreadsWorker.WorkerType.INBOUND, connection);
        assertEquals(0, monitor[0]);

        worker.executeTaskInSequence(new Callable<Void>() {

            @Override public Void call() throws Exception {
                monitor[1] += 80;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
                return null;
            }
        }, ThreadsWorker.WorkerType.INBOUND, connection);

        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(0, monitor[0]);
        assertEquals(80, monitor[1]);
    }
}
