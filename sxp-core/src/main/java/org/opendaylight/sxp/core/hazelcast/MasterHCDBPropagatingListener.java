/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.hazelcast;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryMergedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import java.util.Collections;
import java.util.List;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;

/**
 * Dispatches Update msgs on binding add/remove events.
 */
public class MasterHCDBPropagatingListener implements EntryAddedListener<IpPrefix, MasterDatabaseBinding>,
        EntryRemovedListener<IpPrefix, MasterDatabaseBinding>, EntryMergedListener<IpPrefix, MasterDatabaseBinding>,
        EntryUpdatedListener<IpPrefix, MasterDatabaseBinding> {

    private final BindingDispatcher bindingDispatcher;
    private final SxpDomain domain;

    /**
     * Creates a new propagating listener for Hazelcast Master DB.
     *
     * @param bindingDispatcher a dispatcher to use for propagating Update msgs
     * @param domain            an SXP Domain used to scrape active Speaker connections as propagation targets
     */
    public MasterHCDBPropagatingListener(BindingDispatcher bindingDispatcher, SxpDomain domain) {
        this.bindingDispatcher = bindingDispatcher;
        this.domain = domain;
    }

    @Override
    public void entryAdded(EntryEvent<IpPrefix, MasterDatabaseBinding> event) {
        List<SxpConnection> allOnSpeakerConnections = bindingDispatcher.getOwner()
                .getAllOnSpeakerConnections(domain.getName());
        bindingDispatcher.propagateUpdate(null, Collections.singletonList(event.getValue()), allOnSpeakerConnections);
    }

    @Override
    public void entryMerged(EntryEvent<IpPrefix, MasterDatabaseBinding> event) {
        List<SxpConnection> allOnSpeakerConnections = bindingDispatcher.getOwner()
                .getAllOnSpeakerConnections(domain.getName());
        bindingDispatcher.propagateUpdate(null, Collections.singletonList(event.getMergingValue()), allOnSpeakerConnections);
    }

    @Override
    public void entryRemoved(EntryEvent<IpPrefix, MasterDatabaseBinding> event) {
        List<SxpConnection> allOnSpeakerConnections = bindingDispatcher.getOwner()
                .getAllOnSpeakerConnections(domain.getName());
        bindingDispatcher.propagateUpdate(Collections.singletonList(event.getOldValue()), null, allOnSpeakerConnections);
    }

    @Override
    public void entryUpdated(EntryEvent<IpPrefix, MasterDatabaseBinding> event) {
        List<SxpConnection> allOnSpeakerConnections = bindingDispatcher.getOwner()
                .getAllOnSpeakerConnections(domain.getName());
        bindingDispatcher.propagateUpdate(Collections.singletonList(event.getOldValue()), null, allOnSpeakerConnections);
        bindingDispatcher.propagateUpdate(null, Collections.singletonList(event.getValue()), allOnSpeakerConnections);
    }
}
