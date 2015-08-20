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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.tcpmd5.api.KeyAccessFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.jni.NativeKeyAccessFactory;
import org.opendaylight.tcpmd5.jni.NativeSupportUnavailableException;
import org.opendaylight.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.tcpmd5.netty.MD5NioServerSocketChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5NioSocketChannelFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.PasswordType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConnectFacade {

    private static EventLoopGroup eventLoopGroup = null;
    protected static final Logger LOG = LoggerFactory.getLogger(ConnectFacade.class.getName());

    private static final Function<Map.Entry<InetSocketAddress, SxpConnection>, InetAddress> CONNECTION_ENTRY_TO_INET_ADDR = new Function<Map.Entry<InetSocketAddress, SxpConnection>, InetAddress>() {
        @Override
        public InetAddress apply(final Map.Entry<InetSocketAddress, SxpConnection> input) {
            return input.getKey().getAddress();
        }
    };
    private static final Predicate<Map.Entry<InetSocketAddress, SxpConnection>> CONNECTION_ENTRY_WITH_PASSWORD = new Predicate<Map.Entry<InetSocketAddress, SxpConnection>>() {
        @Override
        public boolean apply(final Map.Entry<InetSocketAddress, SxpConnection> input) {
            return input.getValue().getPasswordType() == PasswordType.Default;
        }
    };

    /**
     * Create new Connection to Peer
     *
     * @param node       SxpNode containing Security options
     * @param connection SxpConnection containing connection details
     * @param hf         HandlerFactory providing handling of communication
     * @return ChannelFuture callback
     * @throws NativeSupportUnavailableException If Security error occurs
     */
    public static ChannelFuture createClient(SxpNode node, SxpConnection connection, final HandlerFactory hf)
            throws NativeSupportUnavailableException {
        Bootstrap bootstrap = new Bootstrap();

        if (connection.getPasswordType().equals(PasswordType.Default) && node.getPassword() != null
                && !node.getPassword().isEmpty()) {
            bootstrap = customizeClientBootstrap(bootstrap, connection.getDestination().getAddress(),
                    node.getPassword());
        }
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Configuration.NETTY_CONNECT_TIMEOUT_MILLIS);
        RecvByteBufAllocator recvByteBufAllocator = new FixedRecvByteBufAllocator(Configuration.getConstants()
                .getMessageLengthMax());
        bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, recvByteBufAllocator);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);

        if (eventLoopGroup == null) {
            eventLoopGroup = new NioEventLoopGroup();
        }
        try {
            bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class);
        } catch (final IllegalStateException e) {
            LOG.info("Not overriding channelFactory on bootstrap {} | {}", bootstrap, e.getMessage());
        }

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                if(Configuration.NETTY_HANDLER_TIMEOUT_MILLIS >0) {
                    ch.pipeline().addLast(new ReadTimeoutHandler(Configuration.NETTY_HANDLER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
                }
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
    public static ChannelFuture createServer(final SxpNode node,final HandlerFactory hf) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        if (eventLoopGroup == null) {
            eventLoopGroup = new NioEventLoopGroup();
        }

        ServerBootstrap bootstrap = new ServerBootstrap();
        if (node.getPassword() != null && !node.getPassword().isEmpty()) {
            try {
                final Collection<InetAddress>
                        connectionsWithPassword =
                        Collections2.transform(Collections2.filter(node.entrySet(), CONNECTION_ENTRY_WITH_PASSWORD),
                                CONNECTION_ENTRY_TO_INET_ADDR);
                bootstrap = customizeServerBootstrap(bootstrap, connectionsWithPassword, node.getPassword());
            } catch (NativeSupportUnavailableException e) {
                LOG.error(node + "Could not customize bootstrap " + e);
            }
        }

        try {
            bootstrap.group(bossGroup, eventLoopGroup).channel(NioServerSocketChannel.class);
        } catch (final IllegalStateException e) {
            LOG.info("Not overriding channelFactory on bootstrap {}", bootstrap, e);
        }

        ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {

            @Override protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(hf.getDecoders());
                ch.pipeline().addLast(hf.getEncoders());
            }
        };

        if (Configuration.NETTY_LOGGER_HANDLER) {
            bootstrap.handler(new LoggingHandler(LogLevel.INFO)).childHandler(channelInitializer);
        } else {
            bootstrap.childHandler(channelInitializer);
        }
        return bootstrap.bind(node.getSourceIp(), node.getServerPort());
    }

    /**
     * Creates custom bootstrap using TCP MD5 signature used for Peer connections
     *
     * @param bootstrap Bootstrap that will be customized
     * @param inetHost  Address of node
     * @param password  Password
     * @return Bootstrap that uses TCP MD5
     * @throws NativeSupportUnavailableException If error occurs while initialising TCP MD5
     */
    private static Bootstrap customizeClientBootstrap(Bootstrap bootstrap, InetAddress inetHost, String password)
            throws NativeSupportUnavailableException {
        KeyMapping keys = new KeyMapping();
        keys.put(inetHost, password.getBytes(Charsets.US_ASCII));

        KeyAccessFactory keyAccessFactory = NativeKeyAccessFactory.getInstance();
        MD5NioSocketChannelFactory chf = new MD5NioSocketChannelFactory(keyAccessFactory);
        bootstrap.channelFactory(chf);
        bootstrap.option(MD5ChannelOption.TCP_MD5SIG, keys);
        LOG.info("Customized client bootstrap");
        return bootstrap;
    }

    /**
     * Creates custom bootstrap using TCP MD5 signature used in Node
     *
     * @param bootstrap Bootstrap that will be customized
     * @param md5Peers  Addresses of connections using password
     * @param password  Password
     * @return Bootstrap that uses TCP MD5
     * @throws NativeSupportUnavailableException If error occurs while initialising TCP MD5
     */
    private static ServerBootstrap customizeServerBootstrap(ServerBootstrap bootstrap, Collection<InetAddress> md5Peers,
            String password) throws NativeSupportUnavailableException {
        KeyMapping keyMapping = new KeyMapping();

        // Every peer has to be configured with password separately
        // Right now the configuration allows only for a single password to be shared by all peers with password set to Default
        for (InetAddress inetAddress : md5Peers) {
            keyMapping.put(inetAddress, password.getBytes(Charsets.US_ASCII));
        }

        KeyAccessFactory keyAccessFactory = NativeKeyAccessFactory.getInstance();
        MD5NioServerSocketChannelFactory md5NioServerSocketChannelFactory = new MD5NioServerSocketChannelFactory(
                keyAccessFactory);

        bootstrap.channelFactory(md5NioServerSocketChannelFactory);
        bootstrap.option(MD5ChannelOption.TCP_MD5SIG, keyMapping);
        LOG.info("Customized server bootstrap");
        return bootstrap;
    }
}
