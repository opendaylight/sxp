/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.ContentHandler;

/**
 * Tests for {@link PojoSerializer}
 */
public class PojoSerializerTest {

    private ContentHandler handler;
    private PojoSerializer serializer;

    @Before public void setUp() throws Exception {
        handler = Mockito.mock(ContentHandler.class);
        serializer = new PojoSerializer();
    }

    @Test public void write() throws Exception {
        serializer.write(handler, new PojoObject("Object", 1));
        Mockito.verify(handler)
                .characters(Mockito.eq("{\"name\":\"Object\",\"id\":1}".toCharArray()), Mockito.eq(0), Mockito.eq(24));
        serializer.write(handler, new PojoObject("Object2", 10));
        Mockito.verify(handler)
                .characters(Mockito.eq("{\"name\":\"Object2\",\"id\":10}".toCharArray()), Mockito.eq(0),
                        Mockito.eq(26));
    }

    private class PojoObject {

        private String name;
        private int id;

        protected PojoObject(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }
    }
}
