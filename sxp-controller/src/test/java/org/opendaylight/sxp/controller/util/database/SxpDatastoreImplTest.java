/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseAccess;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpDatabaseImpl.class}) public class SxpDatastoreImplTest {

        private static SxpDatabaseAccess access;
        private static SxpDatabaseImpl database;
        private static SxpDatastoreImpl datastore;
        private static SxpDatabase sxpDatabase;

        @Before public void init() throws Exception {
                access = mock(SxpDatabaseAccess.class);
                sxpDatabase = mock(SxpDatabase.class);
                when(access.read()).thenReturn(sxpDatabase);
                database = mock(SxpDatabaseImpl.class);
                PowerMockito.whenNew(SxpDatabaseImpl.class).withAnyArguments().thenReturn(database);
                datastore = new SxpDatastoreImpl(access);
        }

        @Test public void testAddBindings() throws Exception {
                datastore.addBindings(mock(SxpDatabase.class));
                verify(access).read();
                verify(access).put(sxpDatabase);
        }

        @Test public void testDeleteBindings() throws Exception {
                datastore.deleteBindings(mock(SxpDatabase.class));
                verify(access).read();
                verify(access).put(sxpDatabase);
        }

        @Test public void testGet() throws Exception {
                datastore.get();
                verify(access).read();
        }

        @Test public void testPurgeBindings() throws Exception {
                datastore.purgeBindings(NodeId.getDefaultInstance("0.0.0.0"));
                verify(access).read();
                verify(access).put(sxpDatabase);
        }

        @Test public void testReadBindings() throws Exception {
                datastore.readBindings();
                verify(access).read();
        }

        @Test public void testToString() throws Exception {
                datastore.toString();
                verify(access).read();

                when(access.read()).thenThrow(DatabaseAccessException.class);
                assertEquals("[error]", datastore.toString());
        }
}
