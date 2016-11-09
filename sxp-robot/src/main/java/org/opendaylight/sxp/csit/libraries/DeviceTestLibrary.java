/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit.libraries;

import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.csit.LibraryServer;
import org.opendaylight.sxp.csit.RobotLibraryServer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

import java.util.Objects;

/**
 * Robot library used for TODO
 */
@RobotKeywords public class DeviceTestLibrary extends AbstractLibrary {

    /**
     * @param libraryServer Server where Library will be added
     */
    public DeviceTestLibrary(RobotLibraryServer libraryServer) {
        super(libraryServer);
    }

    @RobotKeyword("Add Node") @ArgumentNames({"node_id", "version", "port", "password"}) @Override
    public synchronized void addNode(String nodeId, String version, String port, String password) {
        LibraryServer.putNode(SxpNode.createInstance(new NodeId(nodeId),
                new SxpNodeIdentityBuilder().setSourceIp(new IpAddress("0.0.0.0".toCharArray()))
                        .setCapabilities(Configuration.getCapabilities(Version.Version4))
                        .setEnabled(true)
                        .setName(SOURCE)
                        .setVersion(getVersion(version))
                        .setTcpPort(new PortNumber(Integer.parseInt(port)))
                        .setSecurity(new SecurityBuilder().setPassword(
                                password == null || password.isEmpty() ? null : password).build())
                        .setTimers(new TimersBuilder().setRetryOpenTime(5).build())
                        .build()));
    }

    //TODO REMOVE ALREADY IMPLEMENTED
    @RobotKeyword("Start Nodes") @ArgumentNames({}) @Override public synchronized void startNodes() {
        super.startNodes();
    }

    //TODO REMOVE ALREADY IMPLEMENTED
    @RobotKeyword("Add Connection") @ArgumentNames({"version", "mode", "ip", "port", "password", "node_id"}) @Override
    public synchronized void addConnection(String version, String mode, String ip, String port, String password,
            String nodeId) {
        super.addConnection(version, mode, ip, port, password, nodeId);
    }

    //TODO REMOVE ALREADY IMPLEMENTED
    @RobotKeyword("Clean Library") @ArgumentNames({}) @Override public synchronized void cleanLibrary()
            throws Exception {
        super.cleanLibrary();
    }

    @RobotKeyword("Is Peer Connected") @ArgumentNames({"mode", "ip", "port", "node_id"})
    public synchronized boolean isPeerConnected(String mode, String ip, String port, String nodeId) {
        return LibraryServer.getNode(Objects.requireNonNull(nodeId))
                .getAllOnConnections()
                .stream()
                .allMatch(c -> Objects.nonNull(c) && c.getMode().equals(getMode(mode))
                        && c.getDestination().getPort() == Integer.parseInt(port) && Objects.equals(
                        c.getDestination().getAddress().getHostAddress(), ip));
    }

    @RobotKeyword("Get Master Database") @ArgumentNames({"node_id"})
    public synchronized String getMasterDatabase(String nodeId) {
        return "TODO";
    }

    @RobotKeyword("Get Sxp Database") @ArgumentNames({"node_id"})
    public synchronized String getSxpDatabase(String nodeId) {
        return "TODO";
    }

    @RobotKeyword("Add Binding") @ArgumentNames({"prefix", "sgt", "node_id"})
    public synchronized String addBinding(String prefix, String sgt, String node_id) {
        return "TODO";
    }

    @RobotKeyword("Delete Binding") @ArgumentNames({"prefix", "sgt", "node_id"})
    public synchronized String deleteBinding(String prefix, String sgt, String node_id) {
        return "TODO";
    }

    @Override public synchronized void close() {
    }
}
