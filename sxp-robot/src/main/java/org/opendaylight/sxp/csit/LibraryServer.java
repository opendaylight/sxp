/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.csit.libraries.AbstractLibrary;
import org.opendaylight.sxp.csit.libraries.ConnectionTestLibrary;
import org.opendaylight.sxp.csit.libraries.DeviceTestLibrary;
import org.opendaylight.sxp.csit.libraries.ExportTestLibrary;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.robotframework.remoteserver.RemoteServer;
import org.robotframework.remoteserver.RemoteServerImpl;

/**
 * Remote Robot library server providing libraries to robot framework
 */
public class LibraryServer extends RemoteServerImpl implements RemoteServer {

    private static final Map<String, SxpNode> nodes = new ConcurrentHashMap<>();
    private static final List<AbstractLibrary> libraryList = new ArrayList<>();

    /**
     * Standalone version used for remote testing purposes
     *
     * @param args Input args not used
     * @throws Exception If JRobot server fails
     */
    public static void main(String[] args) throws Exception {
        LibraryServer server = new LibraryServer();
        server.setHost("0.0.0.0");
        server.setPort(8270);
        libraryList.add(new ConnectionTestLibrary(server));
        libraryList.add(new DeviceTestLibrary(server));
        libraryList.add(new ExportTestLibrary(server));
        server.start();
    }

    /**
     * @return All SxpNodes in library server
     */
    public static Collection<SxpNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /**
     * Clear all nodes from Library
     */
    public static void clearNodes() {
        nodes.clear();
    }

    /**
     * Puts Node of specific Id
     *
     * @param node Puts SxpNode into library server
     * @return Previous Node on the same ID
     */
    public static SxpNode putNode(SxpNode node) {
        return nodes.put(Preconditions.checkNotNull(node).getNodeId().getValue(), node);
    }

    /**
     * Removes Node of specific Id
     *
     * @param id NodeId of SxpNode to be removed
     * @return Removed SxpNode
     */
    public static SxpNode removeNode(NodeId id) {
        return nodes.remove(Preconditions.checkNotNull(id).getValue());
    }

    /**
     * Gets Node of specific Id
     *
     * @param id String representation of NodeId
     * @return SxpNode with specific NodeId
     */
    public static SxpNode getNode(String id) {
        return nodes.get(Preconditions.checkNotNull(id));
    }

}
