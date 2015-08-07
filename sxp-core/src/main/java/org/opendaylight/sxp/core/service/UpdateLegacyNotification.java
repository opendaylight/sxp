/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessageLegacy;

public final class UpdateLegacyNotification {

    /**
     * Creates new UpdateLegacyNotification using provided values
     *
     * @param message    UpdateMessageLegacy to be used
     * @param connection SxpConnection to be used
     * @return UpdateLegacyNotification created using provided values
     */
    public static UpdateLegacyNotification create(UpdateMessageLegacy message, SxpConnection connection) {
        return new UpdateLegacyNotification(message, connection);
    }

    private SxpConnection connection;

    private UpdateMessageLegacy message;

    /**
     * Default constructor
     *
     * @param message    UpdateMessageLegacy to be used
     * @param connection SxpConnection to be used
     */
    private UpdateLegacyNotification(UpdateMessageLegacy message, SxpConnection connection) {
        super();
        this.message = message;
        this.connection = connection;
    }

    /**
     * @return Gets SxpConnection to which message belongs
     */
    public SxpConnection getConnection() {
        return connection;
    }

    /**
     * @return Gets Message
     */
    public UpdateMessageLegacy getMessage() {
        return message;
    }
}
