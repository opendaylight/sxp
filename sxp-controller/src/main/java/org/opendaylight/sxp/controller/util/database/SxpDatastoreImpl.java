/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.BindingDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.BindingDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.BindingDatabaseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.BindingSources;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.BindingSourcesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.BindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.BindingSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.BindingSourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.SxpDatabaseBindingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.SxpDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class SxpDatastoreImpl extends org.opendaylight.sxp.util.database.SxpDatabase {

    private final DatastoreAccess datastoreAccess;
    private final String nodeId,domain;

    public SxpDatastoreImpl(DatastoreAccess datastoreAccess, String nodeId, String domain) {
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        this.nodeId = Preconditions.checkNotNull(nodeId);
        this.domain = Preconditions.checkNotNull(domain);
        List<BindingDatabase> databases = new ArrayList<>();
        databases.add(
            new BindingDatabaseBuilder().setBindingType(BindingDatabase.BindingType.ActiveBindings)
                .setBindingSources(
                    new BindingSourcesBuilder().setBindingSource(new ArrayList<>()).build())
                .build());
        databases.add(new BindingDatabaseBuilder()
            .setBindingType(BindingDatabase.BindingType.ReconciledBindings).setBindingSources(
                new BindingSourcesBuilder().setBindingSource(new ArrayList<>()).build()).build());
        datastoreAccess.putSynchronous(getIdentifierBuilder().build(),
            new SxpDatabaseBuilder().setBindingDatabase(databases).build(),
            LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * @return InstanceIdentifier pointing to current SxpDatabase
     */
    private InstanceIdentifier.InstanceIdentifierBuilder<SxpDatabase> getIdentifierBuilder() {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class, new NodeKey(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                nodeId)))
                .augmentation(SxpNodeIdentity.class)
                .child(SxpDomains.class)
                .child(SxpDomain.class, new SxpDomainKey(domain))
                .child(SxpDatabase.class);
    }

    /**
     * @param bindingType InstanceIdentifier key
     * @return InstanceIdentifier pointing to specific BindingDatabase
     */
    private InstanceIdentifier.InstanceIdentifierBuilder<BindingDatabase> getIdentifierBuilder(
        BindingDatabase.BindingType bindingType) {
        return getIdentifierBuilder()
            .child(BindingDatabase.class, new BindingDatabaseKey(bindingType));
    }

    /**
     * @param bindingType InstanceIdentifier key
     * @param nodeId      InstanceIdentifier key
     * @return InstanceIdentifier pointing to specific Binding source
     */
    private InstanceIdentifier.InstanceIdentifierBuilder<BindingSource> getIdentifierBuilder(
        BindingDatabase.BindingType bindingType, NodeId nodeId) {
        return getIdentifierBuilder()
            .child(BindingDatabase.class, new BindingDatabaseKey(bindingType))
            .child(BindingSources.class).child(BindingSource.class, new BindingSourceKey(nodeId));
    }

    @Override protected boolean putBindings(NodeId nodeId, BindingDatabase.BindingType bindingType,
            List<SxpDatabaseBinding> bindings) {
        return datastoreAccess.mergeSynchronous(getIdentifierBuilder(bindingType, nodeId).build(),
                new BindingSourceBuilder().setSourceId(nodeId)
                        .setSxpDatabaseBindings(
                                new SxpDatabaseBindingsBuilder().setSxpDatabaseBinding(bindings).build())
                        .build(), LogicalDatastoreType.OPERATIONAL);
    }

    @Override
    protected List<SxpDatabaseBinding> getBindings(BindingDatabase.BindingType bindingType) {
        BindingDatabase result = datastoreAccess
            .readSynchronous(getIdentifierBuilder(bindingType).build(),
                LogicalDatastoreType.OPERATIONAL);
        List<SxpDatabaseBinding> bindings = new ArrayList<>();
        if (result != null && result.getBindingSources() != null
            && result.getBindingSources().getBindingSource() != null) {
            result.getBindingSources().getBindingSource().stream().forEach(s -> {
                if (s.getSxpDatabaseBindings() != null
                    && s.getSxpDatabaseBindings().getSxpDatabaseBinding() != null && !s
                    .getSxpDatabaseBindings().getSxpDatabaseBinding().isEmpty()) {
                    bindings.addAll(s.getSxpDatabaseBindings().getSxpDatabaseBinding());
                }
            });
        }
        return bindings;
    }

    @Override
    protected List<SxpDatabaseBinding> getBindings(BindingDatabase.BindingType bindingType,
        NodeId nodeId) {
        BindingSource result = datastoreAccess
            .readSynchronous(getIdentifierBuilder(bindingType, nodeId).build(),
                LogicalDatastoreType.OPERATIONAL);
        if (result != null && result.getSxpDatabaseBindings() != null) {
            return result.getSxpDatabaseBindings().getSxpDatabaseBinding();
        }
        return new ArrayList<>();
    }

    @Override
    protected boolean deleteBindings(NodeId nodeId, BindingDatabase.BindingType bindingType) {
        return datastoreAccess.checkAndDelete(getIdentifierBuilder(bindingType, nodeId).build(),
            LogicalDatastoreType.OPERATIONAL);
    }

    @Override protected List<SxpDatabaseBinding> deleteBindings(NodeId nodeId, Set<IpPrefix> ipPrefixes,
        BindingDatabase.BindingType bindingType) {
        List<SxpDatabaseBinding> bindingList = getBindings(bindingType, nodeId), removed = new ArrayList<>();
        if (!bindingList.isEmpty() && bindingList.removeIf(b -> {
            boolean result = ipPrefixes.contains(b.getIpPrefix());
            if (result)
                removed.add(b);
            return result;
        })) {
            datastoreAccess.putSynchronous(getIdentifierBuilder(bindingType, nodeId).build(),
                    new BindingSourceBuilder().setSourceId(nodeId)
                            .setSxpDatabaseBindings(
                                    new SxpDatabaseBindingsBuilder().setSxpDatabaseBinding(bindingList).build())
                            .build(), LogicalDatastoreType.OPERATIONAL);
        }
        return removed;
    }
}
