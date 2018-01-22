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

public class CharArraySerializerTest {

    private CharArraySerializer serializer;
    private ContentHandler contentHandler;

    @Before public void setUp() throws Exception {
        serializer = new CharArraySerializer();
        contentHandler = Mockito.mock(ContentHandler.class);
    }

    @Test public void write() throws Exception {
        final String message = "test input";
        serializer.write(contentHandler, message.toCharArray());
        Mockito.verify(contentHandler)
                .startElement(Mockito.anyString(), Mockito.eq(TypeSerializerImpl.VALUE_TAG),
                        Mockito.eq(TypeSerializerImpl.VALUE_TAG), Mockito.any(Attributes.class));
        Mockito.verify(contentHandler)
                .characters(Mockito.eq(message.toCharArray()), Mockito.eq(0), Mockito.eq(message.length()));
        Mockito.verify(contentHandler)
                .endElement(Mockito.anyString(), Mockito.eq(TypeSerializerImpl.VALUE_TAG),
                        Mockito.eq(TypeSerializerImpl.VALUE_TAG));
    }

}
