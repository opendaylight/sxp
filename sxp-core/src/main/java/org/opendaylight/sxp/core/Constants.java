/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

// FIXME remove getters and make constants public
public final class Constants {
    private static final int MESSAGE_HEADER_LENGTH_LENGTH = 4;
    private static final int MESSAGE_HEADER_TYPE_LENGTH = 4;
    private static final int MESSAGE_LENGTH_MAX = 4096;
    private static final int MESSAGE_EXPORT_QUANTITY = 150;
    private static final int NODE_CONNECTIONS_INITIAL_SIZE = 20;
    private static final int PORT = 64999;

    public final int getMessageHeaderLengthLength() {
        return MESSAGE_HEADER_LENGTH_LENGTH;
    }

    public final int getMessageHeaderTypeLength() {
        return MESSAGE_HEADER_TYPE_LENGTH;
    }

    public final int getMessageLengthMax() {
        return MESSAGE_LENGTH_MAX;
    }

    public final int getMessagesExportQuantity() {
        return MESSAGE_EXPORT_QUANTITY;
    }

    public final int getNodeConnectionsInitialSize() {
        return NODE_CONNECTIONS_INITIAL_SIZE;
    }

    public final int getPort() {
        return PORT;
    }

}
