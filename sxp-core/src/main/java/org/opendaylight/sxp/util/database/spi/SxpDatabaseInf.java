/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

import java.util.List;

/**
 * SxpDatabaseInf interface representing supported operations on SxpDatabase
 */
public interface SxpDatabaseInf {

    /**
     * @return All bindings stored in SxpDatabase
     */
    List<SxpDatabaseBinding> getBindings();

    /**
     * Return all bindings learned from specified peer
     *
     * @param nodeId Specifying peer
     * @return Bindings learned from peer
     */
    List<SxpDatabaseBinding> getBindings(NodeId nodeId);

    /**
     * Adds bindings to SxpDatabase under specified peer
     *
     * @param nodeId   Specifying peer from which bindings came
     * @param bindings Bindings to be added
     * @param <T>      Any type extending SxpBindingFields
     * @return List of bindings that were actually added
     */
    <T extends SxpBindingFields> List<SxpDatabaseBinding> addBinding(NodeId nodeId, List<T> bindings);

    /**
     * Delete all binding learned from specified peer
     *
     * @param nodeId Specifying peer from which bindings will be removed
     * @return List of bindings that were actually removed
     */
    List<SxpDatabaseBinding> deleteBindings(NodeId nodeId);

    /**
     * Delete specified bindings from peer
     *
     * @param nodeId   Specifying peer from which bindings will be removed
     * @param bindings List of bindings that will be removed
     * @param <T>      Any type extending SxpBindingFields
     * @return List of bindings that were actually removed
     */
    <T extends SxpBindingFields> List<SxpDatabaseBinding> deleteBindings(NodeId nodeId, List<T> bindings);

    /**
     * Delete all bindings from peer that were previously set for reconciliation
     *
     * @param nodeId Specifying peer on which operation will be held
     * @return List of bindings that were actually removed
     */
    List<SxpDatabaseBinding> reconcileBindings(NodeId nodeId);

    /**
     * Sets all bindings from specified peer marked as reconciled,
     * this state have no effect until reconcileBindings is called on peer
     *
     * @param nodeId Specifying peer on operation will be held
     */
    void setReconciliation(NodeId nodeId);
}
