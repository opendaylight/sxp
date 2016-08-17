/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.sxp.controller.conf;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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

import static org.opendaylight.controller.config.yang.sxp.controller.conf.SxpControllerModule.initTopology;

public class SxpControllerInstance implements AutoCloseable {

    private List<ListenerRegistration<DataTreeChangeListener>> dataChangeListenerRegistrations = new ArrayList<>();
    private final DatastoreAccess datastoreAccess;

    public SxpControllerInstance(DataBroker broker) {
        datastoreAccess = DatastoreAccess.getInstance(Preconditions.checkNotNull(broker));
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
        dataChangeListenerRegistrations.add(datastoreListener.register(broker, LogicalDatastoreType.CONFIGURATION));
        dataChangeListenerRegistrations.add(datastoreListener.register(broker, LogicalDatastoreType.OPERATIONAL));
    }

    @Override public void close() throws Exception {
        dataChangeListenerRegistrations.forEach(ListenerRegistration<DataTreeChangeListener>::close);
        datastoreAccess.close();
    }
}
