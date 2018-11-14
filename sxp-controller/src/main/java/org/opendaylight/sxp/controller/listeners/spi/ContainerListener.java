/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.spi;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.core.SxpDatastoreNode;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ContainerListener class represent logic that handles changes on container Child
 *
 * @param <P> Parent type
 * @param <C> Container Child type
 */
public abstract class ContainerListener<P extends DataObject, C extends ChildOf<? super P>> implements Listener<P, C> {

    protected static final Logger LOG = LoggerFactory.getLogger(ContainerListener.class.getName());
    protected final DatastoreAccess datastoreAccess;
    protected final Class<C> container;
    private final List<Listener> subListeners = new ArrayList<>();

    protected ContainerListener(DatastoreAccess datastoreAccess, Class<C> container) {
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
        this.container = Preconditions.checkNotNull(container);
    }

    @Override
    public Listener addSubListener(Listener listener) {
        subListeners.add(Preconditions.checkNotNull(listener));
        return this;
    }

    /**
     * @param nodeId SxpNode identifier
     * @return DatastoreAcces associated with SxpNode or default if nothing found
     */
    protected DatastoreAccess getDatastoreAccess(String nodeId) {
        SxpNode node = Configuration.getRegisteredNode(nodeId);
        if (node instanceof SxpDatastoreNode) {
            return ((SxpDatastoreNode) node).getDatastoreAccess();
        }
        return datastoreAccess;
    }

    /**
     * @param c          Container modification object
     * @param identifier InstanceIdentifier pointing to Parent of Container
     * @param sxpNode    SxpNode provided may be null
     */
    protected abstract void handleOperational(DataObjectModification<C> c, final InstanceIdentifier<P> identifier,
            SxpNode sxpNode);

    /**
     * Mirrors changes into Operational Datastore
     *
     * @param c          Container modification object
     * @param identifier InstanceIdentifier pointing to Parent of Container
     */
    protected void handleConfig(DataObjectModification<C> c, final InstanceIdentifier<P> identifier) {
        LOG.trace("Config Modification {} {}", getClass(), c.getModificationType());
        final String
                nodeId =
                identifier.firstKeyOf(Node.class) != null ? identifier.firstKeyOf(Node.class)
                        .getNodeId()
                        .getValue() : null;
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        switch (c.getModificationType()) {
            case WRITE:
                if (c.getDataBefore() == null)
                    datastoreAccess.checkAndPut(getIdentifier(c.getDataAfter(), identifier), c.getDataAfter(),
                            LogicalDatastoreType.OPERATIONAL, false);
                else
                    datastoreAccess.merge(getIdentifier(c.getDataAfter(), identifier), c.getDataAfter(),
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

    /**
     * @param d                Data used for building InstanceIdentifier
     * @param parentIdentifier InstanceIdentifier of Parent
     * @return InstanceIdentifier pointing to child with specified values
     */
    protected abstract InstanceIdentifier<C> getIdentifier(C d, InstanceIdentifier<P> parentIdentifier);

    @Override
    public void handleChange(List<DataObjectModification<C>> modifiedChilds, LogicalDatastoreType logicalDatastoreType,
            InstanceIdentifier<P> identifier) {
        if (modifiedChilds != null && !modifiedChilds.isEmpty()) {
            modifiedChilds.stream().filter(Objects::nonNull).forEach(modifiedChildContainer -> {
                final String nodeId = identifier.firstKeyOf(Node.class).getNodeId().getValue();
                SxpNode sxpNode = Configuration.getRegisteredNode(nodeId);
                switch (logicalDatastoreType) {
                    case OPERATIONAL:
                        if (sxpNode != null) {
                            handleOperational(modifiedChildContainer, identifier, sxpNode);
                        } else {
                            LOG.error("Modification {} {} {} could not get SxpNode {}", this, logicalDatastoreType,
                                    modifiedChildContainer.getDataType(), nodeId);
                        }
                        break;
                    case CONFIGURATION:
                        handleConfig(modifiedChildContainer, identifier);
                        break;
                }
                try {
                    if (!DataObjectModification.ModificationType.DELETE.equals(
                            modifiedChildContainer.getModificationType()))
                        subListeners.forEach(l -> {
                            l.handleChange(l.getObjectModifications(modifiedChildContainer), logicalDatastoreType,
                                    getIdentifier(modifiedChildContainer.getDataBefore()
                                                    != null ? modifiedChildContainer.getDataBefore() : modifiedChildContainer.getDataAfter(),
                                            identifier));
                        });
                } catch (IllegalStateException e) {
                    LOG.debug("{} {} modifications on child not found ", identifier, logicalDatastoreType, e);
                }
            });
        }
    }

    @Override
    public List<DataObjectModification<C>> getModifications(DataTreeModification<P> treeModification) {
        List<DataObjectModification<C>> modifications = new ArrayList<>();
        if (treeModification != null) {
            treeModification.getRootNode().getModifiedChildren().forEach(c -> {
                if (c.getDataType().equals(container))
                    modifications.add((DataObjectModification<C>) c);
            });
        }
        return modifications;
    }

    @Override
    public List<DataObjectModification<C>> getObjectModifications(DataObjectModification<P> objectModification) {
        List<DataObjectModification<C>> modifications = new ArrayList<>();
        if (objectModification != null) {
            objectModification.getModifiedChildren().forEach(c -> {
                if (c.getDataType().equals(container))
                    modifications.add((DataObjectModification<C>) c);
            });
        }
        return modifications;
    }
}
