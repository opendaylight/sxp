/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc.serializers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;
import org.apache.ws.commons.util.Base64;
import org.apache.ws.commons.util.Base64.Encoder;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * A {@link org.apache.xmlrpc.serializer.TypeSerializer} for strings.
 */
public class StringSerializer extends org.apache.xmlrpc.serializer.StringSerializer {

    public static final String BASE_64_TAG = "base64";
    private static final Pattern pattern = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]");

    public void write(ContentHandler pHandler, Object pObject) throws SAXException {
        String value = pObject.toString();
        if (pattern.matcher(value).find()) {
            pHandler.startElement("", VALUE_TAG, VALUE_TAG, ZERO_ATTRIBUTES);
            pHandler.startElement("", BASE_64_TAG, BASE_64_TAG, ZERO_ATTRIBUTES);
            try {
                byte[] buffer = value.getBytes("UTF-8");
                if (buffer.length > 0) {
                    char[] charBuffer = new char[buffer.length >= 1024 ? 1024 : ((buffer.length + 3) / 4) * 4];
                    Encoder encoder = new Base64.SAXEncoder(charBuffer, 0, null, pHandler);
                    try {
                        encoder.write(buffer, 0, buffer.length);
                        encoder.flush();
                    } catch (Base64.SAXIOException e) {
                        throw e.getSAXException();
                    } catch (IOException e) {
                        throw new SAXException(e);
                    }
                }
                pHandler.endElement("", BASE_64_TAG, BASE_64_TAG);
                pHandler.endElement("", VALUE_TAG, VALUE_TAG);
            } catch (UnsupportedEncodingException e1) {
                throw new SAXException(e1);
            }
        } else {
            super.write(pHandler, null, value);
        }
    }

}
