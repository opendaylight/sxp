/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.keywords;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sxp.jrobot.remoteserver.AbstractLibraryTest;
import org.opendaylight.sxp.jrobot.remoteserver.RemoteServer;

import static org.mockito.Mockito.mock;

public class OverloadedKeywordImplTest {

    private OverloadedKeywordImpl checkedKeyword, checkedKeywordDefault;
    private AbstractLibraryTest abstractLibrary;

    @Before public void setUp() throws Exception {
        abstractLibrary = new AbstractLibraryTest(mock(RemoteServer.class));
        checkedKeywordDefault =
                new OverloadedKeywordImpl(abstractLibrary, Arrays.stream(abstractLibrary.getClass().getMethods())
                        .filter(m -> "defaultKeyword".equals(m.getName()) && m.getParameterCount() == 2)
                        .findFirst()
                        .get());
        checkedKeyword =
                new OverloadedKeywordImpl(abstractLibrary, Arrays.stream(abstractLibrary.getClass().getMethods())
                        .filter(m -> "plus".equals(m.getName()))
                        .findFirst()
                        .get());
    }

    private void addDefaultOverload() {
        checkedKeywordDefault.addOverload(Arrays.stream(abstractLibrary.getClass().getMethods())
                .filter(m -> "defaultKeyword".equals(m.getName()) && m.getParameterCount() == 3)
                .findFirst()
                .get());
    }

    @Test public void execute() throws Exception {
        Assert.assertEquals(2, checkedKeywordDefault.execute(new Object[] {1, 1}));
        try {
            Assert.assertEquals(3, checkedKeywordDefault.execute(new Object[] {1, 1, 1}));
            Assert.fail("Should not execute due to no available keyword");
        } catch (IllegalArgumentException ignored) {
        }
        addDefaultOverload();
        Assert.assertEquals(2, checkedKeywordDefault.execute(new Object[] {1, 1}));
        Assert.assertEquals(3, checkedKeywordDefault.execute(new Object[] {1, 1, 1}));
    }

    @Test public void addOverload() throws Exception {
        Assert.assertEquals(2, checkedKeywordDefault.getArgumentNames().length);
        addDefaultOverload();
        Assert.assertEquals(3, checkedKeywordDefault.getArgumentNames().length);
    }

    @Test public void getArgumentNames() throws Exception {
        addDefaultOverload();
        Assert.assertArrayEquals(new String[] {"arg0", "arg1", "arg2="},
                Arrays.stream(checkedKeywordDefault.getArgumentNames()).sorted().toArray());
        Assert.assertArrayEquals(new String[] {"a", "b"},
                Arrays.stream(checkedKeyword.getArgumentNames()).sorted().toArray());
    }

    @Test public void getDocumentation() throws Exception {
        Assert.assertEquals("plusDoc", checkedKeyword.getDocumentation());
        Assert.assertEquals("", checkedKeywordDefault.getDocumentation());
    }

    @Test public void getTags() throws Exception {
        Assert.assertArrayEquals(new String[0], Arrays.stream(checkedKeyword.getTags()).sorted().toArray());
        Assert.assertArrayEquals(Stream.of("default").sorted().toArray(),
                Arrays.stream(checkedKeywordDefault.getTags()).sorted().toArray());
        addDefaultOverload();
        Assert.assertArrayEquals(Stream.of("default", "defaultOverload").sorted().toArray(),
                Arrays.stream(checkedKeywordDefault.getTags()).sorted().toArray());

    }

}
