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

//TODO: Rewrite via HC distributed queries
public class HazelcastBackedSxpDB extends SxpDatabase implements AutoCloseable {

    private final HazelcastInstance hcInstance;
    private final MultiMap<NodeId, SxpDatabaseBinding> activeBindingsMap;
    private final MultiMap<NodeId, SxpDatabaseBinding> tentativeBindingsMap;

    public HazelcastBackedSxpDB() {
        this(new Config());
    }

    public HazelcastBackedSxpDB(Config hcConfig) {
        hcConfig.getSerializationConfig()
                .addSerializerConfig(SxpDBBindingSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSequenceSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSerializer.getSerializerConfig());
        this.hcInstance = Hazelcast.newHazelcastInstance(hcConfig);
        this.activeBindingsMap = hcInstance.getMultiMap("ACTIVE_SXP_BINDINGS"); //TODO: name the maps with nodeId/DomainName
        this.tentativeBindingsMap = hcInstance.getMultiMap("TENTATIVE_SXP_BINDINGS");
    }

    @Override
    protected boolean putBindings(NodeId nodeId, BindingDatabase.BindingType bindingType, Collection<SxpDatabaseBinding> bindings) {
        switch (bindingType) {
            case ActiveBindings:
                return putIntoSubDatabase(nodeId, bindings, activeBindingsMap);
            case TentativeBindings:
                return putIntoSubDatabase(nodeId, bindings, tentativeBindingsMap);
            default:
                return false;
        }
    }

    private boolean putIntoSubDatabase(NodeId nodeId, Collection<SxpDatabaseBinding> bindingsToAdd,
                                       MultiMap<NodeId, SxpDatabaseBinding> bindingMap) {
        boolean result = false;
        bindingMap.lock(nodeId);
        try {
            for (SxpDatabaseBinding binding : bindingsToAdd) {
                if (bindingMap.put(nodeId, binding) && !result) {
                    result = true;
                }
            }
        } finally {
            bindingMap.unlock(nodeId);
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
                return removeFromSubDatabase(nodeId, activeBindingsMap);
            case TentativeBindings:
                return removeFromSubDatabase(nodeId, tentativeBindingsMap);
            default:
                return false;
        }
    }

    private boolean removeFromSubDatabase(NodeId nodeId, MultiMap<NodeId, SxpDatabaseBinding> bindingMap) {
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
        hcInstance.shutdown();
    }
}
