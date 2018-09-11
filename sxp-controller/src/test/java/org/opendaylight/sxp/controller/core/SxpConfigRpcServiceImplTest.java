/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.controller.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.AddBindingOriginInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.AddBindingOriginInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.AddBindingOriginOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.DeleteBindingOriginInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.DeleteBindingOriginInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.DeleteBindingOriginOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.UpdateBindingOriginInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.UpdateBindingOriginInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.UpdateBindingOriginOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class SxpConfigRpcServiceImplTest {

    private SxpConfigRpcServiceImpl service;

    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;
    @Mock
    private TransactionChain transactionChain;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(CommitInfo.emptyFluentFuture())
                .when(writeTransaction).commit();
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.empty()));
        when(transactionChain.newReadOnlyTransaction())
                .thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction())
                .thenReturn(writeTransaction);
        when(dataBroker.createTransactionChain(any(TransactionChainListener.class)))
                .thenReturn(transactionChain);
        service = new SxpConfigRpcServiceImpl(dataBroker);
    }

    @After
    public void tearDown() {
        BindingOriginsConfig.INSTANCE.deleteConfiguration();
    }

    @Test
    public void testAddBindingOrigin() throws Exception {
        assertTrue(addBindingOrigin("LOCAL", (short) 0).getResult().isResult());
    }

    @Test
    public void testAddBindingOriginNullType() throws Exception {
        final AddBindingOriginInput input = Mockito.mock(AddBindingOriginInput.class);
        Mockito.when(input.getOrigin()).thenReturn(null);
        Mockito.when(input.getPriority()).thenReturn((short) 1);

        final RpcResult<AddBindingOriginOutput> result = service.addBindingOrigin(input).get();
        Assert.assertFalse(result.getResult().isResult());
    }

    @Test
    public void testAddBindingOriginNullPriority() throws Exception {
        final AddBindingOriginInput input = Mockito.mock(AddBindingOriginInput.class);
        Mockito.when(input.getOrigin()).thenReturn(new OriginType("LOCAL"));
        Mockito.when(input.getPriority()).thenReturn(null);

        final RpcResult<AddBindingOriginOutput> result = service.addBindingOrigin(input).get();
        Assert.assertFalse(result.getResult().isResult());
    }

    @Test
    public void testAddConflictingTypeBinding() throws Exception {
        assertTrue(addBindingOrigin("LOCAL", (short) 0).getResult().isResult());
        assertFalse(addBindingOrigin("LOCAL", (short) 1).getResult().isResult());
    }

    @Test
    public void testAddConflictingPriorityBinding() throws Exception {
        assertTrue(addBindingOrigin("LOCAL", (short) 0).getResult().isResult());
        assertFalse(addBindingOrigin("NETWORK", (short) 0).getResult().isResult());
    }

    @Test
    public void testUpdateBindingOrigin() throws Exception {
        // add binding origin
        assertTrue(addBindingOrigin("NETWORK", (short) 1).getResult().isResult());

        // update it
        final UpdateBindingOriginInput input = new UpdateBindingOriginInputBuilder()
                .setOrigin(new OriginType("NETWORK"))
                .setPriority((short) 2)
                .build();

        final RpcResult<UpdateBindingOriginOutput> result = service.updateBindingOrigin(input).get();
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testUpdateBindingOriginNullType() throws Exception {
        final UpdateBindingOriginInput input = Mockito.mock(UpdateBindingOriginInput.class);
        Mockito.when(input.getOrigin()).thenReturn(null);
        Mockito.when(input.getPriority()).thenReturn((short) 1);

        final RpcResult<UpdateBindingOriginOutput> result = service.updateBindingOrigin(input).get();
        Assert.assertFalse(result.getResult().isResult());
    }

    @Test
    public void testUpdateBindingOriginNullPriority() throws Exception {
        final UpdateBindingOriginInput input = Mockito.mock(UpdateBindingOriginInput.class);
        Mockito.when(input.getOrigin()).thenReturn(new OriginType("LOCAL"));
        Mockito.when(input.getPriority()).thenReturn(null);

        final RpcResult<UpdateBindingOriginOutput> result = service.updateBindingOrigin(input).get();
        Assert.assertFalse(result.getResult().isResult());
    }

    @Test
    public void testUpdateBindingOriginNotExistingType() throws Exception {
        // update not existing origin type
        final UpdateBindingOriginInput input = new UpdateBindingOriginInputBuilder()
                .setOrigin(new OriginType("NETWORK"))
                .setPriority((short) 2)
                .build();

        final RpcResult<UpdateBindingOriginOutput> result = service.updateBindingOrigin(input).get();
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testUpdateBindingOriginConflictingPriority() throws Exception {
        // add LOCAL binding origin
        assertTrue(addBindingOrigin("LOCAL", (short) 1).getResult().isResult());
        // add NETWORK binding origin
        assertTrue(addBindingOrigin("NETWORK", (short) 2).getResult().isResult());

        // try to update LOCAL binding origin to use priority of NETWORK origin
        final UpdateBindingOriginInput input = new UpdateBindingOriginInputBuilder()
                .setOrigin(new OriginType("LOCAL"))
                .setPriority((short) 2)
                .build();

        final RpcResult<UpdateBindingOriginOutput> result = service.updateBindingOrigin(input).get();
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testDeleteBindingOrigin() throws Exception {
        // add binding origin
        assertTrue(addBindingOrigin("CLUSTER", (short) 3).getResult().isResult());

        // delete it
        final DeleteBindingOriginInput input = new DeleteBindingOriginInputBuilder()
                .setOrigin(new OriginType("CLUSTER"))
                .build();

        final RpcResult<DeleteBindingOriginOutput> result = service.deleteBindingOrigin(input).get();
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testDeleteNotExistingBindingOrigin() throws Exception {
        final DeleteBindingOriginInput input = new DeleteBindingOriginInputBuilder()
                .setOrigin(new OriginType("CLUSTER"))
                .build();

        final RpcResult<DeleteBindingOriginOutput> result = service.deleteBindingOrigin(input).get();
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testDeleteDefaultBindingOrigin() throws Exception {
        final DeleteBindingOriginInput localInput = new DeleteBindingOriginInputBuilder()
                .setOrigin(BindingOriginsConfig.LOCAL_ORIGIN)
                .build();
        final DeleteBindingOriginInput networkInput = new DeleteBindingOriginInputBuilder()
                .setOrigin(BindingOriginsConfig.NETWORK_ORIGIN)
                .build();

        assertFalse(service.deleteBindingOrigin(localInput).get().getResult().isResult());
        assertFalse(service.deleteBindingOrigin(networkInput).get().getResult().isResult());
    }

    @Test
    public void testDeleteBindingOriginNullType() throws Exception {
        final DeleteBindingOriginInput input = new DeleteBindingOriginInputBuilder()
                .setOrigin(null)
                .build();

        final RpcResult<DeleteBindingOriginOutput> result = service.deleteBindingOrigin(input).get();
        assertFalse(result.getResult().isResult());
    }

    private RpcResult<AddBindingOriginOutput> addBindingOrigin(String origin, Short priority) throws Exception {
        final AddBindingOriginInput addInput = new AddBindingOriginInputBuilder()
                .setOrigin(new OriginType(origin))
                .setPriority(priority)
                .build();

        return service.addBindingOrigin(addInput).get();
    }
}