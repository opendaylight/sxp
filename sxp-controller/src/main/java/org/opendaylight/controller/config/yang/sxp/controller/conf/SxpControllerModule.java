/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.sxp.controller.conf;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.sxp.controller.core.DataChangeConfigurationListenerImpl;
import org.opendaylight.sxp.controller.core.DataChangeOperationalListenerImpl;
import org.opendaylight.sxp.controller.core.RpcServiceImpl;
import org.opendaylight.sxp.controller.util.database.DatastoreValidator;
import org.opendaylight.sxp.controller.util.database.SxpDatastoreImpl;
import org.opendaylight.sxp.controller.util.database.access.DatastoreAccess;
import org.opendaylight.sxp.controller.util.database.access.SxpDatabaseAccessImpl;
import org.opendaylight.sxp.controller.util.io.ConfigLoader;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

public class SxpControllerModule extends
        org.opendaylight.controller.config.yang.sxp.controller.conf.AbstractSxpControllerModule {

    private List<ListenerRegistration<DataChangeListener>> dataChangeListenerRegistrations = new ArrayList<ListenerRegistration<DataChangeListener>>();

    private RpcRegistration<SxpControllerService> rpcRegistration;

    public SxpControllerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SxpControllerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.sxp.controller.conf.SxpControllerModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final DataBroker dataBroker = getDataBrokerDependency();

        DatastoreValidator datastoreValidator = DatastoreValidator.getInstance(DatastoreAccess.getInstance(dataBroker));
        DatastoreAccess datastoreAccess = datastoreValidator.getDatastoreAccess();

        ConfigLoader.create(datastoreValidator).load(getSxpController());

        for (String controllerName : Configuration.getRegisteredNodesIds()) {
            dataChangeListenerRegistrations.add(dataBroker.registerDataChangeListener(
                    LogicalDatastoreType.CONFIGURATION, DataChangeConfigurationListenerImpl.SUBSCRIBED_PATH,
                    new DataChangeConfigurationListenerImpl(
                            new SxpDatastoreImpl(controllerName, new SxpDatabaseAccessImpl(controllerName,
                                    datastoreAccess, LogicalDatastoreType.OPERATIONAL))), DataChangeScope.SUBTREE));
        }

        // If only one controller is created, enable SXP topology model service
        // to model available bindings sources databases configuration from
        // received protocol data.
        if (Configuration.isNodesRegistered()) {
            DataChangeOperationalListenerImpl dataChangeOperationalListenerImpl = new DataChangeOperationalListenerImpl(
                    Configuration.getNextNodeName(), datastoreValidator);
            dataChangeListenerRegistrations.add(dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                    dataChangeOperationalListenerImpl.getSubscribedPath(), dataChangeOperationalListenerImpl,
                    DataChangeScope.SUBTREE));
        }

        rpcRegistration = getRpcRegistryDependency().addRpcImplementation(SxpControllerService.class,
                new RpcServiceImpl(datastoreAccess));

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                for (ListenerRegistration<DataChangeListener> dataChangeListenerRegistration : dataChangeListenerRegistrations) {
                    dataChangeListenerRegistration.close();
                }

                rpcRegistration.close();

            }
        };
    }

    @Override
    public void customValidation() {
        // Add custom validation form module attributes here.
    }
}
