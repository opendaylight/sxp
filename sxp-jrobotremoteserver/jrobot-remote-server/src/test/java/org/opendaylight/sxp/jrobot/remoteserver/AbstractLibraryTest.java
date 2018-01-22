/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver;

import java.util.concurrent.atomic.AtomicBoolean;
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywordOverload;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.opendaylight.sxp.jrobot.remoteserver.annotations.KeywordDocumentation;
import org.opendaylight.sxp.jrobot.remoteserver.annotations.KeywordTags;
import org.opendaylight.sxp.jrobot.remoteserver.library.AbstractClassLibrary;

@RobotKeywords public class AbstractLibraryTest extends AbstractClassLibrary {

    public static final AtomicBoolean closed = new AtomicBoolean(false);
    public static final String URI = "TestURI";

    public AbstractLibraryTest(RemoteServer server) {
        super(server);
    }

    @RobotKeyword @KeywordDocumentation(value = "plusDoc") @ArgumentNames(value = {"a", "b"})
    public int plus(int a, int b) {
        return a + b;
    }

    @RobotKeyword @KeywordDocumentation(value = "minusDoc") @KeywordTags(value = {"minus", "-"})
    public int minus(int a, int b) {
        return a - b;
    }

    @RobotKeyword @KeywordDocumentation(value = "concatDoc") @ArgumentNames(value = {"a", "b"})
    public String concat(String a, String b) {
        return a + b;
    }

    @RobotKeyword @KeywordTags(value = {"default"}) public int defaultKeyword(int a, int b) {
        return a + b;
    }

    @RobotKeywordOverload @KeywordTags(value = {"default", "defaultOverload"})
    public int defaultKeyword(int a, int b, int c) {
        return a + b + c;
    }

    @Override public String getURI() {
        return URI;
    }

    @Override public void close() {
        closed.set(true);
    }
}
