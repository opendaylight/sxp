/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.restconfclient;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 *
 * @author Martin Dindoffer
 */
public class RestconfClient {

    private final UserCredentials credentials;
    private final Client httpclient;
    private final DSType dsType;
    private final String host;
    private final int port;
    private final MediaType contentType;
    private final MediaType acceptType;
    private final String baseURL;

    private RestconfClient(UserCredentials credentials, DSType dsType, String host, int port,
            MediaType contentType, MediaType acceptType) {
        this.credentials = credentials;
        this.dsType = dsType;
        this.host = host;
        this.port = port;
        this.contentType = contentType;
        this.acceptType = acceptType;
        HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basicBuilder()
                .nonPreemptive()
                .credentials(credentials.getName(), credentials.getPassword())
                .build();
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(authFeature);
        this.httpclient = ClientBuilder.newClient(clientConfig);
        baseURL = createBaseURL(dsType, host, port);
    }

    public Response get(String... urlParts) {
        String targetURL = createTargetURL(urlParts);
        WebTarget target = httpclient.target(targetURL);
        return target.request()
                .accept(acceptType)
                .header("Content-Type", contentType)
                .get();
    }

    public Response put(String payload, String... urlParts) {
        String targetURL = createTargetURL(urlParts);
        WebTarget target = httpclient.target(targetURL);
        return target.request()
                .accept(acceptType)
                .header("Content-Type", contentType)
                .put(Entity.entity(payload, contentType));
    }

    public Response post(String payload, String... urlParts) {
        String targetURL = createTargetURL(urlParts);
        WebTarget target = httpclient.target(targetURL);
        return target.request()
                .accept(acceptType)
                .header("Content-Type", contentType)
                .post(Entity.entity(payload, contentType));
    }

    public Response delete(String... urlParts) {
        String targetURL = createTargetURL(urlParts);
        WebTarget target = httpclient.target(targetURL);
        return target.request()
                .accept(acceptType)
                .header("Content-Type", contentType)
                .delete();
    }

    private static String createBaseURL(DSType dsType, String host, int port) {
        return "http://" + host + ":" + port + "/restconf/" + dsType;
    }

    private String createTargetURL(String... urlParts) {
        return baseURL + "/" + String.join("/", urlParts);
    }

    public static class Builder {

        private UserCredentials credentials;
        private DSType dsType;
        private String host;
        private int port;
        private MediaType contentType;
        private MediaType acceptType;

        public Builder setCredentials(UserCredentials credentials) {
            this.credentials = credentials;
            return this;
        }

        public Builder setDsType(DSType dsType) {
            this.dsType = dsType;
            return this;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setContentType(MediaType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder setAcceptType(MediaType acceptType) {
            this.acceptType = acceptType;
            return this;
        }

        public RestconfClient build() {
            return new RestconfClient(credentials, dsType, host, port, contentType, acceptType);
        }

    }
}
