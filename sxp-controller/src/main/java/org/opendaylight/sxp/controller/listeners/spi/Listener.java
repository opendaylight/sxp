/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.spi;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.function.Function;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Listener interface represent analogy for handling changes in DataStore,
 * listeners can be chained together and for tree listening for change.
 *
 * @param <P> Parent on which check will be performed
 * @param <C> Child that will be checked for changes
 */
public interface Listener<P extends DataObject, C extends DataObject> {

    /**
     * Adds sublistener that will be checking on Child of current listener
     *
     * @param listener sublistener that will be assigned
     * @return listener that was assigned
     */
    Listener addSubListener(Listener listener);

    /**
     * Logic handling changes in DatastoreTree
     *
     * @param modifiedChildContainer Modification of Datastore
     * @param logicalDatastoreType   Datastore type where change occured
     * @param identifier             Path of tree where change occured
     */
    void handleChange(List<DataObjectModification<C>> modifiedChildContainer, LogicalDatastoreType logicalDatastoreType,
            InstanceIdentifier<P> identifier);

    /**
     * @param treeModification Parent modification
     * @return Child modifications
     */
    List<DataObjectModification<C>> getModifications(DataTreeModification<P> treeModification);

    /**
     * @param objectModification Parent modification
     * @return Child modification
     */
    List<DataObjectModification<C>> getObjectModifications(DataObjectModification<P> objectModification);

    /**
     * Class Differences contains simple logic for checking changes in Modifications
     */
    final class Differences {

        /**
         * @param c        Modification that will be checked
         * @param function Function pointing to field that will be checked
         * @param <R>      Parent type
         * @param <T>      Child type
         * @return If Child field was changed
         */
        public static <R, T extends DataObject> boolean checkDifference(DataTreeModification<T> c,
                Function<T, R> function) {
            Preconditions.checkNotNull(c);
            Preconditions.checkNotNull(function);
            return checkDifference(c.getRootNode(), function);
        }

        /**
         * @param c        Modification that will be checked
         * @param function Function pointing to field that will be checked
         * @param <R>      Parent type
         * @param <T>      Child type
         * @return If Child field was changed
         */
        public static <R, T extends DataObject> boolean checkDifference(DataObjectModification<T> c,
                Function<T, R> function) {
            Preconditions.checkNotNull(c);
            Preconditions.checkNotNull(function);
            return checkDifference(c.getDataBefore(), c.getDataAfter(), function);
        }

        /**
         * @param before   Parent data before modification
         * @param after    Parent data after modification
         * @param function Function pointing to field that will be checked
         * @param <R>      Parent type
         * @param <T>      Child type
         * @return If Child field was changed
         */
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
