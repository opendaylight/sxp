/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.io.ui;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.opendaylight.sxp.core.SxpNode;

public class NodesTree {
    protected static HashMap<String, SxpNode> nodes;

    protected static TreeItem findTreeItem(TreeItem rootItem, String treeItemID) {
        if (rootItem.getText().equals(treeItemID)) {
            return rootItem;
        }
        for (TreeItem subTreeItem : rootItem.getItems()) {
            if (subTreeItem.getText().equals(treeItemID)) {
                return subTreeItem;
            }
            TreeItem result = findTreeItem(subTreeItem, treeItemID);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Tree tree;

    public NodesTree(Composite parent, int width, int height) {
        Composite treeComposite = new Composite(parent, SWT.NONE);
        treeComposite.setLayout(new RowLayout(SWT.VERTICAL));
        Text text = new Text(treeComposite, SWT.NONE);
        text.setText("Loaded nodes");
        text.setEnabled(false);
        tree = new Tree(treeComposite, SWT.MULTI | SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        tree.setHeaderVisible(true);
        tree.setLinesVisible(false);
        tree.setLayoutData(new RowData(width - 35, height - 115));
    }

    public Tree getTree() {
        return tree;
    }

    public void loadData(HashMap<String, SxpNode> nodes) throws Exception {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        List<TreeItem> expandedTreeItems = new ArrayList<>();
        for (String _nodeName : nodes.keySet()) {
            SxpNode node = nodes.get(_nodeName);

            String nodeName = node.toString();
            // Search for key-node in which we can link to.
            TreeItem rootItem = null;
            TreeItem[] rootItems = tree.getItems();
            if (rootItems != null) {
                for (TreeItem myRootItem : rootItems) {
                    rootItem = findTreeItem(myRootItem, nodeName);
                    if (rootItem != null) {
                        break;
                    }
                }
            }
            if (rootItem == null) {
                rootItem = new TreeItem(tree, SWT.NONE);
                rootItem.setText(nodeName);
                rootItem.setData(node);

                TreeItem dbItem = new TreeItem(rootItem, SWT.NONE);
                dbItem.setText("databases");

                try {
                    TreeItem mdbItem = new TreeItem(dbItem, SWT.NONE);
                    mdbItem.setText("master");
                    mdbItem.setData(node.getBindingMasterDatabase());
                } catch (Exception e) {
                }
                try {
                    node.getBindingSxpDatabase();
                    TreeItem sdbItem = new TreeItem(dbItem, SWT.NONE);
                    sdbItem.setText("sxp");
                    sdbItem.setData(node.getBindingSxpDatabase());
                } catch (Exception e) {
                }

                rootItem = new TreeItem(rootItem, SWT.NONE);
                rootItem.setText("connections");
                rootItem.setData(node);
            }
            // Fill tree with items, for each item the data is set.
            for (InetSocketAddress connId : node.keySet()) {
                TreeItem connItem = new TreeItem(rootItem, SWT.NONE);
                connItem.setText(node.get(connId).toString());
                connItem.setData(node.get(connId));
            }
        }
        // At first tree has to be constructed, and then modified with
        // expansions.
        for (TreeItem treeItem : expandedTreeItems) {
            treeItem.setChecked(true);
            treeItem.setGrayed(true);
            treeItem.setExpanded(true);
        }
        tree.redraw();
    }
}
