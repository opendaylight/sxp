/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.StoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.security.fields.Tls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.security.fields.TlsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.tls.security.fields.KeystoreBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.tls.security.fields.TruststoreBuilder;

public class SslContextFactoryTest {

    private SslContextFactory contextFactory;
    private Tls tls;

    @Before
    public void setUp() throws Exception {
        tls =
                new TlsBuilder().setCertificatePassword("admin123")
                        .setKeystore(new KeystoreBuilder().setType(StoreType.JKS)
                                .setPathType(PathType.CLASSPATH)
                                .setLocation("keystore")
                                .setPassword("admin123")
                                .build())
                        .setTruststore(new TruststoreBuilder().setType(StoreType.JKS)
                                .setPathType(PathType.CLASSPATH)
                                .setLocation("truststore")
                                .setPassword("admin123")
                                .build())
                        .build();
    }

    @Test
    public void asInputStream() throws Exception {
        Assert.assertNotNull(SslContextFactory.asInputStream("keystore", PathType.CLASSPATH));
        final File file = new File("tst_file");
        if (file.createNewFile()) {
            Assert.assertNotNull(SslContextFactory.asInputStream("tst_file", PathType.PATH));
            file.delete();
        }

        try {
            Assert.assertNotNull(SslContextFactory.asInputStream("", PathType.PATH));
            Assert.fail("Should fail on as file does not exist");
        } catch (Exception e) {
            //NOP
        }

        try {
            Assert.assertNotNull(SslContextFactory.asInputStream("keystore", null));
            Assert.fail("Should fail on as path is not specified");
        } catch (Exception e) {
            //NOP
        }
    }

    @Test
    public void getClientContext() throws Exception {
        contextFactory = new SslContextFactory(null);
        Assert.assertNotNull(contextFactory);
        Assert.assertNotNull(contextFactory.getClientContext());
        Assert.assertFalse(contextFactory.getClientContext().isPresent());

        contextFactory = new SslContextFactory(tls);
        Assert.assertNotNull(contextFactory);
        Assert.assertNotNull(contextFactory.getClientContext());
        Assert.assertTrue(contextFactory.getClientContext().isPresent());
        Assert.assertTrue(contextFactory.getClientContext().get().isClient());
    }

    @Test
    public void getServerContext() throws Exception {
        contextFactory = new SslContextFactory(null);
        Assert.assertNotNull(contextFactory);
        Assert.assertNotNull(contextFactory.getServerContext());
        Assert.assertFalse(contextFactory.getServerContext().isPresent());

        contextFactory = new SslContextFactory(tls);
        Assert.assertNotNull(contextFactory);
        Assert.assertNotNull(contextFactory.getServerContext());
        Assert.assertTrue(contextFactory.getServerContext().isPresent());
        Assert.assertTrue(contextFactory.getServerContext().get().isServer());
    }

    @Test
    public void testConstructorErrorHandling() throws Exception {
        Tls tlsMock = mock(Tls.class);

        when(tlsMock.getCertificatePassword()).thenThrow(IOException.class);
        SslContextFactory sslContextFactory = new SslContextFactory(tlsMock);
        Assert.assertNotNull(sslContextFactory);

        Mockito.reset(tlsMock);
        when(tlsMock.getCertificatePassword()).thenThrow(NoSuchAlgorithmException.class);
        sslContextFactory = new SslContextFactory(tlsMock);
        Assert.assertNotNull(sslContextFactory);

        Mockito.reset(tlsMock);
        when(tlsMock.getCertificatePassword()).thenThrow(CertificateException.class);
        sslContextFactory = new SslContextFactory(tlsMock);
        Assert.assertNotNull(sslContextFactory);

        Mockito.reset(tlsMock);
        when(tlsMock.getCertificatePassword()).thenThrow(Exception.class);
        sslContextFactory = new SslContextFactory(tlsMock);
        Assert.assertNotNull(sslContextFactory);
    }

}
