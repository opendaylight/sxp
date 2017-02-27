/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.Optional;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.security.fields.Tls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SslContextFactory {

    private static final String PROTOCOL = "TLS";
    private static final Logger LOG = LoggerFactory.getLogger(SslContextFactory.class);
    private final Tls tlsConfig;

    /**
     * @param tlsConfig TLS configuration object, contains keystore locations and
     *                  keystore types
     */
    public SslContextFactory(final Tls tlsConfig) {
        this.tlsConfig = Objects.requireNonNull(tlsConfig);
        Objects.requireNonNull(this.tlsConfig.getKeystore());
        Objects.requireNonNull(this.tlsConfig.getTruststore());
    }

    /**
     * InputStream instance of key - key location is on classpath or specific path
     *
     * @param filename keystore location
     * @param pathType keystore location type - "classpath" or "path"
     * @return key as InputStream
     */
    public static InputStream asInputStream(final String filename, final PathType pathType) {
        InputStream in;
        switch (pathType) {
            case CLASSPATH:
                in = SslContextFactory.class.getResourceAsStream(filename);
                Preconditions.checkArgument(in != null, "KeyStore file not found: %s", filename);
                break;
            case PATH:
                LOG.debug("Current dir using System: {}", System.getProperty("user.dir"));
                final File keystorefile = new File(filename);
                try {
                    in = new FileInputStream(keystorefile);
                } catch (final FileNotFoundException e) {
                    throw new IllegalStateException("KeyStore file not found: " + filename, e);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown path type: " + pathType);
        }
        return in;
    }

    public Optional<SSLEngine> getClientContext() {
        try {
            final KeyStore ks = KeyStore.getInstance(this.tlsConfig.getKeystore().getType().name());
            ks.load(asInputStream(this.tlsConfig.getKeystore().getLocation(),
                    this.tlsConfig.getKeystore().getPathType()),
                    this.tlsConfig.getKeystore().getPassword().toCharArray());
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, this.tlsConfig.getCertificatePassword().toCharArray());

            final KeyStore ts = KeyStore.getInstance(this.tlsConfig.getTruststore().getType().name());
            ts.load(asInputStream(this.tlsConfig.getTruststore().getLocation(),
                    this.tlsConfig.getTruststore().getPathType()),
                    this.tlsConfig.getTruststore().getPassword().toCharArray());
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);

            final SSLContext clientContext = SSLContext.getInstance(PROTOCOL);
            clientContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            final SSLEngine sslEngine = clientContext.createSSLEngine();
            sslEngine.setUseClientMode(true);
            return Optional.of(sslEngine);
        } catch (final IOException e) {
            LOG.warn(
                    "IOException - Failed to load keystore / truststore. Failed to initialize the server-side SSLContext",
                    e);
        } catch (final NoSuchAlgorithmException e) {
            LOG.warn(
                    "NoSuchAlgorithmException - Unsupported algorithm. Failed to initialize the server-side SSLContext",
                    e);
        } catch (final CertificateException e) {
            LOG.warn(
                    "CertificateException - Unable to access certificate (check password). Failed to initialize the server-side SSLContext",
                    e);
        } catch (final Exception e) {
            LOG.warn("Exception - Failed to initialize the server-side SSLContext", e);
        }
        return Optional.empty();
    }

    public Optional<SSLEngine> getServerContext() {
        try {
            final KeyStore ks = KeyStore.getInstance(this.tlsConfig.getKeystore().getType().name());
            ks.load(asInputStream(this.tlsConfig.getKeystore().getLocation(),
                    this.tlsConfig.getKeystore().getPathType()),
                    this.tlsConfig.getKeystore().getPassword().toCharArray());
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, this.tlsConfig.getCertificatePassword().toCharArray());

            final KeyStore ts = KeyStore.getInstance(this.tlsConfig.getTruststore().getType().name());
            ts.load(asInputStream(this.tlsConfig.getTruststore().getLocation(),
                    this.tlsConfig.getTruststore().getPathType()),
                    this.tlsConfig.getTruststore().getPassword().toCharArray());
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);

            final SSLContext serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            final SSLEngine sslEngine = serverContext.createSSLEngine();
            sslEngine.setUseClientMode(false);
            sslEngine.setNeedClientAuth(true);
            return Optional.of(sslEngine);
        } catch (final IOException e) {
            LOG.warn(
                    "IOException - Failed to load keystore / truststore. Failed to initialize the server-side SSLContext",
                    e);
        } catch (final NoSuchAlgorithmException e) {
            LOG.warn(
                    "NoSuchAlgorithmException - Unsupported algorithm. Failed to initialize the server-side SSLContext",
                    e);
        } catch (final CertificateException e) {
            LOG.warn(
                    "CertificateException - Unable to access certificate (check password). Failed to initialize the server-side SSLContext",
                    e);
        } catch (final Exception e) {
            LOG.warn("Exception - Failed to initialize the server-side SSLContext", e);
        }
        return Optional.empty();
    }

}
