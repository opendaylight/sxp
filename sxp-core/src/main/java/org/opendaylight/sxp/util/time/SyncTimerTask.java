/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time;

import java.util.TimerTask;

import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.Synchronizer.Command;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncTimerTask extends TimerTask {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncTimerTask.class.getName());

    public boolean done = false;

    protected SxpNode owner;

    int period;

    private Synchronizer synchronizer;

    ManagedTimer timer;

    String timerName;

    TimerType timerType;

    public SyncTimerTask(String timerName) {
        this.timerType = null;
        this.owner = null;
        this.timerName = timerName;
        this.synchronizer = new Synchronizer();
    }

    public SyncTimerTask(TimerType timerType, SxpNode owner, int period) {
        this.timerType = timerType;
        this.owner = owner;
        this.period = period;
        this.timerName = timerType + ":" + NodeIdConv.toString(owner.getNodeId());
        this.synchronizer = new Synchronizer();
    }

    public void assignTimer(ManagedTimer timer) {
        this.timer = timer;
    }

    public void done() {
        this.done = true;
        cancel();
        timer.stopForce();
    }

    public SxpNode getOwner() {
        return owner;
    }

    public int getPeriod() {
        return period;
    }

    public Synchronizer getSynchronizer() {
        return synchronizer;
    }

    public TimerType getTimerType() {
        return timerType;
    }

    public boolean isDone() {
        return done;
    }

    protected void performAction() {
        LOG.info("Performing action..");
        try {
            Thread.sleep(ManagedTimer.TIME_FACTOR);
        } catch (Exception e) {

        }
        LOG.info("Action finished.");
    }

    @Override
    public void run() {
        try {
            synchronizer.provide(Command.AcquireLock);
        } catch (Exception e) {
            e.printStackTrace();
        }
        performAction();
        try {
            synchronizer.provide(Command.ReleaseLock);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return timerName;
    }
}