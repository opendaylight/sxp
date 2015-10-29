/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import com.google.common.base.Preconditions;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseAccess;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseProvider;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.sxp.util.exception.node.NodeIdNotDefinedException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.master.database.Vpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MasterDatabaseImpl class contains logic to operate with Database,
 * used for storing all Bindings and their propagation
 */
public class MasterDatabaseImpl extends MasterDatabaseProvider {

    protected MasterDatabase database;

    /**
     * Default constructor that sets empty Database
     */
    public MasterDatabaseImpl() {
        super(null);
        MasterDatabaseBuilder databaseBuilder = new MasterDatabaseBuilder();
        databaseBuilder.setAttribute(new ArrayList<Attribute>());
        databaseBuilder.setSource(new ArrayList<Source>());
        databaseBuilder.setVpn(new ArrayList<Vpn>());
        database = databaseBuilder.build();
    }

    /**
     * Constructor that sets predefined Database
     *
     * @param database MasterDatabase to be used
     */
    public MasterDatabaseImpl(MasterDatabase database) {
        super(null);
        this.database = database;
    }

    /**
     * Constructor that sets predefined access checked Database
     *
     * @param databaseAccess MasterDatabaseAccess to be used
     */
    public MasterDatabaseImpl(MasterDatabaseAccess databaseAccess) {
        super(databaseAccess);
    }

    /**
     * Adds MasterBindingIdentity into Database
     *
     * @param bindingIdentity MasterBindingIdentity to be added
     * @return If operation was successful
     */
    private boolean addBindingIdentity(MasterBindingIdentity bindingIdentity) {
        synchronized (database) {
            if (database.getSource() != null && !database.getSource().isEmpty()) {

                boolean contain1 = false;
                for (Source source : database.getSource()) {
                    if (bindingIdentity.source.getBindingSource().equals(source.getBindingSource())) {
                        contain1 = true;

                        if (source.getPrefixGroup() != null && !source.getPrefixGroup().isEmpty()) {
                            boolean contain2 = false;
                            for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                                if (bindingIdentity.prefixGroup.getSgt().getValue()
                                        .equals(prefixGroup.getSgt().getValue())) {
                                    contain2 = true;
                                    if (prefixGroup.getBinding() != null && !prefixGroup.getBinding().isEmpty()) {
                                        boolean contain3 = false;
                                        for (Binding binding : prefixGroup.getBinding()) {
                                            if (IpPrefixConv.equalTo(binding.getIpPrefix(),
                                                    bindingIdentity.binding.getIpPrefix()) && binding.getPeerSequence()
                                                    .equals(bindingIdentity.getBinding().getPeerSequence())) {
                                                contain3 = true;
                                                break;
                                            }
                                        }
                                        if (!contain3) {
                                            prefixGroup.getBinding().add(bindingIdentity.binding);
                                            return true;
                                        }
                                    } else if (source.getPrefixGroup() != null) {
                                        prefixGroup.getBinding().add(bindingIdentity.binding);
                                        return true;
                                    }
                                }
                            }
                            if (!contain2) {
                                source.getPrefixGroup().add(bindingIdentity.prefixGroup);
                                return true;
                            }
                        } else if (source.getPrefixGroup() != null) {
                            source.getPrefixGroup().add(bindingIdentity.prefixGroup);
                            return true;
                        }
                    }
                }
                if (!contain1) {
                    database.getSource().add(bindingIdentity.source);
                    return true;
                }
            } else {
                database.getSource().add(bindingIdentity.source);
                return true;
            }

            return false;
        }
    }

    @Override
    public void addBindings(NodeId owner, List<MasterBindingIdentity> contributedBindingIdentities)
            throws NodeIdNotDefinedException, DatabaseAccessException {
        if (owner == null) {
            throw new NodeIdNotDefinedException();
        }
        if (contributedBindingIdentities == null ) {
            return;
        }
        // Local bindings have the priority.
        // Clear binding sources except the local contribution.
        List<MasterBindingIdentity> masterBindingIdentities = new ArrayList<MasterBindingIdentity>();
        synchronized (database) {
            masterBindingIdentities = MasterBindingIdentity.create(database, false);
        }
        for (MasterBindingIdentity bindingIdentity : masterBindingIdentities) {
            if (!bindingIdentity.source.getBindingSource().equals(DatabaseBindingSource.Local)) {
                continue;
            }
            if (bindingIdentity.getBinding() != null && bindingIdentity.getBinding().getSources() != null
                    && bindingIdentity.getBinding().getSources().getSource() != null) {
                bindingIdentity.getBinding().getSources().getSource().clear();
                bindingIdentity.getBinding().getSources().getSource().add(owner);
            }
        }

        // Filter local bindings from contributed.
        List<MasterBindingIdentity> removedContributedBindingIdentities = new ArrayList<>();
        for (MasterBindingIdentity contributedBindingIdentity : contributedBindingIdentities) {
            if (contributedBindingIdentity.getBinding() != null) {
                for (MasterBindingIdentity bindingIdentity : masterBindingIdentities) {
                    if (!bindingIdentity.source.getBindingSource().equals(DatabaseBindingSource.Local)) {
                        continue;
                    }
                    if (bindingIdentity.getBinding() != null && IpPrefixConv.equalTo(
                            bindingIdentity.getBinding().getIpPrefix(),
                            contributedBindingIdentity.getBinding().getIpPrefix()) && bindingIdentity.getBinding()
                            .getAction()
                            .equals(contributedBindingIdentity.binding.getAction())) {
                        // Remove the contributed one (already in local
                        // master database).
                        removedContributedBindingIdentities.add(contributedBindingIdentity);

                        // Add sources to the local binding.
                        for (NodeId _sourceId : NodeIdConv.getSources(contributedBindingIdentity.getBinding()
                                .getSources())) {
                            boolean contains = false;

                            for (NodeId sourceId : NodeIdConv.getSources(bindingIdentity.getBinding().getSources())) {
                                if (NodeIdConv.equalTo(sourceId, _sourceId)) {
                                    contains = true;
                                    break;
                                }
                            }

                            if (!contains) {
                                // Sources should be appropriately initialized.
                                bindingIdentity.getBinding().getSources().getSource().add(_sourceId);
                            }
                        }

                        break;
                    }
                }
            }
        }
        if (!removedContributedBindingIdentities.isEmpty()) {
            contributedBindingIdentities.removeAll(removedContributedBindingIdentities);
        }

        // Subtraction: DeletedBindings = MasterDatabaseNonLocal -
        // ArbitrationOutput.
        List<MasterBindingIdentity> removed = new ArrayList<>();
        List<MasterBindingIdentity> added = new ArrayList<>();
        for (MasterBindingIdentity bindingIdentity : masterBindingIdentities) {
            if (bindingIdentity.source.getBindingSource().equals(DatabaseBindingSource.Local)) {
                continue;
            }
            boolean contains = false;
            if (!contributedBindingIdentities.isEmpty()) {
                for (MasterBindingIdentity _bindingIdentity : contributedBindingIdentities) {
                    if (_bindingIdentity.equals(bindingIdentity)) {
                        contains = true;
                        break;
                    }
                }
            }
            // If a master binding is not in contributed, add it for
            // deletion.
            if (!contains) {
                BindingBuilder bindingBuilder = new BindingBuilder(bindingIdentity.binding);
                bindingBuilder.setAction(DatabaseAction.Delete);
                bindingBuilder.setChanged(true);
                added.add(MasterBindingIdentity.create(bindingBuilder.build(), bindingIdentity.prefixGroup,
                        bindingIdentity.source));

                removed.add(bindingIdentity);
            }
            // If a master binding is in contributed, remark it if it's
            // marked for deletion.
            else if (bindingIdentity.getBinding().getAction() != null
                    && bindingIdentity.getBinding().getAction().equals(DatabaseAction.Delete)) {
                removed.add(bindingIdentity);
            }
        }

        for (MasterBindingIdentity bindingIdentity : removed) {
            deleteBindingIdentity(bindingIdentity);
        }
        for (MasterBindingIdentity bindingIdentity : added) {
            addBindingIdentity(bindingIdentity);
        }
        // Add new bindings.
        for (MasterBindingIdentity bindingIdentity : contributedBindingIdentities) {
            addBindingIdentity(bindingIdentity);
        }
    }

    @Override
    public void addBindingsLocal(SxpNode owner, List<PrefixGroup> prefixGroups) throws DatabaseAccessException {
        Preconditions.checkNotNull(owner);
        if (prefixGroups == null || prefixGroups.isEmpty()) {
            return;
        }
        synchronized (database) {
            Source source = null;
            for (Source _source : database.getSource()) {
                if (_source.getBindingSource().equals(DatabaseBindingSource.Local)) {
                    source = _source;
                    break;
                }
            }
            if (source == null) {
                SourceBuilder sourceBuilder = new SourceBuilder();
                sourceBuilder.setBindingSource(DatabaseBindingSource.Local);
                sourceBuilder.setPrefixGroup(prefixGroups);
                source = sourceBuilder.build();
                database.getSource().add(source);
            } else {
                addPrefixGroups(prefixGroups, source);
            }
            owner.setSvcBindingManagerNotify();
        }
    }

    /**
     * Adds PrefixGroups into Source
     *
     * @param prefixGroups PrefixGroup to be added
     * @param source Source where the PrefixGroup will be added
     */
    private void addPrefixGroups(List<PrefixGroup> prefixGroups, Source source) {
        for (PrefixGroup prefixGroup : prefixGroups) {
            Sgt sgt = prefixGroup.getSgt();

            if (source.getPrefixGroup() != null) {
                PrefixGroup _prefixGroup = null;

                for (PrefixGroup __prefixGroup : source.getPrefixGroup()) {
                    if (__prefixGroup.getSgt().getValue().equals(sgt.getValue())) {
                        _prefixGroup = __prefixGroup;
                        break;
                    }
                }
                if (_prefixGroup == null) {
                    source.getPrefixGroup().add(prefixGroup);
                    continue;
                }

                if (prefixGroup.getBinding() != null) {
                    for (Binding binding : prefixGroup.getBinding()) {

                        Binding _binding = null;
                        if (_prefixGroup.getBinding() != null) {
                            for (Binding __binding : _prefixGroup.getBinding()) {
                                if (__binding.getIpPrefix().equals(binding.getIpPrefix())) {
                                    _binding = __binding;
                                    break;
                                }
                            }
                            if (_binding == null) {
                                _prefixGroup.getBinding().add(binding);
                                continue;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Delete MasterBindingIdentity from database
     *
     * @param bindingIdentity MasterBindingIdentity to be removed
     * @return If operation was successful
     */
    private boolean deleteBindingIdentity(MasterBindingIdentity bindingIdentity) {
        synchronized (database) {
            if (database.getSource() != null) {
                for (Source source : database.getSource()) {
                    if (bindingIdentity.source.getBindingSource().equals(source.getBindingSource())) {
                        if (source.getPrefixGroup() != null) {
                            for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                                if (bindingIdentity.prefixGroup.getSgt().getValue()
                                        .equals(prefixGroup.getSgt().getValue())) {
                                    if (prefixGroup.getBinding() != null) {
                                        for (Binding _binding : prefixGroup.getBinding()) {
                                            if (IpPrefixConv.equalTo(_binding.getIpPrefix(),
                                                    bindingIdentity.binding.getIpPrefix())) {
                                                prefixGroup.getBinding().remove(_binding);

                                                if (prefixGroup.getBinding().isEmpty()) {
                                                    source.getPrefixGroup().remove(prefixGroup);
                                                    if (source.getPrefixGroup().isEmpty()) {
                                                        database.getSource().remove(source);
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
    public void expandBindings(int quantity)
            throws DatabaseAccessException, UnknownPrefixException, UnknownHostException {
        synchronized (database) {
            if (quantity > 0 && database.getSource() != null) {
                for (Source source : database.getSource()) {
                    if (source.getPrefixGroup() != null) {
                        for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                            if (prefixGroup.getBinding() != null) {
                                List<Binding> bindingsExpanded = null;
                                for (Binding binding : prefixGroup.getBinding()) {
                                    int prefixLength = IpPrefixConv.getPrefixLength(binding.getIpPrefix());
                                    // Subnet uses a reasonable expansion size.
                                    if (prefixLength != 32 && prefixLength != 128) {
                                        AtomicInteger _quantity = new AtomicInteger(quantity);
                                        bindingsExpanded = Search.getExpandedBindings(binding, _quantity);
                                    }
                                }
                                // Add expanded.
                                if (bindingsExpanded != null && !bindingsExpanded.isEmpty()) {
                                    prefixGroup.getBinding().addAll(bindingsExpanded);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public MasterDatabase get() throws DatabaseAccessException {
        return database;
    }

    @Override
    public List<MasterDatabase> partition(int quantity, boolean onlyChanged, SxpBindingFilter filter) throws DatabaseAccessException {
        synchronized (database) {
            List<MasterDatabase> split = new ArrayList<>();
            MasterDatabaseBuilder databaseBuilder = new MasterDatabaseBuilder(database);
            databaseBuilder.setSource(new ArrayList<Source>());

            Source source = null;
            PrefixGroup prefixGroup = null;
            int n = 0;

            List<MasterBindingIdentity> bindingIdentities = MasterBindingIdentity.create(database, onlyChanged);

            for (MasterBindingIdentity bindingIdentity : bindingIdentities) {
                if (filter != null && filter.filter(bindingIdentity)) {
                    continue;
                }
                boolean contains = false;
                    SourceBuilder sourceBuilder = new SourceBuilder(bindingIdentity.source);
                    sourceBuilder.setPrefixGroup(new ArrayList<PrefixGroup>());
                    source = sourceBuilder.build();
                    databaseBuilder.getSource().add(source);

                    PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder(bindingIdentity.prefixGroup);
                    prefixGroupBuilder.setBinding(new ArrayList<Binding>());
                    prefixGroup = prefixGroupBuilder.build();
                    source.getPrefixGroup().add(prefixGroup);

                for (Binding _binding : prefixGroup.getBinding()) {
                    if (_binding.getIpPrefix().equals(bindingIdentity.binding.getIpPrefix())) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    BindingBuilder bindingBuilder = new BindingBuilder(bindingIdentity.binding);
                    //Delete replacements have to be marked as change to trigger their propagation
                    if(bindingIdentity.isDeleteReplace()) {
                        bindingBuilder.setChanged(true);
                    }
                    prefixGroup.getBinding().add(bindingBuilder.build());
                    n++;
                }

                if (n >= quantity) {
                    split.add(databaseBuilder.build());
                    databaseBuilder = new MasterDatabaseBuilder(database);
                    databaseBuilder.setSource(new ArrayList<Source>());
                    n = 0;
                }
            }
            if (!databaseBuilder.getSource().isEmpty()) {
                split.add(databaseBuilder.build());
            }
            return split;
        }
    }

    @Override
    public synchronized void purgeAllDeletedBindings() throws DatabaseAccessException {
        synchronized (database) {
            for (Source source : database.getSource()) {
                if (source.getPrefixGroup() != null) {
                    for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                        if (prefixGroup.getBinding() != null) {
                            List<Binding> removed = new ArrayList<Binding>();
                            for (Binding binding : prefixGroup.getBinding()) {
                                if (binding.getAction().equals(DatabaseAction.Delete)) {
                                    removed.add(binding);
                                }
                            }
                            if (!removed.isEmpty()) {
                                prefixGroup.getBinding().removeAll(removed);
                            }
                        }
                    }
                }
            }
            for (Source source : database.getSource()) {
                if (source.getPrefixGroup() != null) {
                    List<PrefixGroup> removed = new ArrayList<PrefixGroup>();
                    for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                        if (prefixGroup.getBinding() != null && prefixGroup.getBinding().isEmpty()) {
                            removed.add(prefixGroup);
                        }
                    }
                    if (!removed.isEmpty()) {
                        source.getPrefixGroup().removeAll(removed);
                    }
                }
            }

            List<Source> removed = new ArrayList<Source>();
            for (Source source : database.getSource()) {
                if (source.getPrefixGroup() != null && source.getPrefixGroup().isEmpty()) {
                    removed.add(source);
                }
            }
            if (!removed.isEmpty()) {
                database.getSource().removeAll(removed);
            }
        }
    }

    @Override
    public void purgeBindings(NodeId nodeId) throws NodeIdNotDefinedException, DatabaseAccessException {
        if (nodeId == null) {
            throw new NodeIdNotDefinedException();
        }
        synchronized (database) {
            if (database.getSource() != null) {
                List<MasterBindingIdentity> removed = new ArrayList<>();
                for (Source source : database.getSource()) {
                    if (source.getPrefixGroup() != null) {
                        for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                            if (prefixGroup.getBinding() != null) {
                                for (Binding binding : prefixGroup.getBinding()) {
                                    if (binding.getPeerSequence() != null) {
                                        for (NodeId peerId : NodeIdConv.getPeerSequence(binding.getPeerSequence())) {
                                            if (NodeIdConv.equalTo(peerId, nodeId)) {
                                                removed.add(MasterBindingIdentity.create(binding, prefixGroup, source));
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                for (MasterBindingIdentity bindingIdentity : removed) {
                    deleteBindingIdentity(bindingIdentity);
                }
            }
        }
    }

    @Override
    public List<MasterBindingIdentity> readBindings() throws DatabaseAccessException {
        List<MasterBindingIdentity> read = new ArrayList<>();
        synchronized (database) {
            if (database.getSource() != null) {
                for (Source source : database.getSource()) {
                    if (source.getPrefixGroup() != null) {
                        for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                            if (prefixGroup.getBinding() != null) {
                                for (Binding binding : prefixGroup.getBinding()) {
                                    read.add(MasterBindingIdentity.create(binding, prefixGroup, source));
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
    public List<PrefixGroup> readBindingsLocal() throws DatabaseAccessException {
        synchronized (database) {
            for (Source source : database.getSource()) {
                if (source.getBindingSource().equals(DatabaseBindingSource.Local)) {
                    return source.getPrefixGroup();
                }
            }
            return new ArrayList<>();
        }
    }

    @Override
    public void resetModified() throws DatabaseAccessException {
        synchronized (database) {
            if (database.getSource() != null) {
                for (Source source : database.getSource()) {
                    if (source.getPrefixGroup() != null) {
                        for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                            List<Binding> removed = new ArrayList<>();
                            List<Binding> added = new ArrayList<>();
                            if (prefixGroup.getBinding() != null) {
                                for (Binding binding : prefixGroup.getBinding()) {
                                    if (binding.isChanged() != null && binding.isChanged()) {
                                        BindingBuilder bindingBuilder = new BindingBuilder(binding);
                                        bindingBuilder.setChanged(false);
                                        added.add(bindingBuilder.build());
                                        removed.add(binding);
                                    }
                                }
                            }
                            if (!removed.isEmpty()) {
                                prefixGroup.getBinding().removeAll(removed);
                            }
                            if (!added.isEmpty()) {
                                prefixGroup.getBinding().addAll(added);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean setAsDeleted(SxpNode owner, List<PrefixGroup> prefixGroups) throws DatabaseAccessException {
        Preconditions.checkNotNull(owner);
        if (prefixGroups == null || prefixGroups.isEmpty()) {
            return false;
        }
        boolean result = false;
        synchronized (database) {
            for (PrefixGroup _prefixGroup : prefixGroups) {
                if (database.getSource() != null) {
                    for (Source source : database.getSource()) {
                        if (source.getPrefixGroup() != null) {
                            for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                                if (!prefixGroup.getSgt().getValue().equals(_prefixGroup.getSgt().getValue())) {
                                    continue;
                                }
                                // Delete all bindings in a prefix group.
                                if (_prefixGroup.getBinding() == null || _prefixGroup.getBinding().isEmpty()) {
                                    if (prefixGroup.getBinding() != null) {
                                        List<Binding> removed = new ArrayList<>();
                                        List<Binding> added = new ArrayList<>();

                                        for (Binding binding : prefixGroup.getBinding()) {
                                            BindingBuilder bindingBuilder = new BindingBuilder(binding);
                                            bindingBuilder.setAction(DatabaseAction.Delete);
                                            bindingBuilder.setChanged(true);
                                            added.add(bindingBuilder.build());
                                            removed.add(binding);
                                        }

                                        if (!removed.isEmpty()) {
                                            prefixGroup.getBinding().removeAll(removed);
                                        }
                                        if (!added.isEmpty()) {
                                            prefixGroup.getBinding().addAll(added);
                                        }
                                    }
                                }
                                // Delete specific bindings.
                                else {
                                    for (Binding _binding : _prefixGroup.getBinding()) {

                                        if (prefixGroup.getBinding() != null) {
                                            List<Binding> removed = new ArrayList<>();
                                            List<Binding> added = new ArrayList<>();

                                            for (Binding binding : prefixGroup.getBinding()) {
                                                if (IpPrefixConv.equalTo(binding.getIpPrefix(), _binding.getIpPrefix())) {
                                                    BindingBuilder bindingBuilder = new BindingBuilder(binding);
                                                    bindingBuilder.setAction(DatabaseAction.Delete);
                                                    bindingBuilder.setChanged(true);
                                                    added.add(bindingBuilder.build());
                                                    removed.add(binding);
                                                }
                                            }

                                            if (!removed.isEmpty()) {
                                                prefixGroup.getBinding().removeAll(removed);
                                            }
                                            if (!added.isEmpty()) {
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
            owner.setSvcBindingManagerNotify();
            return result;
        }
    }

    @Override
    public String toString() {
        String result = this.getClass().getSimpleName();
        synchronized (database) {
            if (database.getSource() != null) {
                for (Source source : database.getSource()) {
                    result += "\n" + PRINT_DELIMITER + source.getBindingSource().toString();
                    if (source.getPrefixGroup() != null) {
                        for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                            result += "\n" + PRINT_DELIMITER + PRINT_DELIMITER + prefixGroup.getSgt().getValue() + " ";
                            if (prefixGroup.getBinding() != null) {
                                for (Binding binding : prefixGroup.getBinding()) {
                                    result += IpPrefixConv.toString(binding.getIpPrefix());
                                    result += " ["
                                            + (binding.getTimestamp() == null
                                                    || binding.getTimestamp().getValue() == null ? "" : binding
                                                    .getTimestamp().getValue()) + "|";
                                    if (binding.isChanged() != null) {
                                        result += binding.isChanged() ? "*" : "";
                                    }
                                    result += binding.getAction() == null ? DatabaseAction.None : binding.getAction();
                                    result += "|Path:" + NodeIdConv.toString(binding.getPeerSequence());
                                    result += "|Src:" + NodeIdConv.toString(binding.getSources()) + "]";
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
