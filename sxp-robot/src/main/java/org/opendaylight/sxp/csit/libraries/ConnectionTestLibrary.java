/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit.libraries;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.csit.LibraryServer;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.robotframework.remoteserver.RemoteServer;

/**
 * Robot library used for connectivity measuring
 */
@RobotKeywords
public class ConnectionTestLibrary extends AbstractLibrary {

    private final Random retryTimeGen = new Random();
    private final ThreadsWorker worker = new ThreadsWorker(10, 10, 10, 4);

    private long connectedPeers;
    private long connectingTimeEnd;
    private long connectingTimeBegin;
    private long totalPeers;

    /**
     * @param libraryServer Server where Library will be added
     */
    public ConnectionTestLibrary(RemoteServer libraryServer) {
        super(libraryServer);
    }

    /**
     * @return Sum of Sxp peers that are currently with state On
     */
    @RobotKeyword("Get Connected Peers")
    @ArgumentNames({})
    public synchronized long getConnectedPeers() {
        connectedPeers = LibraryServer.getNodes()
                .stream()
                .flatMap(sxpNode -> sxpNode.getAllConnections().stream())
                .filter(SxpConnection::isStateOn)
                .count();
        if (connectedPeers >= totalPeers) {
            connectingTimeEnd = (System.currentTimeMillis());
        }
        return connectedPeers;
    }

    /**
     * @return Time elapsed to connect all peers or 0 if some peers are still in progress of connecting
     */
    @RobotKeyword("Get Connect Time")
    @ArgumentNames({})
    public synchronized double getConnectTime() {
        long time = connectingTimeEnd;
        return time == 0 ? 0 : (time - connectingTimeBegin) / 1000d;
    }

    /**
     * @param peersCount Starts SxpNodes and sets expected Peers count
     */
    @RobotKeyword("Initiate Connecting")
    @ArgumentNames({"peers_count"})
    public synchronized void initiateConnecting(String peersCount) {
        close();
        totalPeers = Integer.parseInt(peersCount);
        connectingTimeBegin = System.currentTimeMillis();
        LibraryServer.getNodes().forEach(SxpNode::start);
    }

    @RobotKeyword("Add Node")
    @ArgumentNames({"node_id", "version", "port", "password"})
    @Override
    public synchronized void addNode(String nodeId, String version, String port, String password) {
        LibraryServer.putNode(SxpNode.createInstance(new NodeId(nodeId),
                new SxpNodeIdentityBuilder().setSourceIp(IpAddressBuilder.getDefaultInstance(nodeId))
                        .setCapabilities(Configuration.getCapabilities(Version.Version4))
                        .setEnabled(true)
                        .setVersion(getVersion(version))
                        .setTcpPort(new PortNumber(Integer.parseInt(port)))
                        .setSecurity(new SecurityBuilder().setPassword(
                                password == null || password.isEmpty() ? null : password).build())
                        .setTimers(new TimersBuilder().setHoldTime(90)
                                .setHoldTimeMin(90)
                                .setHoldTimeMax(180)
                                .setHoldTimeMinAcceptable(120)
                                .setReconciliationTime(120)
                                .setDeleteHoldDownTime(120)
                                .setRetryOpenTime(5 + (retryTimeGen.nextInt(11)))
                                .build())
                        .build(), new MasterDatabaseImpl(), new SxpDatabaseImpl() {

                    @Override
                    public <T extends SxpBindingFields> List<SxpDatabaseBinding> deleteBindings(
                            NodeId nodeId, List<T> bindings) {
                        synchronized (ConnectionTestLibrary.this) {
                            return Collections.emptyList();
                        }
                    }

                    @Override
                    public <T extends SxpBindingFields> List<SxpDatabaseBinding> addBinding(NodeId nodeId,
                            List<T> bindings) {
                        synchronized (ConnectionTestLibrary.this) {
                            return Collections.emptyList();
                        }
                    }
                }, worker));
        LibraryServer.getNode(nodeId).addDomain(new SxpDomainBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN).build());
    }

    @Override
    public synchronized void close() {
        totalPeers = 0;
        connectedPeers = 0;
        connectingTimeBegin = 0;
        connectingTimeEnd = 0;
    }
}
