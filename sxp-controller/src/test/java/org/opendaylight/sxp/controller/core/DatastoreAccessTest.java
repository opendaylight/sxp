/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.listeners.TransactionChainListenerImpl;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatastoreAccessTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static DataBroker dataBroker;
        private static DatastoreAccess access;
        private static BindingTransactionChain transactionChain;

        @Before public void init() {
                dataBroker = mock(DataBroker.class);
                transactionChain = mock(BindingTransactionChain.class);
                when(dataBroker.createTransactionChain(any(TransactionChainListenerImpl.class))).thenReturn(
                        transactionChain);
                access = DatastoreAccess.getInstance(dataBroker);
        }

        @Test public void testGetInstance() throws Exception {
                DatastoreAccess datastoreAccess = DatastoreAccess.getInstance(dataBroker);
                assertNotNull(datastoreAccess);
                assertNotEquals(datastoreAccess, DatastoreAccess.getInstance(dataBroker));
        }

        @Test public void testDelete() throws Exception {
                WriteTransaction transaction = mock(WriteTransaction.class);
                when(transaction.submit()).thenReturn(mock(CheckedFuture.class));
                InstanceIdentifier identifier = mock(InstanceIdentifier.class);

                when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
                assertNotNull(access.delete(identifier, LogicalDatastoreType.OPERATIONAL));

                verify(transaction).delete(LogicalDatastoreType.OPERATIONAL, identifier);

                when(transactionChain.newWriteOnlyTransaction()).thenReturn(null);
                exception.expect(NullPointerException.class);
                access.delete(identifier, LogicalDatastoreType.OPERATIONAL);
        }

        @Test public void testDeleteException() throws Exception {
                when(transactionChain.newWriteOnlyTransaction()).thenReturn(mock(WriteTransaction.class));
                exception.expect(NullPointerException.class);
                access.delete(null, LogicalDatastoreType.OPERATIONAL);
        }

        @Test public void testMerge() throws Exception {
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

        @Test public void testMergeException() throws Exception {
                when(transactionChain.newWriteOnlyTransaction()).thenReturn(mock(WriteTransaction.class));
                exception.expect(NullPointerException.class);
                access.put(null, null, LogicalDatastoreType.OPERATIONAL);
        }

        @Test public void testPut() throws Exception {
                WriteTransaction transaction = mock(WriteTransaction.class);
                InstanceIdentifier identifier = mock(InstanceIdentifier.class);
                DataObject dataObject = mock(DataObject.class);

                when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
                access.put(identifier, dataObject, LogicalDatastoreType.OPERATIONAL);

                verify(transaction).put(LogicalDatastoreType.OPERATIONAL, identifier, dataObject);

                when(transactionChain.newWriteOnlyTransaction()).thenReturn(null);
                exception.expect(NullPointerException.class);
                access.put(identifier, dataObject, LogicalDatastoreType.OPERATIONAL);
        }

        @Test public void testPutException() throws Exception {
                when(transactionChain.newWriteOnlyTransaction()).thenReturn(mock(WriteTransaction.class));
                exception.expect(NullPointerException.class);
                access.put(null, null, LogicalDatastoreType.OPERATIONAL);
        }

        @Test public void testRead() throws Exception {
                ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
                InstanceIdentifier identifier = mock(InstanceIdentifier.class);

                when(transactionChain.newReadOnlyTransaction()).thenReturn(transaction);
                access.read(identifier, LogicalDatastoreType.OPERATIONAL);

                verify(transaction).read(LogicalDatastoreType.OPERATIONAL, identifier);

                exception.expect(NullPointerException.class);
                when(transactionChain.newReadOnlyTransaction()).thenReturn(null);
                access.read(identifier, LogicalDatastoreType.OPERATIONAL);
        }

        @Test public void testReadException() throws Exception {
                when(transactionChain.newReadOnlyTransaction()).thenReturn(mock(ReadOnlyTransaction.class));
                exception.expect(NullPointerException.class);
                access.read(null, null);
        }

        @Test public void testMergeSynchronous() throws Exception {
                WriteTransaction transaction = mock(WriteTransaction.class);
                when(transaction.submit()).thenReturn(mock(CheckedFuture.class));
                InstanceIdentifier identifier = mock(InstanceIdentifier.class);
                DataObject dataObject = mock(DataObject.class);

                when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
                assertTrue(access.mergeSynchronous(identifier, dataObject, LogicalDatastoreType.OPERATIONAL));

                verify(transaction).merge(LogicalDatastoreType.OPERATIONAL, identifier, dataObject);

                when(transaction.submit()).thenThrow(ExecutionException.class);
                assertFalse(access.mergeSynchronous(identifier, dataObject, LogicalDatastoreType.OPERATIONAL));
        }

        @Test public void testPutSynchronous() throws Exception {
                WriteTransaction transaction = mock(WriteTransaction.class);
                when(transaction.submit()).thenReturn(mock(CheckedFuture.class));
                InstanceIdentifier identifier = mock(InstanceIdentifier.class);
                DataObject dataObject = mock(DataObject.class);

                when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
                assertTrue(access.putSynchronous(identifier, dataObject, LogicalDatastoreType.OPERATIONAL));

                verify(transaction).put(LogicalDatastoreType.OPERATIONAL, identifier, dataObject);

                when(transaction.submit()).thenThrow(ExecutionException.class);
                assertFalse(access.putSynchronous(identifier, dataObject, LogicalDatastoreType.OPERATIONAL));
        }

        @Test public void testReadSynchronous() throws Exception {
                Optional optional = mock(Optional.class);
                when(optional.isPresent()).thenReturn(true);
                when(optional.get()).thenReturn(mock(DataObject.class));

                CheckedFuture future = mock(CheckedFuture.class);
                when(future.get()).thenReturn(optional);

                ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
                when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                        future);
                InstanceIdentifier identifier = mock(InstanceIdentifier.class);

                when(transactionChain.newReadOnlyTransaction()).thenReturn(transaction);
                assertNotNull(access.readSynchronous(identifier, LogicalDatastoreType.OPERATIONAL));

                verify(transaction).read(LogicalDatastoreType.OPERATIONAL, identifier);

                when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenThrow(
                        ExecutionException.class);
                assertNull(access.readSynchronous(identifier, LogicalDatastoreType.OPERATIONAL));
        }

        @Test public void testCheckAndPut() throws Exception {
                WriteTransaction writeTransaction = mock(WriteTransaction.class);
                ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
                when(transactionChain.newWriteOnlyTransaction()).thenReturn(writeTransaction);
                when(transactionChain.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

                CheckedFuture future = mock(CheckedFuture.class);
                Optional optional = mock(Optional.class);
                when(future.get()).thenReturn(optional);
                when(readOnlyTransaction.read(any(LogicalDatastoreType.class),
                        any(InstanceIdentifier.class))).thenReturn(future);
                when(writeTransaction.submit()).thenReturn(mock(CheckedFuture.class));

                InstanceIdentifier identifier = InstanceIdentifier.create(DataObject.class);

                when(transactionChain.newWriteOnlyTransaction()).thenReturn(writeTransaction);
                assertTrue(access.checkAndPut(identifier, mock(DataObject.class), LogicalDatastoreType.OPERATIONAL,
                        false));
                assertFalse(
                        access.checkAndPut(identifier, mock(DataObject.class), LogicalDatastoreType.OPERATIONAL, true));

                when(optional.isPresent()).thenReturn(true);
                when(optional.get()).thenReturn(mock(DataObject.class));

                assertTrue(
                        access.checkAndPut(identifier, mock(DataObject.class), LogicalDatastoreType.OPERATIONAL, true));
                assertFalse(access.checkAndPut(identifier, mock(DataObject.class), LogicalDatastoreType.OPERATIONAL,
                        false));
        }

        @Test public void testCheckAndMerge() throws Exception {
                WriteTransaction writeTransaction = mock(WriteTransaction.class);
                ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
                when(transactionChain.newWriteOnlyTransaction()).thenReturn(writeTransaction);
                when(transactionChain.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

                CheckedFuture future = mock(CheckedFuture.class);
                Optional optional = mock(Optional.class);
                when(future.get()).thenReturn(optional);
                when(readOnlyTransaction.read(any(LogicalDatastoreType.class),
                        any(InstanceIdentifier.class))).thenReturn(future);
                when(writeTransaction.submit()).thenReturn(mock(CheckedFuture.class));

                InstanceIdentifier identifier = InstanceIdentifier.create(DataObject.class);

                when(transactionChain.newWriteOnlyTransaction()).thenReturn(writeTransaction);

                when(optional.isPresent()).thenReturn(true);
                when(optional.get()).thenReturn(mock(DataObject.class));

        }

        @Test public void testCheckAndDelete() throws Exception {
                WriteTransaction writeTransaction = mock(WriteTransaction.class);
                ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
                when(transactionChain.newWriteOnlyTransaction()).thenReturn(writeTransaction);
                when(transactionChain.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

                CheckedFuture future = mock(CheckedFuture.class);
                Optional optional = mock(Optional.class);
                when(future.get()).thenReturn(optional);
                when(readOnlyTransaction.read(any(LogicalDatastoreType.class),
                        any(InstanceIdentifier.class))).thenReturn(future);
                when(writeTransaction.submit()).thenReturn(mock(CheckedFuture.class));

                InstanceIdentifier identifier = InstanceIdentifier.create(DataObject.class);

                when(transactionChain.newWriteOnlyTransaction()).thenReturn(writeTransaction);
                assertFalse(access.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL));

                when(optional.isPresent()).thenReturn(true);
                when(optional.get()).thenReturn(mock(DataObject.class));

                assertTrue(access.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL));

                when(writeTransaction.submit()).thenThrow(ExecutionException.class);
                assertFalse(access.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL));
        }

        @Test public void testClose() throws Exception {
                access.close();
                verify(transactionChain).close();
        }
}
