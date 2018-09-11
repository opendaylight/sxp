/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.BindingTransactionChain;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionChainListener;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.SxpDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@SuppressWarnings("unchecked")
public class DomainListenerTest {

    @Mock
    private SxpNode sxpNode;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;

    private DomainListener identityListener;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        DatastoreAccess datastoreAccess = prepareDataStore(dataBroker, readTransaction, writeTransaction);
        identityListener = new DomainListener(datastoreAccess);
        when(sxpNode.removeDomain(anyString())).thenReturn(mock(org.opendaylight.sxp.core.SxpDomain.class));
    }

    private DataObjectModification<SxpDomain> getObjectModification(
            DataObjectModification.ModificationType modificationType, SxpDomain before, SxpDomain after) {
        DataObjectModification<SxpDomain> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(modificationType);
        when(modification.getDataAfter()).thenReturn(after);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataType()).thenReturn(SxpDomain.class);
        return modification;
    }

    private DataObjectModification<SxpDomains> getObjectModification(DataObjectModification<SxpDomain> change) {
        DataObjectModification<SxpDomains> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(modification.getDataType()).thenReturn(SxpDomains.class);
        when(modification.getModifiedChildren()).thenAnswer(invocation -> Collections.singletonList(change));
        return modification;
    }

    private InstanceIdentifier<SxpNodeIdentity> getIdentifier() {
        return NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(new NodeId("0.0.0.0")))
                .augmentation(SxpNodeIdentity.class);
    }

    private SxpDomain getDomain(String name) {
        SxpDomainBuilder builder = new SxpDomainBuilder();
        builder.setConnections(new ConnectionsBuilder().setConnection(new ArrayList<>()).build());
        builder.setSxpDatabase(new SxpDatabaseBuilder().setBindingDatabase(new ArrayList<>()).build());
        builder.setMasterDatabase(new MasterDatabaseBuilder().setMasterDatabaseBinding(new ArrayList<>()).build());
        builder.setDomainName(name);
        return builder.build();
    }

    @Test
    public void testHandleOperational_1() throws Exception {
        SxpDomain domain = getDomain("global");
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, domain), getIdentifier(),
                sxpNode);
        verify(sxpNode).addDomain(domain);

        domain = getDomain("secure");
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, domain), getIdentifier(),
                sxpNode);
        verify(sxpNode).addDomain(domain);
    }

    @Test
    public void testHandleOperational_2() throws Exception {
        SxpDomain domain = getDomain("global");
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, domain, null), getIdentifier(),
                sxpNode);
        verify(sxpNode).removeDomain("global");

        domain = getDomain("secure");
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, domain, null), getIdentifier(),
                sxpNode);
        verify(sxpNode).removeDomain("secure");
    }

    @Test
    public void testGetModifications() throws Exception {
        assertNotNull(identityListener.getIdentifier(new SxpDomainBuilder().setDomainName("global").build(),
                getIdentifier()));
        assertTrue(
                identityListener.getIdentifier(new SxpDomainBuilder().setDomainName("global").build(), getIdentifier())
                        .getTargetType()
                        .equals(SxpDomain.class));

        assertNotNull(identityListener.getObjectModifications(null));
        assertNotNull(identityListener.getObjectModifications(mock(DataObjectModification.class)));
        assertNotNull(identityListener.getModifications(null));
        DataTreeModification dtm = mock(DataTreeModification.class);
        when(dtm.getRootNode()).thenReturn(mock(DataObjectModification.class));
        assertNotNull(identityListener.getModifications(dtm));
    }

    @Test
    public void testHandleChange() throws Exception {
        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getDomain("global"),
                        getDomain("global-two")))), LogicalDatastoreType.OPERATIONAL, getIdentifier());
        verify(writeTransaction, never())
                .put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
        verify(writeTransaction, never())
                .merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
        verify(writeTransaction, never()).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, getDomain("global")))),
                LogicalDatastoreType.CONFIGURATION, getIdentifier());

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getDomain("global"),
                        getDomain("global")))), LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(writeTransaction)
                .merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.DELETE, getDomain("global"),
                        getDomain("global")))), LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(writeTransaction).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }

    /**
     * Prepare {@link DatastoreAccess} mock instance backed by {@link DataBroker} for tests.
     * <p>
     * {@link ReadTransaction} and {@link WriteTransaction} are assumed to be created by
     * {@link DatastoreAccess} {@link BindingTransactionChain}.
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
        BindingTransactionChain transactionChain = mock(BindingTransactionChain.class);
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
