/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map.Entry;

public class DataChangeOperationalListenerImpl implements
        org.opendaylight.controller.md.sal.binding.api.DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(DataChangeOperationalListenerImpl.class);

    private String nodeId;

    public DataChangeOperationalListenerImpl(String nodeId) {
        this.nodeId = nodeId;
    }

    public InstanceIdentifier<MasterDatabase> getSubscribedPath() {

        return InstanceIdentifier
                .builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class,
                        new NodeKey(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                        nodeId))).augmentation(SxpNodeIdentity.class).child(MasterDatabase.class)
                .build();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getCreatedData().entrySet()) {
            if (Node.class.equals(entry.getKey().getTargetType())) {
                LOG.debug("Data change event: Created node '{}'", entry);
            }
        }
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getOriginalData().entrySet()) {
            if (Node.class.equals(entry.getKey().getTargetType())) {
                LOG.debug("Data change event: Original node '{}'", entry);
            }
        }
        change.getOriginalSubtree();
        for (InstanceIdentifier<?> entry : change.getRemovedPaths()) {
            if (Node.class.equals(entry.getTargetType())) {
                LOG.debug("Data change event: Removed node '{}'", entry);
            }
        }
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
            if (MasterDatabase.class.equals(entry.getKey().getTargetType())) {
                LOG.debug("Data change event: Updated node '{}'", entry);
            }
        }
        change.getUpdatedSubtree();
    }
}
