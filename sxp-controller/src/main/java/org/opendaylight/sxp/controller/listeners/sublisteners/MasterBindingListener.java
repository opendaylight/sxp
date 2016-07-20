/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.spi.ListListener;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MasterBindingListener extends ListListener<SxpDomain, MasterDatabase, MasterDatabaseBinding> {

    public MasterBindingListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess, MasterDatabase.class);
    }

    @Override protected void handleOperational(DataObjectModification<MasterDatabaseBinding> c,
            InstanceIdentifier<SxpDomain> identifier, SxpNode sxpNode) {
        if (!LOG.isDebugEnabled())
            return;
        final String domainName = identifier.firstKeyOf(SxpDomain.class).getDomainName();
        //Logging of Bindings changes
        switch (c.getModificationType()) {
            case WRITE:
                if (c.getDataBefore() == null && c.getDataAfter() != null)
                    LOG.debug("{} [{}] Added Binding [{}|{}]", sxpNode, domainName,
                            IpPrefixConv.toString(c.getDataAfter().getIpPrefix()),
                            c.getDataAfter().getSecurityGroupTag().getValue());
                if (c.getDataAfter() != null)
                    break;
            case DELETE:
                LOG.debug("{} [{}] Removed Binding [{}|{}]", sxpNode, domainName,
                        IpPrefixConv.toString(c.getDataBefore().getIpPrefix()),
                        c.getDataBefore().getSecurityGroupTag().getValue());
                break;
        }
    }

    @Override protected InstanceIdentifier<MasterDatabaseBinding> getIdentifier(MasterDatabaseBinding d,
            InstanceIdentifier<SxpDomain> parentIdentifier) {
        return parentIdentifier.child(MasterDatabase.class)
                .child(MasterDatabaseBinding.class, new MasterDatabaseBindingKey(d.getIpPrefix()));
    }

}
