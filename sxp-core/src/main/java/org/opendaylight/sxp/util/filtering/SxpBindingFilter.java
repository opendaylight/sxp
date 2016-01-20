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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.SgtMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtMatches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.FilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.PrefixListFilterEntries;

import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/**
 * Abstract Class representing Filter to filter Master/Sxp BindingIdentities
 *
 * @param <T> Type representing entries used inside filter
 */
public abstract class SxpBindingFilter<T extends FilterEntries> {

    protected final SxpFilter sxpFilter;
    private final String peerGroupName;

    /**
     * Parametric constructor for SxpFiltering logic
     *
     * @param filter        SxpFilter containing rules for filtering
     * @param peerGroupName name of PeerGroup in which filter is assigned
     * @throws IllegalArgumentException If SxpFilter fields are not set
     */
    protected SxpBindingFilter(SxpFilter filter, String peerGroupName) {
        if (filter.getFilterType() == null || filter.getFilterEntries() == null) {
            throw new IllegalArgumentException("Filter fields aren't set properly " + filter);
        }
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
     * @throws IllegalArgumentException If filter is set do different type of filtering
     */
    public boolean filter(SxpBindingIdentity identity) {
        if (sxpFilter.getFilterType().equals(FilterType.Outbound)) {
            throw new IllegalArgumentException("Outbound filter cannot filter inbound bindings");
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
     * @throws IllegalArgumentException If filter is set do different type of filtering
     */
    public boolean filter(MasterBindingIdentity identity) {
        if (sxpFilter.getFilterType().equals(FilterType.Inbound)) {
            throw new IllegalArgumentException("Inbound filter cannot filter outbound bindings");
        }
        //noinspection unchecked
        return filter((T) sxpFilter.getFilterEntries(), identity.getPrefixGroup().getSgt(),
                identity.getBinding().getIpPrefix());
    }

    /**
     * Filters values against Match
     *
     * @param t      Match against values are filtered
     * @param sgt    Sgt value that will be compared
     * @param prefix IpPrefix value that wil be compared
     * @return If values will be filtered out
     */
    protected abstract boolean filter(T t, Sgt sgt, IpPrefix prefix);

    /**
     * Filters Sgt according to provided SgtMatch
     *
     * @param sgtMatch Match against Sgt value will be compared
     * @param sgt      Sgt that will be compared
     * @return If Sgt will be filtered out
     */
    protected boolean filterSgtMatch(SgtMatch sgtMatch, Sgt sgt) {
        if (sgtMatch instanceof SgtMatches) {
            List<Sgt> matches = ((SgtMatches) sgtMatch).getMatches();
            return matches.contains(sgt);
        } else if (sgtMatch instanceof SgtRange) {
            SgtRange range = (SgtRange) sgtMatch;
            return sgt.getValue() >= range.getSgtStart().getValue() && sgt.getValue() <= range.getSgtEnd().getValue();
        }
        return false;
    }

    /**
     * @param ip String representation of ip address
     * @return BitSet representing ip address
     */
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
     * @throws IllegalArgumentException If entries of Filter are not supported or other parameters are wrong
     */
    public static SxpBindingFilter generateFilter(
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter filter,
            String peerGroupName) throws IllegalArgumentException {
        if (peerGroupName == null) {
            throw new IllegalArgumentException("PeerGroup name cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        if (filter.getFilterEntries() instanceof AclFilterEntries) {
            return new AclFilter(new SxpFilterBuilder(filter).build(), peerGroupName);
        } else if (filter.getFilterEntries() instanceof PrefixListFilterEntries) {
            return new PrefixListFilter(new SxpFilterBuilder(filter).build(), peerGroupName);
        }
        throw new IllegalArgumentException("Undefined filter type " + filter);
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SxpBindingFilter<?> that = (SxpBindingFilter<?>) o;
        return Objects.equals(sxpFilter.getFilterType(), that.sxpFilter.getFilterType());
    }

    @Override public int hashCode() {
        return Objects.hash(sxpFilter.getFilterEntries());
    }
}
