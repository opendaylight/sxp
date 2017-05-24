/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.handler;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.opendaylight.sxp.core.Configuration;

/**
 * LengthFieldBasedFrameDecoderImpl class is wrapper for LengthFieldBasedFrameDecoder that sets it with preset values
 */
public final class LengthFieldBasedFrameDecoderImpl extends LengthFieldBasedFrameDecoder {

    private static final int lengthAdjustment = -Configuration.getConstants().getMessageHeaderLengthLength();

    private static final int lengthFieldLength = Configuration.getConstants().getMessageHeaderLengthLength();

    private static final int maxFrameLength = Configuration.getConstants().getMessageLengthMax();

    /**
     * Constructor that sets LengthFieldBasedFrameDecoder
     * with default settings
     */
    public LengthFieldBasedFrameDecoderImpl() {
        super(maxFrameLength, 0, lengthFieldLength, lengthAdjustment, 0, true);
    }
}
