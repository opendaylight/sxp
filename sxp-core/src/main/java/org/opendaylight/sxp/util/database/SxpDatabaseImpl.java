/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.sxp.util.database.spi.SxpDatabaseAccess;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseProvider;
import org.opendaylight.sxp.util.exception.node.NodeIdNotDefinedException;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.sxp.database.Vpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;

public class SxpDatabaseImpl extends SxpDatabaseProvider {

    protected SxpDatabase database = new SxpDatabaseBuilder().build();

    public SxpDatabaseImpl() {
        super(null);
        SxpDatabaseBuilder databaseBuilder = new SxpDatabaseBuilder();
        databaseBuilder.setAttribute(new ArrayList<Attribute>());
        databaseBuilder.setPathGroup(new ArrayList<PathGroup>());
        databaseBuilder.setVpn(new ArrayList<Vpn>());
        database = databaseBuilder.build();
    }

    public SxpDatabaseImpl(SxpDatabase database) {
        super(null);
        this.database = database;
    }

    public SxpDatabaseImpl(SxpDatabaseAccess databaseAccess) {
        super(databaseAccess);
    }

    private boolean addBindingIdentity(SxpBindingIdentity bindingIdentity) {
        synchronized (database) {
            if (database.getPathGroup() != null && !database.getPathGroup().isEmpty()) {

                boolean contain1 = false;
                for (PathGroup pathGroup : database.getPathGroup()) {
                    if (bindingIdentity.pathGroup.getPathHash().equals(pathGroup.getPathHash())) {
                        contain1 = true;

                        if (pathGroup.getPrefixGroup() != null && !pathGroup.getPrefixGroup().isEmpty()) {
                            boolean contain2 = false;
                            for (PrefixGroup prefixGroup : pathGroup.getPrefixGroup()) {
                                if (bindingIdentity.prefixGroup.getSgt().getValue()
                                        .equals(prefixGroup.getSgt().getValue())) {
                                    contain2 = true;
                                    if (prefixGroup.getBinding() != null && !prefixGroup.getBinding().isEmpty()) {
                                        boolean contain3 = false;

                                        // Reconciliation cleanup.
                                        List<Binding> removed = new ArrayList<>();
                                        List<Binding> added = new ArrayList<>();

                                        for (Binding binding : prefixGroup.getBinding()) {
                                            if (IpPrefixConv.equalTo(binding.getIpPrefix(),
                                                    bindingIdentity.binding.getIpPrefix())) {
                                                contain3 = true;

                                                if (binding.isCleanUp() != null && binding.isCleanUp()) {
                                                    BindingBuilder bindingBuilder = new BindingBuilder(binding);
                                                    bindingBuilder.setCleanUp(false);
                                                    added.add(bindingBuilder.build());
                                                    removed.add(binding);
                                                }
                                                break;
                                            }
                                        }
                                        if (!contain3) {
                                            prefixGroup.getBinding().add(bindingIdentity.binding);
                                            return true;
                                        } else if (!added.isEmpty()) {
                                            prefixGroup.getBinding().removeAll(removed);
                                            prefixGroup.getBinding().addAll(added);
                                            return true;
                                        }
                                    } else if (pathGroup.getPrefixGroup() != null) {
                                        prefixGroup.getBinding().add(bindingIdentity.binding);
                                        return true;
                                    }
                                }
                            }
                            if (!contain2) {
                                pathGroup.getPrefixGroup().add(bindingIdentity.prefixGroup);
                                return true;
                            }
                        } else if (pathGroup.getPrefixGroup() != null) {
                            pathGroup.getPrefixGroup().add(bindingIdentity.prefixGroup);
                            return true;
                        }
                    }
                }
                if (!contain1) {
                    database.getPathGroup().add(bindingIdentity.pathGroup);
                    return true;
                }
            } else {
                database.getPathGroup().add(bindingIdentity.pathGroup);
                return true;
            }

            return false;
        }
    }

    @Override
    public boolean addBindings(SxpDatabase database) throws Exception {
        boolean result = false;
        List<SxpBindingIdentity> removed = new ArrayList<>();
        List<SxpBindingIdentity> added = new ArrayList<>();

        if (database.getPathGroup() != null) {
            for (PathGroup pathGroup : database.getPathGroup()) {
                if (pathGroup.getPrefixGroup() != null) {
                    for (PrefixGroup prefixGroup : pathGroup.getPrefixGroup()) {
                        if (prefixGroup.getBinding() != null) {
                            for (Binding binding : prefixGroup.getBinding()) {
                                SxpBindingIdentity newBindingIdentity = SxpBindingIdentity.create(binding, prefixGroup,
                                        pathGroup);
                                SxpBindingIdentity oldBindingIdentity = getBindingIdentity(newBindingIdentity, true);
                                if (oldBindingIdentity != null) {
                                    removed.add(oldBindingIdentity);
                                }
                                added.add(newBindingIdentity);
                            }
                        }
                    }
                }
            }
        }

        for (SxpBindingIdentity bindingIdentity : removed) {
            deleteBindingIdentity(bindingIdentity);
        }
        for (SxpBindingIdentity bindingIdentity : added) {
            if (addBindingIdentity(bindingIdentity)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public void cleanUpBindings(NodeId nodeId) throws Exception {
        if (nodeId == null) {
            throw new NodeIdNotDefinedException();
        }
        synchronized (database) {
            if (database.getPathGroup() != null) {
                for (PathGroup pathGroup : database.getPathGroup()) {
                    boolean cleanUp = false;
                    if (pathGroup.getPeerSequence() != null) {
                        for (NodeId peerId : NodeIdConv.getPeerSequence(pathGroup.getPeerSequence())) {
                            if (NodeIdConv.equalTo(peerId, nodeId)) {
                                cleanUp = true;
                            }
                            // Only first one item lookup.
                            break;
                        }
                    }
                    if (cleanUp) {
                        if (pathGroup.getPrefixGroup() != null) {

                            List<PrefixGroup> removed1 = new ArrayList<>();
                            for (PrefixGroup prefixGroup : pathGroup.getPrefixGroup()) {
                                if (prefixGroup.getBinding() != null) {

                                    // Reconciliation cleanup.
                                    List<Binding> removed2 = new ArrayList<>();
                                    for (Binding binding : prefixGroup.getBinding()) {
                                        if (binding.isCleanUp() != null && binding.isCleanUp()) {
                                            removed2.add(binding);
                                        }
                                    }

                                    if (!removed2.isEmpty()) {
                                        prefixGroup.getBinding().removeAll(removed2);

                                        if (prefixGroup.getBinding().isEmpty()) {
                                            removed1.add(prefixGroup);
                                        }
                                    }
                                }
                            }
                            if (!removed1.isEmpty()) {
                                pathGroup.getPrefixGroup().removeAll(removed1);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean deleteBindingIdentity(SxpBindingIdentity bindingIdentity) {
        synchronized (database) {
            if (database.getPathGroup() != null) {
                for (PathGroup pathGroup : database.getPathGroup()) {
                    if (bindingIdentity.pathGroup.getPathHash().equals(pathGroup.getPathHash())) {
                        if (pathGroup.getPrefixGroup() != null) {
                            for (PrefixGroup prefixGroup : pathGroup.getPrefixGroup()) {
                                if (bindingIdentity.prefixGroup.getSgt().getValue()
                                        .equals(prefixGroup.getSgt().getValue())) {
                                    if (prefixGroup.getBinding() != null) {
                                        for (Binding _binding : prefixGroup.getBinding()) {
                                            if (IpPrefixConv.equalTo(_binding.getIpPrefix(),
                                                    bindingIdentity.binding.getIpPrefix())) {
                                                prefixGroup.getBinding().remove(_binding);

                                                if (prefixGroup.getBinding().isEmpty()) {
                                                    pathGroup.getPrefixGroup().remove(prefixGroup);
                                                    if (pathGroup.getPrefixGroup().isEmpty()) {
                                                        database.getPathGroup().remove(pathGroup);
                                                    }
                                                }
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }
    }

    @Override
    public List<SxpBindingIdentity> deleteBindings(SxpDatabase database) throws Exception {
        List<SxpBindingIdentity> removed = new ArrayList<>();

        if (database.getPathGroup() != null) {
            for (PathGroup pathGroup : database.getPathGroup()) {
                if (pathGroup.getPrefixGroup() != null) {
                    for (PrefixGroup prefixGroup : pathGroup.getPrefixGroup()) {
                        if (prefixGroup.getBinding() != null) {
                            for (Binding binding : prefixGroup.getBinding()) {
                                SxpBindingIdentity newBindingIdentity = SxpBindingIdentity.create(binding, prefixGroup,
                                        pathGroup);
                                SxpBindingIdentity oldBindingIdentity = getBindingIdentity(newBindingIdentity, true);
                                if (oldBindingIdentity != null) {
                                    removed.add(oldBindingIdentity);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (SxpBindingIdentity bindingIdentity : removed) {
            deleteBindingIdentity(bindingIdentity);
        }
        return removed;
    }

    @Override
    public SxpDatabase get() throws Exception {
        return database;
    }

    /**
     * @param bindingIdentity
     *            a tree item identification
     * @param ipsgtCoupled
     *            if a binding is coupled, i.e. one IP address can exist only in
     *            one group (Cisco devices impl.)
     * @return
     */
    private SxpBindingIdentity getBindingIdentity(SxpBindingIdentity bindingIdentity, boolean ipsgtCoupled) {
        synchronized (database) {
            if (database.getPathGroup() != null) {
                for (PathGroup pathGroup : database.getPathGroup()) {
                    if (bindingIdentity.pathGroup.getPathHash().equals(pathGroup.getPathHash())) {
                        if (pathGroup.getPrefixGroup() != null) {
                            for (PrefixGroup prefixGroup : pathGroup.getPrefixGroup()) {
                                if (bindingIdentity.prefixGroup.getSgt().getValue()
                                        .equals(prefixGroup.getSgt().getValue())
                                        || ipsgtCoupled) {
                                    if (prefixGroup.getBinding() != null) {
                                        for (Binding _binding : prefixGroup.getBinding()) {
                                            if (IpPrefixConv.equalTo(_binding.getIpPrefix(),
                                                    bindingIdentity.binding.getIpPrefix())) {
                                                return SxpBindingIdentity.create(_binding, prefixGroup, pathGroup);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    @Override
    public void purgeBindings(NodeId nodeId) throws Exception {
        if (nodeId == null) {
            throw new NodeIdNotDefinedException();
        }
        synchronized (database) {
            if (database.getPathGroup() != null) {
                List<PathGroup> removed = new ArrayList<>();
                for (PathGroup pathGroup : database.getPathGroup()) {
                    if (pathGroup.getPeerSequence() != null) {
                        for (NodeId peerId : NodeIdConv.getPeerSequence(pathGroup.getPeerSequence())) {
                            if (NodeIdConv.equalTo(peerId, nodeId)) {
                                removed.add(pathGroup);
                            }
                            // Only first one item lookup.
                            break;
                        }
                    }
                }
                database.getPathGroup().removeAll(removed);
            }
        }
    }

    @Override
    public List<SxpBindingIdentity> readBindings() throws Exception {
        List<SxpBindingIdentity> read = new ArrayList<>();
        synchronized (database) {
            if (database.getPathGroup() != null) {
                for (PathGroup pathGroup : database.getPathGroup()) {
                    if (pathGroup.getPrefixGroup() != null) {
                        for (PrefixGroup prefixGroup : pathGroup.getPrefixGroup()) {
                            if (prefixGroup.getBinding() != null) {
                                for (Binding binding : prefixGroup.getBinding()) {
                                    read.add(SxpBindingIdentity.create(binding, prefixGroup, pathGroup));
                                }
                            }
                        }
                    }
                }
            }
            return read;
        }
    }

    @Override
    public void setAsCleanUp(NodeId nodeId) throws Exception {
        if (nodeId == null) {
            throw new NodeIdNotDefinedException();
        }

        synchronized (database) {
            if (database.getPathGroup() != null) {
                for (PathGroup pathGroup : database.getPathGroup()) {
                    boolean cleanUp = false;
                    if (pathGroup.getPeerSequence() != null) {
                        for (NodeId peerId : NodeIdConv.getPeerSequence(pathGroup.getPeerSequence())) {
                            if (NodeIdConv.equalTo(peerId, nodeId)) {
                                cleanUp = true;
                            }
                            // Only first one item lookup.
                            break;
                        }
                    }
                    if (cleanUp) {
                        if (pathGroup.getPrefixGroup() != null) {
                            for (PrefixGroup prefixGroup : pathGroup.getPrefixGroup()) {
                                if (prefixGroup.getBinding() != null) {

                                    // Reconciliation cleanup.
                                    List<Binding> removed = new ArrayList<>();
                                    List<Binding> added = new ArrayList<>();

                                    for (Binding binding : prefixGroup.getBinding()) {
                                        BindingBuilder bindingBuilder = new BindingBuilder(binding);
                                        bindingBuilder.setCleanUp(true);
                                        added.add(bindingBuilder.build());
                                        removed.add(binding);
                                    }

                                    if (!added.isEmpty()) {
                                        prefixGroup.getBinding().removeAll(removed);
                                        prefixGroup.getBinding().addAll(added);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        String result = this.getClass().getSimpleName();
        synchronized (database) {
            if (database.getPathGroup() != null) {
                for (PathGroup pathGroup : database.getPathGroup()) {
                    result += "\n" + PRINT_DELIMITER + NodeIdConv.toString(pathGroup.getPeerSequence());
                    if (pathGroup.getPrefixGroup() != null) {
                        for (PrefixGroup prefixGroup : pathGroup.getPrefixGroup()) {
                            result += "\n" + PRINT_DELIMITER + PRINT_DELIMITER + prefixGroup.getSgt().getValue() + " ";
                            if (prefixGroup.getBinding() != null) {
                                for (Binding binding : prefixGroup.getBinding()) {
                                    result += IpPrefixConv.toString(binding.getIpPrefix());
                                    if (binding.isCleanUp() != null && binding.isCleanUp()) {
                                        result += " [CleanUp]";
                                    } else {
                                        result += " ["
                                                + (binding.getTimestamp() == null
                                                        || binding.getTimestamp().getValue() == null ? "" : binding
                                                        .getTimestamp().getValue()) + "]";
                                    }
                                    result += " ";
                                }
                                result = result.trim();
                            }
                        }
                    }
                }
            }
            return result;
        }
    }
}
