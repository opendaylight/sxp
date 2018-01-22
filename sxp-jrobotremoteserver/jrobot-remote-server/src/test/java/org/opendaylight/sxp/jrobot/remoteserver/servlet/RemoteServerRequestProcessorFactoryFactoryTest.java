/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.servlet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class RemoteServerRequestProcessorFactoryFactoryTest {

    private RemoteServerRequestProcessorFactoryFactory factory;
    private RemoteServerServlet servlet;

    @Before public void setUp() throws Exception {
        servlet = mock(RemoteServerServlet.class);
        factory = new RemoteServerRequestProcessorFactoryFactory(servlet);
    }

    @Test public void getRequestProcessorFactory() throws Exception {
        Assert.assertNotNull(factory.getRequestProcessorFactory(Object.class));
    }

}
