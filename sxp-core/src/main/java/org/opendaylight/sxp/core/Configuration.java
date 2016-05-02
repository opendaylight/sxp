/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import com.google.common.base.Preconditions;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.capabilities.fields.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.capabilities.fields.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class Configuration {

    private static final Constants CONSTANTS = new Constants();

    public static final int DEFAULT_PREFIX_GROUP = 0;

    public static final int NETTY_CONNECT_TIMEOUT_MILLIS = 15000;

    public static final int NETTY_HANDLER_TIMEOUT_MILLIS = 0;

    public static final boolean NETTY_LOGGER_HANDLER = false;

    private static HashMap<String, SxpNode> nodes = new HashMap<>();

    public static final boolean SET_COMPOSITION_ATTRIBUTE_COMPACT_NO_RESERVED_FIELDS = true;

    public static final String TOPOLOGY_NAME = "sxp";
    
    static {
        _initializeLogger();
    }

    private static void _initializeLogger() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        // System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", new
        // SimpleDateFormat("yyMMdd'T'HH:mm:ss.SZ").toPattern());
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat",
                new SimpleDateFormat("yyMMdd HH:mm:ss").toPattern());
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "false");
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");// "logs/sxp.log");
        System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "false");
        // System.setProperty("org.slf4j.simpleLogger.warnLevelString", "WARN");
    }

    public static Capabilities getCapabilities(Version version) {
        CapabilitiesBuilder capabilitiesBuilder = new CapabilitiesBuilder();

        List<CapabilityType> capabilities = new ArrayList<>();
        if (version.getIntValue() > 0) {
            capabilities.add(CapabilityType.Ipv4Unicast);
            if (version.getIntValue() > 1) {
                capabilities.add(CapabilityType.Ipv6Unicast);
                if (version.getIntValue() > 2) {
                    capabilities.add(CapabilityType.SubnetBindings);
                    if (version.getIntValue() > 3) {
                        capabilities.add(CapabilityType.LoopDetection);
                        capabilities.add(CapabilityType.SxpCapabilityExchange);
                    }
                }
            }
        }
        if (capabilities.isEmpty()) {
            capabilities.add(CapabilityType.None);
        }
        capabilitiesBuilder.setCapability(capabilities);
        return capabilitiesBuilder.build();
    }

    public static Constants getConstants() {
        return CONSTANTS;
    }

    public static HashMap<String, SxpNode> getNodes() {
        return nodes;
    }

    public synchronized static SxpNode getRegisteredNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public synchronized static SxpNode unregister(String nodeId) {
        return nodes.remove(Preconditions.checkNotNull(nodeId));
    }

    public synchronized static SxpNode register(SxpNode node) {
        nodes.put(NodeIdConv.toString(Preconditions.checkNotNull(node).getNodeId()), node);
        return node;
    }
}
