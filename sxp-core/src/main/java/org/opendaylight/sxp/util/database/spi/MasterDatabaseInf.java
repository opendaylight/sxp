/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;

import java.util.List;

/**
 * MasterDatabaseInf interface representing supported operation on MasterDatabase
 */
public interface MasterDatabaseInf {

    /**
     * @return All bindings stored in MasterDatabase
     */
    List<MasterDatabaseBinding> getBindings();

    /**
     * Adds bindings into MasterDatabase as local bindings
     *
     * @param bindings List of bindings that will be added
     * @param <T>      Any type extending SxpBindingsFields
     * @return List of Bindings that were actually added
     */
    <T extends SxpBindingFields> List<MasterDatabaseBinding> addLocalBindings(List<T> bindings);

    /**
     * Removes bindings from MasterDatabase that were stored as local
     *
     * @param bindings List of bindings that will be removed
     * @param <T>      Any   type extending SxpBindingsFields
     * @return List of bindings that were actually removed
     */
    <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindingsLocal(List<T> bindings);

    /**
     * Adds bindings into MasterDatabase as learned bindings
     *
     * @param bindings List of bindings that will be added
     * @param <T>      Any type extending SxpBindingsFields
     * @return List of Bindings that were actually added
     */
    <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings);

    /**
     * Removes bindings from MasterDatabase that were stored as learned
     *
     * @param bindings List of bindings that will be removed
     * @param <T>      Any   type extending SxpBindingsFields
     * @return List of bindings that were actually removed
     */
    <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings);

}
