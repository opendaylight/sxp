/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import com.google.common.net.InetAddresses;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.PrefixListFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.prefix.list.filter.entries.PrefixListEntry;

public class PrefixListFilter extends SxpBindingFilter<PrefixListFilterEntries> {

    public PrefixListFilter(SxpFilter filter, String peerGroupName) {
        super(filter, peerGroupName);
    }

    @Override public boolean filter(PrefixListFilterEntries prefixListFilterEntries, Sgt sgt, IpPrefix prefix) {
        boolean totalResult = true;
        for (PrefixListEntry prefixListEntry : prefixListFilterEntries.getPrefixListEntry()) {
            totalResult =
                    filterSgtMatch(prefixListEntry.getSgtMatch(), sgt, prefixListEntry.getEntryType(), totalResult);
        }
        return totalResult;
    }

    private boolean filterPrefixListMatch(PrefixListMatch prefixListMatch, IpPrefix prefix) {
        byte[]
                address =
                InetAddresses.forString(IpPrefixConv.toString(prefixListMatch.getIpPrefix()).split("/")[0])
                        .getAddress();
        int addressMask = Integer.parseInt(IpPrefixConv.toString(prefixListMatch.getIpPrefix()).split("/")[1]);
        byte[] binding = InetAddresses.forString(IpPrefixConv.toString(prefix).split("/")[0]).getAddress();
        int bindingMask = Integer.parseInt(IpPrefixConv.toString(prefix).split("/")[1]);

        for (int i = 0; i < addressMask; i++) {
            if (binding[i] != address[i]) {
                return true;
            }
        }
        if (prefixListMatch.getMask() != null) {
            switch (prefixListMatch.getMask().getMaskRange()) {
                case Ge:
                    if (bindingMask < addressMask)
                        return true;
                    break;
                case Le:
                    if (bindingMask > addressMask)
                        return true;
                    break;
                case G:
                    if (bindingMask <= addressMask)
                        return true;
                    break;
                case L:
                    if (bindingMask >= addressMask)
                        return true;
                    break;
                case E:
                    if (bindingMask != addressMask)
                        return true;
                    break;
            }
        }
        return false;
    }
}
