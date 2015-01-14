/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.io.ui;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.io.IOManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeOperation extends Thread {
    public static int _PROGRESS_INC = 0;

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeOperation.class.getName());

    protected static HashMap<String, SxpNode> nodes;

    private static ProgressBar getProgressBar(Shell shell) {
        ProgressBar progressBar = new ProgressBar(shell, SWT.HORIZONTAL | SWT.SMOOTH);
        progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        progressBar.setMinimum(0);
        progressBar.setMaximum(0);
        return progressBar;
    }

    private Display display;

    private IOManager ioManager;

    private ProgressBar progressBar;

    public RuntimeOperation(IOManager ioManager, String applicationName, HashMap<String, SxpNode> nodes) {
        this.ioManager = ioManager;
        this.display = ioManager.getShell().getDisplay();
        RuntimeOperation.nodes = nodes;
        this.progressBar = getProgressBar(ioManager.getShell());
    }

    @Override
    public void run() {
        try {
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    ioManager.updateAppTitle();
                }
            });

        } catch (Exception e) {
            LOG.warn("{} | {}", e.getClass().getSimpleName(), e.getMessage());
        }
        if (progressBar != null) {
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (progressBar.isDisposed()) {
                        return;
                    }
                    progressBar.setSelection(progressBar.getSelection() + _PROGRESS_INC);
                }
            });
        }
    }
}
