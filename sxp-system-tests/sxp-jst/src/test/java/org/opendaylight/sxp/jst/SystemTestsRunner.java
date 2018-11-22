/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jst;

import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run tests in {@link SystemTests} and see console logs for their status.
 * <p>
 * To run tests an SXP Karaf instance has to be running on the machine.
 */
public class SystemTestsRunner {
    private static final Logger LOG = LoggerFactory.getLogger(SystemTestsRunner.class);

    public static void main(String[] args) throws Exception {
        SystemTests tests = new SystemTests();
        Method[] methods = tests.getClass().getMethods();

        for (Method method: methods) {
            String methodName = method.getName();
            if (methodName.startsWith("test")) {
                LOG.info("Running test {}", methodName);
                method.invoke(tests);
            }
        }

        tests.cleanupTopo();
    }
}
