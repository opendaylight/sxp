/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;

public abstract class SxpDataChangeListener<T extends DataObject> implements DataTreeChangeListener<T> {

    protected static final Logger LOG = LoggerFactory.getLogger(NodeIdentityListener.class.getName());

    public static final InstanceIdentifier<Topology>
            SUBSCRIBED_PATH =
            InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)));
    protected final DatastoreAccess datastoreAccess;
    protected final LogicalDatastoreType datastoreType;

    protected SxpDataChangeListener(DatastoreAccess datastoreAccess, LogicalDatastoreType datastoreType) {
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        this.datastoreType = Preconditions.checkNotNull(datastoreType);
    }

    public ListenerRegistration<DataTreeChangeListener> register(final DataBroker dataBroker) {
        return dataBroker.registerDataTreeChangeListener(getDataTreeIdentifier(), this);
    }

    @Override public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<T>> changes) {
        changes.stream().forEach(c -> {
            final String nodeId = c.getRootPath().getRootIdentifier().firstKeyOf(Node.class).getNodeId().getValue();
            if (LogicalDatastoreType.CONFIGURATION.equals(datastoreType))
                handleConfig(c, nodeId);
            else
                handleNonConfig(c, nodeId);
        });
    }

    protected void handleConfig(DataTreeModification<T> c, final String nodeId) {
        switch (c.getRootNode().getModificationType()) {
            case WRITE:
                datastoreAccess.put(c.getRootPath().getRootIdentifier(), c.getRootNode().getDataAfter(),
                        LogicalDatastoreType.OPERATIONAL);
                break;
            case DELETE:
                if (c.getRootNode().getDataAfter() == null) {
                    datastoreAccess.delete(c.getRootPath().getRootIdentifier(), LogicalDatastoreType.OPERATIONAL);
                }
                break;
        }
    }

    protected abstract DataTreeIdentifier<T> getDataTreeIdentifier();

    protected abstract void handleNonConfig(DataTreeModification<T> c, final String nodeId);
}
