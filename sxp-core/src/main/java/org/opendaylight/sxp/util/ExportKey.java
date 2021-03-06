/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util;

import java.util.List;
import java.util.Objects;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

/**
 * ExportKey class used for grouping of connections for Binding export based on version export status and group name
 */
public class ExportKey {

    /**
     * Version
     */
    private final Version version;
    /**
     * Group name
     */
    private final String groupName;
    private final List<CapabilityType> capabilityTypes;

    /**
     * Parametric constructor for ExportKey class
     *
     * @param connection SxpConnection containing data necessary for Key generation
     */
    public ExportKey(SxpConnection connection) {
        this.version = connection.getVersion();
        this.groupName = connection.getGroupName(FilterType.Outbound);
        this.capabilityTypes = connection.getCapabilitiesRemote();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExportKey exportKey = (ExportKey) o;
        return Objects.equals(version, exportKey.version) && Objects.equals(groupName, exportKey.groupName)
                && capabilityTypes.containsAll(exportKey.capabilityTypes) && exportKey.capabilityTypes.containsAll(
                capabilityTypes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = Objects.hash(version, groupName);
        for (CapabilityType type : capabilityTypes) {
            hash += Objects.hash(type);
        }
        return hash;
    }
}
