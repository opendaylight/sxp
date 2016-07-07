/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SxpDomainTest {

    private static int PORT = 64999;
    @Rule public ExpectedException exception = ExpectedException.none();

    private SxpDomain domain;
    private SxpDatabaseInf sxpDatabase = mock(SxpDatabaseImpl.class);
    private MasterDatabaseInf masterDatabase = mock(MasterDatabaseImpl.class);
    private List<SxpConnection> connections = new ArrayList<>();

    @Before public void setUp() {
        domain = new SxpDomain(null, "test", sxpDatabase, masterDatabase);
        connections.clear();
    }

    private SxpConnection getSxpConnection(String address) {
        SxpConnection connection = mock(SxpConnection.class);
        when(connection.getDestination()).thenReturn(new InetSocketAddress(address, PORT));
        connections.add(connection);
        return connection;
    }

    @Test public void testGetMasterDatabase() throws Exception {
        assertNotNull(domain.getMasterDatabase());
        assertEquals(masterDatabase, domain.getMasterDatabase());
    }

    @Test public void testGetSxpDatabase() throws Exception {
        assertNotNull(domain.getSxpDatabase());
        assertEquals(sxpDatabase, domain.getSxpDatabase());
    }

    @Test public void testGetName() throws Exception {
        assertEquals("test", domain.getName());
    }

    @Test public void testGetConnections() throws Exception {
        domain.putConnection(getSxpConnection("127.0.0.1"));
        domain.putConnection(getSxpConnection("127.0.0.2"));
        domain.putConnection(getSxpConnection("127.0.0.3"));
        assertEquals(3, domain.getConnections().size());
        domain.putConnection(getSxpConnection("127.0.0.4"));
        domain.putConnection(getSxpConnection("127.0.0.5"));
        assertEquals(5, domain.getConnections().size());
        domain.removeConnection(new InetSocketAddress("127.0.0.1", PORT));
        assertEquals(4, domain.getConnections().size());
    }

    @Test public void testGetConnection() throws Exception {
        domain.putConnection(getSxpConnection("127.0.0.1"));
        assertNull(domain.getConnection(new InetSocketAddress("127.0.0.2", PORT)));
        assertNotNull(domain.getConnection(new InetSocketAddress("127.0.0.1", PORT)));
    }

    @Test public void testHasConnection() throws Exception {
        domain.putConnection(getSxpConnection("127.0.0.1"));
        domain.putConnection(getSxpConnection("127.0.0.3"));
        assertTrue(domain.hasConnection(new InetSocketAddress("127.0.0.1", PORT)));
        assertFalse(domain.hasConnection(new InetSocketAddress("127.0.0.2", PORT)));
        assertTrue(domain.hasConnection(new InetSocketAddress("127.0.0.3", PORT)));
        domain.removeConnection(new InetSocketAddress("127.0.0.1", PORT));
        assertFalse(domain.hasConnection(new InetSocketAddress("127.0.0.1", PORT)));
        assertFalse(domain.hasConnection(new InetSocketAddress("127.0.0.2", PORT)));
        assertTrue(domain.hasConnection(new InetSocketAddress("127.0.0.3", PORT)));
    }

    @Test public void testPutConnection() throws Exception {
        domain.putConnection(getSxpConnection("127.0.0.1"));
        domain.putConnection(getSxpConnection("127.0.0.2"));
        domain.putConnection(getSxpConnection("127.0.0.3"));
        assertEquals(3, domain.getConnections().size());

        exception.expect(IllegalArgumentException.class);
        domain.putConnection(getSxpConnection("127.0.0.1"));
    }

    @Test public void testRemoveConnection() throws Exception {
        domain.putConnection(getSxpConnection("127.0.0.1"));
        domain.putConnection(getSxpConnection("127.0.0.2"));
        domain.putConnection(getSxpConnection("127.0.0.3"));
        assertNotNull(domain.removeConnection(new InetSocketAddress("127.0.0.2", PORT)));
        assertNull(domain.removeConnection(new InetSocketAddress("127.0.0.2", PORT)));

        assertNotNull(domain.removeConnection(new InetSocketAddress("127.0.0.1", PORT)));
        assertNotNull(domain.removeConnection(new InetSocketAddress("127.0.0.3", PORT)));

        assertNull(domain.removeConnection(new InetSocketAddress("127.0.0.1", PORT)));
        assertNull(domain.removeConnection(new InetSocketAddress("127.0.0.3", PORT)));
    }

    @Test public void testClose() throws Exception {
        domain.putConnection(getSxpConnection("127.0.0.1"));
        domain.putConnection(getSxpConnection("127.0.0.2"));
        domain.putConnection(getSxpConnection("127.0.0.3"));

        domain.close();
        connections.forEach(c -> verify(c).shutdown());
    }
}
