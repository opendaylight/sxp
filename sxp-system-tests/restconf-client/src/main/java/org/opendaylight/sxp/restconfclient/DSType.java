package org.opendaylight.sxp.restconfclient;

/**
 *
 * @author Martin Dindoffer
 */
public enum DSType {
    CONFIG("config"), OPERATIONAL("operational");

    private final String url;

    private DSType(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return url;
    }
}
