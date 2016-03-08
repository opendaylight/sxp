/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class}) public class BindingDispatcherTest {

        @Rule public ExpectedException exception = ExpectedException.none();
        private static SxpNode sxpNode;
        private static BindingDispatcher dispatcher;
        private static ThreadsWorker worker;
        private static List<SxpConnection> sxpConnections;

        private SxpConnection mockConnection(Version version) {
                SxpConnection connection = mock(SxpConnection.class);
                when(connection.isStateOn()).thenReturn(true);
                when(connection.isModeSpeaker()).thenReturn(true);
                when(connection.getVersion()).thenReturn(version);
                when(connection.getOwner()).thenReturn(sxpNode);
                when(connection.getCapabilitiesRemote()).thenReturn(
                        Configuration.getCapabilities(version).getCapability());
                return connection;
        }

        private <T extends SxpBindingFields> List<T> getBindings(String... strings) {
                List<T> bindings = new ArrayList<>();
                MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
                for (String s : strings) {
                        bindings.add((T) bindingBuilder.setIpPrefix(new IpPrefix(s.toCharArray())).build());
                }
                return bindings;
        }

        @Before public void init() throws Exception {
                worker = mock(ThreadsWorker.class);
                sxpNode = PowerMockito.mock(SxpNode.class);
                PowerMockito.when(sxpNode.getWorker()).thenReturn(worker);
                PowerMockito.when(sxpNode.isEnabled()).thenReturn(true);
                PowerMockito.when(sxpNode.getExpansionQuantity()).thenReturn(50);
                sxpConnections = new ArrayList<>();
                PowerMockito.when(sxpNode.getAllOnSpeakerConnections()).thenReturn(sxpConnections);
                dispatcher = new BindingDispatcher(sxpNode);
        }

        @Test public void testSetPartitionSize() throws Exception {
                dispatcher.setPartitionSize(25);
                exception.expect(IllegalArgumentException.class);
                dispatcher.setPartitionSize(-10);
        }

        @Test public void testPartitionBindings() throws Exception {
                dispatcher.setPartitionSize(5);
                List
                        partitions =
                        dispatcher.partitionBindings(mockConnection(Version.Version4),
                                getBindings("1.1.1.1/32", "2.2.2.2/32", "3.3.3.3/32", "4.4.4.0/24"),
                                getBindings("5.5.5.5/32", "6.6.6.6/32", "7.7.7.7/32", "8.8.8.0/24"));
                assertEquals(2, partitions.size());

                partitions =
                        dispatcher.partitionBindings(mockConnection(Version.Version4),
                                getBindings("1.1.1.1/32", "2.2.2.2/32"),
                                getBindings("5.5.5.5/32", "6.6.6.6/32", "7.7.7.7/32"));
                assertEquals(1, partitions.size());

                partitions =
                        dispatcher.partitionBindings(mockConnection(Version.Version3),
                                getBindings("1.1.1.1/32", "2.2.2.2/32", "3.3.3.3/32", "4.4.4.0/24"),
                                getBindings("5.5.5.5/32", "6.6.6.6/32", "7.7.7.7/32", "8.8.8.0/24"));
                assertEquals(2, partitions.size());

                partitions =
                        dispatcher.partitionBindings(mockConnection(Version.Version2),
                                getBindings("1.1.1.1/32", "2.2.2.2/32", "3.3.3.3/32", "4.4.4.0/24"),
                                getBindings("5.5.5.5/32", "6.6.6.6/32", "7.7.7.7/32", "8.8.8.0/24"));
                assertEquals(22, partitions.size());

                partitions =
                        dispatcher.partitionBindings(mockConnection(Version.Version2),
                                getBindings("1.1.1.1/32", "2.2.2.2/32", "3.3.3.3/32", "4.4.4.0/24"), null);
                assertEquals(11, partitions.size());

                partitions =
                        dispatcher.partitionBindings(mockConnection(Version.Version2), null,
                                getBindings("5.5.5.5/32", "6.6.6.6/32", "7.7.7.7/32", "8.8.8.0/24"));
                assertEquals(11, partitions.size());

                partitions = dispatcher.partitionBindings(mockConnection(Version.Version2), null, null);
                assertEquals(0, partitions.size());
        }

        @Test public void testPropagateUpdate() throws Exception {
                dispatcher.setPartitionSize(5);
                List<SxpConnection> sxpConnections = new ArrayList<>();
                sxpConnections.add(mockConnection(Version.Version1));
                sxpConnections.add(mockConnection(Version.Version2));
                sxpConnections.add(mockConnection(Version.Version3));
                sxpConnections.add(mockConnection(Version.Version4));

                dispatcher.propagateUpdate(null, null, null);
                verify(worker, never()).executeTaskInSequence(any(Callable.class),
                        eq(ThreadsWorker.WorkerType.OUTBOUND), any(SxpConnection.class));

                dispatcher.propagateUpdate(null, null, sxpConnections);
                verify(worker, never()).executeTaskInSequence(any(Callable.class),
                        eq(ThreadsWorker.WorkerType.OUTBOUND), any(SxpConnection.class));

                dispatcher.propagateUpdate(new ArrayList<>(), new ArrayList<>(), sxpConnections);
                verify(worker, never()).executeTaskInSequence(any(Callable.class),
                        eq(ThreadsWorker.WorkerType.OUTBOUND), any(SxpConnection.class));

                dispatcher.propagateUpdate(getBindings("1.1.1.1/32", "2.2.2.2/32", "3.3.3.3/32", "4.4.4.0/24"),
                        getBindings("5.5.5.5/32", "6.6.6.6/32", "7.7.7.7/32", "8.8.8.0/24"), sxpConnections);
                verify(worker, times(4)).executeTaskInSequence(any(Callable.class),
                        eq(ThreadsWorker.WorkerType.OUTBOUND), any(SxpConnection.class));
        }

        @Test public void testSendPurgeAllMessage() throws Exception {
                SxpConnection connection = mockConnection(Version.Version4);
                BindingDispatcher.sendPurgeAllMessage(connection);
                verify(worker).executeTaskInSequence(any(Callable.class), eq(ThreadsWorker.WorkerType.OUTBOUND),
                        eq(connection));
        }
}
