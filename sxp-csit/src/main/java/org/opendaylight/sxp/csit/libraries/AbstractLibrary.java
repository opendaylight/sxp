/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit.libraries;

import com.google.common.base.Preconditions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.robotframework.javalib.library.AnnotationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RobotKeywords
public abstract class AbstractLibrary extends AnnotationLibrary implements AutoCloseable {

    public final static String SOURCE = "source";
    protected static final Logger LOG = LoggerFactory.getLogger(ConnectionTestLibrary.class.getName());

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

    public static SxpNode addConnection(SxpNode node, Version version, ConnectionMode mode, String ip, String port,
                                        String password) {
        Preconditions.checkNotNull(node)
                .addConnection(new ConnectionBuilder().setVersion(version)
                        .setPeerAddress(new IpAddress(ip.toCharArray()))
                        .setMode(mode)
                        .setTcpPort(new PortNumber(Integer.parseInt(Preconditions.checkNotNull(port))))
                        .setCapabilities(Configuration.getCapabilities(version))
                        .setConnectionTimers(new ConnectionTimersBuilder().setDeleteHoldDownTime(180)
                                .setHoldTime(90)
                                .setHoldTimeMax(60)
                                .setHoldTimeMax(120)
                                .setHoldTimeMinAcceptable(60)
                                .setReconciliationTime(120)
                                .build())
                        .setPassword(password == null || password.isEmpty() ? null : password)
                        .build(), SxpNode.DEFAULT_DOMAIN);
        return node;
    }

    private Stream<Method> getMethods(Object obj) {
        return Stream.concat(Stream.of(Preconditions.checkNotNull(obj).getClass().getDeclaredMethods()),
                Stream.of(AbstractLibrary.class.getDeclaredMethods()));
    }

    private Optional<Method> getMethod(String keywordName, Object obj) {
        return getMethods(obj).filter(
                m -> m.getAnnotation(RobotKeyword.class) != null && m.getAnnotation(RobotKeyword.class)
                        .value()
                        .equals(keywordName)).findFirst();
    }

    public String getUrl() {
        return "/" + getClass().getSimpleName();
    }

    @Override
    public String[] getKeywordArguments(String keywordName) {
        Optional<Method> method = getMethod(Preconditions.checkNotNull(keywordName), this);
        if (method.isPresent() && method.get().getAnnotation(ArgumentNames.class) != null) {
            return method.get().getAnnotation(ArgumentNames.class).value();
        }
        return new String[0];
    }

    @Override
    public String getKeywordDocumentation(String keywordName) {
        return "TODO";
    }

    @Override
    public Object runKeyword(String keywordName, Object[] args) {
        Optional<Method> method = getMethod(Preconditions.checkNotNull(keywordName), this);
        try {
            return method.isPresent() ? method.get().invoke(this, args) : null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String[] getKeywordNames() {
        return getMethods(this).filter(m -> m.getAnnotation(RobotKeyword.class) != null)
                .map(m -> m.getAnnotation(RobotKeyword.class).value())
                .toArray(String[]::new);
    }

    @RobotKeyword("Add Node")
    @ArgumentNames({"node_id", "version", "port", "password"})
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
                        .setTimers(new TimersBuilder().setRetryOpenTime(1).build())
                        .build()));
    }

    @RobotKeyword("Start Nodes")
    @ArgumentNames({})
    public synchronized void startNodes() {
        LibraryServer.getNodes().forEach(SxpNode::start);
    }

    @RobotKeyword("Add Connection")
    @ArgumentNames({"version", "mode", "ip", "port", "password", "node_id"})
    public synchronized void addConnection(String version, String mode, String ip, String port, String password,
                                           String nodeId) {
        addConnection(LibraryServer.getNode(nodeId), getVersion(version), getMode(mode), ip, port, password);
    }

    @RobotKeyword("Library Echo")
    @ArgumentNames({})
    public String libraryEcho() {
        return "Available processors (cores): " + Runtime.getRuntime().availableProcessors() + System.getProperty(
                "line.separator") + "Total memory available to JVM (bytes): " + Runtime.getRuntime().totalMemory();
    }

    @RobotKeyword("Clean Library")
    @ArgumentNames({})
    public synchronized void cleanLibrary() throws Exception {
        LibraryServer.getNodes().forEach(SxpNode::shutdown);
        LibraryServer.getNodes().forEach(n -> LibraryServer.removeNode(n.getNodeId()));
        close();
    }
}
