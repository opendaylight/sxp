/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database.spi;

import java.util.List;

import org.opendaylight.sxp.util.database.SxpBindingIdentity;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.sxp.util.exception.node.NodeIdNotDefinedException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

/**
 * SxpDatabaseInf interface representing supported operations on SxpDatabase
 */
public interface SxpDatabaseInf {

        String PRINT_DELIMITER = " ";

        /**
         * Adds Bindings from specified SxpDatabase
         *
         * @param database SxpDatabase containing Bindings
         * @param filter   Specifies filter used selecting binding that will be exported
         * @return If Bindings were added to database
         * @throws DatabaseAccessException If database isn't accessible
         */
        boolean addBindings(SxpDatabase database,SxpBindingFilter filter) throws DatabaseAccessException;

        /**
         * Remove all bindings with Flag CleanUp from specified NodeId
         *
         * @param nodeId NodeId used to filter Binding that will be removed
         * @throws NodeIdNotDefinedException If NodeId is null
         */
        void cleanUpBindings(NodeId nodeId) throws NodeIdNotDefinedException;

        /**
         * Delete Bindings from specified SxpDatabase
         *
         * @param database SxpDatabase containing Bindings
         * @param filter   Specifies filter used selecting binding that will be exported
         * @return List of removed SxpBindingIdentities
         * @throws DatabaseAccessException If database isn't accessible
         */
        List<SxpBindingIdentity> deleteBindings(SxpDatabase database,SxpBindingFilter filter) throws DatabaseAccessException;

        /**
         * Gets SxpDatabase
         *
         * @return SxpDatabase used
         * @throws DatabaseAccessException If database isn't accessible
         */
        SxpDatabase get() throws DatabaseAccessException;

        /**
         * Delete all Bindings from specified NodeId
         *
         * @param nodeId NodeId used to filter Bindings that will be deleted
         * @throws NodeIdNotDefinedException If NodeId is null
         * @throws DatabaseAccessException   If database isn't accessible
         */
        void purgeBindings(NodeId nodeId) throws NodeIdNotDefinedException, DatabaseAccessException;

        /**
         * Reads all bindings in database
         *
         * @return List of SxpBindingIdentities contained by database
         * @throws DatabaseAccessException If database isn't accessible
         */
        List<SxpBindingIdentity> readBindings() throws DatabaseAccessException;

        /**
         * Sets CleanUp flag for all bindings from specified NodeId
         *
         * @param nodeId NodeId used to filter Bindings that will be set for CleanUp
         * @throws NodeIdNotDefinedException If NodeId is null
         */
        void setAsCleanUp(NodeId nodeId) throws NodeIdNotDefinedException;
}
