/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.hazelcast.MasterDBPropagatingListener;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;

/**
 * MasterDatabaseImpl class contains logic to operate with Database,
 * used for storing all Bindings and their propagation
 */
public class MasterDatabaseImpl extends MasterDatabase {

    private MasterDBPropagatingListener dbListener;
    private final Map<IpPrefix, MasterDatabaseBinding> bindingMap = new HashMap<>();

    @Override
    public void initDBPropagatingListener(BindingDispatcher dispatcher, SxpDomain domain) {
        this.dbListener = new MasterDBPropagatingListener(dispatcher, domain);
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings) {
        List<MasterDatabaseBinding> added = new ArrayList<>();
        if (bindings == null) {
            return added;
        }
        Map<IpPrefix, MasterDatabaseBinding> prefixMap = MasterDatabase
                .filterIncomingBindings(bindings, bindingMap::get, p -> bindingMap.remove(p) != null);
        if (!prefixMap.isEmpty()) {
            bindingMap.putAll(prefixMap);
            added.addAll(prefixMap.values());
        }
        dbListener.onBindingsAdded(added);
        return added;
    }

    @Override
    public synchronized List<MasterDatabaseBinding> getBindings() {
        return new ArrayList<>(bindingMap.values());
    }

    @Override
    public List<MasterDatabaseBinding> getBindings(OriginType origin) {
        return bindingMap.values().stream()
                .filter(binding -> origin.equals(binding.getOrigin()))
                .collect(Collectors.toList());
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings) {
        List<MasterDatabaseBinding> removed = new ArrayList<>();
        if (bindings == null) {
            return removed;
        }
        bindings.forEach(inputBinding -> {
            if (deleteBinding(inputBinding)) {
                removed.add(bindingMap.remove(inputBinding.getIpPrefix()));
            }
        });
        dbListener.onBindingsRemoved(removed);
        return removed;
    }

    /**
     * Decide if binding is to be deleted according to equality with input binding's:
     * <ul>
     *     <li>1. ip prefix</li>
     *     <li>2. security group tag</li>
     *     <li>3. origin</li>
     * </ul>
     */
    private <T extends SxpBindingFields> boolean deleteBinding(T inputBinding) {
        final MasterDatabaseBinding binding = bindingMap.get(inputBinding.getIpPrefix());
        if (binding == null) {
            return false;
        }
        if (!binding.getSecurityGroupTag().getValue()
                .equals(inputBinding.getSecurityGroupTag().getValue())) {
            return false;
        }

        return binding.getOrigin()
                .equals(inputBinding.getOrigin());
    }

    @Override
    public void close() throws Exception {
        //NOOP
    }
}
