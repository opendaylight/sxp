/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.test;

import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.opendaylight.sxp.jrobot.remoteserver.RemoteServer;

@RobotKeywords public class ArgumentsLibrary extends BaseLibrary {

    public ArgumentsLibrary(RemoteServer server) {
        super(server);
    }

    @Override @ArgumentNames({"element_1", "element_2"}) public double add(double a, double b) {
        return super.add(a, b);
    }

    @Override @ArgumentNames({"element_1", "element_2"}) public int add(int a, int b) {
        return super.add(a, b);
    }

    @Override @ArgumentNames({"element_1", "element_2"}) public double sub(double a, double b) {
        return super.sub(a, b);
    }

    @Override @ArgumentNames({"element_1", "element_2"}) public int sub(int a, int b) {
        return super.sub(a, b);
    }
}
