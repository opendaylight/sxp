/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MasterDatabaseImpl class contains logic to operate with Database,
 * used for storing all Bindings and their propagation
 */
public class MasterDatabaseImpl extends MasterDatabase {

    private final Map<IpPrefix, MasterDatabaseBinding> bindingMap = new HashMap<>();
    private final Map<IpPrefix, MasterDatabaseBinding> localBindingMap = new HashMap<>();

    @Override synchronized public List<MasterDatabaseBinding> getBindings() {
        List<MasterDatabaseBinding> bindings = getLocalBindings();
        bindings.addAll(bindingMap.values());
        return bindings;
    }

    @Override synchronized public List<MasterDatabaseBinding> getLocalBindings() {
        return new ArrayList<>(localBindingMap.values());
    }

    private <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings,
            Map<IpPrefix, MasterDatabaseBinding> map) {
        List<MasterDatabaseBinding> added = new ArrayList<>();
        if (map == null || bindings == null || bindings.isEmpty())
            return added;
        Map<IpPrefix, MasterDatabaseBinding>
                prefixMap =
                filterIncomingBindings(bindings, map::get, p -> map.remove(p) != null);
        if (!prefixMap.isEmpty()) {
            map.putAll(prefixMap);
            added.addAll(prefixMap.values());
        }
        return added;
    }

    private <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings,
            Map<IpPrefix, MasterDatabaseBinding> map) {
        List<MasterDatabaseBinding> removed = new ArrayList<>();
        if (map == null || bindings == null || bindings.isEmpty())
            return removed;
        bindings.stream().forEach(b -> {
            if (map.containsKey(b.getIpPrefix()) && map.get(b.getIpPrefix())
                    .getSecurityGroupTag()
                    .getValue()
                    .equals(b.getSecurityGroupTag().getValue())) {
                removed.add(map.remove(b.getIpPrefix()));
            }
        });
        return removed;
    }

    @Override
    synchronized public <T extends SxpBindingFields> List<MasterDatabaseBinding> addLocalBindings(List<T> bindings) {
        return addBindings(bindings, localBindingMap);
    }

    @Override
    synchronized public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindingsLocal(List<T> bindings) {
        return deleteBindings(bindings, localBindingMap);
    }

    @Override
    synchronized public <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings) {
        return addBindings(bindings, bindingMap);
    }

    @Override
    synchronized public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings) {
        return deleteBindings(bindings, bindingMap);
    }
}
