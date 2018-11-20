/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.controller.util.cluster;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.sxp.controller.boot.SxpControllerInstance;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.util.time.SxpTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: provides workaround for closing {@link ClusterSingletonService}, if {@link ClusterSingletonServiceProvider} does not close them
 * TODO remove when cluster will always close its instances when switching
 */
public final class ClusterSanityWatchdogInstance implements AutoCloseable, ClusterSingletonService {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterSanityWatchdogInstance.class);

    private final ListeningScheduledExecutorService scheduledExecutorService;
    private final FollowerSyncStatusTaskFactory taskFactory;
    private final AtomicBoolean isActive = new AtomicBoolean(false);
    private final DataBroker dataBroker;

    private DatastoreAccess datastoreAccess;
    private ClusterSingletonServiceRegistration clusterServiceRegistration;
    private ListenableFuture<Boolean> timer = Futures.immediateCancelledFuture();
    private ClusterSingletonService watchedService;

    /**
     * @param broker                          service providing access to Datastore
     * @param clusterSingletonServiceProvider service used for registration
     * @param period                          period of watchdog feed
     * @param failLimit                       acceptable missed watchdog feeds
     */
    public ClusterSanityWatchdogInstance(final DataBroker broker,
            final ClusterSingletonServiceProvider clusterSingletonServiceProvider, final int period,
            final int failLimit) {
        LOG.info("Cluster sanity watchdog initiating ..");
        this.dataBroker = Objects.requireNonNull(broker);
        this.scheduledExecutorService = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(1));
        taskFactory = new FollowerSyncStatusTaskFactory(period, failLimit);

        clusterServiceRegistration =
                Objects.requireNonNull(clusterSingletonServiceProvider).registerClusterSingletonService(this);
        LOG.info("Cluster sanity watchdog initiated");
    }

    @Override
    public synchronized void close() throws Exception {
        if (clusterServiceRegistration != null) {
            clusterServiceRegistration.close();
            clusterServiceRegistration = null;
        } else {
            return;
        }
        scheduledExecutorService.shutdown();
        datastoreAccess.close();
        timer.cancel(true);
    }

    /**
     * @param task containing logic that determines if cluster is healthy
     */
    private synchronized void schedule(final SxpTimerTask<Boolean> task) {
        if (!timer.isDone()) {
            LOG.warn("double scheduling occurred!");
            return;
        }

        timer = scheduledExecutorService.schedule(Objects.requireNonNull(task), task.getPeriod(), TimeUnit.SECONDS);
        Futures.addCallback(timer, new FutureCallback<Boolean>() {

            @Override
            public void onSuccess(@Nullable final Boolean clusterHealthy) {
                if (clusterHealthy != null && clusterHealthy) {
                    if (isActive.get()) {
                        LOG.debug("Regular rescheduling of cluster health status task");
                        schedule(task);
                    }
                } else {
                    LOG.info("Detected cluster isolation condition");
                    watchedService.closeServiceInstance();
                }
            }

            @Override
            public void onFailure(@Nullable final Throwable t) {
                LOG.info("Failed to track cluster health status", t);
                if (isActive.get() && !(t instanceof CancellationException)) {
                    LOG.debug("Rescheduling of cluster health status task after failure");
                    schedule(taskFactory.createFollowerSyncStatusTask(datastoreAccess));
                }
            }
        });
    }

    /**
     * Adds services that will be guarded by {@link ClusterSanityWatchdogInstance}
     *
     * @param service service that will be watched
     */
    public void setService(ClusterSingletonService service) {
        watchedService = service;
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        LOG.debug("Watched service woke up - firing scheduler");
        if (isActive.getAndSet(true)) {
            return;
        }
        datastoreAccess = DatastoreAccess.getInstance(dataBroker);
        schedule(taskFactory.createFollowerSyncStatusTask(datastoreAccess));
    }

    @Override
    public synchronized ListenableFuture<Void> closeServiceInstance() {
        LOG.debug("Watched service fall asleep - tearing down scheduler");
        datastoreAccess.close();
        timer.cancel(true);
        isActive.set(false);
        return Futures.immediateFuture(null);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return SxpControllerInstance.IDENTIFIER;
    }
}
