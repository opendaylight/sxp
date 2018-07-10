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
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.PredicateBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.hazelcast.MasterDBBindingSerializer;
import org.opendaylight.sxp.core.hazelcast.MasterHCDBPropagatingListener;
import org.opendaylight.sxp.core.hazelcast.PeerSequenceSerializer;
import org.opendaylight.sxp.core.hazelcast.PeerSerializer;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;

public class HazelcastBackedMasterDB extends MasterDatabase {

    private final HazelcastInstance hcInstance;
    private final boolean hcInstanceInjected;
    private final IMap<IpPrefix, MasterDatabaseBinding> bindingMap;
    private final String mapName;
    private MasterHCDBPropagatingListener dbListener;

    /**
     * Create a new Master DB backed by a new Hazelcast instance with a default config.
     * Automatically adds required serializers to the config.
     *
     * @param hcMapName unique name of the map used to store the bindings
     */
    public HazelcastBackedMasterDB(String hcMapName) {
        this(hcMapName, new Config());
    }

    /**
     * Create a new Master DB backed by a new Hazelcast instance.
     * Automatically adds required serializers to the config.
     *
     * @param hcMapName unique name of the map used to store the bindings
     * @param hcConfig  Hazelcast config to use
     */
    public HazelcastBackedMasterDB(String hcMapName, Config hcConfig) {
        hcConfig.getSerializationConfig()
                .addSerializerConfig(MasterDBBindingSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSequenceSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSerializer.getSerializerConfig());
        this.hcInstance = Hazelcast.newHazelcastInstance(hcConfig);
        this.mapName = hcMapName;
        this.bindingMap = hcInstance.getMap(mapName);
        this.hcInstanceInjected = false;
    }

    /**
     * Create a new Master DB backed by a provided Hazelcast instance.
     *
     * @param hcMapName  unique name of the map used to store the bindings
     * @param hcInstance Hazelcast instance to use
     */
    public HazelcastBackedMasterDB(String hcMapName, HazelcastInstance hcInstance) {
        this.hcInstance = hcInstance;
        this.mapName = hcMapName;
        this.bindingMap = hcInstance.getMap(mapName);
        this.hcInstanceInjected = true;
    }

    @Override
    public void initDBPropagatingListener(BindingDispatcher dispatcher, SxpDomain domain) {
        this.dbListener = new MasterHCDBPropagatingListener(dispatcher, domain);
        bindingMap.addEntryListener(dbListener, true);
    }

    @Override
    public List<MasterDatabaseBinding> getBindings() {
        return new ArrayList<>(bindingMap.values());
    }

    @Override
    public List<MasterDatabaseBinding> getBindings(OriginType origin) {
        EntryObject e = new PredicateBuilder().getEntryObject();
        PredicateBuilder equal = e.get("_origin._value").equal(origin.getValue());
        return new ArrayList<>(bindingMap.values(equal));
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

    public <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings) {
        Map<IpPrefix, MasterDatabaseBinding> addedBindings = new HashMap<>();
        for (T incomingBinding : bindings) {
            if (ignoreBinding(incomingBinding)) {
                continue;
            }
            MasterDatabaseBinding bindingToAdd = new MasterDatabaseBindingBuilder(incomingBinding).build();
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
        if (!hcInstanceInjected) {
            hcInstance.shutdown();
        }
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
