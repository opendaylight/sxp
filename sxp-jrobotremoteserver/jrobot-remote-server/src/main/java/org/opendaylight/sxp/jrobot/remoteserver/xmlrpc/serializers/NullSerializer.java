/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * A {@link org.apache.xmlrpc.serializer.TypeSerializer} for null elements that converts to String.
 */
public class NullSerializer extends StringSerializer {

    @Override public void write(ContentHandler pHandler, Object pObject) throws SAXException {
        write(pHandler, null, "");
    }
}
