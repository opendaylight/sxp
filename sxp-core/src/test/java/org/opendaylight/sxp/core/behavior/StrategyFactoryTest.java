/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.behavior;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class}) public class StrategyFactoryTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    private static Context context;
    private static SxpNode sxpNode;

    @Before public void init() {
        sxpNode = PowerMockito.mock(SxpNode.class);
        context = new Context(sxpNode, Version.Version4);
    }

    @Test public void testGetStrategy() throws Exception {
        assertTrue(StrategyFactory.getStrategy(context, Version.Version4) instanceof Sxpv4);
        assertTrue(StrategyFactory.getStrategy(context, Version.Version3) instanceof SxpLegacy);
        assertTrue(StrategyFactory.getStrategy(context, Version.Version2) instanceof SxpLegacy);
        assertTrue(StrategyFactory.getStrategy(context, Version.Version1) instanceof SxpLegacy);
        exception.expect(UnknownVersionException.class);
        assertTrue(StrategyFactory.getStrategy(context, null) instanceof SxpLegacy);
    }
}
