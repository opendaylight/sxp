/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.exception.node;

/**
 * DatabaseAccessException
 */
public class DatabaseAccessException extends Exception {

    private static final long serialVersionUID = 801190427695923174L;

    /**
     * Create a new DatabaseAccessException.
     *
     * @param ownerName the name of the owner
     * @param message a message
     */
    public DatabaseAccessException(String ownerName, String message) {
        super("[" + ownerName + "] " + message);
    }

    @SuppressWarnings("all")
    public DatabaseAccessException(String ownerName, String message, Exception e) {
        super("[" + ownerName + "] " + message + " | " + e.getClass().getSimpleName() + (
                e.getMessage() != null && !e.getMessage().isEmpty() ? " | " + e.getMessage() : ""));
    }
}
