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
import org.opendaylight.sxp.core.handler.MessageDecoder;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.core.messaging.legacy.LegacyMessageFactory;
import org.opendaylight.sxp.util.exception.ErrorMessageReceivedException;
import org.opendaylight.sxp.util.exception.connection.IncompatiblePeerVersionException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageCompositionException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageConnectionStateException;
import org.opendaylight.sxp.util.exception.message.attribute.AddressLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeVariantException;
import org.opendaylight.sxp.util.exception.message.attribute.TlvNotFoundException;
import org.opendaylight.sxp.util.exception.unknown.UnknownNodeIdException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpMessageTypeException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.ErrorMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.PurgeAllMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.Notification;

import java.net.UnknownHostException;

/**
 * SxpLegacy class provides logic for handling connection on Version 1/2/3
 */
public class SxpLegacy implements Strategy {

    protected Context context;

    /**
     * Default constructor that sets its Context
     *
     * @param context Context to be set
     */
    public SxpLegacy(Context context) {
        this.context = context;
    }

    @Override
    public SxpNode getOwner() {
        return context.getOwner();
    }

    @Override
    public void onChannelActivation(ChannelHandlerContext ctx, SxpConnection connection) {
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
    public void onChannelInactivation(ChannelHandlerContext ctx, SxpConnection connection) {
        SxpConnection.ChannelHandlerContextType type = connection.getContextType(ctx);
        if (connection.isStateOn(type)) {
            switch (type) {
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
        } else if (connection.isStatePendingOn()) {
            connection.setStateOff(ctx);
        }
    }

    @Override
    public void onException(ChannelHandlerContext ctx, SxpConnection connection) {
        LOG.warn(connection + " onException");
    }

    /**
     * @param connection
     * @param _message
     * @param ctx
     * @return
     */
    private boolean checkModeMismatch(SxpConnection connection, OpenMessageLegacy _message, ChannelHandlerContext ctx) {
        if (!(connection.isModeListener() && _message.getSxpMode().equals(ConnectionMode.Speaker)) && !(
                connection.isModeSpeaker() && _message.getSxpMode().equals(ConnectionMode.Listener)) && !(
                connection.isModeBoth() && _message.getSxpMode().equals(ConnectionMode.Both))) {
            MessageDecoder.sendErrorMessage(ctx, new ErrorMessageException(ErrorCode.OpenMessageError, null),
                    connection);
            connection.setStateOff(ctx);
            return true;
        }
        return false;
    }

    /**
     * Sets connection mode and NodeId of SxpConnection according to received message
     *
     * @param connection SxpConnection that will be updated
     * @param mode       Mode received in message
     */
    private void setConnectionMode(SxpConnection connection,ConnectionMode mode){
        try {
            connection.setNodeIdRemote(NodeIdConv.createNodeId(connection.getDestination().getAddress()));
        } catch (UnknownNodeIdException e) {
            LOG.error("{} Unknown message relevant peer node ID", connection);
        }
        connection.setModeRemote(mode);
        if (connection.getMode() == null || connection.getMode().equals(ConnectionMode.None)) {
            if (mode.equals(ConnectionMode.Speaker)) {
                connection.setMode(ConnectionMode.Listener);
            } else {
                connection.setMode(ConnectionMode.Speaker);
            }
        }
    }

    @Override
    public void onInputMessage(ChannelHandlerContext ctx, SxpConnection connection, Notification message)
            throws ErrorMessageException, UpdateMessageConnectionStateException, ErrorMessageReceivedException {
        LOG.info("{} Handle {}", connection, MessageFactory.toString(message));

        if (message instanceof OpenMessageLegacy) {
            OpenMessageLegacy _message = (OpenMessageLegacy) message;
            if (_message.getType().equals(MessageType.Open)) {
                // The SXP-mode, if not configured explicitly within the device,
                // is set to the opposite value of the one received in the OPEN
                // message.
                setConnectionMode(connection, _message.getSxpMode());
                if (!checkModeMismatch(connection, _message, ctx)) {
                    // Close the dual channels.
                    connection.closeChannelHandlerContextComplements(ctx);
                    connection.markChannelHandlerContext(ctx);
                    // Set connection state.
                    connection.setStateOn();
                    // Starts sending IP-SGT mappings using the SXP connection.
                    LOG.info("{} Connected", connection);
                    // Send the OPEN_RESP message with the chosen SXP version.
                    ByteBuf
                            response =
                            LegacyMessageFactory.createOpenResp(connection.getVersion(), connection.getMode());
                    LOG.info("{} Sent RESP {}", connection, MessageFactory.toString(response));
                    ctx.writeAndFlush(response);
                    return;
                }
            } else if (_message.getType().equals(MessageType.OpenResp)) {
                // Verifies that the SXP version is compatible with its SXP. If
                // not then send error and close connection.
                if (!connection.getVersion().equals(_message.getVersion())) {
                    throw new ErrorMessageException(ErrorCodeNonExtended.VersionMismatch,
                            new IncompatiblePeerVersionException(connection.getVersion(), _message.getVersion()));
                }
                setConnectionMode(connection, _message.getSxpMode());
                if (!checkModeMismatch(connection, _message, ctx)) {
                    // Close the dual channels.
                    connection.closeChannelHandlerContextComplements(ctx);
                    connection.markChannelHandlerContext(ctx);
                    // Set connection state.
                    connection.setStateOn();
                    // Starts sending IP-SGT mappings using the SXP connection.
                    LOG.info("{} Connected", connection);
                    return;
                }
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
            if (connection.getNodeIdRemote() == null) {
                LOG.warn("{} Unknown message relevant peer node ID", connection);
                return;
            }
            connection.setPurgeAllMessageReceived();
            connection.getContext().getOwner().purgeBindings(connection.getNodeIdRemote());
            connection.getContext().getOwner().notifyService();
            return;
        }
        LOG.warn("{} Cannot handle message, ignoring: {}", connection, MessageFactory.toString(message));
    }

    @Override
    public Notification onParseInput(ByteBuf request) throws ErrorMessageException {
        try {
            return MessageFactory.parse(context.getVersion(), request);
        } catch (AttributeVariantException | UnknownNodeIdException | UnknownSxpMessageTypeException | AddressLengthException | AttributeLengthException | UnknownHostException | TlvNotFoundException | UnknownPrefixException e) {
            throw new ErrorMessageException(ErrorCodeNonExtended.MessageParseError, e);
        }
    }

    @Override
    public ByteBuf onUpdateMessage(SxpConnection connection, MasterDatabase masterDatabase)
            throws UpdateMessageCompositionException {
        // Compose new messages according to all|changed bindings and version.
        return LegacyMessageFactory.createUpdate(masterDatabase, connection.isUpdateAllExported(),
                connection.getVersion());
    }
}
