/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver;

import java.net.Socket;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.sxp.jrobot.remoteserver.library.RemoteLibrary;

public class RemoteServerImplTest {

    private RemoteServerImpl remoteServer;

    @Before public void setUp() throws Exception {
        remoteServer = new RemoteServerImpl();
    }

    @Test public void setPort() throws Exception {
        Assert.assertEquals(0, remoteServer.getPort());
        remoteServer.setPort(8022);
        Assert.assertEquals(8022, remoteServer.getPort());
    }

    @Test public void setHost() throws Exception {
        Assert.assertNull(remoteServer.getHost());
        remoteServer.setHost("localhost");
        Assert.assertNotNull(remoteServer.getHost());
        Assert.assertEquals("localhost", remoteServer.getHost());
    }

    @Test public void putLibrary() throws Exception {
        final String path = "/library";

        Assert.assertTrue(remoteServer.getLibraryMap().isEmpty());
        remoteServer.putLibrary(path, Mockito.mock(RemoteLibrary.class));
        Assert.assertFalse(remoteServer.getLibraryMap().isEmpty());
        Assert.assertNotNull(remoteServer.getLibraryMap().get(path));
    }

    @Test public void removeLibrary() throws Exception {
        final String path = "/library";

        Assert.assertTrue(remoteServer.getLibraryMap().isEmpty());
        remoteServer.putLibrary(path, Mockito.mock(RemoteLibrary.class));
        Assert.assertFalse(remoteServer.getLibraryMap().isEmpty());
        Assert.assertNotNull(remoteServer.removeLibrary(path));
        Assert.assertNull(remoteServer.removeLibrary(path));
        Assert.assertTrue(remoteServer.getLibraryMap().isEmpty());
    }

    @Test public void getLibraryMap() throws Exception {
        Assert.assertNotNull(remoteServer.getLibraryMap());
    }

    private boolean serverListening(String host, int port) {
        Socket s = null;
        try {
            s = new Socket(host, port);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Test public void start() throws Exception {
        remoteServer.setPort(8022);
        remoteServer.setHost("localhost");

        remoteServer.start();
        Assert.assertTrue(serverListening(remoteServer.getHost(), remoteServer.getPort()));
        remoteServer.stop();
        Assert.assertFalse(serverListening(remoteServer.getHost(), remoteServer.getPort()));

        remoteServer.start();
        Assert.assertTrue(serverListening(remoteServer.getHost(), remoteServer.getPort()));
        remoteServer.stop(100);
        Assert.assertFalse(serverListening(remoteServer.getHost(), remoteServer.getPort()));
    }

}
