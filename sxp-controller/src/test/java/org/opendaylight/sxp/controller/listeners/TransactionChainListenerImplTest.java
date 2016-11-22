/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class) @PrepareForTest({Configuration.class, DatastoreAccess.class})
public class TransactionChainListenerImplTest {

    private DatastoreAccess datastoreAccess;
    private TransactionChainListenerImpl chainListener;

    @Before public void init() {
        datastoreAccess = mock(DatastoreAccess.class);
        chainListener = new TransactionChainListenerImpl(datastoreAccess);
    }

    @Test public void onTransactionChainFailed() throws Exception {
        verify(datastoreAccess, never()).reinitializeChain();
        chainListener.onTransactionChainFailed(mock(TransactionChain.class), mock(AsyncTransaction.class),
                mock(Exception.class));
        verify(datastoreAccess).reinitializeChain();
    }

    @Test public void onTransactionChainSuccessful() throws Exception {
        verify(datastoreAccess, never()).reinitializeChain();
        chainListener.onTransactionChainSuccessful(mock(TransactionChain.class));
        verify(datastoreAccess, never()).reinitializeChain();
    }

}
