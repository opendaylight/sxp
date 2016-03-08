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
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class}) public class BindingDispatcherTest {

        @Rule public ExpectedException exception = ExpectedException.none();
        private static SxpNode sxpNode;
        private static BindingDispatcher dispatcher;
        private static ThreadsWorker worker;
        private static MasterDatabase databaseProvider;
        private static List<SxpConnection> sxpConnections;

        private SxpConnection mockConnection(boolean updateExported, boolean updateAll) {
                SxpConnection connection = mock(SxpConnection.class);
                when(connection.getVersion()).thenReturn(Version.Version4);
                when(connection.getOwner()).thenReturn(sxpNode);
                return connection;
        }

        @Before public void init() throws Exception {
                databaseProvider = mock(MasterDatabase.class);
                List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase> masterDatabases = new ArrayList<>();
                masterDatabases.add(mock(
                        org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase.class));
                /*when(databaseProvider.partition(anyInt(), anyBoolean(), any(SxpBindingFilter.class))).thenReturn(
                        masterDatabases);*/
                worker = mock(ThreadsWorker.class);
                sxpNode = PowerMockito.mock(SxpNode.class);
                PowerMockito.when(sxpNode.getWorker()).thenReturn(worker);
                PowerMockito.when(sxpNode.isEnabled()).thenReturn(true);
                PowerMockito.when(sxpNode.getExpansionQuantity()).thenReturn(50);
                PowerMockito.when(sxpNode.getBindingMasterDatabase()).thenReturn(databaseProvider);
                sxpConnections = new ArrayList<>();
                PowerMockito.when(sxpNode.getAllOnSpeakerConnections()).thenReturn(sxpConnections);
                dispatcher = new BindingDispatcher(sxpNode);
        }

        @Test public void testCall() throws Exception {
                SxpConnection connection = mockConnection(false, false);
                sxpConnections.add(mockConnection(false, true));
                sxpConnections.add(connection);

                verify(worker).executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class));

                verify(worker).executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class));
        }

        @Test public void testSetPartitionSize() throws Exception {
                dispatcher.setPartitionSize(25);
                exception.expect(IllegalArgumentException.class);
                dispatcher.setPartitionSize(-10);
        }
}
