/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.io.ui;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class ExpandAllItemsListener implements SelectionListener {
    private boolean expand = false;

    private Tree tree = null;

    public ExpandAllItemsListener(Tree tree, Boolean expand) {
        this.tree = tree;
        this.expand = expand;
    }

    private void expandSubTreeItems(TreeItem treeItem) {
        for (TreeItem subTreeItem : treeItem.getItems()) {
            subTreeItem.setExpanded(expand);
            expandSubTreeItems(subTreeItem);
        }
    }

    private void expandTreeItems() {
        if (tree == null) {
            return;
        }
        TreeItem[] treeItems = tree.getItems();
        if (treeItems != null) {
            for (TreeItem treeItem : treeItems) {
                treeItem.setExpanded(expand);
                expandSubTreeItems(treeItem);
            }
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        expandTreeItems();
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        expandTreeItems();
    }
}
