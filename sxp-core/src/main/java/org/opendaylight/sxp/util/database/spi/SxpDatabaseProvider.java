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

/**
 * SxpDatabaseProvider class provide checked access into SxpDatabase
 */
public abstract class SxpDatabaseProvider implements SxpDatabaseInf {

    protected static final Logger LOG = LoggerFactory.getLogger(SxpDatabaseProvider.class.getName());

    protected SxpDatabaseAccess databaseAccess;

    /**
     * Default constructor that sets SxpDatabaseAccess
     *
     * @param databaseAccess SxpDatabaseAccess to be set
     */
    protected SxpDatabaseProvider(SxpDatabaseAccess databaseAccess) {
        super();
        this.databaseAccess = databaseAccess;
    }

    /**
     * Gets SxpDatabaseAccess of this provider
     *
     * @return SxpDatabaseAccess
     */
    public SxpDatabaseAccess getDatabaseAccess() {
        return databaseAccess;
    }
}
