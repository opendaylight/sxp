/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.it;

import com.google.common.util.concurrent.ListenableFuture;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.MessageBufferingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * VSS Switch over. Here two switches say A and B share the same IP address and so only on can connect. Let us say A is
 * connected. The problem is when they switch from A to B, when B tries to connect ODL code rejects the connection
 * because it sees that it is ON. The fix is to allow the connection and kill the old connection.
 *
 * @author Martin Dindoffer
 */
public class sxp135 {

    private static final Logger LOG = LoggerFactory.getLogger(sxp135.class);

    private SxpNode node1;
    private SxpNode node2;
    private SxpConnection node1Con;
    private SxpConnection node2Con;

    @Before
    public void init() {
        SxpNodeIdentity nodeIdentity1 = createIdentity(true, "127.0.0.1", 1337, Version.Version4, 20, 3);
        this.node1 = SxpNode.createInstance(new NodeId("1.1.1.1"), nodeIdentity1);
        SxpNodeIdentity nodeIdentity2 = createIdentity(true, "127.0.0.2", 1337, Version.Version4, 20, 999);
        this.node2 = SxpNode.createInstance(new NodeId("2.2.2.2"), nodeIdentity2);
        Connection connection1 = createConnection("127.0.0.2", 1337, ConnectionMode.Listener, ConnectionState.Off, Version.Version4);
        node1Con = SxpConnection.create(node1, connection1, "defaultDomain");
        Connection connection2 = createConnection("127.0.0.1", 1337, ConnectionMode.Speaker, ConnectionState.Off, Version.Version4);
        node2Con = SxpConnection.create(node2, connection2, "defaultDomain");
        node1.addConnection(node1Con);
        node2.addConnection(node2Con);
    }

    @Test
    public void test() throws Exception {
        ListenableFuture<Boolean> start1 = node1.start();
        ListenableFuture<Boolean> start2 = node2.start();
        start1.get();
        LOG.debug("Started node 1");
        start2.get();
        LOG.debug("Started node 2");
        LOG.info("Sleeping to allow connections to establish");
        Thread.sleep(4_000);
        LOG.info("Enough sleeping, turning off retry timers");
        node1.setTimer(TimerType.RetryOpenTimer, 0);
        node2.setTimer(TimerType.RetryOpenTimer, 0);
        LOG.info("Sending another open msg");
        ChannelHandlerContext ctxt = node1Con.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.LISTENER_CNTXT);
        ByteBuf openMsg = MessageFactory.createOpen(Version.Version4, ConnectionMode.Listener, node1.getNodeId(), 0);
        ChannelFuture writeFuture = ctxt.writeAndFlush(openMsg);
        writeFuture.sync();
        LOG.info("Written the OpenMSG, sleeping because why not");
        Thread.sleep(1000);
        LOG.info("Enough sleeping, starting Retry timer on the speaker");
        node2.setTimer(TimerType.RetryOpenTimer, 1);
        Thread.sleep(900_000);


        LOG.debug("Slept enough, shutting down");
        ListenableFuture shutdown1 = node1.shutdown();
        ListenableFuture shutdown2 = node2.shutdown();
        shutdown1.get();
        shutdown2.get();
    }

    private Connection createConnection(String peerIpAddress, int peerPort, ConnectionMode mode,
                                        ConnectionState state, Version version) {
        return new ConnectionBuilder().setPeerAddress(new IpAddress(new Ipv4Address(peerIpAddress)))
                .setTcpPort(new PortNumber(peerPort))
                .setMode(mode)
                .setState(state)
                .setVersion(version)
                .setConnectionTimers(new ConnectionTimersBuilder().setDeleteHoldDownTime(0).build())
                .build();
    }

    private SxpNodeIdentity createIdentity(boolean enabled, String sourceIP, int port, Version version,
                                           int deleteHoldDownTimer, int retryOpenTime) {
        return createIdentity(enabled, sourceIP, port, version, deleteHoldDownTimer, 50, 150, retryOpenTime);
    }

    private SxpNodeIdentity createIdentity(boolean enabled, String ip, int port, Version version,
                                           int deleteHoldDownTimer, int inBuffer, int outBuffer, int retryOpenTime) {
        SxpNodeIdentityBuilder builder = new SxpNodeIdentityBuilder();
        builder.setCapabilities(Configuration.getCapabilities(Version.Version4));
        builder.setSecurity(new SecurityBuilder().build());
        builder.setEnabled(enabled);
        builder.setSxpDomains(new SxpDomainsBuilder().setSxpDomain(Collections.singletonList(
                new SxpDomainBuilder().setConnections(new ConnectionsBuilder().build())
                        .setDomainName("defaultDomain")
                        .build())).build());
        builder.setVersion(version);
        builder.setTcpPort(new PortNumber(port));
        builder.setSourceIp(new IpAddress(ip.toCharArray()));
        builder.setTimers(new TimersBuilder().setDeleteHoldDownTime(deleteHoldDownTimer).setRetryOpenTime(retryOpenTime).build());
        builder.setMessageBuffering(
                new MessageBufferingBuilder().setInBuffer(inBuffer).setOutBuffer(outBuffer).build());
        return builder.build();
    }
}
