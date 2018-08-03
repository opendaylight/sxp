/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.threading;

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 * @author Martin Dindoffer
 */
@RunWith(PowerMockRunner.class)
public class SettableListenableFutureTest {

    private ListeningExecutorService listeningExecService;
    private SettableListenableFuture<Object> slFuture;

    @Mock
    private ListenableFuture<Object> lfMock;

    @Before
    public void init() {
        this.listeningExecService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        this.slFuture = new SettableListenableFuture<>(() -> new Object(), listeningExecService);
    }

    @Test
    public void testMultipleSetFuture() {
        slFuture.setFuture(lfMock);
        slFuture.setFuture(lfMock);
    }

    @Test
    public void testIsCancelled() {
        Assert.assertTrue(!slFuture.isCancelled());
        slFuture.setFuture(lfMock);
        Assert.assertTrue(!slFuture.isCancelled());
        when(lfMock.isCancelled()).thenReturn(true);
        Assert.assertTrue(slFuture.isCancelled());
    }

    @Test
    public void testIsDone() {
        Assert.assertTrue(!slFuture.isDone());
        slFuture.setFuture(lfMock);
        Assert.assertTrue(!slFuture.isDone());
        when(lfMock.isDone()).thenReturn(true);
        Assert.assertTrue(slFuture.isDone());
    }

    @Test
    public void testCancelOnNullFuture() {
        Assert.assertTrue(slFuture.cancel(true));
        Assert.assertTrue(slFuture.cancel(false));
    }

    @Test
    public void testCancel() {
        slFuture.setFuture(lfMock);
        Assert.assertTrue(!slFuture.cancel(true));
        Assert.assertTrue(!slFuture.cancel(false));
    }

    @Test
    public void testGetOnSetFuture() throws Exception {
        slFuture.setFuture(lfMock);
        Object response = new Object();
        when(lfMock.get()).thenReturn(response);
        Assert.assertEquals(response, slFuture.get());
    }

    @Test
    public void testGetOnNullCancelledFuture() throws Exception {
        slFuture.cancel(true);
        Object result = slFuture.get();
        Assert.assertNull(result);
    }

    @Test
    public void testGetOnNullFuture() throws Exception {
        slFuture.addListener(() -> new Object(), listeningExecService);
        Object result = slFuture.get();
        Assert.assertNotNull(result);
    }

    @Test
    public void testGetOnSetFutureWithTimeout() throws Exception {
        slFuture.setFuture(lfMock);
        Object response = new Object();
        when(lfMock.get(Matchers.anyLong(), any())).thenReturn(response);
        Assert.assertEquals(response, slFuture.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testGetOnNullCancelledFutureWithTimeout() throws Exception {
        slFuture.cancel(true);
        Object result = slFuture.get(1, TimeUnit.SECONDS);
        Assert.assertNull(result);
    }

    @Test
    public void testGetOnNullFutureWithTimeout() throws Exception {
        slFuture.addListener(() -> new Object(), listeningExecService);
        Object result = slFuture.get(1, TimeUnit.SECONDS);
        Assert.assertNotNull(result);
    }

}
