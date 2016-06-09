/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import io.netty.channel.Channel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.core.handler.MessageDecoder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class}) public class ConnectFacadeTest {

        private static SxpNode sxpNode;

        @Before public void init() throws Exception {
                sxpNode = PowerMockito.mock(SxpNode.class);
                PowerMockito.when(sxpNode.getPassword()).thenReturn("cisco");
                PowerMockito.when(sxpNode.getSourceIp()).thenReturn(InetAddress.getByName("127.0.0.1"));
        }

        @Test public void testCreateClient() throws Exception {
                HandlerFactory handlerFactory = new HandlerFactory(MessageDecoder.createClientProfile(sxpNode));

                SxpConnection connection = mock(SxpConnection.class);
                when(connection.getPassword()).thenReturn("passwd");
                when(connection.getDestination()).thenReturn(new InetSocketAddress("0.0.0.0", 64999));

                Channel
                        channel =
                        ConnectFacade.createClient(sxpNode, connection, handlerFactory).channel();
                assertNotNull(channel.config().getAllocator());
                assertTrue(channel.isOpen());
                assertTrue(channel.isWritable());
        }

        @Test public void testCreateServer() throws Exception {
                HandlerFactory handlerFactory = new HandlerFactory(MessageDecoder.createServerProfile(sxpNode));

                Channel channel = ConnectFacade.createServer(sxpNode, handlerFactory).channel();
                assertTrue(channel.isOpen());
                assertTrue(channel.isWritable());
        }

}
