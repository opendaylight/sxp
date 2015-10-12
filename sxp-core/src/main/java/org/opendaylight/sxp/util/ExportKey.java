/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

import java.util.Objects;

/**
 * ExportKey class used for grouping of connections for Binding export based on version export status and group name
 */
public class ExportKey {

    private final Version version;
    private final boolean exportAll;
    private final String groupName;

    /**
     * Parametric constructor for ExportKey class
     *
     * @param version   Version of Connections
     * @param exportAll If all binding were exported
     * @param groupName Name of group if any
     */
    public ExportKey(Version version, boolean exportAll, String groupName) {
        this.version = version;
        this.exportAll = exportAll;
        this.groupName = groupName;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ExportKey exportKey = (ExportKey) o;
        return Objects.equals(exportAll, exportKey.exportAll) &&
                Objects.equals(version, exportKey.version) &&
                Objects.equals(groupName, exportKey.groupName);
    }

    @Override public int hashCode() {
        return Objects.hash(version, exportAll, groupName);
    }
}
