/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.it;

import static org.awaitility.Awaitility.await;
import static org.opendaylight.sxp.core.BindingOriginsConfig.LOCAL_ORIGIN;
import static org.opendaylight.sxp.core.BindingOriginsConfig.NETWORK_ORIGIN;
import static org.opendaylight.sxp.test.utils.TestDataFactory.createConnection;
import static org.opendaylight.sxp.test.utils.TestDataFactory.createIdentity;

import com.google.common.util.concurrent.ListenableFuture;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.Constants;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.hazelcast.MasterDBBindingSerializer;
import org.opendaylight.sxp.core.hazelcast.PeerSequenceSerializer;
import org.opendaylight.sxp.core.hazelcast.PeerSerializer;
import org.opendaylight.sxp.core.hazelcast.SxpDBBindingSerializer;
import org.opendaylight.sxp.test.utils.TestDataFactory;
import org.opendaylight.sxp.util.database.HazelcastBackedMasterDB;
import org.opendaylight.sxp.util.database.HazelcastBackedSxpDB;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests to verify correct behaviour of various binding origin priorities' scenarios.
 */
public class BindingOriginsIT {

    private static final Logger LOG = LoggerFactory.getLogger(BindingOriginsIT.class);
    private static final String DEFAULT_DOMAIN = "defaultDomain";
    private SxpNode speakerNode;
    private SxpNode listenerNode;
    private static final OriginType CLOUD_ORIGIN_TYPE = new OriginType("CLOUD");
    private HazelcastInstance speakerNodeHCInstance;
    private static final MasterDatabaseBinding DUMMY_LOCAL_BINDING = TestDataFactory.createMasterDBBinding("0.0.0.5/32", 100, LOCAL_ORIGIN);
    private static final MasterDatabaseBinding DUMMY_NETWORK_BINDING = TestDataFactory.createMasterDBBinding("0.0.0.5/32", 200, NETWORK_ORIGIN);
    private static final MasterDatabaseBinding DUMMY_CLOUD_BINDING = TestDataFactory.createMasterDBBinding("0.0.0.5/32", 300, CLOUD_ORIGIN_TYPE);

    @BeforeClass
    public static void setupTestOriginPriorities() {
        LOG.info("Setting up origin priorities");
        BindingOriginsConfig.INSTANCE.deleteConfiguration();
        BindingOriginsConfig.INSTANCE.addOrUpdateBindingOrigin(NETWORK_ORIGIN, 1);
        BindingOriginsConfig.INSTANCE.addOrUpdateBindingOrigin(LOCAL_ORIGIN, 2);
        BindingOriginsConfig.INSTANCE.addOrUpdateBindingOrigin(CLOUD_ORIGIN_TYPE, 0);
    }

    @AfterClass
    public static void cleanTestOriginPriorities() {
        BindingOriginsConfig.INSTANCE.deleteConfiguration();
    }

    private void setupInMemoryTopology() throws ExecutionException, InterruptedException {
        SxpNodeIdentity nodeIdentity1 = createIdentity("127.0.0.1", Constants.SXP_DEFAULT_PORT, Version.Version4, 20, 999);
        this.speakerNode = SxpNode.createInstance(new NodeId("1.1.1.1"), nodeIdentity1);
        SxpNodeIdentity nodeIdentity2 = createIdentity("127.0.0.2", Constants.SXP_DEFAULT_PORT, Version.Version4, 20, 2);
        this.listenerNode = SxpNode.createInstance(new NodeId("2.2.2.2"), nodeIdentity2);

        startNodes();
        createAndStartConnections();
    }

    private void setupMixedTopology() throws ExecutionException, InterruptedException {
        Config hcConfig = new Config();
        hcConfig.getSerializationConfig()
                .addSerializerConfig(SxpDBBindingSerializer.getSerializerConfig())
                .addSerializerConfig(MasterDBBindingSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSequenceSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSerializer.getSerializerConfig());
        speakerNodeHCInstance = Hazelcast.newHazelcastInstance(hcConfig);
        SxpDatabaseInf speakerHCBackedSxpDB = new HazelcastBackedSxpDB("NODE1-ACTIVE", "NODE1-TENTATIVE", speakerNodeHCInstance);
        MasterDatabaseInf speakerHCBackedMasterDB = new HazelcastBackedMasterDB("NODE1-MASTER", speakerNodeHCInstance);

        SxpNodeIdentity nodeIdentity1 = createIdentity("127.0.0.1", Constants.SXP_DEFAULT_PORT, Version.Version4, 20, 999);
        this.speakerNode = SxpNode.createInstance(new NodeId("1.1.1.1"), nodeIdentity1, speakerHCBackedMasterDB, speakerHCBackedSxpDB);
        SxpNodeIdentity nodeIdentity2 = createIdentity("127.0.0.2", Constants.SXP_DEFAULT_PORT, Version.Version4, 20, 2);
        this.listenerNode = SxpNode.createInstance(new NodeId("2.2.2.2"), nodeIdentity2);

        startNodes();
        createAndStartConnections();
    }

    private void startNodes() throws ExecutionException, InterruptedException {
        ListenableFuture<Boolean> start1 = speakerNode.start();
        ListenableFuture<Boolean> start2 = listenerNode.start();
        start1.get();
        LOG.debug("Started speaker node");
        start2.get();
        LOG.debug("Started listener node");
    }

    private void createAndStartConnections() {
        Connection connection1 = createConnection("127.0.0.2", Constants.SXP_DEFAULT_PORT, ConnectionMode.Speaker, ConnectionState.Off, Version.Version4);
        SxpConnection node1Con = SxpConnection.create(speakerNode, connection1, DEFAULT_DOMAIN);
        Connection connection2 = createConnection("127.0.0.1", Constants.SXP_DEFAULT_PORT, ConnectionMode.Listener, ConnectionState.Off, Version.Version4);
        SxpConnection node2Con = SxpConnection.create(listenerNode, connection2, DEFAULT_DOMAIN);
        speakerNode.addConnection(node1Con);
        listenerNode.addConnection(node2Con);
        LOG.info("Waiting for connections to establish");
        await().atMost(4, TimeUnit.SECONDS).until(node1Con::isStateOn);
        await().atMost(4, TimeUnit.SECONDS).until(node2Con::isStateOn);
        LOG.info("Connections are up");
    }

    @After
    public void shutdown() throws ExecutionException, InterruptedException {
        ListenableFuture<Boolean> shutdown1 = listenerNode.shutdown();
        ListenableFuture<Boolean> shutdown2 = speakerNode.shutdown();
        shutdown1.get();
        shutdown2.get();
        if (speakerNodeHCInstance != null) {
            speakerNodeHCInstance.shutdown();
        }
    }

    @Test
    public void testCorrectPropagationOfPrioritizedOverwrites() throws ExecutionException, InterruptedException {
        setupInMemoryTopology();

        LOG.info("Adding bindings to speaker node");
        speakerNode.getBindingMasterDatabase(DEFAULT_DOMAIN).addBindings(Collections.singletonList(DUMMY_LOCAL_BINDING));
        Thread.sleep(2_000);
        speakerNode.getBindingMasterDatabase(DEFAULT_DOMAIN).addBindings(Collections.singletonList(DUMMY_CLOUD_BINDING));
        Thread.sleep(2_000);
        speakerNode.getBindingMasterDatabase(DEFAULT_DOMAIN).addBindings(Collections.singletonList(DUMMY_NETWORK_BINDING));

        LOG.info("Waiting for correct binding propagation");
        await().atMost(4, TimeUnit.SECONDS).until(() -> bindingWithSGTIsPresent(listenerNode, DUMMY_CLOUD_BINDING.getSecurityGroupTag()));
    }

    @Test
    public void testCorrectPropagationUsingMixedHazelcastTopo() throws ExecutionException, InterruptedException {
        setupMixedTopology();

        LOG.info("Adding bindings to speaker node");
        speakerNode.getBindingMasterDatabase(DEFAULT_DOMAIN).addBindings(Collections.singletonList(DUMMY_LOCAL_BINDING));
        speakerNode.getBindingMasterDatabase(DEFAULT_DOMAIN).addBindings(Collections.singletonList(DUMMY_CLOUD_BINDING));
        speakerNode.getBindingMasterDatabase(DEFAULT_DOMAIN).addBindings(Collections.singletonList(DUMMY_NETWORK_BINDING));

        LOG.info("Waiting for correct binding propagation");
        await().atMost(4, TimeUnit.SECONDS).until(() -> bindingWithSGTIsPresent(listenerNode, DUMMY_CLOUD_BINDING.getSecurityGroupTag()));
    }

    @Test
    public void testIfLocalBindingGetsOverwrittenFromRemote() throws ExecutionException, InterruptedException {
        setupInMemoryTopology();

        //store a local binding within listener node
        listenerNode.getBindingMasterDatabase(DEFAULT_DOMAIN).addBindings(Collections.singletonList(DUMMY_LOCAL_BINDING));

        speakerNode.getBindingMasterDatabase(DEFAULT_DOMAIN).addBindings(Collections.singletonList(DUMMY_NETWORK_BINDING));

        LOG.info("Checking whether the original local binding got overwritten");
        await().atMost(4, TimeUnit.SECONDS).until(() -> bindingWithSGTIsPresent(listenerNode, DUMMY_NETWORK_BINDING.getSecurityGroupTag()));
    }

    @Test
    public void testOverwriteDoesNotHappen() throws ExecutionException, InterruptedException {
        setupInMemoryTopology();

        //store a cloud binding within listener node
        listenerNode.getBindingMasterDatabase(DEFAULT_DOMAIN).addBindings(Collections.singletonList(DUMMY_CLOUD_BINDING));

        speakerNode.getBindingMasterDatabase(DEFAULT_DOMAIN).addBindings(Collections.singletonList(DUMMY_NETWORK_BINDING));
        LOG.info("Sleeping for 2s to allow potential binding propagation");
        Thread.sleep(2_000);
        LOG.info("Checking whether the original cloud binding did not get overwritten");
        await().atMost(4, TimeUnit.SECONDS).until(() -> bindingWithSGTIsPresent(listenerNode, DUMMY_CLOUD_BINDING.getSecurityGroupTag()));
    }

    private boolean bindingWithSGTIsPresent(SxpNode node, Sgt sgt) {
        List<MasterDatabaseBinding> bindings = node.getBindingMasterDatabase(DEFAULT_DOMAIN).getBindings();
        LOG.info("Bindings: {}", bindings);
        if (bindings.size() != 1) {
            return false;
        } else {
            return sgt.equals(bindings.get(0).getSecurityGroupTag());
        }
    }
}
