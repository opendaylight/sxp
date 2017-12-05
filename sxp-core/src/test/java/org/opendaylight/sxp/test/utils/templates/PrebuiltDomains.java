/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.test.utils.templates;

import java.util.Arrays;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.fields.DomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.fields.domains.DomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.DomainFiltersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilterBuilder;

/**
 *
 * @author Martin Dindoffer
 */
public class PrebuiltDomains {

    public static final SxpDomain DEFAULT = new SxpDomainBuilder()
            .setConnectionTemplates(PrebuiltConnectionTemplates.DEFAULT_CTS)
            .setDomainName("defaultDomain")
            .setMasterDatabase(new MasterDatabaseBuilder().build())
            .setDomainFilters(new DomainFiltersBuilder()
                    .setDomainFilter(Arrays.asList(new DomainFilterBuilder()
                            .setFilterName("defaultDomainFilter")
                            .setFilterEntries(new AclFilterEntriesBuilder().build())
                            .setDomains(new DomainsBuilder()
                                    .setDomain(Arrays.asList(new DomainBuilder()
                                            .setName("filteredDomain")
                                            .build()))
                                    .build())
                            .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                            .build()))
                    .build())
            .build();

    public static final SxpDomain DEFAULT_CLASHING_FILTER_DOMAIN_NAME = new SxpDomainBuilder()
            .setConnectionTemplates(PrebuiltConnectionTemplates.DEFAULT_CTS)
            .setDomainName("defaultDomain")
            .setMasterDatabase(new MasterDatabaseBuilder().build())
            .setDomainFilters(new DomainFiltersBuilder()
                    .setDomainFilter(Arrays.asList(new DomainFilterBuilder()
                            .setFilterName("defaultDomainFilter")
                            .setFilterEntries(new AclFilterEntriesBuilder().build())
                            .setDomains(new DomainsBuilder()
                                    .setDomain(Arrays.asList(new DomainBuilder()
                                            .setName("defaultDomain")
                                            .build()))
                                    .build())
                            .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                            .build()))
                    .build())
            .build();
}
