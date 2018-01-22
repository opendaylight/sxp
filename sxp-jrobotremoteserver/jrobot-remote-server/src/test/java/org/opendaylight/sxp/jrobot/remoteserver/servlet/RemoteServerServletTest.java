/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.servlet;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.ServletConfig;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sxp.jrobot.remoteserver.AbstractLibraryTest;
import org.opendaylight.sxp.jrobot.remoteserver.RemoteServer;
import org.opendaylight.sxp.jrobot.remoteserver.exceptions.IllegalPathException;
import org.opendaylight.sxp.jrobot.remoteserver.library.RemoteLibrary;

import static org.mockito.Mockito.mock;

public class RemoteServerServletTest {

    final String path = "/library";
    private RemoteServerServlet servlet;

    @Before public void setUp() throws Exception {
        servlet = new RemoteServerServlet();
    }

    @Test public void putLibrary() throws Exception {
        Assert.assertTrue(servlet.getLibraryMap().isEmpty());
        servlet.putLibrary(path, mock(RemoteLibrary.class));
        Assert.assertFalse(servlet.getLibraryMap().isEmpty());
        Assert.assertNotNull(servlet.getLibraryMap().get(path));
    }

    @Test public void removeLibrary() throws Exception {
        Assert.assertTrue(servlet.getLibraryMap().isEmpty());
        servlet.putLibrary(path, mock(RemoteLibrary.class));
        Assert.assertFalse(servlet.getLibraryMap().isEmpty());
        Assert.assertNotNull(servlet.removeLibrary(path));
        Assert.assertNull(servlet.removeLibrary(path));
        Assert.assertTrue(servlet.getLibraryMap().isEmpty());
    }

    @Test public void getLibraryMap() throws Exception {
        Assert.assertNotNull(servlet.getLibraryMap());
    }

    @Test public void cleanPath() throws Exception {
        Assert.assertEquals("/", RemoteServerServlet.cleanPath(null));
        Assert.assertEquals("/path/to/library", RemoteServerServlet.cleanPath("/path/to/library"));
        Assert.assertEquals("/path/to/library", RemoteServerServlet.cleanPath("/path/to/library/"));
        Assert.assertEquals("/path/to/library", RemoteServerServlet.cleanPath("//path///to////library"));
    }

    @Test public void checkPath() throws Exception {
        for (String path : Stream.of(null, "", "//", "/path/", "@").collect(Collectors.toSet())) {
            try {
                RemoteServerServlet.checkPath(path);
                Assert.fail();
            } catch (IllegalPathException ignored) {
            }
        }
        Assert.assertEquals("/valid/path", RemoteServerServlet.checkPath("/valid/path"));
    }

    @Test public void getPage() throws Exception {
        Assert.assertEquals(
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"><HTML><HEAD><TITLE>jrobotremoteserver</TITLE></HEAD><BODY><P>jrobotremoteserver serving:</P><TABLE border='1' cellspacing='0' cellpadding='5'><TR><TH>Path</TH><TH>Library</TH></TR><TR><TD COLSPAN=\"2\">No libraries mapped</TD></TR></TABLE></BODY></HTML>",
                servlet.getPage());
        servlet.putLibrary("/library", new AbstractLibraryTest(mock(RemoteServer.class)));
        Assert.assertEquals(
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"><HTML><HEAD><TITLE>jrobotremoteserver</TITLE></HEAD><BODY><P>jrobotremoteserver serving:</P><TABLE border='1' cellspacing='0' cellpadding='5'><TR><TH>Path</TH><TH>Library</TH></TR><TR><TD>/library</TD><TD>TestURI</TD></TR></TABLE></BODY></HTML>",
                servlet.getPage());
    }

    @Test public void newXmlRpcServer() throws Exception {
        final XmlRpcServletServer server = servlet.newXmlRpcServer(mock(ServletConfig.class));
        Assert.assertNotNull(server);
        Assert.assertNotNull(server.getConfig());
        Assert.assertNotNull(server.getErrorLogger());
        Assert.assertNotNull(server.getTypeFactory());
        Assert.assertNotNull(server.getWorkerFactory());
    }

    @Test public void newXmlRpcHandlerMapping() throws Exception {
        XmlRpcHandlerMapping mapping = servlet.newXmlRpcHandlerMapping();
        Assert.assertNotNull(mapping);
        Assert.assertNotNull(mapping.getHandler("get_keyword_arguments"));
        Assert.assertNotNull(mapping.getHandler("run_keyword"));
        Assert.assertNotNull(mapping.getHandler("get_keyword_names"));
        Assert.assertNotNull(mapping.getHandler("get_keyword_documentation"));
    }

}
