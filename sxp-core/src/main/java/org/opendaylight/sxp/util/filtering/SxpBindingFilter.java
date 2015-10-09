/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import com.google.common.net.InetAddresses;
import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.database.MasterBindingIdentity;
import org.opendaylight.sxp.util.database.SxpBindingIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.SgtMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtMatches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.FilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.PrefixListFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.acl.filter.entries.AclEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.prefix.list.filter.entries.PrefixListEntry;

import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class SxpBindingFilter<T extends FilterEntries> {

    protected final SxpFilter sxpFilter;
    private final String peerGroupName;

    /**
     * Parametric constructor for SxpFiltering logic
     *
     * @param filter        SxpFilter containing rules for filtering
     * @param peerGroupName name of PeerGroup in which filter is assigned
     */
    public SxpBindingFilter(SxpFilter filter, String peerGroupName) {
        sxpFilter = filter;
        this.peerGroupName = peerGroupName;
    }

    /**
     * @return assigned SxpFilter
     */
    public SxpFilter getSxpFilter() {
        return sxpFilter;
    }

    /**
     * @return Name of PeerGroup where filter is assigned
     */
    public String getPeerGroupName() {
        return peerGroupName;
    }

    /**
     * Filters SxpBindingIdentity according to provided SxpFilter
     *
     * @param identity SxpBindingIdentity checked
     * @return If SxpBindingIdentity will be propagated
     */
    public boolean filter(SxpBindingIdentity identity) {
        if (sxpFilter.getFilterType().equals(FilterType.Outbound)) {
            return false;
        }
        //noinspection unchecked
        return filter((T) sxpFilter.getFilterEntries(), identity.getPrefixGroup().getSgt(),
                identity.getBinding().getIpPrefix());
    }

    /**
     * Filters MasterBindingIdentity according to provided SxpFilter
     *
     * @param identity MasterBindingIdentity checked
     * @return If MasterBindingIdentity will be propagated
     */
    public boolean filter(MasterBindingIdentity identity) {
        if (sxpFilter.getFilterType().equals(FilterType.Inbound)) {
            return false;
        }
        //noinspection unchecked
        return filter((T) sxpFilter.getFilterEntries(), identity.getPrefixGroup().getSgt(),
                identity.getBinding().getIpPrefix());
    }

    protected abstract boolean filter(T t, Sgt sgt, IpPrefix prefix);

    protected boolean filterSgtMatch(SgtMatch sgtMatch, Sgt sgt, FilterEntryType entryType, boolean lastState) {
        if (sgtMatch == null) {
            return lastState;
        }
        boolean result = false;
        if (sgtMatch instanceof SgtMatches) {
            List<Sgt> matches = ((SgtMatches) sgtMatch).getMatches();
            result = matches.contains(sgt);
        } else if (sgtMatch instanceof SgtRange) {
            SgtRange range = (SgtRange) sgtMatch;
            result = sgt.getValue() >= range.getSgtStart().getValue() && sgt.getValue() <= range.getSgtEnd().getValue();
        }
        return entryType.equals(FilterEntryType.Deny) ? (result || lastState) : (!result && lastState);
    }

    protected static BitSet getBitAddress(String ip) {
        byte[] address = InetAddresses.forString(ip).getAddress();
        for (int i = 0; i < address.length; i++) {
            address[i] = ArraysUtil.reverseBitsByte(address[i]);
        }
        return BitSet.valueOf(address);
    }

    /**
     * Generate wrapper for SxpFilter containing logic and
     * sort entries in SxlFilter according their sequence
     *
     * @param filter        SxpFilter used for filtering
     * @param peerGroupName Name of PeerGroup where filter is assigned
     * @return Logic for binding filtering
     * @throws IllegalArgumentException If entries of Filter are not supported
     */
    public static SxpBindingFilter generateFilter(
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter filter,
            String peerGroupName) throws IllegalArgumentException {
        if (filter.getFilterEntries() instanceof AclFilterEntries) {
            Collections.sort(((AclFilterEntries) filter.getFilterEntries()).getAclEntry(), new Comparator<AclEntry>() {

                @Override public int compare(AclEntry t1, AclEntry t2) {
                    return t1.getEntrySeq().compareTo(t2.getEntrySeq());
                }
            });
            return new AclFilter(new SxpFilterBuilder(filter).build(), peerGroupName);
        } else if (filter.getFilterEntries() instanceof PrefixListFilterEntries) {
            Collections.sort(((PrefixListFilterEntries) filter.getFilterEntries()).getPrefixListEntry(),
                    new Comparator<PrefixListEntry>() {

                        @Override public int compare(PrefixListEntry t1, PrefixListEntry t2) {
                            return t1.getEntrySeq().compareTo(t2.getEntrySeq());
                        }
                    });
            return new PrefixListFilter(new SxpFilterBuilder(filter).build(), peerGroupName);
        }
        throw new IllegalArgumentException("Undefined filter type " + filter);
    }
}
