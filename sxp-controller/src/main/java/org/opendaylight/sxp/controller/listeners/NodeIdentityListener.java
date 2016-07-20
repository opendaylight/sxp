/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.core.SxpDatastoreNode;
import org.opendaylight.sxp.controller.listeners.spi.Listener;
import org.opendaylight.sxp.controller.util.io.ConfigLoader;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.opendaylight.sxp.controller.listeners.spi.Listener.Differences.checkDifference;

public class NodeIdentityListener implements DataTreeChangeListener<SxpNodeIdentity> {

    public static final InstanceIdentifier<Topology>
            SUBSCRIBED_PATH =
            InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)));
    protected final DatastoreAccess datastoreAccess;
    private final List<Listener> subListeners;

    public NodeIdentityListener(DatastoreAccess datastoreAccess) {
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        subListeners = new ArrayList<>();
    }

    public void addSubListener(Listener<SxpNodeIdentity, ?> listener) {
        subListeners.add(Preconditions.checkNotNull(listener));
    }

    public ListenerRegistration<DataTreeChangeListener> register(final DataBroker dataBroker,
            final LogicalDatastoreType datastoreType) {
        //noinspection unchecked
        return dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(datastoreType,
                SUBSCRIBED_PATH.child(Node.class).augmentation(SxpNodeIdentity.class)), this);
    }

    @Override public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<SxpNodeIdentity>> changes) {
        changes.stream().forEach(c -> {
            final String nodeId = c.getRootPath().getRootIdentifier().firstKeyOf(Node.class).getNodeId().getValue();
            if (LogicalDatastoreType.CONFIGURATION.equals(c.getRootPath().getDatastoreType())) {
                switch (c.getRootNode().getModificationType()) {
                    case WRITE:
                        if (c.getRootNode().getDataBefore() == null) {
                            ConfigLoader.initTopologyNode(nodeId, LogicalDatastoreType.OPERATIONAL, datastoreAccess);
                            datastoreAccess.putSynchronous(c.getRootPath().getRootIdentifier(),
                                    c.getRootNode().getDataAfter(), LogicalDatastoreType.OPERATIONAL);
                        } else {
                            datastoreAccess.mergeSynchronous(c.getRootPath().getRootIdentifier(),
                                    new SxpNodeIdentityBuilder(c.getRootNode().getDataAfter()).setSxpDomains(null)
                                            .setSxpPeerGroups(null)
                                            .build(), LogicalDatastoreType.OPERATIONAL);
                        }
                    case SUBTREE_MODIFIED:
                        if (c.getRootNode().getDataAfter() != null) {
                            subListeners.forEach(l -> {
                                l.handleChange(l.getModifications(c), c.getRootPath().getDatastoreType(),
                                        c.getRootPath().getRootIdentifier());
                            });
                            break;
                        }
                    case DELETE:
                        datastoreAccess.checkAndDelete(c.getRootPath().getRootIdentifier(),
                                LogicalDatastoreType.OPERATIONAL);
                        break;
                }
            } else {
                switch (c.getRootNode().getModificationType()) {
                    case WRITE:
                        if (c.getRootNode().getDataBefore() == null) {
                            Configuration.register(SxpDatastoreNode.createInstance(
                                    NodeId.getDefaultInstance(Preconditions.checkNotNull(nodeId)),
                                    DatastoreAccess.getInstance(datastoreAccess), c.getRootNode().getDataAfter()))
                                    .start();
                            subListeners.forEach(l -> {
                                l.handleChange(l.getModifications(c), c.getRootPath().getDatastoreType(),
                                        c.getRootPath().getRootIdentifier());
                            });
                        } else if (c.getRootNode().getDataAfter() == null) {
                            Configuration.unregister(Preconditions.checkNotNull(nodeId)).shutdown();
                        }
                        break;
                    case SUBTREE_MODIFIED:
                        if (checkDifference(c, SxpNodeFields::isEnabled)) {
                            if (Preconditions.checkNotNull(c.getRootNode().getDataAfter()).isEnabled()) {
                                Configuration.getRegisteredNode(nodeId).start();
                            } else {
                                Configuration.getRegisteredNode(nodeId).shutdown();
                            }
                        } else if (checkDifference(c, d -> d.getSecurity().getPassword()) || checkDifference(c,
                                SxpNodeFields::getVersion) || checkDifference(c, SxpNodeFields::getTcpPort)
                                || checkDifference(c, SxpNodeFields::getSourceIp)) {
                            Configuration.getRegisteredNode(nodeId).shutdown().start();
                        } else if (checkDifference(c, SxpNodeIdentityFields::getTimers)) {
                            Configuration.getRegisteredNode(nodeId).shutdownConnections();
                        }
                        subListeners.forEach(l -> {
                            l.handleChange(l.getModifications(c), c.getRootPath().getDatastoreType(),
                                    c.getRootPath().getRootIdentifier());
                        });
                        break;
                    case DELETE:
                        ((SxpDatastoreNode) Configuration.unregister(Preconditions.checkNotNull(nodeId))).close();
                        break;
                }
            }
        });
    }

}
