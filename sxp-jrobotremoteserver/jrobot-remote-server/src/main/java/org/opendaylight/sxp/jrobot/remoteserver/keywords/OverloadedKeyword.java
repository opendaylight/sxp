/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.keywords;

import java.lang.reflect.Method;
import org.robotframework.javalib.keyword.DocumentedKeyword;

/**
 * Represent extension of {@link DocumentedKeyword} and {@link TaggedKeyword}
 * capable of managing multiple implementations of keyword
 */
public interface OverloadedKeyword extends DocumentedKeyword, TaggedKeyword {

    /**
     * Adds alternative {@link org.robotframework.javalib.keyword.Keyword} implementation,
     * later used for execution of keyword if appropriate arguments are provided
     *
     * @param method Implementation that will be used as alternative,
     *               for {@link org.robotframework.javalib.keyword.Keyword} execution
     */
    void addOverload(Method method);
}
