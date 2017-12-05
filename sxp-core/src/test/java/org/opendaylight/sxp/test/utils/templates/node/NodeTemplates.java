/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.test.utils.templates.node;

import java.util.Arrays;
import org.opendaylight.sxp.test.utils.templates.PrebuiltCapabilities;
import org.opendaylight.sxp.test.utils.templates.PrebuiltConnectionTemplates;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;

/**
 *
 * @author Martin Dindoffer
 */
public final class NodeTemplates {

    private NodeTemplates() {
    }

    public static final SxpNodeIdentity TEST_NODE = new SxpNodeIdentityBuilder()
            .setCapabilities(PrebuiltCapabilities.FULL_CAPABILITIES)
            .setDescription("Test node")
            .setEnabled(Boolean.TRUE)
            .setMappingExpanded(65635)
            .setName("Test node")
            .setSecurity(new SecurityBuilder().setPassword("password").build())
            .setSourceIp(new IpAddress(new Ipv4Address("127.0.0.1")))
            .setSxpDomains(new SxpDomainsBuilder()
                    .setSxpDomain(Arrays.asList(
                            new SxpDomainBuilder()
                                    .setConnectionTemplates(PrebuiltConnectionTemplates.DEFAULT_CTS)
                                    .setDomainName("defaultDomain")
                                    .build()))
                    .build())
            .setTcpPort(new PortNumber(6499))
            .build();
}
