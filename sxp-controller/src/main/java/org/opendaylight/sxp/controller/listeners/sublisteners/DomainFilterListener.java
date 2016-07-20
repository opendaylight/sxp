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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.spi.ListListener;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.DomainFilters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilterKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.opendaylight.sxp.controller.listeners.spi.Listener.Differences.checkDifference;
import static org.opendaylight.sxp.controller.listeners.spi.Listener.Differences.checkFilterEntries;

public class DomainFilterListener extends ListListener<SxpDomain, DomainFilters, DomainFilter> {

    public DomainFilterListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess, DomainFilters.class);
    }

    @Override
    protected void handleOperational(DataObjectModification<DomainFilter> c, InstanceIdentifier<SxpDomain> identifier,
            SxpNode sxpNode) {
        final String domain = identifier.firstKeyOf(SxpDomain.class).getDomainName();
        LOG.trace("Operational Modification {} {}", getClass(), c.getModificationType());
        switch (c.getModificationType()) {
            case WRITE:
                if (c.getDataBefore() == null) {
                    if (!sxpNode.addFilterToDomain(domain, c.getDataAfter())) {
                        datastoreAccess.checkAndDelete(getIdentifier(c.getDataAfter(), identifier),
                                LogicalDatastoreType.CONFIGURATION);
                    }
                    break;
                } else if (c.getDataAfter() == null) {
                    sxpNode.removeFilterFromDomain(domain, c.getDataBefore().getFilterSpecific(),
                            c.getDataBefore().getFilterName());
                    break;
                }
            case SUBTREE_MODIFIED:
                if (checkFilterEntries(c.getDataBefore() == null ? null : c.getDataBefore().getFilterEntries(),
                        c.getDataAfter() == null ? null : c.getDataAfter().getFilterEntries()) || checkDifference(
                        c.getDataBefore() == null || c.getDataBefore().getDomains() == null ? null : c.getDataBefore()
                                .getDomains()
                                .getDomain(),
                        c.getDataAfter() == null || c.getDataAfter().getDomains() == null ? null : c.getDataAfter()
                                .getDomains()
                                .getDomain())) {
                    sxpNode.updateDomainFilter(domain, c.getDataAfter());
                }
                break;
            case DELETE:
                sxpNode.removeFilterFromDomain(domain,
                        Preconditions.checkNotNull(c.getDataBefore()).getFilterSpecific(),
                        c.getDataBefore().getFilterName());
                break;
        }
    }

    @Override protected InstanceIdentifier<DomainFilter> getIdentifier(DomainFilter d,
            InstanceIdentifier<SxpDomain> parentIdentifier) {
        return parentIdentifier.child(DomainFilters.class)
                .child(DomainFilter.class, new DomainFilterKey(d.getFilterName(), d.getFilterSpecific()));
    }
}
