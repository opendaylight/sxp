/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.sxp.Topology.TopologyType;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.util.database.Database;
import org.opendaylight.sxp.util.io.IOManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Runtime {

    private static boolean IO_MANAGER = false;

    private static final Logger LOG;

    private static List<PrefixGroup> prefixGroups;

    private static TopologyType topologyType = TopologyType.TOPOLOGY_SXP_LAB;

    static {
        Configuration.initializeLogger();
        LOG = LoggerFactory.getLogger(Runtime.class.getName());

        prefixGroups = new ArrayList<>();
        try {
            prefixGroups.add(Database.createPrefixGroup(10000, "192.168.0.1/32"));
            prefixGroups.add(Database.createPrefixGroup(20000, "2001::1/64", "10.10.10.10/30"));
            prefixGroups.add(Database.createPrefixGroup(30000, "2002::1/128"));
            prefixGroups.add(Database.createPrefixGroup(40000, "11.11.11.0/29"));
            prefixGroups.add(Database.createPrefixGroup(65000, "172.168.1.0/28"));
        } catch (Exception e) {
            LOG.warn("{} | {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
    	
//    	 SxpNode node  = Topology.createLocalNode("R1", NodeIdConv.create(128), 51000, null, null);
//    	 node.shutdown();
//    	 LOG.info("exit");    	 
    	 
        // Topology.DEFAULT_PASSWORD = "C!sco123";
        Topology.DEVICES_BIDIRECTIONAL = false;
        Topology.EXPANSION_QUANTITY = 0;
        Topology.VERSION = Version.Version4;

        int nEntities = 25;
        int nodeBasePort = 51000;// Constants.Port.getIntValue();
        int nodesInitPort = nodeBasePort + 1;

        switch (topologyType) {
        case TOPOLOGY_DUMMY_NODE:
            Topology.createTopologyDummyNode();
            break;
        case TOPOLOGY_SXP_LAB:
            Topology.createTopologySxpLab(prefixGroups);
            break;
        case TOPOLOGY_NAG:
            Topology.createTopologyNag(prefixGroups);
            break;
        case TOPOLOGY_RING:
            Topology.createTopologyRing(prefixGroups, 3, "127.0.0.1", Configuration.getConstants().getPort() + 1);
            break;
        case TOPOLOGY_MESH:
            Topology.createTopologyMesh(prefixGroups, 10, "127.0.0.1", Configuration.getConstants().getPort() + 1);
            break;
        case TOPOLOGY_STAR_LOCAL:
            Topology.createTopologyStarLocalBase(ConnectionMode.Listener, nEntities, "192.168.1.102", nodesInitPort,
                    nodeBasePort);
            break;
        case TOPOLOGY_STAR_REMOTE:
            Topology.createTopologyStarRemoteNodes(ConnectionMode.Speaker, prefixGroups, nEntities, "192.168.1.104",
                    nodesInitPort, nodeBasePort);
            break;
        }

        if (IO_MANAGER) {
            IOManager.create(Configuration.getNodes());
        }
    }
}
