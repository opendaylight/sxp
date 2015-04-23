/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerDefaultValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerDefaultValuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.capabilities.fields.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.capabilities.fields.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.network.topology.topology.node.Timers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.network.topology.topology.node.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.network.topology.topology.node.timers.ListenerProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.network.topology.topology.node.timers.SpeakerProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connection.fields.ConnectionTimers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Constants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConstantsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

public final class Configuration {

    private static final Constants CONSTANTS;

    public static final String CONTROLLER_NAME = "controller";

    public static final int DEFAULT_PREFIX_GROUP = 0;

    public static final int NETTY_CONNECT_TIMEOUT_MILLIS = 15000;

    public static final int NETTY_HANDLER_TIMEOUT_MILLIS = 0;

    public static final boolean NETTY_LOGGER_HANDLER = false;

    private static HashMap<String, SxpNode> nodes = new HashMap<String, SxpNode>();

    public static final boolean SET_COMPOSITION_ATTRIBUTE_COMPACT_NO_RESERVED_FIELDS = true;

    public static final boolean SET_COMPOSITION_UPDATE_MESSAGE_PEER_SEQUENCE_WITH_EACH_SGT = true;

    private static final TimerDefaultValues TIMER_DEFAULT_VALUES;

    public static final String TOPOLOGY_NAME = "sxp";
    
    static {
        _initializeLogger();

        CONSTANTS = _initializeConstants();

        TIMER_DEFAULT_VALUES = _initializeTimerDefaultValues();
    }

    private static Constants _initializeConstants() {
        ConstantsBuilder constantsBuilder = new ConstantsBuilder();
        if (constantsBuilder.getMessageHeaderLengthLength() == null) {
            constantsBuilder.setMessageHeaderLengthLength(4);
        }
        if (constantsBuilder.getMessageHeaderTypeLength() == null) {
            constantsBuilder.setMessageHeaderTypeLength(4);
        }
        if (constantsBuilder.getMessageLengthMax() == null) {
            constantsBuilder.setMessageLengthMax(4096);
        }
        if (constantsBuilder.getMessagesExportQuantity() == null) {
            constantsBuilder.setMessagesExportQuantity(5);
        }
        if (constantsBuilder.getNodeConnectionsInitialSize() == null) {
            constantsBuilder.setNodeConnectionsInitialSize(20);
        }
        if (constantsBuilder.getPort() == null) {
            constantsBuilder.setPort(64999);
        }
        return constantsBuilder.build();
    }

    private static final void _initializeLogger() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        // System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", new
        // SimpleDateFormat("yyMMdd'T'HH:mm:ss.SZ").toPattern());
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", new SimpleDateFormat("yyMMdd HH:mm:ss").toPattern());
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "false");
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");// "logs/sxp.log");
        System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "false");
        // System.setProperty("org.slf4j.simpleLogger.warnLevelString", "WARN");
    }

    private static TimerDefaultValues _initializeTimerDefaultValues() {
        TimerDefaultValuesBuilder timerDefaultValuesBuilder = new TimerDefaultValuesBuilder();
        if (timerDefaultValuesBuilder.getDeleteHoldDownTimer() == null) {
            timerDefaultValuesBuilder.setDeleteHoldDownTimer(120);
        }
        if (timerDefaultValuesBuilder.getHoldTimer() == null) {
            timerDefaultValuesBuilder.setHoldTimer(90);
        }
        if (timerDefaultValuesBuilder.getHoldTimerMax() == null) {
            timerDefaultValuesBuilder.setHoldTimerMax(180);
        }
        if (timerDefaultValuesBuilder.getHoldTimerMin() == null) {
            timerDefaultValuesBuilder.setHoldTimerMin(90);
        }
        if (timerDefaultValuesBuilder.getHoldTimerMinAcceptable() == null) {
            timerDefaultValuesBuilder.setHoldTimerMinAcceptable(120);
        }
        if (timerDefaultValuesBuilder.getKeepAliveTimer() == null) {
            timerDefaultValuesBuilder.setKeepAliveTimer(30);
        }
        if (timerDefaultValuesBuilder.getReconciliationTimer() == null) {
            timerDefaultValuesBuilder.setReconciliationTimer(120);
        }
        if (timerDefaultValuesBuilder.getRetryOpenTimer() == null) {
            timerDefaultValuesBuilder.setRetryOpenTimer(120);
        }
        return timerDefaultValuesBuilder.build();
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

    public static ConnectionTimers getConnectionTimersBoth() {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder();
        connectionTimersBuilder.setHoldTimeMinAcceptable(45);
        connectionTimersBuilder.setKeepAliveTime(getTimerDefault().getKeepAliveTimer());

        connectionTimersBuilder.setReconciliationTime(120);
        connectionTimersBuilder.setHoldTime(90);
        connectionTimersBuilder.setHoldTimeMin(90);
        connectionTimersBuilder.setHoldTimeMax(180);
        return connectionTimersBuilder.build();
    }

    public static ConnectionTimers getConnectionTimersListener() {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder();
        connectionTimersBuilder.setReconciliationTime(120);
        connectionTimersBuilder.setHoldTime(90);
        connectionTimersBuilder.setHoldTimeMin(90);
        connectionTimersBuilder.setHoldTimeMax(180);
        return connectionTimersBuilder.build();
    }

    public static ConnectionTimers getConnectionTimersSpeaker() {
        ConnectionTimersBuilder connectionTimersBuilder = new ConnectionTimersBuilder();
        connectionTimersBuilder.setHoldTimeMinAcceptable(45);
        connectionTimersBuilder.setKeepAliveTime(getTimerDefault().getKeepAliveTimer());
        return connectionTimersBuilder.build();
    }

    public static Constants getConstants() {
        return CONSTANTS;
    }

    public static String getNextNodeName() {
        return nodes.keySet().iterator().next();
    }

    public static HashMap<String, SxpNode> getNodes() {
        return nodes;
    }

    public static Timers getNodeTimers() {
        TimersBuilder timersBuilder = new TimersBuilder();
        timersBuilder.setRetryOpenTime(5);

        SpeakerProfileBuilder sprofileBuilder = new SpeakerProfileBuilder();
        sprofileBuilder.setHoldTimeMinAcceptable(45);
        sprofileBuilder.setKeepAliveTime(getTimerDefault().getKeepAliveTimer());
        timersBuilder.setSpeakerProfile(sprofileBuilder.build());

        ListenerProfileBuilder lprofileBuilder = new ListenerProfileBuilder();
        lprofileBuilder.setHoldTime(90);
        lprofileBuilder.setHoldTimeMin(90);
        lprofileBuilder.setHoldTimeMax(180);
        timersBuilder.setListenerProfile(lprofileBuilder.build());

        return timersBuilder.build();
    }

    public static SxpNode getRegisteredNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public static Set<String> getRegisteredNodesIds() {
        return nodes.keySet();
    }

    public static TimerDefaultValues getTimerDefault() {
        return TIMER_DEFAULT_VALUES;
    }

    public static final void initializeLogger() {
        ;
    }

    public static boolean isNodesRegistered() {
        return !nodes.keySet().isEmpty();
    }

    public static SxpNode register(SxpNode node) {
        nodes.put(NodeIdConv.toString(node.getNodeId()), node);
        return node;
    }
}
