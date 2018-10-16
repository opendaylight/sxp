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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.core.SxpDatastoreNode;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.ConnectionTemplateListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.ConnectionsListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.DomainFilterListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.DomainListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.FilterListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.PeerGroupListener;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.Registration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.BindingOrigins;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.BindingOriginsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.binding.origins.BindingOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.binding.origins.BindingOriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.binding.origins.BindingOriginKey;
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

    protected static final Logger LOG = LoggerFactory.getLogger(SxpControllerInstance.class);

    public static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(SxpControllerInstance.class.getName());
    private static final InstanceIdentifier<BindingOrigins> BINDING_ORIGINS = InstanceIdentifier
            .builder(BindingOrigins.class).build();

    private DataBroker dataBroker;
    private ClusterSingletonServiceProvider clusteringServiceProvider;
    private DatastoreAccess datastoreAccess;
    private ClusterSingletonServiceRegistration clusterServiceRegistration;
    private final List<ListenerRegistration<DataTreeChangeListener>>
            dataChangeListenerRegistrations = new ArrayList<>();

    public void init() {
        Preconditions.checkNotNull(dataBroker);
        Preconditions.checkNotNull(clusteringServiceProvider);
        LOG.info("Registering into singleton clustering service");
        this.clusterServiceRegistration =
                clusteringServiceProvider.registerClusterSingletonService(this);
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
    }

    /**
     * Put binding origins (key + value) defaults into list of origin bindings if default keys are not already present
     * in data-store preventing overriding user customized priority values from previous Karaf runs.
     */
    private void initBindingOriginsInDS(DatastoreAccess datastoreAccess, Map<OriginType, Integer> defaultOriginPriorities) {
        // ensure empty container binding origins exists, but do not override it
        final BindingOrigins origins = new BindingOriginsBuilder()
                .setBindingOrigin(Collections.emptyList())
                .build();
        datastoreAccess.putIfNotExists(BINDING_ORIGINS, origins, LogicalDatastoreType.CONFIGURATION);

        // ensure default binding origin list entries exist, but do not override them
        defaultOriginPriorities.forEach(this::initBindingOrigin);
    }

    private void initBindingOrigin(OriginType originType, Integer priority) {
        final InstanceIdentifier<BindingOrigin> listPath = InstanceIdentifier.builder(BindingOrigins.class)
                .child(BindingOrigin.class,
                        new BindingOriginKey(
                                new OriginType(
                                        new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType(originType))))
                .build();

        final BindingOriginBuilder origin = new BindingOriginBuilder();
        origin.setOrigin(originType);
        origin.setPriority(priority.shortValue());

        datastoreAccess.putIfNotExists(listPath, origin.build(), LogicalDatastoreType.CONFIGURATION);
    }

    static synchronized void initTopology(final DatastoreAccess datastoreAccess,
            final LogicalDatastoreType datastoreType) {
        InstanceIdentifier<NetworkTopology>
                networkTopologyIndentifier =
                InstanceIdentifier.builder(NetworkTopology.class).build();
        datastoreAccess.merge(networkTopologyIndentifier, new NetworkTopologyBuilder().build(), datastoreType);
        datastoreAccess.merge(NodeIdentityListener.SUBSCRIBED_PATH,
                new TopologyBuilder().withKey(new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME))).build(),
                datastoreType);
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        LOG.warn("Instantiating {}", this.getClass().getSimpleName());
        this.datastoreAccess = DatastoreAccess.getInstance(dataBroker);

        // init binding origins with default values in data-store
        initBindingOriginsInDS(datastoreAccess, BindingOriginsConfig.DEFAULT_ORIGIN_PRIORITIES);
        // validate and init binding origins in internal map
        initBindingOriginsInternal(datastoreAccess);

        // create listener
        NodeIdentityListener datastoreListener = new NodeIdentityListener(datastoreAccess);
        //noinspection unchecked
        datastoreListener.addSubListener(
                new DomainListener(datastoreAccess)
                        .addSubListener(new DomainFilterListener(datastoreAccess))
                        .addSubListener(new ConnectionsListener(datastoreAccess))
                        .addSubListener(new ConnectionTemplateListener(datastoreAccess)));
        //noinspection unchecked
        datastoreListener.addSubListener(
                new PeerGroupListener(datastoreAccess).addSubListener(new FilterListener(datastoreAccess)));

        // init sxp topology
        initTopology(datastoreAccess, LogicalDatastoreType.CONFIGURATION);
        initTopology(datastoreAccess, LogicalDatastoreType.OPERATIONAL);

        // register listener
        dataChangeListenerRegistrations.add(datastoreListener.register(dataBroker, LogicalDatastoreType.CONFIGURATION));
        dataChangeListenerRegistrations.add(datastoreListener.register(dataBroker, LogicalDatastoreType.OPERATIONAL));
    }

    private void initBindingOriginsInternal(DatastoreAccess datastoreAccess) {
        // read binding origins from data-store
        final BindingOrigins bindingOrigins = Preconditions.checkNotNull(datastoreAccess.readSynchronous(
                BINDING_ORIGINS, LogicalDatastoreType.CONFIGURATION));
        final List<BindingOrigin> originList = Preconditions.checkNotNull(bindingOrigins.getBindingOrigin());

        // validate origin bindings
        BindingOriginsConfig.validateBindingOrigins(originList);
        // put origin bindings to internal map
        BindingOriginsConfig.INSTANCE.addBindingOrigins(originList);
    }

    @Override
    public synchronized ListenableFuture<Void> closeServiceInstance() {
        LOG.warn("Clustering provider closed service for {}", this.getClass().getSimpleName());
        dataChangeListenerRegistrations.forEach(ListenerRegistration<DataTreeChangeListener>::close);
        dataChangeListenerRegistrations.clear();
        Registration.getNodes().forEach(n -> {
            if (n instanceof SxpDatastoreNode) {
                ((SxpDatastoreNode) n).close();
            } else {
                n.shutdown();
            }
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

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public ClusterSingletonServiceProvider getClusteringServiceProvider() {
        return clusteringServiceProvider;
    }

    public void setClusteringServiceProvider(ClusterSingletonServiceProvider clusteringServiceProvider) {
        this.clusteringServiceProvider = clusteringServiceProvider;
    }

}
