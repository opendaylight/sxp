/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.behavior;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

public class StrategyFactoryTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    private SxpNode sxpNode;

    @Before
    public void init() {
        sxpNode = mock(SxpNode.class);
    }

    @Test
    public void testGetStrategy() throws Exception {
        assertTrue(StrategyFactory.getStrategy(sxpNode, Version.Version4) instanceof Sxpv4);
        assertTrue(StrategyFactory.getStrategy(sxpNode, Version.Version3) instanceof SxpLegacy);
        assertTrue(StrategyFactory.getStrategy(sxpNode, Version.Version2) instanceof SxpLegacy);
        assertTrue(StrategyFactory.getStrategy(sxpNode, Version.Version1) instanceof SxpLegacy);
        exception.expect(UnknownVersionException.class);
        assertTrue(StrategyFactory.getStrategy(sxpNode, null) instanceof SxpLegacy);
    }
}
