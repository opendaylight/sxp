/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit.libraries;

import com.google.common.base.Preconditions;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.csit.LibraryServer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.robotframework.remoteserver.RemoteServer;
import org.robotframework.remoteserver.library.AbstractClassLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Robot library containing common support for libraries
 */
@RobotKeywords public abstract class AbstractLibrary extends AbstractClassLibrary implements AutoCloseable {

    public final static String SOURCE = "source";
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractLibrary.class.getName());
    protected final ConnectionTimersBuilder connectionTimers = new ConnectionTimersBuilder();

    /**
     * @param libraryServer Server where Library will be added
     */
    protected AbstractLibrary(RemoteServer libraryServer) {
        super(libraryServer);
        connectionTimers.setDeleteHoldDownTime(180)
                .setHoldTime(90)
                .setHoldTimeMin(90)
                .setHoldTimeMax(180)
                .setHoldTimeMinAcceptable(120)
                .setReconciliationTime(120);
    }

    /**
     * @param val String containing Version
     * @return Parsed Version
     */
    public static Version getVersion(String val) {
        if (val != null && !val.isEmpty()) {
            switch (val.toLowerCase()) {
                case "version1":
                    return Version.Version1;
                case "version2":
                    return Version.Version2;
                case "version3":
                    return Version.Version3;
                case "version4":
                    return Version.Version4;
            }
        }
        return Version.Version4;
    }

    /**
     * @param val String containing ConnectionMode
     * @return Parsed ConnectionMode
     */
    public static ConnectionMode getMode(String val) {
        if (val != null && !val.isEmpty()) {
            switch (val.toLowerCase()) {
                case "speaker":
                    return ConnectionMode.Speaker;
                case "listener":
                    return ConnectionMode.Listener;
                case "both":
                    return ConnectionMode.Both;
            }
        }
        return ConnectionMode.None;
    }

    /**
     * Adds connection to SxpNode
     *
     * @param node     SxpNode where connection will be added
     * @param version  Version used
     * @param mode     Mode used
     * @param ip       Ip of remote peer
     * @param port     Port of remote peer
     * @param password Password for TCP-MD5
     * @return SxpNode where connection was added
     */
    public SxpNode addConnection(SxpNode node, Version version, ConnectionMode mode, String ip, String port,
            String password) {
        Preconditions.checkNotNull(node)
                .addConnection(new ConnectionBuilder().setVersion(version)
                        .setPeerAddress(new IpAddress(ip.toCharArray()))
                        .setMode(mode)
                        .setTcpPort(new PortNumber(Integer.parseInt(Preconditions.checkNotNull(port))))
                        .setCapabilities(Configuration.getCapabilities(version))
                        .setConnectionTimers(connectionTimers.build())
                        .setPassword(password == null || password.isEmpty() ? null : password)
                        .build(), SxpNode.DEFAULT_DOMAIN);
        return node;
    }

    /**
     * @return Url on witch Library is placed
     */
    @Override public String getURI() {
        return getClass().getSimpleName();
    }

    /**
     * Adds Node that is used as Source of bindings export
     *
     * @param nodeId   Id of SxpNode
     * @param version  Default version used
     * @param port     Port of SxpNode
     * @param password Password used by TCP-MD5
     */
    @RobotKeyword("Add Node") @ArgumentNames({"node_id", "version", "port", "password"})
    public synchronized void addNode(String nodeId, String version, String port, String password) {
        LibraryServer.putNode(SxpNode.createInstance(new NodeId(nodeId),
                new SxpNodeIdentityBuilder().setSourceIp(new IpAddress(nodeId.toCharArray()))
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

    /**
     * Starts every node.
     */
    @RobotKeyword("Start Nodes") @ArgumentNames({}) public synchronized void startNodes() {
        LibraryServer.getNodes().forEach(SxpNode::start);
    }

    /**
     * Add connection to Sxp node
     *
     * @param version  Version used
     * @param mode     Mode used
     * @param ip       Ip of remote peer
     * @param port     Port of remote peer
     * @param password Password for TCP-MD5
     * @param nodeId   SxpNode id where connection will be added
     */
    @RobotKeyword("Add Connection") @ArgumentNames({"version", "mode", "ip", "port", "password", "node_id"})
    public synchronized void addConnection(String version, String mode, String ip, String port, String password,
            String nodeId) {
        addConnection(LibraryServer.getNode(nodeId), getVersion(version), getMode(mode), ip, port, password);
    }

    /**
     * Clean Library resources
     *
     * @throws Exception If error occurs
     */
    @RobotKeyword("Clean Library") @ArgumentNames({}) public synchronized void cleanLibrary() throws Exception {
        LibraryServer.getNodes().forEach(SxpNode::shutdown);
        LibraryServer.clearNodes();
        close();
    }
}
