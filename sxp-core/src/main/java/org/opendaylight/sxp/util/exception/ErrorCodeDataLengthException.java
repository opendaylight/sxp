/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.exception;

/**
 * Corresponds to a data length error code.
 */
public class ErrorCodeDataLengthException extends Exception {

    private static final long serialVersionUID = 801190427695923174L;

    /**
     * Create a new ErrorCodeDataLengthException.
     *
     * @param string error message
     */
    public ErrorCodeDataLengthException(String string) {
        super(string);
    }
}
