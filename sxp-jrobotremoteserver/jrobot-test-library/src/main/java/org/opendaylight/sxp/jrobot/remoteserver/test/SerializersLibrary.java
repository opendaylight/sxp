/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.opendaylight.sxp.jrobot.remoteserver.RemoteServer;

@RobotKeywords public class SerializersLibrary extends BaseLibrary {

    public SerializersLibrary(RemoteServer server) {
        super(server);
        server.addSerializer(new TestSerializer());
    }

    @Override public String getURI() {
        return getClass().getSimpleName();
    }

    @Override public String getName() {
        return "Serializers Library";
    }

    @RobotKeyword public DummyObject getDummyObject() {
        return new DummyObject();
    }

    @RobotKeyword public TestObject getTestObject(final int x, final int y, final String name) {
        return new TestObject(x, y, name);
    }

    @RobotKeyword public Object getPOJOObject(final int x, final int y, final String name) {
        return new Object() {

            public String getName() {
                return name;
            }

            public int getX() {
                return x;
            }

            public int getY() {
                return y;
            }
        };
    }

    private class DummyObject {

        @Override public String toString() {
            return "Dummy Object";
        }
    }


    private class TestObject {

        private final int x, y;
        private final String name;

        private TestObject(int x, int y, String name) {
            this.x = x;
            this.y = y;
            this.name = name;
        }
    }


    private class TestSerializer extends StdSerializer<TestObject> {

        protected TestSerializer() {
            super(TestObject.class);
        }

        @Override public void serialize(TestObject value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("name", value.name);
            jgen.writeArrayFieldStart("coordinates");
            jgen.writeString(Integer.toString(value.x));
            jgen.writeString(Integer.toString(value.y));
            jgen.writeEndArray();
            jgen.writeEndObject();
        }
    }
}
