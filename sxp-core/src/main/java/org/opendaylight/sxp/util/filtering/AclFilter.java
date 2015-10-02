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
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.entry.AclMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.acl.filter.entries.AclEntry;

public class AclFilter extends SxpBindingFilter<AclFilterEntries> {

    public AclFilter(SxpFilter filter, String peerGroupName) {
        super(filter, peerGroupName);
    }

    @Override public boolean filter(AclFilterEntries aclFilterEntries, Sgt sgt, IpPrefix prefix) {
        boolean totalResult = true;
        for (AclEntry aclEntry : aclFilterEntries.getAclEntry()) {
            totalResult = filterSgtMatch(aclEntry.getSgtMatch(), sgt, aclEntry.getEntryType(), totalResult);
        }
        return totalResult;
    }

    private boolean filterAclMatch(AclMatch aclMatch, IpPrefix prefix) {

        byte[] address = InetAddresses.forString(Search.getAddress(aclMatch.getIpAddress())).getAddress();
        byte[] addresMask = InetAddresses.forString(Search.getAddress(aclMatch.getWildcardMask())).getAddress();
        byte[] binding = InetAddresses.forString(IpPrefixConv.toString(prefix).split("/")[0]).getAddress();

        if (address.length != addresMask.length || address.length != binding.length) {
            return false;
        }

        for (int i = 0; i < binding.length; i++) {
            if (addresMask[i] == 0 && address[i] != binding[i]) {
                return true;
            }
        }
        return false;
    }
}
