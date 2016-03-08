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

    List<MasterDatabaseBinding> getBindings();

    <T extends SxpBindingFields> List<MasterDatabaseBinding> addLocalBindings(List<T> bindings);

    <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindingsLocal(List<T> bindings);

    <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings);

    <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings);

}
