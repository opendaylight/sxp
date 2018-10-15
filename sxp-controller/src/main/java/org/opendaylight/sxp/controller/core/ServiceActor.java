/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.controller.core;

import akka.actor.UntypedActor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerService;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class ServiceActor extends UntypedActor {

    SxpRpcServiceImpl delagate;

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof AddConnectionInput) {
            RpcResult<AddConnectionOutput> res = null;

            try {
                res = delagate.addConnection((AddConnectionInput) message).get()
            } catch (Exception e) {

            }

            sender().tell(res, self());
        }

    }
}
