/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.sxp.controller.conf;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.ConnectionTemplateListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.ConnectionsListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.DomainFilterListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.DomainListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.FilterListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.MasterBindingListener;
import org.opendaylight.sxp.controller.listeners.sublisteners.PeerGroupListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.controller.config.yang.sxp.controller.conf.SxpControllerModule.initTopology;

public class SxpControllerInstance implements ClusterSingletonService, AutoCloseable {

    protected static final Logger LOG = LoggerFactory.getLogger(SxpControllerInstance.class);

    private static final ServiceGroupIdentifier
            IDENTIFIER =
            ServiceGroupIdentifier.create(SxpControllerInstance.class.getName());

    private final DataBroker dataBroker;
    private final DatastoreAccess datastoreAccess;
    private ClusterSingletonServiceRegistration clusterServiceRegistration;
    private List<ListenerRegistration<DataTreeChangeListener>> dataChangeListenerRegistrations = new ArrayList<>();

    public SxpControllerInstance(final DataBroker broker,
            final ClusterSingletonServiceProvider clusteringServiceProvider) {
        this.dataBroker = Preconditions.checkNotNull(broker);
        this.datastoreAccess = DatastoreAccess.getInstance(dataBroker);
        this.clusterServiceRegistration =
                Preconditions.checkNotNull(clusteringServiceProvider).registerClusterSingletonService(this);
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
    }

    @Override public void close() throws Exception {
        dataChangeListenerRegistrations.forEach(ListenerRegistration<DataTreeChangeListener>::close);
        datastoreAccess.close();
    }

    @Override public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        NodeIdentityListener datastoreListener = new NodeIdentityListener(datastoreAccess);
        //noinspection unchecked
        datastoreListener.addSubListener(
                new DomainListener(datastoreAccess).addSubListener(new ConnectionsListener(datastoreAccess))
                        .addSubListener(new MasterBindingListener(datastoreAccess))
                        .addSubListener(new DomainFilterListener(datastoreAccess))
                        .addSubListener(new ConnectionTemplateListener(datastoreAccess)));
        //noinspection unchecked
        datastoreListener.addSubListener(
                new PeerGroupListener(datastoreAccess).addSubListener(new FilterListener(datastoreAccess)));

        initTopology(datastoreAccess, LogicalDatastoreType.CONFIGURATION);
        initTopology(datastoreAccess, LogicalDatastoreType.OPERATIONAL);
        dataChangeListenerRegistrations.add(datastoreListener.register(dataBroker, LogicalDatastoreType.CONFIGURATION));
        dataChangeListenerRegistrations.add(datastoreListener.register(dataBroker, LogicalDatastoreType.OPERATIONAL));
    }

    @Override public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Clustering provider closed for {}", this.getClass().getSimpleName());
        if (clusterServiceRegistration != null) {
            try {
                clusterServiceRegistration.close();
            } catch (Exception e) {
                LOG.warn("{} closed unexpectedly. Cause: {}", e.getMessage());
            }
            clusterServiceRegistration = null;
        }
        return Futures.immediateFuture(null);
    }

    @Override public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }
}
