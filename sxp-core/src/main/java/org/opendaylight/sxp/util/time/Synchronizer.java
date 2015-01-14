/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time;

public class Synchronizer {

    public enum Command {
        AcquireLock, ReleaseLock
    }

    private int lock = 0;

    public synchronized void provide(Command command) throws InterruptedException {
        switch (command) {
        case AcquireLock:
            if (lock == 1) {
                wait();
            }
            lock++;
            return;
        case ReleaseLock:
            lock--;
            notifyAll();
            return;
        }
    }
}
