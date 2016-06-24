/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PrefixListFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.prefix.list.filter.entries.PrefixListEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilter;

import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;

/**
 * PrefixList Filter logic based on Most specific Match that supports SGT matching
 */
public class PrefixListFilter extends SxpBindingFilter<PrefixListFilterEntries> {

    /**
     * Creates PrefixList Filter that filters Bindings according to specified PrefixList
     *
     * @param filter        SxpFilter containing PrefixList entries
     * @param peerGroupName PeerGroupName of Group containing specified filter
     * @throws IllegalArgumentException If no filter entries are defined or type of entries is not supported by this implementation
     */
    public PrefixListFilter(SxpFilter filter, String peerGroupName) {
        super(filter, peerGroupName);
        if (filter.getFilterEntries() == null) {
            throw new IllegalArgumentException("Filter Entries not defined");
        }
        if (!(filter.getFilterEntries() instanceof PrefixListFilterEntries)) {
            throw new IllegalArgumentException("Filter entries of unsupported type");
        }
        PrefixListFilterEntries entries = ((PrefixListFilterEntries) filter.getFilterEntries());
        if (entries.getPrefixListEntry() != null && !entries.getPrefixListEntry().isEmpty()) {
            Collections.sort(entries.getPrefixListEntry(), new Comparator<PrefixListEntry>() {

                @Override public int compare(PrefixListEntry t1, PrefixListEntry t2) {
                    return t1.getEntrySeq().compareTo(t2.getEntrySeq());
                }
            });
        }
    }

    @Override public boolean filter(PrefixListFilterEntries prefixListFilterEntries, SxpBindingFields binding) {
        if (prefixListFilterEntries.getPrefixListEntry() == null || prefixListFilterEntries.getPrefixListEntry()
                .isEmpty()) {
            return false;
        }
        FilterEntryType entryType = FilterEntryType.Deny;
        int entryPriority = 0, sgtRank = binding.getIpPrefix().getIpv6Prefix() == null ? 32 : 128;

        for (PrefixListEntry prefixListEntry : prefixListFilterEntries.getPrefixListEntry()) {
            boolean sgtTest = filterSgtMatch(prefixListEntry.getSgtMatch(), binding.getSecurityGroupTag());
            int prefixTest = filterPrefixListMatch(prefixListEntry.getPrefixListMatch(), binding.getIpPrefix()),
                    priority = prefixTest + (sgtTest ? sgtRank : 0);

            if (priority >= entryPriority && prefixListEntry.getPrefixListMatch() != null
                    && prefixListEntry.getSgtMatch() != null && sgtTest && prefixTest != 0) {
                entryPriority = priority;
                entryType = prefixListEntry.getEntryType();
            } else if ((priority >= entryPriority) && (prefixListEntry.getPrefixListMatch() == null
                    || prefixListEntry.getSgtMatch() == null) && (sgtTest || prefixTest != 0)) {
                entryPriority = priority;
                entryType = prefixListEntry.getEntryType();
            }
        }
        return entryType.equals(FilterEntryType.Deny);
    }

    /**
     * Filter out IpPrefix according to specified PrefixList match
     *
     * @param prefixListMatch PrefixList match according to which value is filtered
     * @param prefix          IpPrefix tested
     * @return If IpPrefix will be filtered
     */
    private int filterPrefixListMatch(PrefixListMatch prefixListMatch, IpPrefix prefix) {
        if (prefixListMatch != null && (
                (prefixListMatch.getIpPrefix().getIpv4Prefix() != null && prefix.getIpv4Prefix() != null) || (
                        prefixListMatch.getIpPrefix().getIpv6Prefix() != null && prefix.getIpv6Prefix() != null))) {
            BitSet address = getBitAddress(IpPrefixConv.toString(prefixListMatch.getIpPrefix()).split("/")[0]);
            int addressMask = Integer.parseInt(IpPrefixConv.toString(prefixListMatch.getIpPrefix()).split("/")[1]);
            BitSet binding = getBitAddress(IpPrefixConv.toString(prefix).split("/")[0]);
            int bindingMask = Integer.parseInt(IpPrefixConv.toString(prefix).split("/")[1]);

            for (int i = 0; i < addressMask; i++) {
                if (binding.get(i) != address.get(i)) {
                    return 0;
                }
            }
            if (prefixListMatch.getMask() != null) {
                int mask = prefixListMatch.getMask().getMaskValue().getValue();
                switch (prefixListMatch.getMask().getMaskRange()) {
                    case Ge:
                        if (bindingMask < mask)
                            return 0;
                        break;
                    case Le:
                        if (bindingMask > mask)
                            return 0;
                        break;
                    case Eq:
                        if (bindingMask != mask)
                            return 0;
                        break;
                }
                addressMask++;
            }
            return addressMask;
        } else {
            return 0;
        }
    }
}
