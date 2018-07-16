package org.opendaylight.sxp.util.netty;

import com.google.common.net.InetAddresses;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class InetAddressExtractor {

    private InetAddressExtractor() {
    }

    public static InetAddress getRemoteInetAddressFrom(ChannelHandlerContext ctx) {
        String remoteAddrString = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        return InetAddresses.forString(remoteAddrString);
    }

    public static InetAddress getLocalInetAddressFrom(ChannelHandlerContext ctx) {
        String localAddrString = ((InetSocketAddress) ctx.channel().localAddress()).getAddress().getHostAddress();
        return InetAddresses.forString(localAddrString);
    }
}
