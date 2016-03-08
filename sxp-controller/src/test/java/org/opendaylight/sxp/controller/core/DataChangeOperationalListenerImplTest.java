/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sxp.controller.util.database.DatastoreValidator;
import org.opendaylight.sxp.controller.util.database.access.DatastoreAccess;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

@RunWith(PowerMockRunner.class) @PrepareForTest({DatastoreAccess.class, DatastoreValidator.class, SxpNode.class})
public class DataChangeOperationalListenerImplTest {

        private static DataChangeOperationalListenerImpl operationalListener;

        @Before public void init() throws ExecutionException, InterruptedException {
                operationalListener = new DataChangeOperationalListenerImpl("0.0.0.0");
        }

        @Test public void testGetSubscribedPath() throws Exception {
                InstanceIdentifier identifier = operationalListener.getSubscribedPath();
                assertNotNull(identifier);
                assertEquals(MasterDatabase.class, identifier.getTargetType());
        }

}
