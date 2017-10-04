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
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.handler.HandlerFactory;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SecurityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionTemplateFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectFacade {

    private static final EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
    private static final EventLoopGroup eventLoopGroup = new EpollEventLoopGroup();
    protected static final Logger LOG = LoggerFactory.getLogger(ConnectFacade.class.getName());

    private static final Predicate<SxpConnection>
            CONNECTION_ENTRY_WITH_MD5_PASSWORD =
            input -> Objects.nonNull(input) && SecurityType.Default.equals(input.getSecurityType()) && Objects.nonNull(
                    input.getPassword()) && !input.getPassword().isEmpty();
    private static final Predicate<SxpConnectionTemplateFields>
            TEMPLATE_ENTRY_WITH_MD5_PASSWORD =
            input -> Objects.nonNull(input) && SecurityType.Default.equals(input.getTemplateSecurityType())
                    && Objects.nonNull(input.getTemplatePassword()) && !input.getTemplatePassword().isEmpty();

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
        final SecurityType securityType = connection.getSecurityType();
        final Optional<SslContext> clientSslContext = node.getSslContextFactory().getClientContext();

        Bootstrap bootstrap = new Bootstrap();
        if (SecurityType.Default.equals(securityType) && connection.getPassword() != null && !connection.getPassword()
                .isEmpty()) {
            bootstrap.option(EpollChannelOption.TCP_MD5SIG,
                    Collections.singletonMap(connection.getDestination().getAddress(),
                            connection.getPassword().getBytes(StandardCharsets.US_ASCII)));
        } else if (SecurityType.TLS.equals(securityType) && !clientSslContext.isPresent()) {
            throw new IllegalStateException(
                    String.format("%s has TSL enabled but %s does not provide any certificates.", connection, node));
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

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                if (SecurityType.TLS.equals(securityType)) {
                    ch.pipeline().addLast(clientSslContext.get().newHandler(ch.alloc()));
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
     * @param keyMapping
     * @return ChannelFuture callback
     */
    public static ChannelFuture createServer(final SxpNode node, final HandlerFactory hf,
                                             final Map<InetAddress, byte[]> keyMapping) {
        if (!Epoll.isAvailable()) {
            throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
        }
        final Optional<SslContext> serverSslContext = node.getSslContextFactory().getServerContext();

        LOG.trace("Scheduling server creation for node {} with registered passwords {}", node, keyMapping);
        keyMapping.remove(node.getSourceIp());
        final ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.channel(EpollServerSocketChannel.class);
        bootstrap.option(EpollChannelOption.TCP_MD5SIG, keyMapping);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.group(bossGroup, eventLoopGroup);
        if (Configuration.NETTY_LOGGER_HANDLER) {
            bootstrap.handler(new LoggingHandler(LogLevel.INFO));
        }
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                final SxpConnection connection = node.getConnection(ch.remoteAddress());
                if (Objects.isNull(connection) || (SecurityType.TLS.equals(connection.getSecurityType())
                        && !serverSslContext.isPresent())) {
                    LOG.warn("{} Closing {} as TLS or Connection not available", node, ch);
                    ch.close();
                } else if (SecurityType.TLS.equals(connection.getSecurityType())) {
                    ch.pipeline().addLast(serverSslContext.get().newHandler(ch.alloc()));
                }
                ch.pipeline().addLast(hf.getDecoders());
                ch.pipeline().addLast(hf.getEncoders());
            }
        });
        return bootstrap.bind(node.getSourceIp(), node.getServerPort());
    }

    public static Map<InetAddress, byte[]> collectAllPasswords(final SxpNode node) {
        Map<InetAddress, byte[]> keyMapping = new HashMap<>();
        node.getDomains()
                .forEach(domain -> domain.getConnectionTemplates()
                        .stream()
                        .filter(TEMPLATE_ENTRY_WITH_MD5_PASSWORD)
                        .forEach(template -> {
                            final byte[] password = template.getTemplatePassword().getBytes(StandardCharsets.US_ASCII);
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
