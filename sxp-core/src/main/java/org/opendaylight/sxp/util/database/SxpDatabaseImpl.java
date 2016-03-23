/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.BindingDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SxpDatabaseImpl class contains logic to operate with Database,
 * used for handling Bindings learned from other Nodes
 */
public class SxpDatabaseImpl extends org.opendaylight.sxp.util.database.SxpDatabase {

    private final Map<BindingDatabase.BindingType, Map<NodeId, List<SxpDatabaseBinding>>> bindings = new HashMap<>(2);

    /**
     * Default constructor that sets empty Database
     */
    public SxpDatabaseImpl() {
        bindings.put(BindingDatabase.BindingType.ActiveBindings, new HashMap<>());
        bindings.put(BindingDatabase.BindingType.ReconciledBindings, new HashMap<>());
    }

    @Override protected boolean putBindings(NodeId nodeId, BindingDatabase.BindingType bindingType,
            List<SxpDatabaseBinding> bindings) {
        if (this.bindings.get(bindingType).get(nodeId) == null) {
            return this.bindings.get(bindingType).put(nodeId, bindings) == null;
        }
        return this.bindings.get(bindingType).get(nodeId).addAll(bindings);
    }

    @Override protected List<SxpDatabaseBinding> getBindings(BindingDatabase.BindingType bindingType) {
        List<SxpDatabaseBinding> bindings = new ArrayList<>();
        this.bindings.get(bindingType).values().stream().forEach(bindings::addAll);
        return bindings;
    }

    @Override protected List<SxpDatabaseBinding> getBindings(BindingDatabase.BindingType bindingType, NodeId nodeId) {
        return this.bindings.get(bindingType).get(nodeId) == null ? new ArrayList<>() : this.bindings.get(bindingType)
                .get(nodeId);
    }

    @Override protected boolean deleteBindings(NodeId nodeId, BindingDatabase.BindingType bindingType) {
        return this.bindings.get(bindingType).remove(nodeId) != null;
    }

    @Override protected List<SxpDatabaseBinding> deleteBindings(NodeId nodeId, Set<IpPrefix> prefixes,
            BindingDatabase.BindingType bindingType) {
        List<SxpDatabaseBinding> removed = new ArrayList<>();
        if (this.bindings.get(bindingType).get(nodeId) != null) {
            this.bindings.get(bindingType).get(nodeId).removeIf(b -> {
                boolean result = prefixes.contains(b.getIpPrefix());
                if (result)
                    removed.add(b);
                return result;
            });
        }
        return removed;
    }
}
