/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.MaskRangeOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.PrefixListMask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PrefixListFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.prefix.list.filter.entries.PrefixListEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.prefix.list.filter.entries.PrefixListEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.match.fields.MaskBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.SgtMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtMatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrefixListFilterTest {

    private List<PrefixListEntry> prefixListEntryList = new ArrayList<>();
    private PrefixListFilter filter;

    @Before public void init() {
        PrefixListFilterEntriesBuilder builder = new PrefixListFilterEntriesBuilder();
        builder.setPrefixListEntry(prefixListEntryList);
        SxpFilterBuilder filterBuilder = new SxpFilterBuilder();
        filterBuilder.setFilterType(FilterType.Outbound);
        filterBuilder.setFilterEntries(builder.build());
        filter = new PrefixListFilter(filterBuilder.build(), "TEST");
    }

    private SgtMatch getSgtMatches(int... sgts) {
        SgtMatchesBuilder builder = new SgtMatchesBuilder();
        List<Sgt> sgtsList = new ArrayList<>();
        builder.setMatches(sgtsList);
        for (int i : sgts) {
            sgtsList.add(new Sgt(i));
        }
        return builder.build();
    }

    private SgtMatch getSgtRange(int start, int end) {
        SgtRangeBuilder builder = new SgtRangeBuilder();
        builder.setSgtStart(new Sgt(start));
        builder.setSgtEnd(new Sgt(end));
        return builder.build();
    }

    private PrefixListMatch getPrefixListMatch(String ipAddress) {
        return getPrefixListMatch(ipAddress, null, null);
    }

    private PrefixListMatch getPrefixListMatch(String prefix, MaskRangeOperator maskRangeOperator, Integer mask) {
        PrefixListMatchBuilder builder = new PrefixListMatchBuilder();
        if (prefix.contains(":")) {
            builder.setIpPrefix(new IpPrefix(Ipv6Prefix.getDefaultInstance(prefix)));
        } else {
            builder.setIpPrefix(new IpPrefix(Ipv4Prefix.getDefaultInstance(prefix)));
        }
        if (mask == null || maskRangeOperator == null) {
            return builder.build();
        }
        MaskBuilder maskBuilder = new MaskBuilder();
        maskBuilder.setMaskRange(maskRangeOperator);
        maskBuilder.setMaskValue(PrefixListMask.getDefaultInstance(mask.toString()));
        builder.setMask(maskBuilder.build());
        return builder.build();
    }

    private PrefixListEntry getPrefixListEntry(FilterEntryType entryType, SgtMatch sgtMatch) {
        return getPrefixListEntry(entryType, sgtMatch, null);
    }

    private PrefixListEntry getPrefixListEntry(FilterEntryType entryType, PrefixListMatch prefixListMatch) {
        return getPrefixListEntry(entryType, null, prefixListMatch);
    }

    private PrefixListEntry getPrefixListEntry(FilterEntryType entryType, SgtMatch sgtMatch,
            PrefixListMatch prefixListMatch) {
        PrefixListEntryBuilder builder = new PrefixListEntryBuilder();
        builder.setEntryType(entryType);
        builder.setSgtMatch(sgtMatch);
        builder.setPrefixListMatch(prefixListMatch);
        return builder.build();
    }

    private boolean filterOutbound(String prefix, int sgt) {
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        if (prefix.contains(":")) {
            bindingBuilder.setIpPrefix(new IpPrefix(Ipv6Prefix.getDefaultInstance(prefix)));
        } else {
            bindingBuilder.setIpPrefix(new IpPrefix(Ipv4Prefix.getDefaultInstance(prefix)));
        }
        bindingBuilder.setSecurityGroupTag(new Sgt(sgt));
        return filter.apply(bindingBuilder.build());
    }

    @Test public void testFilterSgtOnly() throws Exception {
        assertFalse(filterOutbound("0.0.0.0/32", 50));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Permit, getSgtMatches(1, 2, 10, 20, 100, 200)));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Deny, getSgtRange(25, 50)));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Permit, getSgtRange(5, 150)));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Deny, getSgtRange(10, 20)));

        assertFalse(filterOutbound("127.0.0.1/24", 50));
        assertFalse(filterOutbound("127.0.0.1/24", 150));
        assertFalse(filterOutbound("127.0.0.1/24", 25));
        assertTrue(filterOutbound("127.0.0.1/24", 10));
        assertFalse(filterOutbound("127.0.0.1/24", 200));
        assertTrue(filterOutbound("127.0.0.1/24", 186));
    }

    @Test public void testFilterPrefixListOnly() throws Exception {
        assertFalse(filterOutbound("0.0.0.0/32", 50));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Permit, getPrefixListMatch("52.12.0.5/16")));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Permit, getPrefixListMatch("53.128.0.5/9")));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Permit, getPrefixListMatch("53.1.0.5/24")));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Deny, getPrefixListMatch("53.192.0.5/10")));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Permit, getPrefixListMatch("127.10.0.0/32")));

        assertTrue(filterOutbound("127.10.0.1/32", 10));
        assertFalse(filterOutbound("127.10.0.0/32", 10));
        assertFalse(filterOutbound("52.12.3.5/32", 10));
        assertTrue(filterOutbound("52.0.0.5/32", 10));

        assertTrue(filterOutbound("53.200.0.5/32", 10));
        assertFalse(filterOutbound("53.130.0.5/32", 10));
        assertTrue(filterOutbound("53.2.0.5/32", 10));
        assertFalse(filterOutbound("53.1.0.5/32", 10));

        prefixListEntryList.add(
                getPrefixListEntry(FilterEntryType.Permit, getPrefixListMatch("2001:0:8:0:6:205:0:1/96")));
        prefixListEntryList.add(
                getPrefixListEntry(FilterEntryType.Deny, getPrefixListMatch("2001:0:8:0:6:205:0:1/16")));
        prefixListEntryList.add(
                getPrefixListEntry(FilterEntryType.Permit, getPrefixListMatch("2001:1:8:0:6:205:0:1/32")));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Permit, getPrefixListMatch("56:0:B:0:0:0:0:1/128")));

        assertTrue(filterOutbound("2001:0:8:0:6:25:F:1/128", 10));
        assertFalse(filterOutbound("2001:0:8:0:6:205:F:1/128", 10));
        assertTrue(filterOutbound("2001:10:8:0:6:5:0:1/128", 10));
        assertFalse(filterOutbound("2001:1:8:0:6:5:0:1/128", 10));

        assertFalse(filterOutbound("56:0:B:0:0:0:0:1/128", 10));
        assertTrue(filterOutbound("56:0:B:0:0:0:0:0/128", 10));
    }

    @Test public void testFilterPrefixListSgt() throws Exception {
        assertFalse(filterOutbound("0.0.0.0/32", 50));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Permit, getPrefixListMatch("16.24.0.36/24")));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Permit, getSgtMatches(20, 25, 30, 40, 150, 175),
                getPrefixListMatch("53.12.0.5/16")));
        prefixListEntryList.add(
                getPrefixListEntry(FilterEntryType.Deny, getSgtRange(10, 24), getPrefixListMatch("53.12.0.5/16")));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Deny, getSgtRange(26, 50)));
        prefixListEntryList.add(
                getPrefixListEntry(FilterEntryType.Permit, getSgtRange(130, 200), getPrefixListMatch("52.12.0.5/25")));

        assertFalse(filterOutbound("16.24.0.166/24", 51));
        assertFalse(filterOutbound("16.24.0.166/24", 25));
        assertTrue(filterOutbound("16.24.0.166/24", 26));
        assertTrue(filterOutbound("16.24.0.166/24", 50));

        assertFalse(filterOutbound("53.12.0.5/24", 30));
        assertTrue(filterOutbound("53.12.0.5/24", 20));
        assertFalse(filterOutbound("53.12.10.5/24", 150));

        assertFalse(filterOutbound("52.12.0.5/24", 150));
        assertFalse(filterOutbound("52.12.0.5/24", 200));
        assertTrue(filterOutbound("52.12.0.5/24", 201));
        assertTrue(filterOutbound("52.12.1.5/24", 200));
    }

    @Test public void testFilterExtendedPrefixList() throws Exception {
        assertFalse(filterOutbound("0.0.0.0/32", 50));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Permit,
                getPrefixListMatch("52.1.0.5/24", MaskRangeOperator.Eq, 31)));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Permit,
                getPrefixListMatch("53.1.0.5/16", MaskRangeOperator.Ge, 16)));
        prefixListEntryList.add(
                getPrefixListEntry(FilterEntryType.Deny, getPrefixListMatch("53.1.0.5/16", MaskRangeOperator.Le, 23)));
        prefixListEntryList.add(getPrefixListEntry(FilterEntryType.Deny, getSgtRange(100, 300),
                getPrefixListMatch("53.1.0.5/16", MaskRangeOperator.Ge, 29)));

        assertTrue(filterOutbound("52.1.0.5/32", 10));
        assertFalse(filterOutbound("52.1.0.5/31", 10));
        assertTrue(filterOutbound("52.1.0.5/30", 10));

        assertTrue(filterOutbound("53.1.0.5/23", 10));
        assertFalse(filterOutbound("53.1.0.5/24", 10));
        assertFalse(filterOutbound("53.1.0.5/28", 10));
        assertFalse(filterOutbound("53.1.0.5/29", 10));

        assertTrue(filterOutbound("53.1.0.5/29", 100));
        assertFalse(filterOutbound("53.1.0.5/28", 100));
    }
}
