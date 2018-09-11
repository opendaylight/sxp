/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.service;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.netty.channel.Channel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.core.handler.MessageDecoder;
import org.opendaylight.sxp.test.utils.templates.PrebuiltConnectionTemplates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SecurityType;

public class ConnectFacadeTest {

    private static SxpNode sxpNode;

    @Before
    public void init() throws Exception {
        sxpNode = mock(SxpNode.class);
        when(sxpNode.getPassword()).thenReturn("cisco");
        when(sxpNode.getSourceIp()).thenReturn(InetAddress.getByName("127.0.0.1"));
        SxpDomain domainMock = mock(SxpDomain.class);
        when(domainMock.getConnectionTemplates()).thenReturn(Collections.singletonList(PrebuiltConnectionTemplates.DEFAULT_CT));
        when(sxpNode.getDomains()).thenReturn(Collections.singletonList(domainMock));
    }

    @Ignore
    @Test
    public void testCreateClient() throws Exception {
        HandlerFactory handlerFactory
                = HandlerFactory.instanceAddDecoder(MessageDecoder.createClientProfile(sxpNode),
                        HandlerFactory.Position.END);

        SxpConnection connection = mock(SxpConnection.class);
        when(connection.getPassword()).thenReturn("passwd");
        when(connection.getDestination()).thenReturn(new InetSocketAddress("0.0.0.0", 64999));
        when(connection.getSecurityType()).thenReturn(SecurityType.Default);

        Channel channel = ConnectFacade.createClient(sxpNode, connection, handlerFactory).channel();
        assertNotNull(channel.config().getAllocator());
        assertTrue(channel.isOpen());
        assertTrue(channel.isWritable());
        channel.close().get();

        when(connection.getSecurityType()).thenReturn(SecurityType.TLS);

        channel = ConnectFacade.createClient(sxpNode, connection, handlerFactory).channel();
        assertNotNull(channel.config().getAllocator());
        assertTrue(channel.isOpen());
        assertTrue(channel.isWritable());
        channel.close().get();

        try {
            ConnectFacade.createClient(sxpNode, connection, handlerFactory).channel();
            fail("Should fail as SSL context is missing");
        } catch (Exception e) {
            //NOP
        }
    }

    @Ignore
    @Test
    public void testCreateServer() throws Exception {
        HandlerFactory handlerFactory
                = HandlerFactory.instanceAddDecoder(MessageDecoder.createServerProfile(sxpNode),
                        HandlerFactory.Position.END);

        SxpConnection connectionMock = mock(SxpConnection.class);
        when(connectionMock.getPassword()).thenReturn("passwd");
        when(connectionMock.getDestination()).thenReturn(new InetSocketAddress("0.0.0.0", 64999));
        when(connectionMock.getSecurityType()).thenReturn(SecurityType.Default);
        Mockito.when(sxpNode.getAllConnections()).thenReturn(Arrays.asList(connectionMock));

        Channel channel = ConnectFacade.createServer(sxpNode, handlerFactory,
                ConnectFacade.collectAllPasswords(sxpNode)).channel();
        assertTrue(channel.isOpen());
        assertTrue(channel.isWritable());
        channel.close().get();
    }

}
