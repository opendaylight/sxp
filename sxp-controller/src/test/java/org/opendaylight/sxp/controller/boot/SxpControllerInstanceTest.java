/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.controller.boot;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.core.SxpConfigRpcServiceImpl;
import org.opendaylight.sxp.controller.core.SxpDatastoreNode;
import org.opendaylight.sxp.controller.core.SxpRpcServiceImpl;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.SxpConfigControllerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.BindingOrigins;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.BindingOriginsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.binding.origins.BindingOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.binding.origins.BindingOriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SxpControllerInstanceTest {

    private static final InstanceIdentifier<BindingOrigins> BINDING_ORIGINS = InstanceIdentifier
            .builder(BindingOrigins.class).build();

    private static BindingOrigins origins;

    @Mock
    private ClusterSingletonServiceProvider serviceProvider;
    @Mock
    private ClusterSingletonServiceRegistration serviceRegistration;
    @Mock
    private ListenerRegistration listenerRegistration;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;
    @Mock
    private SxpDatastoreNode node;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private RpcProviderService rpcProviderService;
    @Mock
    private ObjectRegistration<SxpRpcServiceImpl> sxpRpcServiceRegistration;
    @Mock
    private ObjectRegistration<SxpConfigRpcServiceImpl> sxpConfigRpcServiceRegistration;

    private DatastoreAccess datastoreAccess;
    private SxpControllerInstance controllerInstance;

    @BeforeClass
    public static void setUpBindings() throws Exception {
        BindingOrigin local = new BindingOriginBuilder()
                .setOrigin(BindingOriginsConfig.LOCAL_ORIGIN)
                .setPriority((short) 1)
                .build();
        BindingOrigin network = new BindingOriginBuilder()
                .setOrigin(BindingOriginsConfig.NETWORK_ORIGIN)
                .setPriority((short) 2)
                .build();
        SxpControllerInstanceTest.origins = new BindingOriginsBuilder()
                .setBindingOrigin(Lists.newArrayList(local, network))
                .build();
    }

    @AfterClass
    public static void tearDown() {
        BindingOriginsConfig.INSTANCE.deleteConfiguration();
    }

    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(serviceProvider.registerClusterSingletonService(any(ClusterSingletonService.class)))
                .thenReturn(serviceRegistration);
        when(dataBroker.registerDataTreeChangeListener(any(DataTreeIdentifier.class),
                any(ClusteredDataTreeChangeListener.class))).thenReturn(listenerRegistration);
        when(rpcProviderService.registerRpcImplementation(eq(SxpControllerService.class), any(SxpRpcServiceImpl.class)))
                .thenReturn(sxpRpcServiceRegistration);
        when(rpcProviderService.registerRpcImplementation(eq(SxpConfigControllerService.class), any(SxpConfigRpcServiceImpl.class)))
                .thenReturn(sxpConfigRpcServiceRegistration);

        controllerInstance = new SxpControllerInstance();
        controllerInstance.setClusteringServiceProvider(serviceProvider);
        controllerInstance.setDataBroker(dataBroker);
        controllerInstance.setRpcProviderService(rpcProviderService);
        controllerInstance.init();

        when(node.getNodeId()).thenReturn(new NodeId("1.1.1.1"));
        Configuration.register(node);

        datastoreAccess = prepareDataStore(dataBroker, readTransaction, writeTransaction);
    }

    @Test
    public void initTopology_1() throws Exception {
        SxpControllerInstance.initTopology(datastoreAccess, LogicalDatastoreType.CONFIGURATION);
        verify(writeTransaction, times(2))
                .merge(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class), any(DataObject.class));
    }

    @Test
    public void initTopology_2() throws Exception {
        SxpControllerInstance.initTopology(datastoreAccess, LogicalDatastoreType.OPERATIONAL);
        verify(writeTransaction, times(2))
                .merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
    }

    @Test
    public void instantiateServiceInstance() throws Exception {
        controllerInstance.instantiateServiceInstance();
        verify(writeTransaction, never())
                .put(eq(LogicalDatastoreType.CONFIGURATION), eq(BINDING_ORIGINS), any());
        verify(writeTransaction, times(2))
                .merge(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class), any(DataObject.class));
        verify(writeTransaction, times(2))
                .merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
        verify(dataBroker, atLeastOnce()).registerDataTreeChangeListener(any(DataTreeIdentifier.class),
                any(ClusteredDataTreeChangeListener.class));
        verify(rpcProviderService).registerRpcImplementation(
                eq(SxpControllerService.class), any(SxpRpcServiceImpl.class));
        verify(rpcProviderService).registerRpcImplementation(
                eq(SxpConfigControllerService.class), any(SxpConfigRpcServiceImpl.class));
    }

    @Test
    public void closeServiceInstance() throws Exception {
        controllerInstance.instantiateServiceInstance();
        controllerInstance.closeServiceInstance();

        verify(listenerRegistration, atLeastOnce()).close();
        verify(node, atLeastOnce()).close();
        verify(sxpRpcServiceRegistration).close();
        verify(sxpConfigRpcServiceRegistration).close();
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
        verify(sxpRpcServiceRegistration).close();
        verify(sxpConfigRpcServiceRegistration).close();
    }

    /**
     * Prepare {@link DatastoreAccess} mock instance backed by {@link DataBroker} for tests.
     * <p>
     * {@link ReadTransaction} and {@link WriteTransaction} are assumed to be created by {@link TransactionChain}.
     * <p>
     * {@link ReadTransaction} reads an mock instance of {@link DataObject} on any read and origins
     * on read specifically on binding origins.
     * {@link WriteTransaction} is committed successfully.
     *
     * @param dataBroker mock of {@link DataBroker}
     * @param readTransaction mock of {@link ReadTransaction}
     * @param writeTransaction mock of {@link WriteTransaction}
     * @return mock of {@link DatastoreAccess}
     */
    private static DatastoreAccess prepareDataStore(DataBroker dataBroker, ReadTransaction readTransaction,
            WriteTransaction writeTransaction) {
        TransactionChain transactionChain = mock(TransactionChain.class);
        doReturn(CommitInfo.emptyFluentFuture())
                .when(writeTransaction).commit();
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(mock(DataObject.class))));
        when(readTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), eq(BINDING_ORIGINS)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(SxpControllerInstanceTest.origins)));
        when(transactionChain.newReadOnlyTransaction())
                .thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction())
                .thenReturn(writeTransaction);
        when(dataBroker.createTransactionChain(any(TransactionChainListener.class)))
                .thenReturn(transactionChain);

        return DatastoreAccess.getInstance(dataBroker);
    }
}
