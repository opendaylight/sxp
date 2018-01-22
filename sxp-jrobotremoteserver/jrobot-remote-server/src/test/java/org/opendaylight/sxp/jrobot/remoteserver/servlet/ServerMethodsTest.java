/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.servlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.sxp.jrobot.remoteserver.AbstractLibraryTest;
import org.opendaylight.sxp.jrobot.remoteserver.RemoteServer;
import org.opendaylight.sxp.jrobot.remoteserver.library.RemoteLibrary;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

public class ServerMethodsTest {

    private final String PASS = "PASS", FAIL = "FAIL";
    private RemoteServerServlet servlet;
    private ServerMethods serverMethods;
    private RemoteLibrary library;

    @Before public void setUp() throws Exception {
        servlet = mock(RemoteServerServlet.class);
        library = mock(RemoteLibrary.class);
        serverMethods = new ServerMethods(servlet);
    }

    @Test public void get_keyword_names() throws Exception {
        Mockito.when(servlet.getLibrary()).thenReturn(library);
        try {
            serverMethods.get_keyword_names();
            Assert.fail();
        } catch (RuntimeException ignored) {
        }
        Mockito.when(library.getKeywordNames()).thenReturn(new String[] {"keyword"});
        final String[] names = serverMethods.get_keyword_names();
        Assert.assertNotNull(names);
        Assert.assertEquals(1, names.length);
        Assert.assertEquals("keyword", names[0]);
    }

    @Test public void get_keyword_tags() throws Exception {
        library = new AbstractLibraryTest(mock(RemoteServer.class));
        Mockito.when(servlet.getLibrary()).thenReturn(library);

        String[] tags = serverMethods.get_keyword_tags("plus");
        Assert.assertNotNull(tags);
        Assert.assertArrayEquals(new String[0], Stream.of(tags).sorted().toArray());

        tags = serverMethods.get_keyword_tags("minus");
        Assert.assertNotNull(tags);
        Assert.assertArrayEquals(Stream.of("-", "minus").sorted().toArray(), Stream.of(tags).sorted().toArray());

        tags = serverMethods.get_keyword_tags("defaultKeyword");
        Assert.assertNotNull(tags);
        Assert.assertArrayEquals(Stream.of("default", "defaultOverload").sorted().toArray(),
                Stream.of(tags).sorted().toArray());
    }

    @Test public void run_keyword() throws Exception {
        library = new AbstractLibraryTest(mock(RemoteServer.class));
        Mockito.when(servlet.getLibrary()).thenReturn(library);

        Map<String, Object> result = serverMethods.run_keyword("plus", new Object[] {1, 2});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("status"));
        Assert.assertTrue(result.containsKey("return"));
        Assert.assertEquals(PASS, result.get("status"));
        Assert.assertEquals(3, result.get("return"));

        result = serverMethods.run_keyword("plus", new Object[] {1, 2.0f});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("status"));
        Assert.assertTrue(result.containsKey("error"));
        Assert.assertTrue(result.containsKey("traceback"));
        Assert.assertEquals(FAIL, result.get("status"));

        result = serverMethods.run_keyword("plus", new Object[] {"a=1", "b=2"});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("status"));
        Assert.assertTrue(result.containsKey("return"));
        Assert.assertEquals(PASS, result.get("status"));
        Assert.assertEquals(3, result.get("return"));

        result = serverMethods.run_keyword("concat", new Object[] {"Base64==", "Another=Base===64=="});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("status"));
        Assert.assertTrue(result.containsKey("return"));
        Assert.assertEquals(PASS, result.get("status"));
        Assert.assertEquals("Base64==Another=Base===64==", result.get("return"));

        result = serverMethods.run_keyword("concat", new Object[] {"=Base64==", "==Another=Base===64=="});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("status"));
        Assert.assertTrue(result.containsKey("return"));
        Assert.assertEquals(PASS, result.get("status"));
        Assert.assertEquals("=Base64====Another=Base===64==", result.get("return"));

        result = serverMethods.run_keyword("concat", new Object[] {"a=Base64==", "b=Another=Base===64=="});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("status"));
        Assert.assertTrue(result.containsKey("return"));
        Assert.assertEquals(PASS, result.get("status"));
        Assert.assertEquals("Base64==Another=Base===64==", result.get("return"));

        result = serverMethods.run_keyword("concat", new Object[] {"a=a", "b=="});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("status"));
        Assert.assertTrue(result.containsKey("return"));
        Assert.assertEquals(PASS, result.get("status"));
        Assert.assertEquals("a=", result.get("return"));

        result = serverMethods.run_keyword("concat", new Object[] {"a=a", "c=="});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("status"));
        Assert.assertTrue(result.containsKey("return"));
        Assert.assertEquals(PASS, result.get("status"));
        Assert.assertEquals("ac==", result.get("return"));

        result = serverMethods.run_keyword("concat", new Object[] {"a=a", "c="});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("status"));
        Assert.assertTrue(result.containsKey("return"));
        Assert.assertEquals(PASS, result.get("status"));
        Assert.assertEquals("ac=", result.get("return"));

        result = serverMethods.run_keyword("plus", new Object[] {"a=1", "b=2.0"});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("status"));
        Assert.assertTrue(result.containsKey("error"));
        Assert.assertTrue(result.containsKey("traceback"));
        Assert.assertEquals(FAIL, result.get("status"));
    }

    @Test public void get_keyword_arguments() throws Exception {
        Mockito.when(servlet.getLibrary()).thenReturn(library);
        Mockito.when(library.getKeywordArguments(anyString())).thenReturn(null);
        Assert.assertArrayEquals(new String[0],
                Arrays.stream(serverMethods.get_keyword_arguments("keyword")).sorted().toArray());

        Mockito.when(library.getKeywordArguments(anyString())).thenReturn(new String[] {"arg0", "arg1"});
        Assert.assertArrayEquals(new String[] {"arg0", "arg1"},
                Arrays.stream(serverMethods.get_keyword_arguments("keyword")).sorted().toArray());
    }

    @Test public void get_keyword_documentation() throws Exception {
        Mockito.when(servlet.getLibrary()).thenReturn(library);
        Mockito.when(library.getKeywordDocumentation(anyString())).thenReturn(null);
        Assert.assertEquals("", serverMethods.get_keyword_documentation("keyword"));

        Mockito.when(library.getKeywordDocumentation(anyString())).thenReturn("documentation");
        Assert.assertEquals("documentation", serverMethods.get_keyword_documentation("keyword"));
    }

    @Test public void arraysToLists() throws Exception {
        Mockito.when(servlet.getLibrary()).thenReturn(library);
        Assert.assertNull(serverMethods.arraysToLists(null));
        Assert.assertNotNull(serverMethods.arraysToLists(new Object()));

        Assert.assertNotNull(serverMethods.arraysToLists(new String[0]));
        Assert.assertTrue(serverMethods.arraysToLists(new String[0]) instanceof List);

        Assert.assertNotNull(serverMethods.arraysToLists(new String[] {"a", "b"}));
        Assert.assertTrue(serverMethods.arraysToLists(new String[] {"a", "b"}) instanceof List);

        Assert.assertNotNull(serverMethods.arraysToLists(Collections.emptyMap()));
        Assert.assertTrue(serverMethods.arraysToLists(Collections.emptyMap()) instanceof Map);

        Assert.assertNotNull(serverMethods.arraysToLists(Collections.singletonMap("key", "value")));
        Assert.assertTrue(serverMethods.arraysToLists(Collections.singletonMap("key", "value")) instanceof Map);
    }

}
