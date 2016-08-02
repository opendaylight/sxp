/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.handler;

import com.google.common.base.Preconditions;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import java.util.Collections;

/**
 * HandlerFactory class represent unification for decoders and encoders used
 */
public final class HandlerFactory {

    protected final ChannelInboundHandler[] decoder;
    protected final ChannelOutboundHandler[] encoder;

    /**
     * Constructor that set decoder, along with default LengthFieldBasedFrameDecoderImpl
     * decoder and ByteArrayEncoder
     *
     * @param decoder ChannelInboundHandler custom decoder
     */
    public HandlerFactory(ChannelInboundHandler decoder) {
        this(new ChannelOutboundHandler[0], Collections.singletonList(decoder).toArray(new ChannelInboundHandler[1]));
    }

    public HandlerFactory(ChannelInboundHandler[] decoders) {
        this(new ChannelOutboundHandler[0], decoders);
    }

    /**
     * Constructor that set custom encoder and decoder,
     * along with default LengthFieldBasedFrameDecoderImpl
     * decoder and ByteArrayEncoder
     *
     * @param encoder ChannelOutboundHandler custom encoder
     * @param decoder ChannelInboundHandler custom decoder
     */
    public HandlerFactory(ChannelOutboundHandler[] encoder, ChannelInboundHandler[] decoder) {
        this.encoder = Preconditions.checkNotNull(encoder);
        this.decoder = Preconditions.checkNotNull(decoder);
    }

    /**
     * @return Gets all decoders
     */
    public ChannelHandler[] getDecoders() {
        ChannelHandler[] decoders = new ChannelHandler[decoder.length + 1];
        decoders[0] = new LengthFieldBasedFrameDecoderImpl();
        System.arraycopy(decoder, 0, decoders, 1, decoder.length);
        return decoders;
    }

    /**
     * @return Gets all encoders
     */
    public ChannelHandler[] getEncoders() {
        ChannelHandler[] encoders = new ChannelHandler[encoder.length + 1];
        encoders[0] = new ByteArrayEncoder();
        System.arraycopy(encoder, 0, encoders, 1, encoder.length);
        return encoders;
    }
}
