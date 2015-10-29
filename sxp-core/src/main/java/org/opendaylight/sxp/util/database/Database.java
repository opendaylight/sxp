/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;

public class Database {

    /**
     * Assign NodeId to PrefixGroups using specified PrefixGroups data
     *
     * @param nodeId       NodeId to be set
     * @param prefixGroups PrefixGroups which data will be used
     * @return List of PrefixGroup generated with specified data
     */
    public static List<PrefixGroup> assignPrefixGroups(NodeId nodeId, List<PrefixGroup> prefixGroups) {
        DateAndTime timestamp = TimeConv.toDt(System.currentTimeMillis());

        List<PrefixGroup> _prefixGroups = new ArrayList<>();
        if (nodeId == null || prefixGroups == null) {
            return _prefixGroups;
        }
        for (PrefixGroup prefixGroup : prefixGroups) {
            if (prefixGroup.getSgt() == null || prefixGroup.getBinding() == null) {
                continue;
            }
            PrefixGroupBuilder _prefixGroupBuilder = new PrefixGroupBuilder();
            _prefixGroupBuilder.setSgt(prefixGroup.getSgt());

            List<Binding> _bindings = new ArrayList<>();
            for (Binding binding : prefixGroup.getBinding()) {
                BindingBuilder bindingBuilder = new BindingBuilder(binding);
                bindingBuilder.setAction(DatabaseAction.Add);
                bindingBuilder.setChanged(true);

                List<NodeId> nodeIds = new ArrayList<>();
                nodeIds.add(nodeId);
                // Unique array.
                bindingBuilder.setPeerSequence(NodeIdConv.createPeerSequence(nodeIds));
                // Unique array.
                bindingBuilder.setSources(NodeIdConv.createSources(nodeIds));

                if (binding.getTimestamp() == null || binding.getTimestamp().getValue() == null) {
                    bindingBuilder.setTimestamp(timestamp);
                }
                _bindings.add(bindingBuilder.build());
            }
            _prefixGroupBuilder.setBinding(_bindings);
            _prefixGroups.add(_prefixGroupBuilder.build());
        }
        return _prefixGroups;
    }

    /**
     * Creates PrefixGroup
     *
     * @param sgt      Sgt that will be assigned to PrefixGroup
     * @param bindings Bindings that will be included into PrefixGroup
     * @return PrefixGroup created using provided data
     * @throws UnknownPrefixException If one of bindings is null or empty
     */
    public static PrefixGroup createPrefixGroup(int sgt, String... bindings) throws UnknownPrefixException {
        PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder();
        prefixGroupBuilder.setSgt(new Sgt(sgt));
        List<Binding> _bindings = new ArrayList<Binding>();
        for (String binding : bindings) {
            BindingBuilder bindingBuilder = new BindingBuilder();
            bindingBuilder.setIpPrefix(IpPrefixConv.createPrefix(binding));
            _bindings.add(bindingBuilder.build());
        }
        prefixGroupBuilder.setBinding(_bindings);
        return prefixGroupBuilder.build();
    }

}
