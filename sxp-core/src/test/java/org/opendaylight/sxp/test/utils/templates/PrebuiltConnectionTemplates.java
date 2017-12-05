/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.test.utils.templates;

import java.util.Arrays;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SecurityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.ConnectionTemplates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.ConnectionTemplatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.connection.templates.ConnectionTemplate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.connection.templates.ConnectionTemplateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.connection.templates.ConnectionTemplateKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;

/**
 *
 * @author Martin Dindoffer
 */
public final class PrebuiltConnectionTemplates {

    private PrebuiltConnectionTemplates() {
    }

    public static final ConnectionTemplate DEFAULT_CT = new ConnectionTemplateBuilder()
            .setKey(new ConnectionTemplateKey(IpPrefixBuilder.getDefaultInstance("127.0.0.0/24")))
            .setTemplateMode(ConnectionMode.Listener)
            .setTemplatePassword("templatepassword")
            .setTemplatePrefix(IpPrefixBuilder.getDefaultInstance("127.0.0.0/24"))
            .setTemplateSecurityType(SecurityType.Default)
            .build();

    public static final ConnectionTemplates DEFAULT_CTS = new ConnectionTemplatesBuilder()
            .setConnectionTemplate(Arrays.asList(PrebuiltConnectionTemplates.DEFAULT_CT)).build();

}
