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
import java.net.UnknownHostException;
import java.util.List;
import org.opendaylight.sxp.core.SxpConnection;
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
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.ErrorMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.Notification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.PurgeAllMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessageLegacy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SxpLegacy class provides logic for handling connection on Version 1/2/3
 */
@SuppressWarnings("all")
public final class SxpLegacy extends AbstractStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(SxpLegacy.class);

    private final Version sxpVersion;

    /**
     * Default constructor specifying the legacy version
     *
     * @param sxpVersion SXP version to be set
     */
    public SxpLegacy(Version sxpVersion) {
        this.sxpVersion = sxpVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onChannelActivation(ChannelHandlerContext ctx, SxpConnection connection) {
        // Default connection mode.
        ConnectionMode connectionMode = ConnectionMode.Speaker;
        if (connection.getMode() != null && !connection.getMode().equals(ConnectionMode.None)) {
            connectionMode = connection.getMode();
        }
        ByteBuf message = LegacyMessageFactory.createOpen(connection.getVersion(), connectionMode);
        LOG.info("{} Sending OPEN {}", connection, MessageFactory.toString(message));
        ctx.writeAndFlush(message);
        if (connection.isStateDeleteHoldDown()) {
            connection.setReconciliationTimer();
        }
        if (!connection.isStateOn()) {
            connection.setStatePendingOn();
        }
    }

    /**
     * Checks if SxpConnections and its peer have correct connection modes
     *
     * @param connection SxpConnection to check version against
     * @param legacyOpenMsg   Message containing version to check
     * @param ctx        ChannelHandlerContext on which error will be send if mismatch occurred
     * @return If ModeMismatch occurred
     */
    private boolean checkModeMismatch(SxpConnection connection, OpenMessageLegacy legacyOpenMsg, ChannelHandlerContext ctx) {
        if (!(connection.isModeListener() && legacyOpenMsg.getSxpMode().equals(ConnectionMode.Speaker)) && !(
                connection.isModeSpeaker() && legacyOpenMsg.getSxpMode().equals(ConnectionMode.Listener)) && !(
                connection.isModeBoth() && legacyOpenMsg.getSxpMode().equals(ConnectionMode.Both))) {
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
     */
    private void setNodeIdRemote(SxpConnection connection) {
        try {
            connection.setNodeIdRemote(NodeIdConv.createNodeId(connection.getDestination().getAddress()));
        } catch (UnknownNodeIdException e) {
            LOG.error("{} Unknown message relevant peer node ID", connection, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInputMessage(ChannelHandlerContext ctx, SxpConnection connection, Notification message)
            throws ErrorMessageException, UpdateMessageConnectionStateException, ErrorMessageReceivedException {
        LOG.info("{} Handle {}", connection, MessageFactory.toString(message));

        if (message instanceof OpenMessageLegacy) {
            OpenMessageLegacy legacyOpenMsg = (OpenMessageLegacy) message;
            if (legacyOpenMsg.getType().equals(MessageType.Open)) {
                if (connection.isStateDeleteHoldDown()) {
                    connection.setReconciliationTimer();
                }
                // The SXP-mode, if not configured explicitly within the device,
                // is set to the opposite value of the one received in the OPEN
                // message.
                setNodeIdRemote(connection);
                if (!checkModeMismatch(connection, legacyOpenMsg, ctx)) {
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
            } else if (legacyOpenMsg.getType().equals(MessageType.OpenResp)) {
                // Verifies that the SXP version is compatible with its SXP. If
                // not then send error and close connection.
                if (!connection.getVersion().equals(legacyOpenMsg.getVersion())) {
                    throw new ErrorMessageException(ErrorCodeNonExtended.VersionMismatch,
                            new IncompatiblePeerVersionException(connection.getVersion(), legacyOpenMsg.getVersion()));
                }
                setNodeIdRemote(connection);
                if (!checkModeMismatch(connection, legacyOpenMsg, ctx)) {
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
            if (!connection.isStateOn(SxpConnection.ChannelHandlerContextType.LISTENER_CNTXT)) {
                throw new UpdateMessageConnectionStateException(connection.getState());
            }
            connection.setUpdateOrKeepaliveMessageTimestamp();
            connection.processUpdateMessage((UpdateMessageLegacy) message);
            return;
        } else if (message instanceof ErrorMessage) {
            throw new ErrorMessageReceivedException(((ErrorMessage) message).getInformation());
        } else if (message instanceof PurgeAllMessage) {
            connection.getOwner().getSvcBindingHandler().processPurgeAllMessage(connection);
            return;
        }
        LOG.warn("{} Cannot handle message, ignoring: {}", connection, MessageFactory.toString(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Notification onParseInput(ByteBuf request) throws ErrorMessageException {
        try {
            return MessageFactory.parse(sxpVersion, request);
        } catch (AttributeVariantException | UnknownNodeIdException | UnknownSxpMessageTypeException | AddressLengthException | AttributeLengthException | UnknownHostException | TlvNotFoundException | UnknownPrefixException e) {
            throw new ErrorMessageException(ErrorCodeNonExtended.MessageParseError, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends SxpBindingFields> ByteBuf onUpdateMessage(SxpConnection connection, List<T> deleteBindings,
            List<T> addBindings, SxpBindingFilter bindingFilter) throws UpdateMessageCompositionException {
        // Compose new messages according to all|changed bindings and version.
        return LegacyMessageFactory.createUpdate(deleteBindings, addBindings, connection.getVersion(), bindingFilter);
    }
}
