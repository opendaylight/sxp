/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.behavior.Context;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({Context.class}) public class UpdateExportTaskTest {

        private static SxpConnection connection;
        private static ByteBuf[] byteBuffs;
        private static MasterDatabase[] masterDatabases;
        private static AtomicInteger atomicInteger;
        private static UpdateExportTask exportTask;

        @Before public void init() throws Exception {
                connection = mock(SxpConnection.class);
                Context context = PowerMockito.mock(Context.class);
                when(connection.getContext()).thenReturn(context);
                ByteBuf byteBuf = mock(ByteBuf.class);
                when(byteBuf.duplicate()).thenReturn(byteBuf);
                PowerMockito.when(
                        context.executeUpdateMessageStrategy(any(SxpConnection.class), any(MasterDatabase.class)))
                        .thenReturn(byteBuf);
                byteBuffs = new ByteBuf[] {byteBuf};
                masterDatabases = new MasterDatabase[] {mock(MasterDatabase.class)};
                atomicInteger = new AtomicInteger(1);
                exportTask = new UpdateExportTask(connection, byteBuffs, masterDatabases, atomicInteger);

        }

        @Test public void testFreeReferences() throws Exception {
                exportTask.freeReferences();
                assertEquals(0, atomicInteger.get());
                verify(byteBuffs[0]).release();

                atomicInteger = new AtomicInteger(2);

                exportTask = new UpdateExportTask(connection, byteBuffs, masterDatabases, atomicInteger);
                exportTask.freeReferences();
                assertEquals(1, atomicInteger.get());
                verify(byteBuffs[0]).release();

        }

        @Test public void testCall() throws Exception {
                when(connection.getChannelHandlerContext(
                        any(SxpConnection.ChannelHandlerContextType.class))).thenReturn(
                        mock(ChannelHandlerContext.class));
                exportTask.call();
                verify(connection).setUpdateMessageExportTimestamp();
                verify(connection).setUpdateAllExported();

                when(connection.getChannelHandlerContext(any(SxpConnection.ChannelHandlerContextType.class))).thenThrow(
                        new ChannelHandlerContextNotFoundException());
                exportTask.call();
                verify(connection).resetUpdateExported();
        }
}
