/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.sxp.route.spi.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinitionBuilder;

/**
 * Test for {@link RoutingServiceFactory}.
 */
public class RoutingServiceFactoryTest {

    @Rule public ExpectedException noServiceThrown = ExpectedException.none();
    private RoutingDefinition routingDefinition;
    private RoutingServiceFactory routingServiceFactory;

    @Before
    public void setUp() throws Exception {
        routingServiceFactory = new RoutingServiceFactory();
        routingDefinition =
                new RoutingDefinitionBuilder().setIpAddress(new IpAddress("0.0.0.0".toCharArray()))
                        .setInterface("eth0:0")
                        .setNetmask(new IpAddress("255.255.255.0".toCharArray()))
                        .build();
    }

    @Test
    public void instantiateRoutingService_linux() throws Exception {
        changeOsIsLinuxField(true);
        try {
            final Routing service = routingServiceFactory.instantiateRoutingService(routingDefinition);
            Assert.assertNotNull(service);
        } catch (Exception e) {
            Assert.fail("expected to create a service, got exception instead: " + e.getMessage());
        }
    }

    @Test
    public void instantiateRoutingService_other() throws Exception {
        changeOsIsLinuxField(false);
        noServiceThrown.expect(UnsupportedOperationException.class);
        routingServiceFactory.instantiateRoutingService(routingDefinition);
    }

    private void changeOsIsLinuxField(final boolean mockedOsName) throws NoSuchFieldException, IllegalAccessException {
        final Field osNameField = SystemUtils.class.getDeclaredField("IS_OS_LINUX");
        osNameField.setAccessible(true);
        Field modifiers = osNameField.getClass().getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(osNameField, osNameField.getModifiers() & ~Modifier.FINAL);
        osNameField.set(null, mockedOsName);
    }

}
