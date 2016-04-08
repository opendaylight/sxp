/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.MaskRangeOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.PeerSequenceFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.peer.sequence.filter.entries.PeerSequenceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.peer.sequence.filter.entries.PeerSequenceEntryBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PeerSequenceFilterTest {

    private List<PeerSequenceEntry> sequenceEntries = new ArrayList<>();
    private PeerSequenceFilter filter;

    @Before public void init() {
        PeerSequenceFilterEntriesBuilder builder = new PeerSequenceFilterEntriesBuilder();
        builder.setPeerSequenceEntry(sequenceEntries);
        SxpFilterBuilder filterBuilder = new SxpFilterBuilder();
        filterBuilder.setFilterType(FilterType.Outbound);
        filterBuilder.setFilterEntries(builder.build());
        filter = new PeerSequenceFilter(filterBuilder.build(), "TEST");
    }

    private PeerSequenceEntry getPeerEntry(FilterEntryType type, MaskRangeOperator operator, int range) {
        PeerSequenceEntryBuilder entryBuilder = new PeerSequenceEntryBuilder();
        entryBuilder.setPeerSequenceLength(range);
        entryBuilder.setPeerSequenceRange(operator);
        entryBuilder.setEntryType(type);
        return entryBuilder.build();
    }

    private SxpBindingFields getBinding(int peerSeqLength) {
        SxpDatabaseBindingBuilder bindingBuilder = new SxpDatabaseBindingBuilder();
        List<Peer> peerList = new ArrayList<>();
        for (int i = 0; i < peerSeqLength; i++) {
            peerList.add(new PeerBuilder().build());
        }
        bindingBuilder.setPeerSequence(new PeerSequenceBuilder().setPeer(peerList).build());
        return bindingBuilder.build();
    }

    @Test public void testFilter() throws Exception {
        sequenceEntries.add(getPeerEntry(FilterEntryType.Deny, MaskRangeOperator.Ge, 150));
        sequenceEntries.add(getPeerEntry(FilterEntryType.Deny, MaskRangeOperator.Eq, 15));
        sequenceEntries.add(getPeerEntry(FilterEntryType.Permit, MaskRangeOperator.Eq, 5));
        sequenceEntries.add(getPeerEntry(FilterEntryType.Permit, MaskRangeOperator.Ge, 10));

        assertFalse(filter.apply(getBinding(5)));
        assertFalse(filter.apply(getBinding(10)));
        assertFalse(filter.apply(getBinding(50)));
        assertTrue(filter.apply(getBinding(0)));
        assertTrue(filter.apply(getBinding(15)));
        assertTrue(filter.apply(getBinding(150)));
        assertTrue(filter.apply(getBinding(200)));
    }
}
