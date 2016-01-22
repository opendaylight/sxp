/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import java.util.Map.Entry;

import javax.xml.soap.Node;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sxp.controller.util.database.SxpDatastoreImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataChangeConfigurationListenerImpl implements
        org.opendaylight.controller.md.sal.binding.api.DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(DataChangeConfigurationListenerImpl.class);

    public static final InstanceIdentifier<Topology> SUBSCRIBED_PATH = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class);

    private SxpDatastoreImpl bindingDatastoreProvider;

    public DataChangeConfigurationListenerImpl(SxpDatastoreImpl bindingDatastoreProvider) {
        this.bindingDatastoreProvider = bindingDatastoreProvider;
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getOriginalData().entrySet()) {
            if (Node.class.equals(entry.getKey().getTargetType())) {
                LOG.debug("Data change event: Original node '{}'", entry);
            }
        }
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getCreatedData().entrySet()) {
            if (PrefixGroup.class.equals(entry.getKey().getTargetType())) {
                LOG.debug("Data change event: Created node '{}'", entry);
            }
        }

        change.getOriginalSubtree();
        for (InstanceIdentifier<?> entry : change.getRemovedPaths()) {
            if (Node.class.equals(entry.getTargetType())) {
                LOG.debug("Data change event: Removed node '{}'", entry);
            }
        }
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
            if (PrefixGroup.class.equals(entry.getKey().getTargetType())) {
                LOG.debug("Data change event: Updated node '{}'", entry);
            }
        }
        change.getUpdatedSubtree();
    }
}
