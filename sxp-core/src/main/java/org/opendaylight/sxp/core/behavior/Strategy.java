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
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.exception.ErrorMessageReceivedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageCompositionException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageConnectionStateException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SXP supports various versions. The details of what is supported in each of
 * the version follows:
 *
 * <pre>
 * +-----+----------+----------+------------+-----------+--------------+
 * | Ver | IPv4     | IPv6     | Subnet     | Loop      | SXP          |
 * |     | Bindings | Bindings | Binding    | Detection | Capability   |
 * |     |          |          | Expansion  |           | Exchange     |
 * +-----+----------+----------+------------+-----------+--------------+
 * | 1   | Yes      | No       | No         | No        | No           |
 * | 2   | Yes      | Yes      | No         | No        | No           |
 * | 3   | Yes      | Yes      | Yes        | No        | No           |
 * | 4   | Yes      | Yes      | Yes        | Yes       | Yes          |
 * +-----+----------+----------+------------+-----------+--------------+
 * </pre>
 */
public interface Strategy {

        Logger LOG = LoggerFactory.getLogger(Strategy.class.getName());

        /**
         * @return Gets SxpNode on which is this strategy executed
         */
        SxpNode getOwner();

        /**
         * Logic for establishment of Connection
         *
         * @param ctx        ChannelHandlerContext on which is communication
         * @param connection SxpConnection that participate in communication
         */
        void onChannelActivation(ChannelHandlerContext ctx, SxpConnection connection);

        /**
         * Logic for disconnecting of Connection
         *
         * @param ctx        ChannelHandlerContext on which communication will be closed
         * @param connection SxpConnection that participated in communication
         */
        void onChannelInactivation(ChannelHandlerContext ctx, SxpConnection connection);

        /**
         * Logic for handling of errors
         *
         * @param ctx        ChannelHandlerContext on which is communication
         * @param connection SxpConnection that participate in communication
         */
        void onException(ChannelHandlerContext ctx, SxpConnection connection);

        /**
         * Logic for handling incoming messages
         *
         * @param ctx        ChannelHandlerContext on which is communication
         * @param connection SxpConnection that participate in communication
         * @param message    Notification that have been received
         * @throws ErrorMessageException                 If error occurs during handling of Notification
         * @throws UpdateMessageConnectionStateException If Update message was received in wrong state
         * @throws ErrorMessageReceivedException         If Peer send error message
         */
        void onInputMessage(ChannelHandlerContext ctx, SxpConnection connection, Notification message)
                throws ErrorMessageException, ErrorMessageReceivedException, UpdateMessageConnectionStateException;

        /**
         * Logic for decoding incoming data
         *
         * @param request ByteBuf containing received data
         * @return Notification with decoded message
         * @throws ErrorMessageException If received data was corrupted or incorrect
         */
        Notification onParseInput(ByteBuf request) throws ErrorMessageException;

        /**
         * Logic that generate message containing Bindings for export
         *
         * @param connection     SxpConnection that participate in communication
         * @param deleteBindings Bindings that will be deleted
         * @param addBindings    Bindings that will be added
         * @return ByteBuf containing Update message
         * @throws UpdateMessageCompositionException If during generating of message error occurs
         */
        <T extends SxpBindingFields> ByteBuf onUpdateMessage(SxpConnection connection, List<T> deleteBindings,
                List<T> addBindings, SxpBindingFilter bindingFilter) throws UpdateMessageCompositionException;
}
