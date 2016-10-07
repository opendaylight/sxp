/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit;

import org.opendaylight.sxp.csit.libraries.AbstractLibrary;
import org.robotframework.remoteserver.library.RemoteLibrary;

import java.util.Map;

/**
 * Robot remote library service API
 */
public interface RobotLibraryServer {

    /**
     * @return Port on which it is listening
     */
    Integer getLocalPort();

    /**
     * @param port Port on which it will listen
     */
    void setPort(int port);

    /**
     * @return IP on which it is listening
     */
    String getHost();

    /**
     * @param hostName IP on which it will listen
     */
    void setHost(String hostName);

    /**
     * Adds Library to Remote library server
     *
     * @param library Library to be added
     * @return RemoteLibrary callback
     */
    RemoteLibrary addLibrary(AbstractLibrary library);

    /**
     * @param path URI path of library
     * @return Removed library
     */
    RemoteLibrary removeLibrary(String path);

    /**
     * Gets all libraries assigned to RemoteLibrary server
     *
     * @return Map consisting of URI and appropriate library
     */
    Map<String, RemoteLibrary> getLibraryMap();

    /**
     * Stops Remote library server
     *
     * @param timeoutMS Milliseconds to wait for stop
     * @throws Exception If Error occurs
     */
    void stop(int timeoutMS) throws Exception;

    /**
     * Stops Remote library server
     *
     * @throws Exception If error occurs
     */
    void stop() throws Exception;

    /**
     * Start Remote library server
     *
     * @throws Exception If error occurs
     */
    void start() throws Exception;

}
