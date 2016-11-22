/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import io.netty.channel.Channel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.Security;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({DatastoreAccess.class, BindingDispatcher.class})
public class SxpDatastoreNodeTest {

    private static String ID = "127.0.0.1";
    private SxpDatastoreNode node;
    private SxpNodeIdentity nodeIdentity;
    private DatastoreAccess datastoreAccess;
    private BindingDispatcher dispatcher;

    @Before public void setUp() throws Exception {
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
        when(nodeIdentity.getTcpPort()).thenReturn(new PortNumber(64999));
        when(nodeIdentity.getSourceIp()).thenReturn(new IpAddress(ID.toCharArray()));
        when(nodeIdentity.getTcpPort()).thenReturn(PortNumber.getDefaultInstance("64999"));
        dispatcher = mock(BindingDispatcher.class);

        node = SxpDatastoreNode.createInstance(NodeIdConv.createNodeId(ID), datastoreAccess, nodeIdentity);
        node.addDomain(new SxpDomainBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN).build());
        PowerMockito.field(SxpNode.class, "serverChannel").set(node, mock(Channel.class));
        PowerMockito.field(SxpNode.class, "svcBindingDispatcher").set(node, dispatcher);
    }

    @Test public void testGetIdentifier() throws Exception {
        assertEquals(SxpNodeIdentity.class, SxpDatastoreNode.getIdentifier(ID).getTargetType());
    }

    @Test public void testAddDomain() throws Exception {
        assertTrue(node.addDomain(new SxpDomainBuilder().setDomainName("private").build()));
        assertFalse(node.addDomain(new SxpDomainBuilder().setDomainName("private").build()));
        assertTrue(node.addDomain(new SxpDomainBuilder().setDomainName("test").build()));
    }

    @Test public void testGetNodeIdentity() throws Exception {
        assertNotNull(node.getNodeIdentity());
        verify(datastoreAccess).readSynchronous(eq(SxpDatastoreNode.getIdentifier(ID)),
                any(LogicalDatastoreType.class));
    }

    @Test public void testSetPassword() throws Exception {
        ArgumentCaptor<Security> captor = ArgumentCaptor.forClass(Security.class);
        assertEquals("test", node.setPassword(new SecurityBuilder().setPassword("test").build()).getPassword());

        verify(datastoreAccess, atLeastOnce()).checkAndMerge(any(InstanceIdentifier.class), captor.capture(),
                any(LogicalDatastoreType.class), anyBoolean());
        assertEquals("test", captor.getValue().getPassword());
    }

    @Test public void testAddConnection() throws Exception {
        SxpConnection
                sxpConnection =
                node.addConnection(new ConnectionBuilder().setPeerAddress(new IpAddress("1.1.1.1".toCharArray()))
                        .setTcpPort(new PortNumber(64999))
                        .setMode(ConnectionMode.Both)
                        .setVersion(Version.Version4)
                        .build(), SxpNode.DEFAULT_DOMAIN);
        assertTrue(sxpConnection instanceof SxpDatastoreConnection);
    }

    @Test public void testPutLocalBindingsMasterDatabase() throws Exception {
        assertNotNull(node.putLocalBindingsMasterDatabase(Collections.emptyList(), SxpNode.DEFAULT_DOMAIN));
        verify(dispatcher).propagateUpdate(anyList(), anyList(), anyList());
    }

    @Test public void testRemoveLocalBindingsMasterDatabase() throws Exception {
        assertNotNull(node.removeLocalBindingsMasterDatabase(Collections.emptyList(), SxpNode.DEFAULT_DOMAIN));
        verify(dispatcher).propagateUpdate(anyList(), anyList(), anyList());
    }

    @Test public void testGetDatastoreAccess() throws Exception {
        assertNotNull(node.getDatastoreAccess());
        assertEquals(datastoreAccess, node.getDatastoreAccess());
    }

    @Test public void testShutdown() throws Exception {
        node.shutdown();
        verify(datastoreAccess, atLeastOnce()).close();
    }

    @Test public void testClose() throws Exception {
        SxpConnection
                connection =
                node.addConnection(new ConnectionBuilder().setPeerAddress(new IpAddress("1.1.1.1".toCharArray()))
                        .setTcpPort(new PortNumber(64999))
                        .setMode(ConnectionMode.Both)
                        .setState(ConnectionState.On)
                        .setVersion(Version.Version4)
                        .build(), SxpNode.DEFAULT_DOMAIN);
        node.close();
        verify(datastoreAccess, atLeastOnce()).close();
        assertTrue(connection.isStateOff());
    }
}
