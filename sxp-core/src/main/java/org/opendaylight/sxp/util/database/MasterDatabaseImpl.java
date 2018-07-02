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
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.hazelcast.MasterDBPropagatingListener;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<MasterDatabaseBinding> getBindings() {
        return new ArrayList<>(bindingMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<MasterDatabaseBinding> getLocalBindings() {
        return getBindings().stream()
                .filter(binding -> BindingOriginsConfig.LOCAL_ORIGIN.equals(binding.getOrigin()))
                .collect(Collectors.toList());
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> addLocalBindings(List<T> bindings) {
        final List<MasterDatabaseBinding> localBindings = bindings.stream()
                .map(binding -> new MasterDatabaseBindingBuilder(binding)
                        .setOrigin(BindingOriginsConfig.LOCAL_ORIGIN)
                        .build())
                .collect(Collectors.toList());

        return addBindings(localBindings);
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindingsLocal(List<T> bindings) {
        return deleteBindings(bindings);
    }

    /**
     * Add given bindings.
     */
    @Override
    public  <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings) {
        List<MasterDatabaseBinding> added = new ArrayList<>();
        if (bindings == null || bindings.isEmpty()) {
            return added;
        }
        Map<IpPrefix, MasterDatabaseBinding> prefixMap = filterIncomingBindings(
                bindings, bindingMap::get, p -> bindingMap.remove(p) != null);
        if (!prefixMap.isEmpty()) {
            bindingMap.putAll(prefixMap);
            added.addAll(prefixMap.values());
        }
        dbListener.onBindingsAdded(added);
        return added;
    }

    /**
     * Delete given bindings from a given map.
     *
     * @param bindings Bindings to be removed
     * @param <T>      Any type extending SxpBindingFields
     * @return Deleted bindings
     */
    @Override
    public  <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings) {
        List<MasterDatabaseBinding> removed = new ArrayList<>();
        if (bindings == null || bindings.isEmpty()) {
            return removed;
        }
        bindings.forEach(b -> {
            if (bindingMap.containsKey(b.getIpPrefix()) && bindingMap.get(b.getIpPrefix())
                    .getSecurityGroupTag()
                    .getValue()
                    .equals(b.getSecurityGroupTag().getValue())) {
                removed.add(bindingMap.remove(b.getIpPrefix()));
            }
        });
        dbListener.onBindingsRemoved(removed);
        return removed;
    }

    @Override
    public void close() throws Exception {
        //NOOP
    }
}
