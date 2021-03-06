/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database.spi;

import java.util.List;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;

/**
 * MasterDatabaseInf interface representing supported operation on MasterDatabase
 */
public interface MasterDatabaseInf extends AutoCloseable {

    /**
     * Initialize a listener of DB events that propagates the changes via Update msgs.
     *
     * @param dispatcher dispatcher for propagating bindings
     * @param domain     a domain to which this DB belongs to
     */
    void initDBPropagatingListener(BindingDispatcher dispatcher, SxpDomain domain);

    /**
     * Get all bindings from master database.
     *
     * @return All bindings stored in MasterDatabase
     */
    List<MasterDatabaseBinding> getBindings();

    /**
     * Get bindings of specified origin from master database.
     *
     * @return All bindings of specified origin
     */
    List<MasterDatabaseBinding> getBindings(OriginType origin);

    /**
     * Adds bindings into MasterDatabase.
     *
     * @param bindings List of bindings that will be added
     * @param <T>      Any type extending SxpBindingsFields
     * @return List of Bindings that were actually added
     */
    <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings);

    /**
     * Removes bindings from MasterDatabase.
     *
     * @param bindings List of bindings that will be removed
     * @param <T>      Any type extending SxpBindingsFields
     * @return List of bindings that were actually removed
     */
    <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings);

}
