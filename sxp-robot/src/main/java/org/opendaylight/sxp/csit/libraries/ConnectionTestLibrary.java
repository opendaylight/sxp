/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit.libraries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.csit.LibraryServer;
import org.opendaylight.sxp.csit.RobotLibraryServer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

/**
 * Robot library used for connectivity measuring
 */
@RobotKeywords public class ConnectionTestLibrary extends AbstractLibrary {

    private final AtomicLong connectedPeers = new AtomicLong(0), connectingTimeEnd = new AtomicLong(0);
    private final List<SxpConnection> peers = Collections.synchronizedList(new ArrayList<>());
    private final ThreadsWorker worker = new ThreadsWorker(4, 4, 4, 1);
    private long connectingTimeBegin, totalPeers;

    /**
     * Robot library constructor
     */
    public ConnectionTestLibrary() {
        super();
    }

    /**
     * @param libraryServer Server where Library will be added
     */
    public ConnectionTestLibrary(RobotLibraryServer libraryServer) {
        super(libraryServer);
    }

    /**
     * @return Sum of Sxp peers that are currently with state On
     */
    @RobotKeyword("Get Connected Peers") @ArgumentNames({}) public synchronized long getConnectedPeers() {
        connectedPeers.set(peers.parallelStream().map(c -> c.isStateOn() ? 1 : 0).reduce(0, Integer::sum));
        if (connectedPeers.get() >= totalPeers) {
            connectingTimeEnd.set(System.currentTimeMillis());
        }
        return connectedPeers.get();
    }

    /**
     * @return Time elapsed to connect all peers or 0 if some peers are still in progress of connecting
     */
    @RobotKeyword("Get Connect Time") @ArgumentNames({}) public synchronized double getExportTime() {
        long time = connectingTimeEnd.get();
        return time == 0 ? 0 : (time - connectingTimeBegin) / 1000f;
    }

    /**
     * @param peersCount Starts SxpNodes and sets expected Peers count
     */
    @RobotKeyword("Initiate Connecting") @ArgumentNames({"peers_count"}) public synchronized void initiateConnecting(
            String peersCount) {
        close();
        totalPeers = Integer.parseInt(peersCount);
        connectingTimeBegin = System.currentTimeMillis();
        LibraryServer.getNodes().forEach(SxpNode::start);
    }

    @RobotKeyword("Add Node") @ArgumentNames({"node_id", "version", "port", "password"}) @Override
    public synchronized void addNode(String nodeId, String version, String port, String password) {
        LibraryServer.putNode(new SxpNode(new NodeId(nodeId),
                new SxpNodeIdentityBuilder().setSourceIp(new IpAddress(nodeId.toCharArray()))
                        .setCapabilities(Configuration.getCapabilities(Version.Version4))
                        .setEnabled(true)
                        .setVersion(getVersion(version))
                        .setTcpPort(new PortNumber(Integer.parseInt(port)))
                        .setSecurity(new SecurityBuilder().setPassword(
                                password == null || password.isEmpty() ? null : password).build())
                        .setTimers(new TimersBuilder().setRetryOpenTime(5).build())
                        .build(), worker) {

            @Override protected SxpConnection addConnection(SxpConnection connection) {
                peers.add(connection);
                return super.addConnection(connection);
            }
        });
        LibraryServer.getNode(nodeId).addDomain(new SxpDomainBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN).build());
    }

    @Override public synchronized void close() {
        totalPeers = 0;
        connectedPeers.set(0);
        connectingTimeBegin = 0;
        connectingTimeEnd.set(0);
    }
}
