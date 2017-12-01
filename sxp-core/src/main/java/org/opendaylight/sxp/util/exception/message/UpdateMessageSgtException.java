/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.exception.message;

/**
 * UpdateMessageSgtException
 */
public class UpdateMessageSgtException extends Exception {

    private static final long serialVersionUID = 801190427695923174L;

    /**
     * Create a new UpdateMessageSgtException.
     *
     * @param string an error message
     */
    public UpdateMessageSgtException(String string) {
        super(string);
    }
}
