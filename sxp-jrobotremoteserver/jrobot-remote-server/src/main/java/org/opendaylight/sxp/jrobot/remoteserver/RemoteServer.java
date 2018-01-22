/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver;

import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.util.Map;
import org.opendaylight.sxp.jrobot.remoteserver.library.RemoteLibrary;

/**
 * Provides way of providing and managing {@link RemoteLibrary} that are used as Robot-framework remote libraries
 */
public interface RemoteServer {

    /**
     * Stops the remote server immediately.
     */
    void stop();

    /**
     * A non-blocking method for stopping the remote server that allows requests
     * to complete within the given timeout before shutting down the server. New
     * connections will not be accepted after calling this.
     *
     * @param timeoutMS the milliseconds to wait for existing request to complete
     *                  before stopping the server
     */
    void stop(int timeoutMS);

    /**
     * Starts the remote server. Add test libraries first before calling this.
     *
     * @throws Exception If server cannot be started
     */
    void start() throws Exception;

    /**
     * Gets a copy of the current library map. Keys in the map are the paths and
     * the values are {@link RemoteLibrary} wrappers of the libraries being
     * served.
     *
     * @return a copy of the current library map
     */
    Map<String, RemoteLibrary> getLibraryMap();

    /**
     * Removes the library mapped to the given path if the mapping exists.
     *
     * @param path path for the library whose mapping is to be removed
     * @return the previous library associated with the path, or null if there
     * was no mapping for the path.
     */
    RemoteLibrary removeLibrary(String path);

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
     * @param path    path to map the test library to
     * @param library instance of the test library
     */
    void putLibrary(String path, RemoteLibrary library);

    /**
     * @param serializer Instance of {@link StdSerializer} that will be responsible for serialization
     * @param <T>        Type that will be serialized
     */
    <T> void addSerializer(StdSerializer<T> serializer);

}
