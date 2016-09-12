/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.csit.libraries.AbstractLibrary;
import org.opendaylight.sxp.csit.libraries.ConnectionTestLibrary;
import org.opendaylight.sxp.csit.libraries.ExportTestLibrary;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.robotframework.remoteserver.RemoteServer;
import org.robotframework.remoteserver.library.RemoteLibrary;

public class LibraryServer extends RemoteServer implements BundleActivator {

    private static Map<String, SxpNode> nodes = new ConcurrentHashMap<>();

    public LibraryServer() {
        setPort(8270);
        addLibrary(new ConnectionTestLibrary());
        addLibrary(new ExportTestLibrary());
    }

    public static Collection<SxpNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public static SxpNode putNode(SxpNode node) {
        return nodes.put(Preconditions.checkNotNull(node).getNodeId().getValue(), node);
    }

    public static SxpNode removeNode(NodeId id) {
        return nodes.remove(Preconditions.checkNotNull(id).getValue());
    }

    public static SxpNode getNode(String id) {
        return nodes.get(Preconditions.checkNotNull(id));
    }

    public RemoteLibrary addLibrary(AbstractLibrary library) {
        return putLibrary(Preconditions.checkNotNull(library).getUrl(), library);
    }

    @Override public void start(BundleContext context) throws Exception {
        start();
    }

    @Override public void stop(BundleContext context) throws Exception {
        stop();
    }

}
