/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database.spi;

import java.util.List;

import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.SxpBindingIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

public interface SxpDatabaseInf {
    public final String PRINT_DELIMITER = " ";

    public boolean addBindings(SxpDatabase database) throws Exception;

    public void cleanUpBindings(NodeId nodeId) throws Exception;

    public List<SxpBindingIdentity> deleteBindings(SxpDatabase database) throws Exception;

    public SxpDatabase get() throws Exception;

    public void purgeBindings(NodeId nodeId) throws Exception;

    public List<SxpBindingIdentity> readBindings() throws Exception;

    public void setAsCleanUp(NodeId nodeId) throws Exception;

    /**
     * Add SxpNode as owner of this database, owner will be notified of local binding changes
     *
     * @param node SxpNode that will be added as owner
     * @throws IllegalStateException If owner of database was already set
     */
    void addOwner(SxpNode node) throws IllegalStateException;

    @Override
    public String toString();
}
