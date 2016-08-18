/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.controller.util.database.SxpDatastoreImpl;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.handler.ConnectionDecoder;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.SxpDatabase;
import org.opendaylight.sxp.util.exception.node.DomainNotFoundException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.Security;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * SxpDatastoreNode class represent Sxp aware entity that reflect its current stare to Operational Datastore
 */
public class SxpDatastoreNode extends org.opendaylight.sxp.core.SxpNode implements AutoCloseable {

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

            @Override protected void addConnection(String domain, Connection connection) {
                Preconditions.checkNotNull(connection);
                datastoreAccess.checkAndPut(SxpDatastoreNode.getIdentifier(NodeIdConv.toString(owner.getNodeId()))
                                .child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(Preconditions.checkNotNull(domain)))
                                .child(Connections.class)
                                .child(Connection.class,
                                        new ConnectionKey(connection.getPeerAddress(), connection.getTcpPort())), connection,
                        LogicalDatastoreType.OPERATIONAL, false);
            }
        }, HandlerFactory.Position.Begin);
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
        super(Preconditions.checkNotNull(nodeId), Preconditions.checkNotNull(node), new ThreadsWorker());
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        this.nodeId = NodeIdConv.toString(nodeId);
    }

    @Override public boolean addDomain(SxpDomain domain) {
        Preconditions.checkNotNull(domain);
        Preconditions.checkNotNull(domain.getDomainName());
        synchronized (sxpDomains) {
            if (!sxpDomains.containsKey(domain.getDomainName()))
                sxpDomains.put(domain.getDomainName(),
                        org.opendaylight.sxp.core.SxpDomain.createInstance(this, domain.getDomainName(),
                                new SxpDatastoreImpl(datastoreAccess, nodeId, domain.getDomainName()),
                                new MasterDatastoreImpl(datastoreAccess, nodeId, domain.getDomainName())));
            else
                return false;
        }
        return true;
    }

    @Override protected SxpNodeIdentity getNodeIdentity() {
        SxpNodeIdentity
                identity =
                datastoreAccess == null ? null : datastoreAccess.readSynchronous(getIdentifier(nodeId),
                        LogicalDatastoreType.OPERATIONAL);
        return identity != null ? identity : super.getNodeIdentity();
    }

    @Override public SxpConnection addConnection(Connection connection, String domain) {
        return addConnection(
                SxpDatastoreConnection.create(datastoreAccess, this, Preconditions.checkNotNull(connection),
                        Preconditions.checkNotNull(domain)));
    }

    @Override protected Security setPassword(final Security security) {
        Security nodeSecurity = super.setPassword(security);
        if (datastoreAccess != null) {
            datastoreAccess.checkAndMerge(getIdentifier(nodeId).child(Security.class), nodeSecurity,
                    LogicalDatastoreType.OPERATIONAL, true);
        }
        return nodeSecurity;
    }

    @Override public List<MasterDatabaseBinding> putLocalBindingsMasterDatabase(List<MasterDatabaseBinding> bindings,
            String domainName) throws DomainNotFoundException {

        final org.opendaylight.sxp.core.SxpDomain sxpDomain = getDomain(domainName);
        if (sxpDomain == null)
            throw new DomainNotFoundException(getName(), "Domain " + domainName + " not found");
        synchronized (sxpDomain) {
            svcBindingDispatcher.propagateUpdate(null, bindings, getAllOnSpeakerConnections(domainName));
            sxpDomain.pushToSharedMasterDatabases(Collections.emptyList(), bindings);
        }
        return bindings;
    }

    @Override public List<MasterDatabaseBinding> removeLocalBindingsMasterDatabase(List<MasterDatabaseBinding> bindings,
            String domainName) throws DomainNotFoundException {

        final org.opendaylight.sxp.core.SxpDomain sxpDomain = getDomain(domainName);
        if (sxpDomain == null)
            throw new DomainNotFoundException(getName(), "Domain " + domainName + " not found");
        Map<NodeId, SxpBindingFilter> filterMap = SxpDatabase.getInboundFilters(this, domainName);
        synchronized (sxpDomain) {
            svcBindingDispatcher.propagateUpdate(bindings, sxpDomain.getMasterDatabase()
                            .addBindings(SxpDatabase.getReplaceForBindings(bindings, sxpDomain.getSxpDatabase(), filterMap)),
                    getAllOnSpeakerConnections(domainName));
            sxpDomain.pushToSharedMasterDatabases(bindings, Collections.emptyList());
        }
        return bindings;
    }

    public DatastoreAccess getDatastoreAccess() {
        return datastoreAccess;
    }

    @Override public synchronized SxpNode shutdown() {
        datastoreAccess.close();
        return super.shutdown();
    }

    @Override public void close() {
        datastoreAccess.close();
        setTimer(TimerType.RetryOpenTimer, 0);
        getAllConnections().forEach(c -> {
            if (c instanceof SxpDatastoreConnection) {
                ((SxpDatastoreConnection) c).close();
            }
        });
        shutdown();
    }
}
