/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit;

import org.junit.Before;
import org.junit.Test;

public class LibraryServerTest {

    private LibraryServer server;

    @Before public void setUp() {
        server = new LibraryServer();
    }

    @Test public void testStart() throws Exception {
        server.start();
    }

    @Test public void testStop() throws Exception {
        server.stop();
    }
}
