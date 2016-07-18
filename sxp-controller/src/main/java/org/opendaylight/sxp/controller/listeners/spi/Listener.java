/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.spi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Ordering;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.entry.AclMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.FilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PeerSequenceFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PrefixListFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.acl.filter.entries.AclEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.peer.sequence.filter.entries.PeerSequenceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.prefix.list.filter.entries.PrefixListEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.SgtMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtMatches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtRange;
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
         * @param before Data before
         * @param after  Data after
         * @param <T>    Type of data
         * @return If list containing data has changed
         */
        public static <T extends DataObject> boolean checkDifference(List<T> before, List<T> after) {
            if (before == null && after == null)
                return true;
            if (before == null || after == null)
                return false;
            if (before.size() != after.size())
                return true;
            if (before.isEmpty() && after.isEmpty())
                return false;
            return !new HashSet<>(before).containsAll(after);
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

        private static boolean checkSgtMatch(SgtMatch match1, SgtMatch match2) {
            if (match1 instanceof SgtMatches && match2 instanceof SgtMatches) {
                SgtMatches match_1 = (SgtMatches) match1, match_2 = (SgtMatches) match2;
                if (match_1.getMatches() == null && match_2.getMatches() == null)
                    return false;
                if (match_1.getMatches() == null || match_2.getMatches() == null ||
                        match_1.getMatches().size() != match_2.getMatches().size())
                    return true;
                Ordering<Sgt> ordering = new Ordering<Sgt>() {

                    @Override public int compare(@Nullable Sgt left, @Nullable Sgt right) {
                        return Integer.compare(left == null ? 0 : left.getValue(),
                                right == null ? 0 : right.getValue());
                    }
                };
                Collections.sort(match_1.getMatches(), ordering);
                Collections.sort(match_2.getMatches(), ordering);
                for (int i = 0; i < match_1.getMatches().size(); i++) {
                    if (!Objects.equals(match_1.getMatches().get(i).getValue(), match_2.getMatches().get(i).getValue()))
                        return true;
                }
                return false;
            } else if (match1 instanceof SgtRange && match2 instanceof SgtRange) {
                SgtRange match_1 = (SgtRange) match1, match_2 = (SgtRange) match2;
                return !Objects.equals(match_1.getSgtStart(), match_2.getSgtStart()) || !Objects.equals(
                        match_1.getSgtEnd(), match_2.getSgtEnd());
            } else
                return true;
        }

        private static boolean checkAclMatch(AclMatch match1, AclMatch match2) {
            return !Objects.equals(match1.getIpAddress(), match2.getIpAddress()) || !Objects.equals(match1.getMask(),
                    match2.getMask()) || !Objects.equals(match1.getWildcardMask(), match2.getWildcardMask());
        }

        private static boolean checkPrefixListMatch(PrefixListMatch match1, PrefixListMatch match2) {
            return !Objects.equals(match1.getMask(), match2.getMask()) || !Objects.equals(match1.getIpPrefix(),
                    match2.getIpPrefix());
        }

        public static boolean checkFilterEntries(FilterEntries entries1, FilterEntries entries2) {
            if (entries1 instanceof AclFilterEntries && entries2 instanceof AclFilterEntries) {
                AclFilterEntries entries_1 = (AclFilterEntries) entries1, entries_2 = (AclFilterEntries) entries2;
                if (entries_1.getAclEntry() == null && entries_2.getAclEntry() == null)
                    return false;
                if (entries_1.getAclEntry() == null || entries_2.getAclEntry() == null
                        || entries_1.getAclEntry().size() != entries_2.getAclEntry().size())
                    return true;
                Ordering<AclEntry> ordering = new Ordering<AclEntry>() {

                    @Override public int compare(@Nullable AclEntry left, @Nullable AclEntry right) {
                        return Integer.compare(left == null ? 0 : left.getEntrySeq(),
                                right == null ? 0 : right.getEntrySeq());
                    }
                };
                //FROM THIS POINT SIZE OF BOTH IS SAME
                Collections.sort(entries_1.getAclEntry(), ordering);
                Collections.sort(entries_2.getAclEntry(), ordering);
                for (int i = 0; i < entries_1.getAclEntry().size(); i++) {
                    AclEntry entry1 = entries_1.getAclEntry().get(i),
                            entry2 = entries_2.getAclEntry().get(i);
                    if (!Objects.equals(entry1.getEntrySeq(), entry2.getEntrySeq()))
                        return true;
                    if (!Objects.equals(entry1.getEntryType(), entry2.getEntryType()))
                        return true;
                    if (checkSgtMatch(entry1.getSgtMatch(), entry2.getSgtMatch()))
                        return true;
                    if (checkAclMatch(entry1.getAclMatch(), entry2.getAclMatch()))
                        return true;
                }
            } else if (entries1 instanceof PrefixListFilterEntries && entries2 instanceof PrefixListFilterEntries) {
                PrefixListFilterEntries entries_1 = (PrefixListFilterEntries) entries1,
                        entries_2 = (PrefixListFilterEntries) entries2;
                if (entries_1.getPrefixListEntry() == null && entries_2.getPrefixListEntry() == null)
                    return false;
                if (entries_1.getPrefixListEntry() == null || entries_2.getPrefixListEntry() == null
                        || entries_1.getPrefixListEntry().size() != entries_2.getPrefixListEntry().size())
                    return true;
                Ordering<PrefixListEntry> ordering = new Ordering<PrefixListEntry>() {

                    @Override public int compare(@Nullable PrefixListEntry left, @Nullable PrefixListEntry right) {
                        return Integer.compare(left == null ? 0 : left.getEntrySeq(),
                                right == null ? 0 : right.getEntrySeq());
                    }
                };
                //FROM THIS POINT SIZE OF BOTH IS SAME
                Collections.sort(entries_1.getPrefixListEntry(), ordering);
                Collections.sort(entries_2.getPrefixListEntry(), ordering);
                for (int i = 0; i < entries_1.getPrefixListEntry().size(); i++) {
                    PrefixListEntry entry1 = entries_1.getPrefixListEntry().get(i),
                            entry2 = entries_2.getPrefixListEntry().get(i);
                    if (!Objects.equals(entry1.getEntrySeq(), entry2.getEntrySeq()))
                        return true;
                    if (!Objects.equals(entry1.getEntryType(), entry2.getEntryType()))
                        return true;
                    if (checkSgtMatch(entry1.getSgtMatch(), entry2.getSgtMatch()))
                        return true;
                    if (checkPrefixListMatch(entry1.getPrefixListMatch(), entry2.getPrefixListMatch()))
                        return true;
                }
            } else if (entries1 instanceof PeerSequenceFilterEntries && entries2 instanceof PeerSequenceFilterEntries) {
                PeerSequenceFilterEntries entries_1 = (PeerSequenceFilterEntries) entries1,
                        entries_2 = (PeerSequenceFilterEntries) entries2;
                if (entries_1.getPeerSequenceEntry() == null && entries_2.getPeerSequenceEntry() == null)
                    return false;
                if (entries_1.getPeerSequenceEntry() == null || entries_2.getPeerSequenceEntry() == null
                        || entries_1.getPeerSequenceEntry().size() != entries_2.getPeerSequenceEntry().size())
                    return true;
                Ordering<PeerSequenceEntry> ordering = new Ordering<PeerSequenceEntry>() {

                    @Override public int compare(@Nullable PeerSequenceEntry left, @Nullable PeerSequenceEntry right) {
                        return Integer.compare(left == null ? 0 : left.getEntrySeq(),
                                right == null ? 0 : right.getEntrySeq());
                    }
                };
                //FROM THIS POINT SIZE OF BOTH IS SAME
                Collections.sort(entries_1.getPeerSequenceEntry(), ordering);
                Collections.sort(entries_2.getPeerSequenceEntry(), ordering);
                for (int i = 0; i < entries_1.getPeerSequenceEntry().size(); i++) {
                    PeerSequenceEntry entry1 = entries_1.getPeerSequenceEntry().get(i),
                            entry2 = entries_2.getPeerSequenceEntry().get(i);
                    if (!Objects.equals(entry1.getEntrySeq(), entry2.getEntrySeq()))
                        return true;
                    if (!Objects.equals(entry1.getEntryType(), entry2.getEntryType()))
                        return true;
                    if (!Objects.equals(entry1.getPeerSequenceLength(), entry2.getPeerSequenceLength()))
                        return true;
                    if (!Objects.equals(entry1.getPeerSequenceRange(), entry2.getPeerSequenceRange()))
                        return true;
                }
            } else
                return true;
            return false;
        }
    }
}
