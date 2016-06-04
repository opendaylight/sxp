/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.spi;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class ListListener<P extends DataObject, C extends ChildOf<? super P>, O extends ChildOf<? super C>>
        implements Listener<P, C> {

    protected static final Logger LOG = LoggerFactory.getLogger(ListListener.class.getName());

    private final List<Listener> subListeners = new ArrayList<>();
    protected final DatastoreAccess datastoreAccess;
    protected final Class<C> container;

    protected ListListener(DatastoreAccess datastoreAccess, Class<C> container) {
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        this.container = Preconditions.checkNotNull(container);
    }

    public Listener addSubListener(Listener listener) {
        subListeners.add(Preconditions.checkNotNull(listener));
        return this;
    }

    protected abstract void handleOperational(DataObjectModification<O> c, final InstanceIdentifier<P> identifier);

    protected void handleConfig(DataObjectModification<O> c, final InstanceIdentifier<P> identifier) {
        LOG.trace("Config Modification {} {}", getClass(), c.getModificationType());
        switch (c.getModificationType()) {
            case WRITE:
                if (c.getDataAfter() != null)
                    datastoreAccess.putSynchronous(getIdentifier(c.getDataAfter(), identifier), c.getDataAfter(),
                            LogicalDatastoreType.OPERATIONAL);
            case SUBTREE_MODIFIED:
                if (c.getDataAfter() != null)
                    break;
            case DELETE:
                datastoreAccess.checkAndDelete(getIdentifier(c.getDataBefore(), identifier),
                        LogicalDatastoreType.OPERATIONAL);
                break;
        }
    }

    protected abstract InstanceIdentifier<O> getIdentifier(O d, InstanceIdentifier<P> parentIdentifier);

    public void handleChange(List<DataObjectModification<C>> modifiedChilds, LogicalDatastoreType logicalDatastoreType,
            InstanceIdentifier<P> identifier) {
        if (modifiedChilds != null && !modifiedChilds.isEmpty()) {
            modifiedChilds.stream().filter(c -> c != null).forEach(modifiedChildContainer -> {
                modifiedChildContainer.getModifiedChildren().stream().forEach(m -> {
                    //noinspection unchecked
                    DataObjectModification<O> c = (DataObjectModification<O>) m;
                    switch (logicalDatastoreType) {
                        case OPERATIONAL:
                            handleOperational(c, identifier);
                            break;
                        case CONFIGURATION:
                            handleConfig(c, identifier);
                            break;
                    }
                    subListeners.forEach(l -> {
                        l.handleChange(l.getObjectModifications(c), logicalDatastoreType,
                                getIdentifier(c.getDataBefore() != null ? c.getDataBefore() : c.getDataAfter(),
                                        identifier));
                    });
                });
            });
        }
    }

    public List<DataObjectModification<C>> getModifications(DataTreeModification<P> treeModification) {
        List<DataObjectModification<C>> modifications = new ArrayList<>();
        modifications.add(treeModification.getRootNode().getModifiedChildContainer(container));
        return modifications;
    }

    public List<DataObjectModification<C>> getObjectModifications(DataObjectModification<P> objectModification) {
        List<DataObjectModification<C>> modifications = new ArrayList<>();
        modifications.add(objectModification.getModifiedChildContainer(container));
        return modifications;
    }
}
