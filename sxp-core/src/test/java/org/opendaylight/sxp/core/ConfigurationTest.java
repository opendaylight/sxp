/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.capabilities.fields.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.Timers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigurationTest {

        @Test public void testGetCapabilities() throws Exception {
                Capabilities capabilities = Configuration.getCapabilities(Version.Version1);
                assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv4Unicast));

                capabilities = Configuration.getCapabilities(Version.Version2);
                assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv4Unicast));
                assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv6Unicast));

                capabilities = Configuration.getCapabilities(Version.Version3);
                assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv4Unicast));
                assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv6Unicast));
                assertTrue(capabilities.getCapability().contains(CapabilityType.SubnetBindings));

                capabilities = Configuration.getCapabilities(Version.Version4);
                assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv4Unicast));
                assertTrue(capabilities.getCapability().contains(CapabilityType.Ipv6Unicast));
                assertTrue(capabilities.getCapability().contains(CapabilityType.SubnetBindings));
                assertTrue(capabilities.getCapability().contains(CapabilityType.SxpCapabilityExchange));
                assertTrue(capabilities.getCapability().contains(CapabilityType.LoopDetection));
        }

        @Test public void testGetConnectionTimersBoth() throws Exception {
                ConnectionTimers timers = Configuration.getConnectionTimersBoth();
                assertEquals(120, (long) timers.getReconciliationTime());
                assertEquals(90, (long) timers.getHoldTime());
                assertEquals(45, (long) timers.getHoldTimeMinAcceptable());
                assertEquals(90, (long) timers.getHoldTimeMin());
                assertEquals(180, (long) timers.getHoldTimeMax());
        }

        @Test public void testGetConnectionTimersListener() throws Exception {
                ConnectionTimers timers = Configuration.getConnectionTimersListener();
                assertEquals(120, (long) timers.getReconciliationTime());
                assertEquals(90, (long) timers.getHoldTime());
                assertEquals(90, (long) timers.getHoldTimeMin());
                assertEquals(180, (long) timers.getHoldTimeMax());
        }

        @Test public void testGetConnectionTimersSpeaker() throws Exception {
                ConnectionTimers timers = Configuration.getConnectionTimersSpeaker();
                assertEquals(45, (long) timers.getHoldTimeMinAcceptable());
        }

        @Test public void testGetConstants() throws Exception {
                Constants defaultValues = Configuration.getConstants();
                assertEquals(4, (long) defaultValues.getMessageHeaderLengthLength());
                assertEquals(4, (long) defaultValues.getMessageHeaderTypeLength());
                assertEquals(4096, (long) defaultValues.getMessageLengthMax());
                assertEquals(150, (long) defaultValues.getMessagesExportQuantity());
                assertEquals(20, (long) defaultValues.getNodeConnectionsInitialSize());
                assertEquals(64999, (long) defaultValues.getPort());
        }

        @Test public void testGetNodeTimers() throws Exception {
                Timers timers = Configuration.getNodeTimers();
                assertEquals(90, (long) timers.getListenerProfile().getHoldTime());
                assertEquals(90, (long) timers.getListenerProfile().getHoldTimeMin());
                assertEquals(180, (long) timers.getListenerProfile().getHoldTimeMax());
                assertEquals(45, (long) timers.getSpeakerProfile().getHoldTimeMinAcceptable());
        }

        @Test public void testGetTimerDefault() throws Exception {
                TimerDefaultValues defaultValues = Configuration.getTimerDefault();
                assertEquals(120, (long) defaultValues.getDeleteHoldDownTimer());
                assertEquals(90, (long) defaultValues.getHoldTimer());
                assertEquals(90, (long) defaultValues.getHoldTimerMin());
                assertEquals(180, (long) defaultValues.getHoldTimerMax());
                assertEquals(120, (long) defaultValues.getHoldTimerMinAcceptable());
                assertEquals(30, (long) defaultValues.getKeepAliveTimer());
                assertEquals(120, (long) defaultValues.getReconciliationTimer());
                assertEquals(120, (long) defaultValues.getRetryOpenTimer());
        }

}
