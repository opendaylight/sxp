/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

public class UpdateExportTaskTest {

    private static SxpConnection connection;
    private static ByteBuf[] byteBuffs;
    private static BiFunction<SxpConnection, SxpBindingFilter, ByteBuf>[] partitions;
    private static AtomicInteger atomicInteger;
    private static UpdateExportTask exportTask;

    @Before
    public void init() throws Exception {
        connection = mock(SxpConnection.class);
        when(connection.getVersion()).thenReturn(Version.Version4);
        ByteBuf byteBuf = mock(ByteBuf.class);
        when(byteBuf.duplicate()).thenReturn(byteBuf);
        when(byteBuf.capacity()).thenReturn(10);
        byteBuffs = new ByteBuf[] {byteBuf};
        partitions = new BiFunction[] {(c, f) -> byteBuf};
        atomicInteger = new AtomicInteger(1);
        exportTask = new UpdateExportTask(connection, byteBuffs, partitions, atomicInteger);

    }

    @Test
    public void testFreeReferences() throws Exception {
        exportTask.freeReferences();
        assertEquals(0, atomicInteger.get());
        verify(byteBuffs[0]).release();

        atomicInteger = new AtomicInteger(2);

        exportTask = new UpdateExportTask(connection, byteBuffs, partitions, atomicInteger);
        exportTask.freeReferences();
        assertEquals(1, atomicInteger.get());
        verify(byteBuffs[0]).release();
    }

    @Test
    public void testCall() throws Exception {
        exportTask = new UpdateExportTask(connection, new ByteBuf[1], partitions, atomicInteger);
        when(connection.getChannelHandlerContext(any(SxpConnection.ChannelHandlerContextType.class))).thenReturn(
                mock(ChannelHandlerContext.class));
        exportTask.call();
        verify(connection).setUpdateOrKeepaliveMessageTimestamp();

        when(connection.getChannelHandlerContext(any(SxpConnection.ChannelHandlerContextType.class))).thenThrow(
                new ChannelHandlerContextNotFoundException());
        exportTask.call();
        verify(connection).setUpdateOrKeepaliveMessageTimestamp();
    }

    @Test
    public void testCallWithNullMessage() {
        partitions[0] = (t, u) -> null;
        new UpdateExportTask(connection, byteBuffs, partitions, atomicInteger).call();
        verifyNoMoreInteractions(byteBuffs);
    }
}
