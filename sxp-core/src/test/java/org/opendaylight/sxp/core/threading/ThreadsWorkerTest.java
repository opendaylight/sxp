/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.threading;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.sxp.core.SxpConnection;

public class ThreadsWorkerTest {

    @Mock
    private Callable callable;
    @Mock
    private Runnable runnable;
    @Mock
    private SxpConnection connection;
    
    private ThreadsWorker worker;

    @Before
    public void init() {
        callable = mock(Callable.class);
        runnable = mock(Runnable.class);
        connection = mock(SxpConnection.class);
        worker = new ThreadsWorker();
    }

    @After
    public void tearDown() throws Exception {
        if (worker != null) {
            worker.close();
        }
    }

    @Test
    public void testScheduleTask() throws Exception {
        assertNotNull(worker.scheduleTask(callable, 0, TimeUnit.SECONDS));
    }

    @Test
    public void testExecuteTaskCallable() throws Exception {
        assertNotNull(worker.executeTask(callable, ThreadsWorker.WorkerType.DEFAULT));
        assertNotNull(worker.executeTask(callable, ThreadsWorker.WorkerType.OUTBOUND));
        assertNotNull(worker.executeTask(callable, ThreadsWorker.WorkerType.INBOUND));
    }

    @Test
    public void testExecuteTaskRunnable() throws Exception {
        assertNotNull(worker.executeTask(runnable, ThreadsWorker.WorkerType.DEFAULT));
        assertNotNull(worker.executeTask(runnable, ThreadsWorker.WorkerType.OUTBOUND));
        assertNotNull(worker.executeTask(runnable, ThreadsWorker.WorkerType.INBOUND));
    }

    @Test
    public void testAddListener() throws Exception {
        ListenableFuture future = mock(ListenableFuture.class);
        worker.addListener(future, runnable);
        verify(future).addListener(eq(runnable), any(ExecutorService.class));
    }

    @Test
    public void testExecuteTaskInSequence_Ordering() throws Exception {
        final CountDownLatch latch = new CountDownLatch(3);
        final int[] samples = {3, 10};

        worker.addListener(worker.executeTaskInSequence((Callable<Void>) () -> {
            samples[0] *= 5;
            return null;
        }, ThreadsWorker.WorkerType.DEFAULT), () -> {
            synchronized (samples) {
                samples[1] += 5;
            }
            latch.countDown();
        });

        worker.addListener(worker.executeTaskInSequence((Callable<Void>) () -> {
            samples[0] += 4;
            return null;
        }, ThreadsWorker.WorkerType.DEFAULT), () -> {
            synchronized (samples) {
                samples[1] += 4;
            }
            latch.countDown();
        });

        worker.addListener(worker.executeTaskInSequence((Callable<Void>) () -> {
            samples[0] *= 6;
            return null;
        }, ThreadsWorker.WorkerType.DEFAULT), () -> {
            synchronized (samples) {
                samples[1] += 6;
            }
            latch.countDown();
        });

        for (int i = 0; latch.getCount() != 0 && i < 10; i++) {
            latch.await(1, TimeUnit.SECONDS);
        }

        assertEquals(25, samples[1]);
        assertEquals(114, samples[0]);
    }

    @Test
    public void testExecuteTaskInSequence_Canceling() throws Exception {
        final int[] monitor = {0, 0};

        ListenableFuture future = worker.executeTaskInSequence((Callable<Void>) () -> {
            for (; monitor != null; ) {
            }
            return null;
        }, ThreadsWorker.WorkerType.OUTBOUND);

        ListenableFuture future_2 = worker.executeTaskInSequence((Callable<Void>) () -> {
            for (; monitor != null; ) {
            }
            return null;
        }, ThreadsWorker.WorkerType.OUTBOUND);

        worker.addListener(future_2, () -> {
            synchronized (monitor) {
                monitor[0] += 5;
                monitor.notifyAll();
            }
        });

        worker.executeTaskInSequence((Callable<Void>) () -> {
            synchronized (monitor) {
                monitor[1] += 40;
                monitor.notifyAll();
            }
            return null;
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

    @Test
    public void testExecuteTaskInSequence_Get() throws Exception {
        final int[] monitor = {0, 0};

        ListenableFuture future = worker.executeTaskInSequence((Callable<Void>) () -> {
            for (; monitor != null; ) {
            }
            return null;
        }, ThreadsWorker.WorkerType.INBOUND);

        ListenableFuture future_2 = worker.executeTaskInSequence((Callable<Void>) () -> {
            synchronized (monitor) {
                monitor[0] += 40;
                monitor.notifyAll();
            }
            return null;
        }, ThreadsWorker.WorkerType.INBOUND);

        worker.executeTaskInSequence((Callable<Void>) () -> {
            synchronized (monitor) {
                monitor[1] += 80;
                monitor.notifyAll();
            }
            return null;
        }, ThreadsWorker.WorkerType.INBOUND);
        future_2.get(500L, TimeUnit.MILLISECONDS);
        assertEquals(40, monitor[0]);

        future.cancel(true);
        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(80, monitor[1]);
    }

    @Test
    public void testCancelTasksInSequence() throws Exception {
        final int[] monitor = {0, 0};

        worker.executeTaskInSequence((Callable<Void>) () -> {
            for (; monitor != null; ) {
            }
            return null;
        }, ThreadsWorker.WorkerType.INBOUND);

        worker.executeTaskInSequence((Callable<Void>) () -> {
            synchronized (monitor) {
                monitor[0] += 40;
                monitor.notifyAll();
            }
            return null;
        }, ThreadsWorker.WorkerType.INBOUND);

        worker.cancelTasksInSequence(true, ThreadsWorker.WorkerType.INBOUND);
        assertEquals(0, monitor[0]);

        worker.executeTaskInSequence((Callable<Void>) () -> {
            synchronized (monitor) {
                monitor[1] += 80;
                monitor.notifyAll();
            }
            return null;
        }, ThreadsWorker.WorkerType.INBOUND);

        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(0, monitor[0]);
        assertEquals(80, monitor[1]);
    }

    @Test
    public void testExecuteTaskInSequence_Ordering_Connection() throws Exception {
        final CountDownLatch latch = new CountDownLatch(3);
        final int[] samples = {3, 10};

        worker.addListener(worker.executeTaskInSequence((Callable<Void>) () -> {
            samples[0] *= 5;
            return null;
        }, ThreadsWorker.WorkerType.DEFAULT, connection), () -> {
            synchronized (samples) {
                samples[1] += 5;
            }
            latch.countDown();
        });

        worker.addListener(worker.executeTaskInSequence((Callable<Void>) () -> {
            samples[0] += 4;
            return null;
        }, ThreadsWorker.WorkerType.DEFAULT, connection), () -> {
            synchronized (samples) {
                samples[1] += 4;
            }
            latch.countDown();
        });

        worker.addListener(worker.executeTaskInSequence((Callable<Void>) () -> {
            samples[0] *= 6;
            return null;
        }, ThreadsWorker.WorkerType.DEFAULT, connection), () -> {
            synchronized (samples) {
                samples[1] += 6;
            }
            latch.countDown();
        });

        for (int i = 0; latch.getCount() != 0 && i < 10; i++) {
            latch.await(1L, TimeUnit.SECONDS);
        }

        assertEquals(114, samples[0]);
        assertEquals(25, samples[1]);
    }

    @Test
    public void testExecuteTaskInSequence_Canceling_Connection() throws Exception {
        final int[] monitor = {0, 0};

        ListenableFuture future = worker.executeTaskInSequence((Callable<Void>) () -> {
            for (; monitor != null; ) {
            }
            return null;
        }, ThreadsWorker.WorkerType.OUTBOUND, connection);

        ListenableFuture future_2 = worker.executeTaskInSequence((Callable<Void>) () -> {
            for (; monitor != null; ) {
            }
            return null;
        }, ThreadsWorker.WorkerType.OUTBOUND, connection);

        worker.addListener(future_2, () -> {
            synchronized (monitor) {
                monitor[0] += 5;
                monitor.notifyAll();
            }
        });

        worker.executeTaskInSequence((Callable<Void>) () -> {
            synchronized (monitor) {
                monitor[1] += 40;
                monitor.notifyAll();
            }
            return null;
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

    @Test
    public void testExecuteTaskInSequence_Get_Connection() throws Exception {
        final int[] monitor = {0, 0};

        ListenableFuture future = worker.executeTaskInSequence((Callable<Void>) () -> {
            for (; monitor != null; ) {
            }
            return null;
        }, ThreadsWorker.WorkerType.INBOUND, connection);

        ListenableFuture future_2 = worker.executeTaskInSequence((Callable<Void>) () -> {
            synchronized (monitor) {
                monitor[0] += 40;
                monitor.notifyAll();
            }
            return null;
        }, ThreadsWorker.WorkerType.INBOUND, connection);

        worker.executeTaskInSequence((Callable<Void>) () -> {
            synchronized (monitor) {
                monitor[1] += 80;
                monitor.notifyAll();
            }
            return null;
        }, ThreadsWorker.WorkerType.INBOUND, connection);
        future_2.get(500L, TimeUnit.MILLISECONDS);
        assertEquals(40, monitor[0]);

        future.cancel(true);
        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(80, monitor[1]);
    }

    @Test
    public void testCancelTasksInSequence_Connection() throws Exception {
        final int[] monitor = {0, 0};

        worker.executeTaskInSequence((Callable<Void>) () -> {
            for (; monitor != null; ) {
            }
            return null;
        }, ThreadsWorker.WorkerType.INBOUND, connection);

        worker.executeTaskInSequence((Callable<Void>) () -> {
            synchronized (monitor) {
                monitor[0] += 40;
                monitor.notifyAll();
            }
            return null;
        }, ThreadsWorker.WorkerType.INBOUND, connection);

        worker.cancelTasksInSequence(true, ThreadsWorker.WorkerType.INBOUND, connection);
        assertEquals(0, monitor[0]);

        worker.executeTaskInSequence((Callable<Void>) () -> {
            synchronized (monitor) {
                monitor[1] += 80;
                monitor.notifyAll();
            }
            return null;
        }, ThreadsWorker.WorkerType.INBOUND, connection);

        synchronized (monitor) {
            monitor.wait(5000L);
        }
        assertEquals(0, monitor[0]);
        assertEquals(80, monitor[1]);
    }

    @Test
    public void testGenerateDefaultExecutor() {
        ExecutorService generatedExecutor = ThreadsWorker.generateExecutor(1, null);
        assertNotNull(generatedExecutor);
    }

    @Test
    public void testGenerateDefaultScheduledExecutor() {
        ExecutorService generatedExecutor = ThreadsWorker.generateScheduledExecutor(1, null);
        assertNotNull(generatedExecutor);
    }
}
