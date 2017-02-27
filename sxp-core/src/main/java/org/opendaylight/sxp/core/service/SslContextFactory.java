/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.Optional;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.security.fields.Tls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SslContextFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SslContextFactory.class);
    private final Tls tlsConfig;
    private SslContext clientSslContext = null, serverSslContext = null;

    /**
     * Plain SslContext
     */
    public SslContextFactory() {
        this.tlsConfig = null;
    }

    /**
     * @param tlsConfig TLS configuration object, contains keystore locations and
     *                  keystore types
     */
    public SslContextFactory(final Tls tlsConfig) {
        this.tlsConfig = tlsConfig;
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
                in = SslContextFactory.class.getClassLoader().getResourceAsStream(filename);
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

    /**
     * @return KeyStore containing Keys that will be propagated to peers
     * @throws KeyStoreException         If any error regarding KeyStore generation occurs
     * @throws CertificateException      If any error regarding Certification import occurs
     * @throws NoSuchAlgorithmException  If KeyStore Algorithm is not supported by platform
     * @throws IOException               If Keystore path is invalid
     * @throws UnrecoverableKeyException If KeyStore is corrupted
     */
    @VisibleForTesting
    private KeyManagerFactory getKeyStore()
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            UnrecoverableKeyException {
        final KeyStore ks = KeyStore.getInstance(this.tlsConfig.getKeystore().getType().name());
        ks.load(asInputStream(this.tlsConfig.getKeystore().getLocation(), this.tlsConfig.getKeystore().getPathType()),
                this.tlsConfig.getKeystore().getPassword().toCharArray());
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, this.tlsConfig.getCertificatePassword().toCharArray());
        return kmf;
    }

    /**
     * @return TrustStore used for matching valid peers
     * @throws KeyStoreException        If any error regarding KeyStore generation occurs
     * @throws CertificateException     If any error regarding Certification import occurs
     * @throws NoSuchAlgorithmException If KeyStore Algorithm is not supported by platform
     * @throws IOException              If Keystore path is invalid
     */
    @VisibleForTesting
    private TrustManagerFactory getTrustStore()
            throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        final KeyStore ts = KeyStore.getInstance(this.tlsConfig.getTruststore().getType().name());
        ts.load(asInputStream(this.tlsConfig.getTruststore().getLocation(),
                this.tlsConfig.getTruststore().getPathType()),
                this.tlsConfig.getTruststore().getPassword().toCharArray());
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        return tmf;
    }

    /**
     * @return Client SSL context used for establishing connections
     */
    public Optional<SslContext> getClientContext() {
        if (Objects.isNull(tlsConfig) || Objects.isNull(this.tlsConfig.getTruststore()) || Objects.isNull(
                this.tlsConfig.getKeystore())) {
            return Optional.empty();
        }
        if (Objects.isNull(this.clientSslContext)) {
            try {
                this.clientSslContext =
                        SslContextBuilder.forClient().keyManager(getKeyStore()).trustManager(getTrustStore()).build();
            } catch (final IOException e) {
                LOG.warn(
                        "IOException - Failed to load keystore / truststore. Failed to initialize the client-side SSLContext",
                        e);
            } catch (final NoSuchAlgorithmException e) {
                LOG.warn(
                        "NoSuchAlgorithmException - Unsupported algorithm. Failed to initialize the client-side SSLContext",
                        e);
            } catch (final CertificateException e) {
                LOG.warn(
                        "CertificateException - Unable to access certificate (check password). Failed to initialize the client-side SSLContext",
                        e);
            } catch (final Exception e) {
                LOG.warn("Exception - Failed to initialize the client-side SSLContext", e);
            }
        }
        return Optional.of(this.clientSslContext);
    }

    /**
     * @return Server SSL context used for establishing connections
     */
    public Optional<SslContext> getServerContext() {
        if (Objects.isNull(tlsConfig) || Objects.isNull(this.tlsConfig.getTruststore()) || Objects.isNull(
                this.tlsConfig.getKeystore())) {
            return Optional.empty();
        }
        if (Objects.isNull(this.serverSslContext)) {
            try {
                this.serverSslContext =
                        SslContextBuilder.forServer(getKeyStore())
                                .trustManager(getTrustStore())
                                .clientAuth(ClientAuth.REQUIRE)
                                .build();
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
        }
        return Optional.of(this.serverSslContext);
    }

}
