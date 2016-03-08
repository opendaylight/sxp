/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MasterDatabaseImpl class contains logic to operate with Database,
 * used for storing all Bindings and their propagation
 */
public class MasterDatabaseImpl extends org.opendaylight.sxp.util.database.spi.MasterDatabase {

    private final Map<IpPrefix, MasterDatabaseBinding> bindingMap = new HashMap<>();

    /**
     * Default constructor that sets empty Database
     */
    public MasterDatabaseImpl() {
    }

    /**
     * Constructor that sets predefined Database
     *
     * @param database MasterDatabase to be used
     */
    public MasterDatabaseImpl(MasterDatabase database) {
        Preconditions.checkNotNull(database);
    }

    @Override public List<MasterDatabaseBinding> getBindings() {
        return new ArrayList<>(bindingMap.values());
    }

    @Override public <T extends SxpBindingFields> List<MasterDatabaseBinding> addLocalBindings(List<T> bindings) {
        List<MasterDatabaseBinding> added = addBindings(bindings);
        // TODO backup of bindings
        return added;
    }

    @Override public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindingsLocal(List<T> bindings) {
        List<MasterDatabaseBinding> removed = deleteBindings(bindings);
        // TODO backup of bindings
        return removed;
    }

    @Override public <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings) {
        List<MasterDatabaseBinding> added = new ArrayList<>();
        if (bindings == null || bindings.isEmpty())
            return added;
        Map<IpPrefix, MasterDatabaseBinding> prefixMap = filterIncomingBindings(bindings, bindingMap::get);
        if (!prefixMap.isEmpty()) {
            bindingMap.putAll(prefixMap);
            added.addAll(prefixMap.values());
        }
        return added;
    }

    @Override public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings) {
        List<MasterDatabaseBinding> removed = new ArrayList<>();
        if (bindings == null || bindings.isEmpty())
            return removed;
        bindings.stream().forEach(b -> {
            if (bindingMap.containsKey(b.getIpPrefix()) && bindingMap.get(b.getIpPrefix())
                    .getSecurityGroupTag()
                    .getValue()
                    .equals(b.getSecurityGroupTag().getValue())) {
                removed.add(bindingMap.remove(b.getIpPrefix()));
            }
        });
        return removed;
    }
}
