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
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.core.messaging.legacy.LegacyMessageFactory;
import org.opendaylight.sxp.core.service.ConnectFacade;
import org.opendaylight.sxp.util.exception.ErrorMessageReceivedException;
import org.opendaylight.sxp.util.exception.connection.IncompatiblePeerVersionException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageCompositionException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageConnectionStateException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.OpenMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.PurgeAllMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.Notification;

public class Sxpv1 implements Strategy {

    protected Context context;

    public Sxpv1(Context context) {
        this.context = context;
    }

    @Override
    public SxpNode getOwner() {
        return context.getOwner();
    }

    @Override
    public void onChannelActivation(ChannelHandlerContext ctx, SxpConnection connection) throws Exception {
        // Default connection mode.
        ConnectionMode connectionMode = ConnectionMode.Speaker;
        if (connection.getMode() != null && !connection.getMode().equals(ConnectionMode.None)) {
            connectionMode = connection.getMode();
        }
        ByteBuf message = LegacyMessageFactory.createOpen(connection.getVersion(), connectionMode);
        LOG.info("{} Sent OPEN {}", connection, MessageFactory.toString(message));
        ctx.writeAndFlush(message);
        if(connection.isStateDeleteHoldDown()) {
            connection.setReconciliationTimer();
        }
        connection.setStatePendingOn();
    }

    @Override
    public void onChannelInactivation(ChannelHandlerContext ctx, SxpConnection connection) throws Exception {
        if (connection.isStateOn(connection.getContextType(ctx))) {
            switch (connection.getContextType(ctx)) {
                case ListenerContext:
                    if (!connection.isPurgeAllMessageReceived()) {
                        LOG.info(connection + " onChannelInactivation/setDeleteHoldDownTimer");
                        connection.setDeleteHoldDownTimer();
                    } else {
                        connection.setStateOff(ctx);
                    }
                    break;
                case SpeakerContext:
                    ctx.writeAndFlush(MessageFactory.createPurgeAll());
                    connection.setStateOff(ctx);
                    break;
            }
        }
    }

    @Override
    public void onException(ChannelHandlerContext ctx, SxpConnection connection) {
        LOG.warn(connection + " onException");
    }

    @Override
    public void onInputMessage(ChannelHandlerContext ctx, SxpConnection connection, Notification message)
            throws Exception {
        LOG.info("{} Handle {}", connection, MessageFactory.toString(message));

        if (message instanceof OpenMessageLegacy) {
            OpenMessageLegacy _message = (OpenMessageLegacy) message;
            if (_message.getType().equals(MessageType.Open)) {
                // The SXP-mode, if not configured explicitly within the device,
                // is set to the opposite value of the one received in the OPEN
                // message.
                if (connection.getMode() == null || connection.getMode().equals(ConnectionMode.None)) {
                    if (_message.getSxpMode().equals(ConnectionMode.Speaker)) {
                        connection.setMode(ConnectionMode.Listener);
                    } else if (_message.getSxpMode().equals(ConnectionMode.Listener)) {
                        connection.setMode(ConnectionMode.Speaker);
                    } else {
                        connection.setMode(ConnectionMode.Speaker);
                    }
                }
                // Close the dual channels.
                connection.closeChannelHandlerContextComplements(ctx);
                // Set connection state.
                connection.setStateOn();
                // Starts sending IP-SGT mappings using the SXP connection.
                LOG.info("{} Connected", connection);
                // Send the OPEN_RESP message with the chosen SXP version.
                ByteBuf response = LegacyMessageFactory.createOpenResp(connection.getVersion(), connection.getMode());
                LOG.info("{} Sent RESP {}", connection, MessageFactory.toString(response));
                ctx.writeAndFlush(response);
                return;

            } else if (_message.getType().equals(MessageType.OpenResp)) {
                // Verifies that the SXP version is compatible with its SXP. If
                // not then send error and close connection.
                if (!connection.getVersion().equals(_message.getVersion())) {
                    throw new ErrorMessageException(ErrorCodeNonExtended.VersionMismatch,
                            new IncompatiblePeerVersionException(connection.getVersion(), _message.getVersion()));
                }
                // Close the dual channels.
                connection.closeChannelHandlerContextComplements(ctx);
                // Set connection state.
                connection.setStateOn();
                // Starts sending IP-SGT mappings using the SXP connection.
                LOG.info("{} Connected", connection);
                return;
            }

        } else if (message instanceof UpdateMessageLegacy) {
            // Accepted only if connection is in ON state.
            if (!connection.isStateOn(SxpConnection.ChannelHandlerContextType.ListenerContext)) {
                throw new UpdateMessageConnectionStateException(connection.getState());
            }
            connection.setUpdateOrKeepaliveMessageTimestamp();
            connection.getContext().getOwner().processUpdateMessage((UpdateMessageLegacy) message, connection);
            return;

        } else if (message instanceof ErrorMessage) {
            throw new ErrorMessageReceivedException(((ErrorMessage) message).getInformation());

        } else if (message instanceof PurgeAllMessage) {
            // Remove all bindings received from the speaker
            // counter-part (no delete hold-down timer).
            LOG.info("{} PURGEALL processing", connection);

            // Get message relevant peer node ID.
            NodeId peerId;
            try {
                peerId = NodeIdConv.createNodeId(connection.getDestination().getAddress());
            } catch (Exception e) {
                LOG.warn(connection + " Unknown message relevant peer node ID | {} | {}", e.getClass().getSimpleName(),
                        e.getMessage());
                return;
            }

            connection.setPurgeAllMessageReceived();
            connection.getContext().getOwner().purgeBindings(peerId);
            connection.getContext().getOwner().notifyService();
            return;
        }
    }

    @Override
    public Notification onParseInput(ByteBuf request) throws Exception {
        try {
            return MessageFactory.parse(Version.Version1, request);
        } catch (Exception e) {
            throw new ErrorMessageException(ErrorCodeNonExtended.MessageParseError, e);
        }
    }

    @Override
    public ByteBuf onUpdateMessage(SxpConnection connection, MasterDatabase masterDatabase)
            throws Exception {
        // Supports: IPv4 Bindings
        // Compose new messages according to all|changed bindings and version.
        try {
            return LegacyMessageFactory.createUpdate(masterDatabase, connection.isUpdateAllExported(),
                    connection.getVersion());
        } catch (Exception e) {
            throw new UpdateMessageCompositionException(connection.getVersion(), connection.isUpdateAllExported(), e);
        }
    }
}
