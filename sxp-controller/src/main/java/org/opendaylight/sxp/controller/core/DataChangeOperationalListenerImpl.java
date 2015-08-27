/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.util.database.DatastoreValidator;
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.controller.util.database.access.MasterDatabaseAccessImpl;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
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

public class DataChangeOperationalListenerImpl implements
        org.opendaylight.controller.md.sal.binding.api.DataChangeListener {

    private static DatastoreValidator datastoreValidator;

    private static final Logger LOG = LoggerFactory.getLogger(DataChangeOperationalListenerImpl.class);

    protected static MasterDatabaseInf getDatastoreProviderMaster(String nodeId) {
        return Configuration.getRegisteredNode(nodeId).getBindingMasterDatabase();
    }

    private String nodeId;

    public DataChangeOperationalListenerImpl(String nodeId, DatastoreValidator datastoreValidator) {
        this.nodeId = nodeId;
        DataChangeOperationalListenerImpl.datastoreValidator = datastoreValidator;
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
                // MasterDatabase database = (MasterDatabase) entry.getValue();

                MasterDatabase database;
                try {
                    database = getDatastoreProviderMaster(nodeId).get();
                } catch (Exception e) {
                    LOG.warn("OnDataChanged exception | Master database not defined [node='{}']", nodeId);
                    return;
                }

                for (NodeId nodeId : RpcServiceImpl.getBindingsSources(database)) {
                    if (NodeIdConv.toString(nodeId).equals(this.nodeId)) {
                        continue;
                    }

                    List<PrefixGroup> prefixGroups = new ArrayList<>();
                    for (Binding binding : RpcServiceImpl.getNodeBindings(database, nodeId)) {
                        PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder();
                        prefixGroupBuilder.setSgt(binding.getSgt());
                        prefixGroupBuilder
                                .setBinding(new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding>());

                        for (IpPrefix ipPrefix : binding.getIpPrefix()) {
                            BindingBuilder bindingBuilder = new BindingBuilder();
                            bindingBuilder.setIpPrefix(ipPrefix);
                            prefixGroupBuilder.getBinding().add(bindingBuilder.build());
                        }
                        prefixGroups.add(prefixGroupBuilder.build());
                    }

                    List<Source> sources = new ArrayList<>();
                    SourceBuilder sourceBuilder = new SourceBuilder();
                    sourceBuilder.setBindingSource(DatabaseBindingSource.Local);
                    sourceBuilder.setPrefixGroup(prefixGroups);
                    sources.add(sourceBuilder.build());

                    MasterDatabaseBuilder databaseBuilder = new MasterDatabaseBuilder();
                    databaseBuilder.setSource(sources);

                    String _nodeId = NodeIdConv.toString(nodeId);

                    try {
                        datastoreValidator.validateSxpNodePath(_nodeId, LogicalDatastoreType.OPERATIONAL);
                    } catch (Exception e) {
                        LOG.warn("Failed to create node \"" + nodeId + "\" identity in operationl datastore");
                    }

                    InstanceIdentifier<MasterDatabase> databaseIdentifier = InstanceIdentifier
                            .builder(NetworkTopology.class)
                            .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                            .child(Node.class,
                                    new NodeKey(
                                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                                    _nodeId))).augmentation(SxpNodeIdentity.class)
                            .child(MasterDatabase.class).build();
                    try {
                        datastoreValidator.getDatastoreAccess()
                                .merge(databaseIdentifier, databaseBuilder.build(), LogicalDatastoreType.OPERATIONAL)
                                .get();
                    } catch (CancellationException | ExecutionException | InterruptedException e) {
                        LOG.warn("Failed to create node \"" + nodeId + "\" identity in operationl datastore");
                    }
                }
            }
        }
        change.getUpdatedSubtree();
    }
}
