/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class PojoSerializer extends TypeSerializerImpl {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override public synchronized void write(ContentHandler pHandler, Object pObject) throws SAXException {
        String value;
        synchronized (mapper) {
            try {
                value = mapper.writeValueAsString(pObject);
            } catch (JsonProcessingException e) {
                value = pObject.toString();
            }
        }
        write(pHandler, VALUE_TAG, value);
    }
}
