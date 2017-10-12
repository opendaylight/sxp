/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core;

public final class Constants {

    public static final int MESSAGE_HEADER_LENGTH_LENGTH = 4;
    public static final int MESSAGE_HEADER_TYPE_LENGTH = 4;
    public static final int MESSAGE_LENGTH_MAX = 4096;
    public static final int MESSAGE_EXPORT_QUANTITY = 150;
    public static final int NODE_CONNECTIONS_INITIAL_SIZE = 20;
    public static final int PORT = 64999;

    private Constants() {
    }
}
