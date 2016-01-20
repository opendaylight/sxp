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

/**
 * HandlerFactory class represent unification for decoders and encoders used
 */
public final class HandlerFactory {

    protected final ChannelInboundHandler decoder;

    protected final ChannelOutboundHandler encoder;

    /**
     * Constructor that set decoder, along with default LengthFieldBasedFrameDecoderImpl
     * decoder and ByteArrayEncoder
     *
     * @param decoder ChannelInboundHandler custom decoder
     */
    public HandlerFactory(ChannelInboundHandler decoder) {
        this(null, decoder);
    }

    /**
     * Constructor that set custom encoder and decoder,
     * along with default LengthFieldBasedFrameDecoderImpl
     * decoder and ByteArrayEncoder
     *
     * @param encoder ChannelOutboundHandler custom encoder
     * @param decoder ChannelInboundHandler custom decoder
     */
    public HandlerFactory(ChannelOutboundHandler encoder, ChannelInboundHandler decoder) {
        super();
        this.encoder = encoder;
        this.decoder = decoder;
    }

    /**
     * @return Gets all decoders
     */
    public ChannelHandler[] getDecoders() {
        //return new ChannelHandler[] { new LengthFieldBasedFrameDecoderImpl(), decoder };
        return new ChannelHandler[] { decoder };
    }

    /**
     * @return Gets all encoders
     */
    public ChannelHandler[] getEncoders() {
        //return new ChannelHandler[] { new ByteArrayEncoder(), encoder };
        return new ChannelHandler[] { encoder };
    }
}
