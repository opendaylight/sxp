/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.ExportedBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.exported.bindings.Bindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationListenerImpl implements SxpControllerListener {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationListenerImpl.class);

    @Override
    public void onExportedBindings(ExportedBindings notification) {
        if (notification.getBindings() != null) {
            for (Bindings binding : notification.getBindings()) {
                binding.getVpnLabel();
                binding.getSgt();
                binding.getVpnLabel();
            }
        }

        LOG.info("Notification event: onExportedBinding '{}'", notification.toString());
    }
}
