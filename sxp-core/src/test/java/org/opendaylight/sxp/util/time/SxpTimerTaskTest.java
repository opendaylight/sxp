/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.time.connection.DeleteHoldDownTimerTask;
import org.opendaylight.sxp.util.time.connection.HoldTimerTask;
import org.opendaylight.sxp.util.time.connection.KeepAliveTimerTask;
import org.opendaylight.sxp.util.time.connection.ReconcilationTimerTask;
import org.opendaylight.sxp.util.time.connection.RetryOpenTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.TimerType;

public class SxpTimerTaskTest {

    private static SxpNode sxpNode;
    private static SxpConnection sxpConnection;

    @Before
    public void init() {
        sxpNode = mock(SxpNode.class);
        sxpConnection = mock(SxpConnection.class);
        List<SxpConnection> connections = new ArrayList<>();
        connections.add(sxpConnection);
        when(sxpNode.getAllConnections()).thenReturn(connections);
        when(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.SPEAKER_CNTXT)).thenReturn(true);
        when(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.LISTENER_CNTXT)).thenReturn(true);
        when(sxpConnection.isStateOn()).thenReturn(true);
        when(sxpConnection.isModeSpeaker()).thenReturn(true);
        when(sxpConnection.isModeListener()).thenReturn(true);
        when(sxpConnection.isVersion4()).thenReturn(true);
    }

    @Test
    public void testRetryOpenTimerTask() throws Exception {
        when(sxpNode.isEnabled()).thenReturn(true).thenReturn(false);
        RetryOpenTimerTask timerTask = new RetryOpenTimerTask(sxpConnection);
        timerTask.call();
        verify(sxpConnection).openConnection();
    }

    @Test
    public void testKeepAliveTimerTask() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        when(sxpConnection.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.SPEAKER_CNTXT)).thenReturn(
                ctx);
        KeepAliveTimerTask timerTask = new KeepAliveTimerTask(sxpConnection, 0);

        when(sxpConnection.getTimestampUpdateOrKeepAliveMessage()).thenReturn(0l);
        timerTask.call();
        verify(ctx).writeAndFlush(any());
        verify(sxpConnection).setTimer(TimerType.KeepAliveTimer, timerTask.getPeriod());

        when(sxpConnection.getTimestampUpdateOrKeepAliveMessage()).thenReturn(2 * System.currentTimeMillis());
        timerTask.call();
        verify(ctx, times(2)).writeAndFlush(any());
        verify(sxpConnection, times(2)).setTimer(TimerType.KeepAliveTimer, timerTask.getPeriod());

        when(sxpConnection.isVersion4()).thenReturn(false);
        timerTask.call();
        verifyNoMoreInteractions(ctx);
        verify(sxpConnection, times(2)).setTimer(TimerType.KeepAliveTimer, timerTask.getPeriod());

        when(sxpConnection.isVersion4()).thenReturn(true);
        when(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.SPEAKER_CNTXT)).thenReturn(false);
        timerTask.call();
        verifyNoMoreInteractions(ctx);
        verify(sxpConnection, times(2)).setTimer(TimerType.KeepAliveTimer, timerTask.getPeriod());

        when(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.SPEAKER_CNTXT)).thenReturn(true);
        when(sxpConnection.isModeSpeaker()).thenReturn(Boolean.FALSE);
        timerTask.call();
        verifyNoMoreInteractions(ctx);
        verify(sxpConnection, times(2)).setTimer(TimerType.KeepAliveTimer, timerTask.getPeriod());

        when(sxpConnection.isVersion4()).thenReturn(Boolean.FALSE);
        timerTask.call();
        verifyNoMoreInteractions(ctx);
        verify(sxpConnection, times(2)).setTimer(TimerType.KeepAliveTimer, timerTask.getPeriod());
    }

    @Test
    public void testKeepAliveTimerTaskErrorHandling() throws Exception {
        KeepAliveTimerTask timerTask = new KeepAliveTimerTask(sxpConnection, 0);
        when(sxpConnection.getChannelHandlerContext(any())).thenThrow(ChannelHandlerContextNotFoundException.class);
        timerTask.call();
        verify(sxpConnection).setTimer(TimerType.KeepAliveTimer, timerTask.getPeriod());
    }

    @Test
    public void testHoldTimerTask() throws Exception {
        HoldTimerTask timerTask = new HoldTimerTask(sxpConnection, 0);
        when(sxpConnection.getTimestampUpdateOrKeepAliveMessage()).thenReturn(2 * System.currentTimeMillis());

        timerTask.call();
        verify(sxpConnection).setTimer(TimerType.HoldTimer, timerTask.getPeriod());

        when(sxpConnection.isStateOn()).thenReturn(false);
        timerTask.call();
        verify(sxpConnection, times(1)).setTimer(TimerType.HoldTimer, timerTask.getPeriod());

        when(sxpConnection.isStateOn()).thenReturn(true);
        when(sxpConnection.isModeListener()).thenReturn(Boolean.FALSE);
        timerTask.call();
        verify(sxpConnection, times(1)).setTimer(TimerType.HoldTimer, timerTask.getPeriod());

        when(sxpConnection.isModeListener()).thenReturn(Boolean.TRUE);
        when(sxpConnection.isVersion4()).thenReturn(Boolean.FALSE);
        timerTask.call();
        verify(sxpConnection, times(1)).setTimer(TimerType.HoldTimer, timerTask.getPeriod());

        when(sxpConnection.isStateOn()).thenReturn(true);
        when(sxpConnection.isVersion4()).thenReturn(true);
        when(sxpConnection.getTimestampUpdateOrKeepAliveMessage()).thenReturn(0l);
        when(sxpConnection.getChannelHandlerContext(any(SxpConnection.ChannelHandlerContextType.class))).thenReturn(
                mock(ChannelHandlerContext.class));
        timerTask.call();
        verify(sxpConnection).setDeleteHoldDownTimer();
    }

    @Test
    public void testHoldTimerTaskErrorHandling() throws Exception {
        HoldTimerTask timerTask = new HoldTimerTask(sxpConnection, 0);
        when(sxpConnection.getChannelHandlerContext(Mockito.eq(SxpConnection.ChannelHandlerContextType.LISTENER_CNTXT)))
                .thenThrow(ChannelHandlerContextDiscrepancyException.class);
        timerTask.call();
        verify(sxpConnection, times(0)).setDeleteHoldDownTimer();
    }

    @Test
    public void testDeleteHoldDOwnTimerTask() throws Exception {
        when(sxpConnection.isStateDeleteHoldDown()).thenReturn(true);
        DeleteHoldDownTimerTask timerTask = new DeleteHoldDownTimerTask(sxpConnection, 0);

        timerTask.call();
        verify(sxpConnection).purgeBindings();

        when(sxpConnection.isStateDeleteHoldDown()).thenReturn(false);
        timerTask.call();
        verify(sxpConnection).purgeBindings();

        when(sxpConnection.isStatePendingOn()).thenReturn(Boolean.TRUE);
        timerTask.call();
        verify(sxpConnection, times(2)).purgeBindings();
    }

    @Test
    public void testReconciliationTimerTask() throws Exception {
        ReconcilationTimerTask timerTask = new ReconcilationTimerTask(sxpConnection, 0);

        timerTask.call();
        verify(sxpConnection).cleanUpBindings();

        when(sxpConnection.isStateOn()).thenReturn(false);
        timerTask.call();
        verify(sxpConnection).cleanUpBindings();
    }

}
