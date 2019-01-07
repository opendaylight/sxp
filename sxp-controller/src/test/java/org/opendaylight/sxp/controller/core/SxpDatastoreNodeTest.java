/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.channel.Channel;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.netty.ImmediateCancelledFuture;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.Security;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatastoreAccess.class, BindingDispatcher.class})
public class SxpDatastoreNodeTest {

    private static String ID = "127.0.0.1";
    private SxpDatastoreNode node;
    private SxpNodeIdentity nodeIdentity;
    private DatastoreAccess datastoreAccess;
    private BindingDispatcher dispatcher;

    @Before
    public void setUp() throws Exception {
        nodeIdentity = mock(SxpNodeIdentity.class);
        datastoreAccess = mock(DatastoreAccess.class);
        PowerMockito.mockStatic(DatastoreAccess.class);
        PowerMockito.when(DatastoreAccess.getInstance(any(DataBroker.class))).thenReturn(datastoreAccess);
        PowerMockito.when(DatastoreAccess.getInstance(any(DatastoreAccess.class))).thenReturn(datastoreAccess);
        when(nodeIdentity.getVersion()).thenReturn(Version.Version4);
        Security security = mock(Security.class);
        when(security.getPassword()).thenReturn("default");
        when(nodeIdentity.getName()).thenReturn("NAME");
        when(nodeIdentity.getSecurity()).thenReturn(security);
        when(nodeIdentity.getMappingExpanded()).thenReturn(150);
        when(nodeIdentity.getTcpPort()).thenReturn(new PortNumber(64977));
        when(nodeIdentity.getSourceIp()).thenReturn(IpAddressBuilder.getDefaultInstance(ID));
        when(nodeIdentity.getTcpPort()).thenReturn(PortNumber.getDefaultInstance("64977"));
        when(nodeIdentity.getTimers()).thenReturn(new TimersBuilder().build());
        dispatcher = mock(BindingDispatcher.class);

        node = SxpDatastoreNode.createInstance(NodeIdConv.createNodeId(ID), datastoreAccess, nodeIdentity);
        node.addDomain(new SxpDomainBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN).build());
        PowerMockito.field(SxpNode.class, "serverChannel").set(node, mock(Channel.class));
        PowerMockito.field(SxpNode.class, "svcBindingDispatcher").set(node, dispatcher);
    }

    @Test
    public void testGetIdentifier() throws Exception {
        assertEquals(SxpNodeIdentity.class, SxpDatastoreNode.getIdentifier(ID).getTargetType());
    }

    @Test
    public void testAddDomain() throws Exception {
        assertTrue(node.addDomain(new SxpDomainBuilder().setDomainName("private").build()));
        assertFalse(node.addDomain(new SxpDomainBuilder().setDomainName("private").build()));
        assertTrue(node.addDomain(new SxpDomainBuilder().setDomainName("test").build()));
    }

    @Test
    public void testAddConnection() throws Exception {
        SxpConnection
                sxpConnection =
                node.addConnection(new ConnectionBuilder().setPeerAddress(IpAddressBuilder.getDefaultInstance("1.1.1.1"))
                        .setTcpPort(new PortNumber(64977))
                        .setMode(ConnectionMode.Both)
                        .setVersion(Version.Version4)
                        .build(), SxpNode.DEFAULT_DOMAIN);
        assertTrue(sxpConnection instanceof SxpDatastoreConnection);
    }

    @Test
    public void testPutBindingsMasterDatabase() throws Exception {
        assertNotNull(node.putBindingsMasterDatabase(Collections.emptyList(), SxpNode.DEFAULT_DOMAIN));
        verify(dispatcher).propagateUpdate(anyList(), anyList(), anyList());
    }

    @Test
    public void testRemoveBindingsMasterDatabase() throws Exception {
        assertNotNull(node.removeBindingsMasterDatabase(Collections.emptyList(), SxpNode.DEFAULT_DOMAIN));
        verify(dispatcher).propagateUpdate(anyList(), anyList(), anyList());
    }

    @Test
    public void testGetDatastoreAccess() throws Exception {
        assertNotNull(node.getDatastoreAccess());
        assertEquals(datastoreAccess, node.getDatastoreAccess());
    }

    @Test
    public void testShutdown() throws Exception {
        node.start().get();
        assertTrue(node.shutdown().get());
        verify(datastoreAccess).close();
    }

    /**
     * Test that it is possible to successfully start/stop the same node more than once.
     */
    @Test
    public void testDoubleShutdown() throws Exception {
        node.start().get();
        assertTrue(node.shutdown().get());
        node.start().get();
        assertTrue(node.shutdown().get());
        verify(datastoreAccess, times(2)).close();
    }

    @Test
    public void testClose() throws Exception {
        final SxpConnection sxpConnection = mock(SxpConnection.class);
        when(sxpConnection.getDomainName()).thenReturn(SxpNode.DEFAULT_DOMAIN);
        when(sxpConnection.getDestination()).thenReturn(InetSocketAddress.createUnresolved("127.0.0.2", 64977));
        when(sxpConnection.openConnection()).thenReturn(new ImmediateCancelledFuture<>());

        node.start().get();
        node.addConnection(sxpConnection);
        verify(sxpConnection).openConnection();
        node.close();
        verify(sxpConnection).shutdown();
    }
}
