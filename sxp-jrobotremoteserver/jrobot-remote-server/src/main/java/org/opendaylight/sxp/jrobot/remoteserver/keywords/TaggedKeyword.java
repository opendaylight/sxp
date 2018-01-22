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
 * Represent extension of {@link DocumentedKeyword} providing option to for tagging of {@link org.robotframework.javalib.keyword.Keyword}
 */
public interface TaggedKeyword extends DocumentedKeyword {

    /**
     * @return Tags associated with current {@link org.robotframework.javalib.keyword.Keyword}
     */
    String[] getTags();

}
