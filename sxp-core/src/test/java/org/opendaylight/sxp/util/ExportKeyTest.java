/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util;

import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ExportKeyTest {

    private static ExportKey getKey(Version version, boolean exportAll, String groupName,
            List<CapabilityType> capabilityTypes) {
        SxpConnection connection = Mockito.mock(SxpConnection.class);
        Mockito.when(connection.getVersion()).thenReturn(version);
        Mockito.when(connection.getGroupName(FilterType.Outbound)).thenReturn(groupName);
        Mockito.when(connection.getCapabilitiesRemote()).thenReturn(capabilityTypes);
        return new ExportKey(connection);
    }

    @Test public void testEquals() throws Exception {
        List<CapabilityType> capabilityTypes = new ArrayList<>();
        ExportKey exportKey = getKey(Version.Version4, false, null, capabilityTypes);

        assertTrue(exportKey.equals(getKey(Version.Version4, false, null, new ArrayList<CapabilityType>())));
        assertTrue(exportKey.equals(exportKey));
        assertFalse(exportKey.equals(null));

        assertFalse(exportKey.equals(getKey(Version.Version3, false, null, new ArrayList<CapabilityType>())));
        assertFalse(exportKey.equals(getKey(Version.Version4, true, null, new ArrayList<CapabilityType>())));
        assertFalse(exportKey.equals(getKey(Version.Version4, false, "wad", new ArrayList<CapabilityType>())));

        capabilityTypes.add(CapabilityType.Ipv4Unicast);
        capabilityTypes.add(CapabilityType.Ipv6Unicast);
        assertFalse(exportKey.equals(getKey(Version.Version4, false, null, new ArrayList<CapabilityType>())));

        List<CapabilityType> capabilityTypes_ = new ArrayList<>();
        capabilityTypes_.add(CapabilityType.Ipv6Unicast);
        capabilityTypes_.add(CapabilityType.Ipv4Unicast);
        assertTrue(exportKey.equals(getKey(Version.Version4, false, null, capabilityTypes_)));

        assertEquals(exportKey.hashCode(), getKey(Version.Version4, false, null, capabilityTypes_).hashCode());
        capabilityTypes_.add(CapabilityType.LoopDetection);
        assertNotEquals(exportKey.hashCode(), getKey(Version.Version4, false, null, capabilityTypes_).hashCode());
    }
}
