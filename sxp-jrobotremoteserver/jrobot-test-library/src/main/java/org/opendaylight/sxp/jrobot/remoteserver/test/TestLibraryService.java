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

@RobotKeywords public interface TestLibraryService {

    @RobotKeyword String getName();

    @RobotKeyword double add(double a, double b);

    @RobotKeywordOverload int add(int a, int b);

    @RobotKeyword double sub(double a, double b);

    @RobotKeywordOverload int sub(int a, int b);

    @RobotKeyword String concat(String input1, String input2);

    @RobotKeywordOverload String concat(String input1, int input2);

    @RobotKeywordOverload String concat(int input1, String input2);

    @RobotKeywordOverload String concat(String input1, String input2, String input3);
}
