/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import com.google.common.base.Preconditions;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * SxpDomain class represent layer that isolate Connections
 * and Bindings learned from them to only Peers of the same domain.
 * Sharing of Bindings can be achieved by filters between domains.
 */
public class SxpDomain implements AutoCloseable {

    private final MasterDatabaseInf masterDatabase;
    private final SxpDatabaseInf sxpDatabase;
    private final String name;
    private final Map<String, SxpBindingFilter<?>> filters = new HashMap<>();
    private final Map<InetAddress, SxpConnection> connections = new HashMap<>();

    /**
     * @param name           Name of Domain
     * @param sxpDatabase    SxpDatabase that will be use to store incoming Bindings
     * @param masterDatabase MasterDatabase that will be used to store filtered Bindings
     */
    public SxpDomain(String name, SxpDatabaseInf sxpDatabase, MasterDatabaseInf masterDatabase) {
        this.masterDatabase = Preconditions.checkNotNull(masterDatabase);
        this.sxpDatabase = Preconditions.checkNotNull(sxpDatabase);
        this.name = Preconditions.checkNotNull(name);
    }

    /**
     * @param owner  SxpNode to which Domain belongs
     * @param domain SxpDomain initializer
     */
    public SxpDomain(SxpNode owner,
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain domain) {
        Preconditions.checkNotNull(domain);
        this.masterDatabase = Preconditions.checkNotNull(new MasterDatabaseImpl());
        this.sxpDatabase = Preconditions.checkNotNull(new SxpDatabaseImpl());
        this.name = Preconditions.checkNotNull(domain.getDomainName());
        if (domain.getMasterDatabase() != null) {
            masterDatabase.addBindings(domain.getMasterDatabase().getMasterDatabaseBinding());
        }
        if (domain.getConnections() != null && domain.getConnections().getConnection() != null) {
            domain.getConnections().getConnection().forEach(c -> {
                putConnection(new SxpConnection(owner, c, name));
            });
        }
    }

    /**
     * @return MasterDatabase assigned to this domain
     */
    public MasterDatabaseInf getMasterDatabase() {
        return masterDatabase;
    }

    /**
     * @return SxpDatabase assigned into this domain
     */
    public SxpDatabaseInf getSxpDatabase() {
        return sxpDatabase;
    }

    /**
     * @return Name of this domain
     */
    public String getName() {
        return name;
    }

    /**
     * @return Unmodifiable collection of SxpConnections in this domain
     */
    public synchronized Collection<SxpConnection> getConnections() {
        return Collections.unmodifiableCollection(connections.values());
    }

    /**
     * Gets SxpConnection from Domain
     *
     * @param address Address of remote peer
     * @return SxpConnection specified by address or null
     */
    public synchronized SxpConnection getConnection(InetSocketAddress address) {
        return connections.get(Preconditions.checkNotNull(address).getAddress());
    }

    /**
     * Check if Domain contains SxpConnection
     *
     * @param address Address of Peer to look for
     * @return If Domain contains remote peer with specified address
     */
    public synchronized boolean hasConnection(InetSocketAddress address) {
        return connections.containsKey(Preconditions.checkNotNull(address).getAddress());
    }

    /**
     * Adds SxpConnection into domain
     *
     * @param connection SxpConnection that will be added
     * @return Added SxpConnection
     */
    public synchronized SxpConnection putConnection(SxpConnection connection) {
        if (connections.containsKey(Preconditions.checkNotNull(connection).getDestination().getAddress()))
            throw new IllegalArgumentException(
                    "Connection " + connection + " with destination " + connection.getDestination() + " exist.");
        connections.put(Preconditions.checkNotNull(connection).getDestination().getAddress(), connection);
        return connection;
    }

    /**
     * @param address InetSocketAddress of remote peer that will be removed
     * @return Removed SxpConnection or null if there was no connection with specified address
     */
    public synchronized SxpConnection removeConnection(InetSocketAddress address) {
        return connections.remove(Preconditions.checkNotNull(address).getAddress());
    }

    @Override public synchronized void close() {
        connections.values().forEach(SxpConnection::shutdown);
    }
}
