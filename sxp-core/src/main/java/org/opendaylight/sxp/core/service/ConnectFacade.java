/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.service;

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
import io.netty.handler.ssl.SslContext;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.Constants;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SecurityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionTemplateFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A "facade" used for creating SXP servers and clients.
 * Hardwired to use netty epoll.
 * Able to use TLS when certificates are provided.
 */
public class ConnectFacade {//NOSONAR

    private static final Logger LOG = LoggerFactory.getLogger(ConnectFacade.class);

    private static final EventLoopGroup BOSS_GROUP = new EpollEventLoopGroup(1);
    private static final EventLoopGroup EVENT_LOOP_GROUP = new EpollEventLoopGroup();

    /**
     * A test if given connection has an MD5 password
     */
    private static final Predicate<SxpConnection>
            CONNECTION_ENTRY_WITH_MD5_PASSWORD =
            input -> Objects.nonNull(input) && (input.getSecurityType() == SecurityType.Default) && Objects.nonNull(
                    input.getPassword()) && !input.getPassword().isEmpty();
    /**
     * A test if given connection template has an MD5 password
     */
    private static final Predicate<SxpConnectionTemplateFields>
            TEMPLATE_ENTRY_WITH_MD5_PASSWORD =
            input -> Objects.nonNull(input) && (input.getTemplateSecurityType() == SecurityType.Default)
                    && Objects.nonNull(input.getTemplatePassword()) && !input.getTemplatePassword().isEmpty();

    /**
     * Create new Connection to Peer.
     * The supplied connection object provides password
     * and destination info.
     * A connection attempt will be made with a future
     * holding the connection result.
     *
     * @param node       SxpNode containing Security options
     * @param connection SxpConnection containing connection details
     * @param hf         HandlerFactory providing handling of communication
     * @return ChannelFuture callback
     */
    public static ChannelFuture createClient(SxpNode node, SxpConnection connection, HandlerFactory hf) {
        if (!Epoll.isAvailable()) {
            throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
        }
        SecurityType connectionSecurityType = connection.getSecurityType();
        Optional<SslContext> clientSslContext = node.getSslContextFactory().getClientContext();

        RecvByteBufAllocator recvByteBufAllocator = new FixedRecvByteBufAllocator(Constants.MESSAGE_LENGTH_MAX);
        Bootstrap bootstrap = new Bootstrap();
        if ((connectionSecurityType == SecurityType.Default)
                && connection.getPassword() != null
                && !connection.getPassword().isEmpty()) {
            bootstrap.option(EpollChannelOption.TCP_MD5SIG,
                    Collections.singletonMap(connection.getDestination().getAddress(),
                            connection.getPassword().getBytes(StandardCharsets.US_ASCII)));
        } else if ((connectionSecurityType == SecurityType.TLS) && !clientSslContext.isPresent()) {
            throw new IllegalStateException(
                    String.format("%s has TSL enabled but %s does not provide any certificates.", connection, node));
        }
        bootstrap.channel(EpollSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Configuration.NETTY_CONNECT_TIMEOUT_MILLIS)
                .option(ChannelOption.RCVBUF_ALLOCATOR, recvByteBufAllocator)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .localAddress(node.getSourceIp().getHostAddress(), 0)
                .group(EVENT_LOOP_GROUP)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        if ((connectionSecurityType == SecurityType.TLS)) {
                            ch.pipeline().addLast(clientSslContext.get().newHandler(ch.alloc()));
                        }
                        ch.pipeline().addLast(hf.getDecoders());
                        ch.pipeline().addLast(hf.getEncoders());
                    }
                });
        return bootstrap.connect(connection.getDestination());
    }

    /**
     * Create new Node that listens to incoming connections.
     * This method will bind the given node to its specified source IP and port.
     * A future is returned holding a result of the bind operation.
     *
     * @param node       SxpNode containing options
     * @param hf         HandlerFactory providing handling of communication
     * @param keyMapping target to password mapping
     * @return ChannelFuture callback
     */
    public static ChannelFuture createServer(SxpNode node, HandlerFactory hf, Map<InetAddress, byte[]> keyMapping) {
        if (!Epoll.isAvailable()) {
            throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
        }
        LOG.trace("Scheduling server creation for node {} with registered passwords {}", node, keyMapping);
        Optional<SslContext> serverSslContext = node.getSslContextFactory().getServerContext();
        keyMapping.remove(node.getSourceIp());
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.channel(EpollServerSocketChannel.class)
                .option(EpollChannelOption.TCP_MD5SIG, keyMapping)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .group(BOSS_GROUP, EVENT_LOOP_GROUP);
        if (Configuration.NETTY_LOGGER_HANDLER) {
            bootstrap.handler(new LoggingHandler(LogLevel.INFO));
        }
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                SxpConnection connection = node.getConnection(ch.remoteAddress());
                if (connection == null) {
                    LOG.error("Could not find a connection on node: {} for the peer: {}, closing channel: {}",
                            node, ch.remoteAddress(), ch);
                    throw new IllegalStateException("Could not find a connection for the peer " + ch.remoteAddress());
                }
                if (connection.getSecurityType() == SecurityType.TLS) {
                    if (serverSslContext.isPresent()) {
                        ch.pipeline().addLast(serverSslContext.get().newHandler(ch.alloc()));
                    } else {
                        LOG.error("{} - Closing {} as the SSL context is not available", node, ch);
                        throw new IllegalStateException("SSL connection specified, but no serverSSLContext found");
                    }
                }
                ch.pipeline().addLast(hf.getDecoders());
                ch.pipeline().addLast(hf.getEncoders());
            }
        });
        return bootstrap.bind(node.getSourceIp(), node.getServerPort());
    }

    /**
     * Retrieves all passwords from a given node.
     *
     * @param node node to collect passwords from
     * @return a map of addresses and passwords
     */
    public static Map<InetAddress, byte[]> collectAllPasswords(SxpNode node) {
        Map<InetAddress, byte[]> keyMapping = new HashMap<>();
        node.getDomains()
                .forEach(domain -> domain.getConnectionTemplates()
                        .stream()
                        .filter(TEMPLATE_ENTRY_WITH_MD5_PASSWORD)
                        .forEach(template -> {
                            byte[] password = template.getTemplatePassword().getBytes(StandardCharsets.US_ASCII);
                            Search.expandPrefix(template.getTemplatePrefix())
                                    .forEach(inetAddress -> keyMapping.put(inetAddress, password));
                        }));
        node.getAllConnections()
                .stream()
                .filter(CONNECTION_ENTRY_WITH_MD5_PASSWORD)
                .forEach(connection -> keyMapping.put(connection.getDestination().getAddress(),
                        connection.getPassword().getBytes(StandardCharsets.US_ASCII)));
        return keyMapping;
    }
}
