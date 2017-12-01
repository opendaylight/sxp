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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import java.util.ArrayDeque;
import java.util.Deque;
import org.opendaylight.sxp.core.Constants;

/**
 * HandlerFactory class represent unification for decoders and encoders used
 */
public final class HandlerFactory {

    private final Deque<ChannelInboundHandler> decoders = new ArrayDeque<>();
    private final Deque<ChannelOutboundHandler> encoders = new ArrayDeque<>();

    /**
     * Creates a HandlerFactory instance with a given decoder.
     *
     * @param decoder decoder to add
     * @param pos position
     * @return new HandlerFactory instance
     */
    public static HandlerFactory instanceAddDecoder(ChannelInboundHandler decoder, Position pos) {
        return new HandlerFactory().addDecoder(decoder, pos);
    }

    /**
     * Creates a HandlerFactory instance with a given encoder.
     *
     * @param encoder encoder to add
     * @param pos position
     * @return new HandlerFactory instance
     */
    public static HandlerFactory instanceAddEncoder(ChannelOutboundHandler encoder, Position pos) {
        return new HandlerFactory().addEncoder(encoder, pos);
    }

    public synchronized HandlerFactory addDecoder(ChannelInboundHandler decoder, Position pos) {
        Preconditions.checkNotNull(decoder);
        Preconditions.checkNotNull(pos);
        if (Position.END.equals(pos)) {
            decoders.addLast(decoder);
        } else {
            decoders.addFirst(decoder);
        }
        return this;
    }

    public synchronized HandlerFactory addEncoder(ChannelOutboundHandler encoder, Position pos) {
        Preconditions.checkNotNull(encoder);
        Preconditions.checkNotNull(pos);
        if (Position.END.equals(pos)) {
            encoders.addLast(encoder);
        } else {
            encoders.addFirst(encoder);
        }
        return this;
    }

    /**
     * @return Gets all decoders
     */
    public synchronized ChannelHandler[] getDecoders() {
        decoders.addFirst(new LengthFieldBasedFrameDecoder(
                Constants.MESSAGE_LENGTH_MAX,
                0,
                Constants.MESSAGE_HEADER_LENGTH_LENGTH,
                Constants.MESSAGE_HEADER_LENGTH_LENGTH * (-1),
                0,
                true));
        ChannelHandler[] out = decoders.toArray(new ChannelHandler[decoders.size()]);
        decoders.pollFirst();
        return out;
    }

    /**
     * @return Gets all encoders
     */
    public synchronized ChannelHandler[] getEncoders() {
        encoders.addFirst(new ByteArrayEncoder());
        ChannelHandler[] out = encoders.toArray(new ChannelHandler[encoders.size()]);
        encoders.pollFirst();
        return out;
    }

    public enum Position {BEGIN, END}
}
