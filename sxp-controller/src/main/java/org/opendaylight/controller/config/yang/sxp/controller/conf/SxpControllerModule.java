/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.sxp.controller.conf;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.core.RpcServiceImpl;
import org.opendaylight.sxp.controller.listeners.ConnectionsListener;
import org.opendaylight.sxp.controller.listeners.FilterListener;
import org.opendaylight.sxp.controller.listeners.MasterBindingListener;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.controller.listeners.PeerGroupListener;
import org.opendaylight.sxp.controller.listeners.SxpDataChangeListener;
import org.opendaylight.sxp.controller.util.io.ConfigLoader;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

public class SxpControllerModule
        extends org.opendaylight.controller.config.yang.sxp.controller.conf.AbstractSxpControllerModule {

    private List<ListenerRegistration<DataTreeChangeListener>> dataChangeListenerRegistrations = new ArrayList<>();

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

    private boolean initTopology(final DatastoreAccess datastoreAccess, final LogicalDatastoreType datastoreType) {
        InstanceIdentifier<NetworkTopology>
                networkTopologyIndentifier =
                InstanceIdentifier.builder(NetworkTopology.class).build();
        if (datastoreAccess.readSynchronous(networkTopologyIndentifier, datastoreType) == null) {
            datastoreAccess.putSynchronous(networkTopologyIndentifier, new NetworkTopologyBuilder().build(),
                    datastoreType);
        }
        if (datastoreAccess.readSynchronous(SxpDataChangeListener.SUBSCRIBED_PATH, datastoreType) == null) {
            datastoreAccess.putSynchronous(SxpDataChangeListener.SUBSCRIBED_PATH,
                    new TopologyBuilder().setKey(new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME))).build(),
                    datastoreType);
            return true;
        }
        return false;
    }

    @Override public java.lang.AutoCloseable createInstance() {
        final DataBroker dataBroker = getDataBrokerDependency();
        DatastoreAccess datastoreAccess = DatastoreAccess.getInstance(dataBroker);
        ConfigLoader configLoader = new ConfigLoader(datastoreAccess);
        if (initTopology(datastoreAccess, LogicalDatastoreType.OPERATIONAL) && initTopology(datastoreAccess,
                LogicalDatastoreType.CONFIGURATION)) {
            //First run setup from config file
            configLoader.load(getSxpController());
        } else {
            //Init nodes from cofig datastore
            configLoader.load(datastoreAccess.readSynchronous(SxpDataChangeListener.SUBSCRIBED_PATH,
                    LogicalDatastoreType.CONFIGURATION));
        }
        SxpDataChangeListener<?>[]
                listeners =
                {new NodeIdentityListener(datastoreAccess), new MasterBindingListener(datastoreAccess),
                        new ConnectionsListener(datastoreAccess), new PeerGroupListener(datastoreAccess),
                        new FilterListener(datastoreAccess)};
        for (SxpDataChangeListener<?> listener : listeners) {
            dataChangeListenerRegistrations.add(listener.register(dataBroker, LogicalDatastoreType.CONFIGURATION));
            dataChangeListenerRegistrations.add(listener.register(dataBroker, LogicalDatastoreType.OPERATIONAL));
        }
        rpcRegistration =
                getRpcRegistryDependency().addRpcImplementation(SxpControllerService.class,
                        new RpcServiceImpl(datastoreAccess));
        return () -> {
            dataChangeListenerRegistrations.forEach(ListenerRegistration<DataTreeChangeListener>::close);
            rpcRegistration.close();
        };
    }

    @Override public void customValidation() {
        // Add custom validation form module attributes here.
    }
}
