/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.route.core;

/**
 * internal states of route configuration listener
 */
enum RouteListenerState {
    /**
     * Active but no DS event received yet
     * <ul>
     * <li>processing DS events: YES</li>
     * <li>next state: any</li>
     * </ul>
     */
    BEFORE_FIRST(true), /**
     * Active and already processed at least one DS event
     * <ul>
     * <li>processing DS events: YES</li>
     * <li>next state: STOPPED</li>
     * </ul>
     */
    WORKING(true), /**
     * Not active
     * <ul>
     * <li>processing DS events: NO</li>
     * <li>next state: BEFORE_FIRST</li>
     * </ul>
     */
    STOPPED(false);

    private final boolean processing;

    /**
     * @param processing If initial state is processing
     */
    RouteListenerState(final boolean processing) {
        this.processing = processing;
    }

    /**
     * @return If state is processing
     */
    public boolean isProcessing() {
        return processing;
    }
}
