/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.BooleanSerializer;
import org.apache.xmlrpc.serializer.ByteArraySerializer;
import org.apache.xmlrpc.serializer.DoubleSerializer;
import org.apache.xmlrpc.serializer.I4Serializer;
import org.apache.xmlrpc.serializer.ListSerializer;
import org.apache.xmlrpc.serializer.MapSerializer;
import org.apache.xmlrpc.serializer.ObjectArraySerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers.AbstractSerializer;
import org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers.CharArraySerializer;
import org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers.IterableSerializer;
import org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers.NullSerializer;
import org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers.PojoSerializer;
import org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers.PrimitiveArraySerializer;
import org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers.StringSerializer;
import org.xml.sax.SAXException;

public class TypeFactory extends TypeFactoryImpl {

    private static final TypeSerializer STRING_SERIALIZER = new StringSerializer();
    private static final TypeSerializer I4_SERIALIZER = new I4Serializer();
    private static final TypeSerializer DOUBLE_SERIALIZER = new DoubleSerializer();
    private static final TypeSerializer BOOLEAN_SERIALIZER = new BooleanSerializer();
    private static final TypeSerializer NULL_SERIALIZER = new NullSerializer();
    private static final TypeSerializer CHAR_ARRAY_SERIALIZER = new CharArraySerializer();
    private static final TypeSerializer POJO_SERIALIZER = new PojoSerializer();
    private static final TypeParser BYTE_ARRAY_PARSER = new ByteArrayToStringParser();
    private final List<AbstractSerializer<?>> serializers = new ArrayList<>();

    public TypeFactory(XmlRpcController pController) {
        super(pController);
    }

    public static Byte[] toObject(final byte[] primitives) {
        final Byte[] bytes = new Byte[primitives.length];
        Arrays.setAll(bytes, n -> primitives[n]);
        return bytes;
    }

    public static Short[] toObject(final short[] primitives) {
        final Short[] bytes = new Short[primitives.length];
        Arrays.setAll(bytes, n -> primitives[n]);
        return bytes;
    }

    public static Integer[] toObject(final int[] primitives) {
        return IntStream.of(primitives).boxed().toArray(Integer[]::new);
    }

    public static Long[] toObject(final long[] primitives) {
        return LongStream.of(primitives).boxed().toArray(Long[]::new);
    }

    public static Float[] toObject(final float[] primitives) {
        final Float[] bytes = new Float[primitives.length];
        Arrays.setAll(bytes, n -> primitives[n]);
        return bytes;
    }

    public static Double[] toObject(final double[] primitives) {
        return DoubleStream.of(primitives).boxed().toArray(Double[]::new);
    }

    public static Boolean[] toObject(final boolean[] primitives) {
        final Boolean[] bytes = new Boolean[primitives.length];
        Arrays.setAll(bytes, n -> primitives[n]);
        return bytes;
    }

    @Override public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException {
        if (Objects.isNull(pObject))
            return NULL_SERIALIZER;
        else if (pObject instanceof String)
            return STRING_SERIALIZER;
        else if (pObject instanceof Integer || pObject instanceof Short || pObject instanceof Byte)
            return I4_SERIALIZER;
        else if (pObject instanceof Boolean)
            return BOOLEAN_SERIALIZER;
        else if (pObject instanceof Double || pObject instanceof Float)
            return DOUBLE_SERIALIZER;
        else if (pObject instanceof Object[])
            return new ObjectArraySerializer(this, pConfig);
        else if (pObject instanceof List)
            return new ListSerializer(this, pConfig);
        else if (pObject instanceof Map)
            return new MapSerializer(this, pConfig);
        else if (pObject instanceof Iterable)
            return new IterableSerializer(this, pConfig);
        else if (pObject instanceof char[])
            return CHAR_ARRAY_SERIALIZER;
        else if (pObject.getClass().isArray())
            return new PrimitiveArraySerializer(TypeFactory.this, pConfig);
        for (AbstractSerializer<?> serializer : serializers) {
            if (serializer.canSerialize(pObject.getClass())) {
                return serializer;
            }
        }
        return POJO_SERIALIZER;
    }

    @Override public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI,
            String pLocalName) {
        if (ByteArraySerializer.BASE_64_TAG.equals(pLocalName)) {
            return BYTE_ARRAY_PARSER;
        }
        return super.getParser(pConfig, pContext, pURI, pLocalName);
    }

    public <T> void addSerializer(final StdSerializer<T> serializer) {
        Objects.requireNonNull(serializer);
        serializers.add(new AbstractSerializer<T>() {

            @Override protected JsonSerializer<T> getSerializer() {
                return serializer;
            }
        });
    }

}
