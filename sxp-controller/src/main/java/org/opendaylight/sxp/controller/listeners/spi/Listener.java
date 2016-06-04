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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.List;
import java.util.function.Function;

public interface Listener<P extends DataObject, C extends DataObject> {

    Listener addSubListener(Listener listener);

    void handleChange(List<DataObjectModification<C>> modifiedChildContainer, LogicalDatastoreType logicalDatastoreType,
            InstanceIdentifier<P> identifier);

    List<DataObjectModification<C>> getModifications(DataTreeModification<P> treeModification);

    List<DataObjectModification<C>> getObjectModifications(DataObjectModification<P> objectModification);

    final class Differences {

        public static <R, T extends DataObject> boolean checkDifference(DataTreeModification<T> c,
                Function<T, R> function) {
            Preconditions.checkNotNull(c);
            Preconditions.checkNotNull(function);
            Preconditions.checkNotNull(c.getRootNode());
            return checkDifference(c.getRootNode().getDataBefore(), c.getRootNode().getDataAfter(), function);
        }

        public static <R, T extends DataObject> boolean checkDifference(DataObjectModification<T> c,
                Function<T, R> function) {
            Preconditions.checkNotNull(c);
            Preconditions.checkNotNull(function);
            return checkDifference(c.getDataBefore(), c.getDataAfter(), function);
        }

        public static <R, T extends DataObject> boolean checkDifference(T before, T after, Function<T, R> function) {
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
}
