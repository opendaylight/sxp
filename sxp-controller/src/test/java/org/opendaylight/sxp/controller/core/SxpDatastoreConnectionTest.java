/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.capabilities.fields.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class, DatastoreAccess.class})
public class SxpDatastoreConnectionTest {

    private DatastoreAccess datastoreAccess;
    private SxpDatastoreConnection connection;
    private static SxpNode sxpNode;
    private static ThreadsWorker worker;

    @Before public void setUp() throws Exception {
        worker = mock(ThreadsWorker.class);
        when(worker.scheduleTask(any(Callable.class), anyInt(), any(TimeUnit.class))).thenReturn(
                mock(ListenableScheduledFuture.class));
        when(worker.executeTask(any(Runnable.class), any(ThreadsWorker.WorkerType.class))).thenReturn(
                mock(ListenableFuture.class));
        when(worker.executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class))).thenReturn(
                mock(ListenableFuture.class));
        sxpNode = PowerMockito.mock(SxpNode.class);
        datastoreAccess = mock(DatastoreAccess.class);
        PowerMockito.when(sxpNode.getWorker()).thenReturn(worker);
        connection =
                SxpDatastoreConnection.create(datastoreAccess, sxpNode,
                        new ConnectionBuilder().setPeerAddress(new IpAddress("127.0.0.1".toCharArray()))
                                .setTcpPort(new PortNumber(64999))
                                .setMode(ConnectionMode.None)
                                .setState(ConnectionState.On)
                                .setVersion(Version.Version2)
                                .build(), "test");
    }

    @Test public void testSetTimers() throws Exception {
        ConnectionTimers
                timers =
                new ConnectionTimersBuilder().setHoldTime(60).setHoldTimeMax(180).setHoldTimeMinAcceptable(90).build();
        connection.setTimers(timers);
        assertEquals(60, connection.getHoldTime());
        assertEquals(180, connection.getHoldTimeMax());
        assertEquals(90, connection.getHoldTimeMinAcceptable());
        verify(datastoreAccess, atLeastOnce()).checkAndPut(any(InstanceIdentifier.class), eq(timers),
                any(LogicalDatastoreType.class), anyBoolean());
    }

    @Test public void testSetCapabilities() throws Exception {
        Capabilities capabilities = Configuration.getCapabilities(Version.Version4);
        connection.setCapabilities(capabilities);
        assertFalse(connection.getCapabilitiesRemote().contains(CapabilityType.Ipv4Unicast));
        assertFalse(connection.getCapabilitiesRemote().contains(CapabilityType.Ipv6Unicast));
        assertFalse(connection.getCapabilitiesRemote().contains(CapabilityType.LoopDetection));
        assertFalse(connection.getCapabilitiesRemote().contains(CapabilityType.SubnetBindings));
        assertFalse(connection.getCapabilitiesRemote().contains(CapabilityType.SxpCapabilityExchange));
        verify(datastoreAccess, atLeastOnce()).checkAndPut(any(InstanceIdentifier.class), eq(capabilities),
                any(LogicalDatastoreType.class), anyBoolean());
    }

    @Test public void testSetVersion() throws Exception {
        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        connection.setVersion(Version.Version1);
        assertEquals(Version.Version1, connection.getVersion());
        verify(datastoreAccess, atLeastOnce()).checkAndMerge(any(InstanceIdentifier.class), captor.capture(),
                any(LogicalDatastoreType.class), anyBoolean());
        assertEquals(Version.Version1, captor.getValue().getVersion());
    }

    @Test public void testSetState() throws Exception {
        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        connection.setState(ConnectionState.DeleteHoldDown);
        assertEquals(ConnectionState.DeleteHoldDown, connection.getState());
        verify(datastoreAccess, atLeastOnce()).checkAndMerge(any(InstanceIdentifier.class), captor.capture(),
                any(LogicalDatastoreType.class), anyBoolean());
        assertEquals(ConnectionState.DeleteHoldDown, captor.getValue().getState());
    }

    @Test public void testSetNodeIdRemote() throws Exception {
        NodeId nodeId = new NodeId("1.1.1.1");
        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        connection.setNodeIdRemote(nodeId);
        assertEquals(nodeId, connection.getNodeIdRemote());
        verify(datastoreAccess, atLeastOnce()).checkAndMerge(any(InstanceIdentifier.class), captor.capture(),
                any(LogicalDatastoreType.class), anyBoolean());
        assertEquals(nodeId, captor.getValue().getNodeId());
    }

    @Test public void testSetUpdateOrKeepaliveMessageTimestamp() throws Exception {
        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        connection.setUpdateOrKeepaliveMessageTimestamp();
        verify(datastoreAccess, atLeastOnce()).checkAndMerge(any(InstanceIdentifier.class), captor.capture(),
                any(LogicalDatastoreType.class), anyBoolean());
        assertEquals(connection.getTimestampUpdateOrKeepAliveMessage(),
                TimeConv.toLong(captor.getValue().getTimestampUpdateOrKeepAliveMessage()));
    }

    @Test public void testShutdown() throws Exception {
        connection.shutdown();
        verify(datastoreAccess, atLeastOnce()).readSynchronous(any(InstanceIdentifier.class),
                any(LogicalDatastoreType.class));
    }
}
