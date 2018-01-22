/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers;

import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.TypeFactory;
import org.xml.sax.ContentHandler;

/**
 * Tests for {@link PrimitiveArraySerializer}
 */
public class PrimitiveArraySerializerTest {

    private PrimitiveArraySerializer serializer;
    private TypeFactory typeFactory;
    private TypeSerializer typeSerializer;
    private XmlRpcStreamConfig config;
    private ContentHandler contentHandler;

    @Before public void setUp() throws Exception {
        contentHandler = Mockito.mock(ContentHandler.class);
        typeFactory = Mockito.mock(TypeFactory.class);
        typeSerializer = Mockito.mock(TypeSerializer.class);
        Mockito.when(typeFactory.getSerializer(Mockito.any(XmlRpcStreamConfig.class), Mockito.anyObject()))
                .thenReturn(typeSerializer);
        config = Mockito.mock(XmlRpcStreamConfig.class);
        serializer = new PrimitiveArraySerializer(typeFactory, config);
    }

    @Test public void writeData() throws Exception {
        serializer.writeData(contentHandler, new byte[] {1});
        Mockito.verify(typeSerializer).write(Mockito.eq(contentHandler), Mockito.eq(new Byte((byte) 1)));

        serializer.writeData(contentHandler, new short[] {10});
        Mockito.verify(typeSerializer).write(Mockito.eq(contentHandler), Mockito.eq(new Short((short) 10)));

        serializer.writeData(contentHandler, new int[] {11});
        Mockito.verify(typeSerializer).write(Mockito.eq(contentHandler), Mockito.eq(new Integer(11)));

        serializer.writeData(contentHandler, new long[] {12});
        Mockito.verify(typeSerializer).write(Mockito.eq(contentHandler), Mockito.eq(new Long(12l)));

        serializer.writeData(contentHandler, new float[] {10.5f});
        Mockito.verify(typeSerializer).write(Mockito.eq(contentHandler), Mockito.eq(new Float(10.5f)));

        serializer.writeData(contentHandler, new double[] {10.25});
        Mockito.verify(typeSerializer).write(Mockito.eq(contentHandler), Mockito.eq(new Double(10.25)));

        serializer.writeData(contentHandler, new boolean[] {false});
        Mockito.verify(typeSerializer).write(Mockito.eq(contentHandler), Mockito.eq(new Boolean(false)));
    }

}
