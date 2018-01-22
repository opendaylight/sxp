/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers;

import org.apache.xmlrpc.common.TypeFactory;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.serializer.ObjectArraySerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class IterableSerializer extends ObjectArraySerializer {

    public IterableSerializer(TypeFactory pTypeFactory, XmlRpcStreamConfig pConfig) {
        super(pTypeFactory, pConfig);
    }

    @Override protected void writeData(ContentHandler pHandler, Object pObject) throws SAXException {
        Iterable<?> obj = (Iterable<?>) pObject;
        for (Object anObj : obj) {
            writeObject(pHandler, anObj);
        }
    }
}
