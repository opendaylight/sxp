/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.controller.boot;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.core.SxpDatastoreNode;
import org.opendaylight.sxp.controller.core.SxpRpcServiceImpl;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatastoreAccess.class})
public class SxpControllerInstanceTest {

    private DatastoreAccess datastoreAccess;
    private SxpControllerInstance controllerInstance;
    private ClusterSingletonServiceRegistration serviceRegistration;
    private SxpDatastoreNode node;
    private DataBroker dataBroker;
    private ListenerRegistration listenerRegistration;
    private RpcProviderRegistry rpcProviderRegistry;
    private RpcRegistration<SxpControllerService> sxpRpcServiceRegistration;

    @Before
    public void init() {
        PowerMockito.mockStatic(DatastoreAccess.class);
        datastoreAccess = mock(DatastoreAccess.class);
        PowerMockito.when(DatastoreAccess.getInstance(any(DataBroker.class))).thenReturn(datastoreAccess);
        serviceRegistration = mock(ClusterSingletonServiceRegistration.class);
        ClusterSingletonServiceProvider serviceProvider = mock(ClusterSingletonServiceProvider.class);
        when(serviceProvider.registerClusterSingletonService(any(ClusterSingletonService.class))).thenReturn(
                serviceRegistration);
        dataBroker = mock(DataBroker.class);
        listenerRegistration = mock(ListenerRegistration.class);
        when(dataBroker.registerDataTreeChangeListener(any(DataTreeIdentifier.class),
                any(ClusteredDataTreeChangeListener.class))).thenReturn(listenerRegistration);
        rpcProviderRegistry = mock(RpcProviderRegistry.class);
        sxpRpcServiceRegistration = mock(RpcRegistration.class);
        when(rpcProviderRegistry.addRpcImplementation(eq(SxpControllerService.class), any(SxpRpcServiceImpl.class)))
                .thenReturn(sxpRpcServiceRegistration);
        controllerInstance = new SxpControllerInstance();
        controllerInstance.setClusteringServiceProvider(serviceProvider);
        controllerInstance.setDataBroker(dataBroker);
        controllerInstance.setRpcProviderRegistry(rpcProviderRegistry);
        controllerInstance.init();
        node = mock(SxpDatastoreNode.class);
        when(node.getNodeId()).thenReturn(new NodeId("1.1.1.1"));
        Configuration.register(node);
    }

    @Test
    public void initTopology_1() throws Exception {
        SxpControllerInstance.initTopology(datastoreAccess, LogicalDatastoreType.CONFIGURATION);
        verify(datastoreAccess, times(2)).merge(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.CONFIGURATION));
    }

    @Test
    public void initTopology_2() throws Exception {
        SxpControllerInstance.initTopology(datastoreAccess, LogicalDatastoreType.OPERATIONAL);
        verify(datastoreAccess, times(2)).merge(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
    }

    @Test
    public void instantiateServiceInstance() throws Exception {
        controllerInstance.instantiateServiceInstance();
        verify(datastoreAccess, times(2)).merge(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.CONFIGURATION));
        verify(datastoreAccess, times(2)).merge(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
        verify(dataBroker, atLeastOnce()).registerDataTreeChangeListener(any(DataTreeIdentifier.class),
                any(ClusteredDataTreeChangeListener.class));
        verify(rpcProviderRegistry).addRpcImplementation(
                eq(SxpControllerService.class), any(SxpRpcServiceImpl.class));
    }

    @Test
    public void closeServiceInstance() throws Exception {
        controllerInstance.instantiateServiceInstance();
        controllerInstance.closeServiceInstance();

        verify(listenerRegistration, atLeastOnce()).close();
        verify(node, atLeastOnce()).close();
        verify(datastoreAccess, atLeastOnce()).close();
        verify(sxpRpcServiceRegistration).close();
    }

    @Test
    public void getIdentifier() throws Exception {
        assertNotNull(controllerInstance.getIdentifier());
    }

    @Test
    public void close() throws Exception {
        controllerInstance.instantiateServiceInstance();
        controllerInstance.close();

        verify(listenerRegistration, atLeastOnce()).close();
        verify(node, atLeastOnce()).close();
        verify(serviceRegistration).close();
        verify(datastoreAccess, atLeastOnce()).close();
        verify(sxpRpcServiceRegistration).close();
    }
}
