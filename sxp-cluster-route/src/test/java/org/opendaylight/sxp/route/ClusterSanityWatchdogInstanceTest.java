/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route;

import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.BindingTransactionChain;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link ClusterSanityWatchdogInstance}.
 */
public class ClusterSanityWatchdogInstanceTest {

    private final long TIMEOUT = 5000;
    @Mock private Topology topology;
    @Mock private DataBroker dataBroker;
    @Mock private BindingTransactionChain txChain;
    @Mock private ReadTransaction roTx;
    @Mock private ClusterSingletonService singletonService;
    @Mock private ClusterSingletonServiceProvider singletonServiceProvider;
    @Mock private ClusterSingletonServiceRegistration serviceRegistration;

    private ClusterSanityWatchdogInstance watchdogInstance;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.when(dataBroker.createTransactionChain(ArgumentMatchers.any())).thenReturn(txChain);
        Mockito.when(txChain.newReadOnlyTransaction()).thenReturn(roTx);
        Mockito.when(
                singletonServiceProvider.registerClusterSingletonService(Mockito.any(ClusterSingletonService.class)))
                .thenReturn(serviceRegistration);

        watchdogInstance = new ClusterSanityWatchdogInstance(dataBroker, singletonServiceProvider, 1, 3);
        watchdogInstance.setServices(Collections.singletonList(singletonService));
    }

    private void setClusterHealth(boolean healty) {
        if (healty) {
            Mockito.when(roTx.read(ArgumentMatchers.any(), ArgumentMatchers.<InstanceIdentifier<Topology>>any()))
                    .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(topology)));
        } else {
            Mockito.when(roTx.read(ArgumentMatchers.any(), ArgumentMatchers.<InstanceIdentifier<Topology>>any()))
                    .thenReturn(FluentFutures.immediateFluentFuture(Optional.empty()));
        }
    }

    private void setClusterInfoUnreachable() {
        Mockito.when(roTx.read(ArgumentMatchers.any(), ArgumentMatchers.<InstanceIdentifier<Topology>>any()))
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
