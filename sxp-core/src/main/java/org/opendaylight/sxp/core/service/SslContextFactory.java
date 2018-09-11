/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.tls.security.fields.Keystore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.tls.security.fields.Truststore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles providing SSL key-store and trust-store for Netty pipeline
 */
public final class SslContextFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SslContextFactory.class);
    private SslContext clientSslContext = null;
    private SslContext serverSslContext = null;

    /**
     * @param tlsConfig TLS configuration object, contains keystore locations and
     *                  keystore types
     */
    public SslContextFactory(final Tls tlsConfig) {
        try {
            if (Objects.nonNull(tlsConfig)) {
                final KeyManagerFactory
                        keystoreFactory =
                        getKeyStore(tlsConfig.getKeystore(), tlsConfig.getCertificatePassword());
                final TrustManagerFactory truststoreFactory = getTrustStore(tlsConfig.getTruststore());

                this.clientSslContext =
                        SslContextBuilder.forClient()
                                .keyManager(keystoreFactory)
                                .trustManager(truststoreFactory)
                                .build();
                this.serverSslContext =
                        SslContextBuilder.forServer(keystoreFactory)
                                .trustManager(truststoreFactory)
                                .clientAuth(ClientAuth.REQUIRE)
                                .build();
            }
        } catch (final IOException e) {
            LOG.error("IOException - Failed to load keystore / truststore.", e);
        } catch (final NoSuchAlgorithmException e) {
            LOG.error("NoSuchAlgorithmException - Unsupported algorithm.", e);
        } catch (final CertificateException e) {
            LOG.error("CertificateException - Unable to access certificate (check password).", e);
        } catch (final Exception e) {//NOSONAR
            LOG.error("Exception - Failed to initialize the SSLContext", e);
        }
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
                Preconditions.checkArgument(in != null, "File not found: %s", filename);
                break;
            case PATH://NOSONAR
                LOG.debug("Current dir using System: {}", System.getProperty("user.dir"));
                final File keystorefile = new File(filename);
                try {
                    in = new FileInputStream(keystorefile);
                } catch (final FileNotFoundException e) {
                    throw new IllegalStateException("File not found: " + filename, e);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown path type: " + pathType);
        }
        return in;
    }

    /**
     * @param keystore            Keystore definition
     * @param certificatePassword password used in certificate
     * @return KeyStore containing Keys that will be propagated to peers
     * @throws KeyStoreException         If any error regarding KeyStore generation occurs
     * @throws CertificateException      If any error regarding Certification import occurs
     * @throws NoSuchAlgorithmException  If KeyStore Algorithm is not supported by platform
     * @throws IOException               If Keystore path is invalid
     * @throws UnrecoverableKeyException If KeyStore is corrupted
     */
    private KeyManagerFactory getKeyStore(final Keystore keystore, final String certificatePassword)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            UnrecoverableKeyException {
        final KeyStore ks = KeyStore.getInstance(Objects.requireNonNull(keystore).getType().name());
        ks.load(asInputStream(keystore.getLocation(), keystore.getPathType()), keystore.getPassword().toCharArray());

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, Objects.isNull(certificatePassword) ? new char[0] : certificatePassword.toCharArray());
        return kmf;
    }

    /**
     * @param truststore Truststore definition
     * @return TrustStore used for matching valid peers
     * @throws KeyStoreException        If any error regarding KeyStore generation occurs
     * @throws CertificateException     If any error regarding Certification import occurs
     * @throws NoSuchAlgorithmException If KeyStore Algorithm is not supported by platform
     * @throws IOException              If Keystore path is invalid
     */
    private TrustManagerFactory getTrustStore(final Truststore truststore)
            throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        final KeyStore ts = KeyStore.getInstance(Objects.requireNonNull(truststore).getType().name());
        ts.load(asInputStream(truststore.getLocation(), truststore.getPathType()),
                truststore.getPassword().toCharArray());
        return null;
    }

    /**
     * @return Client SSL context used for establishing connections
     */
    public Optional<SslContext> getClientContext() {
        if (Objects.isNull(this.clientSslContext)) {
            return Optional.empty();
        }
        return Optional.of(this.clientSslContext);
    }

    /**
     * @return Server SSL context used for establishing connections
     */
    public Optional<SslContext> getServerContext() {
        if (Objects.isNull(this.serverSslContext)) {
            return Optional.empty();
        }
        return Optional.of(this.serverSslContext);
    }

}
