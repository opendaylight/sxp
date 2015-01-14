/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseProvider;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseProvider;

public abstract class Service implements Runnable {

    protected static final long THREAD_DELAY = 500;

    protected volatile boolean finished = false;

    protected SxpNode owner;

    protected Service(SxpNode owner) {
        this.owner = owner;
    }

    public void cancel() {
        this.finished = true;
    }

    public synchronized MasterDatabaseProvider getBindingMasterDatabase() throws Exception {
        return owner.getBindingMasterDatabase();
    }

    public synchronized SxpDatabaseProvider getBindingSxpDatabase() throws Exception {
        return owner.getBindingSxpDatabase();
    }

    public void notifyChange() {
        owner.setSvcBindingManagerNotify();
    }

    public void reset() {
        this.finished = false;
    }
}
