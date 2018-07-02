/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.hazelcast.MasterDBPropagatingListener;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.util.database.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class MasterDatastoreImpl extends MasterDatabase {

    private final DatastoreAccess datastoreAccess;
    private final String nodeId, domain;
    private MasterDBPropagatingListener dbListener;

    public MasterDatastoreImpl(DatastoreAccess datastoreAccess, String nodeId, String domain) {
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        this.nodeId = Preconditions.checkNotNull(nodeId);
        this.domain = Preconditions.checkNotNull(domain);
        datastoreAccess.checkAndPut(getIdentifierBuilder().build(),
                new MasterDatabaseBuilder().setMasterDatabaseBinding(new ArrayList<>()).build(),
                LogicalDatastoreType.OPERATIONAL, false);
        datastoreAccess.checkAndPut(getIdentifierBuilder().build(),
                new MasterDatabaseBuilder().setMasterDatabaseBinding(new ArrayList<>()).build(),
                LogicalDatastoreType.CONFIGURATION, false);
    }

    @Override
    public void initDBPropagatingListener(BindingDispatcher dispatcher, org.opendaylight.sxp.core.SxpDomain domain) {
        this.dbListener = new MasterDBPropagatingListener(dispatcher, domain);
    }

    /**
     * @return InstanceIdentifier pointing to current Database
     */
    private InstanceIdentifier.InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase> getIdentifierBuilder() {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class, new NodeKey(new NodeId(nodeId)))
                .augmentation(SxpNodeIdentity.class)
                .child(SxpDomains.class)
                .child(SxpDomain.class, new SxpDomainKey(domain))
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase.class);
    }

    /**
     * @param ipPrefix InstanceIdentifier key
     * @return InstanceIdentifier pointing to Binding
     */
    private InstanceIdentifier.InstanceIdentifierBuilder<MasterDatabaseBinding> getIdentifierBuilder(
            IpPrefix ipPrefix) {
        return getIdentifierBuilder().child(MasterDatabaseBinding.class, new MasterDatabaseBindingKey(ipPrefix));
    }

    /**
     * Decide if binding is to be removed according to its match in deletion map with key {@link IpPrefix}.
     */
    private <T extends SxpBindingFields> Predicate<MasterDatabaseBinding> bindingIsToBeDeleted(
            final Map<IpPrefix, T> bindingsToDelete) {
        return dbBinding -> {
            final IpPrefix ipPrefix = dbBinding.getIpPrefix();
            return bindingsToDelete.containsKey(ipPrefix)
                    && Objects.equals(bindingsToDelete.get(ipPrefix).getSecurityGroupTag(), dbBinding.getSecurityGroupTag());
        };
    }

    /**
     * @return DatastoreAccess assigned to current node
     */
    public DatastoreAccess getDatastoreAccess() {
        return datastoreAccess;
    }

    @Override
    synchronized public List<MasterDatabaseBinding> getBindings() {
        List<MasterDatabaseBinding> bindings = new ArrayList<>();
        org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase
                database =
                datastoreAccess.readSynchronous(getIdentifierBuilder().build(), LogicalDatastoreType.OPERATIONAL);
        if (database != null && database.getMasterDatabaseBinding() != null && !database.getMasterDatabaseBinding()
                .isEmpty()) {
            bindings.addAll(database.getMasterDatabaseBinding());
        }
        Set<IpPrefix>
                prefixSet =
                bindings.parallelStream().map(SxpBindingFields::getIpPrefix).collect(Collectors.toSet());
        database = datastoreAccess.readSynchronous(getIdentifierBuilder().build(), LogicalDatastoreType.CONFIGURATION);
        if (database != null && database.getMasterDatabaseBinding() != null && !database.getMasterDatabaseBinding()
                .isEmpty()) {
            database.getMasterDatabaseBinding().forEach(b -> {
                if (!prefixSet.contains(b.getIpPrefix()))
                    bindings.add(b);
            });
        }
        return bindings;
    }

    @Override
    synchronized public List<MasterDatabaseBinding> getLocalBindings() {
        return getBindings().stream()
                .filter(b -> b.getPeerSequence() == null || b.getPeerSequence().getPeer() == null || b.getPeerSequence()
                        .getPeer()
                        .isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * @param bindings      Bindings that will be added
     * @param datastoreType Defines from where bindings will be added
     * @param <T>           Any type extending SxpBindingFields
     * @return List of added Bindings
     */
    private <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings,
            LogicalDatastoreType datastoreType) {
        List<MasterDatabaseBinding> added = new ArrayList<>();
        if (bindings == null || bindings.isEmpty() || datastoreType == null) {
            return added;
        }
        final Map<IpPrefix, MasterDatabaseBinding> databaseMaster;
        org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase
                database =
                datastoreAccess.readSynchronous(getIdentifierBuilder().build(), datastoreType);
        if (Objects.nonNull(database) && Objects.nonNull(database.getMasterDatabaseBinding())
                && !database.getMasterDatabaseBinding().isEmpty()) {
            databaseMaster =
                    database.getMasterDatabaseBinding()
                            .parallelStream()
                            .collect(Collectors.toMap(SxpBindingFields::getIpPrefix, b -> b));
        } else {
            databaseMaster = new HashMap<>();
        }
        added.addAll(filterIncomingBindings(bindings, databaseMaster::get,
                p -> datastoreAccess.checkAndDelete(getIdentifierBuilder(p).build(),
                        LogicalDatastoreType.OPERATIONAL)).values()
                );
        if (!added.isEmpty()) {
            datastoreAccess.merge(getIdentifierBuilder().build(),
                    new MasterDatabaseBuilder().setMasterDatabaseBinding(added).build(), datastoreType);
        }
        dbListener.onBindingsAdded(added);
        return added;
    }

    /**
     * @param bindings      Bindings to be removed
     * @param datastoreType Defines from where bindings will be removed
     * @param <T>           Any type extending SxpBindingFields
     * @return List of removed Bindings
     */
    private <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings,
            LogicalDatastoreType datastoreType) {
        if (bindings == null || bindings.isEmpty() || datastoreType == null) {
            return Collections.emptyList();
        }
        org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase
                database =
                datastoreAccess.readSynchronous(getIdentifierBuilder().build(), datastoreType);
        if (database == null) {
            return Collections.emptyList();
        }
        final List<MasterDatabaseBinding> databaseBindings = database.getMasterDatabaseBinding();
        if (databaseBindings == null || databaseBindings.isEmpty()) {
            return Collections.emptyList();
        }

        final List<MasterDatabaseBinding> removedBindings = new ArrayList<>(databaseBindings);
        final Map<IpPrefix, T> mapBindingsToDelete = bindings.stream()
                .collect(Collectors.toMap(SxpBindingFields::getIpPrefix, Functions.identity()));

        if (databaseBindings.removeIf(bindingIsToBeDeleted(mapBindingsToDelete))) {
            datastoreAccess.put(getIdentifierBuilder().build(), database, datastoreType);
            removedBindings.removeAll(databaseBindings);
            dbListener.onBindingsRemoved(removedBindings);
            return removedBindings;
        }

        return Collections.emptyList();
    }

    @Override
    synchronized public <T extends SxpBindingFields> List<MasterDatabaseBinding> addLocalBindings(List<T> bindings) {
        return addBindings(bindings, LogicalDatastoreType.CONFIGURATION);
    }

    @Override
    synchronized public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindingsLocal(List<T> bindings) {
        //Does not wait for config mirroring
        deleteBindings(bindings, LogicalDatastoreType.OPERATIONAL);
        return deleteBindings(bindings, LogicalDatastoreType.CONFIGURATION);
    }

    @Override
    synchronized public <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings) {
        return addBindings(bindings, LogicalDatastoreType.OPERATIONAL);
    }

    @Override
    synchronized public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings) {
        return deleteBindings(bindings, LogicalDatastoreType.OPERATIONAL);
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindingsWithPriorities(List<T> bindings) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public void close() throws Exception {
        //NOOP
    }
}
