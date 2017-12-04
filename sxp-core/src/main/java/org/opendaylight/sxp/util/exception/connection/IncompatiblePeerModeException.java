/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.exception.connection;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;

/**
 * IncompatiblePeerModeException
 */
public class IncompatiblePeerModeException extends Exception {

    private static final long serialVersionUID = 801190427695923174L;

    /**
     *
     * @param localMode local mode
     * @param peerMode  peer mode
     */
    public IncompatiblePeerModeException(ConnectionMode localMode, ConnectionMode peerMode) {
        super("[localMode=\"" + localMode + "\" peerMode=\"" + peerMode + "\"]");
    }
}
