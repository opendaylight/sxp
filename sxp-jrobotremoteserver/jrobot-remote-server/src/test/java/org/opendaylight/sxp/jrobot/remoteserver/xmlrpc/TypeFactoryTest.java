/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Collections;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TypeFactory}
 */
public class TypeFactoryTest {

    private final StdSerializer<TestObject> serializer = new StdSerializer<TestObject>(TestObject.class) {

        @Override public void serialize(TestObject value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {

        }
    };
    private XmlRpcStreamConfig pConfig;
    private NamespaceContextImpl pContext;
    private TypeFactory typeFactory;

    @Before public void setUp() throws Exception {
        pConfig = mock(XmlRpcStreamConfig.class);
        pContext = mock(NamespaceContextImpl.class);
        typeFactory = new TypeFactory(mock(XmlRpcController.class));
    }

    @Test public void toObject() throws Exception {
        Assert.assertArrayEquals(new Boolean[] {true, true, false, false},
                TypeFactory.toObject(new boolean[] {true, true, false, false}));
    }

    @Test public void toObject1() throws Exception {
        Assert.assertArrayEquals(new Double[] {1.5, 2.0, 8.99, 5.0},
                TypeFactory.toObject(new double[] {1.5, 2.0, 8.99, 5.0}));
    }

    @Test public void toObject2() throws Exception {
        Assert.assertArrayEquals(new Float[] {1.5f, 2.0f, 8.99f, 5.0f},
                TypeFactory.toObject(new float[] {1.5f, 2.0f, 8.99f, 5.0f}));
    }

    @Test public void toObject3() throws Exception {
        Assert.assertArrayEquals(new Long[] {4984849884L, 32189879L, -6542154L, 0L},
                TypeFactory.toObject(new long[] {4984849884L, 32189879L, -6542154L, 0L}));
    }

    @Test public void toObject4() throws Exception {
        Assert.assertArrayEquals(new Long[] {4984849884L, 32189879L, -6542154L, 0L},
                TypeFactory.toObject(new long[] {4984849884L, 32189879L, -6542154L, 0L}));
    }

    @Test public void toObject5() throws Exception {
        Assert.assertArrayEquals(new Integer[] {498484, 43218, -2154, 0},
                TypeFactory.toObject(new int[] {498484, 43218, -2154, 0}));
    }

    @Test public void toObject6() throws Exception {
        Assert.assertArrayEquals(new Short[] {4984, 43, -2154, 0},
                TypeFactory.toObject(new short[] {4984, 43, -2154, 0}));
    }

    @Test public void getSerializer() throws Exception {
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, null));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, ""));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, 0));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, (short) 0));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, (byte) 0));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, false));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, 0d));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, 0f));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, new Object[] {}));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, Collections.emptyList()));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, Collections.emptyMap()));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, Collections.emptyIterator()));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, new char[] {}));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, new byte[] {}));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, new short[] {}));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, new int[] {}));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, new long[] {}));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, new float[] {}));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, new double[] {}));
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, new boolean[] {}));

        typeFactory.addSerializer(serializer);
        Assert.assertNotNull(typeFactory.getSerializer(pConfig, new TestObject()));
        Assert.assertNotEquals(typeFactory.getSerializer(pConfig, new TestObject()),
                typeFactory.getSerializer(pConfig, new Object()));
    }

    @Test public void getParser() throws Exception {
        Assert.assertNotNull(typeFactory.getParser(pConfig, pContext, "", "base64"));
        Assert.assertNotNull(typeFactory.getParser(pConfig, pContext, "", "double"));
        Assert.assertNull(typeFactory.getParser(pConfig, pContext, "", null));
    }

    private class TestObject {

        @Override public String toString() {
            return "TestObject";
        }
    }
}
