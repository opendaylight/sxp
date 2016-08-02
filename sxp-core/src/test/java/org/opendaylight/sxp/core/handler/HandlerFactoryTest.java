/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.handler;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;

public class HandlerFactoryTest {

    private ChannelInboundHandler getDecoder() {
        return mock(ChannelInboundHandler.class);
    }

    private ChannelOutboundHandler getEncoder() {
        return mock(ChannelOutboundHandler.class);
    }

    @Test public void testAddDecoder() throws Exception {
        ChannelInboundHandler decoder = getDecoder();
        HandlerFactory handler = HandlerFactory.instanceAddDecoder(decoder, HandlerFactory.Position.End);

        assertNotNull(handler);
        assertEquals(2, handler.getDecoders().length);
        assertNotEquals(decoder, handler.getDecoders()[0]);
        assertEquals(decoder, handler.getDecoders()[1]);

        decoder = getDecoder();
        handler.addDecoder(decoder, HandlerFactory.Position.Begin);
        assertEquals(3, handler.getDecoders().length);
        assertNotEquals(decoder, handler.getDecoders()[0]);
        assertEquals(decoder, handler.getDecoders()[1]);
        assertNotEquals(decoder, handler.getDecoders()[2]);
    }

    @Test public void testAddEncoder() throws Exception {
        ChannelOutboundHandler encoder = getEncoder();
        HandlerFactory handler = HandlerFactory.instanceAddEncoder(encoder, HandlerFactory.Position.End);

        assertNotNull(handler);
        assertEquals(2, handler.getEncoders().length);
        assertNotEquals(encoder, handler.getEncoders()[0]);
        assertEquals(encoder, handler.getEncoders()[1]);

        encoder = getEncoder();
        handler.addEncoder(encoder, HandlerFactory.Position.Begin);
        assertEquals(3, handler.getEncoders().length);
        assertNotEquals(encoder, handler.getEncoders()[0]);
        assertEquals(encoder, handler.getEncoders()[1]);
        assertNotEquals(encoder, handler.getEncoders()[2]);
    }
}
