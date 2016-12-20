/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link FollowerSyncStatusTask}.
 */
@RunWith(MockitoJUnitRunner.class)
public class FollowerSyncStatusTaskTest {

    @Mock private Topology topology;
    @Mock private DataBroker dataBroker;
    @Mock private BindingTransactionChain txChain;
    @Mock private ReadOnlyTransaction roTx;

    private FollowerSyncStatusTask task;

    @Before
    public void setUp() throws Exception {
        Mockito.when(dataBroker.createTransactionChain(Matchers.any())).thenReturn(txChain);
        Mockito.when(txChain.newReadOnlyTransaction()).thenReturn(roTx);
        task = new FollowerSyncStatusTask(1, DatastoreAccess.getInstance(dataBroker), 2);
    }

    @Test
    public void call_success1() throws Exception {
        Mockito.when(roTx.read(Matchers.any(), Matchers.<InstanceIdentifier<Topology>>any()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(topology)));

        Assert.assertTrue("Expected healthy cluster, got condition", task.call());
    }

    @Test
    public void call_success2() throws Exception {
        Mockito.when(roTx.read(Matchers.any(), Matchers.<InstanceIdentifier<Topology>>any()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.absent()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(topology)));

        Assert.assertTrue("Expected healthy cluster, got condition", task.call());
        Assert.assertTrue("Expected healthy cluster, got condition", task.call());
    }

    @Test
    public void call_fail1() throws Exception {
        Mockito.when(roTx.read(Matchers.any(), Matchers.<InstanceIdentifier<Topology>>any()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.absent()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.absent()));

        Assert.assertTrue("Expected healthy cluster, got condition", task.call());
        Assert.assertFalse("Expected isolated cluster, got healthy one", task.call());
    }

    @Test
    public void call_fail2() throws Exception {
        Mockito.when(roTx.read(Matchers.any(), Matchers.<InstanceIdentifier<Topology>>any()))
                .thenThrow(new IllegalStateException());

        Assert.assertTrue("Expected healthy cluster, got condition", task.call());
        Assert.assertFalse("Expected isolated cluster, got healthy one", task.call());
    }

}
