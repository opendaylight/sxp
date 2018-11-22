/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.restconfclient;

/**
 *
 * @author Martin Dindoffer
 */
public class UserCredentials {

    private final String name;
    private final String password;

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public UserCredentials(String name, String password) {
        this.name = name;
        this.password = password;
    }

}
