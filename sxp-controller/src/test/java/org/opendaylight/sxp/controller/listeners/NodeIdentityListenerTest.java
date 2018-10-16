/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId.getDefaultInstance;

import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.core.SxpDatastoreNode;
import org.opendaylight.sxp.controller.listeners.spi.Listener;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.Registration;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.MessageBufferingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NodeIdentityListenerTest {

    @Mock
    private Listener listener;
    @Mock
    private SxpDatastoreNode sxpNode;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;

    private NodeIdentityListener identityListener;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        DatastoreAccess datastoreAccess = prepareDataStore(dataBroker, readTransaction, writeTransaction);
        identityListener = new NodeIdentityListener(datastoreAccess);
        identityListener.addSubListener(listener);
        when(sxpNode.getDatastoreAccess()).thenReturn(datastoreAccess);
        when(sxpNode.shutdown()).thenReturn(Futures.immediateFuture(false));
        when(sxpNode.start()).thenReturn(Futures.immediateFuture(true));
        when(sxpNode.getWorker()).thenReturn(new ThreadsWorker());
        when(sxpNode.getNodeId()).thenReturn(getDefaultInstance("0.0.0.0"));
        Registration.register(sxpNode);
    }

    @After
    public void tearDown() {
        Registration.unRegister(sxpNode.getNodeId().getValue());
    }

    @Test
    public void testRegister() throws Exception {
        DataBroker dataBroker = mock(DataBroker.class);
        identityListener.register(dataBroker, LogicalDatastoreType.CONFIGURATION);
        verify(dataBroker).registerDataTreeChangeListener(any(DataTreeIdentifier.class), eq(identityListener));
    }

    private DataTreeModification<SxpNodeIdentity> getTreeModification(
            DataObjectModification.ModificationType modificationType, LogicalDatastoreType datastoreType,
            SxpNodeIdentity before, SxpNodeIdentity after) {
        DataTreeModification<SxpNodeIdentity> modification = mock(DataTreeModification.class);
        DataObjectModification<SxpNodeIdentity>
                objectModification =
                getObjectModification(modificationType, before, after);
        when(modification.getRootNode()).thenReturn(objectModification);
        DataTreeIdentifier<SxpNodeIdentity>
                identifier =
                DataTreeIdentifier.create(datastoreType,
                        NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(new NodeId("0.0.0.0")))
                                .augmentation(SxpNodeIdentity.class));
        when(modification.getRootPath()).thenReturn(identifier);
        return modification;
    }

    private DataObjectModification<SxpNodeIdentity> getObjectModification(
            DataObjectModification.ModificationType modificationType, SxpNodeIdentity before, SxpNodeIdentity after) {
        DataObjectModification<SxpNodeIdentity> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(modificationType);
        when(modification.getDataAfter()).thenReturn(after);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataType()).thenReturn(SxpNodeIdentity.class);
        return modification;
    }

    private SxpNodeIdentity createIdentity(boolean enabled, String ip, int port, Version version,
            int deleteHoldDownTimer) {
        return createIdentity(enabled, ip, port, version, deleteHoldDownTimer, 50, 150);
    }

    private SxpNodeIdentity createIdentity(boolean enabled, String ip, int port, Version version,
            int deleteHoldDownTimer, int inBuffer, int outBuffer) {
        SxpNodeIdentityBuilder builder = new SxpNodeIdentityBuilder();
        builder.setCapabilities(Configuration.getCapabilities(Version.Version4));
        builder.setSecurity(new SecurityBuilder().build());
        builder.setEnabled(enabled);
        builder.setSxpDomains(new SxpDomainsBuilder().setSxpDomain(Collections.singletonList(
                new SxpDomainBuilder().setConnections(new ConnectionsBuilder().build()).build())).build());
        builder.setVersion(version);
        builder.setTcpPort(new PortNumber(port));
        builder.setSourceIp(IpAddressBuilder.getDefaultInstance(ip));
        builder.setTimers(new TimersBuilder().setDeleteHoldDownTime(deleteHoldDownTimer).build());
        builder.setMessageBuffering(
                new MessageBufferingBuilder().setInBuffer(inBuffer).setOutBuffer(outBuffer).build());
        return builder.build();
    }

    @Test
    public void testOnDataTreeChanged_1() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.OPERATIONAL,
                        null, createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).getNodeId();
    }

    @Test
    public void testOnDataTreeChanged_2() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.OPERATIONAL,
                        createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120), null));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
    }

    @Test
    public void testOnDataTreeChanged_3() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.DELETE, LogicalDatastoreType.OPERATIONAL,
                        createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120), null));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).close();
    }

    @Test
    public void testOnDataTreeChanged_4() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120),
                createIdentity(true, "0.0.0.0", 64999, Version.Version3, 120)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
        verify(sxpNode, timeout(500)).start();
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test
    public void testOnDataTreeChanged_5() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120),
                createIdentity(true, "0.0.0.0", 64990, Version.Version4, 120)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
        verify(sxpNode, timeout(500)).start();
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test
    public void testOnDataTreeChanged_6() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120),
                createIdentity(true, "0.0.0.1", 64999, Version.Version4, 120)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
        verify(sxpNode, timeout(500)).start();
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test
    public void testOnDataTreeChanged_7() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120),
                createIdentity(false, "0.0.0.0", 64999, Version.Version4, 120)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
    }

    @Test
    public void testOnDataTreeChanged_8() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(false, "0.0.0.0", 64999, Version.Version4, 120),
                createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).start();
    }

    @Test
    public void testOnDataTreeChanged_9() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120),
                createIdentity(true, "0.0.0.0", 64999, Version.Version4, 12)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdownConnections();
    }

    @Test
    public void testOnDataTreeChanged_10() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.CONFIGURATION, null,
                createIdentity(true, "0.0.0.0", 64999, Version.Version4, 12)));

        identityListener.onDataTreeChanged(modificationList);
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test
    public void testOnDataTreeChanged_11() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.CONFIGURATION, createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12),
                createIdentity(true, "0.0.0.0", 64999, Version.Version4, 12)));

        identityListener.onDataTreeChanged(modificationList);
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test
    public void testOnDataTreeChanged_12() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.DELETE, LogicalDatastoreType.CONFIGURATION,
                        createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12), null));

        identityListener.onDataTreeChanged(modificationList);
        verify(writeTransaction).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }

    @Test
    public void testOnDataTreeChanged_13() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.CONFIGURATION,
                        null, createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12)));

        identityListener.onDataTreeChanged(modificationList);
        verify(writeTransaction).merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
        verify(writeTransaction, never()).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }

    @Test
    public void testOnDataTreeChanged_14() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.CONFIGURATION,
                        createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12),
                        createIdentity(true, "0.0.0.0", 64999, Version.Version3, 122)));

        identityListener.onDataTreeChanged(modificationList);
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction).merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
        verify(writeTransaction, never()).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }

    @Test
    public void testOnDataTreeChanged_15() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.CONFIGURATION,
                        createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12), null));

        identityListener.onDataTreeChanged(modificationList);
        verify(writeTransaction).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }

    @Test
    public void testOnDataTreeChanged_16() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12, 50, 150),
                createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12, 50, 150)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode, never()).setMessageMergeSize(anyInt());
        modificationList.clear();

        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12, 1, 150),
                createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12, 50, 150)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).setMessageMergeSize(anyInt());
    }

    @Test
    public void testOnDataTreeChanged_17() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12, 50, 150),
                createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12, 50, 150)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode, never()).setMessagePartitionSize(anyInt());
        modificationList.clear();

        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12, 50, 50),
                createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12, 50, 150)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).setMessagePartitionSize(anyInt());
    }

    /**
     * Prepare {@link DatastoreAccess} mock instance backed by {@link DataBroker} for tests.
     * <p>
     * {@link ReadTransaction} and {@link WriteTransaction} are assumed to be created by {@link TransactionChain}.
     * <p>
     * {@link ReadTransaction} reads a mock instance of {@link DataObject} on any read.
     * {@link WriteTransaction} is committed successfully.
     *
     * @param dataBroker mock of {@link DataBroker}
     * @param readTransaction mock of {@link ReadTransaction}
     * @param writeTransaction mock of {@link WriteTransaction}
     * @return mock of {@link DatastoreAccess}
     */
    private static DatastoreAccess prepareDataStore(DataBroker dataBroker, ReadTransaction readTransaction,
            WriteTransaction writeTransaction) {
        TransactionChain transactionChain = mock(TransactionChain.class);
        doReturn(CommitInfo.emptyFluentFuture())
                .when(writeTransaction).commit();
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(mock(DataObject.class))));
        when(transactionChain.newReadOnlyTransaction())
                .thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction())
                .thenReturn(writeTransaction);
        when(dataBroker.createTransactionChain(any(TransactionChainListener.class)))
                .thenReturn(transactionChain);

        return DatastoreAccess.getInstance(dataBroker);
    }
}
