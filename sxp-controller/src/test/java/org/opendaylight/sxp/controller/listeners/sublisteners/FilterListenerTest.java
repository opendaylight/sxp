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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.acl.filter.entries.AclEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FilterListenerTest {

    @Mock
    private SxpNode sxpNode;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;

    private FilterListener filterListener;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        DatastoreAccess datastoreAccess = prepareDataStore(dataBroker, readTransaction, writeTransaction);
        filterListener = new FilterListener(datastoreAccess);
    }

    private DataObjectModification<SxpFilter> getObjectModification(
            DataObjectModification.ModificationType modificationType, SxpFilter before, SxpFilter after) {
        DataObjectModification<SxpFilter> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(modificationType);
        when(modification.getDataAfter()).thenReturn(after);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataType()).thenReturn(SxpFilter.class);
        return modification;
    }

    private InstanceIdentifier<SxpPeerGroup> getIdentifier() {
        return NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(new NodeId("0.0.0.0")))
                .augmentation(SxpNodeIdentity.class)
                .child(SxpPeerGroups.class)
                .child(SxpPeerGroup.class, new SxpPeerGroupKey("GROUP"));
    }

    private SxpFilter getSxpFilter(int entries) {
        SxpFilterBuilder builder = new SxpFilterBuilder();
        builder.setFilterType(FilterType.InboundDiscarding);
        builder.setFilterSpecific(FilterSpecific.AccessOrPrefixList);
        List<AclEntry> entrList = new ArrayList<>();
        for (int i = 0; i < entries; i++) {
            entrList.add(mock(AclEntry.class));
        }
        builder.setFilterEntries(new AclFilterEntriesBuilder().setAclEntry(entrList).build());
        return builder.build();
    }

    @Test
    public void testHandleOperational_1() throws Exception {
        filterListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, getSxpFilter(5)),
                getIdentifier(), sxpNode);
        verify(sxpNode).addFilterToPeerGroup(anyString(), any(SxpFilter.class));
    }

    @Test
    public void testHandleOperational_2() throws Exception {
        filterListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getSxpFilter(5), null),
                getIdentifier(), sxpNode);
        verify(sxpNode).removeFilterFromPeerGroup(anyString(), any(FilterType.class), any(FilterSpecific.class));
    }

    @Test
    public void testHandleOperational_3() throws Exception {
        filterListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED, getSxpFilter(5),
                        getSxpFilter(8)), getIdentifier(), sxpNode);
        verify(sxpNode).removeFilterFromPeerGroup(anyString(), any(FilterType.class), any(FilterSpecific.class));
        verify(sxpNode).addFilterToPeerGroup(anyString(), any(SxpFilter.class));
    }

    @Test
    public void testHandleOperational_4() throws Exception {
        filterListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.DELETE, getSxpFilter(5), null),
                getIdentifier(), sxpNode);
        verify(sxpNode).removeFilterFromPeerGroup(anyString(), any(FilterType.class), any(FilterSpecific.class));
    }

    @Test
    public void testGetModifications() throws Exception {
        assertNotNull(filterListener.getIdentifier(new SxpFilterBuilder().setFilterType(FilterType.InboundDiscarding)
                .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                .build(), getIdentifier()));
        assertTrue(filterListener.getIdentifier(new SxpFilterBuilder().setFilterType(FilterType.InboundDiscarding)
                .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                .build(), getIdentifier()).getTargetType().equals(SxpFilter.class));

        assertNotNull(filterListener.getObjectModifications(null));
        assertNotNull(filterListener.getObjectModifications(mock(DataObjectModification.class)));
        assertNotNull(filterListener.getModifications(null));
        DataTreeModification dtm = mock(DataTreeModification.class);
        when(dtm.getRootNode()).thenReturn(mock(DataObjectModification.class));
        assertNotNull(filterListener.getModifications(dtm));
    }

    @Test
    public void testHandleChange() throws Exception {
        filterListener.handleChange(Collections.singletonList(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getSxpFilter(5), getSxpFilter(4))),
                LogicalDatastoreType.OPERATIONAL, getIdentifier());
        verify(writeTransaction, never())
                .put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
        verify(writeTransaction, never())
                .merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));

        filterListener.handleChange(Collections.singletonList(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, getSxpFilter(5))),
                LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(writeTransaction).put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(DataObject.class));

        filterListener.handleChange(Collections.singletonList(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getSxpFilter(5), getSxpFilter(6))),
                LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(writeTransaction)
                .merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));

        filterListener.handleChange(Collections.singletonList(
                getObjectModification(DataObjectModification.ModificationType.DELETE, getSxpFilter(5),
                        getSxpFilter(6))), LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(writeTransaction, times(1)).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }

    /**
     * Prepare {@link DatastoreAccess} mock instance backed by {@link DataBroker} for tests.
     * <p>
     * {@link ReadTransaction} and {@link WriteTransaction} are assumed to be created by {@link TransactionChain}.
     * <p>
     * {@link ReadTransaction} reads {@link Optional#empty()} or a mock instance of {@link DataObject}
     * to satisfies {@link DatastoreAccess#checkAndPut(InstanceIdentifier, DataObject, LogicalDatastoreType, boolean)}
     * called with {@code false}.
     * <p>
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
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.empty()))
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
