/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.binding.origins.BindingOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.binding.origins.BindingOriginBuilder;

public class BindingOriginsConfigTest {
    private static final BindingOriginsConfig INSTANCE = BindingOriginsConfig.INSTANCE;
    private static final BindingOrigin LOCAL_ORIGIN = new BindingOriginBuilder()
            .setOrigin(BindingOriginsConfig.LOCAL_ORIGIN)
            .setPriority((short) 1)
            .build();
    private static final BindingOrigin NETWORK_ORIGIN = new BindingOriginBuilder()
            .setOrigin(BindingOriginsConfig.NETWORK_ORIGIN)
            .setPriority((short) 2)
            .build();
    private static final BindingOrigin CLUSTER_ORIGIN = new BindingOriginBuilder()
            .setOrigin(new OriginType("CLUSTER"))
            .setPriority((short) 3)
            .build();
    private static final BindingOrigin CLOUD_ORIGIN = new BindingOriginBuilder()
            .setOrigin(new OriginType("CLOUD"))
            .setPriority((short) 4)
            .build();
    private static final List<BindingOrigin> ORIGINS = Lists.newArrayList(CLUSTER_ORIGIN, CLOUD_ORIGIN);

    @Before
    public void setUp() {
        INSTANCE.addBindingOrigins(BindingOriginsConfig.DEFAULT_ORIGIN_PRIORITIES);
        INSTANCE.addBindingOrigins(ORIGINS);
    }

    @After
    public void tearDown() {
        INSTANCE.deleteConfiguration();
    }

    @Test
    public void testAddBindingOrigin() {
        final OriginType originType = new OriginType("NEW_TYPE");
        Assert.assertTrue(INSTANCE.addBindingOrigin(originType, 0));
        Assert.assertEquals(5, INSTANCE.getBindingOrigins().size());
        Assert.assertTrue(INSTANCE.containsOrigin(originType));
    }

    @Test
    public void testAddBindingOriginDuplicateType() {
        Assert.assertFalse(INSTANCE.addBindingOrigin(BindingOriginsConfig.LOCAL_ORIGIN, 0));
        Assert.assertEquals(4, INSTANCE.getBindingOrigins().size());
    }

    @Test
    public void testAddBindingOriginDuplicatePriority() {
        Assert.assertFalse(INSTANCE.addBindingOrigin(new OriginType("DUPLICATE_PRIORITY"), 4));
        Assert.assertEquals(4, INSTANCE.getBindingOrigins().size());
    }

    @Test
    public void testAddOrUpdateBindingOriginAdd() {
        final OriginType originType = new OriginType("NEW_TYPE");
        Assert.assertTrue(INSTANCE.addOrUpdateBindingOrigin(originType, 0));
        Assert.assertEquals(5, INSTANCE.getBindingOrigins().size());
        Assert.assertTrue(INSTANCE.containsOrigin(originType));
    }

    @Test
    public void testAddOrUpdateBindingOriginUpdate() {
        Assert.assertTrue(INSTANCE.addOrUpdateBindingOrigin(BindingOriginsConfig.LOCAL_ORIGIN, 0));
        final Map<OriginType, Integer> bindingOrigins = INSTANCE.getBindingOrigins();
        Assert.assertEquals(0, bindingOrigins.get(BindingOriginsConfig.LOCAL_ORIGIN).intValue());
    }

    @Test
    public void testAddOrUpdateBindingOriginDuplicatePriorityAdd() {
        Assert.assertFalse(INSTANCE.addOrUpdateBindingOrigin(new OriginType("NEW_TYPE"), 4));
        Assert.assertEquals(4, INSTANCE.getBindingOrigins().size());
    }

    @Test
    public void testAddOrUpdateBindingOriginDuplicatePriorityUpdate() {
        Assert.assertFalse(INSTANCE.addOrUpdateBindingOrigin(BindingOriginsConfig.LOCAL_ORIGIN, 4));
        final Map<OriginType, Integer> bindingOrigins = INSTANCE.getBindingOrigins();
        Assert.assertEquals(1, bindingOrigins.get(BindingOriginsConfig.LOCAL_ORIGIN).intValue());
    }

    @Test
    public void testUpdateBindingOrigin() {
        Assert.assertTrue(INSTANCE.updateBindingOrigin(BindingOriginsConfig.LOCAL_ORIGIN, 0));
        final Map<OriginType, Integer> bindingOrigins = INSTANCE.getBindingOrigins();
        Assert.assertEquals(0, bindingOrigins.get(BindingOriginsConfig.LOCAL_ORIGIN).intValue());
    }

    @Test
    public void testUpdateBindingOriginNotExists() {
        Assert.assertFalse(INSTANCE.updateBindingOrigin(new OriginType("NOT_EXISTING_TYPE"), 0));
    }

    @Test
    public void testUpdateBindingOriginDuplicatePriority() {
        Assert.assertFalse(INSTANCE.updateBindingOrigin(BindingOriginsConfig.LOCAL_ORIGIN, 4));
        final Map<OriginType, Integer> bindingOrigins = INSTANCE.getBindingOrigins();
        Assert.assertEquals(1, bindingOrigins.get(BindingOriginsConfig.LOCAL_ORIGIN).intValue());
    }

    @Test
    public void testDeleteBindingOrigin() {
        Assert.assertTrue(INSTANCE.deleteBindingOrigin(new OriginType("CLUSTER")));
        Assert.assertEquals(3, INSTANCE.getBindingOrigins().size());
    }

    @Test
    public void testDeleteBindingOriginDefault() {
        Assert.assertFalse(INSTANCE.deleteBindingOrigin(BindingOriginsConfig.LOCAL_ORIGIN));
        Assert.assertEquals(4, INSTANCE.getBindingOrigins().size());
    }

    @Test
    public void testDeleteBindingOriginNotFound() {
        Assert.assertFalse(INSTANCE.deleteBindingOrigin(new OriginType("NOT_EXISTING_TYPE")));
        Assert.assertEquals(4, INSTANCE.getBindingOrigins().size());
    }

    @Test
    public void testValidateBindingOrigins() {
        final Collection<BindingOrigin> origins = new ArrayList<>();
        origins.add(LOCAL_ORIGIN);
        origins.add(NETWORK_ORIGIN);
        origins.add(CLUSTER_ORIGIN);
        BindingOriginsConfig.validateBindingOrigins(origins);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateBindingOriginsMissingNetwork() {
        final Collection<BindingOrigin> origins = new ArrayList<>();
        origins.add(LOCAL_ORIGIN);
        BindingOriginsConfig.validateBindingOrigins(origins);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateBindingOriginsMissingLocal() {
        final Collection<BindingOrigin> origins = new ArrayList<>();
        origins.add(NETWORK_ORIGIN);
        BindingOriginsConfig.validateBindingOrigins(origins);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateBindingOriginsDuplicateType() {
        final BindingOriginBuilder builder = new BindingOriginBuilder();
        builder.setOrigin(new OriginType("DUPLICATE_TYPE"));

        final Collection<BindingOrigin> origins = new ArrayList<>();
        origins.add(LOCAL_ORIGIN);
        origins.add(NETWORK_ORIGIN);
        origins.add(builder.setPriority((short) 1).build());
        origins.add(builder.setPriority((short) 2).build());
        BindingOriginsConfig.validateBindingOrigins(origins);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateBindingOriginsDuplicatePriority() {
        final BindingOriginBuilder builder = new BindingOriginBuilder();
        builder.setPriority((short) 0);

        final Collection<BindingOrigin> origins = new ArrayList<>();
        origins.add(LOCAL_ORIGIN);
        origins.add(NETWORK_ORIGIN);
        origins.add(builder.setOrigin(new OriginType("FIRST")).build());
        origins.add(builder.setOrigin(new OriginType("SECOND")).build());
        BindingOriginsConfig.validateBindingOrigins(origins);
    }
}