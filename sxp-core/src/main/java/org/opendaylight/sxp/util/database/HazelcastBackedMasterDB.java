/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.database;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.AbstractEntryProcessor;
import com.hazelcast.query.PredicateBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.hazelcast.MasterDBBindingSerializer;
import org.opendaylight.sxp.core.hazelcast.MasterHCDBListener;
import org.opendaylight.sxp.core.hazelcast.PeerSequenceSerializer;
import org.opendaylight.sxp.core.hazelcast.PeerSerializer;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;

public class HazelcastBackedMasterDB extends MasterDatabase implements AutoCloseable {

    private final HazelcastInstance hcInstance;
    private final IMap<IpPrefix, MasterDatabaseBinding> bindingMap;
    private final String mapName;
    private final MasterHCDBListener dbListener;

    /**
     * Create a new Master DB backed by Hazelcast with a default config.
     * Automatically adds required serializers to config.
     *
     * @param hcMapName unique name of the map used to store the bindings
     */
    public HazelcastBackedMasterDB(String hcMapName, BindingDispatcher bindingDispatcher, SxpDomain domain) {
        this(hcMapName, bindingDispatcher, domain, new Config());
    }

    /**
     * Create a new Master DB backed by Hazelcast.
     * Automatically adds required serializers to config.
     *
     * @param hcMapName unique name of the map used to store the bindings
     * @param hcConfig  Hazelcast config to use
     */
    public HazelcastBackedMasterDB(String hcMapName, BindingDispatcher dispatcher, SxpDomain domain, Config hcConfig) {
        hcConfig.getSerializationConfig()
                .addSerializerConfig(MasterDBBindingSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSequenceSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSerializer.getSerializerConfig());
        this.hcInstance = Hazelcast.newHazelcastInstance(hcConfig);
        this.mapName = hcMapName;
        this.bindingMap = hcInstance.getMap(mapName);
        this.dbListener = new MasterHCDBListener(dispatcher, domain);
        bindingMap.addEntryListener(dbListener, true);
    }

    @Override
    public List<MasterDatabaseBinding> getBindings() {
        return new ArrayList<>(bindingMap.values());
    }

    @Override
    public Collection<MasterDatabaseBinding> getLocalBindings() {
        PredicateBuilder predicate = new PredicateBuilder().getEntryObject().get("_origin").equal(OriginType.LOCAL);
        return bindingMap.values(predicate);
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> addLocalBindings(List<T> bindings) {
        return doAddBindings(bindings, OriginType.LOCAL);
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings) {
        return doAddBindings(bindings, OriginType.NETWORK);
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindingsLocal(List<T> bindings) {
        return deleteBindings(bindings);
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings) {
        List<MasterDatabaseBinding> deletedBindings = new ArrayList<>();
        for (T binding : bindings) {
            MasterDatabaseBinding removed = bindingMap.remove(binding.getIpPrefix());
            if (removed != null) {
                deletedBindings.add(removed);
            }
        }
        return deletedBindings;
    }

    private <T extends SxpBindingFields> List<MasterDatabaseBinding> doAddBindings(Iterable<T> bindings, OriginType bindingType) {
        Map<IpPrefix, MasterDatabaseBinding> addedBindings = new HashMap<>();
        for (T incomingBinding : bindings) {
            if (ignoreBinding(incomingBinding)) {
                continue;
            }
            MasterDatabaseBinding bindingToAdd = new MasterDatabaseBindingBuilder(incomingBinding)
                    .setOrigin(bindingType)
                    .build();
            if (bindingMap.containsKey(incomingBinding.getIpPrefix())) {
                if ((Boolean) bindingMap.executeOnKey(bindingToAdd.getIpPrefix(), new ConditionalAddProcessor(bindingToAdd))) {
                    addedBindings.put(bindingToAdd.getIpPrefix(), bindingToAdd);
                }
            } else {
                bindingMap.set(incomingBinding.getIpPrefix(), bindingToAdd);
                addedBindings.put(bindingToAdd.getIpPrefix(), bindingToAdd);
            }
        }
        return new ArrayList<>(addedBindings.values());
    }

    @Override
    public void close() {
        hcInstance.shutdown();
    }

    public HazelcastInstance getHcInstance() {
        return hcInstance;
    }

    public String getMapName() {
        return mapName;
    }

    private static final class ConditionalAddProcessor extends AbstractEntryProcessor<IpPrefix, MasterDatabaseBinding> {

        private final MasterDatabaseBinding mdbToAdd;

        private ConditionalAddProcessor(MasterDatabaseBinding mdbToAdd) {
            this.mdbToAdd = mdbToAdd;
        }

        @Override
        public Object process(Map.Entry<IpPrefix, MasterDatabaseBinding> entry) {
            if (MasterDBBindingComparator.INSTANCE.compare(mdbToAdd, entry.getValue()) < 0) {
                entry.setValue(mdbToAdd);
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
    }
}
