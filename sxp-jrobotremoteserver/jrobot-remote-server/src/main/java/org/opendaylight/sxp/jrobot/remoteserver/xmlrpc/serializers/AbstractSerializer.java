/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

abstract public class AbstractSerializer<T> extends TypeSerializerImpl implements TypeSerializer {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSerializer.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();

    protected AbstractSerializer() {
        mapper.registerModule(new SimpleModule().addSerializer(getSerializer()));
    }

    protected abstract JsonSerializer<T> getSerializer();

    public boolean canSerialize(Class<?> aClass) {
        return mapper.canSerialize(aClass) && getSerializer().handledType().equals(aClass);
    }

    @Override public synchronized void write(ContentHandler pHandler, Object pObject) throws SAXException {
        String value;
        synchronized (mapper) {
            try {
                value = mapper.writeValueAsString(pObject);
            } catch (JsonProcessingException e) {
                LOG.error("Cannot serialize {} {}", pHandler, pObject, e);
                throw new RuntimeException(String.format("Cannot serialize data %s", pObject), e);
            }
        }
        write(pHandler, VALUE_TAG, value);
    }
}
