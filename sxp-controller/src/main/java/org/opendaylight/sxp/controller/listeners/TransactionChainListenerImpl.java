/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import java.util.Objects;
import org.opendaylight.mdsal.common.api.AsyncTransaction;
import org.opendaylight.mdsal.common.api.TransactionChain;
import org.opendaylight.mdsal.common.api.TransactionChainListener;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TransactionChanListener class provides loggings of transaction events and chain restoration upon failures
 */
public class TransactionChainListenerImpl implements TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionChainListenerImpl.class);
    private final DatastoreAccess datastoreAccess;

    /**
     * @param datastoreAccess DatastoreAccess that will be associated with Listener
     */
    public TransactionChainListenerImpl(DatastoreAccess datastoreAccess) {
        this.datastoreAccess = Objects.requireNonNull(datastoreAccess);
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
            Throwable cause) {
        datastoreAccess.reinitializeChain();
        LOG.warn("{} Transaction chain failed creating new one.", datastoreAccess);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Transaction chain failed ", cause);
        }
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Transaction chain Success");
        }
    }
}
