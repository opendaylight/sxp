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