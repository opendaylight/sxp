/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.netty;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * A netty take on guava's ImmediateCancelledFuture.
 *
 * @param <V> type of the future
 */
public final class ImmediateCancelledFuture<V> extends DefaultPromise<V> {

    public ImmediateCancelledFuture() {
        super(ImmediateEventExecutor.INSTANCE);
        cancel(true);
    }
}
