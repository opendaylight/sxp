/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database.spi;

import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.MasterBindingIdentity;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.sxp.util.exception.node.NodeIdNotDefinedException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

import java.net.UnknownHostException;
import java.util.List;

/**
 * MasterDatabaseInf interface representing supported operation on MasterDatabase
 */
public interface MasterDatabaseInf {

        String PRINT_DELIMITER = " ";

        /**
         * Adds Bindings into MasterDatabase
         *
         * @param owner                        NodeId specifying from where Bindings came from
         * @param contributedBindingIdentities MasterBindingIdentities that will be added into MasterDatabase
         * @throws NodeIdNotDefinedException If NodeID is null
         * @throws DatabaseAccessException   If database isn't accessible
         */
        void addBindings(NodeId owner, List<MasterBindingIdentity> contributedBindingIdentities)
                throws DatabaseAccessException, NodeIdNotDefinedException;

        /**
         * Adds Bindings into MasterDatabase with Flag Local
         *
         * @param owner        SxpNode that will be notified after change
         * @param prefixGroups PrefixGroups containing Bindings that will be added
         * @throws DatabaseAccessException If database isn't accessible
         */
        void addBindingsLocal(SxpNode owner, List<PrefixGroup> prefixGroups)
                throws NodeIdNotDefinedException, DatabaseAccessException;

        /**
         * Expand All bindings that have prefix length different from 32-IPv4 or 128-IPv6
         *
         * @param quantity Limits max number of newly generated Binding
         * @throws DatabaseAccessException If database isn't accessible
         * @throws UnknownHostException    If invalid Ip address was in database
         * @throws UnknownPrefixException  If invalid prefix was was in database
         */
        void expandBindings(int quantity) throws DatabaseAccessException, UnknownPrefixException, UnknownHostException;

        /**
         * Gets MasterDatabase
         *
         * @return MasterDatabase used
         * @throws DatabaseAccessException If database isn't accessible
         */
        MasterDatabase get() throws DatabaseAccessException;

        /**
         * Split MasterDatabase into multiple smaller MasterDatabases
         *
         * @param quantity    Specifies number of Bindings that one MasterDatabase may contain
         * @param onlyChanged If only Bindings with Flag Changed will be exported
         * @param filter      Specifies filter used selecting binding that will be exported
         * @return List of MasterDatabases containing Bindings
         * @throws DatabaseAccessException If database isn't accessible
         */
        List<MasterDatabase> partition(int quantity, boolean onlyChanged,SxpBindingFilter filter) throws DatabaseAccessException;

        /**
         * Remove all Bindings with Flag Delete
         *
         * @throws DatabaseAccessException If database isn't accessible
         */
        void purgeAllDeletedBindings() throws DatabaseAccessException;

        /**
         * Removes all Bindings from specified NodeId
         *
         * @param nodeId NodeId used to filter what Bindings will be removed
         * @throws NodeIdNotDefinedException If NOdeId is null
         * @throws DatabaseAccessException   If database isn't accessible
         */
        void purgeBindings(NodeId nodeId) throws NodeIdNotDefinedException, DatabaseAccessException;

        /**
         * Gets All Bindings from MasterDatabase
         *
         * @return Bindings inside MasterDatabase
         * @throws DatabaseAccessException If database isn't accessible
         */
        List<MasterBindingIdentity> readBindings() throws DatabaseAccessException;

        /**
         * Gets Bindings with Flag Local from MasterDatabase
         *
         * @return Local Bindings inside MasterDatabase
         * @throws DatabaseAccessException If database isn't accessible
         */
        List<PrefixGroup> readBindingsLocal() throws DatabaseAccessException;

        /**
         * Reset Flag Modified for All Bindings
         *
         * @throws DatabaseAccessException If database isn't accessible
         */
        void resetModified() throws DatabaseAccessException;

        /**
         * Sets Delete Flag for Bindings in specified PrefixGroups
         *
         * @param owner        SxpNode that will be notified after change
         * @param prefixGroups PrefixGroups used to filter Bindings that will set for Deletion
         * @return If operation was successful
         * @throws DatabaseAccessException If database isn't accessible
         */
        boolean setAsDeleted(SxpNode owner, List<PrefixGroup> prefixGroups) throws Exception;
}
