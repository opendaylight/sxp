/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.sxp.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodesConnectionTest {

    private static final Logger LOG;

    static {
        Configuration.initializeLogger();
        LOG = LoggerFactory.getLogger(NodesConnectionTest.class.getName());
    }

    // TODO: nodes connection test - under construction - see Runtime
    @BeforeClass
    public static void init() {
        LOG.warn("NodesConnectionTest:init");
    }

    @Test
    public void NodesConnection() throws Exception {
        // Assert.assertThat(_message, IsEqual.equalTo(result));
    }

    @Test
    public void NodesCreation() throws Exception {
        // Assert.assertThat(_message, IsEqual.equalTo(result));
    }
}
