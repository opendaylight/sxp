/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.DataObject;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.sxp.controller.listeners.spi.Listener.Differences.checkDifference;

public class DifferencesTest {

    private <T extends DataObject> DataTreeModification<T> getModification(T before, T after) {
        DataTreeModification<T> tree = mock(DataTreeModification.class);
        DataObjectModification<T> modification = mock(DataObjectModification.class);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataAfter()).thenReturn(after);
        when(tree.getRootNode()).thenReturn(modification);
        return tree;
    }

    private Connection getChange(String address, int port, Version version, ConnectionMode mode) {
        return new ConnectionBuilder().setVersion(version)
                .setMode(mode)
                .setPeerAddress(new IpAddress(address.toCharArray()))
                .setTcpPort(new PortNumber(port))
                .build();
    }

    private List<Connection> getChanges(Connection... connection) {
        return Arrays.asList(connection);
    }

    @Test public void testCheckDifference() throws Exception {
        assertTrue(checkDifference(
                getModification(getChange("127.0.0.1", 64999, Version.Version4, ConnectionMode.Speaker),
                        getChange("127.0.0.2", 64999, Version.Version4, ConnectionMode.Speaker)),
                SxpConnectionFields::getPeerAddress));

        assertTrue(checkDifference(
                getModification(getChange("127.0.0.1", 64990, Version.Version4, ConnectionMode.Speaker),
                        getChange("127.0.0.1", 64999, Version.Version4, ConnectionMode.Speaker)),
                SxpConnectionFields::getTcpPort));

        assertFalse(checkDifference(
                getModification(getChange("127.0.0.5", 64999, Version.Version4, ConnectionMode.Speaker),
                        getChange("127.0.0.1", 64999, Version.Version4, ConnectionMode.Speaker)),
                SxpConnectionFields::getVersion));

        assertFalse(checkDifference(
                getModification(getChange("127.0.0.1", 64999, Version.Version2, ConnectionMode.Speaker),
                        getChange("127.0.0.1", 64999, Version.Version4, ConnectionMode.Speaker)),
                SxpConnectionFields::getPeerAddress));

        Connection change1 = getChange("127.0.0.1", 64999, Version.Version2, ConnectionMode.Speaker),
                change2 = getChange("127.0.0.2", 64999, Version.Version2, ConnectionMode.Speaker),
                change3 = getChange("127.0.0.1", 64998, Version.Version2, ConnectionMode.Speaker),
                change4 = getChange("127.0.0.1", 64999, Version.Version4, ConnectionMode.Speaker);

        assertFalse(checkDifference(new ArrayList<>(), new ArrayList<>()));
        assertFalse(checkDifference(getChanges(change1, change2), getChanges(change1, change2)));
        assertFalse(checkDifference(getChanges(change2, change1), getChanges(change1, change2)));

        assertTrue(checkDifference(getChanges(change1, change2, change3), getChanges(change1, change2)));
        assertTrue(checkDifference(getChanges(change1, change2), getChanges(change1, change2, change3)));
        assertTrue(checkDifference(getChanges(change1, change2), new ArrayList<>()));
        assertTrue(checkDifference(new ArrayList<>(), getChanges(change1, change2)));
        assertTrue(checkDifference(getChanges(change1, change2), getChanges(change1)));
        assertTrue(checkDifference(getChanges(change1, change2), getChanges(change1, change3)));
        assertTrue(checkDifference(getChanges(change3, change2), getChanges(change1, change3)));
    }
}
