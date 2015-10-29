/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.PrefixListFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;

public class PrefixListFilter extends SxpBindingFilter<PrefixListFilterEntries> {

    public PrefixListFilter(SxpFilter filter, String peerGroupName) {
        super(filter, peerGroupName);
    }

    @Override public boolean filter(PrefixListFilterEntries prefixListFilterEntries, Sgt sgt, IpPrefix prefix) {
        //TODO
        return false;
    }

    private boolean filterPrefixListMatch(PrefixListMatch prefixListMatch, IpPrefix prefix, FilterEntryType entryType,
            boolean lastState) {
        //TODO
        return lastState;
    }
}
