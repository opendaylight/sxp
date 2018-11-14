/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.controller.boot;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.core.SxpDatastoreNode;
import org.opendaylight.sxp.controller.core.SxpRpcServiceImpl;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.ConnectionTemplateListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.ConnectionsListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.DomainFilterListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.DomainListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.FilterListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.MasterDatabaseListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.PeerGroupListener;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SxpControllerInstance implements ClusterSingletonService, AutoCloseable {
    public static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(SxpControllerInstance.class.getName());

    private static final Logger LOG = LoggerFactory.getLogger(SxpControllerInstance.class);

    private final List<ListenerRegistration<DataTreeChangeListener>> dataChangeListenerRegistrations = new ArrayList<>();

    private DataBroker dataBroker;
    private ClusterSingletonServiceProvider clusteringServiceProvider;
    private DatastoreAccess datastoreAccess;
    private ClusterSingletonServiceRegistration clusterServiceRegistration;
    private RpcProviderRegistry rpcProviderRegistry;
    private RpcRegistration<SxpControllerService> sxpRpcServiceRegistration;

    public void init() {
        Preconditions.checkNotNull(dataBroker);
        Preconditions.checkNotNull(clusteringServiceProvider);
        Preconditions.checkNotNull(rpcProviderRegistry);
        LOG.info("Registering into singleton clustering service");
        this.clusterServiceRegistration =
                clusteringServiceProvider.registerClusterSingletonService(this);
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
    }

    private void registerRpcServices() {
        sxpRpcServiceRegistration = rpcProviderRegistry.addRpcImplementation(
                SxpControllerService.class, new SxpRpcServiceImpl(dataBroker));
    }

    static synchronized void initTopology(final DatastoreAccess datastoreAccess,
            final LogicalDatastoreType datastoreType) {
        InstanceIdentifier<NetworkTopology>
                networkTopologyIndentifier =
                InstanceIdentifier.builder(NetworkTopology.class).build();
        datastoreAccess.merge(networkTopologyIndentifier, new NetworkTopologyBuilder().build(), datastoreType);
        datastoreAccess.merge(NodeIdentityListener.SUBSCRIBED_PATH,
                new TopologyBuilder().setKey(new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME))).build(),
                datastoreType);
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        LOG.warn("Instantiating {}", this.getClass().getSimpleName());
        this.datastoreAccess = DatastoreAccess.getInstance(dataBroker);

        // register RPC services to be running on cluster owner only
        registerRpcServices();

        // create listener
        NodeIdentityListener datastoreListener = new NodeIdentityListener(datastoreAccess);
        //noinspection unchecked
        datastoreListener.addSubListener(
                new DomainListener(datastoreAccess).addSubListener(new MasterDatabaseListener(datastoreAccess))
                        .addSubListener(new DomainFilterListener(datastoreAccess))
                        .addSubListener(new ConnectionsListener(datastoreAccess))
                        .addSubListener(new ConnectionTemplateListener(datastoreAccess)));
        //noinspection unchecked
        datastoreListener.addSubListener(
                new PeerGroupListener(datastoreAccess).addSubListener(new FilterListener(datastoreAccess)));

        initTopology(datastoreAccess, LogicalDatastoreType.CONFIGURATION);
        initTopology(datastoreAccess, LogicalDatastoreType.OPERATIONAL);
        dataChangeListenerRegistrations.add(datastoreListener.register(dataBroker, LogicalDatastoreType.CONFIGURATION));
        dataChangeListenerRegistrations.add(datastoreListener.register(dataBroker, LogicalDatastoreType.OPERATIONAL));
    }

    private void closeRpcServices() {
        sxpRpcServiceRegistration.close();
    }

    @Override
    public synchronized ListenableFuture<Void> closeServiceInstance() {
        LOG.warn("Clustering provider closed service for {}", this.getClass().getSimpleName());
        closeRpcServices();
        dataChangeListenerRegistrations.forEach(ListenerRegistration::close);
        dataChangeListenerRegistrations.clear();
        Configuration.getNodes().forEach(node -> {
            if (node instanceof SxpDatastoreNode) {
                ((SxpDatastoreNode) node).close();
            } else {
                node.shutdown();
            }
            Configuration.unRegister(node.getNodeId().getValue());
        });
        datastoreAccess.close();
        return Futures.immediateFuture(null);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public synchronized void close() throws Exception {
        closeServiceInstance().get();
        if (clusterServiceRegistration != null) {
            clusterServiceRegistration.close();
            clusterServiceRegistration = null;
        }
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void setClusteringServiceProvider(ClusterSingletonServiceProvider clusteringServiceProvider) {
        this.clusteringServiceProvider = clusteringServiceProvider;
    }

    public void setRpcProviderRegistry(RpcProviderRegistry rpcProviderRegistry) {
        this.rpcProviderRegistry = rpcProviderRegistry;
    }
}
