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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

import java.net.UnknownHostException;
import java.util.List;

public interface MasterDatabaseInf {

        String PRINT_DELIMITER = " ";

        void addBindings(NodeId owner, List<MasterBindingIdentity> contributedBindingIdentities)
                throws DatabaseAccessException, NodeIdNotDefinedException;

        void addBindingsLocal(SxpNode owner, List<PrefixGroup> prefixGroups)
                throws NodeIdNotDefinedException, DatabaseAccessException;

        void expandBindings(int quantity) throws DatabaseAccessException, UnknownPrefixException, UnknownHostException;

        MasterDatabase get() throws DatabaseAccessException;

        List<MasterDatabase> partition(int quantity, boolean onlyChanged) throws DatabaseAccessException;

        void purgeAllDeletedBindings() throws DatabaseAccessException;

        void purgeBindings(NodeId nodeId) throws NodeIdNotDefinedException, DatabaseAccessException;

        List<MasterBindingIdentity> readBindings() throws DatabaseAccessException;

        List<PrefixGroup> readBindingsLocal() throws DatabaseAccessException;

        void resetModified() throws DatabaseAccessException;

        boolean setAsDeleted(SxpNode owner, List<PrefixGroup> prefixGroups) throws Exception;
}
