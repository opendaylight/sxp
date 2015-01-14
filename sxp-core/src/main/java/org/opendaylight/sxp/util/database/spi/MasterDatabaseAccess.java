/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;

public interface MasterDatabaseAccess {

    public void delete(MasterDatabase database) throws Exception;

    public void merge(MasterDatabase database) throws Exception;

    public void put(MasterDatabase database) throws Exception;

    public MasterDatabase read() throws Exception;
}
