/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.BindingTransactionChain;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DatastoreAccessTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Mock
    private DataBroker dataBroker;
    @Mock
    private BindingTransactionChain transactionChain;

    private DatastoreAccess access;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(dataBroker.createTransactionChain(any(SxpTransactionChainListenerImpl.class))).thenReturn(transactionChain);
        access = DatastoreAccess.getInstance(dataBroker);
    }

    @Test
    public void testGetInstance() throws Exception {
        DatastoreAccess datastoreAccess = DatastoreAccess.getInstance(dataBroker);
        assertNotNull(datastoreAccess);
        assertNotEquals(datastoreAccess, DatastoreAccess.getInstance(dataBroker));
    }

    @Test
    public void testDelete() throws Exception {
        WriteTransaction transaction = mock(WriteTransaction.class);
        doReturn(CommitInfo.emptyFluentFuture()).when(transaction).commit();
        InstanceIdentifier identifier = mock(InstanceIdentifier.class);

        when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        assertNotNull(access.delete(identifier, LogicalDatastoreType.OPERATIONAL));

        verify(transaction).delete(LogicalDatastoreType.OPERATIONAL, identifier);

        when(transactionChain.newWriteOnlyTransaction()).thenReturn(null);
        exception.expect(NullPointerException.class);
        access.delete(identifier, LogicalDatastoreType.OPERATIONAL);
    }

    @Test
    public void testDeleteException() throws Exception {
        when(transactionChain.newWriteOnlyTransaction()).thenReturn(mock(WriteTransaction.class));
        exception.expect(NullPointerException.class);
        access.delete(null, LogicalDatastoreType.OPERATIONAL);
    }

    @Test
    public void testMerge() throws Exception {
        WriteTransaction transaction = mock(WriteTransaction.class);
        InstanceIdentifier identifier = mock(InstanceIdentifier.class);
        DataObject dataObject = mock(DataObject.class);

        when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        access.merge(identifier, dataObject, LogicalDatastoreType.OPERATIONAL);

        verify(transaction).merge(LogicalDatastoreType.OPERATIONAL, identifier, dataObject);

        when(transactionChain.newWriteOnlyTransaction()).thenReturn(null);
        exception.expect(NullPointerException.class);
        access.merge(identifier, dataObject, LogicalDatastoreType.OPERATIONAL);
    }

    @Test
    public void testMergeException() throws Exception {
        when(transactionChain.newWriteOnlyTransaction()).thenReturn(mock(WriteTransaction.class));
        exception.expect(NullPointerException.class);
        access.put(null, null, LogicalDatastoreType.OPERATIONAL);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPutIfNotExists() throws Exception {
        InstanceIdentifier path = InstanceIdentifier.create(SxpNodeIdentity.class);

        ReadTransaction readTransaction = mock(ReadTransaction.class);
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(transactionChain.newReadOnlyTransaction()).thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        when(readTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), eq(path)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.empty()));
        when(writeTransaction.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());

        Assert.assertTrue(access.checkAndPut(
                path, mock(DataObject.class), LogicalDatastoreType.CONFIGURATION, false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotPutIfExists() throws Exception {
        InstanceIdentifier path = InstanceIdentifier.create(SxpNodeIdentity.class);

        ReadTransaction readTransaction = mock(ReadTransaction.class);
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(transactionChain.newReadOnlyTransaction()).thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        when(readTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), eq(path)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(mock(SxpNodeIdentity.class))));
        when(writeTransaction.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());

        Assert.assertFalse(access.checkAndPut(
                path, mock(DataObject.class), LogicalDatastoreType.CONFIGURATION, false));
    }

    @Test
    public void testPutException() throws Exception {
        when(transactionChain.newWriteOnlyTransaction()).thenReturn(mock(WriteTransaction.class));
        exception.expect(NullPointerException.class);
        access.put(null, null, LogicalDatastoreType.OPERATIONAL);
    }

    @Test
    public void testRead() throws Exception {
        ReadTransaction transaction = mock(ReadTransaction.class);
        InstanceIdentifier identifier = mock(InstanceIdentifier.class);

        when(transactionChain.newReadOnlyTransaction()).thenReturn(transaction);
        access.read(identifier, LogicalDatastoreType.OPERATIONAL);

        verify(transaction).read(LogicalDatastoreType.OPERATIONAL, identifier);

        exception.expect(NullPointerException.class);
        when(transactionChain.newReadOnlyTransaction()).thenReturn(null);
        access.read(identifier, LogicalDatastoreType.OPERATIONAL);
    }

    @Test
    public void testReadException() throws Exception {
        when(transactionChain.newReadOnlyTransaction()).thenReturn(mock(ReadTransaction.class));
        exception.expect(NullPointerException.class);
        access.read(null, null);
    }

    @Test
    public void testMergeSynchronous() throws Exception {
        WriteTransaction transaction = mock(WriteTransaction.class);
        doReturn(CommitInfo.emptyFluentFuture()).when(transaction).commit();
        InstanceIdentifier identifier = mock(InstanceIdentifier.class);
        DataObject dataObject = mock(DataObject.class);

        when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        assertTrue(access.mergeSynchronous(identifier, dataObject, LogicalDatastoreType.OPERATIONAL));

        verify(transaction).merge(LogicalDatastoreType.OPERATIONAL, identifier, dataObject);

        when(transaction.commit()).thenThrow(ExecutionException.class);
        assertFalse(access.mergeSynchronous(identifier, dataObject, LogicalDatastoreType.OPERATIONAL));
    }

    @Test
    public void testPutSynchronous() throws Exception {
        WriteTransaction transaction = mock(WriteTransaction.class);
        doReturn(CommitInfo.emptyFluentFuture()).when(transaction).commit();
        InstanceIdentifier identifier = mock(InstanceIdentifier.class);
        DataObject dataObject = mock(DataObject.class);

        when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        assertTrue(access.putSynchronous(identifier, dataObject, LogicalDatastoreType.OPERATIONAL));

        verify(transaction).put(LogicalDatastoreType.OPERATIONAL, identifier, dataObject);

        when(transaction.commit()).thenThrow(ExecutionException.class);
        assertFalse(access.putSynchronous(identifier, dataObject, LogicalDatastoreType.OPERATIONAL));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReadSynchronous() throws Exception {
        InstanceIdentifier path = InstanceIdentifier.create(SxpNodeIdentity.class);

        ReadTransaction readTransaction = mock(ReadTransaction.class);
        when(transactionChain.newReadOnlyTransaction()).thenReturn(readTransaction);

        when(readTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), eq(path)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(mock(SxpNodeIdentity.class))));

        Assert.assertNotNull(access.readSynchronous(path, LogicalDatastoreType.CONFIGURATION));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCheckAndPut() throws Exception {
        InstanceIdentifier path = InstanceIdentifier.create(SxpNodeIdentity.class);

        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        ReadTransaction readTransaction = mock(ReadTransaction.class);
        when(transactionChain.newReadOnlyTransaction()).thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        when(readTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), eq(path)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.empty()));
        when(writeTransaction.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());

        Assert.assertTrue(access.checkAndPut(
                path, mock(DataObject.class), LogicalDatastoreType.CONFIGURATION, false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCheckAndMerge() throws Exception {
        InstanceIdentifier path = InstanceIdentifier.create(SxpNodeIdentity.class);

        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        ReadTransaction readTransaction = mock(ReadTransaction.class);
        when(transactionChain.newReadOnlyTransaction()).thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        when(readTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), eq(path)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.empty()));
        when(writeTransaction.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());

        Assert.assertTrue(access.checkAndMerge(
                path, mock(DataObject.class), LogicalDatastoreType.CONFIGURATION, false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCheckAndDelete() throws Exception {
        InstanceIdentifier path = InstanceIdentifier.create(SxpNodeIdentity.class);

        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        ReadTransaction readTransaction = mock(ReadTransaction.class);
        when(transactionChain.newReadOnlyTransaction()).thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        when(readTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), eq(path)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(mock(SxpNodeIdentity.class))));
        when(writeTransaction.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());

        Assert.assertTrue(access.checkAndDelete(path, LogicalDatastoreType.CONFIGURATION));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteSynchronous() throws Exception {
        WriteTransaction transaction = mock(WriteTransaction.class);
        doReturn(CommitInfo.emptyFluentFuture()).when(transaction).commit();
        when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        InstanceIdentifier identifier = mock(InstanceIdentifier.class);

        assertTrue(access.deleteSynchronous(identifier, LogicalDatastoreType.CONFIGURATION));
        verify(transaction).delete(LogicalDatastoreType.CONFIGURATION, identifier);
    }

    @Test
    public void testClose() throws Exception {
        access.close();
        verify(transactionChain).close();
    }
}
