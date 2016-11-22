/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.spi.ContainerListener;
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MasterDatabaseListener extends ContainerListener<SxpDomain, MasterDatabase> {

    public MasterDatabaseListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess, MasterDatabase.class);
    }

    private void getChanges(DataObjectModification<MasterDatabase> c, List<MasterDatabaseBinding> removed,
            List<MasterDatabaseBinding> added) {
        Map<IpPrefix, MasterDatabaseBinding>
                before =
                c.getDataBefore() == null
                        || c.getDataBefore().getMasterDatabaseBinding() == null ? new HashMap<>() : c.getDataBefore()
                        .getMasterDatabaseBinding()
                        .stream()
                        .filter(this::isLocalBinding)
                        .collect(Collectors.toMap(SxpBindingFields::getIpPrefix, Function.identity())),
                after =
                        c.getDataAfter() == null || c.getDataAfter().getMasterDatabaseBinding()
                                == null ? new HashMap<>() : c.getDataAfter()
                                .getMasterDatabaseBinding()
                                .stream()
                                .filter(this::isLocalBinding)
                                .collect(Collectors.toMap(SxpBindingFields::getIpPrefix, Function.identity()));
        MapDifference<IpPrefix, MasterDatabaseBinding> databaseDifference = Maps.difference(before, after);
        removed.addAll(databaseDifference.entriesOnlyOnLeft().values());
        added.addAll(databaseDifference.entriesOnlyOnRight().values());

        databaseDifference.entriesDiffering().forEach((p, d) -> {
            removed.add(d.leftValue());
            added.add(d.rightValue());
        });
    }

    @Override
    protected void handleConfig(DataObjectModification<MasterDatabase> c, InstanceIdentifier<SxpDomain> identifier) {
        final SxpNode
                sxpNode =
                Configuration.getRegisteredNode(identifier.firstKeyOf(Node.class).getNodeId().getValue());
        if (sxpNode != null) {
            sxpNode.getWorker().executeTaskInSequence(() -> {
                final List<MasterDatabaseBinding> removed = new ArrayList<>(), added = new ArrayList<>();
                getChanges(c, removed, added);
                if (!removed.isEmpty() || !added.isEmpty()) {
                    final String domainName = identifier.firstKeyOf(SxpDomain.class).getDomainName();
                    final DatastoreAccess
                            datastoreAccess =
                            Objects.nonNull(sxpNode.getDomain(domainName)) && Objects.nonNull(
                                    sxpNode.getDomain(domainName)
                                            .getMasterDatabase()) ? ((MasterDatastoreImpl) sxpNode.getDomain(domainName)
                                    .getMasterDatabase()).getDatastoreAccess() : this.datastoreAccess;
                    final Object
                            monitor =
                            Objects.isNull(sxpNode.getDomain(domainName)) ? new Object() : sxpNode.getDomain(
                                    domainName);
                    synchronized (monitor) {
                        MasterDatabase
                                database =
                                datastoreAccess.readSynchronous(identifier.child(MasterDatabase.class),
                                        LogicalDatastoreType.OPERATIONAL);
                        if (database != null && database.getMasterDatabaseBinding() != null) {
                            Set<IpPrefix>
                                    delete =
                                    removed.stream().map(SxpBindingFields::getIpPrefix).collect(Collectors.toSet());
                            database.getMasterDatabaseBinding().removeIf(b -> delete.contains(b.getIpPrefix()));
                            database.getMasterDatabaseBinding().addAll(added);
                            datastoreAccess.put(identifier.child(MasterDatabase.class), database,
                                    LogicalDatastoreType.OPERATIONAL);
                        } else {
                            datastoreAccess.put(identifier.child(MasterDatabase.class), c.getDataAfter(),
                                    LogicalDatastoreType.OPERATIONAL);
                        }
                    }
                }
                return null;
            }, ThreadsWorker.WorkerType.INBOUND);
        } else {
            datastoreAccess.put(identifier.child(MasterDatabase.class), c.getDataAfter(),
                    LogicalDatastoreType.OPERATIONAL);
        }
    }

    @Override
    protected void handleOperational(DataObjectModification<MasterDatabase> c, InstanceIdentifier<SxpDomain> identifier,
            SxpNode sxpNode) {
        sxpNode.getWorker().executeTaskInSequence(() -> {
            final String domainName = identifier.firstKeyOf(SxpDomain.class).getDomainName();
            List<MasterDatabaseBinding> removed = new ArrayList<>(), added = new ArrayList<>();
            getChanges(c, removed, added);
            if (!removed.isEmpty()) {
                sxpNode.removeLocalBindingsMasterDatabase(removed, domainName);
            }
            if (!added.isEmpty()) {
                sxpNode.putLocalBindingsMasterDatabase(added, domainName);
            }
            return null;
        }, ThreadsWorker.WorkerType.OUTBOUND);
    }

    private boolean isLocalBinding(MasterDatabaseBinding binding) {
        if (binding != null && binding.getPeerSequence() != null) {
            if (binding.getPeerSequence().getPeer() == null || binding.getPeerSequence().getPeer().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override protected InstanceIdentifier<MasterDatabase> getIdentifier(MasterDatabase d,
            InstanceIdentifier<SxpDomain> parentIdentifier) {
        return parentIdentifier.child(MasterDatabase.class);
    }
}
