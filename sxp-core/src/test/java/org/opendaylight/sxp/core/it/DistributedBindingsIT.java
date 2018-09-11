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
import static org.opendaylight.sxp.test.utils.TestDataFactory.createConnection;
import static org.opendaylight.sxp.test.utils.TestDataFactory.createIdentity;

import com.google.common.util.concurrent.ListenableFuture;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
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
 * The IT test of SXP basic functionality running with Hazelcast backend.
 * These tests rely on Awaitility and Thread.sleep()-ing, because the
 * current state of the codebase is **** and does not allow for consumers to listen to important events.
 * So, TODO: remove polling and sleeping when a proper observer is available
 */
public class DistributedBindingsIT {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedBindingsIT.class);
    private static final String DEFAULT_DOMAIN = "defaultDomain";
    private static final int DELETE_HOLD_DOWN_TIMER = 20;
    private static final String NODE1_MASTER_DB_NAME = "NODE1-MASTER";
    private static final String NODE2_MASTER_DB_NAME = "NODE2-MASTER";
    private final MasterDatabaseBinding dummyBinding = TestDataFactory.createMasterDBBinding("0.0.0.5/32", 123, LOCAL_ORIGIN);
    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            LOG.info("Starting test: {}", description.getMethodName());
        }
    };
    @Rule
    public Timeout globalTimeout = new Timeout(90_000);

    private SxpNode node1;
    private SxpNode node2;
    private HazelcastInstance testingHCInstance;
    private HazelcastInstance node1HcInstance;
    private HazelcastInstance node2HcInstance;

    @BeforeClass
    public static void initClass() {
        BindingOriginsConfig.INSTANCE.addBindingOrigins(BindingOriginsConfig.DEFAULT_ORIGIN_PRIORITIES);
    }

    @AfterClass
    public static void tearDown() {
        BindingOriginsConfig.INSTANCE.deleteConfiguration();
    }

    @Before
    public void init() throws InterruptedException, ExecutionException {
        Config hcConfig = new Config();
        hcConfig.getSerializationConfig()
                .addSerializerConfig(SxpDBBindingSerializer.getSerializerConfig())
                .addSerializerConfig(MasterDBBindingSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSequenceSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSerializer.getSerializerConfig());
        hcConfig.getGroupConfig().setName(UUID.randomUUID().toString());
        node1HcInstance = Hazelcast.newHazelcastInstance(hcConfig);
        node2HcInstance = Hazelcast.newHazelcastInstance(hcConfig);
        SxpDatabaseInf node1HCBackedSxpDB = new HazelcastBackedSxpDB("NODE1-ACTIVE", "NODE1-TENTATIVE", node1HcInstance);
        MasterDatabaseInf node1HCBackedMasterDB = new HazelcastBackedMasterDB(NODE1_MASTER_DB_NAME, node1HcInstance);
        SxpDatabaseInf node2HCBackedSxpDB = new HazelcastBackedSxpDB("NODE2-ACTIVE", "NODE2-TENTATIVE", node2HcInstance);
        MasterDatabaseInf node2HCBackedMasterDB = new HazelcastBackedMasterDB(NODE2_MASTER_DB_NAME, node2HcInstance);

        SxpNodeIdentity nodeIdentity1 = createIdentity("127.0.0.1", Constants.SXP_DEFAULT_PORT, Version.Version4, DELETE_HOLD_DOWN_TIMER, 999); //Listener
        this.node1 = SxpNode.createInstance(new NodeId("1.1.1.1"), nodeIdentity1, node1HCBackedMasterDB, node1HCBackedSxpDB);
        SxpNodeIdentity nodeIdentity2 = createIdentity("127.0.0.2", Constants.SXP_DEFAULT_PORT, Version.Version4, DELETE_HOLD_DOWN_TIMER, 2); //Speaker
        this.node2 = SxpNode.createInstance(new NodeId("2.2.2.2"), nodeIdentity2, node2HCBackedMasterDB, node2HCBackedSxpDB);

        ListenableFuture<Boolean> start1 = node1.start();
        ListenableFuture<Boolean> start2 = node2.start();
        start1.get();
        LOG.debug("Started node 1");
        start2.get();
        LOG.debug("Started node 2");
        LOG.info("Hazelcast instances running: {}", Hazelcast.getAllHazelcastInstances().size());
        this.testingHCInstance = Hazelcast.newHazelcastInstance(hcConfig);
    }

    @Test
    public void testPropagationOfBindingsWithExternalMapIO() {
        Connection connection1 = createConnection("127.0.0.2", Constants.SXP_DEFAULT_PORT, ConnectionMode.Listener, ConnectionState.Off, Version.Version4);
        SxpConnection node1Con = SxpConnection.create(node1, connection1, DEFAULT_DOMAIN);
        Connection connection2 = createConnection("127.0.0.1", Constants.SXP_DEFAULT_PORT, ConnectionMode.Speaker, ConnectionState.Off, Version.Version4);
        SxpConnection node2Con = SxpConnection.create(node2, connection2, DEFAULT_DOMAIN);
        node1.addConnection(node1Con);
        node2.addConnection(node2Con);

        LOG.info("Waiting for connections to establish");
        await().atMost(4, TimeUnit.SECONDS).until(node1Con::isStateOn);
        await().atMost(2, TimeUnit.SECONDS).until(node2Con::isStateOn);
        LOG.info("Connections established");
        IMap<IpPrefix, MasterDatabaseBinding> node1MasterMap = testingHCInstance.getMap(NODE1_MASTER_DB_NAME);
        IMap<IpPrefix, MasterDatabaseBinding> node2MasterMap = testingHCInstance.getMap(NODE2_MASTER_DB_NAME);
        LOG.info("Adding a dummy binding to node2 master map");
        node2MasterMap.set(dummyBinding.getIpPrefix(), dummyBinding);
        LOG.info("Waiting for propagation of bindings to node1 map of testing HC instance ");
        await().atMost(5, TimeUnit.SECONDS).until(() -> node1MasterMap.containsKey(dummyBinding.getIpPrefix()));
    }

    @After
    public void shutdown() throws InterruptedException, ExecutionException {
        ListenableFuture<Boolean> shutdown1 = node1.shutdown();
        ListenableFuture<Boolean> shutdown2 = node2.shutdown();
        shutdown1.get();
        shutdown2.get();
        testingHCInstance.shutdown();
        node1HcInstance.shutdown();
        node2HcInstance.shutdown();
    }

}
