/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.exception.connection;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

/**
 * IncompatiblePeerVersionException
 */
public class IncompatiblePeerVersionException extends Exception {

    private static final long serialVersionUID = 801190427695923174L;

    /**
     *
     * @param localLegacy legacy local version
     * @param peerVersion peer version
     */
    public IncompatiblePeerVersionException(boolean localLegacy, Version peerVersion) {
        super("[localVersion=\"" + (localLegacy ? "Legacy" : "NonLegacy") + "\" peerVersion=\"" + peerVersion + "\"]");
    }

    public IncompatiblePeerVersionException(Version localVersion, Version peerVersion) {
        super("[localVersion=\"" + localVersion + "\" peerVersion=\"" + peerVersion + "\"]");
    }
}
