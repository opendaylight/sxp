/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.behavior;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.Notification;

public class Sxpv2 extends Sxpv1 {
    public Sxpv2(Context context) {
        super(context);
    }

    @Override
    public Notification onParseInput(ByteBuf request) throws Exception {
        try {
            return MessageFactory.parse(Version.Version2, request);
        } catch (Exception e) {
            throw new ErrorMessageException(ErrorCodeNonExtended.MessageParseError, e);
        }
    }

    @Override
    public ByteBuf onUpdateMessage(SxpConnection connection, MasterDatabase masterDatabase)
            throws Exception {
        // + IPv6 Bindings
        return super.onUpdateMessage(connection, masterDatabase);
    }
}
