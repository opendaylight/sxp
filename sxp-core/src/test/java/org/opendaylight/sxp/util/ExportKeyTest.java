/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

public class ExportKeyTest {

    private static ExportKey getKey(Version version, String groupName, List<CapabilityType> capabilityTypes) {
        SxpConnection connection = Mockito.mock(SxpConnection.class);
        Mockito.when(connection.getVersion()).thenReturn(version);
        Mockito.when(connection.getGroupName(FilterType.Outbound)).thenReturn(groupName);
        Mockito.when(connection.getCapabilitiesRemote()).thenReturn(capabilityTypes);
        return new ExportKey(connection);
    }

    @Test
    public void testEquals() throws Exception {
        List<CapabilityType> capabilityTypes = new ArrayList<>();
        ExportKey exportKey = getKey(Version.Version4, null, capabilityTypes);

        assertTrue(exportKey.equals(getKey(Version.Version4, null, new ArrayList<CapabilityType>())));
        assertTrue(exportKey.equals(exportKey));
        assertFalse(exportKey.equals(null));

        assertFalse(exportKey.equals(getKey(Version.Version3, null, new ArrayList<CapabilityType>())));
        assertFalse(exportKey.equals(getKey(Version.Version4, "wad", new ArrayList<CapabilityType>())));

        capabilityTypes.add(CapabilityType.Ipv4Unicast);
        capabilityTypes.add(CapabilityType.Ipv6Unicast);
        assertFalse(exportKey.equals(getKey(Version.Version4, null, new ArrayList<CapabilityType>())));

        List<CapabilityType> capabilityTypes_ = new ArrayList<>();
        capabilityTypes_.add(CapabilityType.Ipv6Unicast);
        capabilityTypes_.add(CapabilityType.Ipv4Unicast);
        assertTrue(exportKey.equals(getKey(Version.Version4, null, capabilityTypes_)));

        assertEquals(exportKey.hashCode(), getKey(Version.Version4, null, capabilityTypes_).hashCode());
        capabilityTypes_.add(CapabilityType.LoopDetection);
        assertNotEquals(exportKey.hashCode(), getKey(Version.Version4, null, capabilityTypes_).hashCode());
    }
}
