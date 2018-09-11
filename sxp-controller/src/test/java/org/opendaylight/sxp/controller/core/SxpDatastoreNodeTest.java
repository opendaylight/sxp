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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.Security;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

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
        when(DatastoreAccess.getInstance(any(DataBroker.class))).thenReturn(datastoreAccess);
        when(DatastoreAccess.getInstance(any(DatastoreAccess.class))).thenReturn(datastoreAccess);
        when(nodeIdentity.getVersion()).thenReturn(Version.Version4);
        Security security = mock(Security.class);
        when(security.getPassword()).thenReturn("default");
        when(nodeIdentity.getName()).thenReturn("NAME");
        when(nodeIdentity.getSecurity()).thenReturn(security);
        when(nodeIdentity.getMappingExpanded()).thenReturn(150);
        when(nodeIdentity.getTcpPort()).thenReturn(new PortNumber(64999));
        when(nodeIdentity.getSourceIp()).thenReturn(IpAddressBuilder.getDefaultInstance(ID));
        when(nodeIdentity.getTcpPort()).thenReturn(PortNumber.getDefaultInstance("64999"));
        dispatcher = mock(BindingDispatcher.class);

        node = SxpDatastoreNode.createInstance(NodeIdConv.createNodeId(ID), datastoreAccess, nodeIdentity);
        node.addDomain(new SxpDomainBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN).build());
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
                        .setTcpPort(new PortNumber(64999))
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
        node.shutdown();
        verify(datastoreAccess, atLeastOnce()).close();
    }

    @Test
    public void testClose() throws Exception {
        SxpConnection
                connection =
                node.addConnection(new ConnectionBuilder().setPeerAddress(IpAddressBuilder.getDefaultInstance("1.1.1.1"))
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
