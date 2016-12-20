/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import static org.opendaylight.sxp.route.util.RouteUtil.addressToString;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.sxp.route.spi.SystemCall;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinitionBuilder;

/**
 * Test for {@link LinuxRoutingService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LinuxRoutingServiceTest {

    private final String virtualIp = "1.2.3.4", ifName = "eth42:0", netMask = "255.255.255.0";

    private LinuxRoutingService service;
    @Mock private SystemCall systemCall;
    @Mock private Process process;

    @Before
    public void setUp() throws Exception {
        Mockito.when(systemCall.execute(Matchers.anyString())).thenReturn(process);
        service =
                new LinuxRoutingService(systemCall, new RoutingDefinitionBuilder().setInterface(ifName)
                        .setNetmask(new IpAddress(netMask.toCharArray()))
                        .setIpAddress(new IpAddress(virtualIp.toCharArray()))
                        .build());
        mockCommand("");
        mockCommand(0);
    }

    private String mockCommand(final String result) {
        Mockito.when(process.getInputStream()).thenReturn(new ByteArrayInputStream(result.getBytes()));
        return result;
    }

    private int mockCommand(int rc) {
        Mockito.when(process.exitValue()).thenReturn(rc);
        return rc;
    }

    private void mockCommand(Exception e) throws InterruptedException {
        Mockito.when(process.waitFor(Mockito.anyInt(), Mockito.any(TimeUnit.class))).thenThrow(e);
    }

    @Test
    public void executeCommand() throws Exception {
        Assert.assertEquals("Expected \"valid output\", got", mockCommand("valid output"),
                service.executeCommand("test cmd"));
        mockCommand(new InterruptedException("invalid"));
        Assert.assertEquals("Expected \"\", got", "", service.executeCommand("invalid cmd"));
    }

    @Test
    public void executeCommandRC() throws Exception {
        Assert.assertEquals("Expected 0, got", mockCommand(0), service.executeCommandRC("test cmd"));
        Assert.assertEquals("Expected 100, got", mockCommand(100), service.executeCommandRC("test cmd"));
        mockCommand(new InterruptedException("invalid"));
        Assert.assertEquals("Expected 1, got", 1, service.executeCommandRC("invalid cmd"));
    }

    @Test
    public void setInterface() throws Exception {
        service.setInterface("eth0:0");
        Assert.assertEquals("Expected \"eth0:0\", got", "eth0:0", service.getInterface());

        service.addRouteForCurrentService();
        service.setInterface("eth0:0");
        Assert.assertEquals("Expected \"eth0:0\", got", "eth0:0", service.getInterface());

        service.setInterface("eth0:1");
        Assert.assertEquals("Expected \"eth0:1\", got", "eth0:1", service.getInterface());
    }

    @Test
    public void setNetmask() throws Exception {
        service.setNetmask(new IpAddress("255.255.0.0".toCharArray()));
        Assert.assertEquals("Expected \"255.255.0.0\", got", "255.255.0.0", addressToString(service.getNetmask()));

        service.addRouteForCurrentService();
        service.setNetmask(new IpAddress("255.255.0.0".toCharArray()));
        Assert.assertEquals("Expected \"255.255.0.0\", got", "255.255.0.0", addressToString(service.getNetmask()));

        service.setNetmask(new IpAddress("255.0.0.0".toCharArray()));
        Assert.assertEquals("Expected \"255.0.0.0\", got", "255.0.0.0", addressToString(service.getNetmask()));
    }

    @Test
    public void getNetmask() throws Exception {
        Assert.assertNotNull("Expected non null value, got", service.getNetmask());
        Assert.assertEquals("Expected unchanged net mask, got", netMask, addressToString(service.getNetmask()));
    }

    @Test
    public void getInterface() throws Exception {
        Assert.assertNotNull("Expected non null value, got", service.getInterface());
        Assert.assertEquals("Expected unchanged interface, got", ifName, service.getInterface());
    }

    @Test
    public void getVirtualIp() throws Exception {
        Assert.assertNotNull("Expected non null value, got", service.getVirtualIp());
        Assert.assertEquals("Expected unchanged net mask, got", virtualIp, addressToString(service.getVirtualIp()));
    }

    @Test
    public void isActive() throws Exception {
        Assert.assertFalse("Expected False as route was not created, got", service.isActive());
        service.addRouteForCurrentService();
        Assert.assertTrue("Expected True as route was created, got", service.isActive());
    }

    @Test
    public void removeRouteForCurrentService_0() throws Exception {
        mockCommand(255);
        Assert.assertFalse("Expected False as Routed was not removed by ifconfig, got",
                service.removeRouteForCurrentService());
    }

    @Test
    public void removeRouteForCurrentService_1() throws Exception {
        mockCommand(0);
        Assert.assertTrue("Expected True as Routed was removed by ifconfig, got",
                service.removeRouteForCurrentService());
    }

    @Test
    public void addRouteForCurrentService_0() throws Exception {
        mockCommand("NO ENTRY in ifconfig");
        mockCommand(255);
        Assert.assertFalse("Expected False as Routed is not in ifconfig and cannot be added, got",
                service.addRouteForCurrentService());
    }

    @Test
    public void addRouteForCurrentService_1() throws Exception {
        String newLine = System.getProperty("line.separator");
        mockCommand("eth42:0  Link encap:Ethernet  HWaddr 28:d2:44:e4:39:f3  " + newLine
                + "          inet addr:1.2.3.4  Bcast:1.2.3.255  Mask:255.255.255.0" + newLine
                + "          UP BROADCAST MULTICAST  MTU:1500  Metric:1" + newLine);
        mockCommand(255);
        Assert.assertTrue("Expected True as Route is in ifconfig, got", service.addRouteForCurrentService());
        Assert.assertTrue("Expected True as Route was already created, got", service.addRouteForCurrentService());
    }

    @Test
    public void addRouteForCurrentService_2() throws Exception {
        mockCommand("NO ENTRY in ifconfig");
        mockCommand(0);
        Assert.assertTrue("Expected True as Route succesfully added, got", service.addRouteForCurrentService());
        Assert.assertTrue("Expected True as Route was already created, got", service.addRouteForCurrentService());
    }

    @Test
    public void updateArpTableForCurrentService() throws Exception {
        mockCommand(0);
        Assert.assertTrue("Expected True as update was successful, got", service.updateArpTableForCurrentService());
        mockCommand(255);
        Assert.assertFalse("Expected False as update was not successful, got",
                service.updateArpTableForCurrentService());
    }

    @Test
    public void createVirtualIpRegisteredMatch() throws Exception {
        Assert.assertEquals("(.*)inet addr:1\\.2\\.3\\.4(.*)Bcast:(.*)Mask:255\\.255\\.255\\.0(.*)",
                service.createVirtualIpRegisteredMatch());
    }

    @Test
    public void createIfaceDownCmd() throws Exception {
        Assert.assertEquals("sudo ifconfig eth42:0 down", service.createIfaceDownCmd());
    }

    @Test
    public void createIfaceUpCmd() throws Exception {
        Assert.assertEquals("sudo ifconfig eth42:0 1.2.3.4 netmask 255.255.255.0 up", service.createIfaceUpCmd());
    }

    @Test
    public void createPingArpCmd() throws Exception {
        Assert.assertEquals("sudo arping -q -c 1 -S 1.2.3.4 -i eth42 -B", service.createPingArpCmd());
    }

    @Test
    public void toStringTest() throws Exception {
        final String
                expected =
                "LinuxRoute [virtualIp=" + virtualIp + ", interfaceName=" + ifName + ", netmask=" + netMask
                        + ", active=false]";
        Assert.assertEquals("Expected \"" + expected + "\", got", expected, service.toString());
    }
}
