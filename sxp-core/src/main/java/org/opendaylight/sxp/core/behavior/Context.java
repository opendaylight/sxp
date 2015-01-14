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
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.Notification;

public final class Context {
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
        this.strategy.onInputMessage(ctx, connection, message);
    }

    public Notification executeParseInput(ByteBuf request) throws Exception {
        return this.strategy.onParseInput(request);
    }

    public ByteBuf executeUpdateMessageStrategy(ChannelHandlerContext ctx, SxpConnection connection,
            MasterDatabase masterDatabase) throws Exception {
        return this.strategy.onUpdateMessage(ctx, connection, masterDatabase);
    }

    public SxpNode getOwner() {
        return owner;
    }

    public Version getVersion() {
        return version;
    }
}
