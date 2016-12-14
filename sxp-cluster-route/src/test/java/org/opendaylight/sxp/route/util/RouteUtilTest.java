/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.util;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.route.spi.Routing;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinitionBuilder;

/**
 * Test for {@link RouteUtil}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RouteUtilTest {

    private static final String DUMMY_ADDRESS_IPV4 = "1.2.3.4";

    private static final String DUMMY_NETMASK_IPV4 = "255.254.253.252";

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    public ThreadsWorker threadsWorker;
    @Mock
    private MasterDatabaseInf masterDatabase;
    @Mock
    private SxpDatabaseInf sxpDatabase;
    public static final String DUMMY_IFACE = "iface123";

    @Test
    public void addressToString_NPE() throws Exception {
        thrown.expect(NullPointerException.class);
        RouteUtil.addressToString(null);
    }

    @Test
    public void addressToString() throws Exception {
        final String actual = RouteUtil.addressToString(new IpAddress(DUMMY_ADDRESS_IPV4.toCharArray()));
        Assert.assertEquals(DUMMY_ADDRESS_IPV4, actual);
    }

    @Test
    public void findSxpNodesOnVirtualIp_empty() throws Exception {
        final Collection<SxpNode> sxpNodes = Collections.emptyList();
        final IpAddress virtualIp = new IpAddress(DUMMY_ADDRESS_IPV4.toCharArray());
        final Collection<SxpNode> actual = RouteUtil.findSxpNodesOnVirtualIp(virtualIp, sxpNodes);

        Assert.assertEquals(0, actual.size());
    }

    @Test
    public void findSxpNodesOnVirtualIp_not_found() throws Exception {
        final Collection<SxpNode> sxpNodes = Collections.singletonList(createSxpNode("5.6.7.8"));
        final IpAddress virtualIp = new IpAddress(DUMMY_ADDRESS_IPV4.toCharArray());

        final Collection<SxpNode> actual = RouteUtil.findSxpNodesOnVirtualIp(virtualIp, sxpNodes);

        Assert.assertEquals(0, actual.size());
    }

    @Test
    public void findSxpNodesOnVirtualIp() throws Exception {
        final Collection<SxpNode> sxpNodes = Lists.newArrayList(
                createSxpNode("5.6.7.8"),
                createSxpNode(DUMMY_ADDRESS_IPV4));
        final IpAddress virtualIp = new IpAddress(DUMMY_ADDRESS_IPV4.toCharArray());

        final Collection<SxpNode> actual = RouteUtil.findSxpNodesOnVirtualIp(virtualIp, sxpNodes);

        Assert.assertEquals(1, actual.size());
        Assert.assertEquals("node-name-1.2.3.4", actual.iterator().next().getName());
    }

    @Test
    public void createOperationalRouteDefinition() throws Exception {
        final RoutingDefinition routingDefinition = new RoutingDefinitionBuilder()
                .setInterface(DUMMY_IFACE)
                .setIpAddress(new IpAddress(DUMMY_ADDRESS_IPV4.toCharArray()))
                .setNetmask(new IpAddress(DUMMY_NETMASK_IPV4.toCharArray()))
                .build();
        final long beforeCreated = (System.currentTimeMillis() / 1000) * 1000;
        final RoutingDefinition actual = RouteUtil.createOperationalRouteDefinition(
                routingDefinition, true, "dummy explanation");

        Assert.assertEquals(DUMMY_ADDRESS_IPV4, String.valueOf(actual.getIpAddress().getValue()));
        Assert.assertEquals(DUMMY_NETMASK_IPV4, String.valueOf(actual.getNetmask().getValue()));
        Assert.assertEquals(DUMMY_IFACE, actual.getInterface());
        Assert.assertTrue(actual.isConsistent());
        Assert.assertEquals("dummy explanation", actual.getInfo());
        Assert.assertTrue(TimeConv.toLong(actual.getTimestamp()) >= beforeCreated);
    }

    @Test
    public void extractRoutingDefinition() throws Exception {
        final Routing routingService = Mockito.mock(Routing.class);
        Mockito.when(routingService.getInterface()).thenReturn(DUMMY_IFACE);
        Mockito.when(routingService.getVirtualIp()).thenReturn(new IpAddress(DUMMY_ADDRESS_IPV4.toCharArray()));
        Mockito.when(routingService.getNetmask()).thenReturn(new IpAddress(DUMMY_NETMASK_IPV4.toCharArray()));

        final RoutingDefinition actual = RouteUtil.extractRoutingDefinition(routingService);

        Assert.assertEquals(DUMMY_ADDRESS_IPV4, String.valueOf(actual.getIpAddress().getValue()));
        Assert.assertEquals(DUMMY_NETMASK_IPV4, String.valueOf(actual.getNetmask().getValue()));
        Assert.assertEquals(DUMMY_IFACE, actual.getInterface());
    }

    private SxpNode createSxpNode(final String ipString) {
        final InetAddress sourceIp = InetAddresses.forString(ipString);
        final SxpNode sxpNode = Mockito.mock(SxpNode.class);
        Mockito.when(sxpNode.getSourceIp()).thenReturn(sourceIp);
        Mockito.when(sxpNode.getName()).thenReturn("node-name-" + ipString);
        return sxpNode;
    }
}