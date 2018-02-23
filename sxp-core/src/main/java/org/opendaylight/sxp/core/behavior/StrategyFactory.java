/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.behavior;

import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

/**
 * SXP supports various versions. The details of what is supported in each of
 * the version follows:
 * <pre>
 * +-----+----------+----------+------------+-----------+--------------+
 * | Ver | IPv4     | IPv6     | Subnet     | Loop      | SXP          |
 * |     | Bindings | Bindings | Binding    | Detection | Capability   |
 * |     |          |          | Expansion  |           | Exchange     |
 * +-----+----------+----------+------------+-----------+--------------+
 * | 1   | Yes      | No       | No         | No        | No           |
 * | 2   | Yes      | Yes      | No         | No        | No           |
 * | 3   | Yes      | Yes      | Yes        | No        | No           |
 * | 4   | Yes      | Yes      | Yes        | Yes       | Yes          |
 * +-----+----------+----------+------------+-----------+--------------+
 * </pre>
 */
@SuppressWarnings("all")
public final class StrategyFactory {

    private StrategyFactory() {
    }

    /**
     * Creates a behaviour strategy for given SXP version and an SXP node.
     *
     * @param ownerNode SXPNode accompanying the new strategy
     * @param version   Version of SXP to use
     * @return Chosen strategy
     * @throws UnknownVersionException If version is not supported
     */
    public static Strategy getStrategy(SxpNode ownerNode, Version version) {
        if (version != null) {
            switch (version) {
                case Version1:
                case Version2:
                case Version3:
                    return new SxpLegacy(version);
                case Version4:
                    return new Sxpv4(ownerNode);
            }
        }
        throw new UnknownVersionException();
    }
}
