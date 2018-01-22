/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.test;

import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywordOverload;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.opendaylight.sxp.jrobot.remoteserver.RemoteServer;
import org.opendaylight.sxp.jrobot.remoteserver.library.AbstractClassLibrary;

@RobotKeywords public class ConflictLibrary extends AbstractClassLibrary {

    public ConflictLibrary(RemoteServer server) {
        super(server);
    }

    @Override public String getURI() {
        return getClass().getSimpleName();
    }

    @Override public void close() {
    }

    public void conflictMethod(final RemoteServer s, final ConflictLibrary a) {
        throw new RuntimeException("Invalid Keyword executed");
    }

    @RobotKeyword public String conflictMethod(final String s, final String a) {
        return "[" + s + "],[" + a + "]";
    }

    @RobotKeyword public void conflictOverloadedMethod() {
        throw new RuntimeException("Invalid Keyword executed");
    }

    public void conflictOverloadedMethod(final String s, final boolean b, final int a) {
        throw new RuntimeException("Invalid Keyword executed");
    }

    @RobotKeywordOverload public String conflictOverloadedMethod(final String s, final int a, final boolean b) {
        return "[" + s + "],[" + a + "],[" + b + "]";
    }

    @RobotKeywordOverload public String originalKeyword(double d) {
        return "Double " + d;
    }

    @RobotKeyword("Original Keyword") public String alternative(int a, int b) {
        return "Ints " + a + " " + b;
    }

}
