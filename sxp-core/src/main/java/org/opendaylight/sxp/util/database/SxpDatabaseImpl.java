/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.database;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

/**
 * SxpDatabaseImpl class contains logic to operate with Database,
 * used for handling Bindings learned from other Nodes
 */
public class SxpDatabaseImpl extends org.opendaylight.sxp.util.database.SxpDatabase {

    /**
     * A map of bindings.
     * Bindings are mapped firstly via their type
     * and then via corresponding NodeIds
     */
    private final Map<SxpDatabaseBinding.BindingType, Map<NodeId, List<SxpDatabaseBinding>>> bindings =
            new EnumMap(SxpDatabaseBinding.BindingType.class);

    /**
     * Default constructor that sets empty Database.
     */
    public SxpDatabaseImpl() {
        bindings.put(SxpDatabaseBinding.BindingType.ActiveBindings, new HashMap<>());
        bindings.put(SxpDatabaseBinding.BindingType.ReconciledBindings, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean putBindings(NodeId nodeId, SxpDatabaseBinding.BindingType bindingType,
            List<SxpDatabaseBinding> bindings) {
        if (this.bindings.get(bindingType).get(nodeId) == null) {
            return this.bindings.get(bindingType).put(nodeId, bindings) == null;
        }
        return this.bindings.get(bindingType).get(nodeId).addAll(bindings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SxpDatabaseBinding> getBindings(SxpDatabaseBinding.BindingType bindingType) {
        List<SxpDatabaseBinding> bindingsList = new ArrayList<>();
        this.bindings.get(bindingType).values().forEach(bindingsList::addAll);
        return bindingsList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SxpDatabaseBinding> getBindings(SxpDatabaseBinding.BindingType bindingType, NodeId nodeId) {
        return this.bindings.get(bindingType).get(nodeId) == null ? new ArrayList<>() : this.bindings.get(bindingType)
                .get(nodeId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean deleteBindings(NodeId nodeId, SxpDatabaseBinding.BindingType bindingType) {
        return this.bindings.get(bindingType).remove(nodeId) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SxpDatabaseBinding> deleteBindings(NodeId nodeId, Set<IpPrefix> prefixes,
                                                      SxpDatabaseBinding.BindingType bindingType) {
        List<SxpDatabaseBinding> removed = new ArrayList<>();
        if (this.bindings.get(bindingType).get(nodeId) != null) {
            this.bindings.get(bindingType).get(nodeId).removeIf(b -> {
                boolean result = prefixes.contains(b.getIpPrefix());
                if (result) {
                    removed.add(b);
                }
                return result;
            });
        }
        return removed;
    }
}
