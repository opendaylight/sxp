/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database.access;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabaseBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class) @PrepareForTest({DatastoreAccess.class}) public class SxpDatabaseAccessImplTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static SxpDatabaseAccessImpl databaseAccess;
        private static DatastoreAccess access;
        private static Optional optional;

        @Before public void init() throws Exception {
                access = PowerMockito.mock(DatastoreAccess.class);
                CheckedFuture future = mock(CheckedFuture.class);
                optional = mock(Optional.class);
                when(optional.isPresent()).thenReturn(true);
                when(future.get()).thenReturn(optional);
                PowerMockito.when(access.delete(any(InstanceIdentifier.class), any(LogicalDatastoreType.class)))
                        .thenReturn(future);
                PowerMockito.when(access.merge(any(InstanceIdentifier.class), any(DataObject.class),
                        any(LogicalDatastoreType.class))).thenReturn(future);
                PowerMockito.when(access.put(any(InstanceIdentifier.class), any(DataObject.class),
                        any(LogicalDatastoreType.class))).thenReturn(future);
                PowerMockito.when(access.read(any(InstanceIdentifier.class), any(LogicalDatastoreType.class)))
                        .thenReturn(future);
                databaseAccess = new SxpDatabaseAccessImpl("Test", access, LogicalDatastoreType.OPERATIONAL);
        }

        @Test public void testDelete() throws Exception {
                SxpDatabaseBuilder database = new SxpDatabaseBuilder();
                List<PathGroup> pathGroups = new ArrayList<>();
                database.setPathGroup(pathGroups);
                PathGroupBuilder builder = new PathGroupBuilder();
                builder.setPathHash(50);
                pathGroups.add(builder.build());

                databaseAccess.delete(database.build());
                verify(access).delete(any(InstanceIdentifier.class), any(LogicalDatastoreType.class));

                PowerMockito.when(access.delete(any(InstanceIdentifier.class), any(LogicalDatastoreType.class)))
                        .thenThrow(InterruptedException.class);
                exception.expect(DatabaseAccessException.class);
                databaseAccess.delete(database.build());
        }

        @Test public void testMerge() throws Exception {
                databaseAccess.merge(new SxpDatabaseBuilder().build());
                verify(access).merge(any(InstanceIdentifier.class), any(SxpDatabase.class),
                        any(LogicalDatastoreType.class));

                PowerMockito.when(access.merge(any(InstanceIdentifier.class), any(DataObject.class),
                        any(LogicalDatastoreType.class))).thenThrow(InterruptedException.class);
                exception.expect(DatabaseAccessException.class);
                databaseAccess.merge(new SxpDatabaseBuilder().build());
        }

        @Test public void testPut() throws Exception {
                databaseAccess.put(new SxpDatabaseBuilder().build());
                verify(access).put(any(InstanceIdentifier.class), any(SxpDatabase.class),
                        any(LogicalDatastoreType.class));

                PowerMockito.when(access.put(any(InstanceIdentifier.class), any(DataObject.class),
                        any(LogicalDatastoreType.class))).thenThrow(InterruptedException.class);
                exception.expect(DatabaseAccessException.class);
                databaseAccess.put(new SxpDatabaseBuilder().build());
        }

        @Test public void testRead() throws Exception {
                databaseAccess.read();
                verify(access).read(any(InstanceIdentifier.class), any(LogicalDatastoreType.class));
        }

        @Test public void testReadException0() throws Exception {
                PowerMockito.when(access.read(any(InstanceIdentifier.class), any(LogicalDatastoreType.class)))
                        .thenThrow(InterruptedException.class);
                exception.expect(DatabaseAccessException.class);
                databaseAccess.read();

        }

        @Test public void testReadException1() throws Exception {
                when(optional.isPresent()).thenReturn(false);
                exception.expect(DatabaseAccessException.class);
                databaseAccess.read();
        }
}
