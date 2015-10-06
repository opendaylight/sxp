/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.PrefixListFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.prefix.list.filter.entries.PrefixListEntry;

import java.util.BitSet;

public class PrefixListFilter extends SxpBindingFilter<PrefixListFilterEntries> {

    public PrefixListFilter(SxpFilter filter, String peerGroupName) {
        super(filter, peerGroupName);
    }

    @Override public boolean filter(PrefixListFilterEntries prefixListFilterEntries, Sgt sgt, IpPrefix prefix) {
        boolean totalResult = true;
        for (PrefixListEntry prefixListEntry : prefixListFilterEntries.getPrefixListEntry()) {
            boolean
                    sgtTest =
                    filterSgtMatch(prefixListEntry.getSgtMatch(), sgt, prefixListEntry.getEntryType(), totalResult),
                    prefixTest =
                            filterPrefixListMatch(prefixListEntry.getPrefixListMatch(), prefix,
                                    prefixListEntry.getEntryType(), totalResult);
            if (prefixListEntry.getSgtMatch() != null && prefixListEntry.getPrefixListMatch() != null) {
                if (prefixListEntry.getEntryType().equals(FilterEntryType.Permit)) {
                    totalResult = sgtTest || prefixTest;
                } else {
                    totalResult = sgtTest && prefixTest;
                }
            } else {
                totalResult =
                        filterSgtMatch(prefixListEntry.getSgtMatch(), sgt, prefixListEntry.getEntryType(),
                                filterPrefixListMatch(prefixListEntry.getPrefixListMatch(), prefix,
                                        prefixListEntry.getEntryType(), totalResult));
            }
        }
        return totalResult;
    }

    private boolean filterPrefixListMatch(PrefixListMatch prefixListMatch, IpPrefix prefix, FilterEntryType entryType,
            boolean lastState) {
        if (prefixListMatch != null && (
                (prefixListMatch.getIpPrefix().getIpv4Prefix() != null && prefix.getIpv4Prefix() != null) || (
                        prefixListMatch.getIpPrefix().getIpv6Prefix() != null && prefix.getIpv6Prefix() != null))) {
            boolean result = false;
            //TODO
            BitSet address = getBitAddress(IpPrefixConv.toString(prefixListMatch.getIpPrefix()).split("/")[0]);
            int addressMask = Integer.parseInt(IpPrefixConv.toString(prefixListMatch.getIpPrefix()).split("/")[1]);
            BitSet binding = getBitAddress(IpPrefixConv.toString(prefix).split("/")[0]);
            int bindingMask = Integer.parseInt(IpPrefixConv.toString(prefix).split("/")[1]);

            for (int i = 0; i < addressMask; i++) {
                if (binding.get(i) != address.get(i)) {
                    result = true;
                }
            }
            if (prefixListMatch.getMask() != null) {
                switch (prefixListMatch.getMask().getMaskRange()) {
                    case Ge:
                        if (bindingMask < addressMask)
                            result = true;
                        break;
                    case Le:
                        if (bindingMask > addressMask)
                            result = true;
                        break;
                    case G:
                        if (bindingMask <= addressMask)
                            result = true;
                        break;
                    case L:
                        if (bindingMask >= addressMask)
                            result = true;
                        break;
                    case E:
                        if (bindingMask != addressMask)
                            result = true;
                        break;
                }
            }
            return entryType.equals(FilterEntryType.Deny) ? (result || lastState) : (!result && lastState);
        } else {
            return lastState;
        }
    }
}
