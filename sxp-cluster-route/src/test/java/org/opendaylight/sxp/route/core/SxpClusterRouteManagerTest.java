/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.sxp.controller.boot.SxpControllerInstance;
import org.opendaylight.sxp.route.api.RouteReactor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinition;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;

/**
 * Test for {@link SxpClusterRouteManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SxpClusterRouteManagerTest {

    @Mock private DataBroker dataBroker;
    @Mock private ListenerRegistration<SxpClusterRouteManager> listenerRegistration;
    @Mock private ClusterSingletonServiceProvider cssProvider;
    @Mock private ClusterSingletonServiceRegistration cssRegistration;
    @Mock private RouteReactor routeReactor;
    @Mock private DataTreeModification<SxpClusterRoute> modification;
    @Mock private DataObjectModification<SxpClusterRoute> dataObjectModification;

    private SxpClusterRouteManager manager;

    @Before
    public void setUp() throws Exception {
        Mockito.when(routeReactor.updateRouting(Matchers.any(), Matchers.any()))
                .thenReturn(FluentFutures.immediateNullFluentFuture());
        Mockito.when(routeReactor.wipeRouting()).thenReturn(FluentFutures.immediateNullFluentFuture());
        manager = new SxpClusterRouteManager(dataBroker, cssProvider, routeReactor);
        Assert.assertEquals(RouteListenerState.STOPPED, manager.getState());

        Mockito.when(cssProvider.registerClusterSingletonService(manager)).thenReturn(cssRegistration);
    }

    @After
    public void tearDown() throws Exception {
        Mockito.verifyNoMoreInteractions(dataBroker, cssProvider, routeReactor, listenerRegistration, cssRegistration);
    }

    @Test
    public void init() throws Exception {
        manager.init();
        Mockito.verify(cssProvider).registerClusterSingletonService(manager);
    }

    @Test
    public void onDataTreeChanged() throws Exception {
        final List<RoutingDefinition> oldDefs = Lists.newArrayList(RouteTestFactory.createDummyRoutingDef(1, 1));
        final SxpClusterRoute oldRouting = new SxpClusterRouteBuilder().setRoutingDefinition(oldDefs).build();
        final List<RoutingDefinition>
                newDefs =
                Lists.newArrayList(RouteTestFactory.createDummyRoutingDef(1, 2),
                        RouteTestFactory.createDummyRoutingDef(3, 3));
        final SxpClusterRoute newRouting = new SxpClusterRouteBuilder().setRoutingDefinition(newDefs).build();

        Mockito.when(dataObjectModification.getDataBefore()).thenReturn(oldRouting);
        Mockito.when(dataObjectModification.getDataAfter()).thenReturn(newRouting);

        Mockito.when(modification.getRootNode()).thenReturn(dataObjectModification);

        manager.init();
        Assert.assertEquals(RouteListenerState.STOPPED, manager.getState());
        manager.instantiateServiceInstance();
        Assert.assertEquals(RouteListenerState.BEFORE_FIRST, manager.getState());

        manager.onDataTreeChanged(Collections.singletonList(modification));

        Mockito.verify(dataBroker)
                .registerDataTreeChangeListener(SxpClusterRouteManager.ROUTING_DEFINITION_DT_IDENTIFIER, manager);
        Mockito.verify(cssProvider).registerClusterSingletonService(manager);
        Mockito.verify(routeReactor).updateRouting(oldRouting, newRouting);
        Assert.assertEquals(RouteListenerState.WORKING, manager.getState());
    }

    @Test
    public void getIdentifier() throws Exception {
        Assert.assertEquals(SxpControllerInstance.class.getName(), manager.getIdentifier().getValue());
    }

    @Test
    public void instantiateServiceInstance() throws Exception {
        Mockito.when(dataBroker.registerDataTreeChangeListener(SxpClusterRouteManager.ROUTING_DEFINITION_DT_IDENTIFIER,
                manager)).thenReturn(listenerRegistration);

        manager.init();
        Assert.assertEquals(RouteListenerState.STOPPED, manager.getState());
        manager.instantiateServiceInstance();

        Mockito.verify(dataBroker)
                .registerDataTreeChangeListener(SxpClusterRouteManager.ROUTING_DEFINITION_DT_IDENTIFIER, manager);
        Mockito.verify(cssProvider).registerClusterSingletonService(manager);
        Assert.assertEquals(RouteListenerState.BEFORE_FIRST, manager.getState());
    }

    @Test
    public void closeServiceInstance() throws Exception {
        Mockito.when(dataBroker.registerDataTreeChangeListener(SxpClusterRouteManager.ROUTING_DEFINITION_DT_IDENTIFIER,
                manager)).thenReturn(listenerRegistration);

        manager.init();
        Assert.assertEquals(RouteListenerState.STOPPED, manager.getState());
        manager.instantiateServiceInstance();
        Assert.assertEquals(RouteListenerState.BEFORE_FIRST, manager.getState());
        manager.closeServiceInstance();

        Mockito.verify(cssProvider).registerClusterSingletonService(manager);
        Mockito.verify(dataBroker)
                .registerDataTreeChangeListener(SxpClusterRouteManager.ROUTING_DEFINITION_DT_IDENTIFIER, manager);
        Mockito.verify(listenerRegistration).close();
        Mockito.verify(routeReactor).wipeRouting();
        Assert.assertEquals(RouteListenerState.STOPPED, manager.getState());
    }

    @Test
    public void close() throws Exception {
        Mockito.when(dataBroker.registerDataTreeChangeListener(SxpClusterRouteManager.ROUTING_DEFINITION_DT_IDENTIFIER,
                manager)).thenReturn(listenerRegistration);

        manager.init();
        manager.instantiateServiceInstance();
        Assert.assertEquals(RouteListenerState.BEFORE_FIRST, manager.getState());
        manager.close();

        Mockito.verify(cssProvider).registerClusterSingletonService(manager);
        Mockito.verify(cssRegistration).close();
        Mockito.verify(dataBroker)
                .registerDataTreeChangeListener(SxpClusterRouteManager.ROUTING_DEFINITION_DT_IDENTIFIER, manager);
        Mockito.verify(listenerRegistration).close();
        Mockito.verify(routeReactor).wipeRouting();
        Assert.assertEquals(RouteListenerState.STOPPED, manager.getState());
    }

}
