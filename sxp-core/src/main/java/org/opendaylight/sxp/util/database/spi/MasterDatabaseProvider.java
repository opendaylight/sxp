/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MasterDatabaseProvider implements MasterDatabaseInf {

    protected static final Logger LOG = LoggerFactory.getLogger(MasterDatabaseProvider.class.getName());

    protected MasterDatabaseAccess databaseAccess;

    protected MasterDatabaseProvider(MasterDatabaseAccess databaseAccess) {
        super();
        this.databaseAccess = databaseAccess;
    }

    public MasterDatabaseAccess getDatabaseAccess() {
        return databaseAccess;
    }
}
