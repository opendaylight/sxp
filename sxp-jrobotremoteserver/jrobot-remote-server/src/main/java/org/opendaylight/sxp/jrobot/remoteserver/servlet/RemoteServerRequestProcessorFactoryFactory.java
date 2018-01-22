/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.servlet;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;

/**
 * Class providing implementation of {@link RequestProcessorFactoryFactory}
 */
public class RemoteServerRequestProcessorFactoryFactory implements RequestProcessorFactoryFactory {

    private final RequestProcessorFactory factory = new RemoteServerRequestProcessorFactory();
    private final JRobotServlet serverMethods;

    /**
     * Constructor creating {@link RequestProcessorFactoryFactory}
     *
     * @param servlet {@link RemoteServerServlet} used for handling requests
     */
    public RemoteServerRequestProcessorFactoryFactory(RemoteServerServlet servlet) {
        this.serverMethods = new ServerMethods(servlet);
    }

    @Override @SuppressWarnings("rawtypes") public RequestProcessorFactory getRequestProcessorFactory(Class aClass)
            throws XmlRpcException {
        //FIXME parameter is unused
        return factory;
    }

    /**
     * Class providing implementation of {@link org.apache.xmlrpc.server.RequestProcessorFactoryFactory.RequestProcessorFactory}
     */
    private class RemoteServerRequestProcessorFactory implements RequestProcessorFactory {

        @Override public JRobotServlet getRequestProcessor(XmlRpcRequest xmlRpcRequest) throws XmlRpcException {
            return serverMethods;
        }
    }
}
