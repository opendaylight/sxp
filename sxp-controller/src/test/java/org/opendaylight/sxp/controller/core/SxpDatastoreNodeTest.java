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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.Security;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({DatastoreAccess.class}) public class SxpDatastoreNodeTest {

    private static String ID = "127.0.0.1";
    private SxpDatastoreNode node;
    private SxpNodeIdentity nodeIdentity;
    private DatastoreAccess datastoreAccess;

    @Before public void setUp() throws Exception {
        nodeIdentity = mock(SxpNodeIdentity.class);
        datastoreAccess = mock(DatastoreAccess.class);
        when(nodeIdentity.getVersion()).thenReturn(Version.Version4);
        Security security = mock(Security.class);
        when(security.getPassword()).thenReturn("default");
        when(nodeIdentity.getName()).thenReturn("NAME");
        when(nodeIdentity.getSecurity()).thenReturn(security);
        when(nodeIdentity.getMappingExpanded()).thenReturn(150);
        when(nodeIdentity.getTcpPort()).thenReturn(new PortNumber(64999));
        when(nodeIdentity.getSourceIp()).thenReturn(new IpAddress(ID.toCharArray()));
        when(nodeIdentity.getTcpPort()).thenReturn(PortNumber.getDefaultInstance("64999"));

        node = SxpDatastoreNode.createInstance(NodeIdConv.createNodeId(ID), datastoreAccess, nodeIdentity);
        PowerMockito.field(SxpNode.class, "serverChannel").set(node, mock(Channel.class));
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
}
