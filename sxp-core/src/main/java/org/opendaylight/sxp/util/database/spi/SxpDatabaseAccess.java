/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database.spi;

import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;

public interface SxpDatabaseAccess {

        /**
         * Delete Bindings from SxpDatabase
         *
         * @param database SxpDatabase containing Bindings to be deleted
         * @throws DatabaseAccessException If database isn't accessible
         */
        void delete(SxpDatabase database) throws DatabaseAccessException;

        /**
         * Merge SxpDatabase with current one
         *
         * @param database SxpDatabase that will be merged
         * @throws DatabaseAccessException If database isn't accessible
         */
        void merge(SxpDatabase database) throws DatabaseAccessException;

        /**
         * Adds Bindings to SxpDatabase
         *
         * @param database SxpDatabase containing Bindings to be added
         * @throws DatabaseAccessException If database isn't accessible
         */
        void put(SxpDatabase database) throws DatabaseAccessException;

        /**
         * Gets access to SxpDatabase
         *
         * @return SxpDatabase
         * @throws DatabaseAccessException If database isn't accessible
         */
        SxpDatabase read() throws DatabaseAccessException;
}
