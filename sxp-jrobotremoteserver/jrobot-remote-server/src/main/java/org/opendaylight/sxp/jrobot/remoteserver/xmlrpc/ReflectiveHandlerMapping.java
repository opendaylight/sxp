/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.xmlrpc;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping;

public class ReflectiveHandlerMapping extends AbstractReflectiveHandlerMapping {

    /**
     * Removes the prefixes from all keys in this handler mapping assuming a String was used as the key and period was
     * used as a separator. Example: AccountsReceivable.Billing.getInvoice getInvoice
     */
    public void removePrefixes() {
        Map<String, Object> newHandlerMap = new HashMap<>();
        for (Entry<String, Object> entry : (Set<Entry<String, Object>>) this.handlerMap.entrySet()) {
            String newKey = entry.getKey();
            if (entry.getKey() != null) {
                String key = entry.getKey();
                if (key.contains(".")) {
                    newKey = key.substring(key.lastIndexOf(".") + 1);
                }
            }
            newHandlerMap.put(newKey, entry.getValue());
        }
        this.handlerMap = newHandlerMap;
    }

    /**
     * Adds handlers for the given object to the mapping. The handlers are build by invoking
     * {@link #registerPublicMethods(String, Class)}.
     *
     * @param pKey   The class key, which is passed to {@link #registerPublicMethods(String, Class)}.
     * @param pClass Class, which is responsible for handling the request.
     * @throws XmlRpcException If error occurs
     */
    public void addHandler(String pKey, Class<?> pClass) throws XmlRpcException {
        registerPublicMethods(pKey, pClass);
    }
}
