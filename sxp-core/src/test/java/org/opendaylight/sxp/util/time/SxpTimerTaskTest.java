/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time;

import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.time.connection.DeleteHoldDownTimerTask;
import org.opendaylight.sxp.util.time.connection.HoldTimerTask;
import org.opendaylight.sxp.util.time.connection.KeepAliveTimerTask;
import org.opendaylight.sxp.util.time.connection.ReconcilationTimerTask;
import org.opendaylight.sxp.util.time.node.RetryOpenTimerTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.TimerType;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class}) public class SxpTimerTaskTest {

        private static SxpNode sxpNode;
        private static SxpConnection sxpConnection;

        @Before public void init() {
                sxpNode = PowerMockito.mock(SxpNode.class);
                sxpConnection = mock(SxpConnection.class);
                List<SxpConnection> connections = new ArrayList<>();
                connections.add(sxpConnection);
                PowerMockito.when(sxpNode.getAllOffConnections()).thenReturn(connections);
                when(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.SpeakerContext)).thenReturn(true);
                when(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.ListenerContext)).thenReturn(true);
                when(sxpConnection.isStateOn()).thenReturn(true);
                when(sxpConnection.isModeSpeaker()).thenReturn(true);
                when(sxpConnection.isModeListener()).thenReturn(true);
                when(sxpConnection.isVersion4()).thenReturn(true);
        }

        @Test public void testRetryOpenTimerTask() throws Exception {
                PowerMockito.when(sxpNode.isEnabled()).thenReturn(true).thenReturn(false);
                RetryOpenTimerTask timerTask = new RetryOpenTimerTask(sxpNode, 0);
                timerTask.call();
                verify(sxpNode).openConnections();
                verify(sxpNode).setTimer(TimerType.RetryOpenTimer, timerTask.getPeriod());
                timerTask.call();
                verify(sxpNode, times(1)).openConnections();
        }

        @Test public void testKeepAliveTimerTask() throws Exception {
                ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
                when(sxpConnection.getChannelHandlerContext(
                        SxpConnection.ChannelHandlerContextType.SpeakerContext)).thenReturn(ctx);
                KeepAliveTimerTask timerTask = new KeepAliveTimerTask(sxpConnection, 0);

                when(sxpConnection.getTimestampUpdateOrKeepAliveMessage()).thenReturn(0l);
                timerTask.call();
                verify(ctx).writeAndFlush(any());
                verify(sxpConnection).setTimer(TimerType.KeepAliveTimer, timerTask.getPeriod());

                when(sxpConnection.getTimestampUpdateOrKeepAliveMessage()).thenReturn(2 * System.currentTimeMillis());
                timerTask.call();
                verifyNoMoreInteractions(ctx);
                verify(sxpConnection, times(2)).setTimer(TimerType.KeepAliveTimer, timerTask.getPeriod());

                when(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.SpeakerContext)).thenReturn(false);
                timerTask.call();
                verifyNoMoreInteractions(ctx);
                verify(sxpConnection, times(2)).setTimer(TimerType.KeepAliveTimer, timerTask.getPeriod());
        }

        @Test public void testHoldTimerTask() throws Exception {
                HoldTimerTask timerTask = new HoldTimerTask(sxpConnection, 0);
                when(sxpConnection.getTimestampUpdateOrKeepAliveMessage()).thenReturn(2 * System.currentTimeMillis());

                timerTask.call();
                verify(sxpConnection).setTimer(TimerType.HoldTimer, timerTask.getPeriod());

                when(sxpConnection.isStateOn()).thenReturn(false);
                timerTask.call();
                verify(sxpConnection, times(1)).setTimer(TimerType.HoldTimer, timerTask.getPeriod());

                when(sxpConnection.isStateOn()).thenReturn(true);
                when(sxpConnection.getTimestampUpdateOrKeepAliveMessage()).thenReturn(0l);
                when(sxpConnection.getChannelHandlerContext(any(SxpConnection.ChannelHandlerContextType.class))).thenReturn(mock(ChannelHandlerContext.class));
                timerTask.call();
                verify(sxpConnection).setDeleteHoldDownTimer();

        }

        @Test public void testDeleteHoldDOwnTimerTask() throws Exception {
                when(sxpConnection.isStateDeleteHoldDown()).thenReturn(true);
                DeleteHoldDownTimerTask timerTask = new DeleteHoldDownTimerTask(sxpConnection, 0);

                timerTask.call();
                verify(sxpConnection).purgeBindings();

                when(sxpConnection.isStateDeleteHoldDown()).thenReturn(false);
                timerTask.call();
                verify(sxpConnection).purgeBindings();
        }

        @Test public void testReconciliationTimerTask() throws Exception {
                ReconcilationTimerTask timerTask = new ReconcilationTimerTask(sxpConnection, 0);

                timerTask.call();
                verify(sxpConnection).cleanUpBindings();

                when(sxpConnection.isStateOn()).thenReturn(false);
                timerTask.call();
                verify(sxpConnection).cleanUpBindings();
        }

}
