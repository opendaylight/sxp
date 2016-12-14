/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.spi;

import java.io.IOException;

/**
 * Purpose: provides access to system CLI
 */
public interface SystemCall {

    /**
     * @param s Command that will be executed by system
     * @return system callback associated with provided command
     * @throws IOException If runtime providing execution is not available
     */
    java.lang.Process execute(String s) throws IOException;

}
