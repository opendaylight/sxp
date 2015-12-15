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
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.MasterBindingIdentity;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseAccess;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class) @PrepareForTest({MasterDatabaseImpl.class, SxpNode.class})
public class MasterDatastoreImplTest {

        private static MasterDatabaseAccess access;
        private static MasterDatastoreImpl datastore;
        private static MasterDatabase masterDatabase;

        @Before public void init() throws Exception {
                access = mock(MasterDatabaseAccess.class);
                masterDatabase = mock(MasterDatabase.class);
                when(access.read()).thenReturn(masterDatabase);
                datastore = new MasterDatastoreImpl(access);
        }

        @Test public void testAddBindings() throws Exception {
                datastore.addBindings(NodeId.getDefaultInstance("0.0.0.0"), new ArrayList<MasterBindingIdentity>());
                verify(access).read();
                verify(access).put(masterDatabase);
        }

        @Test public void testAddBindingsLocal() throws Exception {
                datastore.addBindingsLocal(PowerMockito.mock(SxpNode.class), new ArrayList<PrefixGroup>());
                verify(access).read();
                verify(access).put(masterDatabase);
        }

        @Test public void testExpandBindings() throws Exception {
                datastore.expandBindings(10);
                verify(access).read();
                verify(access).put(masterDatabase);
        }

        @Test public void testGet() throws Exception {
                datastore.get();
                verify(access).read();
        }

        @Test public void testPartition() throws Exception {
                datastore.partition(10, false, null);
                verify(access).read();
        }

        @Test public void testPurgeAllDeletedBindings() throws Exception {
                datastore.purgeAllDeletedBindings();
                verify(access).read();
                verify(access).put(masterDatabase);
        }

        @Test public void testPurgeBindings() throws Exception {
                datastore.purgeBindings(NodeId.getDefaultInstance("0.0.0.0"));
                verify(access).read();
                verify(access).put(masterDatabase);
        }

        @Test public void testReadBindings() throws Exception {
                datastore.readBindings();
                verify(access).read();
        }

        @Test public void testReadBindingsLocal() throws Exception {
                datastore.readBindingsLocal();
                verify(access).read();
        }

        @Test public void testResetModified() throws Exception {
                datastore.resetModified();
                verify(access).read();
                verify(access).put(masterDatabase);
        }

        @Test public void testSetAsDeleted() throws Exception {
                datastore.setAsDeleted(PowerMockito.mock(SxpNode.class), new ArrayList<PrefixGroup>());
                verify(access).read();
                verify(access).put(masterDatabase);
        }

        @Test public void testToString() throws Exception {
                datastore.toString();
                verify(access).read();

                when(access.read()).thenThrow(DatabaseAccessException.class);
                assertEquals("[error]", datastore.toString());
        }
}
