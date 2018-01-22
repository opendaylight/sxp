/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.keywords;

import org.robotframework.javalib.keyword.DocumentedKeyword;

/**
 * Represent extension of {@link DocumentedKeyword} and {@link TaggedKeyword}
 * capable of verifying if Keyword can be executed with provided arguments
 */
public interface CheckedKeyword extends DocumentedKeyword, TaggedKeyword {

    /**
     * Checks if {@link org.robotframework.javalib.keyword.Keyword} is capable of execution with provided arguments
     *
     * @param args Arguments that will be used in {@link org.robotframework.javalib.keyword.Keyword} execution
     * @return If {@link org.robotframework.javalib.keyword.Keyword} compatible with arguments
     */
    boolean canExecute(Object[] args);

    /**
     * Returns argument types used in current {@link org.robotframework.javalib.keyword.Keyword}
     *
     * @return Arrays containing ordered {@link Class} of each argument
     */
    Class<?>[] getArguments();

}
