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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.robotframework.javalib.reflection.IArgumentConverter;
import org.robotframework.javalib.reflection.IArgumentGrouper;
import org.opendaylight.sxp.jrobot.remoteserver.AbstractLibraryTest;
import org.opendaylight.sxp.jrobot.remoteserver.RemoteServer;

import static org.mockito.Mockito.mock;

public class CheckedKeywordImplTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    private CheckedKeywordImpl checkedKeyword, checkedKeywordDefault;
    private AbstractLibraryTest abstractLibrary;

    @Before public void setUp() throws Exception {
        abstractLibrary = new AbstractLibraryTest(mock(RemoteServer.class));
        checkedKeywordDefault =
                new CheckedKeywordImpl(abstractLibrary, Arrays.stream(abstractLibrary.getClass().getMethods())
                        .filter(m -> "defaultKeyword".equals(m.getName()) && m.getParameterCount() == 2)
                        .findFirst()
                        .get());
        checkedKeyword =
                new CheckedKeywordImpl(abstractLibrary, Arrays.stream(abstractLibrary.getClass().getMethods())
                        .filter(m -> "plus".equals(m.getName()))
                        .findFirst()
                        .get());
    }

    @Test public void execute() throws Exception {
        Assert.assertEquals(5, checkedKeyword.execute(new Object[] {3, 2}));
        Assert.assertEquals(1, checkedKeyword.execute(new Object[] {3, -2}));

        exception.expect(RuntimeException.class);
        Assert.assertEquals(1, checkedKeyword.execute(new Object[] {3, "bad input"}));
    }

    @Test public void canExecute() throws Exception {
        Assert.assertTrue(checkedKeyword.canExecute(new Object[] {1, 2}));
        Assert.assertFalse(checkedKeyword.canExecute(new Object[] {1.2f, 2}));
        Assert.assertFalse(checkedKeyword.canExecute(new Object[] {1, 2.2f}));
        Assert.assertFalse(checkedKeyword.canExecute(new Object[] {"bad input", 2}));
    }

    @Test public void getArgumentConverter() throws Exception {
        final IArgumentConverter argumentConverter = checkedKeyword.getArgumentConverter();
        Assert.assertNotNull(argumentConverter);
        Assert.assertEquals(argumentConverter, checkedKeyword.getArgumentConverter());
    }

    @Test public void getArgumentGrouper() throws Exception {
        final IArgumentGrouper argumentGrouper = checkedKeyword.getArgumentGrouper();
        Assert.assertNotNull(argumentGrouper);
        Assert.assertEquals(argumentGrouper, checkedKeyword.getArgumentGrouper());
    }

    @Test public void getDocumentation() throws Exception {
        Assert.assertEquals("plusDoc", checkedKeyword.getDocumentation());
        Assert.assertEquals("", checkedKeywordDefault.getDocumentation());
    }

    @Test public void getTags() throws Exception {
        Assert.assertArrayEquals(new String[0], Arrays.stream(checkedKeyword.getTags()).sorted().toArray());
        Assert.assertArrayEquals(Stream.of("default").sorted().toArray(),
                Arrays.stream(checkedKeywordDefault.getTags()).sorted().toArray());
    }

    @Test public void getArgumentNames() throws Exception {
        Assert.assertArrayEquals(new String[] {"arg0", "arg1"},
                Arrays.stream(checkedKeywordDefault.getArgumentNames()).sorted().toArray());
        Assert.assertArrayEquals(new String[] {"a", "b"},
                Arrays.stream(checkedKeyword.getArgumentNames()).sorted().toArray());
    }

}
