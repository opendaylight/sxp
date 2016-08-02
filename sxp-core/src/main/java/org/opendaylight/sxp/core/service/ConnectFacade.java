/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectFacade {

    private static EventLoopGroup eventLoopGroup = new EpollEventLoopGroup();
    protected static final Logger LOG = LoggerFactory.getLogger(ConnectFacade.class.getName());

    private static final Predicate<SxpConnection>
            CONNECTION_ENTRY_WITH_PASSWORD =
            input -> input.getPassword() != null && !input.getPassword().isEmpty();

    /**
     * Create new Connection to Peer
     *
     * @param node       SxpNode containing Security options
     * @param connection SxpConnection containing connection details
     * @param hf         HandlerFactory providing handling of communication
     * @return ChannelFuture callback
     */
    public static ChannelFuture createClient(SxpNode node, SxpConnection connection, final HandlerFactory hf) {
        if (!Epoll.isAvailable()) {
            throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
        }
        Bootstrap bootstrap = new Bootstrap();
        if (connection.getPassword() != null && !connection.getPassword().isEmpty()) {
            bootstrap.option(EpollChannelOption.TCP_MD5SIG,
                    Collections.singletonMap(connection.getDestination().getAddress(),
                            connection.getPassword().getBytes(StandardCharsets.US_ASCII)));
        }
        bootstrap.channel(EpollSocketChannel.class);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Configuration.NETTY_CONNECT_TIMEOUT_MILLIS);
        RecvByteBufAllocator
                recvByteBufAllocator =
                new FixedRecvByteBufAllocator(Configuration.getConstants().getMessageLengthMax());
        bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, recvByteBufAllocator);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.localAddress(node.getSourceIp().getHostAddress(), 0);
        bootstrap.group(eventLoopGroup);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(hf.getDecoders());
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
    public static ChannelFuture createServer(final SxpNode node, final HandlerFactory hf) {
        if (!Epoll.isAvailable()) {
            throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
        }
        final EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
        Map<InetAddress, byte[]> keyMapping = new HashMap<>();
        ServerBootstrap bootstrap = new ServerBootstrap();
        Collections2.filter(node.getAllConnections(), CONNECTION_ENTRY_WITH_PASSWORD)
                .forEach(p -> keyMapping.put(p.getDestination().getAddress(),
                        p.getPassword().getBytes(StandardCharsets.US_ASCII)));
        bootstrap.channel(EpollServerSocketChannel.class);
        bootstrap.option(EpollChannelOption.TCP_MD5SIG, keyMapping);
        bootstrap.group(bossGroup, eventLoopGroup);
        if (Configuration.NETTY_LOGGER_HANDLER) {
            bootstrap.handler(new LoggingHandler(LogLevel.INFO));
        }
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(hf.getDecoders());
                ch.pipeline().addLast(hf.getEncoders());
            }
        });
        ChannelFuture channelFuture = bootstrap.bind(node.getSourceIp(), node.getServerPort());
        channelFuture.channel().closeFuture().addListener(future -> bossGroup.shutdownGracefully());
        return channelFuture;
    }
}
