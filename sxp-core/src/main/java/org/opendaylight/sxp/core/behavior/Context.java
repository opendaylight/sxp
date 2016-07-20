/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.behavior;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.handler.MessageDecoder;
import org.opendaylight.sxp.util.exception.ErrorMessageReceivedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageCompositionException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageConnectionStateException;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.Notification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessageLegacyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Context class is used for handling different behaviour in versions
 */
public final class Context {

    protected static final Logger LOG = LoggerFactory.getLogger(Context.class.getName());

    private SxpNode owner;

    private Strategy strategy;

    private Version version;

    /**
     * Default constructor that sets strategy according to provided version
     *
     * @param owner   SxpNode on which is this context executed
     * @param version Version according to which strategy will be set
     * @throws UnknownVersionException If Version isn't supported
     */
    public Context(SxpNode owner, Version version) throws UnknownVersionException {
        this.owner = owner;
        this.version = version;
        this.strategy = StrategyFactory.getStrategy(this, version);
    }

    /**
     * Logic for establishment of Connection
     *
     * @param ctx        ChannelHandlerContext on which is communication
     * @param connection SxpConnection that participate in communication
     */
    public void executeChannelActivationStrategy(ChannelHandlerContext ctx, SxpConnection connection) {
        this.strategy.onChannelActivation(ctx, connection);
    }

    /**
     * Logic for disconnecting of Connection
     *
     * @param ctx        ChannelHandlerContext on which communication will be closed
     * @param connection SxpConnection that participated in communication
     */
    public void executeChannelInactivationStrategy(ChannelHandlerContext ctx, SxpConnection connection) {
        this.strategy.onChannelInactivation(ctx, connection);
    }

    /**
     * Logic for handling of errors
     *
     * @param ctx        ChannelHandlerContext on which is communication
     * @param connection SxpConnection that participate in communication
     */
    public void executeExceptionCaughtStrategy(ChannelHandlerContext ctx, SxpConnection connection) {
        strategy.onException(ctx, connection);
    }

    /**
     * Logic for handling incoming messages, including version negotiation
     *
     * @param ctx        ChannelHandlerContext on which is communication
     * @param connection SxpConnection that participate in communication
     * @param message    Notification that have been received
     * @throws ErrorMessageException                 If error occurs during handling of Notification
     * @throws UpdateMessageConnectionStateException If Update message was received in wrong state
     * @throws ErrorMessageReceivedException         If Peer send error message
     */
    public void executeInputMessageStrategy(ChannelHandlerContext ctx, SxpConnection connection, Notification message)
            throws ErrorMessageReceivedException, ErrorMessageException, UpdateMessageConnectionStateException {
        // Version negotiation detection
        if (isOpenRespMessage(message)) {
            Version negotiatedVersion = extractVersion(message);
            if (isVersionMismatch(negotiatedVersion)) {
                // Update strategy according to version specified is Open Resp from remote
                LOG.info("{} SXP version negotiated to {} from {}", connection, negotiatedVersion, version);
                connection.setBehaviorContexts(negotiatedVersion);
                // Delegate the processing of current message to new context
                connection.getContext().executeInputMessageStrategy(ctx, connection, message);
                return;
            }
        } else if (isOpenMessage(message)) {
            Version messageVersion = extractVersion(message);
            if (!messageVersion.equals(version)) {
                if (connection.getVersion().equals(Version.Version1)) {
                    MessageDecoder.sendErrorMessage(ctx,
                            new ErrorMessageException(ErrorCodeNonExtended.VersionMismatch, null), connection);
                    connection.setStateOff(ctx);
                    //Fix timing issue
                    owner.openConnection(connection);
                    return;
                } else if (messageVersion.compareTo(version) < 0) {
                    // Update strategy according to version specified in Open from remote
                    LOG.info("{} SXP version dropping version down from {} to {}", connection, version,
                            messageVersion);
                    connection.setBehaviorContexts(messageVersion);
                    // Delegate the processing of current message to new context
                    connection.getContext().executeInputMessageStrategy(ctx, connection, message);
                    return;
                } else if (message instanceof OpenMessage) {
                    message = new OpenMessageLegacyBuilder(((OpenMessage) message)).build();
                }
            }
        }
        this.strategy.onInputMessage(ctx, connection, message);
    }

    /**
     * @param remoteVersion Version to be checked
     * @return If isMismatch of Versions
     */
    private boolean isVersionMismatch(final Version remoteVersion) {
        Preconditions.checkState(remoteVersion.compareTo(version) <= 0, "Remote peer sent higher version of SXP in open resp message");
        return remoteVersion != version;
    }

    /**
     * Extract version of Peer from Notification
     *
     * @param message Notification to be checked
     * @return Found Version
     */
    private Version extractVersion(final Notification message) {
        Version remoteVersion;
        if(message instanceof OpenMessage) {
            remoteVersion = ((OpenMessage) message).getVersion();
        } else if (message instanceof OpenMessageLegacy) {
            remoteVersion = ((OpenMessageLegacy) message).getVersion();
        } else {
            throw new IllegalArgumentException("Cannot extract version from message " + message);
        }
        return remoteVersion;
    }

    /**
     * Check if the message is OpenResp in any version
     */
    private boolean isOpenRespMessage(final Notification message) {
        boolean isOpenResp = message instanceof OpenMessage && ((OpenMessage) message).getType() == MessageType.OpenResp;
        isOpenResp |= message instanceof OpenMessageLegacy && ((OpenMessageLegacy) message).getType() == MessageType.OpenResp;
        return isOpenResp;
    }

    /**
     * Check if the message is Open in any version
     */
    private boolean isOpenMessage(final Notification message) {
        boolean isOpen = message instanceof OpenMessage && ((OpenMessage) message).getType() == MessageType.Open;
        isOpen |= message instanceof OpenMessageLegacy && ((OpenMessageLegacy) message).getType() == MessageType.Open;
        return isOpen;
    }

    /**
     * Logic for decoding incoming data
     *
     * @param request ByteBuf containing received data
     * @return Notification with decoded message
     * @throws ErrorMessageException If received data was corrupted or incorrect
     */
    public Notification executeParseInput(ByteBuf request) throws ErrorMessageException {
        return this.strategy.onParseInput(request);
    }

    /**
     * Logic that generate message containing Bindings for export
     *
     * @param connection     SxpConnection that participate in communication
     * @param deleteBindings Bindings that will be deleted
     * @param addBindings    Bindings that will be added
     * @return ByteBuf containing Update message
     * @throws UpdateMessageCompositionException If during generating of message error occurs
     */
    public <T extends SxpBindingFields> ByteBuf executeUpdateMessageStrategy(SxpConnection connection,
            List<T> deleteBindings, List<T> addBindings, SxpBindingFilter bindingFilter)
            throws UpdateMessageCompositionException {
        return this.strategy.onUpdateMessage(connection, deleteBindings, addBindings, bindingFilter);
    }

    /**
     * @return Gets SxpNode on which is this context executed
     */
    public SxpNode getOwner() {
        return owner;
    }

    /**
     * @return Gets Version of current Context
     */
    public Version getVersion() {
        return version;
    }
}
