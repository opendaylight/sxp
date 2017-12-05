/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core;

import java.lang.reflect.Constructor;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Martin Dindoffer
 */
public class ConstantsTest {

    @Test
    public void testInstantiation() throws Exception {
        Constructor<Constants> c = Constants.class.getDeclaredConstructor(new Class[0]);
        c.setAccessible(true);
        Constants newInstance = c.newInstance(new Object[0]);
        Assert.assertNotNull(newInstance);
    }

}