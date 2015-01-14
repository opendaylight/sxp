/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessage;

public final class UpdateNotification {

    public static UpdateNotification create(UpdateMessage message, SxpConnection connection) {
        return new UpdateNotification(message, connection);
    }

    private SxpConnection connection;

    private UpdateMessage message;

    private UpdateNotification(UpdateMessage message, SxpConnection connection) {
        super();
        this.message = message;
        this.connection = connection;
    }

    public SxpConnection getConnection() {
        return connection;
    }

    public UpdateMessage getMessage() {
        return message;
    }
}
