/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time;

import java.util.Timer;

import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.time.Synchronizer.Command;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedTimer {
    private static final Logger LOG = LoggerFactory.getLogger(ManagedTimer.class.getName());

    public static final int TIME_FACTOR = 1000;

    public static void main(String[] args) throws InterruptedException {
        ManagedTimer timer = new ManagedTimer(new SyncTimerTask("T1"), 1500);
        Thread.sleep(5000);
        timer.stop();
        LOG.info("Timer stopped.");
    }

    int period;

    private boolean running = false;

    Synchronizer synchronizer;

    Timer timer;

    SyncTimerTask timerTask;

    public ManagedTimer(SyncTimerTask timerTask, int period) {

        this.timerTask = timerTask;
        this.period = period;
        this.synchronizer = timerTask.getSynchronizer();

    }

    public SxpNode getOwner() {
        return timerTask.getOwner();
    }

    public int getPeriod() {
        return period;
    }

    public SyncTimerTask getTimerTask() {
        return timerTask;
    }

    public TimerType getTimerType() {
        return timerTask.getTimerType();
    }

    public boolean isDone() {
        return timerTask.isDone();
    }

    public boolean isRunning() {
        return running;
    }

    public void start() throws Exception {
        timer = new Timer(false);
        timerTask.assignTimer(this);
        // TODO: new scheduling cyclic without done
        // timer.scheduleAtFixedRate(timerTask, 0, period);
        running = true;
        timerTask.done = false;
        timer.schedule(timerTask, period);
    }

    public void stop() {
        try {
            synchronizer.provide(Command.AcquireLock);
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopForce();
        try {
            synchronizer.provide(Command.ReleaseLock);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopForce() {
        timer.cancel();
        running = false;
    }
}
