/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.exception.message.attribute;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;

/**
 * AttributeNotFoundException
 */
public class AttributeNotFoundException extends Exception {

    private static final long serialVersionUID = 801190427695923174L;

    /**
     * Create a new AttributeNotFoundException.
     *
     * @param type an attribute type
     */
    public AttributeNotFoundException(AttributeType type) {
        super("[" + type.toString() + "]");
    }
}
