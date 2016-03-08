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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.WildcardMask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.entry.AclMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.entry.AclMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.match.fields.MaskBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.SgtMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtMatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.AclFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.acl.filter.entries.AclEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.acl.filter.entries.AclEntryBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AclFilterTest {

    private List<AclEntry> aclEntryList = new ArrayList<>();
    private AclFilter filter;

    @Before public void init() {
        AclFilterEntriesBuilder builder = new AclFilterEntriesBuilder();
        builder.setAclEntry(aclEntryList);
        SxpFilterBuilder filterBuilder = new SxpFilterBuilder();
        filterBuilder.setFilterType(FilterType.Outbound);
        filterBuilder.setFilterEntries(builder.build());
        filter = new AclFilter(filterBuilder.build(), "TEST");
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

    private AclMatch getAclMatch(String ipAddress, String wildCardMask) {
        return getAclMatch(ipAddress, wildCardMask, null, null);
    }

    private AclMatch getAclMatch(String ipAddress, String wildCardMask, String mask, String maskWildcard) {
        AclMatchBuilder builder = new AclMatchBuilder();
        if (ipAddress.contains(":")) {
            builder.setIpAddress(new IpAddress(Ipv6Address.getDefaultInstance(ipAddress)));
            builder.setWildcardMask(new WildcardMask(Ipv6Address.getDefaultInstance(wildCardMask)));
        } else {
            builder.setIpAddress(new IpAddress(Ipv4Address.getDefaultInstance(ipAddress)));
            builder.setWildcardMask(new WildcardMask(Ipv4Address.getDefaultInstance(wildCardMask)));
        }
        if (mask == null || maskWildcard == null) {
            return builder.build();
        }
        MaskBuilder maskBuilder = new MaskBuilder();
        if (mask.contains(":")) {
            maskBuilder.setAddressMask(new IpAddress(Ipv6Address.getDefaultInstance(mask)));
            maskBuilder.setWildcardMask(new WildcardMask(Ipv6Address.getDefaultInstance(maskWildcard)));
        } else {
            maskBuilder.setAddressMask(new IpAddress(Ipv4Address.getDefaultInstance(mask)));
            maskBuilder.setWildcardMask(new WildcardMask(Ipv4Address.getDefaultInstance(maskWildcard)));
        }
        builder.setMask(maskBuilder.build());
        return builder.build();
    }

    private AclEntry getAclEntry(FilterEntryType entryType, SgtMatch sgtMatch) {
        return getAclEntry(entryType, sgtMatch, null);
    }

    private AclEntry getAclEntry(FilterEntryType entryType, AclMatch aclMatch) {
        return getAclEntry(entryType, null, aclMatch);
    }

    private AclEntry getAclEntry(FilterEntryType entryType, SgtMatch sgtMatch, AclMatch aclMatch) {
        AclEntryBuilder builder = new AclEntryBuilder();
        builder.setEntryType(entryType);
        builder.setSgtMatch(sgtMatch);
        builder.setAclMatch(aclMatch);
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
        return filter.filter(bindingBuilder.build());
    }

    @Test public void testFilterSgtOnly() throws Exception {
        aclEntryList.add(getAclEntry(FilterEntryType.Permit, getSgtMatches(1, 2, 10, 20, 100, 200)));
        aclEntryList.add(getAclEntry(FilterEntryType.Deny, getSgtRange(25, 50)));
        aclEntryList.add(getAclEntry(FilterEntryType.Permit, getSgtRange(5, 150)));
        aclEntryList.add(getAclEntry(FilterEntryType.Deny, getSgtRange(10, 20)));

        assertTrue(filterOutbound("127.0.0.1/24", 50));
        assertFalse(filterOutbound("127.0.0.1/24", 150));
        assertTrue(filterOutbound("127.0.0.1/24", 25));
        assertFalse(filterOutbound("127.0.0.1/24", 10));
        assertFalse(filterOutbound("127.0.0.1/24", 200));
        assertTrue(filterOutbound("127.0.0.1/24", 186));
    }

    @Test public void testFilterAclOnly() throws Exception {
        aclEntryList.add(getAclEntry(FilterEntryType.Permit, getAclMatch("52.12.0.5", "0.255.0.0")));
        aclEntryList.add(getAclEntry(FilterEntryType.Deny, getAclMatch("53.12.0.5", "0.254.0.0")));
        aclEntryList.add(getAclEntry(FilterEntryType.Permit, getAclMatch("53.1.0.5", "0.254.0.0")));
        aclEntryList.add(getAclEntry(FilterEntryType.Permit, getAclMatch("127.10.0.0", "0.0.0.0")));

        assertTrue(filterOutbound("127.0.0.1/32", 10));
        assertFalse(filterOutbound("127.10.0.0/24", 10));
        assertTrue(filterOutbound("52.12.3.5/24", 10));
        assertFalse(filterOutbound("52.0.0.5/12", 10));

        assertTrue(filterOutbound("53.128.0.5/32", 10));
        assertFalse(filterOutbound("53.127.0.5/24", 10));
        assertTrue(filterOutbound("53.2.0.5/12", 10));
        assertFalse(filterOutbound("53.1.0.5/24", 10));

        aclEntryList.add(
                getAclEntry(FilterEntryType.Permit, getAclMatch("2001:0:8:0:6:205:0:1", "0:0:0:0:0:0:FFFF:0")));
        aclEntryList.add(getAclEntry(FilterEntryType.Deny, getAclMatch("2001:0:8:0:6:205:0:1", "0:FFFE:0:0:0:0:0:0")));
        aclEntryList.add(getAclEntry(FilterEntryType.Permit, getAclMatch("2001:1:8:0:6:205:0:1", "0:FFFE:0:0:0:0:0:0")));
        aclEntryList.add(getAclEntry(FilterEntryType.Permit, getAclMatch("56:0:B:0:0:0:0:1", "0:0:0:0:0:0:0:0")));

        assertTrue(filterOutbound("2001:0:8:0:6:25:0:1/128", 10));
        assertFalse(filterOutbound("2001:0:8:0:6:205:F:1/32", 10));
        assertTrue(filterOutbound("2001:158:8:0:6:205:0:1/128", 10));
        assertFalse(filterOutbound("2001:33:8:0:6:205:0:1/128", 10));
        assertFalse(filterOutbound("56:0:B:0:0:0:0:1/128", 10));
        assertTrue(filterOutbound("56:0:B:0:0:0:0:0/64", 10));
    }

    @Test public void testFilterAclSgt() throws Exception {
        aclEntryList.add(getAclEntry(FilterEntryType.Deny, getSgtMatches(20, 25, 30, 40),
                getAclMatch("53.12.0.5", "255.254.0.0")));
        aclEntryList.add(getAclEntry(FilterEntryType.Permit, getSgtRange(5, 100)));
        aclEntryList.add(
                getAclEntry(FilterEntryType.Permit, getSgtRange(130, 200), getAclMatch("52.12.0.5", "255.254.0.0")));
        aclEntryList.add(getAclEntry(FilterEntryType.Permit, getAclMatch("16.24.0.36", "0.0.0.250")));

        assertFalse(filterOutbound("16.24.0.166/24", 2));
        assertTrue(filterOutbound("16.24.0.16/24", 2));
        assertFalse(filterOutbound("19.3.0.0/24", 15));
        assertTrue(filterOutbound("19.3.0.0/24", 915));

        assertFalse(filterOutbound("52.8.0.5/24", 160));
        assertTrue(filterOutbound("52.8.0.5/24", 260));
        assertTrue(filterOutbound("52.9.0.5/24", 160));

        assertTrue(filterOutbound("53.24.0.5/24", 20));
        assertFalse(filterOutbound("53.24.0.5/24", 200));
        assertFalse(filterOutbound("53.23.0.5/24", 20));

        assertTrue(filterOutbound("180.34.0.5/24", 250));
        assertFalse(filterOutbound("180.34.0.5/24", 175));
        assertFalse(filterOutbound("180.33.0.5/24", 25));

        assertTrue(filterOutbound("3.0.0.5/24", 4));
        assertFalse(filterOutbound("3.0.0.5/24", 140));
        assertFalse(filterOutbound("3.1.0.5/24", 40));
    }

    @Test public void testFilterExtendedAcl() throws Exception {
        aclEntryList.add(
                getAclEntry(FilterEntryType.Deny, getAclMatch("53.12.0.5", "0.254.0.0", "255.255.255.0", "0.0.0.243")));
        aclEntryList.add(getAclEntry(FilterEntryType.Permit,
                getAclMatch("52.12.0.5", "255.255.0.0", "255.255.255.0", "0.0.1.255")));
        aclEntryList.add(getAclEntry(FilterEntryType.Permit,
                getAclMatch("127.150.0.0", "0.0.0.0", "255.255.255.255", "0.254.0.0")));

        assertTrue(filterOutbound("127.150.0.0/7", 10));
        assertFalse(filterOutbound("127.150.0.0/8", 10));
        assertFalse(filterOutbound("127.150.0.0/14", 10));
        assertTrue(filterOutbound("127.150.0.0/15", 10));
        assertTrue(filterOutbound("127.150.0.0/16", 10));

        assertTrue(filterOutbound("52.12.3.5/24", 10));
        assertTrue(filterOutbound("52.0.0.5/12", 10));

        assertFalse(filterOutbound("53.128.0.5/32", 10));
        assertTrue(filterOutbound("53.128.0.5/24", 10));
        assertFalse(filterOutbound("53.127.0.5/24", 10));

        assertTrue(filterOutbound("53.2.0.5/26", 10));
        assertFalse(filterOutbound("53.1.0.5/29", 10));

        assertFalse(filterOutbound("53.128.0.5/23", 10));
        assertTrue(filterOutbound("53.128.0.5/24", 10));
        assertTrue(filterOutbound("53.128.0.5/28", 10));
        assertFalse(filterOutbound("53.128.0.5/29", 10));

        assertFalse(filterOutbound("53.1.0.5/23", 10));
        assertFalse(filterOutbound("53.1.0.5/24", 10));
        assertFalse(filterOutbound("53.1.0.5/28", 10));
        assertFalse(filterOutbound("53.1.0.5/29", 10));
    }
}
