/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.controller.util.database.SxpDatastoreImpl;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.handler.ConnectionDecoder;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.SxpDatabase;
import org.opendaylight.sxp.util.exception.node.DomainNotFoundException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SxpDatastoreNode class represent Sxp aware entity that reflect its current stare to Operational Datastore
 */
public class SxpDatastoreNode extends org.opendaylight.sxp.core.SxpNode implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SxpDatastoreNode.class);

    /**
     * @param nodeId Id representing Node in Topology
     * @return InstanceIdentifier pointing to specific node
     */
    public static InstanceIdentifier<SxpNodeIdentity> getIdentifier(final String nodeId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class, new NodeKey(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                Preconditions.checkNotNull(nodeId))))
                .augmentation(SxpNodeIdentity.class)
                .build();
    }

    /**
     * Create new instance of SxpNode with empty databases
     * and default ThreadWorkers
     *
     * @param nodeId          ID of newly created Node
     * @param datastoreAccess Handle used for writing and reading from Datastore
     * @param node            Node setup data
     * @return New instance of SxpNode
     */
    public static SxpDatastoreNode createInstance(NodeId nodeId, DatastoreAccess datastoreAccess,
            SxpNodeIdentity node) {
        SxpDatastoreNode sxpNode = new SxpDatastoreNode(nodeId, datastoreAccess, node);
        sxpNode.handlerFactoryServer.addDecoder(new ConnectionDecoder(sxpNode) {

            @Override
            protected void addConnection(String domain, Connection connection) {
                Preconditions.checkNotNull(connection);
                datastoreAccess.checkAndPut(SxpDatastoreNode.getIdentifier(NodeIdConv.toString(owner.getNodeId()))
                                .child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(Preconditions.checkNotNull(domain)))
                                .child(Connections.class)
                                .child(Connection.class,
                                        new ConnectionKey(connection.getPeerAddress(), connection.getTcpPort())), connection,
                        LogicalDatastoreType.OPERATIONAL, false);
            }
        }, HandlerFactory.Position.BEGIN);
        return sxpNode;
    }

    private final DatastoreAccess datastoreAccess;
    private final String nodeId;

    /**
     * Default constructor that creates and start SxpDatastoreNode using provided values
     *
     * @param node Node setup data
     */
    protected SxpDatastoreNode(NodeId nodeId, DatastoreAccess datastoreAccess, SxpNodeIdentity node) {
        super(Preconditions.checkNotNull(nodeId), Preconditions.checkNotNull(node), new ThreadsWorker(4, 4, 4, 4));
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        this.nodeId = NodeIdConv.toString(nodeId);
    }

    @Override
    public boolean addDomain(SxpDomain domain) {
        Preconditions.checkNotNull(domain);
        Preconditions.checkNotNull(domain.getDomainName());
        synchronized (sxpDomains) {
            if (!sxpDomains.containsKey(domain.getDomainName()))
                sxpDomains.put(domain.getDomainName(),
                        org.opendaylight.sxp.core.SxpDomain.createInstance(this, domain.getDomainName(),
                                new SxpDatastoreImpl(DatastoreAccess.getInstance(datastoreAccess), nodeId,
                                        domain.getDomainName()),
                                new MasterDatastoreImpl(DatastoreAccess.getInstance(datastoreAccess), nodeId,
                                        domain.getDomainName())));
            else
                return false;
        }
        return true;
    }

    @Override
    public SxpConnection addConnection(Connection connection, String domain) {
        return addConnection(
                SxpDatastoreConnection.create(datastoreAccess, this, Preconditions.checkNotNull(connection),
                        Preconditions.checkNotNull(domain)));
    }

    @Override
    public List<MasterDatabaseBinding> putBindingsMasterDatabase(List<MasterDatabaseBinding> bindings,
            String domainName) throws DomainNotFoundException {

        final org.opendaylight.sxp.core.SxpDomain sxpDomain = getDomain(domainName);
        if (sxpDomain == null)
            throw new DomainNotFoundException(getName(), domainName);
        synchronized (sxpDomain) {
            svcBindingDispatcher.propagateUpdate(null, bindings, getAllOnSpeakerConnections(domainName));
            sxpDomain.pushToSharedMasterDatabases(Collections.emptyList(), bindings);
        }
        return bindings;
    }

    @Override
    public List<MasterDatabaseBinding> removeBindingsMasterDatabase(List<MasterDatabaseBinding> bindings,
            String domainName) throws DomainNotFoundException {

        final org.opendaylight.sxp.core.SxpDomain sxpDomain = getDomain(domainName);
        if (sxpDomain == null)
            throw new DomainNotFoundException(getName(), domainName);
        Map<NodeId, SxpBindingFilter> filterMap = SxpDatabase.getInboundFilters(this, domainName);
        synchronized (sxpDomain) {
            svcBindingDispatcher.propagateUpdate(bindings, sxpDomain.getMasterDatabase()
                            .addBindings(SxpDatabase.getReplaceForBindings(bindings, sxpDomain.getSxpDatabase(), filterMap)),
                    getAllOnSpeakerConnections(domainName));
            sxpDomain.pushToSharedMasterDatabases(bindings, Collections.emptyList());
        }
        return bindings;
    }

    /**
     * @return DatastoreAccess assigned to current node
     */
    public DatastoreAccess getDatastoreAccess() {
        return datastoreAccess;
    }

    @Override
    public synchronized ListenableFuture shutdown() {
        try {
            super.shutdown().get();
            datastoreAccess.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Failed to shutdown SXP node {}", this);
        } catch (ExecutionException e) {
            LOG.error("Failed to shutdown SXP node {}", this);
        }
        return Futures.immediateFuture(Boolean.TRUE);
    }

    @Override
    public void close() {
        final ListenableFuture future = shutdown();
        if (Objects.nonNull(future)) {
            getWorker().addListener(future, () -> getWorker().close());
        } else {
            getWorker().close();
        }
    }
}
