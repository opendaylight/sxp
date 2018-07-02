/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.database;

import java.util.Comparator;
import java.util.Map;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;

public enum MasterDBBindingComparator implements Comparator<MasterDatabaseBinding> {

    INSTANCE;

    @Override
    public int compare(MasterDatabaseBinding mdb1, MasterDatabaseBinding mdb2) {
        // binding priority
        final Map<OriginType, Integer> bindingOrigins = BindingOriginsConfig.INSTANCE.getBindingOrigins();
        final Integer o1Priority = bindingOrigins.get(mdb1.getOrigin());
        final Integer o2Priority = bindingOrigins.get(mdb2.getOrigin());

        if (o1Priority == null) {
            throw new IllegalArgumentException("Cannot find binding priority: " + mdb1.getOrigin().getValue());
        }
        if (o2Priority == null) {
            throw new IllegalArgumentException("Cannot find binding priority: " + mdb2.getOrigin().getValue());
        }

        if (o1Priority < o2Priority) {
            return -1;
        }
        if (o1Priority > o2Priority) {
            return 1;
        }

        // peer sequence length
        int o1PSLength = getPeerSequenceLength(mdb1);
        int o2PSLength = getPeerSequenceLength(mdb2);
        if (o1PSLength < o2PSLength) {
            return -1;
        }
        if (o1PSLength > o2PSLength) {
            return 1;
        }

        // creation time
        if (TimeConv.toLong(mdb1.getTimestamp()) > TimeConv.toLong(mdb2.getTimestamp())) {
            return -1;
        }

        return 0;
    }

    /**
     * Get the length of the peer sequence.
     */
    public static <T extends SxpBindingFields> int getPeerSequenceLength(T b) {
        return b == null || b.getPeerSequence() == null || b.getPeerSequence().getPeer() == null ?
                0 : b.getPeerSequence().getPeer().size();
    }
}
