/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.ContentHandler;

/**
 * Tests for {@link AbstractSerializer}
 */
public class AbstractSerializerTest {

    private AbstractSerializer<CustomObject> abstractSerializer;
    private JsonSerializer<CustomObject> serializer;
    private ContentHandler handler;

    @Before public void setUp() throws Exception {
        handler = Mockito.mock(ContentHandler.class);
        serializer = new StdSerializer<CustomObject>(CustomObject.class) {

            @Override public void serialize(CustomObject value, JsonGenerator jgen, SerializerProvider provider)
                    throws IOException {
                jgen.writeString(value.id);
            }
        };
        abstractSerializer = new AbstractSerializer<CustomObject>() {

            @Override protected JsonSerializer<CustomObject> getSerializer() {
                return serializer;
            }
        };
    }

    @Test public void canSerialize() throws Exception {
        Assert.assertFalse(abstractSerializer.canSerialize(Object.class));
        Assert.assertFalse(abstractSerializer.canSerialize(String.class));
        Assert.assertTrue(abstractSerializer.canSerialize(CustomObject.class));
    }

    @Test public void write() throws Exception {
        abstractSerializer.write(handler, new CustomObject("Test"));
        Mockito.verify(handler).characters(Mockito.eq("\"Test\"".toCharArray()), Mockito.eq(0), Mockito.eq(6));

        abstractSerializer.write(handler, new CustomObject("Test2"));
        Mockito.verify(handler).characters(Mockito.eq("\"Test2\"".toCharArray()), Mockito.eq(0), Mockito.eq(7));
    }

    private class CustomObject {

        String id;

        CustomObject(String id) {
            this.id = id;
        }
    }

}
