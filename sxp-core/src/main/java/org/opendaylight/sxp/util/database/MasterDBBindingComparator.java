/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.database;

import static org.opendaylight.sxp.util.database.MasterDatabase.getPeerSequenceLength;

import java.util.Comparator;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;

public enum MasterDBBindingComparator implements Comparator<MasterDatabaseBinding> {

    INSTANCE;

    @Override
    public int compare(MasterDatabaseBinding mdb1, MasterDatabaseBinding mdb2) {
        if ((mdb1.getOrigin() == OriginType.LOCAL) && (mdb2.getOrigin() == OriginType.NETWORK)) {
            return -1;
        }
        if ((mdb1.getOrigin() == OriginType.NETWORK) && (mdb2.getOrigin() == OriginType.LOCAL)) {
            return 1;
        }
        int o1PSLength = getPeerSequenceLength(mdb1);
        int o2PSLength = getPeerSequenceLength(mdb2);
        if (o1PSLength < o2PSLength) {
            return -1;
        }
        if (o1PSLength > o2PSLength) {
            return 1;
        }
        if (TimeConv.toLong(mdb1.getTimestamp()) > TimeConv.toLong(mdb2.getTimestamp())) {
            return -1;
        }
        return 0;
    }
}
