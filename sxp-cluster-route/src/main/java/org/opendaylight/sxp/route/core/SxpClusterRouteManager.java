/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.config.yang.sxp.controller.conf.SxpControllerInstance;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.route.spi.Routing;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinitionKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: listen to changes in sxp cluster route configuration
 */
public class SxpClusterRouteManager
        implements ClusteredDataTreeChangeListener<SxpClusterRoute>, ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SxpClusterRouteManager.class);

    static final InstanceIdentifier<SxpClusterRoute>
            SXP_CLUSTER_ROUTE_CONFIG_PATH =
            InstanceIdentifier.create(SxpClusterRoute.class);
    static final DataTreeIdentifier<SxpClusterRoute>
            ROUTING_DEFINITION_DT_IDENTIFIER =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, SXP_CLUSTER_ROUTE_CONFIG_PATH);

    private final DataBroker dataBroker;
    private final ClusterSingletonServiceProvider cssProvider;
    private ClusterSingletonServiceRegistration cssRegistration;
    private List<ListenerRegistration<SxpClusterRouteManager>>
            routingDefinitionListenerRegistration =
            new ArrayList<>();
    private DatastoreAccess datastoreAccess;

    private final ListeningExecutorService
            executorService =
            MoreExecutors.listeningDecorator(ThreadsWorker.generateExecutor(1));
    private final Map<IpAddress, Routing> routingMap = Collections.synchronizedMap(new HashMap<>());
    private final AtomicReference<SxpClusterRoute> oldConfig = new AtomicReference<>(null),
            newConfig =
                    new AtomicReference<>(null);

    public SxpClusterRouteManager(final DataBroker dataBroker, final ClusterSingletonServiceProvider cssProvider) {
        this.dataBroker = Objects.requireNonNull(dataBroker);
        this.cssProvider = Objects.requireNonNull(cssProvider);
    }

    public void init() {
        cssRegistration = cssProvider.registerClusterSingletonService(this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<SxpClusterRoute>> changes) {
        changes.stream()
                .map(DataTreeModification::getRootNode)
                .map(DataObjectModification::getDataAfter)
                .forEach((dataAfter) -> LOG.debug("data-after: {}", dataAfter));

        changes.forEach(c -> {
            if (Objects.isNull(oldConfig.get())) {
                oldConfig.set(c.getRootNode().getDataBefore());
            }
            if (Objects.isNull(newConfig.getAndSet(Objects.isNull(
                    c.getRootNode().getDataAfter()) ? new SxpClusterRouteBuilder().build() : c.getRootNode()
                    .getDataAfter()))) {
                executorService.submit(this::updateRoutes);
            }
        });
    }

    private void updateRoutes() {
        final SxpClusterRoute before = oldConfig.get(), after = newConfig.getAndSet(null);
        final Map<IpAddress, RoutingDefinition> oldDefinitions = new HashMap<>(), newDefinitions = new HashMap<>();

        if (Objects.nonNull(before) && Objects.nonNull(before.getRoutingDefinition()) && !before.getRoutingDefinition()
                .isEmpty()) {
            before.getRoutingDefinition().forEach(r -> oldDefinitions.put(r.getIpAddress(), r));
        }
        if (Objects.nonNull(after) && Objects.nonNull(after.getRoutingDefinition()) && !after.getRoutingDefinition()
                .isEmpty()) {
            after.getRoutingDefinition().forEach(r -> newDefinitions.put(r.getIpAddress(), r));
        }

        final MapDifference<IpAddress, RoutingDefinition>
                routingDifference =
                Maps.difference(oldDefinitions, newDefinitions);

        // remove all deleted or updated definitions
        routingDifference.entriesOnlyOnLeft().keySet().forEach((k) -> {
            if (routingMap.containsKey(k)) {
                findSxpNodesOnVirtualIp(k).forEach(SxpNode::shutdown);
                if (routingMap.get(k).removeRouteForCurrentService()) {
                    datastoreAccess.checkAndDelete(
                            SXP_CLUSTER_ROUTE_CONFIG_PATH.child(RoutingDefinition.class, new RoutingDefinitionKey(k)),
                            LogicalDatastoreType.OPERATIONAL);
                } else {
                    LOG.error("Route {} cannot be closed.", routingMap.get(k));
                }
                routingMap.remove(k);
            }
        });
        routingDifference.entriesDiffering().forEach((k, d) -> {
            if (routingMap.containsKey(k)) {
                final Routing route = routingMap.get(k);
                findSxpNodesOnVirtualIp(k).forEach(SxpNode::shutdown);
                if (route.removeRouteForCurrentService()) {
                    route.setNetmask(d.rightValue().getNetmask()).setInterface(d.rightValue().getInterface());
                } else {
                    LOG.error("Route {} cannot be closed.", route);
                    findSxpNodesOnVirtualIp(k).forEach(SxpNode::start);
                }
            }
        });
        // add all updated and new definitions
        routingDifference.entriesDiffering().forEach((k, d) -> {
            if (routingMap.containsKey(k)) {
                final Routing route = routingMap.get(k);
                if ((!route.getInterface().equals(d.rightValue().getInterface()) || route.getNetmask()
                        .equals(d.rightValue().getNetmask())) && route.addRouteForCurrentService()) {
                    findSxpNodesOnVirtualIp(k).forEach(SxpNode::start);
                    datastoreAccess.merge(
                            SXP_CLUSTER_ROUTE_CONFIG_PATH.child(RoutingDefinition.class, new RoutingDefinitionKey(k)),
                            new RoutingDefinitionBuilder(d.rightValue()).setTimestamp(
                                    TimeConv.toDt(System.currentTimeMillis())).build(),
                            LogicalDatastoreType.OPERATIONAL);
                } else {
                    LOG.error("Route {} cannot be created", route);
                }
            }
        });
        routingDifference.entriesOnlyOnRight().forEach((k, d) -> {
            final Routing route = RoutingServiceFactory.instantiateRoutingService(d);
            if (routingMap.containsKey(k)) {
                LOG.warn("Found unexpected route {} closing it.", routingMap.get(k));
                findSxpNodesOnVirtualIp(k).forEach(SxpNode::shutdown);
                if (!routingMap.get(k).removeRouteForCurrentService()) {
                    LOG.error("Route {} cannot be closed.", routingMap.get(k));
                }
            }
            routingMap.put(k, route);
            if (route.addRouteForCurrentService()) {
                findSxpNodesOnVirtualIp(k).forEach(SxpNode::start);
                datastoreAccess.put(
                        SXP_CLUSTER_ROUTE_CONFIG_PATH.child(RoutingDefinition.class, new RoutingDefinitionKey(k)),
                        new RoutingDefinitionBuilder(d).setTimestamp(TimeConv.toDt(System.currentTimeMillis())).build(),
                        LogicalDatastoreType.OPERATIONAL);
            } else {
                LOG.error("Route {} cannot be created", route);
            }
        });
        oldConfig.set(after);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return SxpControllerInstance.IDENTIFIER;
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.warn("Instantiating {}", this.getClass().getSimpleName());
        this.datastoreAccess = DatastoreAccess.getInstance(dataBroker);

        routingDefinitionListenerRegistration.add(
                dataBroker.registerDataTreeChangeListener(ROUTING_DEFINITION_DT_IDENTIFIER, this));
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.warn("Clustering provider closed service for {}", this.getClass().getSimpleName());
        this.datastoreAccess.close();
        routingDefinitionListenerRegistration.forEach(ListenerRegistration::close);
        routingDefinitionListenerRegistration.clear();
        routingMap.values().forEach(Routing::removeRouteForCurrentService);
        routingMap.clear();
        return Futures.immediateFuture(null);
    }

    @Override
    public void close() throws Exception {
        this.datastoreAccess.close();
        cssRegistration.close();
        // teardown routing - just to be sure
        routingDefinitionListenerRegistration.forEach(ListenerRegistration::close);
        routingDefinitionListenerRegistration.clear();
        routingMap.values().forEach(Routing::removeRouteForCurrentService);
        routingMap.clear();
    }

    public static String addressToString(IpAddress address) {
        return new String(Objects.requireNonNull(address).getValue());
    }

    private static Collection<SxpNode> findSxpNodesOnVirtualIp(final IpAddress virtualIp) {
        final Collection<SxpNode> affectedNodes;
        if (Objects.isNull(virtualIp)) {
            affectedNodes = Collections.emptyList();
        } else {
            affectedNodes =
                    Configuration.getNodes()
                            .stream()
                            .filter(n -> Objects.nonNull(n) && n.getSourceIp()
                                    .getHostAddress()
                                    .equals(addressToString(virtualIp)))
                            .collect(Collectors.toList());
        }
        return affectedNodes;
    }
}
