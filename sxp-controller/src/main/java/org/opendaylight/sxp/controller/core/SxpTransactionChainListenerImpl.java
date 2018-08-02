/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.controller.core;

import org.opendaylight.mdsal.common.api.AsyncReadTransaction;
import org.opendaylight.mdsal.common.api.AsyncTransaction;
import org.opendaylight.mdsal.common.api.AsyncWriteTransaction;
import org.opendaylight.mdsal.common.api.TransactionChain;
import org.opendaylight.mdsal.common.api.TransactionChainListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SxpTransactionChainListenerImpl implements TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(SxpTransactionChainListenerImpl.class);

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
            Throwable cause) {
        LOG.debug("Transaction chain {} failed in {}", chain, transaction, cause);
        if (transaction instanceof AsyncReadTransaction) {
            ((AsyncReadTransaction) transaction).close();
        }
        if (transaction instanceof AsyncWriteTransaction) {
            ((AsyncWriteTransaction) transaction).cancel();
        }
        chain.close();
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain {} successful", chain);
        chain.close();
    }
}

