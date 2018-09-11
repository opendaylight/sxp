/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.capabilities.fields.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

public class ConfigurationTest {

    @Test
    public void testGetCapabilities() throws Exception {
        Capabilities capabilities = Configuration.getCapabilities(Version.Version1);
        assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv4Unicast));

        capabilities = Configuration.getCapabilities(Version.Version2);
        assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv4Unicast));
        assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv6Unicast));

        capabilities = Configuration.getCapabilities(Version.Version3);
        assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv4Unicast));
        assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv6Unicast));
        assertTrue(capabilities.getCapability().contains(CapabilityType.SubnetBindings));

        capabilities = Configuration.getCapabilities(Version.Version4);
        assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv4Unicast));
        assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv6Unicast));
        assertTrue(capabilities.getCapability().contains(CapabilityType.SubnetBindings));
        assertTrue(capabilities.getCapability().contains(CapabilityType.SxpCapabilityExchange));
        assertTrue(capabilities.getCapability().contains(CapabilityType.LoopDetection));
    }

    @Test
    public void testRegisterNode() {
        SxpNode nodeMock = mock(SxpNode.class);
        String nodeIdString = "127.0.0.1";
        NodeId nodeId = new NodeId(nodeIdString);
        when(nodeMock.getNodeId()).thenReturn(nodeId);
        Configuration.register(nodeMock);
        Assert.assertEquals(1, Configuration.getNodes().size());
        SxpNode registeredNode = Configuration.getRegisteredNode(nodeIdString);
        Assert.assertEquals(registeredNode, nodeMock);
        Configuration.unRegister(nodeIdString);
        Assert.assertEquals(0, Configuration.getNodes().size());
    }

}
