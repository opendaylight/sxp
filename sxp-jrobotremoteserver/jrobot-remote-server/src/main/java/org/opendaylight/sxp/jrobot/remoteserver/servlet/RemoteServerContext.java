/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.servlet;

import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;
import org.opendaylight.sxp.jrobot.remoteserver.library.RemoteLibrary;

/**
 * Represent extension of {@link Servlet} adding capabilities,
 * for managing Robot-framework remote libraries
 */
public interface RemoteServerContext extends Servlet {

    /**
     * Gets a copy of the current library map. Keys in the map are the paths and
     * the values are {@link RemoteLibrary} wrappers of the libraries being
     * served.
     *
     * @return a copy of the current library map
     */
    Map<String, RemoteLibrary> getLibraryMap();

    /**
     * Returns a {@link HttpServletRequest} object that contains the request the
     * client has made of the remote server servlet.
     *
     * @return {@link HttpServletRequest} object that contains the request the
     * client has made of the remote server servlet
     */
    HttpServletRequest getRequest();

    /**
     * Map the given test library to the specified path. Paths must:
     * <ul>
     * <li>start with a /</li>
     * <li>contain only alphanumeric characters or any of these: / - . _ ~</li>
     * <li>not end in a /</li>
     * <li>not contain a repeating sequence of /s</li>
     * </ul>
     * Example: <code>putLibrary("/myLib", new MyLibrary());</code>
     *
     * @param library instance of the test library
     * @param path    path to map the test library to
     * @return the previous library mapped to the path, or null if there was no
     * mapping for the path
     */
    RemoteLibrary putLibrary(String path, RemoteLibrary library);

    /**
     * Removes the library mapped to the given path if the mapping exists
     *
     * @param path path for the library whose mapping is to be removed
     * @return the previous library associated with the path, or null if there
     * was no mapping for the path.
     */
    RemoteLibrary removeLibrary(String path);

    /**
     * Returns the servlets instance of {@link XmlRpcServletServer}.
     *
     * @return The configurable instance of {@link XmlRpcServletServer}.
     */
    XmlRpcServletServer getXmlRpcServletServer();

}
