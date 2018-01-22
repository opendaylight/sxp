/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation represents Docs string that is presented as description of Robot-framework Keyword
 */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface KeywordDocumentation {

    /**
     * @return Brief description of Keyword
     */
    String value() default "";
}
