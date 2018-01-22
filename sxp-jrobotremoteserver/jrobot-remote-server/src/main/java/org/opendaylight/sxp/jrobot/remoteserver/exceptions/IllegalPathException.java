/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.exceptions;

/**
 * Thrown when a path is not valid for mapping to a library
 */
public class IllegalPathException extends RuntimeException {

    /**
     * @param message Description of error
     */
    public IllegalPathException(String message) {
        super(message);
    }
}
