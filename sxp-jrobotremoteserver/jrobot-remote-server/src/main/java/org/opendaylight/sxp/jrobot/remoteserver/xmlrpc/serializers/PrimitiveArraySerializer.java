/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers;

import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.serializer.ObjectArraySerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import static org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.TypeFactory.toObject;

/**
 * A {@link org.apache.xmlrpc.serializer.TypeSerializer} for primitive array elements.
 */
public class PrimitiveArraySerializer extends ObjectArraySerializer {

    /**
     * Creates a new instance.
     *
     * @param pTypeFactory The factory being used for creating serializers.
     * @param pConfig      The configuration being used for creating serializers.
     */
    public PrimitiveArraySerializer(org.apache.xmlrpc.common.TypeFactory pTypeFactory, XmlRpcStreamConfig pConfig) {
        super(pTypeFactory, pConfig);
    }

    @Override protected void writeData(ContentHandler pHandler, Object pObject1) throws SAXException {
        Object[] array = new Object[0];
        if (pObject1 instanceof byte[])
            array = toObject((byte[]) pObject1);
        else if (pObject1 instanceof short[])
            array = toObject((short[]) pObject1);
        else if (pObject1 instanceof int[])
            array = toObject((int[]) pObject1);
        else if (pObject1 instanceof long[])
            array = toObject((long[]) pObject1);
        else if (pObject1 instanceof float[])
            array = toObject((float[]) pObject1);
        else if (pObject1 instanceof double[])
            array = toObject((double[]) pObject1);
        else if (pObject1 instanceof boolean[])
            array = toObject((boolean[]) pObject1);
        super.writeData(pHandler, array);
    }
}
