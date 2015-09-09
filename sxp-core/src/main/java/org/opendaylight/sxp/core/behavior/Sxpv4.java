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
import org.opendaylight.sxp.core.SxpConnection.ChannelHandlerContextType;
import org.opendaylight.sxp.core.messaging.AttributeList;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.util.exception.ErrorMessageReceivedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageBindingSourceException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageCompositionException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageConnectionStateException;
import org.opendaylight.sxp.util.exception.message.attribute.AddressLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeNotFoundException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeVariantException;
import org.opendaylight.sxp.util.exception.message.attribute.CapabilityLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.HoldTimeMaxException;
import org.opendaylight.sxp.util.exception.message.attribute.HoldTimeMinException;
import org.opendaylight.sxp.util.exception.message.attribute.SecurityGroupTagValueException;
import org.opendaylight.sxp.util.exception.message.attribute.TlvNotFoundException;
import org.opendaylight.sxp.util.exception.unknown.UnknownConnectionModeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownNodeIdException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpMessageTypeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.sxp.util.inet.InetAddressComparator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.PurgeAllMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.HoldTimeAttribute;
import org.opendaylight.yangtools.yang.binding.Notification;

import java.net.UnknownHostException;

/**
 * Sxpv4 class provides logic for handling connection on Version 4
 */
public final class Sxpv4 extends SxpLegacy {

    /**
     * OpenMessageType enum is used to distinguish between messages types
     */
    private enum OpenMessageType {
        Open, OperResp
    }

    /**
     * Default constructor that sets its Context
     *
     * @param context Context to be set
     */
    public Sxpv4(Context context) {
        super(context);
    }

    /**
     * Creates OpenMessage containing HoldTime attribute
     *
     * @param connection      SxpConnection containing setting
     * @param openMessageType Type of message to be generated
     * @return ByteBuf representation of generated message
     * @throws CapabilityLengthException If some Attributes has incorrect length
     * @throws UnknownVersionException   If version isn't supported
     * @throws HoldTimeMaxException      If Max hold time is greater than minimal
     * @throws HoldTimeMinException      If Min hold time isn't in range of <0,65535>
     * @throws AttributeVariantException If attribute variant isn't supported
     */
    private ByteBuf composeOpenHoldTimeMessage(SxpConnection connection, OpenMessageType openMessageType)
            throws CapabilityLengthException, HoldTimeMaxException, AttributeVariantException, HoldTimeMinException,
            UnknownVersionException {
        return composeOpenHoldTimeMessage(connection, openMessageType, connection.getMode());
    }

    /**
     * Creates OpenMessage containing HoldTime attribute
     *
     * @param connection      SxpConnection containing setting
     * @param openMessageType Type of message to be generated
     * @param connectionMode  ConnectionMode for which message will be generated
     * @return ByteBuf representation of generated message
     * @throws CapabilityLengthException If some Attributes has incorrect length
     * @throws UnknownVersionException   If version isn't supported
     * @throws HoldTimeMaxException      If Max hold time is greater than minimal
     * @throws HoldTimeMinException      If Min hold time isn't in range of <0,65535>
     * @throws AttributeVariantException If attribute variant isn't supported
     */
    private ByteBuf composeOpenHoldTimeMessage(SxpConnection connection, OpenMessageType openMessageType,
            ConnectionMode connectionMode)
            throws CapabilityLengthException, UnknownVersionException, HoldTimeMaxException, HoldTimeMinException,
            AttributeVariantException {
        if (connectionMode.equals(ConnectionMode.Listener)) {
            // Per-connection time settings: User (not)defined.
            Integer holdTimeMin = connection.getHoldTimeMin();
            Integer holdTimeMax = connection.getHoldTimeMax();
            // Global time settings: Default values have been pulled during node
            // creation TimeSettings.pullDefaults().
            if (holdTimeMin == null || holdTimeMin == 0 || holdTimeMax == null || holdTimeMax == 0) {
                holdTimeMin = getOwner().getHoldTimeMin();
                holdTimeMax = getOwner().getHoldTimeMax();
            }
            // Local current connection settings.
            if (openMessageType.equals(OpenMessageType.OperResp)) {
                Integer holdTime = connection.getHoldTime();
                if (holdTime != null && holdTime == 0) {
                    holdTimeMin = 0;
                    holdTimeMax = 0;
                }
            }
            // Timers setup: 0 to disable specific timer usability.
            switch (openMessageType) {
            case Open:
                return MessageFactory.createOpen(connection.getVersion(), connectionMode, connection.getOwnerId(),
                        holdTimeMin, holdTimeMax);
            case OperResp:
                return MessageFactory.createOpenResp(connection.getVersion(), connectionMode, connection.getOwnerId(),
                        holdTimeMin, holdTimeMax);
            }

        } else if (connectionMode.equals(ConnectionMode.Speaker)) {
            // Per-connection time settings: User (not)defined.
            Integer holdTimeMinAcc = connection.getHoldTimeMinAcceptable();
            // Global time settings: Default values have been pulled during node
            // creation TimeSettings.pullDefaults().
            if (holdTimeMinAcc == null || holdTimeMinAcc == 0) {
                holdTimeMinAcc = getOwner().getHoldTimeMinAcceptable();
            }
            // Local current connection settings.
            if (openMessageType.equals(OpenMessageType.OperResp)) {
                Integer keepAliveTime = connection.getKeepaliveTime();
                if (keepAliveTime != null && keepAliveTime == 0) {
                    holdTimeMinAcc = 0;
                }
            }

            // Timers setup: 0 to disable specific timer usability.
            switch (openMessageType) {
            case Open:
                return MessageFactory.createOpen(connection.getVersion(), connectionMode, connection.getOwnerId(),
                        holdTimeMinAcc);
            case OperResp:
                return MessageFactory.createOpenResp(connection.getVersion(), connectionMode, connection.getOwnerId(),
                        holdTimeMinAcc);
            }
        }
        throw new UnknownConnectionModeException();
    }

    /**
     * Creates OpenRespMessage containing HoldTime attribute
     *
     * @param connection SxpConnection containing setting
     * @param message    OpenMessage to be parsed for values
     * @return ByteBuf representation of generated message
     * @throws CapabilityLengthException If some Attributes has incorrect length
     * @throws UnknownVersionException   If version isn't supported
     * @throws HoldTimeMaxException      If Max hold time is greater than minimal
     * @throws HoldTimeMinException      If Min hold time isn't in range of <0,65535>
     * @throws AttributeVariantException If attribute variant isn't supported
     */
    private ByteBuf composeOpenRespHoldTimeMessage(SxpConnection connection, OpenMessage message)
            throws CapabilityLengthException, AttributeVariantException, HoldTimeMaxException,
            HoldTimeMinException, UnknownVersionException {
        return composeOpenRespHoldTimeMessage(connection, message, connection.getMode());
    }

    /**
     * Creates OpenRespMessage containing HoldTime attribute
     *
     * @param connection     SxpConnection containing setting
     * @param message        OpenMessage to be parsed for values
     * @param connectionMode ConnectionMode for which message will be generated
     * @return ByteBuf representation of generated message
     * @throws CapabilityLengthException If some Attributes has incorrect length
     * @throws UnknownVersionException   If version isn't supported
     * @throws HoldTimeMaxException      If Max hold time is greater than minimal
     * @throws HoldTimeMinException      If Min hold time isn't in range of <0,65535>
     * @throws AttributeVariantException If attribute variant isn't supported
     */
    private ByteBuf composeOpenRespHoldTimeMessage(SxpConnection connection, OpenMessage message,
            ConnectionMode connectionMode)
            throws CapabilityLengthException, UnknownVersionException, AttributeVariantException,
            HoldTimeMaxException, HoldTimeMinException {
        // If valid HoldTimeAttribute is received, HoldTimeAttribute must be
        // included.
        HoldTimeAttribute attHoldTime;
        try {
            attHoldTime = (HoldTimeAttribute) AttributeList.get(message.getAttribute(), AttributeType.HoldTime);
        } catch (AttributeNotFoundException e) {
            return MessageFactory.createOpenResp(connection.getVersion(), connectionMode, connection.getOwnerId());
        }
        if (connectionMode.equals(ConnectionMode.Listener)) {
            int holdTimeMinAcc = attHoldTime.getHoldTimeAttributes().getHoldTimeMinValue();
            if (holdTimeMinAcc == 0) {
                return MessageFactory.createOpenResp(connection.getVersion(), connectionMode, connection.getOwnerId());
            }
            return composeOpenHoldTimeMessage(connection, OpenMessageType.OperResp, connectionMode);

        } else if (connectionMode.equals(ConnectionMode.Speaker)) {
            int holdTimeMin = attHoldTime.getHoldTimeAttributes().getHoldTimeMinValue();
            int holdTimeMax = attHoldTime.getHoldTimeAttributes().getHoldTimeMaxValue();
            if (holdTimeMin == 0 || holdTimeMax == 0 || holdTimeMin >= holdTimeMax) {
                return MessageFactory.createOpenResp(connection.getVersion(), connectionMode, connection.getOwnerId());
            }
            return composeOpenHoldTimeMessage(connection, OpenMessageType.OperResp, connectionMode);

        }
        throw new UnknownConnectionModeException();
    }

    @Override
    public void onChannelActivation(final ChannelHandlerContext ctx, SxpConnection connection) {
        ByteBuf message;
        try {
            if (connection.isModeBoth() && !connection.isBidirectionalBoth()) {
                message = composeOpenHoldTimeMessage(connection, OpenMessageType.Open, ConnectionMode.Listener);
            } else {
                message = composeOpenHoldTimeMessage(connection, OpenMessageType.Open);
            }
        } catch (CapabilityLengthException | UnknownVersionException | HoldTimeMinException | AttributeVariantException | UnknownConnectionModeException | HoldTimeMaxException e) {
            LOG.error("{} Error sending OpenMessage due to creation error ", this, e);
            return;
        }
        LOG.info("{} Sent OPEN {}", connection, MessageFactory.toString(message));
        ctx.writeAndFlush(message);
        connection.setStatePendingOn();
    }

    @Override
    public void onInputMessage(ChannelHandlerContext ctx, SxpConnection connection, Notification message)
            throws ErrorMessageReceivedException, ErrorMessageException, UpdateMessageConnectionStateException {
        LOG.info("{} Handle {}", connection, MessageFactory.toString(message));

        if (message instanceof OpenMessage) {
            OpenMessage _message = (OpenMessage) message;
            if (_message.getType().equals(MessageType.Open)) {

                if (connection.isModeBoth()) {
                    /**
                     * SXPv4 on routers is lightly different in using
                     * bidirectional functionality, e.g. after both commands set
                     * "no cts sxp enable" and "cts sxp enable"<br>
                     * ASR acts<br>
                     * 1. sent OPEN message as a Listener<br>
                     * 2. sent OPEN message as a Speaker<br>
                     * 3. after that after few seconds only one OPEN message as
                     * a Listener is sent and thats it
                     * <p>
                     * ISR acts<br>
                     * 1. sent OPEN message as a Speaker<br>
                     * 2. every few seconds an OPEN message as a Speaker is sent
                     * <p>
                     * If an OPEN message as a Speaker is sent to a router,
                     * router responds with PURGEALL.
                     */

                    if (_message.getSxpMode().equals(ConnectionMode.Listener)) {
                        if (!connection.isBidirectionalBoth()) {
                            connection.markChannelHandlerContext(ctx, ChannelHandlerContextType.SpeakerContext);
                            connection.setConnectionSpeakerPart(_message);
                            try {
                                ByteBuf response = composeOpenRespHoldTimeMessage(connection, _message, ConnectionMode.Speaker);
                                LOG.info("{} Sent RESP {}", connection, MessageFactory.toString(response));
                                ctx.writeAndFlush(response);
                            } catch (CapabilityLengthException | HoldTimeMinException | HoldTimeMaxException | AttributeVariantException e) {
                                LOG.error("{} Error sending RESP shutting down connection {} ", this, connection, e);
                                connection.setStateOff(ctx);
                                return;
                            }
                        }

                    } else if (_message.getSxpMode().equals(ConnectionMode.Speaker)) {
                        ByteBuf response = MessageFactory.createPurgeAll();
                        LOG.info("{} Sent PURGEALL {}", connection, MessageFactory.toString(response));
                        ctx.writeAndFlush(response);
                        connection.setStateOff(ctx);
                    }

                    if (connection.isBidirectionalBoth()) {
                        // Setup connection parameters.
                        connection.setConnection(_message);
                        LOG.info("{} Connected", connection);
                    }
                    return;
                }

                /**
                 * SXPv4 I-D discrepancy: If both ends of an SXP connection set
                 * up the TCP connection at the same time, the end with source
                 * IP address higher than the peer IP address wins: i.e. the TCP
                 * connection initiated from that end is kept and the other TCP
                 * connection is torn down.
                 * <p>
                 * Cisco device's implementation: Only the Speaker includes
                 * NODE_ID in OPEN message.
                 */
                if (InetAddressComparator.greaterThan(connection.getDestination().getAddress(),
                        connection.getLocalAddress().getAddress())) {
                    // Close the dual channel.
                    connection.closeChannelHandlerContextComplements(ctx);

                } else if (connection.isStateDeleteHoldDown()) {
                    // Replace the existing one.
                    connection.closeChannelHandlerContextComplements(ctx);
                } else if (connection.isStatePendingOn()) {
                    // Close the current channel.
                    connection.closeChannelHandlerContext(ctx);
                    return;
                }
                connection.markChannelHandlerContext(ctx);
                // Setup connection parameters.
                connection.setConnection(_message);

                // Send a response.
                try {
                    ByteBuf response = composeOpenRespHoldTimeMessage(connection, _message);
                    LOG.info("{} Sent RESP {}", connection, MessageFactory.toString(response));
                    ctx.writeAndFlush(response);
                } catch (CapabilityLengthException | HoldTimeMinException | HoldTimeMaxException | AttributeVariantException e) {
                    LOG.error("{} Error sending RESP shutting down connection {} ", this, connection, e);
                    connection.setStateOff();
                }
                return;

            } else if (_message.getType().equals(MessageType.OpenResp)) {
                if (connection.isModeBoth()) {
                    if (_message.getSxpMode().equals(ConnectionMode.Listener)) {
                        if (!connection.isBidirectionalBoth()) {
                            connection.markChannelHandlerContext(ctx, ChannelHandlerContextType.SpeakerContext);
                            connection.setConnectionSpeakerPart(_message);
                        }

                    } else if (_message.getSxpMode().equals(ConnectionMode.Speaker)) {
                        if (!connection.isBidirectionalBoth()) {
                            connection.markChannelHandlerContext(ctx, ChannelHandlerContextType.ListenerContext);
                            connection.setConnectionListenerPart(_message);
                        }
                    }

                    if (connection.isBidirectionalBoth()) {
                        // Setup connection parameters.
                        connection.setConnection(_message);
                        LOG.info("{} Connected", connection);
                    }
                    return;
                }
                if (connection.isStateDeleteHoldDown()) {
                    // Replace the existing one.
                    connection.closeChannelHandlerContextComplements(ctx);
                } else if (connection.isStateOn()) {
                    // Close the current channel.
                    connection.closeChannelHandlerContext(ctx);
                    return;
                }
                connection.markChannelHandlerContext(ctx);
                // Setup connection parameters.
                connection.setConnection(_message);
                LOG.info("{} Connected", connection);
                return;
            }

        } else if (message instanceof UpdateMessage) {
            // Accepted only if connection is in ON state.
            if (!connection.isStateOn(SxpConnection.ChannelHandlerContextType.ListenerContext)) {
                throw new UpdateMessageConnectionStateException(connection.getState());
            }
            connection.setUpdateOrKeepaliveMessageTimestamp();
            connection.getContext().getOwner().processUpdateMessage((UpdateMessage) message, connection);
            return;

        } else if (message instanceof ErrorMessage) {
            throw new ErrorMessageReceivedException(((ErrorMessage) message).getInformation());

        } else if (message instanceof PurgeAllMessage) {
            // Remove all bindings received from the speaker
            // counter-part (no delete hold-down timer).
            LOG.info("{} PURGEALL processing", connection);

            // Get message relevant peer node ID.
            NodeId peerId;
            peerId = connection.getNodeIdRemote();
            connection.setPurgeAllMessageReceived();
            connection.getContext().getOwner().purgeBindings(peerId);
            connection.getContext().getOwner().notifyService();
            return;

        } else if (message instanceof KeepaliveMessage) {
            connection.setUpdateOrKeepaliveMessageTimestamp();
            return;
        }

        LOG.warn("{} Cannot handle message, ignoring: {}", connection, MessageFactory.toString(message));
    }

    @Override
    public Notification onParseInput(ByteBuf request) throws ErrorMessageException {
        try {
            return MessageFactory.parse(Version.Version4, request);
        } catch (UnknownSxpMessageTypeException | AddressLengthException | UnknownHostException | AttributeLengthException | TlvNotFoundException | UnknownNodeIdException | UnknownPrefixException e) {
            throw new ErrorMessageException(ErrorCodeNonExtended.MessageParseError, e);
        }
    }

    @Override
    public ByteBuf onUpdateMessage(SxpConnection connection, MasterDatabase masterDatabase)
            throws UpdateMessageCompositionException {
        // Supports: New implementations of IPv4 Binding, IPv6 Bindings, Subnet
        // Bindings Expansion.
        // Compose new messages according to all|changed bindings and version.
        try {
            return MessageFactory
                    .createUpdate(masterDatabase, getOwner().getNodeId(), connection.isUpdateAllExported());
        } catch (UpdateMessageBindingSourceException | SecurityGroupTagValueException | AttributeVariantException e) {
            throw new UpdateMessageCompositionException(connection.getVersion(), connection.isUpdateAllExported(), e);
        }
    }

}
