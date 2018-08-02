/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.controller.core;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.AddBindingOriginInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.AddBindingOriginOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.AddBindingOriginOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.DeleteBindingOriginInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.DeleteBindingOriginOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.DeleteBindingOriginOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.SxpConfigControllerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.UpdateBindingOriginInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.UpdateBindingOriginOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.controller.rev180629.UpdateBindingOriginOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.BindingOrigins;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.binding.origins.BindingOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.binding.origins.BindingOriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.binding.origins.BindingOriginKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SxpConfigRpcServiceImpl implements SxpConfigControllerService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SxpRpcServiceImpl.class);

    private final DatastoreAccess datastoreAccess;
    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(
            ThreadsWorker.generateExecutor(1, "SXP-CONFIG-RPC"));

    public SxpConfigRpcServiceImpl(final DataBroker broker) {
        this.datastoreAccess = DatastoreAccess.getInstance(broker);
        LOG.info("ConfigRpcService started for {}", this.getClass().getSimpleName());
    }

    @Override
    public void close() {
        executor.shutdown();
        datastoreAccess.close();
    }

    @Override
    public ListenableFuture<RpcResult<AddBindingOriginOutput>> addBindingOrigin(final AddBindingOriginInput input) {
        final AddBindingOriginOutputBuilder output = new AddBindingOriginOutputBuilder().setResult(false);

        return executor.submit(() -> {
             LOG.info("RpcAddBindingOrigin event | {}", input.toString());

            // verify input
            final OriginType origin = input.getOrigin();
            if (origin == null) {
                LOG.info("RpcAddBindingOrigin exception | Parameter 'origin' not defined", input.toString());
                return RpcResultBuilder.success(output.build()).build();
            }
            final Short priority = input.getPriority();
            if (priority == null) {
                LOG.info("RpcAddBindingOrigin exception | Parameter 'priority' not defined", input.toString());
                return RpcResultBuilder.success(output.build()).build();
            }

            synchronized (BindingOriginsConfig.INSTANCE) {
                // put to internal map representation first because internal map is better validated than data-store
                final boolean addToInternal = BindingOriginsConfig.INSTANCE
                        .addBindingOrigin(origin, priority.intValue());
                if (addToInternal) {
                    // then put to data-store
                    final BindingOrigin bindingOrigin = new BindingOriginBuilder()
                            .setOrigin(origin)
                            .setPriority(priority)
                            .build();
                    final boolean addToDataStore = datastoreAccess
                            .putIfNotExists(InstanceIdentifier.builder(BindingOrigins.class)
                                            .child(BindingOrigin.class, new BindingOriginKey(new OriginType(origin))).build(),
                                    bindingOrigin, LogicalDatastoreType.CONFIGURATION);

                    output.setResult(addToDataStore);
                }
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<UpdateBindingOriginOutput>> updateBindingOrigin(UpdateBindingOriginInput input) {
        final UpdateBindingOriginOutputBuilder output = new UpdateBindingOriginOutputBuilder().setResult(false);

        return executor.submit(() -> {
            LOG.info("RpcUpdateBindingOrigin event | {}", input.toString());

            // verify input
            final OriginType origin = input.getOrigin();
            if (origin == null) {
                LOG.info("RpcUpdateBindingOrigin exception | Parameter 'origin' not defined", input.toString());
                return RpcResultBuilder.success(output.build()).build();
            }
            final Short priority = input.getPriority();
            if (priority == null) {
                LOG.info("RpcUpdateBindingOrigin exception | Parameter 'priority' not defined", input.toString());
                return RpcResultBuilder.success(output.build()).build();
            }

            synchronized (BindingOriginsConfig.INSTANCE) {
                // update in internal map
                final boolean updateInInternal = BindingOriginsConfig.INSTANCE
                        .updateBindingOrigin(origin, priority.intValue());
                if (updateInInternal) {
                    // then update in data-store
                    final BindingOrigin bindingOrigin = new BindingOriginBuilder()
                            .setOrigin(origin)
                            .setPriority(priority)
                            .build();
                    final boolean addToDataStore = datastoreAccess
                            .putSynchronous(InstanceIdentifier.builder(BindingOrigins.class)
                                            .child(BindingOrigin.class, new BindingOriginKey(new OriginType(origin))).build(),
                                    bindingOrigin, LogicalDatastoreType.CONFIGURATION);

                    output.setResult(addToDataStore);
                }
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<DeleteBindingOriginOutput>> deleteBindingOrigin(DeleteBindingOriginInput input) {
        final DeleteBindingOriginOutputBuilder output = new DeleteBindingOriginOutputBuilder().setResult(false);

        return executor.submit(() -> {
            LOG.info("RpcDeleteBindingOrigin event | {}", input.toString());

            // verify input
            final OriginType origin = input.getOrigin();
            if (origin == null) {
                LOG.info("RpcDeleteBindingOrigin exception | Parameter 'origin' not defined", input.toString());
                return RpcResultBuilder.success(output.build()).build();
            }

            synchronized (BindingOriginsConfig.INSTANCE) {
                // remove from internal map to verify existence
                final boolean deleteFromInternal = BindingOriginsConfig.INSTANCE.deleteBindingOrigin(origin);
                if (deleteFromInternal) {
                    // then remove from data-store
                    final boolean deleteFromDataStore = datastoreAccess
                            .deleteSynchronous(InstanceIdentifier.builder(BindingOrigins.class)
                                            .child(BindingOrigin.class, new BindingOriginKey(new OriginType(origin))).build(),
                                    LogicalDatastoreType.CONFIGURATION);

                    output.setResult(deleteFromDataStore);
                }
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }
}
