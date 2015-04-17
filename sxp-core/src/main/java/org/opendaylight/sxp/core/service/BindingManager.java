/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.MasterBindingIdentity;
import org.opendaylight.sxp.util.database.SxpBindingIdentity;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseProvider;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BindingManager extends Service {

    protected static final Logger LOG = LoggerFactory.getLogger(BindingManager.class.getName());

    private static boolean contains(
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup> masterPrefixGroups,
            Sgt sgt, IpPrefix ipPrefix) {
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup prefixGroup : masterPrefixGroups) {
            if (!prefixGroup.getSgt().equals(sgt)) {
                continue;
            }
            if (prefixGroup.getBinding() == null) {
                continue;
            }
            for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding binding : prefixGroup
                    .getBinding()) {
                if (IpPrefixConv.equalTo(binding.getIpPrefix(), ipPrefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup getPrefixGroup(
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup> masterPrefixGroups,
            SxpBindingIdentity bindingItentity) {
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup prefixGroup : masterPrefixGroups) {
            if (prefixGroup.getSgt().getValue().equals(bindingItentity.getPrefixGroup().getSgt().getValue())) {
                return prefixGroup;
            }
        }

        // If not exists, create new one.
        PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder();
        prefixGroupBuilder.setSgt(bindingItentity.getPrefixGroup().getSgt());
        prefixGroupBuilder
                .setBinding(new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding>());
        org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup prefixGroup = prefixGroupBuilder
                .build();
        masterPrefixGroups.add(prefixGroup);
        return prefixGroup;
    }

    private final BlockingQueue<Boolean> queue = new LinkedBlockingQueue<>(32);

    public BindingManager(SxpNode owner) {
        super(owner);
    }

    @Override
    public void cancel() {
        super.cancel();
        notifyChange(Boolean.FALSE); 
    }

    public void cleanUpBindings(NodeId nodeID) throws Exception {
        getBindingSxpDatabase().cleanUpBindings(nodeID);

        LOG.info(owner + " cleanUpBindings {}", getBindingSxpDatabase());

    }

    private void databaseArbitration(SxpDatabaseInf database) throws Exception {
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup> masterPrefixGroupsContributed = new ArrayList<>();

        // Through all bindings in the arbitrated database: Find a related
        // contributed binding to each arbitrated binding.
        for (SxpBindingIdentity bindingItentity : database.readBindings()) {
            // Already processed.
            if (contains(masterPrefixGroupsContributed, bindingItentity.getPrefixGroup().getSgt(), bindingItentity
                    .getBinding().getIpPrefix())) {
                continue;
            }

            // Get prefix group if already created.
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup prefixGroup = getPrefixGroup(
                    masterPrefixGroupsContributed, bindingItentity);

            BindingBuilder bindingContributedBuilder = new BindingBuilder();
            bindingContributedBuilder.setAction(DatabaseAction.Add);
            bindingContributedBuilder.setChanged(true);
            bindingContributedBuilder.setIpPrefix(bindingItentity.getBinding().getIpPrefix());
            bindingContributedBuilder.setPeerSequence(NodeIdConv.createPeerSequence(null));
            bindingContributedBuilder.setSources(NodeIdConv.createSources(null));
            bindingContributedBuilder.setTimestamp(bindingItentity.getBinding().getTimestamp());

            // Find binding with the short path or the best timestamp.
            int pathLength = -1;

            if (database.get().getPathGroup() != null) {
                for (PathGroup _pathGroup : database.get().getPathGroup()) {
                    if (_pathGroup.getPrefixGroup() != null) {
                        for (PrefixGroup _prefixGroup : _pathGroup.getPrefixGroup()) {
                            boolean contains = false;
                            if (_prefixGroup.getBinding() != null) {
                                for (Binding _binding : _prefixGroup.getBinding()) {
                                    if (IpPrefixConv.equalTo(_binding.getIpPrefix(), bindingItentity.getBinding()
                                            .getIpPrefix())) {
                                        contains = true;
                                        break;
                                    }
                                }
                            }
                            if (!contains) {
                                continue;
                            }
                            bindingContributedBuilder.setPeerSequence(_pathGroup.getPeerSequence());
                            bindingContributedBuilder.setSources(NodeIdConv.createSources(null));
                            // Shortest Path rule.
                            int _pathLength = NodeIdConv.getPeerSequence(_pathGroup.getPeerSequence()).size();
                            if (_pathLength < pathLength || pathLength == -1) {
                                pathLength = _pathLength;
                            }
                            // Test - Most Recently Received rule.
                            else if (_pathLength == pathLength) {
                                for (Binding _binding : _prefixGroup.getBinding()) {
                                    if (IpPrefixConv.equalTo(_binding.getIpPrefix(),
                                            bindingContributedBuilder.getIpPrefix())
                                            && TimeConv.toLong(_binding.getTimestamp()) < TimeConv
                                                    .toLong(bindingContributedBuilder.getTimestamp())) {
                                        bindingContributedBuilder.setPeerSequence(_pathGroup.getPeerSequence());
                                        bindingContributedBuilder.setSources(NodeIdConv.createSources(null));
                                        bindingContributedBuilder.setIpPrefix(_binding.getIpPrefix());
                                        //was generating duplicities of previously added timestamps
                                        //bindingContributedBuilder.setTimestamp(_binding.getTimestamp());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (pathLength != -1) {
                prefixGroup.getBinding().add(bindingContributedBuilder.build());
            }
        }

        // Detect sources for each unique binding.
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup prefixGroup : masterPrefixGroupsContributed) {
            if (prefixGroup.getBinding() != null) {
                for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding binding : prefixGroup
                        .getBinding()) {

                    for (SxpBindingIdentity bindingItentity : database.readBindings()) {
                        if (IpPrefixConv.equalTo(bindingItentity.getBinding().getIpPrefix(), binding.getIpPrefix())) {
                            List<NodeId> peerSequence = NodeIdConv.getPeerSequence(bindingItentity.getPathGroup()
                                    .getPeerSequence());
                            // Add the last one item.
                            if (peerSequence.size() > 0) {
                                NodeId sourceId = peerSequence.get(peerSequence.size() - 1);
                                boolean contains = false;
                                for (NodeId _sourceId : NodeIdConv.getSources(binding.getSources())) {
                                    if (NodeIdConv.equalTo(_sourceId, sourceId)) {
                                        contains = true;
                                        break;
                                    }
                                }

                                if (!contains) {
                                    // Sources should be appropriately
                                    // initialized.
                                    binding.getSources().getSource().add(sourceId);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Contribute with unique bindings.
        SourceBuilder sourceBuilder = new SourceBuilder();
        sourceBuilder.setBindingSource(DatabaseBindingSource.Sxp);
        sourceBuilder.setPrefixGroup(masterPrefixGroupsContributed);
        Source sourceContributed = sourceBuilder.build();

        List<MasterBindingIdentity> masterBindingIdentityContributed = new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup prefixGroupContributed : masterPrefixGroupsContributed) {
            if (prefixGroupContributed.getBinding() != null) {
                for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding bindingContributed : prefixGroupContributed
                        .getBinding()) {
                    masterBindingIdentityContributed.add(MasterBindingIdentity.create(bindingContributed,
                            prefixGroupContributed, sourceContributed));
                }
            }
        }

        getBindingMasterDatabase().addBindings(owner.getNodeId(), masterBindingIdentityContributed);
    }

    @Override
    public void notifyChange() {
        notifyChange(Boolean.TRUE);                
    }
    
    private void notifyChange(boolean notification) {
        try {
            queue.add(notification);
        } catch (IllegalStateException e1) {
            try {
                queue.put(notification);
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
        }        
    }

    public void purgeBindings(NodeId nodeID) throws Exception {
        getBindingSxpDatabase().purgeBindings(nodeID);
    }

    @Override
    public void run() {
        LOG.debug(owner + " Starting {}", BindingManager.class.getSimpleName());
        Boolean notification;

        while (!finished) {
            try {
                notification = queue.take();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }

            // Shutdown.
            if (!notification) {
                break;
            } else if (!owner.isEnabled()) {
                notifyChange(notification);                
                continue;
            }

            // Provide one arbitration process for all received
            // notifications in one cycle.
            queue.clear();
            try {
                SxpDatabaseInf sxpDatabase = getBindingSxpDatabase();
                synchronized (sxpDatabase) {
                    databaseArbitration(sxpDatabase);

                    // Listener databases clearing: Remove deleted bindings.
                    if (!owner.isSvcBindingDispatcherStarted()) {
                        MasterDatabaseProvider masterDatabase = getBindingMasterDatabase();
                        synchronized (masterDatabase) {
                            masterDatabase.purgeAllDeletedBindings();
                        }
                    }

                    // LOG.info(owner + " {}", sxpDatabase);
                }
                owner.setSvcBindingDispatcherDispatch();

            } catch (Exception e) {
                LOG.warn(owner + " " + BindingManager.class.getSimpleName() + " | {} | {}", e.getClass()
                        .getSimpleName(), e.getMessage());
                e.printStackTrace();
                continue;
            }
        }
        LOG.info(owner + " Shutdown {}", getClass().getSimpleName());
    }

    public void setAsCleanUp(NodeId nodeID) throws Exception {
        getBindingSxpDatabase().setAsCleanUp(nodeID);
        LOG.info(owner + " setAsCleanUp {}", getBindingSxpDatabase());
    }
}
