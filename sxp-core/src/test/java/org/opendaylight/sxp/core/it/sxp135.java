/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.Constants;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.test.utils.templates.BindingUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
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

/**
 * IT tests for the VSS Switchover (bug SXP-135). These tests rely heavily on Thread.sleep()-ing, because the current
 * state of the codebase is **** and does not allow for consumers to listen to important events.
 * <p>
 * Here two switches say A and B share the same IP address and so only on can connect. Let's say A is connected. The
 * problem is when they switch from A to B - when B tries to connect the code rejects the connection because it sees
 * that it is ON. The fix is to allow the connection and kill the old connection.
 *
 * @author Martin Dindoffer
 */
public class sxp135 {

    private static final Logger LOG = LoggerFactory.getLogger(sxp135.class);
    private static final String DEFAULT_DOMAIN = "defaultDomain";
    private static final int DELETE_HOLD_DOWN_TIMER = 20;
    private final MasterDatabaseBinding dummyBinding = BindingUtils.createMasterDBBinding("0.0.0.5/32", 123);
    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            LOG.info("Starting test: {}", description.getMethodName());
        }
    };
    @Rule
    public Timeout globalTimeout = new Timeout(15_000);


    private SxpNode node1;
    private SxpNode node2;
    private SxpConnection node1Con;
    private SxpConnection node2Con;

    @Before
    public void init() throws InterruptedException, ExecutionException {
        SxpNodeIdentity nodeIdentity1 = createIdentity("127.0.0.1", Constants.SXP_DEFAULT_PORT, Version.Version4, DELETE_HOLD_DOWN_TIMER, 3); //Listener
        this.node1 = SxpNode.createInstance(new NodeId("1.1.1.1"), nodeIdentity1);
        SxpNodeIdentity nodeIdentity2 = createIdentity("127.0.0.2", Constants.SXP_DEFAULT_PORT, Version.Version4, DELETE_HOLD_DOWN_TIMER, 999); //Speaker
        this.node2 = SxpNode.createInstance(new NodeId("2.2.2.2"), nodeIdentity2);

        ListenableFuture<Boolean> start1 = node1.start();
        ListenableFuture<Boolean> start2 = node2.start();
        start1.get();
        LOG.debug("Started node 1");
        start2.get();
        LOG.debug("Started node 2");
        node2.putLocalBindingsMasterDatabase(Collections.singletonList(dummyBinding), DEFAULT_DOMAIN);
    }

    @Test
    public void testListenerSwitchover() throws Exception {
        Connection connection1 = createConnection("127.0.0.2", Constants.SXP_DEFAULT_PORT, ConnectionMode.Listener, ConnectionState.Off, Version.Version4);
        node1Con = SxpConnection.create(node1, connection1, DEFAULT_DOMAIN);
        Connection connection2 = createConnection("127.0.0.1", Constants.SXP_DEFAULT_PORT, ConnectionMode.Speaker, ConnectionState.Off, Version.Version4);
        node2Con = SxpConnection.create(node2, connection2, DEFAULT_DOMAIN);
        node1.addConnection(node1Con);
        node2.addConnection(node2Con);

        LOG.info("Sleeping to allow connections to establish");
        Thread.sleep(4_000);
        LOG.info("Enough sleeping, turning off retry timers");
        node1.setRetryOpenTimerPeriod(0);
        node2.setRetryOpenTimerPeriod(0);
        LOG.info("Removing bindings from the listener (node1)");
        node1.getDomain(DEFAULT_DOMAIN).getSxpDatabase().deleteBindings(new NodeId(new Ipv4Address("127.0.0.2")));
        assertTrue("Bindings not deleted in node1 sxpDatabase", node1.getBindingSxpDatabase(DEFAULT_DOMAIN).getBindings().isEmpty());
        LOG.info("Sending another open msg from Listener to Speaker");
        ChannelHandlerContext ctxt = node1Con.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.LISTENER_CNTXT);
        ByteBuf openMsg = MessageFactory.createOpen(Version.Version4, ConnectionMode.Listener, node1.getNodeId(), 0);
        ctxt.writeAndFlush(openMsg).sync();
        LOG.info("Written the OpenMSG, sleeping because why not");
        Thread.sleep(1000);
        LOG.info("Enough sleeping, starting Retry timer on the speaker");
        node2.setRetryOpenTimerPeriod(1);
        Thread.sleep(4000);
        LOG.info("Slept enough, checking if bindings have been propagated to switched listener");
        assertFalse("No bindings present in the node1 sxpDatabase", node1.getBindingSxpDatabase(DEFAULT_DOMAIN).getBindings().isEmpty());
    }

    @Test
    public void testSpeakerSwitchover() throws Exception {
        Connection connection1 = createConnection("127.0.0.2", Constants.SXP_DEFAULT_PORT, ConnectionMode.Listener, ConnectionState.Off, Version.Version4);
        node1Con = SxpConnection.create(node1, connection1, DEFAULT_DOMAIN);
        Connection connection2 = createConnection("127.0.0.1", Constants.SXP_DEFAULT_PORT, ConnectionMode.Speaker, ConnectionState.Off, Version.Version4);
        node2Con = SxpConnection.create(node2, connection2, DEFAULT_DOMAIN);
        node1.addConnection(node1Con);
        node2.addConnection(node2Con);

        LOG.info("Sleeping to allow connections to establish");
        Thread.sleep(4_000);
        LOG.info("Enough sleeping, turning off retry timers");
        node1.setRetryOpenTimerPeriod(0);
        node2.setRetryOpenTimerPeriod(0);
        LOG.info("Removing bindings from the listener (node1)");
        node1.getDomain(DEFAULT_DOMAIN).getSxpDatabase().deleteBindings(new NodeId(new Ipv4Address("127.0.0.2")));
        assertTrue("Bindings not deleted in node1 sxpDatabase", node1.getBindingSxpDatabase(DEFAULT_DOMAIN).getBindings().isEmpty());
        LOG.info("Sending another open msg from Speaker to Listener");
        ChannelHandlerContext ctxt = node2Con.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.SPEAKER_CNTXT);
        ByteBuf openMsg = MessageFactory.createOpen(Version.Version4, ConnectionMode.Speaker, node2.getNodeId(), 0);
        ctxt.writeAndFlush(openMsg).sync();
        LOG.info("Written the OpenMSG, sleeping because why not");
        Thread.sleep(1000);
        LOG.info("Enough sleeping, starting Retry timer on the Speaker");
        node2.setRetryOpenTimerPeriod(1);
        Thread.sleep(4000);
        LOG.info("Slept enough, checking if bindings have been propagated to listener");
        assertFalse("No bindings present in the node1 sxpDatabase", node1.getBindingSxpDatabase(DEFAULT_DOMAIN).getBindings().isEmpty());
    }

    @Test
    public void testDuplexSwitchover() throws Exception {
        Connection connection1 = createConnection("127.0.0.2", Constants.SXP_DEFAULT_PORT, ConnectionMode.Both, ConnectionState.Off, Version.Version4);
        node1Con = SxpConnection.create(node1, connection1, DEFAULT_DOMAIN);
        Connection connection2 = createConnection("127.0.0.1", Constants.SXP_DEFAULT_PORT, ConnectionMode.Both, ConnectionState.Off, Version.Version4);
        node2Con = SxpConnection.create(node2, connection2, DEFAULT_DOMAIN);
        node1.addConnection(node1Con);
        node2.addConnection(node2Con);
        node1.setRetryOpenTimerPeriod(1);
        node2.setRetryOpenTimerPeriod(1);

        LOG.info("Sleeping to allow connections to establish");
        Thread.sleep(3_000);
        LOG.info("Enough sleeping, turning off retry timers");
        node1.setRetryOpenTimerPeriod(0);
        node2.setRetryOpenTimerPeriod(0);
        LOG.info("Removing bindings from node1");
        node1.getDomain(DEFAULT_DOMAIN).getSxpDatabase().deleteBindings(new NodeId(new Ipv4Address("127.0.0.2")));
        assertTrue("Bindings not deleted in node1 sxpDatabase", node1.getBindingSxpDatabase(DEFAULT_DOMAIN).getBindings().isEmpty());
        LOG.info("Sending another open msg from node 2 to node 1");
        ChannelHandlerContext ctxt = node2Con.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.LISTENER_CNTXT);
        ByteBuf openMsg = MessageFactory.createOpen(Version.Version4, ConnectionMode.Listener, node2.getNodeId(), 0);
        ctxt.writeAndFlush(openMsg).sync();
        LOG.info("Written the OpenMSG, sleeping because why not");
        Thread.sleep(1000);
        LOG.info("Enough sleeping, starting Retry timer on the nodes 1 and 2");
        node1.setRetryOpenTimerPeriod(1);
        node2.setRetryOpenTimerPeriod(1);
        Thread.sleep(4000);
        LOG.info("Slept enough, checking if bindings have been propagated to node 1");
        assertFalse("No bindings present in the node1 sxpDatabase", node1.getBindingSxpDatabase(DEFAULT_DOMAIN).getBindings().isEmpty());
    }

    @After
    public void shutdownNodes() throws InterruptedException, ExecutionException {
        ListenableFuture<Boolean> shutdown1 = node1.shutdown();
        ListenableFuture<Boolean> shutdown2 = node2.shutdown();
        shutdown1.get();
        shutdown2.get();
    }

    private Connection createConnection(String peerIpAddress, int peerPort, ConnectionMode mode, ConnectionState state,
                                        Version version) {
        return new ConnectionBuilder().setPeerAddress(new IpAddress(new Ipv4Address(peerIpAddress)))
                .setTcpPort(new PortNumber(peerPort))
                .setMode(mode)
                .setState(state)
                .setVersion(version)
                .setConnectionTimers(new ConnectionTimersBuilder()
                        .setDeleteHoldDownTime(DELETE_HOLD_DOWN_TIMER)
                        .setReconciliationTime(1).build())
                .build();
    }

    private SxpNodeIdentity createIdentity(String ip, int port, Version version, int deleteHoldDownTimer, int retryOpenTime) {
        SxpNodeIdentityBuilder builder = new SxpNodeIdentityBuilder();
        builder.setCapabilities(Configuration.getCapabilities(Version.Version4));
        builder.setSecurity(new SecurityBuilder().build());
        builder.setEnabled(true);
        builder.setSxpDomains(new SxpDomainsBuilder().setSxpDomain(Collections.singletonList(
                new SxpDomainBuilder().setConnections(new ConnectionsBuilder().build())
                        .setDomainName(DEFAULT_DOMAIN)
                        .build())).build());
        builder.setVersion(version);
        builder.setTcpPort(new PortNumber(port));
        builder.setSourceIp(new IpAddress(ip.toCharArray()));
        builder.setTimers(new TimersBuilder().setDeleteHoldDownTime(deleteHoldDownTimer).setRetryOpenTime(retryOpenTime).build());
        builder.setMessageBuffering(
                new MessageBufferingBuilder().setInBuffer(50).setOutBuffer(150).build());
        return builder.build();
    }
}
