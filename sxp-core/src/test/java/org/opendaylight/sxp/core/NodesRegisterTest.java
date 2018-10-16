/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

public class NodesRegisterTest {

    @Test
    public void testRegisterNode() {
        SxpNode nodeMock = mock(SxpNode.class);
        String nodeIdString = "127.0.0.1";
        NodeId nodeId = new NodeId(nodeIdString);
        when(nodeMock.getNodeId()).thenReturn(nodeId);
        NodesRegister.register(nodeMock);
        Assert.assertEquals(1, NodesRegister.getNodes().size());
        SxpNode registeredNode = NodesRegister.getRegisteredNode(nodeIdString);
        Assert.assertEquals(registeredNode, nodeMock);
        NodesRegister.unRegister(nodeIdString);
        Assert.assertEquals(0, NodesRegister.getNodes().size());
    }
}