/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.sxp.controller.boot.SxpControllerInstance;
import org.opendaylight.sxp.route.api.RouteReactor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRoute;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: listen to changes in sxp cluster route configuration and delegate route update task
 */
public class SxpClusterRouteManager
        implements ClusteredDataTreeChangeListener<SxpClusterRoute>, ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SxpClusterRouteManager.class);
    private static final FutureCallback<CommitInfo> LOGGING_CALLBACK = new FutureCallback<CommitInfo>() {

        @Override
        public void onSuccess(final CommitInfo result) {
            LOG.debug("Finished sxp-cluster-routing update task");
        }

        @Override
        public void onFailure(@Nullable final Throwable t) {
            LOG.warn("Finished sxp-cluster-routing update task", t);
        }
    };
    private static final FutureCallback<CommitInfo> LOGGING_CALLBACK_CLOSE = new FutureCallback<CommitInfo>() {

        @Override
        public void onSuccess(final CommitInfo result) {
            LOG.debug("IN_CLOSE: Finished sxp-cluster-routing update task");
        }

        @Override
        public void onFailure(@Nullable final Throwable t) {
            LOG.warn("IN_CLOSE: Finished sxp-cluster-routing update task", t);
        }
    };

    static final InstanceIdentifier<SxpClusterRoute>
            SXP_CLUSTER_ROUTE_CONFIG_PATH =
            InstanceIdentifier.create(SxpClusterRoute.class);
    static final DataTreeIdentifier<SxpClusterRoute>
            ROUTING_DEFINITION_DT_IDENTIFIER =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, SXP_CLUSTER_ROUTE_CONFIG_PATH);

    @GuardedBy("stateLock") private RouteListenerState state;
    private final Object stateLock = new Object();

    private final DataBroker dataBroker;
    private final ClusterSingletonServiceProvider cssProvider;
    private final RouteReactor routeReactor;
    private ClusterSingletonServiceRegistration cssRegistration;
    private ListenerRegistration<SxpClusterRouteManager> routingDefinitionListenerRegistration;

    /**
     * @param dataBroker   service providing access to Datastore
     * @param cssProvider  service used for registration
     * @param routeReactor service providing routing logic
     */
    public SxpClusterRouteManager(final DataBroker dataBroker, final ClusterSingletonServiceProvider cssProvider,
            final RouteReactor routeReactor) {
        this.dataBroker = Objects.requireNonNull(dataBroker);
        this.cssProvider = Objects.requireNonNull(cssProvider);
        this.routeReactor = Objects.requireNonNull(routeReactor);
        state = RouteListenerState.STOPPED;
    }

    /**
     * Registers {@link ClusterSingletonService} into {@link ClusterSingletonServiceProvider}
     */
    public void init() {
        LOG.debug("Registering css for: {}", getClass().getSimpleName());
        cssRegistration = cssProvider.registerClusterSingletonService(this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<SxpClusterRoute>> changes) {
        if (!state.isProcessing()) {
            return;
        }

        synchronized (stateLock) {
            if (!state.isProcessing()) {
                LOG.info("Surprise: data changed notification obtained outside listening time frame");
                return;
            }

            changes.forEach(change -> {
                if (RouteListenerState.BEFORE_FIRST.equals(state) && change.getRootNode().getDataBefore() != null) {
                    LOG.warn("first notification after lister registered contains BEFORE");
                }

                final DataObjectModification<SxpClusterRoute> rootNode = change.getRootNode();
                final SxpClusterRoute dataBefore = rootNode.getDataBefore();
                final SxpClusterRoute dataAfter = rootNode.getDataAfter();

                final FluentFuture<? extends CommitInfo> routingOutcome = routeReactor.updateRouting(dataBefore, dataAfter);
                Futures.addCallback(routingOutcome, LOGGING_CALLBACK);
            });

            state = RouteListenerState.WORKING;
        }
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return SxpControllerInstance.IDENTIFIER;
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());

        synchronized (stateLock) {
            state = RouteListenerState.BEFORE_FIRST;
            routingDefinitionListenerRegistration =
                    dataBroker.registerDataTreeChangeListener(ROUTING_DEFINITION_DT_IDENTIFIER, this);
        }
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Clustering provider closed service for {}", this.getClass().getSimpleName());
        final FluentFuture<? extends CommitInfo> routingOutcome;
        synchronized (stateLock) {
            state = RouteListenerState.STOPPED;
            routingDefinitionListenerRegistration.close();
            routingOutcome = routeReactor.wipeRouting();
            Futures.addCallback(routingOutcome, LOGGING_CALLBACK);
        }
        final SettableFuture<Void> outputFuture = SettableFuture.create();
        Futures.addCallback(routingOutcome, new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(CommitInfo result) {
                outputFuture.set(null);
            }
            @Override
            public void onFailure(Throwable t) {
                outputFuture.setException(t);
            }
        });
        return outputFuture;
    }

    @Override
    public void close() throws Exception {
        cssRegistration.close();
        LOG.info("definitely closing instance and scheduling wipe-route task");
        // teardown routing - just to be sure (will be skipped if listening is closed via state)
        synchronized (stateLock) {
            state = RouteListenerState.STOPPED;
            routingDefinitionListenerRegistration.close();
            final FluentFuture<? extends CommitInfo> routingOutcome = routeReactor.wipeRouting();
            Futures.addCallback(routingOutcome, LOGGING_CALLBACK_CLOSE);
        }
    }

    /**
     * @return current listener state
     */
    @VisibleForTesting
    RouteListenerState getState() {
        return state;
    }
}
