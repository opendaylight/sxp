/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.sxp.controller.core.DatastoreAccess;

/**
 * Test for {@link FollowerSyncStatusTaskFactory}.
 */
public class FollowerSyncStatusTaskFactoryTest {

    private final int taskPeriod = 10;
    @Mock private DataBroker dataBroker;
    private DatastoreAccess datastoreAccess;
    private FollowerSyncStatusTaskFactory taskFactory;

    @Before
    public void setUp() throws Exception {
        datastoreAccess = DatastoreAccess.getInstance(dataBroker);
        taskFactory = new FollowerSyncStatusTaskFactory(taskPeriod, 0);
    }

    @Test
    public void createFollowerSyncStatusTask() throws Exception {
        Assert.assertNotNull("Expected functional task, got",
                taskFactory.createFollowerSyncStatusTask(datastoreAccess));
    }

}
