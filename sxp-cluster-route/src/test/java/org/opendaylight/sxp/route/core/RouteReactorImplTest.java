/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.BindingTransactionChain;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.sxp.route.spi.Routing;
import org.opendaylight.sxp.route.util.RouteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.SxpClusterRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinitionBuilder;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link RouteReactorImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RouteReactorImplTest {

    @Mock private DataBroker dataBroker;
    @Mock private BindingTransactionChain txChain;
    @Mock private ReadWriteTransaction wTx;
    @Mock private ReadTransaction rTx;
    @Mock private RoutingServiceFactory routingServiceFactory;
    @Mock private Routing routingService;
    @Captor ArgumentCaptor<InstanceIdentifier<SxpClusterRoute>> pathCaptor;
    @Captor ArgumentCaptor<SxpClusterRoute> dataCaptor;

    private ArrayList<RoutingDefinition> leftSide;
    private ArrayList<RoutingDefinition> rightSide;
    private MapDifference<IpAddress, RoutingDefinition> mapDifference;

    private RouteReactorImpl reactor;

    @Before
    public void setUp() throws Exception {
        Mockito.when(routingService.setInterface(Matchers.any())).thenReturn(routingService);
        Mockito.when(routingService.setNetmask(Matchers.any())).thenReturn(routingService);
        Mockito.when(routingServiceFactory.instantiateRoutingService(Matchers.any())).thenReturn(routingService);
        Mockito.when(dataBroker.createTransactionChain(Matchers.any())).thenReturn(txChain);
        Mockito.when(txChain.newWriteOnlyTransaction()).thenReturn(wTx);
        Mockito.when(txChain.newReadOnlyTransaction()).thenReturn(rTx);

        reactor = new RouteReactorImpl(dataBroker, routingServiceFactory);
        leftSide =
                Lists.newArrayList(RouteTestFactory.createDummyRoutingDef(1, 1),
                        RouteTestFactory.createDummyRoutingDef(2, 2), RouteTestFactory.createDummyRoutingDef(3, 3));

        rightSide =
                Lists.newArrayList(RouteTestFactory.createDummyRoutingDef(2, 2),
                        RouteTestFactory.createDummyRoutingDef(3, 1), RouteTestFactory.createDummyRoutingDef(4, 4));

        // 1 -> -  [remove]
        // 2 -> 2  [NOOP]
        // 3 -> 3' [UPDATE]
        // - -> 4  [NEW]

        mapDifference = Maps.difference(wrapToIpMap(leftSide), wrapToIpMap(rightSide));
    }

    private Map<IpAddress, RoutingDefinition> wrapToIpMap(final ArrayList<RoutingDefinition> routingDefs) {
        return Maps.uniqueIndex(routingDefs, RoutingDefinition::getIpAddress);
    }

    @Test
    public void processAdded() throws Exception {
        final List<RoutingDefinition> outcome = new ArrayList<>();
        Mockito.when(routingService.addRouteForCurrentService()).thenReturn(true);

        reactor.processAdded(mapDifference, outcome);

        final RoutingDefinition expected = rightSide.get(2);
        Assert.assertEquals(1, outcome.size());

        final RoutingDefinition actual = outcome.get(0);
        Assert.assertEquals(expected, distillDefinition(actual));
        Assert.assertTrue(actual.isConsistent());
        Assert.assertEquals("added", actual.getInfo());
    }

    @Test
    public void processAdded_failed() throws Exception {
        final List<RoutingDefinition> outcome = new ArrayList<>();

        reactor.processAdded(mapDifference, outcome);

        final RoutingDefinition expected = rightSide.get(2);
        Assert.assertEquals(1, outcome.size());

        final RoutingDefinition actual = outcome.get(0);
        Assert.assertEquals(expected, distillDefinition(actual));
        Assert.assertFalse(actual.isConsistent());
        Assert.assertEquals("route can not be created (by add)", actual.getInfo());
    }

    @Test
    public void processAdded_failed_can_not_clean() throws Exception {
        final List<RoutingDefinition> outcome = new ArrayList<>();

        final RoutingDefinition newman = rightSide.get(2);
        reactor.getRoutingServiceMap().put(newman.getIpAddress(), routingService);
        Mockito.when(routingService.getNetmask()).thenReturn(newman.getNetmask());
        Mockito.when(routingService.getInterface()).thenReturn(newman.getInterface());
        Mockito.when(routingService.getVirtualIp()).thenReturn(newman.getIpAddress());

        reactor.processAdded(mapDifference, outcome);

        Assert.assertEquals(1, outcome.size());

        final RoutingDefinition actual = outcome.get(0);
        Assert.assertEquals(newman, distillDefinition(actual));
        Assert.assertFalse(actual.isConsistent());
        Assert.assertEquals("route can not be closed (by cleaning before add)", actual.getInfo());
    }

    @Test
    public void processAdded_with_cleaning() throws Exception {
        final List<RoutingDefinition> outcome = new ArrayList<>();
        Mockito.when(routingService.removeRouteForCurrentService()).thenReturn(true);
        Mockito.when(routingService.addRouteForCurrentService()).thenReturn(true);

        final RoutingDefinition newman = rightSide.get(2);
        reactor.getRoutingServiceMap().put(newman.getIpAddress(), routingService);

        reactor.processAdded(mapDifference, outcome);

        Assert.assertEquals(1, outcome.size());

        final RoutingDefinition actual = outcome.get(0);
        Assert.assertEquals(newman, distillDefinition(actual));
        Assert.assertTrue(actual.isConsistent());
        Assert.assertEquals("added", actual.getInfo());
    }

    @Test
    public void processUpdated_fail_noService() throws Exception {
        final List<RoutingDefinition> outcome = new ArrayList<>();
        reactor.processUpdated(mapDifference, outcome);

        final RoutingDefinition expected = rightSide.get(1);
        Assert.assertEquals(1, outcome.size());

        final RoutingDefinition actual = outcome.get(0);
        Assert.assertEquals(expected, distillDefinition(actual));
        Assert.assertFalse(actual.isConsistent());
        Assert.assertEquals("route can not be updated - missing routingService", actual.getInfo());
    }

    @Test
    public void processUpdated_fail() throws Exception {
        final List<RoutingDefinition> outcome = new ArrayList<>();

        final RoutingDefinition yesman = leftSide.get(2);
        reactor.getRoutingServiceMap().put(yesman.getIpAddress(), routingService);

        reactor.processUpdated(mapDifference, outcome);

        Assert.assertEquals(1, outcome.size());

        final RoutingDefinition actual = outcome.get(0);
        Assert.assertEquals(yesman, distillDefinition(actual));
        Assert.assertFalse(actual.isConsistent());
        Assert.assertEquals("route can not be closed (by update)", actual.getInfo());
    }

    @Test
    public void processUpdated_fail_to_create() throws Exception {
        final List<RoutingDefinition> outcome = new ArrayList<>();
        Mockito.when(routingService.removeRouteForCurrentService()).thenReturn(true);

        final RoutingDefinition yesman = leftSide.get(2);
        reactor.getRoutingServiceMap().put(yesman.getIpAddress(), routingService);

        reactor.processUpdated(mapDifference, outcome);

        Assert.assertEquals(1, outcome.size());

        final RoutingDefinition expected = rightSide.get(1);
        final RoutingDefinition actual = outcome.get(0);
        Assert.assertEquals(expected, distillDefinition(actual));
        Assert.assertFalse(actual.isConsistent());
        Assert.assertEquals("route can not be created (by update)", actual.getInfo());
    }

    @Test
    public void processUpdated() throws Exception {
        final List<RoutingDefinition> outcome = new ArrayList<>();
        Mockito.when(routingService.removeRouteForCurrentService()).thenReturn(true);
        Mockito.when(routingService.addRouteForCurrentService()).thenReturn(true);

        final RoutingDefinition yesman = leftSide.get(2);
        reactor.getRoutingServiceMap().put(yesman.getIpAddress(), routingService);

        reactor.processUpdated(mapDifference, outcome);

        Assert.assertEquals(1, outcome.size());

        final RoutingDefinition expected = rightSide.get(1);
        final RoutingDefinition actual = outcome.get(0);
        Assert.assertEquals(expected, distillDefinition(actual));
        Assert.assertTrue(actual.isConsistent());
        Assert.assertEquals("updated", actual.getInfo());
    }

    @Test
    public void processDeleted() throws Exception {
        final List<RoutingDefinition> outcome = new ArrayList<>();
        Mockito.when(routingService.removeRouteForCurrentService()).thenReturn(true);

        final RoutingDefinition deadman = leftSide.get(0);
        reactor.getRoutingServiceMap().put(deadman.getIpAddress(), routingService);

        reactor.processDeleted(mapDifference, outcome);

        Assert.assertEquals(0, outcome.size());
    }

    @Test
    public void processDeleted_fail() throws Exception {
        final List<RoutingDefinition> outcome = new ArrayList<>();

        final RoutingDefinition zombie = leftSide.get(0);
        reactor.getRoutingServiceMap().put(zombie.getIpAddress(), routingService);

        reactor.processDeleted(mapDifference, outcome);

        Assert.assertEquals(1, outcome.size());

        final RoutingDefinition actual = outcome.get(0);
        Assert.assertEquals(zombie, distillDefinition(actual));
        Assert.assertFalse(actual.isConsistent());
        Assert.assertEquals("route can not be closed (by remove)", actual.getInfo());
    }

    @Test
    public void wipeRouting() throws Exception {
        leftSide.forEach((routingDefinition -> {
            reactor.getRoutingServiceMap().put(routingDefinition.getIpAddress(), routingService);
        }));
        Mockito.when(routingService.removeRouteForCurrentService()).thenReturn(true);

        reactor.wipeRouting();

        Mockito.verify(routingService, Mockito.times(3)).removeRouteForCurrentService();
    }

    @Test
    public void wipeRouting_fail() throws Exception {
        leftSide.forEach((routingDefinition -> {
            reactor.getRoutingServiceMap().put(routingDefinition.getIpAddress(), routingService);
        }));

        reactor.wipeRouting();

        Mockito.verify(routingService, Mockito.times(3)).removeRouteForCurrentService();
    }

    @Test
    public void fillDefinitionsSafely() throws Exception {
        final Map<IpAddress, RoutingDefinition> targetMap = new HashMap<>();
        final SxpClusterRoute sxpClusterRoute = new SxpClusterRouteBuilder().setRoutingDefinition(rightSide).build();
        reactor.fillDefinitionsSafely(sxpClusterRoute, targetMap);

        Assert.assertEquals(rightSide.size(), targetMap.size());
    }

    @Test
    public void fillDefinitionsSafely_null() throws Exception {
        final Map<IpAddress, RoutingDefinition> targetMap = new HashMap<>();
        final SxpClusterRoute sxpClusterRoute = new SxpClusterRouteBuilder().build();
        reactor.fillDefinitionsSafely(sxpClusterRoute, targetMap);

        Assert.assertEquals(0, targetMap.size());
    }

    @Test
    public void updateRouting() throws Exception {
        final SxpClusterRoute oldRoute = new SxpClusterRouteBuilder().setRoutingDefinition(leftSide).build();
        final SxpClusterRoute newRoute = new SxpClusterRouteBuilder().setRoutingDefinition(rightSide).build();

        Mockito.when(routingService.addRouteForCurrentService()).thenReturn(true);
        Mockito.when(routingService.removeRouteForCurrentService()).thenReturn(true);

        Mockito.when(rTx.read(Matchers.any(), Matchers.any()))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(oldRoute)));
        Mockito.when(wTx.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());

        Iterables.concat(leftSide, rightSide).forEach(routeDef -> {
            reactor.getRoutingServiceMap().putIfAbsent(routeDef.getIpAddress(), routingService);
        });

        reactor.updateRouting(oldRoute, newRoute);

        Mockito.verify(wTx).put(Matchers.any(), pathCaptor.capture(), dataCaptor.capture());
        Assert.assertEquals(1, pathCaptor.getAllValues().size());
        Assert.assertEquals(1, dataCaptor.getAllValues().size());

        final List<RoutingDefinition> actual = dataCaptor.getValue().getRoutingDefinition();
        Assert.assertEquals(3, actual.size());

        Collections.sort(actual, Comparator.comparing(o -> RouteUtil.addressToString(o.getIpAddress())));

        int idx = 0;
        Assert.assertEquals(null, actual.get(idx).getInfo());
        Assert.assertNull(actual.get(idx).isConsistent());
        Assert.assertEquals("1.2.3.2", RouteUtil.addressToString(actual.get(idx).getIpAddress()));
        idx++;
        Assert.assertEquals("updated", actual.get(idx).getInfo());
        Assert.assertEquals(true, actual.get(idx).isConsistent());
        Assert.assertEquals("1.2.3.3", RouteUtil.addressToString(actual.get(idx).getIpAddress()));
        idx++;
        Assert.assertEquals("added", actual.get(idx).getInfo());
        Assert.assertEquals(true, actual.get(idx).isConsistent());
        Assert.assertEquals("1.2.3.4", RouteUtil.addressToString(actual.get(idx).getIpAddress()));
    }

    private RoutingDefinition distillDefinition(final RoutingDefinition routingDefinition) {
        return new RoutingDefinitionBuilder(routingDefinition).setInfo(null)
                .setTimestamp(null)
                .setConsistent(null)
                .build();
    }
}
