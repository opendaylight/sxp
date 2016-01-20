/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.core.handler.LengthFieldBasedFrameDecoderImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.PasswordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ConnectFacade {

    private static EventLoopGroup eventLoopGroup = null;
    protected static final Logger LOG = LoggerFactory.getLogger(ConnectFacade.class.getName());

    private static final Function<SxpConnection, InetAddress>
            CONNECTION_ENTRY_TO_INET_ADDR =
            new Function<SxpConnection, InetAddress>() {

                @Override public InetAddress apply(final SxpConnection input) {
                    return input.getDestination().getAddress();
                }
            };
    private static final Predicate<SxpConnection> CONNECTION_ENTRY_WITH_PASSWORD = new Predicate<SxpConnection>() {

        @Override public boolean apply(final SxpConnection input) {
            return input.getPasswordType() == PasswordType.Default;
        }
    };

    /**
     * Create new Connection to Peer
     *
     * @param node       SxpNode containing Security options
     * @param connection SxpConnection containing connection details
     * @param hf         HandlerFactory providing handling of communication
     * @return ChannelFuture callback
     */
    public static ChannelFuture createClient(SxpNode node, SxpConnection connection, final HandlerFactory hf) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(EpollSocketChannel.class);

        if (connection.getPasswordType().equals(PasswordType.Default) && node.getPassword() != null
                && !node.getPassword().isEmpty()) {
            Map<InetAddress, byte[]> keys = new HashMap<>();
            keys.put(connection.getDestination().getAddress(), node.getPassword().getBytes(Charsets.US_ASCII));
            bootstrap.option(EpollChannelOption.TCP_MD5SIG, keys);
        }
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Configuration.NETTY_CONNECT_TIMEOUT_MILLIS);
        RecvByteBufAllocator recvByteBufAllocator = new FixedRecvByteBufAllocator(Configuration.getConstants()
                .getMessageLengthMax());
        bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, recvByteBufAllocator);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.localAddress(node.getSourceIp().getHostAddress(), 0);
        if (eventLoopGroup == null) {
            eventLoopGroup = new EpollEventLoopGroup();
        }
        bootstrap.group(eventLoopGroup);

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoderImpl());
                ch.pipeline().addLast(hf.getDecoders());
                ch.pipeline().addLast("arrayEncoder", new ByteArrayEncoder());
                ch.pipeline().addLast(hf.getEncoders());
            }
        });
        return bootstrap.connect(connection.getDestination());
    }

    /**
     * Create new Node that listens to incoming connections
     *
     * @param node SxpNode containing options
     * @param hf   HandlerFactory providing handling of communication
     * @return ChannelFuture callback
     */
    public static ChannelFuture createServer(final SxpNode node,final HandlerFactory hf) {
        final EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
        if (eventLoopGroup == null) {
            eventLoopGroup = new EpollEventLoopGroup();
        }

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.channel(EpollServerSocketChannel.class);
        if (node.getPassword() != null && !node.getPassword().isEmpty()) {
            Map<InetAddress, byte[]> keyMapping = new HashMap<>();
            // Every peer has to be configured with password separately
            // Right now the configuration allows only for a single password to be shared by all peers with password set to Default
            final Collection<InetAddress>
                    connectionsWithPassword =
                    Collections2.transform(
                            Collections2.filter(node.getAllConnections(), CONNECTION_ENTRY_WITH_PASSWORD),
                            CONNECTION_ENTRY_TO_INET_ADDR);
            for (InetAddress inetAddress : connectionsWithPassword) {
                keyMapping.put(inetAddress, node.getPassword().getBytes(Charsets.US_ASCII));
            }
            bootstrap.option(EpollChannelOption.TCP_MD5SIG, keyMapping);
        }
        bootstrap.group(bossGroup, eventLoopGroup);

        ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {

            @Override protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoderImpl());
                ch.pipeline().addLast(hf.getDecoders());
                ch.pipeline().addLast("arrayEncoder", new ByteArrayEncoder());
                ch.pipeline().addLast(hf.getEncoders());
            }
        };

        if (Configuration.NETTY_LOGGER_HANDLER) {
            bootstrap.handler(new LoggingHandler(LogLevel.INFO)).childHandler(channelInitializer);
        } else {
            bootstrap.childHandler(channelInitializer);
        }
        ChannelFuture channelFuture = bootstrap.bind(node.getSourceIp(), node.getServerPort());
        channelFuture.channel().closeFuture().addListener(new ChannelFutureListener() {

            @Override public void operationComplete(ChannelFuture future) throws Exception {
                bossGroup.shutdownGracefully();
            }
        });
        return channelFuture;
    }
}
