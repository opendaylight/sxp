/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.exception.node;

public class DomainNotFoundException extends RuntimeException {

    public DomainNotFoundException(String ownerName, String message) {
        super("[" + ownerName + "] " + message);
    }

}
