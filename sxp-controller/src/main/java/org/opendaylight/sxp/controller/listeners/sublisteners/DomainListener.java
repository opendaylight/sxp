/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.spi.ListListener;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DomainListener extends ListListener<SxpNodeIdentity, SxpDomains, SxpDomain> {

    public DomainListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess, SxpDomains.class);
    }

    @Override protected void handleOperational(DataObjectModification<SxpDomain> c,
            InstanceIdentifier<SxpNodeIdentity> identifier, SxpNode sxpNode) {
        LOG.trace("Operational Modification {} {}", getClass(), c.getModificationType());
        switch (c.getModificationType()) {
            case WRITE:
                if (c.getDataBefore() == null)
                    sxpNode.addDomain(c.getDataAfter());
            case SUBTREE_MODIFIED:
                if (c.getDataAfter() != null)
                    break;
            case DELETE:
                sxpNode.removeDomain(Preconditions.checkNotNull(c.getDataBefore()).getDomainName()).close();
                break;
        }
    }

    @Override protected InstanceIdentifier<SxpDomain> getIdentifier(SxpDomain d,
            InstanceIdentifier<SxpNodeIdentity> parentIdentifier) {
        return parentIdentifier.child(SxpDomains.class).child(SxpDomain.class, new SxpDomainKey(d.getDomainName()));
    }
}
