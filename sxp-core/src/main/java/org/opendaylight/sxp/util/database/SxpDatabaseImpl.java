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
import java.util.Iterator;
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

    private final Map<NodeId, List<SxpDatabaseBinding>> bindingMap = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean putBindings(NodeId nodeId, SxpDatabaseBinding.BindingType bindingType,
                                  List<SxpDatabaseBinding> bindings) {
        if (!bindingMap.containsKey(nodeId)) {
            bindingMap.put(nodeId, new ArrayList<>());
        }
        return bindingMap.get(nodeId).addAll(bindings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SxpDatabaseBinding> getBindings(SxpDatabaseBinding.BindingType bindingType) {
        List<SxpDatabaseBinding> bindingsList = new ArrayList<>();
        bindingMap.values().forEach(bindingsList::addAll);
        return bindingsList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SxpDatabaseBinding> getBindings(SxpDatabaseBinding.BindingType bindingType, NodeId nodeId) {
        List<SxpDatabaseBinding> list = new ArrayList<>();
        if (bindingMap.containsKey(nodeId)) {
            for (SxpDatabaseBinding b : bindingMap.get(nodeId)) {
                if (b.getBindingType() == bindingType) {
                    list.add(b);
                }
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean deleteBindings(NodeId nodeId, SxpDatabaseBinding.BindingType bindingType) {
        if (bindingMap.containsKey(nodeId)) {
            return bindingMap.get(nodeId).removeIf(b -> b.getBindingType() == bindingType);
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SxpDatabaseBinding> deleteBindings(NodeId nodeId, Set<IpPrefix> prefixes,
                                                      SxpDatabaseBinding.BindingType bindingType) {
        List<SxpDatabaseBinding> removedBindings = new ArrayList<>();
        if (bindingMap.containsKey(nodeId)) {
            List<SxpDatabaseBinding> nodeBindings = bindingMap.get(nodeId);
            Iterator<SxpDatabaseBinding> bindingListIterator = nodeBindings.iterator();
            while (bindingListIterator.hasNext()) {
                SxpDatabaseBinding binding = bindingListIterator.next();
                if ((binding.getBindingType() == bindingType) && prefixes.contains(binding.getIpPrefix())) {
                    removedBindings.add(binding);
                    bindingListIterator.remove();
                }
            }
        }
        return removedBindings;
    }
}
