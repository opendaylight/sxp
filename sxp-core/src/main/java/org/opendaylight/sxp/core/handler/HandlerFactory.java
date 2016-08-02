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
import java.util.Arrays;

/**
 * HandlerFactory class represent unification for decoders and encoders used
 */
public final class HandlerFactory {

    public enum Position {Begin, End}


    protected ChannelInboundHandler[] decoders;
    protected ChannelOutboundHandler[] encoders;

    public HandlerFactory() {
        decoders = new ChannelInboundHandler[1];
        encoders = new ChannelOutboundHandler[1];
    }

    public static HandlerFactory instanceAddDecoder(ChannelInboundHandler decoder, Position pos) {
        return new HandlerFactory().addDecoder(decoder, pos);
    }

    public static HandlerFactory instanceAddEncoder(ChannelOutboundHandler encoder, Position pos) {
        return new HandlerFactory().addEncoder(encoder, pos);
    }

    public synchronized HandlerFactory addDecoder(ChannelInboundHandler decoder, Position pos) {
        Preconditions.checkNotNull(decoder);
        Preconditions.checkNotNull(pos);
        if (Position.End.equals(pos)) {
            decoders = Arrays.copyOf(decoders, decoders.length + 1);
            decoders[decoders.length - 1] = decoder;
        } else {
            ChannelInboundHandler[] decoders_ = decoders;
            decoders = new ChannelInboundHandler[decoders.length + 1];
            System.arraycopy(decoders_, 0, decoders, 1, decoders_.length);
            decoders[1] = decoder;
        }
        return this;
    }

    public synchronized HandlerFactory addEncoder(ChannelOutboundHandler encoder, Position pos) {
        Preconditions.checkNotNull(encoder);
        Preconditions.checkNotNull(pos);
        if (Position.End.equals(pos)) {
            encoders = Arrays.copyOf(encoders, encoders.length + 1);
            encoders[encoders.length - 1] = encoder;
        } else {
            ChannelOutboundHandler[] encoders_ = encoders;
            encoders = new ChannelOutboundHandler[encoders.length + 1];
            System.arraycopy(encoders_, 0, encoders, 1, encoders_.length);
            encoders[1] = encoder;
        }
        return this;
    }

    /**
     * @return Gets all decoders
     */
    public synchronized ChannelHandler[] getDecoders() {
        decoders[0] = new LengthFieldBasedFrameDecoderImpl();
        return decoders;
    }

    /**
     * @return Gets all encoders
     */
    public synchronized ChannelHandler[] getEncoders() {
        encoders[0] = new ByteArrayEncoder();
        return encoders;
    }
}
