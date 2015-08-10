/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database.access;

import com.google.common.util.concurrent.CheckedFuture;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DatastoreAccessTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static DataBroker dataBroker;
        private static DatastoreAccess access;
        private static BindingTransactionChain transactionChain;

        @BeforeClass public static void initClass() {
                dataBroker = mock(DataBroker.class);
                transactionChain = mock(BindingTransactionChain.class);
                when(dataBroker.createTransactionChain(any(TransactionChainListenerImpl.class))).thenReturn(
                        transactionChain);
                access = DatastoreAccess.getInstance(dataBroker);
        }

        @Test public void testGetInstance() throws Exception {
                DatastoreAccess datastoreAccess = DatastoreAccess.getInstance(dataBroker);
                assertNotNull(datastoreAccess);
                assertEquals(datastoreAccess, DatastoreAccess.getInstance(dataBroker));
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

        @Test public void testPutListenable() throws Exception {
                WriteTransaction transaction = mock(WriteTransaction.class);
                when(transaction.submit()).thenReturn(mock(CheckedFuture.class));
                InstanceIdentifier identifier = mock(InstanceIdentifier.class);
                DataObject dataObject = mock(DataObject.class);

                when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
                assertNotNull(access.putListenable(identifier, dataObject, LogicalDatastoreType.OPERATIONAL));

                verify(transaction).put(LogicalDatastoreType.OPERATIONAL, identifier, dataObject);

                when(transactionChain.newWriteOnlyTransaction()).thenReturn(null);
                exception.expect(NullPointerException.class);
                access.putListenable(identifier, dataObject, LogicalDatastoreType.OPERATIONAL);
        }

        @Test public void testPutListenableException() throws Exception {
                when(transactionChain.newWriteOnlyTransaction()).thenReturn(mock(WriteTransaction.class));
                exception.expect(NullPointerException.class);
                access.putListenable(null, null, LogicalDatastoreType.OPERATIONAL);
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

}
