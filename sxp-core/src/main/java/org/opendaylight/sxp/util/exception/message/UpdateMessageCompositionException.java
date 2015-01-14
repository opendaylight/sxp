/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.exception.message;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

public class UpdateMessageCompositionException extends Exception {

    /** */
    private static final long serialVersionUID = 801190427695923174L;

    public UpdateMessageCompositionException(Version version, boolean updateExported, Exception e) {
        super("UPDATEv" + version.getIntValue() + "(" + (updateExported ? "C" : "A")
                + ") message composition failed | " + e.getClass().getSimpleName() + " | " + e.getMessage());
    }
}
