/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessage;

public final class UpdateNotification {

    /**
     * Creates new UpdateNotification using provided values
     *
     * @param message    UpdateMessage to be used
     * @param connection SxpConnection to be used
     * @return UpdateNotification created using provided values
     */
    public static UpdateNotification create(UpdateMessage message, SxpConnection connection) {
        return new UpdateNotification(message, connection);
    }

    private SxpConnection connection;

    private UpdateMessage message;

    /**
     * Default constructor
     *
     * @param message    UpdateMessage to be used
     * @param connection SxpConnection to be used
     */
    private UpdateNotification(UpdateMessage message, SxpConnection connection) {
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
    public UpdateMessage getMessage() {
        return message;
    }
}
