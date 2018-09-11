/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.sxp.route.api.RouteReactor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRouteBuilder;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;

/**
 * Test for {@link RouteReactorZipImpl}.
 */
public class RouteReactorZipImplTest {

    @Mock private RouteReactor delegate;

    private SxpClusterRoute route1;
    private SxpClusterRoute route2;
    private SxpClusterRoute route3;
    private SxpClusterRoute route4;

    private RouteReactorZipImpl reactor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        route1 =
                new SxpClusterRouteBuilder().setRoutingDefinition(
                        Collections.singletonList(RouteTestFactory.createDummyRoutingDef(1, 1))).build();
        route2 =
                new SxpClusterRouteBuilder().setRoutingDefinition(
                        Collections.singletonList(RouteTestFactory.createDummyRoutingDef(2, 2))).build();

        route3 =
                new SxpClusterRouteBuilder().setRoutingDefinition(
                        Collections.singletonList(RouteTestFactory.createDummyRoutingDef(3, 3))).build();
        route4 =
                new SxpClusterRouteBuilder().setRoutingDefinition(
                        Collections.singletonList(RouteTestFactory.createDummyRoutingDef(4, 4))).build();

        reactor = new RouteReactorZipImpl(delegate);
    }

    @Test
    public void updateRouting_simple() throws Exception {
        Mockito.when(delegate.updateRouting(route1, route2)).thenReturn(FluentFutures.immediateNullFluentFuture());

        reactor.updateRouting(route1, route2).get(1, TimeUnit.SECONDS);

        Mockito.verify(delegate).updateRouting(route1, route2);
    }

    @Test
    public void updateRouting_compression() throws Exception {
        final CountDownLatch firstUpdateLatch = new CountDownLatch(1);
        Mockito.when(delegate.updateRouting(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .then((Answer<FluentFuture<? extends CommitInfo>>) invocationOnMock -> {
                    firstUpdateLatch.countDown();
                    return FluentFutures.immediateNullFluentFuture();
                });

        // fire first change and block later tasks by unfinished outcome + countdown the latch
        final FluentFuture<? extends CommitInfo> outcome1 = reactor.updateRouting(route1, route2);
        firstUpdateLatch.await(1, TimeUnit.SECONDS);

        final FluentFuture<? extends CommitInfo> outcome2 = reactor.updateRouting(route2, route3);
        final FluentFuture<? extends CommitInfo> outcome3 = reactor.updateRouting(route3, route4);

        // expect fast response because only compression got involved
        outcome3.get(1, TimeUnit.SECONDS);
        // expect fast response because it just got unblocked
        outcome1.get(1, TimeUnit.SECONDS);
        // expect slower response - update task started after the unblock of 1
        outcome2.get(5, TimeUnit.SECONDS);

        final InOrder inOrder = Mockito.inOrder(delegate);
        inOrder.verify(delegate).updateRouting(route1, route2);
        inOrder.verify(delegate).updateRouting(route2, route4);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void wipeRouting_simple() throws Exception {
        Mockito.when(delegate.wipeRouting()).thenReturn(FluentFutures.immediateNullFluentFuture());

        reactor.wipeRouting().get(1, TimeUnit.SECONDS);

        Mockito.verify(delegate).wipeRouting();
    }

    @Test
    public void wipeRouting_compression() throws Exception {
        final SettableFuture<Void> updateTask1Outcome = SettableFuture.create();
        final CountDownLatch firstUpdateLatch = new CountDownLatch(1);

        Mockito.when(delegate.updateRouting(ArgumentMatchers.any(), ArgumentMatchers.any())).then(new Answer<ListenableFuture<Void>>() {

            @Override
            public ListenableFuture<Void> answer(final InvocationOnMock invocationOnMock) throws Throwable {
                firstUpdateLatch.countDown();
                return updateTask1Outcome;
            }
        });
        Mockito.when(delegate.wipeRouting()).thenReturn(FluentFutures.immediateNullFluentFuture());

        // fire first change and block later tasks by unfinished outcome + countdown the latch
        final FluentFuture<? extends CommitInfo> outcome1 = reactor.updateRouting(route1, route2);
        firstUpdateLatch.await(1, TimeUnit.SECONDS);

        final FluentFuture<? extends CommitInfo> outcome2 = reactor.updateRouting(route2, route3);
        final FluentFuture<? extends CommitInfo> outcome3 = reactor.wipeRouting();

        // expect fast response because only compression got involved
        outcome3.get(1, TimeUnit.SECONDS);
        // unblock result of the first update task
        updateTask1Outcome.set(null);
        // expect fast response because it just got unblocked
        outcome1.get(1, TimeUnit.SECONDS);
        // expect slower response - update task started after the unblock of 1
        outcome2.get(5, TimeUnit.SECONDS);

        final InOrder inOrder = Mockito.inOrder(delegate);
        inOrder.verify(delegate).updateRouting(route1, route2);
        inOrder.verify(delegate).wipeRouting();
        inOrder.verifyNoMoreInteractions();
    }
}
