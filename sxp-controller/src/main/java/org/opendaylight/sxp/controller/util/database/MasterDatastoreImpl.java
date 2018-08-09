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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.hazelcast.MasterDBPropagatingListener;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.util.database.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
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
            final T binding = bindingsToDelete.get(dbBinding.getIpPrefix());
            if (binding == null) {
                return false;
            }
            return Objects.equals(binding.getSecurityGroupTag(), dbBinding.getSecurityGroupTag());
        };
    }

    /**
     * @return DatastoreAccess assigned to current node
     */
    public DatastoreAccess getDatastoreAccess() {
        return datastoreAccess;
    }

    @Override
    public synchronized <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings) {
        List<MasterDatabaseBinding> added = new ArrayList<>();
        if (bindings == null || bindings.isEmpty()) {
            return added;
        }
        org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase
                database = datastoreAccess.readSynchronous(getIdentifierBuilder().build(), LogicalDatastoreType.OPERATIONAL);
        Map<IpPrefix, MasterDatabaseBinding> databaseMap;
        if (database != null
                && database.getMasterDatabaseBinding() != null && !database.getMasterDatabaseBinding().isEmpty()) {
            databaseMap = database.getMasterDatabaseBinding()
                    .stream()
                    .collect(Collectors.toMap(SxpBindingFields::getIpPrefix, Function.identity()));
        } else {
            databaseMap = new HashMap<>();
        }

        added.addAll(MasterDatabase.filterIncomingBindings(bindings, databaseMap::get,
                p -> datastoreAccess.checkAndDelete(getIdentifierBuilder(p).build(),
                        LogicalDatastoreType.OPERATIONAL)).values());

        if (!added.isEmpty()) {
            datastoreAccess.merge(getIdentifierBuilder().build(),
                    new MasterDatabaseBuilder().setMasterDatabaseBinding(added).build(),
                    LogicalDatastoreType.OPERATIONAL);
        }

        dbListener.onBindingsAdded(added);
        return added;
    }

    @Override
    public synchronized List<MasterDatabaseBinding> getBindings() {
        List<MasterDatabaseBinding> bindings = new ArrayList<>();
        org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase
                database = datastoreAccess.readSynchronous(getIdentifierBuilder().build(), LogicalDatastoreType.OPERATIONAL);
        if (database != null
                && database.getMasterDatabaseBinding() != null && !database.getMasterDatabaseBinding().isEmpty()) {
            bindings.addAll(database.getMasterDatabaseBinding());
        }
        return bindings;
    }

    @Override
    public synchronized List<MasterDatabaseBinding> getBindings(OriginType origin) {
        return getBindings().stream()
                .filter(binding -> origin.equals(binding.getOrigin()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return Collections.emptyList();
        }
        org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase
                database = datastoreAccess.readSynchronous(getIdentifierBuilder().build(), LogicalDatastoreType.OPERATIONAL);
        if (database == null) {
            return Collections.emptyList();
        }
        List<MasterDatabaseBinding> databaseBindings = database.getMasterDatabaseBinding();
        if (databaseBindings == null || databaseBindings.isEmpty()) {
            return Collections.emptyList();
        }

        List<MasterDatabaseBinding> removedBindings = new ArrayList<>(databaseBindings);
        Map<IpPrefix, T> mapBindingsToDelete = bindings.stream()
                .collect(Collectors.toMap(SxpBindingFields::getIpPrefix, Function.identity()));

        if (databaseBindings.removeIf(bindingIsToBeDeleted(mapBindingsToDelete))) {
            datastoreAccess.put(getIdentifierBuilder().build(), database, LogicalDatastoreType.OPERATIONAL);
            removedBindings.removeAll(databaseBindings);
            dbListener.onBindingsRemoved(removedBindings);
            return removedBindings;
        }
        return Collections.emptyList();
    }

    @Override
    public void close() throws Exception {
        //NOOP
    }
}
