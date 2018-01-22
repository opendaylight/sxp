/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.library;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.collections.map.HashedMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.robotframework.javalib.factory.KeywordFactory;
import org.opendaylight.sxp.jrobot.remoteserver.AbstractLibraryTest;
import org.opendaylight.sxp.jrobot.remoteserver.RemoteServer;

import static org.mockito.Mockito.mock;

public class AbstractClassLibraryTest {

    private final String keywordPlus = "plus", keywordMinus = "minus", keywordDocPlus = "plusDoc",
            keywordDocMinus =
                    "minusDoc";
    private AbstractClassLibrary library;

    @Before public void setUp() throws Exception {
        library = new AbstractLibraryTest(mock(RemoteServer.class));
    }

    @Test public void createKeywordFactory() throws Exception {
        KeywordFactory keywordFactory = library.createKeywordFactory();
        Assert.assertNotNull(keywordFactory);
        Assert.assertNotNull(library.createKeywordFactory());
        Assert.assertEquals(keywordFactory, library.createKeywordFactory());
    }

    private Map<String, Object> setArgs(String a0, String a1, Object v0, Object v1) {
        Map<String, Object> map = new HashedMap();
        map.put(a0, v0);
        map.put(a1, v1);
        return map;
    }

    @Test public void runKeyword() throws Exception {
        Assert.assertEquals(7, library.runKeyword(keywordPlus, new Object[] {5, 2}, Collections.emptyMap()));
        Assert.assertEquals(3, library.runKeyword(keywordMinus, new Object[] {5, 2}, Collections.emptyMap()));

        Assert.assertEquals(7, library.runKeyword(keywordPlus, new Object[0], setArgs("a", "b", 5, 2)));
        Assert.assertEquals(3, library.runKeyword(keywordPlus, new Object[0], setArgs("a", "b", 5, -2)));

        Assert.assertEquals(3, library.runKeyword(keywordMinus, new Object[0], setArgs("arg0", "arg1", 5, 2)));
        Assert.assertEquals(-3, library.runKeyword(keywordMinus, new Object[0], setArgs("arg0", "arg1", 2, 5)));
    }

    @Test public void getKeywordArguments() throws Exception {
        Assert.assertArrayEquals(Stream.of("arg0", "arg1").sorted().toArray(),
                Arrays.stream(library.getKeywordArguments(keywordMinus)).sorted().toArray());

        Assert.assertArrayEquals(Stream.of("a", "b").sorted().toArray(),
                Arrays.stream(library.getKeywordArguments(keywordPlus)).sorted().toArray());
    }

    @Test public void getKeywordTags() throws Exception {
        Assert.assertArrayEquals(new String[0], Arrays.stream(library.getKeywordTags(keywordPlus)).sorted().toArray());

        Assert.assertArrayEquals(Stream.of("-", "minus").sorted().toArray(),
                Arrays.stream(library.getKeywordTags(keywordMinus)).sorted().toArray());
    }

    @Test public void getKeywordDocumentation() throws Exception {
        Assert.assertEquals(keywordDocPlus, library.getKeywordDocumentation(keywordPlus));
        Assert.assertEquals(keywordDocMinus, library.getKeywordDocumentation(keywordMinus));
    }

    @Test public void getURI() throws Exception {
        Assert.assertEquals(AbstractLibraryTest.URI, library.getURI());
    }

    @Test public void libraryCleanup() throws Exception {
        Assert.assertFalse(AbstractLibraryTest.closed.get());
        library.libraryCleanup();
        Assert.assertTrue(AbstractLibraryTest.closed.get());
    }

}
