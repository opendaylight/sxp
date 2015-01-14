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

    public static UpdateLegacyNotification create(UpdateMessageLegacy message, SxpConnection connection) {
        return new UpdateLegacyNotification(message, connection);
    }

    private SxpConnection connection;

    private UpdateMessageLegacy message;

    private UpdateLegacyNotification(UpdateMessageLegacy message, SxpConnection connection) {
        super();
        this.message = message;
        this.connection = connection;
    }

    public SxpConnection getConnection() {
        return connection;
    }

    public UpdateMessageLegacy getMessage() {
        return message;
    }
}
