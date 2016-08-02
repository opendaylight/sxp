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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionTemplateFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.ConnectionTemplates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.connection.templates.ConnectionTemplate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.connection.templates.ConnectionTemplateKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.opendaylight.sxp.controller.listeners.spi.Listener.Differences.checkDifference;

public class ConnectionTemplateListener extends ListListener<SxpDomain, ConnectionTemplates, ConnectionTemplate> {

    public ConnectionTemplateListener(DatastoreAccess datastoreAccess) {
        super(datastoreAccess, ConnectionTemplates.class);
    }

    @Override protected void handleOperational(DataObjectModification<ConnectionTemplate> c,
            InstanceIdentifier<SxpDomain> identifier, SxpNode sxpNode) {
        final String nodeId = identifier.firstKeyOf(Node.class).getNodeId().getValue(),
                domainName = identifier.firstKeyOf(SxpDomain.class).getDomainName();
        org.opendaylight.sxp.core.SxpDomain domain = sxpNode.getDomain(domainName);
        if (domain == null) {
            LOG.error("Operational Modification {} {} could not get SXPDomain {}", getClass(), c.getModificationType(),
                    nodeId);
            return;
        }
        LOG.trace("Operational Modification {} {}", getClass(), c.getModificationType());
        switch (c.getModificationType()) {
            case WRITE:
                if (c.getDataBefore() == null) {
                    domain.addConnectionTemplate(c.getDataAfter());
                    break;
                } else if (c.getDataAfter() == null) {
                    domain.removeConnectionTemplate(c.getDataBefore().getTemplatePrefix());
                    break;
                }
            case SUBTREE_MODIFIED:
                if (checkDifference(c, SxpConnectionTemplateFields::getTemplatePrefix) ||
                        checkDifference(c, SxpConnectionTemplateFields::getTemplatePassword) ||
                        checkDifference(c, SxpConnectionTemplateFields::getTemplateMode) ||
                        checkDifference(c, SxpConnectionTemplateFields::getTemplateTcpPort) ||
                        checkDifference(c, SxpConnectionTemplateFields::getTemplateVersion)) {
                    domain.removeConnectionTemplate(c.getDataBefore().getTemplatePrefix());
                    domain.addConnectionTemplate(c.getDataAfter());
                }
                break;
            case DELETE:
                if (c.getDataBefore() != null)
                    domain.removeConnectionTemplate(c.getDataBefore().getTemplatePrefix());
                break;
        }
    }

    @Override protected InstanceIdentifier<ConnectionTemplate> getIdentifier(ConnectionTemplate d,
            InstanceIdentifier<SxpDomain> parentIdentifier) {
        Preconditions.checkNotNull(d);
        Preconditions.checkNotNull(parentIdentifier);
        return parentIdentifier.child(ConnectionTemplates.class)
                .child(ConnectionTemplate.class, new ConnectionTemplateKey(d.getTemplatePrefix()));
    }

}
