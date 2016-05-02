/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public abstract class SxpDataChangeListener<T extends DataObject> {

    protected static final Logger LOG = LoggerFactory.getLogger(SxpDataChangeListener.class.getName());

    protected final DatastoreAccess datastoreAccess;

    protected SxpDataChangeListener(DatastoreAccess datastoreAccess) {
        this.datastoreAccess = Preconditions.checkNotNull(datastoreAccess);
    }

    protected abstract void handleOperational(DataObjectModification<T> c,
            final InstanceIdentifier<SxpNodeIdentity> identifier);

    protected void handleConfig(DataObjectModification<T> c, final InstanceIdentifier<SxpNodeIdentity> identifier) {
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

    public abstract <R extends DataObject> Class<R> getWatchedContainer();

    protected abstract InstanceIdentifier<T> getIdentifier(T d, InstanceIdentifier<SxpNodeIdentity> parentIdentifier);

    public void handleChange(DataObjectModification<ChildOf<? super SxpNodeIdentity>> modifiedChildContainer,
            LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<SxpNodeIdentity> identifier) {
        if (modifiedChildContainer != null) {
            modifiedChildContainer.getModifiedChildren().stream().forEach(m -> {
                //noinspection unchecked
                DataObjectModification<T> c = (DataObjectModification<T>) m;
                switch (logicalDatastoreType) {
                    case OPERATIONAL:
                        handleOperational(c, identifier);
                        break;
                    case CONFIGURATION:
                        handleConfig(c, identifier);
                        break;
                }
            });
        }
    }

    public static <R, T extends DataObject> boolean checkChange(DataTreeModification<T> c, Function<T, R> function) {
        Preconditions.checkNotNull(c);
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(c.getRootNode());
        return checkChange(c.getRootNode().getDataBefore(), c.getRootNode().getDataAfter(), function);
    }

    public static <R, T extends DataObject> boolean checkChange(DataObjectModification<T> c, Function<T, R> function) {
        Preconditions.checkNotNull(c);
        Preconditions.checkNotNull(function);
        return checkChange(c.getDataBefore(), c.getDataAfter(), function);
    }

    public static <R, T extends DataObject> boolean checkChange(T before, T after, Function<T, R> function) {
        if (before == null && after == null)
            return false;
        else if (before == null || after == null)
            return true;
        R before_resp = function.apply(before), after_resp = function.apply(after);
        if (before_resp == null && after_resp == null)
            return false;
        else if (before_resp == null || after_resp == null)
            return true;
        return !before_resp.equals(after_resp);
    }
}
