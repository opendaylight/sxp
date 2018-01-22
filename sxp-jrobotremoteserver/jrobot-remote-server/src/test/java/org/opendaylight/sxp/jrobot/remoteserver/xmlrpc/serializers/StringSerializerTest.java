/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers;

import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/**
 * Tests for {@link StringSerializer}
 */
public class StringSerializerTest {

    private StringSerializer serializer;
    private ContentHandler handler;

    @Before public void setUp() throws Exception {
        serializer = new StringSerializer();
        handler = Mockito.mock(ContentHandler.class);
    }

    @Test public void write1() throws Exception {
        final String message = "test sample";
        serializer.write(handler, message);
        Mockito.verify(handler)
                .startElement(Mockito.anyString(), Mockito.eq(TypeSerializerImpl.VALUE_TAG),
                        Mockito.eq(TypeSerializerImpl.VALUE_TAG), Mockito.any(Attributes.class));
        Mockito.verify(handler)
                .characters(Mockito.eq(message.toCharArray()), Mockito.eq(0), Mockito.eq(message.length()));
        Mockito.verify(handler)
                .endElement(Mockito.anyString(), Mockito.eq(TypeSerializerImpl.VALUE_TAG),
                        Mockito.eq(TypeSerializerImpl.VALUE_TAG));
    }

    @Test public void write2() throws Exception {
        final String message = new String(new char[] {0x0, 0x1});
        serializer.write(handler, message);
        Mockito.verify(handler)
                .startElement(Mockito.anyString(), Mockito.eq(TypeSerializerImpl.VALUE_TAG),
                        Mockito.eq(TypeSerializerImpl.VALUE_TAG), Mockito.any(Attributes.class));
        Mockito.verify(handler)
                .startElement(Mockito.anyString(), Mockito.eq("base64"), Mockito.eq("base64"),
                        Mockito.any(Attributes.class));

        Mockito.verify(handler).characters(Mockito.eq(new char[] {'A', 'A', 'E', '='}), Mockito.eq(0), Mockito.eq(4));

        Mockito.verify(handler).endElement(Mockito.anyString(), Mockito.eq("base64"), Mockito.eq("base64"));
        Mockito.verify(handler)
                .endElement(Mockito.anyString(), Mockito.eq(TypeSerializerImpl.VALUE_TAG),
                        Mockito.eq(TypeSerializerImpl.VALUE_TAG));
    }

}
