/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.database.access;

import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionChainListenerImpl implements TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionChainListenerImpl.class);

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> transactionChain,
            AsyncTransaction<?, ?> asyncTransaction, Throwable throwable) {
        LOG.warn("Transaction chain failed");
        throwable.printStackTrace();
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> transactionChain) {

    }
}
