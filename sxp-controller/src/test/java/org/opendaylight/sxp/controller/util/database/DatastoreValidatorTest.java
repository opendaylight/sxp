/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.util.database.access.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class) @PrepareForTest({DatastoreAccess.class}) public class DatastoreValidatorTest {

        private static DatastoreAccess access;
        private static DatastoreValidator validator;
        private static CheckedFuture future;

        @BeforeClass public static void init() throws Exception {
                if (validator == null) {
                        access = PowerMockito.mock(DatastoreAccess.class);
                }
                future = mock(CheckedFuture.class);
                Optional optional = mock(Optional.class);
                when(optional.isPresent()).thenReturn(false);
                when(future.get()).thenReturn(optional);
                PowerMockito.when(access.put(any(InstanceIdentifier.class), any(DataObject.class),
                        any(LogicalDatastoreType.class))).thenReturn(future);
                PowerMockito.when(access.read(any(InstanceIdentifier.class), any(LogicalDatastoreType.class)))
                        .thenReturn(future);
                validator = DatastoreValidator.getInstance(access);

        }

        @Test public void testGetInstance() throws Exception {
                DatastoreValidator validator = DatastoreValidator.getInstance(access);
                assertNotNull(validator);
                assertEquals(validator.getDatastoreAccess(),
                        DatastoreValidator.getInstance(PowerMockito.mock(DatastoreAccess.class)).getDatastoreAccess());
        }

        @Test public void testValidateSxpNodePath() throws Exception {
                PowerMockito.when(access.put(any(InstanceIdentifier.class), any(DataObject.class),
                        any(LogicalDatastoreType.class)))
                        .thenThrow(InterruptedException.class)
                        .thenReturn(future)
                        .thenThrow(InterruptedException.class)
                        .thenReturn(future)
                        .thenReturn(future)
                        .thenThrow(InterruptedException.class)
                        .thenReturn(future);
                validator.validateSxpNodePath("TestNode", LogicalDatastoreType.OPERATIONAL);

                ArgumentCaptor<NetworkTopology>
                        networkTopologyArgumentCaptor =
                        ArgumentCaptor.forClass(NetworkTopology.class);
                verify(access, atLeastOnce()).put(any(InstanceIdentifier.class),
                        networkTopologyArgumentCaptor.capture(), any(LogicalDatastoreType.class));
                assertTrue(networkTopologyArgumentCaptor.getValue() instanceof NetworkTopology);

                validator.validateSxpNodePath("TestNode", LogicalDatastoreType.OPERATIONAL);

                ArgumentCaptor<Topology> topologyArgumentCaptor = ArgumentCaptor.forClass(Topology.class);
                verify(access, atLeastOnce()).put(any(InstanceIdentifier.class), topologyArgumentCaptor.capture(),
                        any(LogicalDatastoreType.class));
                assertTrue(topologyArgumentCaptor.getValue() instanceof Topology);
                assertEquals(Configuration.TOPOLOGY_NAME, topologyArgumentCaptor.getValue().getTopologyId().getValue());

                validator.validateSxpNodePath("TestNode", LogicalDatastoreType.OPERATIONAL);

                ArgumentCaptor<Node> nodeArgumentCaptor = ArgumentCaptor.forClass(Node.class);
                verify(access, atLeastOnce()).put(any(InstanceIdentifier.class), nodeArgumentCaptor.capture(),
                        any(LogicalDatastoreType.class));
                assertTrue(nodeArgumentCaptor.getValue() instanceof Node);
                assertEquals("TestNode", nodeArgumentCaptor.getValue().getNodeId().getValue());

                PowerMockito.when(access.put(any(InstanceIdentifier.class), any(DataObject.class),
                        any(LogicalDatastoreType.class))).thenReturn(future);
                validator.validateSxpNodePath("TestNode", LogicalDatastoreType.OPERATIONAL);

                ArgumentCaptor<SxpNodeIdentity> argumentCaptor = ArgumentCaptor.forClass(SxpNodeIdentity.class);
                verify(access, atLeastOnce()).put(any(InstanceIdentifier.class), argumentCaptor.capture(),
                        any(LogicalDatastoreType.class));
                assertTrue(argumentCaptor.getValue() instanceof SxpNodeIdentity);
        }
}
