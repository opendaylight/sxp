/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOption;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.core.handler.MessageDecoder;
import org.opendaylight.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.PasswordType;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//TODO msunal uncomment before commit
@Ignore
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
                when(connection.getPasswordType()).thenReturn(PasswordType.Default);
                when(connection.getDestination()).thenReturn(new InetSocketAddress("0.0.0.0", 64999));

                ChannelConfig
                        config =
                        ConnectFacade.createClient(sxpNode, connection, handlerFactory).channel().config();
                assertNotNull(config.getAllocator());
                assertTrue(config.getOptions().containsKey(ChannelOption.TCP_NODELAY));
                assertTrue(config.getOptions().containsKey(MD5ChannelOption.TCP_MD5SIG));

                when(connection.getPasswordType()).thenReturn(PasswordType.None);
                config = ConnectFacade.createClient(sxpNode, connection, handlerFactory).channel().config();
                assertNotNull(config.getAllocator());
                assertTrue(config.getOptions().containsKey(ChannelOption.TCP_NODELAY));
                assertFalse(config.getOptions().containsKey(MD5ChannelOption.TCP_MD5SIG));
        }

}
