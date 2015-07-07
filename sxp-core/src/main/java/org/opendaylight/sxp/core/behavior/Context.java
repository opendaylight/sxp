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
import org.opendaylight.sxp.core.messaging.legacy.LegacyMessageFactory;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.*;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Context {

    protected static final Logger LOG = LoggerFactory.getLogger(Context.class.getName());

    private SxpNode owner;

    private Strategy strategy;

    private Version version;

    public Context(SxpNode owner, Version version) throws UnknownVersionException {
        this.owner = owner;
        this.version = version;
        this.strategy = StrategyFactory.getStrategy(this, version);
    }

    public void executeChannelActivationStrategy(ChannelHandlerContext ctx, SxpConnection connection) throws Exception {
        this.strategy.onChannelActivation(ctx, connection);
    }

    public void executeChannelInactivationStrategy(ChannelHandlerContext ctx, SxpConnection connection)
            throws Exception {
        this.strategy.onChannelInactivation(ctx, connection);
    }

    public void executeExceptionCaughtStrategy(ChannelHandlerContext ctx, SxpConnection connection) {
        strategy.onException(ctx, connection);
    }

    public void executeInputMessageStrategy(ChannelHandlerContext ctx, SxpConnection connection, Notification message)
            throws Exception {
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
            Version negotiatedVersion = extractVersion(message);
            if (isVersionMismatch(negotiatedVersion)) {
                if (connection.getVersion().equals(Version.Version1)) {
                    ByteBuf error = LegacyMessageFactory.createError(ErrorCodeNonExtended.VersionMismatch);
                    LOG.info("{} Sent ERROR {}", toString(), error.toString());
                    ctx.writeAndFlush(error);
                    connection.closeChannelHandlerContext(ctx);
                    connection.getOwner().openConnection(connection);
                } else {
                    // Update strategy according to version specified in Open from remote
                    LOG.info("{} SXP version dropping version down from {} to {}", connection, version,
                            negotiatedVersion);
                    connection.setBehaviorContexts(negotiatedVersion);
                    // Delegate the processing of current message to new context
                    connection.getContext().executeInputMessageStrategy(ctx, connection, message);
                }
                return;
            }
        }
        this.strategy.onInputMessage(ctx, connection, message);
    }

    private boolean isVersionMismatch(final Version remoteVersion) {
        Preconditions.checkState(remoteVersion.compareTo(version) <= 0, "Remote peer sent higher version of SXP in open resp message");
        return remoteVersion != version;
    }

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

    public Notification executeParseInput(ByteBuf request) throws Exception {
        return this.strategy.onParseInput(request);
    }

    public ByteBuf executeUpdateMessageStrategy(SxpConnection connection, MasterDatabase masterDatabase)
            throws Exception {
        return this.strategy.onUpdateMessage(connection, masterDatabase);
    }

    public SxpNode getOwner() {
        return owner;
    }

    public Version getVersion() {
        return version;
    }
}
