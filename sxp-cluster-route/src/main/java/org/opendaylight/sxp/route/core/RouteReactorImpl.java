/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.route.api.RouteReactor;
import org.opendaylight.sxp.route.spi.Routing;
import org.opendaylight.sxp.route.util.RouteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: update route change on system level, expect single thread involvement
 */
public class RouteReactorImpl implements RouteReactor {

    private static final Logger LOG = LoggerFactory.getLogger(RouteReactorImpl.class);

    private final ConcurrentMap<IpAddress, Routing> routingServiceMap = new ConcurrentHashMap<>();

    private final DatastoreAccess datastoreAccess;
    private final RoutingServiceFactory routingServiceFactory;

    /**
     * @param dataBroker            service providing access to Datastore
     * @param routingServiceFactory factory providing {@link Routing} instances
     */
    public RouteReactorImpl(final DataBroker dataBroker, final RoutingServiceFactory routingServiceFactory) {
        datastoreAccess = DatastoreAccess.getInstance(Objects.requireNonNull(dataBroker));
        this.routingServiceFactory = Objects.requireNonNull(routingServiceFactory);
    }

    @Override
    public ListenableFuture<Void> updateRouting(@Nullable final SxpClusterRoute oldRoute,
            @Nullable final SxpClusterRoute newRoute) {
        final Map<IpAddress, RoutingDefinition> oldDefinitions = new HashMap<>();
        final Map<IpAddress, RoutingDefinition> newDefinitions = new HashMap<>();
        final List<RoutingDefinition> outcomingRouteDefinitions = new ArrayList<>();

        fillDefinitionsSafely(oldRoute, oldDefinitions);
        fillDefinitionsSafely(newRoute, newDefinitions);

        final MapDifference<IpAddress, RoutingDefinition>
                routingDifference =
                Maps.difference(oldDefinitions, newDefinitions);

        // ----------------------------
        // ROUTE UPDATE STRATEGY:
        // 1. remove all deleted routes
        // 2. remove all changed routes
        // 3. add all changed routes
        // 4. add all added routes
        // ----------------------------

        // 1
        processDeleted(routingDifference, outcomingRouteDefinitions);
        // 2+3
        processUpdated(routingDifference, outcomingRouteDefinitions);
        // 4
        processAdded(routingDifference, outcomingRouteDefinitions);

        collectUnchanged(routingDifference, outcomingRouteDefinitions);

        // update DS/operational
        final SxpClusterRoute
                sxpClusterRoute =
                new SxpClusterRouteBuilder().setRoutingDefinition(outcomingRouteDefinitions).build();

        return datastoreAccess.put(SxpClusterRouteManager.SXP_CLUSTER_ROUTE_CONFIG_PATH, sxpClusterRoute,
                LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Add all unchanged {@link RoutingDefinition} into provided {@link List}
     *
     * @param routingDifference         contains configuration changes
     * @param outcomingRouteDefinitions where result will be stored
     */
    private void collectUnchanged(final MapDifference<IpAddress, RoutingDefinition> routingDifference,
            final List<RoutingDefinition> outcomingRouteDefinitions) {
        final SxpClusterRoute
                sxpClusterRouteOper =
                datastoreAccess.readSynchronous(SxpClusterRouteManager.SXP_CLUSTER_ROUTE_CONFIG_PATH,
                        LogicalDatastoreType.OPERATIONAL);

        final ImmutableMap<IpAddress, RoutingDefinition>
                routingDefinitionMap =
                Optional.ofNullable(sxpClusterRouteOper)
                        .map(SxpClusterRoute::getRoutingDefinition)
                        .map((routingDefinitions -> Maps.uniqueIndex(routingDefinitions,
                                RoutingDefinition::getIpAddress)))
                        .orElse(ImmutableMap.of());

        routingDifference.entriesInCommon().forEach((virtualIface, routingDefinition) -> {
            final RoutingDefinition routingDef = routingDefinitionMap.get(virtualIface);
            if (routingDef != null) {
                outcomingRouteDefinitions.add(routingDef);
            } else {
                LOG.debug("Missing unchainged routing definition in current DS/operational: {}", routingDefinition);
            }
        });
    }

    /**
     * Process all newly added {@link RoutingDefinition} and create new interface and virtual ip-address for them,
     * adds them into into provided {@link List}
     *
     * @param routingDifference         contains configuration changes
     * @param outcomingRouteDefinitions where result will be stored
     */
    @VisibleForTesting
    void processAdded(final MapDifference<IpAddress, RoutingDefinition> routingDifference,
            final List<RoutingDefinition> outcomingRouteDefinitions) {
        routingDifference.entriesOnlyOnRight().forEach((vIface, routingDef) -> {
            final boolean readyToAdd;
            // clean old unexpected state if any
            final Routing existingRouting = routingServiceMap.get(vIface);
            if (existingRouting != null) {
                LOG.info("Found unexpected route -> closing it: {}", existingRouting);
                findSxpNodesOnVirtualIp(vIface).forEach(SxpNode::shutdown);
                final boolean removalSucceded = existingRouting.removeRouteForCurrentService();
                if (!removalSucceded) {
                    LOG.warn("Route cannot be closed (cleaning before A): {}", existingRouting);
                    RoutingDefinition oldDefinition = RouteUtil.extractRoutingDefinition(existingRouting);
                    outcomingRouteDefinitions.add(RouteUtil.createOperationalRouteDefinition(oldDefinition, false,
                            "route can not be closed (by cleaning before add)"));
                    findSxpNodesOnVirtualIp(vIface).forEach(SxpNode::start);
                    readyToAdd = false;
                } else {
                    readyToAdd = true;
                }
            } else {
                readyToAdd = true;
            }

            if (readyToAdd) {
                final Routing routeService = routingServiceFactory.instantiateRoutingService(routingDef);
                routingServiceMap.put(vIface, routeService);
                final boolean succeeded = routeService.addRouteForCurrentService();
                if (succeeded) {
                    routeService.updateArpTableForCurrentService();
                    findSxpNodesOnVirtualIp(vIface).forEach(SxpNode::start);
                    outcomingRouteDefinitions.add(
                            RouteUtil.createOperationalRouteDefinition(routingDef, true, "added"));
                } else {
                    LOG.warn("Route can not be created (by add): {}", routeService);
                    outcomingRouteDefinitions.add(RouteUtil.createOperationalRouteDefinition(routingDef, false,
                            "route can not be created (by add)"));
                }
            }
        });
    }

    /**
     * Updates routes for all changed {@link RoutingDefinition} and recreate new {@link Routing} for them,
     * adds updated definitions into provided {@link List}
     *
     * @param routingDifference         contains configuration changes
     * @param outcomingRouteDefinitions where result will be stored
     */
    @VisibleForTesting
    void processUpdated(final MapDifference<IpAddress, RoutingDefinition> routingDifference,
            final List<RoutingDefinition> outcomingRouteDefinitions) {
        routingDifference.entriesDiffering().forEach((vIface, routingDiff) -> {
            final Routing routing = routingServiceMap.get(vIface);
            if (routing != null) {
                findSxpNodesOnVirtualIp(vIface).forEach(SxpNode::shutdown);
                final RoutingDefinition outcomingRouteDef;
                final boolean succeededRemoval = routing.removeRouteForCurrentService();
                if (succeededRemoval) {
                    final RoutingDefinition newRoutingDefinition = routingDiff.rightValue();
                    routing.setNetmask(newRoutingDefinition.getNetmask())
                            .setInterface(newRoutingDefinition.getInterface());
                    final boolean succeededAddition = routing.addRouteForCurrentService();
                    if (succeededAddition) {
                        routing.updateArpTableForCurrentService();
                        outcomingRouteDef =
                                RouteUtil.createOperationalRouteDefinition(routingDiff.rightValue(), true, "updated");
                    } else {
                        outcomingRouteDef =
                                RouteUtil.createOperationalRouteDefinition(routingDiff.rightValue(), false,
                                        "route can not be created (by update)");
                    }
                } else {
                    LOG.warn("Route cannot be closed (U): {}", routing);
                    outcomingRouteDef =
                            RouteUtil.createOperationalRouteDefinition(routingDiff.leftValue(), false,
                                    "route can not be closed (by update)");
                }
                findSxpNodesOnVirtualIp(vIface).forEach(SxpNode::start);
                outcomingRouteDefinitions.add(outcomingRouteDef);
            } else {
                outcomingRouteDefinitions.add(
                        RouteUtil.createOperationalRouteDefinition(routingDiff.rightValue(), false,
                                "route can not be updated - missing routingService"));
            }
        });
    }

    /**
     * Removes routes for deleted {@link RoutingDefinition},
     * if {@link Routing} was unsuccessfully removed adds them into provided {@link List}
     *
     * @param routingDifference         contains configuration changes
     * @param outcomingRouteDefinitions where result will be stored
     */
    @VisibleForTesting
    void processDeleted(final MapDifference<IpAddress, RoutingDefinition> routingDifference,
            final List<RoutingDefinition> outcomingRouteDefinitions) {
        routingDifference.entriesOnlyOnLeft().forEach((vIpAddress, routingDef) -> {
            final Routing routingService = routingServiceMap.remove(vIpAddress);
            if (routingService != null) {
                findSxpNodesOnVirtualIp(vIpAddress).forEach(SxpNode::shutdown);
                final boolean succeeded = routingService.removeRouteForCurrentService();
                if (!succeeded) {
                    LOG.warn("Route cannot be closed (D): {}", routingService);
                    outcomingRouteDefinitions.add(RouteUtil.createOperationalRouteDefinition(routingDef, false,
                            "route can not be closed (by remove)"));
                }
            }
        });
    }

    /**
     * @param vIpAddress virtual address
     * @return Nodes related to specified virtual ip-address
     */
    private static Collection<SxpNode> findSxpNodesOnVirtualIp(final IpAddress vIpAddress) {
        return RouteUtil.findSxpNodesOnVirtualIp(vIpAddress, Configuration.getNodes());
    }

    /**
     * @param route       configuration of {@link SxpClusterRoute} that will be transformed
     * @param definitions configuration that will receive data
     */
    @VisibleForTesting
    void fillDefinitionsSafely(final @Nullable SxpClusterRoute route,
            final Map<IpAddress, RoutingDefinition> definitions) {
        Optional.ofNullable(route)
                .map(SxpClusterRoute::getRoutingDefinition)
                .map((routingDefs) -> routingDefs.stream()
                        .map((routingDef) -> definitions.put(routingDef.getIpAddress(), routingDef))
                        .count());
    }

    @Override
    public ListenableFuture<Void> wipeRouting() {
        routingServiceMap.forEach((vIpAddress, routingService) -> {
            findSxpNodesOnVirtualIp(vIpAddress).forEach(SxpNode::shutdown);
            final boolean succeeded = routingService.removeRouteForCurrentService();
            if (succeeded) {
                LOG.debug("wiped out route: {}", routingService);
            } else {
                LOG.warn("failed to wipe out route: {}", routingService);
            }
        });
        routingServiceMap.clear();
        return Futures.immediateFuture(null);
    }

    /**
     * @return currently defined routings
     */
    @VisibleForTesting
    ConcurrentMap<IpAddress, Routing> getRoutingServiceMap() {
        return routingServiceMap;
    }
}
