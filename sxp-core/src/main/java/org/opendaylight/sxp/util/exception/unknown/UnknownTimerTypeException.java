/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.exception.unknown;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.TimerType;

public class UnknownTimerTypeException extends RuntimeException{

    /** */
    private static final long serialVersionUID = 801190427695923174L;

    public UnknownTimerTypeException(String message) {
        super(message);
    }

    public UnknownTimerTypeException(TimerType timerType) {
        super(timerType.toString());
    }
}
