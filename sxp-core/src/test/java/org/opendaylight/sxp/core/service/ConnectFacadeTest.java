/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.netty.channel.Channel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.core.handler.MessageDecoder;
import org.opendaylight.sxp.test.utils.templates.PrebuiltConnectionTemplates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SecurityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.StoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.security.fields.Tls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.security.fields.TlsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.tls.security.fields.KeystoreBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.tls.security.fields.TruststoreBuilder;

public class ConnectFacadeTest {

    private static final Tls TLS = new TlsBuilder()
            .setCertificatePassword("admin123")
            .setKeystore(new KeystoreBuilder()
                    .setType(StoreType.JKS)
                    .setPathType(PathType.CLASSPATH)
                    .setLocation("keystore")
                    .setPassword("admin123")
                    .build())
            .setTruststore(new TruststoreBuilder()
                    .setType(StoreType.JKS)
                    .setPathType(PathType.CLASSPATH)
                    .setLocation("truststore")
                    .setPassword("admin123")
                    .build())
            .build();

    @Mock
    private SxpNode sxpNode;
    @Mock
    private SxpDomain sxpDomain;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(sxpNode.getPassword()).thenReturn("cisco");
        when(sxpNode.getSourceIp()).thenReturn(InetAddress.getByName("127.0.0.1"));
        when(sxpNode.getDomains()).thenReturn(Collections.singletonList(sxpDomain));
        when(sxpNode.getSslContextFactory()).thenReturn(new SslContextFactory(TLS));
        when(sxpDomain.getConnectionTemplates()).thenReturn(Collections.singletonList(PrebuiltConnectionTemplates.DEFAULT_CT));
    }

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
    }

    @Test
    public void testCreateServer() throws Exception {
        HandlerFactory handlerFactory
                = HandlerFactory.instanceAddDecoder(MessageDecoder.createServerProfile(sxpNode),
                        HandlerFactory.Position.END);

        SxpConnection connectionMock = mock(SxpConnection.class);
        when(connectionMock.getPassword()).thenReturn("passwd");
        when(connectionMock.getDestination()).thenReturn(new InetSocketAddress("0.0.0.0", 64999));
        when(connectionMock.getSecurityType()).thenReturn(SecurityType.Default);
        Mockito.when(sxpNode.getAllConnections()).thenReturn(Collections.singletonList(connectionMock));

        Channel channel = ConnectFacade.createServer(sxpNode, handlerFactory,
                ConnectFacade.collectAllPasswords(sxpNode)).channel();
        assertTrue(channel.isOpen());
        assertTrue(channel.isWritable());
        channel.close().get();
    }
}
