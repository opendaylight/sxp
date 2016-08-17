/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.sxp.controller.conf;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.common.util.NoopAutoCloseable;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.controller.util.io.ConfigLoader;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SxpControllerModule
        extends org.opendaylight.controller.config.yang.sxp.controller.conf.AbstractSxpControllerModule {

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

    public static synchronized boolean initTopology(final DatastoreAccess datastoreAccess,
            final LogicalDatastoreType datastoreType) {
        InstanceIdentifier<NetworkTopology>
                networkTopologyIndentifier =
                InstanceIdentifier.builder(NetworkTopology.class).build();
        datastoreAccess.checkAndPut(networkTopologyIndentifier, new NetworkTopologyBuilder().build(), datastoreType,
                false);
        if (datastoreAccess.readSynchronous(NodeIdentityListener.SUBSCRIBED_PATH, datastoreType) == null) {
            datastoreAccess.putSynchronous(NodeIdentityListener.SUBSCRIBED_PATH,
                    new TopologyBuilder().setKey(new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME))).build(),
                    datastoreType);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked") @Override public java.lang.AutoCloseable createInstance() {
        try (DatastoreAccess datastoreAccess = DatastoreAccess.getInstance(getDataBrokerDependency())) {
            ConfigLoader configLoader = new ConfigLoader(datastoreAccess);
            if (initTopology(datastoreAccess, LogicalDatastoreType.OPERATIONAL) && initTopology(datastoreAccess,
                    LogicalDatastoreType.CONFIGURATION)) {
                //First run setup from config file
                configLoader.load(getSxpController());
            }
        }
        return NoopAutoCloseable.INSTANCE;
    }

    @Override public void customValidation() {
        // Add custom validation form module attributes here.
    }
}
