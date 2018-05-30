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
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.hazelcast.MasterDBListener;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;

/**
 * MasterDatabaseImpl class contains logic to operate with Database,
 * used for storing all Bindings and their propagation
 */
public class MasterDatabaseImpl extends MasterDatabase {

    private final Map<IpPrefix, MasterDatabaseBinding> bindingMap = new HashMap<>();
    private final Map<IpPrefix, MasterDatabaseBinding> localBindingMap = new HashMap<>();
    private MasterDBListener dbListener;

    @Override
    public void initDBListener(BindingDispatcher dispatcher, SxpDomain domain) {
        this.dbListener = new MasterDBListener(dispatcher, domain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<MasterDatabaseBinding> getBindings() {
        List<MasterDatabaseBinding> bindings = new ArrayList<>(bindingMap.values());
        Set<IpPrefix>
                ipPrefixSet =
                bindings.parallelStream().map(SxpBindingFields::getIpPrefix).collect(Collectors.toSet());
        getLocalBindings().forEach(b -> {
            if (!ipPrefixSet.contains(b.getIpPrefix())) {
                bindings.add(b);
            }
        });
        return bindings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<MasterDatabaseBinding> getLocalBindings() {
        return new ArrayList<>(localBindingMap.values());
    }

    /**
     * Add given bindings.
     */
    private <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings,
            Map<IpPrefix, MasterDatabaseBinding> map, OriginType bindingType) {
        List<MasterDatabaseBinding> added = new ArrayList<>();
        if (map == null || bindings == null || bindings.isEmpty()) {
            return added;
        }
        Map<IpPrefix, MasterDatabaseBinding>
                prefixMap =
                filterIncomingBindings(bindings, map::get, p -> map.remove(p) != null, bindingType);
        if (!prefixMap.isEmpty()) {
            map.putAll(prefixMap);
            added.addAll(prefixMap.values());
        }
        if (map == localBindingMap) {
            dbListener.onBindingsAdded(added);
        }
        return added;
    }

    /**
     * Delete given bindings from a given map.
     *
     * @param bindings Bindings to be removed
     * @param map      Map from where bindings will be removed
     * @param <T>      Any type extending SxpBindingFields
     * @return Deleted bindings
     */
    private <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings,
            Map<IpPrefix, MasterDatabaseBinding> map) {
        List<MasterDatabaseBinding> removed = new ArrayList<>();
        if (map == null || bindings == null || bindings.isEmpty()) {
            return removed;
        }
        bindings.forEach(b -> {
            if (map.containsKey(b.getIpPrefix()) && map.get(b.getIpPrefix())
                    .getSecurityGroupTag()
                    .getValue()
                    .equals(b.getSecurityGroupTag().getValue())) {
                removed.add(map.remove(b.getIpPrefix()));
            }
        });
        if (map == localBindingMap) {
            dbListener.onBindingsRemoved(removed);
        }
        return removed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized <T extends SxpBindingFields> List<MasterDatabaseBinding> addLocalBindings(List<T> bindings) {
        return addBindings(bindings, localBindingMap, OriginType.LOCAL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindingsLocal(List<T> bindings) {
        return deleteBindings(bindings, localBindingMap);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings) {
        return addBindings(bindings, bindingMap, OriginType.NETWORK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings) {
        return deleteBindings(bindings, bindingMap);
    }

    @Override
    public void close() throws Exception {
        //NOOP
    }
}
