/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.net.InetAddresses;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntriesFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpDomainFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.SxpFilterFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.FilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PeerSequenceFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PrefixListFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.SgtMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtMatches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.SxpDomainFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder;

/**
 * Abstract Class representing Filter to filter Master/Sxp BindingIdentities
 *
 * @param <T> Type representing entries used inside filter
 */
public abstract class SxpBindingFilter<T extends FilterEntries, R extends FilterEntriesFields>
        implements Function<SxpBindingFields, Boolean>, Predicate<SxpBindingFields> {

    protected final R sxpFilter;
    private final String identifier;

    private SxpBindingFilter(String identifier) {
        this.sxpFilter = null;
        this.identifier = identifier;
    }

    /**
     * Parametric constructor for SxpFiltering logic
     *
     * @param filter     SxpFilter containing rules for filtering
     * @param identifier name of PeerGroup in which filter is assigned
     * @throws IllegalArgumentException If SxpFilter fields are not set
     */
    protected SxpBindingFilter(R filter, String identifier) {
        if (filter.getFilterEntries() == null) {
            throw new IllegalArgumentException("Filter fields aren't set properly " + filter);
        }
        sxpFilter = filter;
        this.identifier = identifier;
    }

    /**
     * @return assigned SxpFilter
     */
    public R getSxpFilter() {
        return sxpFilter;
    }

    /**
     * @return Name of PeerGroup where filter is assigned
     */
    public String getIdentifier() {
        return identifier;
    }

    @Override public Boolean apply(SxpBindingFields binding) {
        //noinspection unchecked
        return filter(sxpFilter != null ? (T) sxpFilter.getFilterEntries() : null, Preconditions.checkNotNull(binding));
    }

    @Override public boolean test(SxpBindingFields binding) {
        return apply(binding);
    }

    /**
     * Filters values against Match
     *
     * @param t       Match against values are filtered
     * @param binding Binding that will be compared
     * @return If values will be filtered out
     */
    protected abstract boolean filter(T t, SxpBindingFields binding);

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
    public static <T extends SxpFilterFields> SxpBindingFilter<?, ? extends SxpFilterFields> generateFilter(T filter,
            String peerGroupName) throws IllegalArgumentException {
        if (peerGroupName == null) {
            throw new IllegalArgumentException("PeerGroup name cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        if (filter.getFilterEntries() instanceof AclFilterEntries) {
            return new AclFilter<>(new SxpFilterBuilder(filter).build(), peerGroupName);
        } else if (filter.getFilterEntries() instanceof PrefixListFilterEntries) {
            return new PrefixListFilter<>(new SxpFilterBuilder(filter).build(), peerGroupName);
        } else if (filter.getFilterEntries() instanceof PeerSequenceFilterEntries) {
            return new PeerSequenceFilter<>(new SxpFilterBuilder(filter).build(), peerGroupName);
        }
        throw new IllegalArgumentException("Undefined filter type " + filter);
    }

    /**
     * Generate wrapper for SxpFilter containing logic and
     * sort entries in SxlFilter according their sequence
     *
     * @param filter     SxpFilter used for filtering
     * @param domainName Name of Domain where filter is assigned
     * @return Logic for binding filtering
     * @throws IllegalArgumentException If entries of Filter are not supported or other parameters are wrong
     */
    public static <T extends SxpDomainFilterFields> SxpBindingFilter<?, ? extends SxpDomainFilterFields> generateFilter(
            T filter, String domainName) throws IllegalArgumentException {
        if (domainName == null) {
            throw new IllegalArgumentException("Domain name cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        if (filter.getFilterEntries() instanceof AclFilterEntries) {
            return new AclFilter<>(new SxpDomainFilterBuilder(Preconditions.checkNotNull(filter)).setFilterSpecific(
                    FilterSpecific.AccessOrPrefixList).build(), domainName);
        } else if (filter.getFilterEntries() instanceof PrefixListFilterEntries) {
            return new PrefixListFilter<>(
                    new SxpDomainFilterBuilder(Preconditions.checkNotNull(filter)).setFilterSpecific(
                            FilterSpecific.AccessOrPrefixList).build(), domainName);
        } else if (filter.getFilterEntries() instanceof PeerSequenceFilterEntries) {
            return new PeerSequenceFilter<>(
                    new SxpDomainFilterBuilder(Preconditions.checkNotNull(filter)).setFilterSpecific(
                            FilterSpecific.PeerSequence).build(), domainName);
        }
        throw new IllegalArgumentException("Undefined filter type " + filter);
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SxpBindingFilter that = (SxpBindingFilter) o;
        if (sxpFilter instanceof SxpDomainFilterFields && that.sxpFilter instanceof SxpDomainFilterFields) {
            return Objects.equals(((SxpDomainFilterFields) sxpFilter).getFilterSpecific(),
                    ((SxpDomainFilterFields) that.sxpFilter).getFilterSpecific());
        } else if (sxpFilter instanceof SxpFilterFields && that.sxpFilter instanceof SxpFilterFields) {
            return Objects.equals(((SxpFilterFields) sxpFilter).getFilterType(),
                    ((SxpFilterFields) that.sxpFilter).getFilterType()) && Objects.equals(
                    ((SxpFilterFields) sxpFilter).getFilterSpecific(),
                    ((SxpFilterFields) that.sxpFilter).getFilterSpecific());
        }
        return false;
    }

    @Override public int hashCode() {
        return Objects.hash(sxpFilter.getFilterEntries());
    }

    public static SxpBindingFilter<?, ? extends SxpFilterFields> mergeFilters(
            final Collection<SxpBindingFilter<?, ? extends SxpFilterFields>> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() == 1)
            return Iterables.get(values, 0);
        StringBuilder builder = new StringBuilder().append("MultiGroup[ ");
        values.stream().map(SxpBindingFilter::getIdentifier).sorted().forEach(g -> builder.append(g).append(" "));
        //noinspection unchecked
        return new SxpBindingFilter(builder.append("]").toString()) {

            protected boolean filter(FilterEntries filterEntries, SxpBindingFields binding) {
                for (SxpBindingFilter filter : values) {
                    if (filter.apply(binding))
                        return true;
                }
                return false;
            }
        };
    }

    public static <F extends SxpFilterFields> boolean checkInCompatibility(F filter1, F filter2) {
        if (filter1 == null || filter2 == null)
            return false;
        if (filter1 == filter2)
            return true;
        if (!Preconditions.checkNotNull(filter1.getFilterType()).equals(filter2.getFilterType()))
            return false;
        return Preconditions.checkNotNull(filter1.getFilterEntries())
                .getClass()
                .equals(Preconditions.checkNotNull(filter2.getFilterEntries()).getClass()) ||
                filter1.getFilterEntries() instanceof AclFilterEntries
                        && filter2.getFilterEntries() instanceof PrefixListFilterEntries ||
                filter1.getFilterEntries() instanceof PrefixListFilterEntries
                        && filter2.getFilterEntries() instanceof AclFilterEntries;
    }

}
