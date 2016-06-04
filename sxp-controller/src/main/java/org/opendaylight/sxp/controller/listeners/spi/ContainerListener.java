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

public abstract class ContainerListener<P extends DataObject, C extends ChildOf<? super P>> implements Listener<P, C> {

    protected static final Logger LOG = LoggerFactory.getLogger(ContainerListener.class.getName());

    private final List<Listener> subListeners = new ArrayList<>();
    protected final DatastoreAccess datastoreAccess;
    protected final Class<C> container;

    protected ContainerListener(DatastoreAccess datastoreAccess, Class<C> container) {
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        this.container = Preconditions.checkNotNull(container);
    }

    public Listener addSubListener(Listener listener) {
        subListeners.add(Preconditions.checkNotNull(listener));
        return this;
    }

    protected abstract void handleOperational(DataObjectModification<C> c, final InstanceIdentifier<P> identifier);

    protected void handleConfig(DataObjectModification<C> c, final InstanceIdentifier<P> identifier) {
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

    protected abstract InstanceIdentifier<C> getIdentifier(C d, InstanceIdentifier<P> parentIdentifier);

    public void handleChange(List<DataObjectModification<C>> modifiedChilds, LogicalDatastoreType logicalDatastoreType,
            InstanceIdentifier<P> identifier) {
        if (modifiedChilds != null && !modifiedChilds.isEmpty()) {
            modifiedChilds.parallelStream().filter(c -> c != null).forEach(modifiedChildContainer -> {
                switch (logicalDatastoreType) {
                    case OPERATIONAL:
                        handleOperational(modifiedChildContainer, identifier);
                        break;
                    case CONFIGURATION:
                        handleConfig(modifiedChildContainer, identifier);
                        break;
                }
                subListeners.parallelStream().forEach(l -> {
                    l.handleChange(l.getObjectModifications(modifiedChildContainer), logicalDatastoreType,
                            getIdentifier(modifiedChildContainer.getDataBefore()
                                            != null ? modifiedChildContainer.getDataBefore() : modifiedChildContainer.getDataAfter(),
                                    identifier));
                });
            });
        }
    }

    public List<DataObjectModification<C>> getModifications(DataTreeModification<P> treeModification) {
        List<DataObjectModification<C>> modifications = new ArrayList<>();
        treeModification.getRootNode().getModifiedChildren().forEach(c -> {
            if (c.getDataType().equals(container))
                modifications.add((DataObjectModification<C>) c);
        });
        return modifications;
    }

    public List<DataObjectModification<C>> getObjectModifications(DataObjectModification<P> objectModification) {
        List<DataObjectModification<C>> modifications = new ArrayList<>();
        objectModification.getModifiedChildren().forEach(c -> {
            if (c.getDataType().equals(container))
                modifications.add((DataObjectModification<C>) c);
        });
        return modifications;
    }
}
