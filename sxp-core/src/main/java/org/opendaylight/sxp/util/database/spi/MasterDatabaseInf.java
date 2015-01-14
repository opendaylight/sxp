/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database.spi;

import java.util.List;

import org.opendaylight.sxp.util.database.MasterBindingIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

public interface MasterDatabaseInf {
    public final String PRINT_DELIMITER = " ";

    public void addBindings(NodeId owner, List<MasterBindingIdentity> contributedBindingIdentities) throws Exception;

    public void addBindingsLocal(List<PrefixGroup> prefixGroups) throws Exception;

    public void expandBindings(int quantity) throws Exception;

    public MasterDatabase get() throws Exception;

    public List<MasterDatabase> partition(int quantity, boolean onlyChanged) throws Exception;

    public void purgeAllDeletedBindings() throws Exception;

    public void purgeBindings(NodeId nodeId) throws Exception;

    public List<MasterBindingIdentity> readBindings() throws Exception;

    public List<PrefixGroup> readBindingsLocal() throws Exception;

    public void resetModified() throws Exception;

    public boolean setAsDeleted(List<PrefixGroup> prefixGroups) throws Exception;

    @Override
    public String toString();
}
