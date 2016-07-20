/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import java.util.Collections;
import org.opendaylight.sxp.util.database.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntriesFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PeerSequenceFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.peer.sequence.filter.entries.PeerSequenceEntry;

/**
 * Filters Bindings according their peer sequence length
 */
public class PeerSequenceFilter<T extends FilterEntriesFields> extends SxpBindingFilter<PeerSequenceFilterEntries, T> {

    /**
     * Parametric constructor for SxpFiltering logic
     *
     * @param filter        SxpFilter containing rules for filtering
     * @param peerGroupName name of PeerGroup in which filter is assigned
     * @throws IllegalArgumentException If SxpFilter fields are not set
     */
    protected PeerSequenceFilter(T filter, String peerGroupName) {
        super(filter, peerGroupName);
        if (filter.getFilterEntries() == null) {
            throw new IllegalArgumentException("Filter Entries not defined");
        }
        if (!(filter.getFilterEntries() instanceof PeerSequenceFilterEntries)) {
            throw new IllegalArgumentException("Filter entries of unsupported type");
        }
        PeerSequenceFilterEntries entries = ((PeerSequenceFilterEntries) filter.getFilterEntries());
        if (entries.getPeerSequenceEntry() != null && !entries.getPeerSequenceEntry().isEmpty()) {
            Collections.sort(entries.getPeerSequenceEntry(), (t1, t2) -> t1.getEntrySeq().compareTo(t2.getEntrySeq()));
        }
    }

    @Override protected boolean filter(PeerSequenceFilterEntries filterEntries, SxpBindingFields binding) {
        if (filterEntries.getPeerSequenceEntry() == null || filterEntries.getPeerSequenceEntry().isEmpty() ||
                binding.getPeerSequence() == null || binding.getPeerSequence().getPeer() == null) {
            return true;
        }
        //Using First Match Logic
        int peerSeqLength = MasterDatabase.getPeerSequenceLength(binding);
        for (PeerSequenceEntry psEntry : filterEntries.getPeerSequenceEntry()) {
            boolean entryMatch = false;
            switch (psEntry.getPeerSequenceRange()) {
                case Ge:
                    entryMatch = peerSeqLength >= psEntry.getPeerSequenceLength();
                    break;
                case Le:
                    entryMatch = peerSeqLength <= psEntry.getPeerSequenceLength();
                    break;
                case Eq:
                    entryMatch = peerSeqLength == psEntry.getPeerSequenceLength();
                    break;
            }
            if (entryMatch) {
                return FilterEntryType.Deny.equals(psEntry.getEntryType());
            }
        }
        return true;
    }

}
