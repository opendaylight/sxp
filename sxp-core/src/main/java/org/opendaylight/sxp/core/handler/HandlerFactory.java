/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

public final class HandlerFactory {

    protected final ChannelInboundHandler decoder;

    protected final ChannelOutboundHandler encoder;

    public HandlerFactory(ChannelInboundHandler decoder) {
        this(null, decoder);
    }

    public HandlerFactory(ChannelOutboundHandler encoder, ChannelInboundHandler decoder) {
        super();
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public ChannelHandler[] getDecoders() {
        return new ChannelHandler[] { new LengthFieldBasedFrameDecoderImpl(), decoder };
    }

    public ChannelHandler[] getEncoders() {
        return new ChannelHandler[] { new ByteArrayEncoder(), encoder };
    }
}
