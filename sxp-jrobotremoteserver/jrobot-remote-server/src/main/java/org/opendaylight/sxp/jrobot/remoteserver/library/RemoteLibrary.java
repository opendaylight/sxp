/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.library;

import java.util.Map;
import org.robotframework.javalib.library.KeywordDocumentationRepository;
import org.robotframework.javalib.library.RobotJavaLibrary;

/**
 * An interface for handling libraries in {@link org.opendaylight.sxp.jrobot.remoteserver.RemoteServer}.
 * User libraries are wrapped so that they can be handled in the same way.
 */
public interface RemoteLibrary extends KeywordDocumentationRepository, RobotJavaLibrary, AutoCloseable {

    @Override String[] getKeywordNames();

    /**
     * Executes the keyword with the given name. As some library implementations
     * may be case-, space-, or underscore-sensitive, it is best to use the name
     * as returned from {@link #getKeywordNames()}.
     *
     * @param name      name of the keyword to execute
     * @param arguments positional arguments to the keyword
     * @param kwargs    keyword arguments
     * @return value returned by the keyword
     */
    Object runKeyword(String name, Object[] arguments, Map<String, Object> kwargs);

    /**
     * Gets the argument descriptors for the given keyword name.
     *
     * @param keyword name of the keyword to get argument specifications for
     * @return array of argument specifications
     */
    @Override String[] getKeywordArguments(String keyword);

    /**
     * Gets the tags for the given keyword name.
     *
     * @param keyword name of the keyword to get argument specifications for
     * @return array of tags
     */
    String[] getKeywordTags(String keyword);

    /**
     * Gets the documentation string for the given keyword name.
     *
     * @param name name of the keyword to get documentation for
     * @return keyword documentation string
     */
    @Override String getKeywordDocumentation(String name);

    /**
     * Gets the name of the remote library.
     *
     * @return The name of the remote library, which is the same as the class
     * name
     */
    String getURI();

    @Override void close();
}
