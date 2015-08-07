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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

public interface SxpDatabaseInf {

        String PRINT_DELIMITER = " ";

        boolean addBindings(SxpDatabase database) throws DatabaseAccessException;

        void cleanUpBindings(NodeId nodeId) throws NodeIdNotDefinedException;

        List<SxpBindingIdentity> deleteBindings(SxpDatabase database) throws DatabaseAccessException;

        SxpDatabase get() throws DatabaseAccessException;

        void purgeBindings(NodeId nodeId) throws NodeIdNotDefinedException, DatabaseAccessException;

        List<SxpBindingIdentity> readBindings() throws DatabaseAccessException;

        void setAsCleanUp(NodeId nodeId) throws NodeIdNotDefinedException;
}
