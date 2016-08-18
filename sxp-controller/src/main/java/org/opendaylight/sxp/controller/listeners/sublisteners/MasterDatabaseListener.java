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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.spi.ContainerListener;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MasterDatabaseListener extends ContainerListener<SxpDomain, MasterDatabase> {

    public MasterDatabaseListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess, MasterDatabase.class);
    }

    @Override
    protected void handleConfig(DataObjectModification<MasterDatabase> c, InstanceIdentifier<SxpDomain> identifier) {
    }

    @Override
    protected void handleOperational(DataObjectModification<MasterDatabase> c, InstanceIdentifier<SxpDomain> identifier,
            SxpNode sxpNode) {
        final String domainName = identifier.firstKeyOf(SxpDomain.class).getDomainName();
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
        List<MasterDatabaseBinding> removed = new ArrayList<>(), added = new ArrayList<>();

        removed.addAll(databaseDifference.entriesOnlyOnLeft().values());
        added.addAll(databaseDifference.entriesOnlyOnRight().values());

        databaseDifference.entriesDiffering().forEach((p, d) -> {
            removed.add(d.leftValue());
            added.add(d.rightValue());
        });
        if (!removed.isEmpty()) {
            sxpNode.removeLocalBindingsMasterDatabase(removed, domainName);
        }
        if (!added.isEmpty()) {
            sxpNode.putLocalBindingsMasterDatabase(added, domainName);
        }
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
