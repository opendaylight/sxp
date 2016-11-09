/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit.libraries;

import org.opendaylight.sxp.csit.RobotLibraryServer;
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

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

    //TODO REMOVE ALREADY IMPLEMENTED
    @RobotKeyword("Add Node") @ArgumentNames({"node_id", "version", "port", "password"}) @Override
    public synchronized void addNode(String nodeId, String version, String port, String password) {
        super.addNode(nodeId, version, port, password);
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
        return false;
    }

    @RobotKeyword("Get Master Database") @ArgumentNames({"node_id"})
    public synchronized String getMasterDatabase(String nodeId) {
        return "TODO";
    }

    @RobotKeyword("Get Sxp Database") @ArgumentNames({"node_id"})
    public synchronized String getSxpDatabase(String nodeId) {
        return "TODO";
    }

    @Override public synchronized void close() {
    }
}
