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
import com.hazelcast.core.MultiMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.opendaylight.sxp.core.hazelcast.PeerSequenceSerializer;
import org.opendaylight.sxp.core.hazelcast.PeerSerializer;
import org.opendaylight.sxp.core.hazelcast.SxpDBBindingSerializer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.BindingDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

public class HazelcastBackedSxpDB extends SxpDatabase {

    private final HazelcastInstance hcInstance;
    private final boolean hcInstanceInjected;
    private final MultiMap<NodeId, SxpDatabaseBinding> activeBindingsMap;
    private final MultiMap<NodeId, SxpDatabaseBinding> tentativeBindingsMap;
    private final String activeBindingMapName;
    private final String tentativeBindingMapName;

    /**
     * Create a new SXP database backed by Hazelcast multimaps using default config.
     * Automatically adds required serializers to config.
     *
     * @param activeBindingMapName    unique name of the multimap used for active bindings
     * @param tentativeBindingMapName unique name of the multimap used for tentative bindings
     */
    public HazelcastBackedSxpDB(String activeBindingMapName, String tentativeBindingMapName) {
        this(activeBindingMapName, tentativeBindingMapName, new Config());
    }

    /**
     * Create a new SXP database backed by Hazelcast multimaps.
     * Automatically adds required serializers to config.
     *
     * @param activeBindingMapName    unique name of the multimap used for active bindings
     * @param tentativeBindingMapName unique name of the multimap used for tentative bindings
     * @param hcConfig                Hazelcast config to use
     */
    public HazelcastBackedSxpDB(String activeBindingMapName, String tentativeBindingMapName, Config hcConfig) {
        hcConfig.getSerializationConfig()
                .addSerializerConfig(SxpDBBindingSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSequenceSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSerializer.getSerializerConfig());
        this.hcInstance = Hazelcast.newHazelcastInstance(hcConfig);
        this.activeBindingMapName = activeBindingMapName;
        this.tentativeBindingMapName = tentativeBindingMapName;
        this.activeBindingsMap = hcInstance.getMultiMap(activeBindingMapName);
        this.tentativeBindingsMap = hcInstance.getMultiMap(tentativeBindingMapName);
        this.hcInstanceInjected = false;
    }

    /**
     * Create a new SXP database backed by a provided Hazelcast instance.
     *
     * @param activeBindingMapName    unique name of the multimap used for active bindings
     * @param tentativeBindingMapName unique name of the multimap used for tentative bindings
     * @param hcInstance              Hazelcast instance to use
     */
    public HazelcastBackedSxpDB(String activeBindingMapName, String tentativeBindingMapName, HazelcastInstance hcInstance) {
        this.hcInstance = hcInstance;
        this.activeBindingMapName = activeBindingMapName;
        this.tentativeBindingMapName = tentativeBindingMapName;
        this.activeBindingsMap = hcInstance.getMultiMap(activeBindingMapName);
        this.tentativeBindingsMap = hcInstance.getMultiMap(tentativeBindingMapName);
        this.hcInstanceInjected = true;
    }

    @Override
    public boolean putBindings(NodeId nodeId, BindingDatabase.BindingType bindingType, Collection<SxpDatabaseBinding> bindings) {
        switch (bindingType) {
            case ActiveBindings:
                return putIntoSubDatabase(nodeId, bindings, activeBindingsMap);
            case TentativeBindings:
                return putIntoSubDatabase(nodeId, bindings, tentativeBindingsMap);
            default:
                return false;
        }
    }

    private boolean putIntoSubDatabase(NodeId nodeId, Iterable<SxpDatabaseBinding> bindingsToAdd,
                                       MultiMap<NodeId, SxpDatabaseBinding> bindingMap) {
        boolean result = false;
        for (SxpDatabaseBinding binding : bindingsToAdd) {
            if (bindingMap.put(nodeId, binding) && !result) {
                result = true;
            }
        }
        return result;
    }

    @Override
    protected Collection<SxpDatabaseBinding> getBindings(BindingDatabase.BindingType bindingType) {
        switch (bindingType) {
            case ActiveBindings:
                return activeBindingsMap.values();
            case TentativeBindings:
                return tentativeBindingsMap.values();
        }
        return Collections.emptyList();
    }

    @Override
    protected Collection<SxpDatabaseBinding> getBindings(BindingDatabase.BindingType bindingType, NodeId nodeId) {
        switch (bindingType) {
            case ActiveBindings:
                return activeBindingsMap.get(nodeId);
            case TentativeBindings:
                return tentativeBindingsMap.get(nodeId);
        }
        return Collections.emptyList();
    }

    @Override
    protected boolean deleteBindings(NodeId nodeId, BindingDatabase.BindingType bindingType) {
        switch (bindingType) {
            case ActiveBindings:
                return deleteFromSubDatabase(nodeId, activeBindingsMap);
            case TentativeBindings:
                return deleteFromSubDatabase(nodeId, tentativeBindingsMap);
            default:
                return false;
        }
    }

    private boolean deleteFromSubDatabase(NodeId nodeId, MultiMap<NodeId, SxpDatabaseBinding> bindingMap) {
        Collection<SxpDatabaseBinding> removed;
        bindingMap.lock(nodeId);
        try {
            removed = bindingMap.remove(nodeId);
        } finally {
            bindingMap.unlock(nodeId);
        }
        return removed != null;
    }

    @Override
    protected List<SxpDatabaseBinding> deleteBindings(NodeId nodeId, Set<IpPrefix> prefixesToRemove,
                                                      BindingDatabase.BindingType bindingType) {
        switch (bindingType) {
            case ActiveBindings:
                return deleteFromSubDatabase(nodeId, prefixesToRemove, activeBindingsMap);
            case TentativeBindings:
                return deleteFromSubDatabase(nodeId, prefixesToRemove, tentativeBindingsMap);
            default:
                return Collections.emptyList();
        }
    }

    private List<SxpDatabaseBinding> deleteFromSubDatabase(NodeId nodeId, Collection<IpPrefix> prefixesToRemove,
                                                           MultiMap<NodeId, SxpDatabaseBinding> bindingMap) {
        List<SxpDatabaseBinding> removed = new ArrayList<>();
        bindingMap.lock(nodeId);
        try {
            Collection<SxpDatabaseBinding> dbBindings = bindingMap.get(nodeId);
            for (SxpDatabaseBinding dbBinding : dbBindings) {
                if (prefixesToRemove.contains(dbBinding.getIpPrefix())) {
                    removed.add(dbBinding);
                    bindingMap.remove(nodeId, dbBinding);
                }
            }
        } finally {
            bindingMap.unlock(nodeId);
        }
        return removed;
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

    public String getActiveBindingMapName() {
        return activeBindingMapName;
    }

    public String getTentativeBindingMapName() {
        return tentativeBindingMapName;
    }
}
