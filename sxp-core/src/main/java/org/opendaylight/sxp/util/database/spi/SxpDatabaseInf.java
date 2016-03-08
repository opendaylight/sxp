/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database.spi;

import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

import java.util.List;

/**
 * SxpDatabaseInf interface representing supported operations on SxpDatabase
 */
public interface SxpDatabaseInf {

    List<SxpDatabaseBinding> getBindings();

    List<SxpDatabaseBinding> getBindings(NodeId nodeId);

    <T extends SxpBindingFields> List<SxpDatabaseBinding> addBinding(NodeId nodeId, List<T> bindings);

    List<SxpDatabaseBinding> filterDatabase(NodeId nodeId, SxpBindingFilter filter);

    List<SxpDatabaseBinding> deleteBindings(NodeId nodeId);

    <T extends SxpBindingFields> List<SxpDatabaseBinding> deleteBindings(NodeId nodeId, List<T> bindings);

    <T extends SxpBindingFields> List<SxpDatabaseBinding> getReplaceForBindings(List<T> bindings);

    List<SxpDatabaseBinding> reconcileBindings(NodeId nodeId);

    void setReconciliation(NodeId nodeId);
}
