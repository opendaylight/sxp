/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.route.api.RouteReactor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRouteBuilder;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: update route change on system level, expect single thread involvement
 */
public class RouteReactorZipImpl implements RouteReactor {

    private static final Logger LOG = LoggerFactory.getLogger(RouteReactorZipImpl.class);

    private static final FluentFuture<? extends CommitInfo> COMPRESSED_FUTURE_RESULT = FluentFutures.immediateNullFluentFuture();

    private static final SxpClusterRoute WIPE_ROUTING_MARK = new SxpClusterRouteBuilder().build();

    private final ListeningExecutorService
            servicePool =
            MoreExecutors.listeningDecorator(ThreadsWorker.generateExecutor(1, "route-reactor"));

    private final ArrayBlockingQueue<MutablePair<SxpClusterRoute, SxpClusterRoute>> compressionQueue;
    private final Semaphore queueGuard;

    private final RouteReactor delegate;
    private final Callable<? extends CommitInfo> updateRouteTask;

    /**
     * @param delegate service used for delegation
     */
    public RouteReactorZipImpl(RouteReactor delegate) {
        this.delegate = Objects.requireNonNull(delegate);
        compressionQueue = new ArrayBlockingQueue<>(1, true);
        queueGuard = new Semaphore(1, true);
        updateRouteTask = createUpdateRoutingTask();
    }

    @Override
    public FluentFuture<? extends CommitInfo> updateRouting(@Nullable final SxpClusterRoute oldRoute,
                                                            @Nullable final SxpClusterRoute newRoute) {
        // with state compression
        FluentFuture<? extends CommitInfo> futureResult;
        try {
            queueGuard.acquire();

            if (compressionQueue.isEmpty()) {
                compressionQueue.add(new MutablePair<>(oldRoute, newRoute));
                // schedule task
                futureResult = FluentFuture.from(servicePool.submit(updateRouteTask));
            } else {
                // compress, expect that task is already scheduled
                compressionQueue.peek().setRight(newRoute);
                futureResult = COMPRESSED_FUTURE_RESULT;
                LOG.trace("route update request got state compressed - firing immediate future");
            }

            queueGuard.release();
        } catch (Exception e) {
            LOG.warn("failed to get lock upon compression queue: {}", e.getMessage());
            futureResult = FluentFutures.immediateFailedFluentFuture(e);
        }
        return futureResult;
    }

    /**
     * @return callback containing logic for mapping configuration into system routing
     */
    private Callable<? extends CommitInfo> createUpdateRoutingTask() {
        return () -> {
            try {
                queueGuard.acquire();

                final Pair<SxpClusterRoute, SxpClusterRoute> latestPair = compressionQueue.poll();
                final FluentFuture<? extends CommitInfo> futureResult;
                if (WIPE_ROUTING_MARK == latestPair.getRight()) {
                    futureResult = delegate.wipeRouting();
                } else {
                    futureResult = delegate.updateRouting(latestPair.getLeft(), latestPair.getRight());
                }

                queueGuard.release();

                futureResult.get(60, TimeUnit.SECONDS);
                LOG.debug("Route update was finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("failed to get lock upon compression queue: {}", e.getMessage());
            } catch (ExecutionException | TimeoutException e) {
                LOG.warn("failed to propagate route update: {}", e.getMessage());
            }

            return null;
        };
    }

    @Override
    public FluentFuture<? extends CommitInfo> wipeRouting() {
        FluentFuture<? extends CommitInfo> futureResult;
        try {
            queueGuard.acquire();

            if (compressionQueue.isEmpty()) {
                compressionQueue.add(new MutablePair<>(null, WIPE_ROUTING_MARK));
                // schedule task
                futureResult = FluentFuture.from(servicePool.submit(updateRouteTask));
            } else {
                // compress, expect that task is already scheduled
                compressionQueue.peek().setRight(WIPE_ROUTING_MARK);
                futureResult = COMPRESSED_FUTURE_RESULT;
                LOG.trace("route update request got state compressed - firing immediate future");
            }

            queueGuard.release();
        } catch (Exception e) {
            LOG.warn("failed to get lock upon compression queue: {}", e.getMessage());
            futureResult = FluentFutures.immediateFailedFluentFuture(e);
        }
        return futureResult;
    }
}
