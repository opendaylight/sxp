/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.util.database.Database;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabasesTest {

    private static final Logger LOG;

    // TODO: databases test - under construction
    private static List<PrefixGroup> prefixGroups1, prefixGroups2;

    static {
        Configuration.initializeLogger();
        LOG = LoggerFactory.getLogger(DatabasesTest.class.getName());
    }

    @BeforeClass
    public static void init() {
        prefixGroups1 = new ArrayList<>();
        prefixGroups2 = new ArrayList<>();
        try {
            prefixGroups1.add(Database.createPrefixGroup(10000, "192.168.0.1/32"));
            prefixGroups1.add(Database.createPrefixGroup(20000, "2001::1/64", "10.10.10.10/30"));
            prefixGroups1.add(Database.createPrefixGroup(30000, "2002::1/128"));
            prefixGroups1.add(Database.createPrefixGroup(40000, "11.11.11.0/29"));
            prefixGroups1.add(Database.createPrefixGroup(65000, "172.168.1.0/28"));

            prefixGroups2.add(Database.createPrefixGroup(10000, "192.168.0.2/32"));
            prefixGroups2.add(Database.createPrefixGroup(20000, "3001::1/64", "11.11.11.11/30"));
            prefixGroups2.add(Database.createPrefixGroup(30000, "3002::1/128"));
            prefixGroups2.add(Database.createPrefixGroup(40000, "12.12.12.0/29"));
            prefixGroups2.add(Database.createPrefixGroup(65000, "172.168.2.0/28"));

        } catch (Exception e) {
            LOG.warn("{} | {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Test
    public void AddLocalBindings() throws Exception {
        MasterDatabaseInf masterDatabase = new MasterDatabaseImpl();
        masterDatabase.addBindingsLocal(prefixGroups1);
        masterDatabase.addBindingsLocal(prefixGroups2);
        LOG.info(masterDatabase.toString());

        for (MasterDatabase _masterDatabase : masterDatabase.partition(3, false)) {
            LOG.info(new MasterDatabaseImpl(_masterDatabase).toString());
        }
    }
}
