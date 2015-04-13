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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.util.exception.connection.SocketAddressNotRecognizedException;
import org.opendaylight.tcpmd5.api.KeyAccessFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.jni.NativeKeyAccessFactory;
import org.opendaylight.tcpmd5.jni.NativeSupportUnavailableException;
import org.opendaylight.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.tcpmd5.netty.MD5NioServerSocketChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5NioSocketChannelFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.PasswordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectFacade {

    private static HashMap<Integer, InetSocketAddress> clientUsedPorts = new HashMap<Integer, InetSocketAddress>();

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

    public static ChannelFuture createClient(SxpNode node, SxpConnection connection, final HandlerFactory hf)
            throws Exception {
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

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            bootstrap.group(group).channel(NioSocketChannel.class);
        } catch (final IllegalStateException e) {
            LOG.info("Not overriding channelFactory on bootstrap {} | {}", bootstrap, e.getMessage());
        }

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ReadTimeoutHandler(Configuration.NETTY_HANDLER_TIMEOUT_MILLIS));
                ch.pipeline().addLast(hf.getDecoders());
                ch.pipeline().addLast(hf.getEncoders());
            }
        });
        try {
            ChannelFuture chf = bootstrap.connect(connection.getDestination()).sync();
            Channel channel = chf.channel();
            setClientPort(channel.localAddress(), node.getServerPort());
            return channel.closeFuture().sync();
        } catch (Exception e) {
            throw e;
        } finally {
            group.shutdownGracefully();
        }
    }

    public static boolean createServer(SxpNode node, int port, final HandlerFactory hf) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        boolean result = true;

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            if (node.getPassword() != null && !node.getPassword().isEmpty()) {
                final Collection<InetAddress> connectionsWithPassword =
                        Collections2.transform(Collections2.filter(node.entrySet(), CONNECTION_ENTRY_WITH_PASSWORD), CONNECTION_ENTRY_TO_INET_ADDR);
                bootstrap = customizeServerBootstrap(bootstrap, connectionsWithPassword, node.getPassword());
            }

            try {
                bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
            } catch (final IllegalStateException e) {
                LOG.info("Not overriding channelFactory on bootstrap {} | {}", bootstrap, e.getMessage());
            }

            ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(hf.getDecoders());
                    ch.pipeline().addLast(hf.getEncoders());
                }
            };

            if (Configuration.NETTY_LOGGER_HANDLER) {
                bootstrap.handler(new LoggingHandler(LogLevel.INFO)).childHandler(channelInitializer);
            } else {
                bootstrap.childHandler(channelInitializer);
            }

            Channel channel = bootstrap.bind(port).sync().channel();
            node.setServerChannel(channel);
            LOG.info(node + " Server created [port=" + port + "]");
            channel.closeFuture().sync();

        } catch (Exception e) {
            LOG.error(node + "Server created exception [port=\"{}\"] " + e.getMessage(), port);
            result = false;
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
        return result;

    }

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

    private static ServerBootstrap customizeServerBootstrap(ServerBootstrap bootstrap, Collection<InetAddress> md5Peers,
            String password) throws Exception {
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

    public static InetSocketAddress getClientUsedAddress(int port) {
        return clientUsedPorts.get(port);
    }

    public static void removeClientPort(SocketAddress localAddress) throws Exception {
        if (!(localAddress instanceof InetSocketAddress)) {
            throw new SocketAddressNotRecognizedException(localAddress);
        }
        int localPort = ((InetSocketAddress) localAddress).getPort();
        clientUsedPorts.remove(localPort);
    }

    private static void setClientPort(SocketAddress localAddress, int destination) throws Exception {
        if (!(localAddress instanceof InetSocketAddress)) {
            throw new SocketAddressNotRecognizedException(localAddress);
        }

        int localPort = ((InetSocketAddress) localAddress).getPort();
        if (!clientUsedPorts.containsKey(localPort)) {
            clientUsedPorts.put(localPort, new InetSocketAddress(destination));
        }
    }
}
