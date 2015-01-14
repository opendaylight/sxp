/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.behavior;

import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

/**
 * SXP supports various versions. The details of what is supported in each of
 * the version follows:
 * 
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
public class StrategyFactory {

    public static Strategy getStrategy(Context context, Version version) throws UnknownVersionException {
        switch (version) {
        case Version1:
            return new Sxpv1(context);
        case Version2:
            return new Sxpv2(context);
        case Version3:
            return new Sxpv3(context);
        case Version4:
            return new Sxpv4(context);
        default:
            throw new UnknownVersionException();
        }
    }
}
