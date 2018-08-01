/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.netty;

import com.google.common.net.InetAddresses;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class InetAddressExtractor {

    private InetAddressExtractor() {
    }

    /**
     * Get remote address of the connection.
     *
     * @param ctx Connection channel handler
     * @return remote address of the connection
     */
    public static InetAddress getRemoteInetAddressFrom(ChannelHandlerContext ctx) {
        String remoteAddrString = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        return InetAddresses.forString(remoteAddrString);
    }

    /**
     * Get local address of the connection.
     *
     * @param ctx Connection channel handler
     * @return local address of the connection
     */
    public static InetAddress getLocalInetAddressFrom(ChannelHandlerContext ctx) {
        String localAddrString = ((InetSocketAddress) ctx.channel().localAddress()).getAddress().getHostAddress();
        return InetAddresses.forString(localAddrString);
    }
}
