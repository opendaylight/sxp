/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.io;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.io.ui.ExpandAllItemsListener;
import org.opendaylight.sxp.util.io.ui.NodesTree;
import org.opendaylight.sxp.util.io.ui.NodesTreeListener;
import org.opendaylight.sxp.util.io.ui.RuntimeOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOManager {

    public enum Action {
        Run, Shutdown
    }

    public static String APPLICATION_NAME = "SXPv4 I/O Manager";

    private static final Logger LOG = LoggerFactory.getLogger(IOManager.class.getName());

    private static Tree tree = null;

    private static NodesTree tree_nodes = null;

    private static final int wndHeight = 600;

    private static final int wndWidth = 440;

    public static void create(HashMap<String, SxpNode> nodes) {
        new IOManager(nodes);
    }

    private Button button_run = null, button_shutdown = null;

    private Display display = new Display();

    private HashMap<String, SxpNode> nodes;

    private Shell shell;

    private IOManager(HashMap<String, SxpNode> nodes) {
        this.nodes = nodes;

        // Default: SHELL_TRIM = CLOSE | TITLE | MIN | MAX | RESIZE.
        shell = new Shell(display, SWT.SHELL_TRIM & ~SWT.RESIZE);
        shell.setLayout(new GridLayout(1, true));
        shell.setText(APPLICATION_NAME + " @ " + wndWidth + "x" + wndHeight);
        shell.setSize(wndWidth, wndHeight);
        initShell(shell);
        load();
        new RuntimeOperation(this, APPLICATION_NAME, nodes).start();
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        shell.dispose();
        exit();
    }

    public void exit() {
        for (SxpNode sxpNode : nodes.values()) {
            sxpNode.shutdown();
        }

        LOG.info("Runtime \"" + APPLICATION_NAME + "\" closed");
        System.exit(0);
    }

    public Shell getShell() {
        return shell;
    }

    private void initShell(Shell shell) {
        Composite buttonsComposite = new Composite(shell, SWT.NONE);
        buttonsComposite.setLayout(new RowLayout());
        Button button_expand_all = new Button(buttonsComposite, SWT.PUSH);
        button_expand_all.setText("+");
        RowData rowData = new RowData(23, 23);
        button_expand_all.setLayoutData(rowData);
        button_expand_all.setToolTipText("Expand all");
        Button button_collapse = new Button(buttonsComposite, SWT.PUSH);
        button_collapse.setText("-");
        button_collapse.setLayoutData(rowData);
        button_collapse.setToolTipText("Collapse all");
        button_run = new Button(buttonsComposite, SWT.PUSH);
        button_run.setText(Action.Run.toString());
        rowData = new RowData(30, 23);
        button_run.setLayoutData(rowData);
        button_run.setToolTipText(Action.Run.toString() + " selected nodes");
        button_shutdown = new Button(buttonsComposite, SWT.PUSH);
        button_shutdown.setText(Action.Shutdown.toString());
        rowData = new RowData(65, 23);
        button_shutdown.setLayoutData(rowData);
        button_shutdown.setToolTipText(Action.Shutdown.toString() + " selected nodes");
        tree_nodes = new NodesTree(shell, wndWidth, wndHeight);
        tree = tree_nodes.getTree();
        button_expand_all.addSelectionListener(new ExpandAllItemsListener(tree, true));
        button_collapse.addSelectionListener(new ExpandAllItemsListener(tree, false));
        org.eclipse.swt.graphics.Point size = tree.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int width = Math.max(shell.getSize().x, size.x);
        int height = Math.max(shell.getSize().y, size.y);
        shell.setSize(shell.computeSize(width, height));
        shell.redraw();
    }

    private void load() {
        if (tree_nodes != null) {
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        tree_nodes.loadData(nodes);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                if (button_run != null) {
                    button_run.addSelectionListener(new NodesTreeListener(tree, nodes));
                }
                if (button_shutdown != null) {
                    button_shutdown.addSelectionListener(new NodesTreeListener(tree, nodes));
                }
            }
        });

    }

    public void updateAppTitle() {
        if (shell != null) {
            shell.setText(APPLICATION_NAME + " @ " + wndWidth + "x" + wndHeight);
            shell.redraw();
        }
    }
}
