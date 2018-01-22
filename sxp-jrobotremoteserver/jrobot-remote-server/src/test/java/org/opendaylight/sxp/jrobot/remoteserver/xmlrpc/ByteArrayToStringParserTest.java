/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ByteArrayToStringParserTest {

    private ByteArrayToStringParser parser;

    @Before public void setUp() throws Exception {
        parser = new ByteArrayToStringParser();
    }

    @Test public void setResult() throws Exception {
        parser.setResult(new byte[] {'a', 'b', 'c', 'd'});
        Assert.assertEquals("abcd", parser.getResult());

        parser.setResult(new int[] {'a', 'b', 'c', 'd'});
        Assert.assertArrayEquals(new int[] {'a', 'b', 'c', 'd'}, (int[]) parser.getResult());
    }

}
