/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.MaskRangeOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpDomainFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PeerSequenceFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PrefixListFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.acl.filter.entries.AclEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.acl.filter.entries.AclEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.peer.sequence.filter.entries.PeerSequenceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.peer.sequence.filter.entries.PeerSequenceEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.prefix.list.filter.entries.PrefixListEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.prefix.list.filter.entries.PrefixListEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtMatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.SxpDomainFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilterBuilder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SxpBindingFilterTest {

    @Test public void testGetBitAddress() throws Exception {
        assertArrayEquals(new byte[] {-2, 0, 0, -128}, SxpBindingFilter.getBitAddress("127.0.0.1").toByteArray());
        assertArrayEquals(new byte[] {-1, -1, -1, 127},
                SxpBindingFilter.getBitAddress("255.255.255.254").toByteArray());
    }

    @Test public void testGenerateFilter() throws Exception {
        SxpBindingFilter bindingFilter = SxpBindingFilter.generateFilter(getAclFilter(FilterType.Inbound), "TEST");
        assertEquals("TEST", bindingFilter.getIdentifier());
        assertEquals(new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder(
                getAclFilter(FilterType.Inbound)).build(), bindingFilter.getSxpFilter());

        bindingFilter = SxpBindingFilter.generateFilter(getPrefixListFilter(FilterType.Inbound), "TEST1");
        assertEquals("TEST1", bindingFilter.getIdentifier());
        assertEquals(new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder(
                getPrefixListFilter(FilterType.Inbound)).build(), bindingFilter.getSxpFilter());

        bindingFilter = SxpBindingFilter.generateFilter(getPeerSequenceFilter(FilterType.Inbound), "TEST2");
        assertEquals("TEST2", bindingFilter.getIdentifier());
        assertEquals(new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder(
                getPeerSequenceFilter(FilterType.Inbound)).build(), bindingFilter.getSxpFilter());
        SxpFilter filter = null;
        try {
            SxpBindingFilter.generateFilter(filter, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            SxpBindingFilter.generateFilter(getAclFilter(FilterType.Outbound), null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            SxpBindingFilter.generateFilter(getPrefixListFilter(FilterType.Outbound), null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            SxpBindingFilter.generateFilter(getPeerSequenceFilter(FilterType.Outbound), null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            SxpBindingFilter.generateFilter(filter, "NAME");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test public void testGenerateDomainFilter() throws Exception {
        SxpBindingFilter bindingFilter = SxpBindingFilter.generateFilter(getAclFilter("basic-domain"), "TEST");
        assertEquals("TEST", bindingFilter.getIdentifier());
        assertEquals(new SxpDomainFilterBuilder(getAclFilter("basic-domain")).build(), bindingFilter.getSxpFilter());

        bindingFilter = SxpBindingFilter.generateFilter(getPrefixListFilter("basic-domain"), "TEST1");
        assertEquals("TEST1", bindingFilter.getIdentifier());
        assertEquals(new SxpDomainFilterBuilder(getPrefixListFilter("basic-domain")).build(),
                bindingFilter.getSxpFilter());

        bindingFilter = SxpBindingFilter.generateFilter(getPeerSequenceFilter("basic-domain"), "TEST2");
        assertEquals("TEST2", bindingFilter.getIdentifier());
        assertEquals(new SxpDomainFilterBuilder(getPeerSequenceFilter("basic-domain")).build(),
                bindingFilter.getSxpFilter());
        SxpFilter filter = null;
        try {
            SxpBindingFilter.generateFilter(filter, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            SxpBindingFilter.generateFilter(getAclFilter("basic-filter"), null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            SxpBindingFilter.generateFilter(getPrefixListFilter("basic-filter"), null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            SxpBindingFilter.generateFilter(getPeerSequenceFilter("basic-filter"), null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            SxpBindingFilter.generateFilter(filter, "NAME");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    private SxpFilter getAclFilter(FilterType type) {
        SxpFilterBuilder builder = new SxpFilterBuilder();
        builder.setFilterType(type);
        builder.setFilterSpecific(FilterSpecific.AccessOrPrefixList);
        AclFilterEntriesBuilder aclFilterEntriesBuilder = new AclFilterEntriesBuilder();
        ArrayList<AclEntry> aclEntries = new ArrayList<>();
        AclEntryBuilder aclEntryBuilder = new AclEntryBuilder();
        aclEntryBuilder.setEntrySeq(1);
        aclEntryBuilder.setEntryType(FilterEntryType.Permit);
        SgtMatchesBuilder matchesBuilder = new SgtMatchesBuilder();
        ArrayList<Sgt> sgts = new ArrayList<>();
        sgts.add(new Sgt(5));
        matchesBuilder.setMatches(sgts);
        aclEntryBuilder.setSgtMatch(matchesBuilder.build());
        aclEntries.add(aclEntryBuilder.build());
        aclFilterEntriesBuilder.setAclEntry(aclEntries);
        builder.setFilterEntries(aclFilterEntriesBuilder.build());
        return builder.build();
    }

    private SxpFilter getPrefixListFilter(FilterType type) {
        SxpFilterBuilder builder = new SxpFilterBuilder();
        builder.setFilterType(type);
        builder.setFilterSpecific(FilterSpecific.AccessOrPrefixList);
        PrefixListFilterEntriesBuilder entriesBuilder = new PrefixListFilterEntriesBuilder();
        ArrayList<PrefixListEntry> prefixListEntries = new ArrayList<>();
        PrefixListEntryBuilder prefixListEntryBuilder = new PrefixListEntryBuilder();
        prefixListEntryBuilder.setEntrySeq(1);
        prefixListEntryBuilder.setEntryType(FilterEntryType.Permit);
        SgtMatchesBuilder matchesBuilder = new SgtMatchesBuilder();
        ArrayList<Sgt> sgts = new ArrayList<>();
        sgts.add(new Sgt(5));
        matchesBuilder.setMatches(sgts);
        prefixListEntryBuilder.setSgtMatch(matchesBuilder.build());
        prefixListEntries.add(prefixListEntryBuilder.build());
        entriesBuilder.setPrefixListEntry(prefixListEntries);
        builder.setFilterEntries(entriesBuilder.build());
        return builder.build();
    }

    private SxpFilter getPeerSequenceFilter(FilterType type) {
        SxpFilterBuilder builder = new SxpFilterBuilder();
        builder.setFilterType(type);
        builder.setFilterSpecific(FilterSpecific.PeerSequence);
        PeerSequenceFilterEntriesBuilder entriesBuilder = new PeerSequenceFilterEntriesBuilder();
        ArrayList<PeerSequenceEntry> prefixListEntries = new ArrayList<>();
        PeerSequenceEntryBuilder peerSequenceEntryBuilder = new PeerSequenceEntryBuilder();
        peerSequenceEntryBuilder.setEntrySeq(1);
        peerSequenceEntryBuilder.setEntryType(FilterEntryType.Permit);
        peerSequenceEntryBuilder.setPeerSequenceLength(10);
        peerSequenceEntryBuilder.setPeerSequenceRange(MaskRangeOperator.Eq);
        prefixListEntries.add(peerSequenceEntryBuilder.build());
        entriesBuilder.setPeerSequenceEntry(prefixListEntries);
        builder.setFilterEntries(entriesBuilder.build());
        return builder.build();
    }

    private DomainFilter getAclFilter(String name) {
        DomainFilterBuilder builder = new DomainFilterBuilder();
        builder.setFilterName(name);
        builder.setFilterSpecific(FilterSpecific.AccessOrPrefixList);
        AclFilterEntriesBuilder aclFilterEntriesBuilder = new AclFilterEntriesBuilder();
        ArrayList<AclEntry> aclEntries = new ArrayList<>();
        AclEntryBuilder aclEntryBuilder = new AclEntryBuilder();
        aclEntryBuilder.setEntrySeq(1);
        aclEntryBuilder.setEntryType(FilterEntryType.Permit);
        SgtMatchesBuilder matchesBuilder = new SgtMatchesBuilder();
        ArrayList<Sgt> sgts = new ArrayList<>();
        sgts.add(new Sgt(5));
        matchesBuilder.setMatches(sgts);
        aclEntryBuilder.setSgtMatch(matchesBuilder.build());
        aclEntries.add(aclEntryBuilder.build());
        aclFilterEntriesBuilder.setAclEntry(aclEntries);
        builder.setFilterEntries(aclFilterEntriesBuilder.build());
        return builder.build();
    }

    private DomainFilter getPrefixListFilter(String name) {
        DomainFilterBuilder builder = new DomainFilterBuilder();
        builder.setFilterName(name);
        builder.setFilterSpecific(FilterSpecific.AccessOrPrefixList);
        PrefixListFilterEntriesBuilder entriesBuilder = new PrefixListFilterEntriesBuilder();
        ArrayList<PrefixListEntry> prefixListEntries = new ArrayList<>();
        PrefixListEntryBuilder prefixListEntryBuilder = new PrefixListEntryBuilder();
        prefixListEntryBuilder.setEntrySeq(1);
        prefixListEntryBuilder.setEntryType(FilterEntryType.Permit);
        SgtMatchesBuilder matchesBuilder = new SgtMatchesBuilder();
        ArrayList<Sgt> sgts = new ArrayList<>();
        sgts.add(new Sgt(5));
        matchesBuilder.setMatches(sgts);
        prefixListEntryBuilder.setSgtMatch(matchesBuilder.build());
        prefixListEntries.add(prefixListEntryBuilder.build());
        entriesBuilder.setPrefixListEntry(prefixListEntries);
        builder.setFilterEntries(entriesBuilder.build());
        return builder.build();
    }

    private DomainFilter getPeerSequenceFilter(String name) {
        DomainFilterBuilder builder = new DomainFilterBuilder();
        builder.setFilterName(name);
        builder.setFilterSpecific(FilterSpecific.PeerSequence);
        PeerSequenceFilterEntriesBuilder entriesBuilder = new PeerSequenceFilterEntriesBuilder();
        ArrayList<PeerSequenceEntry> prefixListEntries = new ArrayList<>();
        PeerSequenceEntryBuilder peerSequenceEntryBuilder = new PeerSequenceEntryBuilder();
        peerSequenceEntryBuilder.setEntrySeq(1);
        peerSequenceEntryBuilder.setEntryType(FilterEntryType.Permit);
        peerSequenceEntryBuilder.setPeerSequenceLength(10);
        peerSequenceEntryBuilder.setPeerSequenceRange(MaskRangeOperator.Eq);
        prefixListEntries.add(peerSequenceEntryBuilder.build());
        entriesBuilder.setPeerSequenceEntry(prefixListEntries);
        builder.setFilterEntries(entriesBuilder.build());
        return builder.build();
    }

    @Test public void testMergeFilters() throws Exception {
        List<SxpBindingFilter<?, ? extends SxpFilterFields>> filterList = new ArrayList<>();
        filterList.add(SxpBindingFilter.generateFilter(getPrefixListFilter(FilterType.Outbound), "GROUP"));
        assertEquals("GROUP", SxpBindingFilter.mergeFilters(filterList).getIdentifier());

        filterList.add(SxpBindingFilter.generateFilter(getPrefixListFilter(FilterType.Outbound), "Peers"));
        assertEquals("MultiGroup[ GROUP Peers ]", SxpBindingFilter.mergeFilters(filterList).getIdentifier());
        filterList.clear();

        filterList.add(SxpBindingFilter.generateFilter(getPrefixListFilter(FilterType.Outbound), "Peers"));
        filterList.add(SxpBindingFilter.generateFilter(getPrefixListFilter(FilterType.Outbound), "GROUP"));
        assertEquals("MultiGroup[ GROUP Peers ]", SxpBindingFilter.mergeFilters(filterList).getIdentifier());
    }

    @Test public void testMergeDomainFilters() throws Exception {
        List<SxpBindingFilter<?, ? extends SxpDomainFilterFields>> filterList = new ArrayList<>();
        filterList.add(SxpBindingFilter.generateFilter(getPrefixListFilter("basic-domain"), "GROUP"));
        assertEquals("GROUP", SxpBindingFilter.mergeFilters(filterList).getIdentifier());

        filterList.add(SxpBindingFilter.generateFilter(getPrefixListFilter("basic-domain"), "Peers"));
        assertEquals("MultiGroup[ GROUP Peers ]", SxpBindingFilter.mergeFilters(filterList).getIdentifier());
        filterList.clear();

        filterList.add(SxpBindingFilter.generateFilter(getPrefixListFilter("basic-domain"), "Peers"));
        filterList.add(SxpBindingFilter.generateFilter(getPrefixListFilter("basic-domain"), "GROUP"));
        assertEquals("MultiGroup[ GROUP Peers ]", SxpBindingFilter.mergeFilters(filterList).getIdentifier());
    }

    @Test public void testCheckInCompatibility() throws Exception {
        assertTrue(SxpBindingFilter.checkInCompatibility(getAclFilter(FilterType.Inbound),
                getAclFilter(FilterType.Inbound)));
        assertFalse(SxpBindingFilter.checkInCompatibility(getAclFilter(FilterType.Inbound),
                getAclFilter(FilterType.Outbound)));

        assertTrue(SxpBindingFilter.checkInCompatibility(getPrefixListFilter(FilterType.Inbound),
                getAclFilter(FilterType.Inbound)));
        assertFalse(SxpBindingFilter.checkInCompatibility(getPrefixListFilter(FilterType.Inbound),
                getAclFilter(FilterType.Outbound)));

        assertTrue(SxpBindingFilter.checkInCompatibility(getPrefixListFilter(FilterType.Inbound),
                getAclFilter(FilterType.Inbound)));
        assertTrue(SxpBindingFilter.checkInCompatibility(getAclFilter(FilterType.Inbound),
                getPrefixListFilter(FilterType.Inbound)));
        assertFalse(SxpBindingFilter.checkInCompatibility(getPrefixListFilter(FilterType.Inbound),
                getAclFilter(FilterType.Outbound)));
        assertFalse(SxpBindingFilter.checkInCompatibility(getAclFilter(FilterType.Inbound),
                getPrefixListFilter(FilterType.Outbound)));

        assertFalse(SxpBindingFilter.checkInCompatibility(getPeerSequenceFilter(FilterType.Inbound),
                getAclFilter(FilterType.Inbound)));
        assertFalse(SxpBindingFilter.checkInCompatibility(getPeerSequenceFilter(FilterType.Inbound),
                getPrefixListFilter(FilterType.Inbound)));
    }

    @Test public void testEquals() throws Exception {
        assertNotEquals(SxpBindingFilter.generateFilter(getAclFilter("basic-domain"), "TEST"),
                SxpBindingFilter.generateFilter(getAclFilter(FilterType.Inbound), "TEST"));

        assertNotEquals(SxpBindingFilter.generateFilter(getAclFilter("basic-domain"), "TEST"),
                SxpBindingFilter.generateFilter(getAclFilter("basic-domain"), "TEST2"));
        assertNotEquals(SxpBindingFilter.generateFilter(getAclFilter("basic-domain1"), "TEST"),
                SxpBindingFilter.generateFilter(getAclFilter("basic-domain"), "TEST"));

        assertNotEquals(SxpBindingFilter.generateFilter(getAclFilter("basic-domain"), "TEST"),
                SxpBindingFilter.generateFilter(getPeerSequenceFilter("basic-domain"), "TEST"));
        assertNotEquals(SxpBindingFilter.generateFilter(getAclFilter("basic-domain"), "TEST"),
                SxpBindingFilter.generateFilter(getPrefixListFilter("basic-domain"), "TEST"));

        assertNotEquals(SxpBindingFilter.generateFilter(getAclFilter(FilterType.Inbound), "TEST2"),
                SxpBindingFilter.generateFilter(getAclFilter(FilterType.Inbound), "TEST"));
        assertNotEquals(SxpBindingFilter.generateFilter(getAclFilter(FilterType.Outbound), "TEST"),
                SxpBindingFilter.generateFilter(getAclFilter(FilterType.Inbound), "TEST"));

        assertNotEquals(SxpBindingFilter.generateFilter(getAclFilter(FilterType.Inbound), "TEST"),
                SxpBindingFilter.generateFilter(getPrefixListFilter(FilterType.Inbound), "TEST"));
        assertNotEquals(SxpBindingFilter.generateFilter(getAclFilter(FilterType.Inbound), "TEST"),
                SxpBindingFilter.generateFilter(getPeerSequenceFilter(FilterType.Inbound), "TEST"));

        assertEquals(SxpBindingFilter.generateFilter(getAclFilter(FilterType.Outbound), "TEST"),
                SxpBindingFilter.generateFilter(getAclFilter(FilterType.Outbound), "TEST"));
        assertEquals(SxpBindingFilter.generateFilter(getAclFilter("basic-domain"), "TEST"),
                SxpBindingFilter.generateFilter(getAclFilter("basic-domain"), "TEST"));
    }
}
