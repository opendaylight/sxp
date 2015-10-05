/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.filtering;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.entry.AclMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.match.fields.Mask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.AclFilterEntries;

public class AclFilter extends SxpBindingFilter<AclFilterEntries> {

    public AclFilter(SxpFilter filter, String peerGroupName) {
        super(filter, peerGroupName);
    }

    @Override public boolean filter(AclFilterEntries aclFilterEntries, Sgt sgt, IpPrefix prefix) {
        //TODO
        return false;
    }

    private boolean filterAclMatch(AclMatch aclMatch, IpPrefix prefix, FilterEntryType entryType, boolean lastState) {
        //TODO
        return lastState;
    }

    private boolean filterAclMask(Mask mask, IpPrefix prefix, boolean lastState, int addressLength) {
        //TODO
        return lastState;
    }
}
