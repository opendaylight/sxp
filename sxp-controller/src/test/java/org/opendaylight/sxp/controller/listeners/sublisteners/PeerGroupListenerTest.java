/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.controller.listeners.sublisteners;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpPeersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.sxp.peers.SxpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.sxp.peers.SxpPeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PeerGroupListenerTest {

    private PeerGroupListener identityListener;

    @Mock
    private SxpNode sxpNode;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        DatastoreAccess datastoreAccess = prepareDataStore(dataBroker, readTransaction, writeTransaction);
        identityListener = new PeerGroupListener(datastoreAccess);
    }

    private DataObjectModification<SxpPeerGroup> getObjectModification(
            DataObjectModification.ModificationType modificationType, SxpPeerGroup before, SxpPeerGroup after) {
        DataObjectModification<SxpPeerGroup> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(modificationType);
        when(modification.getDataAfter()).thenReturn(after);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataType()).thenReturn(SxpPeerGroup.class);
        return modification;
    }

    private DataObjectModification<SxpPeerGroups> getObjectModification(DataObjectModification<SxpPeerGroup> change) {
        DataObjectModification<SxpPeerGroups> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(modification.getDataType()).thenReturn(SxpPeerGroups.class);
        when(modification.getModifiedChildren()).thenAnswer(invocation -> Collections.singletonList(change));
        return modification;
    }

    private InstanceIdentifier<SxpNodeIdentity> getIdentifier() {
        return NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(new NodeId("0.0.0.0")))
                .augmentation(SxpNodeIdentity.class);
    }

    private SxpPeerGroup getPeerGroup(String name, int peers) {
        SxpPeerGroupBuilder builder = new SxpPeerGroupBuilder();
        builder.setName(name);
        List<SxpPeer> peerList = new ArrayList<>();
        for (int i = 0; i < peers; i++) {
            peerList.add(new SxpPeerBuilder().setPeerAddress(IpAddressBuilder.getDefaultInstance("1.0.0." + i)).build());
        }
        builder.setSxpPeers(new SxpPeersBuilder().setSxpPeer(peerList).build());
        return builder.build();
    }

    @Test
    public void testHandleOperational_1() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, getPeerGroup("GR", 2)),
                getIdentifier(), sxpNode);
        verify(sxpNode).addPeerGroup(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup.class));
    }

    @Test
    public void testHandleOperational_2() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getPeerGroup("GR", 2), null),
                getIdentifier(), sxpNode);
        verify(sxpNode).removePeerGroup(anyString());
    }

    @Test
    public void testHandleOperational_3() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED, getPeerGroup("GR", 5),
                        getPeerGroup("GR", 2)), getIdentifier(), sxpNode);
        verify(sxpNode).removePeerGroup(anyString());
        verify(sxpNode).addPeerGroup(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup.class));
    }

    @Test
    public void testHandleOperational_4() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.DELETE, getPeerGroup("GR", 5), null),
                getIdentifier(), sxpNode);
        verify(sxpNode).removePeerGroup(anyString());
    }

    @Test
    public void testGetIdentifier() throws Exception {
        assertNotNull(identityListener.getIdentifier(new SxpPeerGroupBuilder().setName("TO").build(), getIdentifier()));
        assertTrue(identityListener.getIdentifier(new SxpPeerGroupBuilder().setName("TO").build(), getIdentifier())
                .getTargetType()
                .equals(SxpPeerGroup.class));
    }

    @Test
    public void testHandleChange() throws Exception {
        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getPeerGroup("GR", 2),
                        getPeerGroup("GR", 3)))), LogicalDatastoreType.OPERATIONAL, getIdentifier());
        verify(writeTransaction, never())
                .merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
        verify(writeTransaction, never())
                .merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, getPeerGroup("GR", 2)))),
                LogicalDatastoreType.CONFIGURATION, getIdentifier());

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getPeerGroup("GR", 2),
                        getPeerGroup("GR", 3)))), LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(writeTransaction)
                .merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.DELETE, getPeerGroup("GR", 3),
                        getPeerGroup("GR", 2)))), LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(writeTransaction, times(1)).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }

    @Test
    public void testGetModifications() throws Exception {
        assertNotNull(identityListener.getObjectModifications(null));
        assertNotNull(identityListener.getObjectModifications(mock(DataObjectModification.class)));
        assertNotNull(identityListener.getModifications(null));
        DataTreeModification dtm = mock(DataTreeModification.class);
        when(dtm.getRootNode()).thenReturn(mock(DataObjectModification.class));
        assertNotNull(identityListener.getModifications(dtm));
    }

    /**
     * Prepare {@link DatastoreAccess} mock instance backed by {@link DataBroker} for tests.
     * <p>
     * {@link ReadTransaction} and {@link WriteTransaction} are assumed to be created by {@link TransactionChain}.
     * <p>
     * {@link ReadTransaction} reads an mock instance of {@link DataObject} on any read.
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
