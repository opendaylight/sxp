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

@RobotKeywords public class ConnectionTestLibrary extends AbstractLibrary {

    private final AtomicLong connectedPeers = new AtomicLong(0), connectingTimeEnd = new AtomicLong(0);
    private final List<SxpConnection> peers = Collections.synchronizedList(new ArrayList<>());
    private final ThreadsWorker worker = new ThreadsWorker(4, 4, 4, 1);
    private long connectingTimeBegin, totalPeers;

    @RobotKeyword("Get Connected Peers") @ArgumentNames({}) public synchronized long getConnectedPeers() {
        connectedPeers.set(peers.parallelStream().map(c -> c.isStateOn() ? 1 : 0).reduce(0, Integer::sum));
        if (connectedPeers.get() >= totalPeers) {
            connectingTimeEnd.set(System.currentTimeMillis());
        }
        return connectedPeers.get();
    }

    @RobotKeyword("Get Connecting Time Total") @ArgumentNames({}) public synchronized double getExportTimeTotal() {
        long time = connectingTimeEnd.get();
        return time == 0 ? 0 : (time - connectingTimeBegin) / 1000f;
    }

    @RobotKeyword("Get Connecting Time Current") @ArgumentNames({})
    public synchronized double getConnectingTimeCurrent() {
        double time = getExportTimeTotal();
        return time != 0 ? time : (System.currentTimeMillis() - connectingTimeBegin) / 1000f;
    }

    @RobotKeyword("Initiate Connecting") @ArgumentNames({"peers_count"})
    public synchronized void initiateConnectig(String peersCount) {
        close();
        totalPeers = Integer.parseInt(peersCount);
        connectingTimeBegin = System.currentTimeMillis();
        LibraryServer.getNodes().forEach(SxpNode::start);
    }

    @RobotKeyword("Add Node") @ArgumentNames({"node_id", "version", "port", "password"})
    public synchronized void addNode(String nodeId, String version, String port, String password) {
        LibraryServer.putNode(new SxpNode(new NodeId(nodeId),
                new SxpNodeIdentityBuilder().setSourceIp(new IpAddress(nodeId.toCharArray()))
                        .setCapabilities(Configuration.getCapabilities(Version.Version4))
                        .setEnabled(true)
                        .setVersion(getVersion(version))
                        .setTcpPort(new PortNumber(Integer.parseInt(port)))
                        .setSecurity(new SecurityBuilder().setPassword(
                                password == null || password.isEmpty() ? null : password).build())
                        .setTimers(new TimersBuilder().setRetryOpenTime(1).build())
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
