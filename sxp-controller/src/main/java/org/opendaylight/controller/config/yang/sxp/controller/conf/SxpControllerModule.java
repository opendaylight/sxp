/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.sxp.controller.conf;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.util.io.ConfigLoader;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SxpControllerModule
        extends org.opendaylight.controller.config.yang.sxp.controller.conf.AbstractSxpControllerModule {

    protected static final Logger LOG = LoggerFactory.getLogger(SxpControllerModule.class);
    private static final AtomicBoolean bundleActivatedOnNode = new AtomicBoolean(false);

    public SxpControllerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SxpControllerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.sxp.controller.conf.SxpControllerModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    /**
     * Notify ConfigSubsystem routine that SXP service is ready to accept initial configuration
     */
    public static void notifyBundleActivated() {
        synchronized (bundleActivatedOnNode) {
            bundleActivatedOnNode.set(true);
            bundleActivatedOnNode.notify();
        }
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        ExecutorService worker = ThreadsWorker.generateExecutor(1, "CSS-init");
        worker.submit(() -> {
            try (ConfigLoader configLoader = new ConfigLoader(DatastoreAccess.getInstance(getDataBrokerDependency()))) {
                if (!bundleActivatedOnNode.get()) {
                    synchronized (bundleActivatedOnNode) {
                        TimeUnit.SECONDS.timedWait(bundleActivatedOnNode, 180);
                    }
                }
                if (bundleActivatedOnNode.get()) {
                    configLoader.load(getSxpController());
                }
            } catch (InterruptedException e) {
                LOG.warn("Cannot push initial configuration to DS", e);
            }
            worker.shutdown();
        });
        return worker::shutdown;
    }
}
