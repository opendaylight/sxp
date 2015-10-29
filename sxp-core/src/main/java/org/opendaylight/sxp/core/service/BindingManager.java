/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.MasterBindingIdentity;
import org.opendaylight.sxp.util.database.SxpBindingIdentity;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseProvider;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseProvider;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.sxp.util.exception.node.NodeIdNotDefinedException;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BindingManager extends Service<Void> {

    protected static final Logger LOG = LoggerFactory.getLogger(BindingManager.class.getName());

    /**
     * Look for PrefixGroup in provided MasterPrefixGroups, if PrefixGroup
     * isn't present create new one and adds it there
     *
     * @param masterPrefixGroups MasterPrefixGroups where to look for
     * @param bindingItentity    SxpBindingIdentities that we are looking for
     * @return MasterPrefixGroups that have been updated
     */
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

    /**
     * Default constructor that sets SxpNode
     *
     * @param owner SxpNode to be set
     */
    public BindingManager(SxpNode owner) {
        super(owner);
    }

    /**
     * Remove all bindings with Flag CleanUp from specified NodeId
     *
     * @param nodeID NodeId used to filter Binding that will be removed
     */
    public void cleanUpBindings(NodeId nodeID) {
        try {
            synchronized (getBindingSxpDatabase()) {
                getBindingSxpDatabase().cleanUpBindings(nodeID);
                LOG.info(owner + " cleanUpBindings {}", getBindingSxpDatabase());
            }
        } catch (NodeIdNotDefinedException e) {
            LOG.error("{} Error cleaning bindings ", this, e);
        }
    }

    /**
     * Filter List of SxpBindingIdentities and return only unique Bindings
     * according Shortest Path rule and Most recent rule
     *
     * @param bindingIdentities SxpBindingIdentities to be filtered
     * @return List of SxpBindingIdentities with only unique Bindings
     */
    private List<MasterBindingIdentity> databaseArbitration(List<SxpBindingIdentity> bindingIdentities) {
        Map<String, SxpBindingIdentity> biMap = new HashMap<>();

        for (SxpBindingIdentity bindingIdentity : bindingIdentities) {
            String key = new String(bindingIdentity.getBinding().getIpPrefix().getValue());
            bindingIdentity.getBinding().isCleanUp();
            if (!biMap.containsKey(key)) {
                biMap.put(key, bindingIdentity);
                continue;
            }

            SxpBindingIdentity temp = biMap.get(key);

            int pathLength1 = NodeIdConv.getPeerSequence(temp.getPathGroup().getPeerSequence()).size();
            int pathLength2 = NodeIdConv.getPeerSequence(bindingIdentity.getPathGroup().getPeerSequence()).size();

            if (pathLength1 > pathLength2) {
                biMap.put(key, bindingIdentity);
                continue;
            } else if (pathLength1 == pathLength2) {
                Binding binding1 = temp.getBinding();
                Binding binding2 = bindingIdentity.getBinding();

                if (TimeConv.toLong(binding1.getTimestamp()) < TimeConv.toLong(binding2.getTimestamp())) {
                    biMap.put(key, bindingIdentity);
                }
            }
        }
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup> masterPrefixGroupsContributed = new ArrayList<>();

        // Through all bindings in the arbitrated database: Find a related
        // contributed binding to each arbitrated binding.
        for (SxpBindingIdentity bindingIdentity : biMap.values()) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup prefixGroup = getPrefixGroup(
                    masterPrefixGroupsContributed, bindingIdentity);

            BindingBuilder bindingContributedBuilder = new BindingBuilder();
            bindingContributedBuilder.setAction(DatabaseAction.Add);
            bindingContributedBuilder.setChanged(true);
            bindingContributedBuilder.setIpPrefix(bindingIdentity.getBinding().getIpPrefix());
            bindingContributedBuilder.setPeerSequence(bindingIdentity.getPathGroup().getPeerSequence());
            bindingContributedBuilder.setSources(NodeIdConv.createSources(null));

            bindingContributedBuilder.setTimestamp(bindingIdentity.getBinding().getTimestamp());
            prefixGroup.getBinding().add(bindingContributedBuilder.build());

        }

        // Detect sources for each unique binding.
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup prefixGroup : masterPrefixGroupsContributed) {
            if (prefixGroup.getBinding() != null) {
                for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding binding : prefixGroup
                        .getBinding()) {

                    for (SxpBindingIdentity bindingIdentity : biMap.values()) {
                        if (IpPrefixConv.equalTo(bindingIdentity.getBinding().getIpPrefix(), binding.getIpPrefix())) {
                            List<NodeId> peerSequence = NodeIdConv.getPeerSequence(
                                    bindingIdentity.getPathGroup().getPeerSequence());
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
        return masterBindingIdentityContributed;
    }

    /**
     * Delete all Bindings from specified NodeId
     *
     * @param nodeID NodeId used to filter Bindings that will be deleted
     */
    public void purgeBindings(NodeId nodeID) {
        try {
            synchronized (getBindingSxpDatabase()) {
                getBindingSxpDatabase().purgeBindings(nodeID);
            }
        } catch (NodeIdNotDefinedException | DatabaseAccessException e) {
            LOG.error("{} Error purging bindings ", this, e);
        }
    }

    /**
     * Filters SxpBindingIdentities by InBound Filter used per Listener/Both connection
     *
     * @param bindingIdentities SxpBindingIdentities to be filtered
     * @return Filtered List of SxpBindingIdentities
     */
    private List<SxpBindingIdentity> filter(List<SxpBindingIdentity> bindingIdentities) {
        final Map<NodeId, SxpConnection> connectionMap = new HashMap<>();
        for (SxpConnection connection : owner.getAllOnListenerConnections()) {
            if (connection.getFilter(FilterType.Inbound) != null) {
                connectionMap.put(connection.getNodeIdRemote(), connection);
            }
        }
        return new ArrayList<>(Collections2.filter(bindingIdentities, new Predicate<SxpBindingIdentity>() {

            @Override public boolean apply(SxpBindingIdentity identity) {
                //TODO
                SxpConnection
                        connection =
                        connectionMap.get(identity.getPathGroup().getPeerSequence().getPeer().get(0).getNodeId());
                return connection == null || connection.getFilter(FilterType.Inbound) == null || !connection.getFilter(
                        FilterType.Inbound).filter(identity);
            }
        }));
    }

    @Override
    public Void call() {
        LOG.debug(owner + " Starting {}", BindingManager.class.getSimpleName());
        if (owner.isEnabled()) {
            try {
                List<SxpBindingIdentity> bindingIdentities;
                synchronized (getBindingSxpDatabase()) {
                    bindingIdentities = getBindingSxpDatabase().readBindings();
                }
                List<MasterBindingIdentity>
                        masterBindingIdentityContributed =
                        databaseArbitration(filter(bindingIdentities));
                synchronized (getBindingMasterDatabase()) {
                    getBindingMasterDatabase().addBindings(owner.getNodeId(), masterBindingIdentityContributed);
                    owner.setSvcBindingDispatcherDispatch();
                }
            } catch (NodeIdNotDefinedException | DatabaseAccessException e) {
                LOG.warn("{} {} ", owner, BindingManager.class.getSimpleName(), e);
            }
        }
        return null;
    }

    /**
     * Sets CleanUp flag for all bindings from specified NodeId
     *
     * @param nodeID NodeId used to filter Bindings that will set set for CleanUp
     */
    public void setAsCleanUp(NodeId nodeID) {
        try {
            synchronized (getBindingSxpDatabase()) {
                getBindingSxpDatabase().setAsCleanUp(nodeID);
                LOG.info(owner + " setAsCleanUp {}", getBindingSxpDatabase());
            }
        } catch (NodeIdNotDefinedException e) {
            LOG.error("{} Error setting to clean up bindings ", this, e);
        }
    }
}
