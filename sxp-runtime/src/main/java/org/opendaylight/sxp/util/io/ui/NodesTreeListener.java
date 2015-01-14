/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.io.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.io.IOManager;
import org.opendaylight.sxp.util.io.IOManager.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodesTreeListener implements SelectionListener {

    private static final Logger LOG = LoggerFactory.getLogger(IOManager.class.getName());

    private static HashMap<String, SxpNode> nodes;

    private Tree tree = null;

    public NodesTreeListener(Tree tree, HashMap<String, SxpNode> nodes) {
        this.tree = tree;
        NodesTreeListener.nodes = nodes;
    }

    private List<TreeItem> getCheckedSubTreeItems(TreeItem treeItem) {
        List<TreeItem> checkedTreeItems = new ArrayList<>();
        for (TreeItem subTreeItem : treeItem.getItems()) {
            if (subTreeItem.getChecked()) {
                checkedTreeItems.add(subTreeItem);
                checkedTreeItems.addAll(getCheckedSubTreeItems(subTreeItem));
            }
        }
        return checkedTreeItems;
    }

    private List<TreeItem> getCheckedTreeItems() {
        if (tree == null) {
            return null;
        }
        List<TreeItem> checkedTreeItems = new ArrayList<>();
        TreeItem[] treeItems = tree.getItems();
        if (treeItems != null) {
            for (TreeItem treeItem : treeItems) {
                if (treeItem.getChecked()) {
                    checkedTreeItems.add(treeItem);
                    checkedTreeItems.addAll(getCheckedSubTreeItems(treeItem));
                }
            }
        }
        return checkedTreeItems;
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {

    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        if (nodes == null) {
            return;
        }
        List<TreeItem> checkedTreeItems = getCheckedTreeItems();
        if (checkedTreeItems == null) {
            return;
        }

        if (event.getSource() instanceof Button) {
            if (((Button) event.getSource()).getText().equals(Action.Run.toString())) {
                for (TreeItem treeItem : checkedTreeItems) {
                    if (treeItem.getData() instanceof SxpNode) {
                        LOG.info(treeItem.getText() + " Starting.. ");
                        try {
                            ((SxpNode) treeItem.getData()).start();
                        } catch (Exception e) {
                            LOG.info(treeItem.getText() + " {} | {}", e.getClass().getSimpleName(), e.getMessage());
                        }
                    }
                }
            } else if (((Button) event.getSource()).getText().equals(Action.Shutdown.toString())) {
                for (TreeItem treeItem : checkedTreeItems) {
                    if (treeItem.getData() instanceof SxpNode) {
                        LOG.info(treeItem.getText() + " Shutdown.. ");
                        ((SxpNode) treeItem.getData()).shutdown();
                    }
                }
            }
        }
    }
}
