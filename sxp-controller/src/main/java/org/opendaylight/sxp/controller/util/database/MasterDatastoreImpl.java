/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.util.database.access.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MasterDatastoreImpl extends org.opendaylight.sxp.util.database.spi.MasterDatabase {

    private final DatastoreAccess datastoreAccess;
    private final String nodeId;

    public MasterDatastoreImpl(DatastoreAccess datastoreAccess, String nodeId) {
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        this.nodeId = Preconditions.checkNotNull(nodeId);
        datastoreAccess.putSynchronous(getIdentifierBuilder().build(),
                new MasterDatabaseBuilder().setMasterDatabaseBinding(new ArrayList<>()).build(),
                LogicalDatastoreType.OPERATIONAL);
        datastoreAccess.mergeSynchronous(getIdentifierBuilder().build(),
                new MasterDatabaseBuilder().setMasterDatabaseBinding(new ArrayList<>()).build(),
                LogicalDatastoreType.CONFIGURATION);
    }

    private InstanceIdentifier.InstanceIdentifierBuilder<MasterDatabase> getIdentifierBuilder() {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class, new NodeKey(new NodeId(nodeId)))
                .augmentation(SxpNodeIdentity.class)
                .child(MasterDatabase.class);
    }

    private InstanceIdentifier.InstanceIdentifierBuilder<MasterDatabaseBinding> getIdentifierBuilder(
            IpPrefix ipPrefix) {
        return getIdentifierBuilder().child(MasterDatabaseBinding.class, new MasterDatabaseBindingKey(ipPrefix));
    }

    @Override public List<MasterDatabaseBinding> getBindings() {
        List<MasterDatabaseBinding> bindings = new ArrayList<>();
        MasterDatabase
                database =
                datastoreAccess.readSynchronous(getIdentifierBuilder().build(), LogicalDatastoreType.OPERATIONAL);
        if (database != null && database.getMasterDatabaseBinding() != null && !database.getMasterDatabaseBinding()
                .isEmpty()) {
            bindings.addAll(database.getMasterDatabaseBinding());
        }

        database = datastoreAccess.readSynchronous(getIdentifierBuilder().build(), LogicalDatastoreType.CONFIGURATION);
        if (database != null && database.getMasterDatabaseBinding() != null && !database.getMasterDatabaseBinding()
                .isEmpty()) {
            bindings.addAll(database.getMasterDatabaseBinding());
        }
        return bindings;
    }

    private <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings,
            LogicalDatastoreType datastoreType) {
        List<MasterDatabaseBinding> added = new ArrayList<>();
        if (bindings == null || bindings.isEmpty() || datastoreType == null) {
            return added;
        }
        added.addAll(filterIncomingBindings(bindings, t -> {
            MasterDatabaseBinding
                    binding =
                    datastoreAccess.readSynchronous(getIdentifierBuilder(t).build(), LogicalDatastoreType.OPERATIONAL);
            return binding != null ? binding : datastoreAccess.readSynchronous(getIdentifierBuilder(t).build(),
                    LogicalDatastoreType.CONFIGURATION);
        }).values());
        if (!added.isEmpty()) {
            datastoreAccess.mergeSynchronous(getIdentifierBuilder().build(),
                    new MasterDatabaseBuilder().setMasterDatabaseBinding(added).build(), datastoreType);
        }
        return added;
    }

    private <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings,
            LogicalDatastoreType datastoreType) {
        List<MasterDatabaseBinding> removed = new ArrayList<>();
        if (bindings == null || bindings.isEmpty() || datastoreType == null) {
            return removed;
        }
        MasterDatabase database = datastoreAccess.readSynchronous(getIdentifierBuilder().build(), datastoreType);
        if (database == null || database.getMasterDatabaseBinding() == null || database.getMasterDatabaseBinding()
                .isEmpty()) {
            return removed;
        }
        //TODO optimize
        Map<IpPrefix, MasterDatabaseBinding> bindingMap = new HashMap<>();
        database.getMasterDatabaseBinding().forEach(b -> bindingMap.put(b.getIpPrefix(), b));
        database.getMasterDatabaseBinding().clear();

        bindings.stream().forEach(b -> {
            if (bindingMap.containsKey(b.getIpPrefix()) && bindingMap.get(b.getIpPrefix())
                    .getSecurityGroupTag()
                    .getValue()
                    .equals(b.getSecurityGroupTag().getValue())) {
                removed.add(bindingMap.remove(b.getIpPrefix()));
            }
        });
        database.getMasterDatabaseBinding().addAll(bindingMap.values());
        datastoreAccess.putSynchronous(getIdentifierBuilder().build(), database, datastoreType);
        return removed;
    }

    @Override public <T extends SxpBindingFields> List<MasterDatabaseBinding> addLocalBindings(List<T> bindings) {
        return addBindings(bindings, LogicalDatastoreType.CONFIGURATION);
    }

    @Override public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindingsLocal(List<T> bindings) {
        return deleteBindings(bindings, LogicalDatastoreType.CONFIGURATION);
    }

    @Override public <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings) {
        return addBindings(bindings, LogicalDatastoreType.OPERATIONAL);
    }

    @Override public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings) {
        return deleteBindings(bindings, LogicalDatastoreType.OPERATIONAL);
    }
}
