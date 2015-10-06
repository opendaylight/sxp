/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.entry.AclMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.match.fields.Mask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.acl.filter.entries.AclEntry;

import java.util.BitSet;

public class AclFilter extends SxpBindingFilter<AclFilterEntries> {

    public AclFilter(SxpFilter filter, String peerGroupName) {
        super(filter, peerGroupName);
    }

    @Override public boolean filter(AclFilterEntries aclFilterEntries, Sgt sgt, IpPrefix prefix) {
        boolean totalResult = true;
        for (AclEntry aclEntry : aclFilterEntries.getAclEntry()) {
            if (aclEntry.getSgtMatch() != null && aclEntry.getAclMatch() != null) {
                boolean sgtTest = filterSgtMatch(aclEntry.getSgtMatch(), sgt, aclEntry.getEntryType(), totalResult),
                        aclTest = filterAclMatch(aclEntry.getAclMatch(), prefix, aclEntry.getEntryType(), totalResult);
                if (aclEntry.getEntryType().equals(FilterEntryType.Permit)) {
                    totalResult = sgtTest || aclTest;
                } else {
                    totalResult = sgtTest && aclTest;
                }
            } else {
                totalResult =
                        filterSgtMatch(aclEntry.getSgtMatch(), sgt, aclEntry.getEntryType(),
                                filterAclMatch(aclEntry.getAclMatch(), prefix, aclEntry.getEntryType(), totalResult));
            }
        }
        return totalResult;
    }

    private boolean filterAclMatch(AclMatch aclMatch, IpPrefix prefix, FilterEntryType entryType, boolean lastState) {
        if (aclMatch != null && (
                (aclMatch.getIpAddress().getIpv4Address() != null && aclMatch.getWildcardMask().getIpv4Address() != null
                        && prefix.getIpv4Prefix() != null) || (aclMatch.getIpAddress().getIpv6Address() != null
                        && aclMatch.getWildcardMask().getIpv6Address() != null && prefix.getIpv6Prefix() != null))) {
            boolean result = true;
            int addressLength = prefix.getIpv4Prefix() != null ? 32 : 128;
            BitSet address = getBitAddress(Search.getAddress(aclMatch.getIpAddress()));
            BitSet addressMask = getBitAddress(Search.getAddress(aclMatch.getWildcardMask()));
            BitSet binding = getBitAddress(IpPrefixConv.toString(prefix).split("/")[0]);

            for (int i = 0; i < addressLength; i++) {
                if (!addressMask.get(i) && address.get(i) != binding.get(i)) {
                    result = false;
                    break;
                }
            }
            result = filterAclMask(aclMatch.getMask(), prefix, result, addressLength);
            return entryType.equals(FilterEntryType.Deny) ? (result || lastState) : (!result && lastState);
        } else {
            return lastState;
        }
    }

    private boolean filterAclMask(Mask mask, IpPrefix prefix, boolean lastState, int addressLength) {
        if (mask != null && mask.getAddressMask() != null && mask.getWildcardMask() != null) {
            BitSet bitMask = getBitAddress(Search.getAddress(mask.getAddressMask()));
            BitSet bitWildcardMask = getBitAddress(Search.getAddress(mask.getWildcardMask()));
            int bindingMask = Integer.parseInt(IpPrefixConv.toString(prefix).split("/")[1]);
            for (int i = 0; i < addressLength; i++) {
                if (!bitWildcardMask.get(i) && bitMask.get(i) && bindingMask >= 0) {
                    bindingMask--;
                } else if (bitWildcardMask.get(i) && bindingMask > 0) {
                    bindingMask--;
                } else {
                    break;
                }
            }
            System.out.println();
            return bindingMask == 0 && lastState;

        }
        return lastState;
    }
}
