/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.hazelcast;

import java.util.List;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;

/**
 * Dispatches Update msgs on binding add/remove events.
 */
public class MasterDBPropagatingListener {

    private final BindingDispatcher bindingDispatcher;
    private final SxpDomain domain;

    /**
     * Creates a new propagating listener for in-memory Master DB.
     *
     * @param bindingDispatcher a dispatcher to use for propagating Update msgs
     * @param domain            an SXP Domain used to scrape active Speaker connections as propagation targets
     */
    public MasterDBPropagatingListener(BindingDispatcher bindingDispatcher, SxpDomain domain) {
        this.bindingDispatcher = bindingDispatcher;
        this.domain = domain;
    }

    public void onBindingsAdded(List<MasterDatabaseBinding> added) {
        List<SxpConnection> allOnSpeakerConnections = bindingDispatcher.getOwner()
                .getAllOnSpeakerConnections(domain.getName());
        bindingDispatcher.propagateUpdate(null, added, allOnSpeakerConnections);
    }

    public void onBindingsRemoved(List<MasterDatabaseBinding> removed) {
        List<SxpConnection> allOnSpeakerConnections = bindingDispatcher.getOwner()
                .getAllOnSpeakerConnections(domain.getName());
        bindingDispatcher.propagateUpdate(removed, null, allOnSpeakerConnections);
    }

}
