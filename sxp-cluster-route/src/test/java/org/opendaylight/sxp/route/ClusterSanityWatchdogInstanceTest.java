/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
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
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link ClusterSanityWatchdogInstance}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterSanityWatchdogInstanceTest {

    private final long TIMEOUT = 5000;
    @Mock private Topology topology;
    @Mock private DataBroker dataBroker;
    @Mock private BindingTransactionChain txChain;
    @Mock private ReadOnlyTransaction roTx;
    @Mock private ClusterSingletonService singletonService;
    @Mock private ClusterSingletonServiceProvider singletonServiceProvider;
    @Mock private ClusterSingletonServiceRegistration serviceRegistration;

    private ClusterSanityWatchdogInstance watchdogInstance;

    @Before
    public void setUp() throws Exception {
        Mockito.when(dataBroker.createTransactionChain(Matchers.any())).thenReturn(txChain);
        Mockito.when(txChain.newReadOnlyTransaction()).thenReturn(roTx);
        Mockito.when(
                singletonServiceProvider.registerClusterSingletonService(Mockito.any(ClusterSingletonService.class)))
                .thenReturn(serviceRegistration);

        watchdogInstance = new ClusterSanityWatchdogInstance(dataBroker, singletonServiceProvider, 1, 3);
        watchdogInstance.setServices(Collections.singletonList(singletonService));
    }

    private void setClusterHealth(boolean healty) {
        if (healty) {
            Mockito.when(roTx.read(Matchers.any(), Matchers.<InstanceIdentifier<Topology>>any()))
                    .thenReturn(Futures.immediateCheckedFuture(Optional.of(topology)));
        } else {
            Mockito.when(roTx.read(Matchers.any(), Matchers.<InstanceIdentifier<Topology>>any()))
                    .thenReturn(Futures.immediateCheckedFuture(Optional.absent()));
        }
    }

    private void setClusterInfoUnreachable() {
        Mockito.when(roTx.read(Matchers.any(), Matchers.<InstanceIdentifier<Topology>>any()))
                .thenThrow(new RuntimeException());
    }

    @Test
    public void close() throws Exception {
        setClusterHealth(true);
        watchdogInstance.instantiateServiceInstance();
        watchdogInstance.close();
        setClusterHealth(false);
        Mockito.timeout(TIMEOUT);
        Mockito.verify(singletonService, Mockito.never()).closeServiceInstance();
        Mockito.verify(serviceRegistration).close();
    }

    @Test
    public void onServiceInitiated_0() throws Exception {
        setClusterHealth(true);
        watchdogInstance.instantiateServiceInstance();
        Mockito.timeout(TIMEOUT);
        Mockito.verify(singletonService, Mockito.never()).closeServiceInstance();
    }

    @Test
    public void onServiceInitiated_1() throws Exception {
        setClusterHealth(false);
        watchdogInstance.instantiateServiceInstance();
        Mockito.verify(singletonService, Mockito.timeout(TIMEOUT)).closeServiceInstance();
    }

    @Test
    public void onServiceInitiated_2() throws Exception {
        setClusterInfoUnreachable();
        watchdogInstance.instantiateServiceInstance();
        Mockito.timeout(TIMEOUT);
        Mockito.verify(singletonService, Mockito.never()).closeServiceInstance();
    }

    @Test
    public void onServiceStopped() throws Exception {
        setClusterHealth(true);
        watchdogInstance.instantiateServiceInstance();
        watchdogInstance.closeServiceInstance();
        setClusterHealth(false);
        Mockito.timeout(TIMEOUT);
        Mockito.verify(singletonService, Mockito.never()).closeServiceInstance();
    }

}
