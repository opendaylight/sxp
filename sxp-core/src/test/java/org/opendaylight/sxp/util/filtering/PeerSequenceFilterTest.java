/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntriesFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.MaskRangeOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PeerSequenceFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PeerSequenceFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.peer.sequence.filter.entries.PeerSequenceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.peer.sequence.filter.entries.PeerSequenceEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder;

public class PeerSequenceFilterTest {

    private List<PeerSequenceEntry> sequenceEntries = new ArrayList<>();
    private PeerSequenceFilter filter;

    @Before
    public void init() {
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

    @Test
    public void testFilter() throws Exception {
        sequenceEntries.add(getPeerEntry(FilterEntryType.Deny, MaskRangeOperator.Ge, 150));
        sequenceEntries.add(getPeerEntry(FilterEntryType.Deny, MaskRangeOperator.Eq, 15));
        sequenceEntries.add(getPeerEntry(FilterEntryType.Deny, MaskRangeOperator.Le, 3));
        sequenceEntries.add(getPeerEntry(FilterEntryType.Permit, MaskRangeOperator.Eq, 5));
        sequenceEntries.add(getPeerEntry(FilterEntryType.Permit, MaskRangeOperator.Ge, 10));

        assertFalse(filter.apply(getBinding(5)));
        assertFalse(filter.apply(getBinding(10)));
        assertFalse(filter.apply(getBinding(50)));
        assertTrue(filter.apply(getBinding(0)));
        assertTrue(filter.apply(getBinding(15)));
        assertTrue(filter.apply(getBinding(150)));
        assertTrue(filter.apply(getBinding(200)));
        assertTrue(filter.apply(getBinding(3)));
    }

    @Test
    public void testFilterOnNullInputs() {
        PeerSequenceFilterEntries psfNullEntries = new PeerSequenceFilterEntriesBuilder().build();
        SxpDatabaseBinding nullBinding = new SxpDatabaseBindingBuilder().build();
        PeerSequenceFilterEntries psfEmptyEntries = new PeerSequenceFilterEntriesBuilder()
                .setPeerSequenceEntry(new ArrayList<>()).build();
        SxpDatabaseBinding emptyBinding = new SxpDatabaseBindingBuilder()
                .setPeerSequence(new PeerSequenceBuilder().build()).build();
        assertTrue(filter.filter(psfNullEntries, nullBinding));
        assertTrue(filter.filter(psfEmptyEntries, emptyBinding));
        assertTrue(filter.filter(psfEmptyEntries, nullBinding));
        assertTrue(filter.filter(psfNullEntries, emptyBinding));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBadInput() {
        FilterEntriesFields fefMock = mock(FilterEntriesFields.class);
        when(fefMock.getFilterEntries()).thenReturn(null);
        new PeerSequenceFilter<>(fefMock, "default");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBadInput2() {
        AclFilterEntries aclFilterEntries = new AclFilterEntriesBuilder().build();
        SxpFilterBuilder filterBuilder = new SxpFilterBuilder();
        filterBuilder.setFilterType(FilterType.Outbound);
        filterBuilder.setFilterEntries(aclFilterEntries);
        new PeerSequenceFilter<>(filterBuilder.build(), "default");
    }
}
