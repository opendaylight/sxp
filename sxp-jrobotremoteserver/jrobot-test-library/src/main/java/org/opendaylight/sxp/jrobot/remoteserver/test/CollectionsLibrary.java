/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.opendaylight.sxp.jrobot.remoteserver.RemoteServer;

@RobotKeywords public class CollectionsLibrary extends BaseLibrary {

    public CollectionsLibrary(RemoteServer server) {
        super(server);
    }

    @Override public String getURI() {
        return getClass().getSimpleName();
    }

    @Override public String getName() {
        return "Collections Library";
    }

    @RobotKeyword public Map<Integer, String> getMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(5, "five");
        map.put(6, "six");
        map.put(10, "ten");
        return map;
    }

    @RobotKeyword public int[] getArrayInts() {
        return new int[] {41, 42, 43, 44, 45};
    }

    @RobotKeyword public char[] getArrayChars() {
        return new char[] {'a', 'b', 'c', 'd'};
    }

    @RobotKeyword public List<Integer> getListIntegers() {
        return IntStream.range(1, 5).boxed().collect(Collectors.toList());
    }

    @RobotKeyword public List<String> getListStrings() {
        return IntStream.range(1, 5).mapToObj(Integer::toString).collect(Collectors.toList());
    }

    @RobotKeyword public Set<Integer> getSetIntegers() {
        return IntStream.range(1, 5).boxed().collect(Collectors.toSet());
    }

    @RobotKeyword public Set<String> getSetStrings() {
        return IntStream.range(1, 5).mapToObj(Integer::toString).collect(Collectors.toSet());
    }

}
