/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import static org.opendaylight.sxp.controller.core.SxpDatastoreNode.getIdentifier;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sxp.controller.util.io.ConfigLoader;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.NodesRegister;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddBindingsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddBindingsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddBindingsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionTemplateInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionTemplateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionTemplateOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainFilterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.ConfigPersistence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteBindingsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteBindingsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteBindingsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionTemplateInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionTemplateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionTemplateOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainFilterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteFilterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteNodeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeletePeerGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeletePeerGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeletePeerGroupOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetConnectionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetConnectionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetConnectionsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetNodeBindingsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetNodeBindingsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetNodeBindingsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.fields.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.fields.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.FilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PeerSequenceFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PrefixListFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.MessageBufferingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.ConnectionTemplates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.ConnectionTemplatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.connection.templates.ConnectionTemplate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.connection.templates.ConnectionTemplateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.connection.templates.ConnectionTemplateKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.DomainFilters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.DomainFiltersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SxpRpcServiceImpl implements SxpControllerService, AutoCloseable {

    private final DatastoreAccess serviceDatastoreAccess;
    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(
            ThreadsWorker.generateExecutor(1, "SXP-RPC"));
    private static final Logger LOG = LoggerFactory.getLogger(SxpRpcServiceImpl.class.getName());

    /**
     * @param requestedNodeId NodeId to be converted
     * @return String representation of NodeId
     */
    private static String getNodeId(final NodeId requestedNodeId) {
        return requestedNodeId != null ? NodeIdConv.toString(requestedNodeId) : null;
    }

    public SxpRpcServiceImpl(final DataBroker broker) {
        this.serviceDatastoreAccess = DatastoreAccess.getInstance(broker);
        LOG.info("RpcService started for {}", this.getClass().getSimpleName());
    }

    /**
     * Execute task on SXP node if exists or return immediately.
     *
     * @param nodeId         NodeId specifying Node where task will be executed
     * @param response       Response used for failure case
     * @param resultCallable Task representing request
     * @param <T>            Any type
     * @return Future callback to RpcResult
     */
    private <T> ListenableFuture<RpcResult<T>> getResponse(final String nodeId, final T response,
            final Callable<RpcResult<T>> resultCallable) {
        final SxpNode node = NodesRegister.getRegisteredNode(nodeId);
        if (node != null) {
            return node.getWorker().executeTaskInSequence(resultCallable, ThreadsWorker.WorkerType.DEFAULT);
        } else {
            return Futures.immediateFuture(RpcResultBuilder.success(response).build());
        }
    }

    /**
     * @param nodeId SxpNode identifier
     * @return DatastoreAccess associated with SxpNode or default if nothing found
     */
    private DatastoreAccess getDatastoreAccess(final String nodeId) {
        final SxpNode node = NodesRegister.getRegisteredNode(nodeId);
        if (node instanceof SxpDatastoreNode) {
            return ((SxpDatastoreNode) node).getDatastoreAccess();
        }
        return serviceDatastoreAccess;
    }

    /**
     * Get master database for the specified domain of the node.
     * <p>
     * It is responsibility of the caller to ensure that the node exists in data-store
     * and it is registered in configuration.
     *
     * @param nodeId Node identifier
     * @param domainName Domain name of the domain in the node
     * @return master database of specified domain in the node or {@code null} if domain does not exist
     */
    private MasterDatabaseInf getMasterDatabase(final String nodeId, final String domainName) {
        final org.opendaylight.sxp.core.SxpDomain domain = NodesRegister.getRegisteredNode(nodeId).getDomain(domainName);
        if (domain == null) {
            return null;
        }
        return domain.getMasterDatabase();
    }

    /**
     * Transform {@link List} of {@link Binding} into {@link List} of {@link MasterDatabaseBinding}
     *
     * @param bindings {@link List} of {@link Binding}
     * @param peerSequence Peers sequence path
     * @param created Data and time when binding is created
     * @param origin Origin of binding
     * @return {@link List} of {@link MasterDatabaseBinding}
     */
    private List<MasterDatabaseBinding> transformBindings(final List<Binding> bindings,
            final PeerSequence peerSequence, final DateAndTime created, final OriginType origin) {
        final List<MasterDatabaseBinding> masterDatabaseBindings = new ArrayList<>();
        for (final Binding binding : bindings) {
            final List<IpPrefix> ipPrefixes = binding.getIpPrefix();
            if (ipPrefixes != null) {
                for (final IpPrefix ipPrefix : ipPrefixes) {
                    masterDatabaseBindings.add(
                            new MasterDatabaseBindingBuilder()
                                    .setIpPrefix(ipPrefix)
                                    .setSecurityGroupTag(binding.getSgt())
                                    .setPeerSequence(peerSequence)
                                    .setTimestamp(created)
                                    .setOrigin(origin)
                                    .build()
                    );
                }
            }
        }

        return masterDatabaseBindings;
    }

    private List<MasterDatabaseBinding> transformBindings(final List<Binding> bindings) {
        return transformBindings(bindings, null, null, null);
    }

    private boolean containsBindings(final MasterDatabase masterDatabase) {
        return masterDatabase != null
                && masterDatabase.getBinding() != null && !masterDatabase.getBinding().isEmpty();
    }

    private boolean mergeDatabaseBindingsToDs(final String nodeId, final String domain,
            final MasterDatabase masterDatabase, final OriginType origin) {
        // nothing to process, return success
        if (!containsBindings(masterDatabase)) {
            return true;
        }

        // merge bindings to data-store
        final MasterDatabaseInf database = getMasterDatabase(nodeId, domain);
        if (database != null) {
            final List<MasterDatabaseBinding> bindings = transformBindings(
                    masterDatabase.getBinding(),
                    new PeerSequenceBuilder().setPeer(new ArrayList<>()).build(),
                    TimeConv.toDt(System.currentTimeMillis()), origin);
            return !database.addBindings(bindings).isEmpty();
        }
        return false;
    }

    /**
     * @param persistence Config Persistence to be checked
     * @return DataStore type corresponding to config persistence
     */
    private LogicalDatastoreType getDatastoreType(final ConfigPersistence persistence) {
        if (ConfigPersistence.Operational.equals(persistence))
            return LogicalDatastoreType.OPERATIONAL;
        else
            return LogicalDatastoreType.CONFIGURATION;
    }

    /**
     * @param entries Entries to be checked
     * @return FilterSpecific corresponding to entries
     */
    private FilterSpecific getFilterSpecific(final FilterEntries entries) {
        if (entries instanceof AclFilterEntries || entries instanceof PrefixListFilterEntries) {
            return FilterSpecific.AccessOrPrefixList;
        } else if (entries instanceof PeerSequenceFilterEntries) {
            return FilterSpecific.PeerSequence;
        }
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<AddConnectionOutput>> addConnection(final AddConnectionInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final AddConnectionOutputBuilder output = new AddConnectionOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddConnection event | {}", input.toString());
            Preconditions.checkNotNull(input.getConnections());
            Preconditions.checkNotNull(input.getConnections().getConnection()).forEach(c -> {
                output.setResult(datastoreAccess.checkAndPut(getIdentifier(nodeId).child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                .child(Connections.class)
                                .child(Connection.class, new ConnectionKey(c.getPeerAddress(), c.getTcpPort())),
                        new ConnectionBuilder(c).setNodeId(null)
                                .setState(null)
                                .setTimestampUpdateOrKeepAliveMessage(null)
                                .build(), getDatastoreType(input.getConfigPersistence()), false));
            });
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<DeleteDomainFilterOutput>> deleteDomainFilter(final DeleteDomainFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final DeleteDomainFilterOutputBuilder output = new DeleteDomainFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeleteDomainFilter event | {}", input.toString());
            if (input.getDomainName() != null && input.getFilterName() != null) {
                if (input.getFilterSpecific() != null) {
                    final InstanceIdentifier
                            identifier =
                            getIdentifier(nodeId).child(SxpDomains.class)
                                    .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                    .child(DomainFilters.class)
                                    .child(DomainFilter.class,
                                            new DomainFilterKey(input.getFilterName(), input.getFilterSpecific()));
                    output.setResult(datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION)
                            || datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL));
                } else {
                    final DomainFilters
                            domainFilters =
                            datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpDomains.class)
                                    .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                    .child(DomainFilters.class), LogicalDatastoreType.OPERATIONAL);
                    if (domainFilters != null && domainFilters.getDomainFilter() != null) {
                        domainFilters.getDomainFilter()
                                .stream()
                                .filter(f -> input.getFilterName().equals(f.getFilterName()))
                                .forEach(f -> {
                                    final InstanceIdentifier
                                            identifier =
                                            getIdentifier(nodeId).child(SxpDomains.class)
                                                    .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                                    .child(DomainFilters.class)
                                                    .child(DomainFilter.class, new DomainFilterKey(f.getFilterName(),
                                                            f.getFilterSpecific()));
                                    output.setResult(output.isResult() || datastoreAccess.checkAndDelete(identifier,
                                            LogicalDatastoreType.CONFIGURATION) || datastoreAccess.checkAndDelete(
                                            identifier, LogicalDatastoreType.OPERATIONAL));
                                });
                    }
                }
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<AddDomainFilterOutput>> addDomainFilter(final AddDomainFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final AddDomainFilterOutputBuilder output = new AddDomainFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddDomainFilter event | {}", input.toString());
            if (input.getDomainName() != null && input.getSxpDomainFilter() != null
                    && input.getSxpDomainFilter().getFilterName() != null) {
                if (NodesRegister.getRegisteredNode(nodeId).getDomain(input.getDomainName()) == null) {
                    LOG.warn("RpcAddDomainFilter event | domain {} does not exist", input.getDomainName());
                    return RpcResultBuilder.success(output.build()).build();
                }
                final DomainFilterBuilder filter = new DomainFilterBuilder(input.getSxpDomainFilter());
                filter.setFilterSpecific(getFilterSpecific(filter.getFilterEntries()));
                output.setResult(datastoreAccess.checkAndPut(getIdentifier(nodeId).child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                .child(DomainFilters.class)
                                .child(DomainFilter.class,
                                        new DomainFilterKey(filter.getFilterName(), filter.getFilterSpecific())),
                        filter.build(), getDatastoreType(input.getConfigPersistence()), false));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<AddFilterOutput>> addFilter(final AddFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final AddFilterOutputBuilder output = new AddFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddFilter event | {}", input.toString());
            if (NodesRegister.getRegisteredNode(nodeId).getPeerGroup(input.getPeerGroupName()) == null) {
                LOG.warn("RpcAddFilter event | peer group {} does not exist", input.getPeerGroupName());
                return RpcResultBuilder.success(output.build()).build();
            }
            final SxpFilterBuilder filter = new SxpFilterBuilder(input.getSxpFilter());
            filter.setFilterSpecific(getFilterSpecific(filter.getFilterEntries()));
            if (filter.getFilterSpecific() != null) {
                output.setResult(datastoreAccess.checkAndPut(getIdentifier(nodeId).child(SxpPeerGroups.class)
                                .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName()))
                                .child(SxpFilter.class, new SxpFilterKey(filter.getFilterSpecific(), filter.getFilterType())),
                        filter.build(), getDatastoreType(input.getConfigPersistence()), false));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<AddPeerGroupOutput>> addPeerGroup(final AddPeerGroupInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final AddPeerGroupOutputBuilder output = new AddPeerGroupOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddPerGroup event | {}", input.toString());
            if (input.getSxpPeerGroup() != null) {
                output.setResult(datastoreAccess.checkAndPut(getIdentifier(nodeId).child(SxpPeerGroups.class)
                                .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getSxpPeerGroup().getName())),
                        new SxpPeerGroupBuilder(input.getSxpPeerGroup()).build(),
                        getDatastoreType(input.getConfigPersistence()), false));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<DeleteConnectionTemplateOutput>> deleteConnectionTemplate(
            final DeleteConnectionTemplateInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final DeleteConnectionTemplateOutputBuilder
                output =
                new DeleteConnectionTemplateOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeleteConnectionTemplate event | {}", input.toString());
            if (input.getDomainName() != null && !input.getDomainName().isEmpty()
                    && input.getTemplatePrefix() != null) {
                final InstanceIdentifier
                        identifier =
                        getIdentifier(nodeId).child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                .child(ConnectionTemplates.class)
                                .child(ConnectionTemplate.class, new ConnectionTemplateKey(input.getTemplatePrefix()));
                output.setResult(serviceDatastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION)
                        || serviceDatastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<DeleteNodeOutput>> deleteNode(final DeleteNodeInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final DeleteNodeOutputBuilder output = new DeleteNodeOutputBuilder().setResult(false);
        return executor.submit(() -> {
            if (nodeId != null) {
                final InstanceIdentifier
                        identifier =
                        InstanceIdentifier.builder(NetworkTopology.class)
                                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                                .child(Node.class, new NodeKey(
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                                nodeId)))
                                .build();
                LOG.info("RpcDeleteNode event | {}", input.toString());
                final boolean config = datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION);
                if (config) {
                    output.setResult(true);
                    return RpcResultBuilder.success(output.build()).build();
                }
                final boolean operational = datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL);
                output.setResult(operational);
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<DeleteBindingsOutput>> deleteBindings(final DeleteBindingsInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final DeleteBindingsOutputBuilder output = new DeleteBindingsOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug("RpcDeleteBindings event | {}", input.toString());
            } else {
                LOG.info("RpcDeleteBindings event | {}", input.getNodeId());
            }
            if (input.getDomainName() == null) {
                LOG.warn("RpcDeleteEntry exception | Parameter 'domain-name' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }

            final MasterDatabaseInf masterDatabase = getMasterDatabase(nodeId, input.getDomainName());
            if (input.getBinding() != null && !input.getBinding().isEmpty()
                    && masterDatabase != null && masterDatabase.getBindings() != null) {
                final List<MasterDatabaseBinding> bindingsToBeRemoved = transformBindings(input.getBinding());
                if (!bindingsToBeRemoved.isEmpty()) {
                    final List<MasterDatabaseBinding> deleted = masterDatabase.deleteBindings(bindingsToBeRemoved);
                    output.setResult(!deleted.isEmpty());
                } else {
                    output.setResult(true);
                }
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public void close() {
        executor.shutdown();
        serviceDatastoreAccess.close();
    }

    @Override
    public ListenableFuture<RpcResult<DeleteConnectionOutput>> deleteConnection(final DeleteConnectionInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final DeleteConnectionOutputBuilder output = new DeleteConnectionOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeleteConnection event | {}", input.toString());
            final InstanceIdentifier
                    identifier =
                    getIdentifier(nodeId).child(SxpDomains.class)
                            .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                            .child(Connections.class)
                            .child(Connection.class,
                                    new ConnectionKey(new IpAddress(input.getPeerAddress()), input.getTcpPort()));
            output.setResult(datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION)
                    || datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL));
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<DeleteFilterOutput>> deleteFilter(final DeleteFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final DeleteFilterOutputBuilder output = new DeleteFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeleteFilter event | {}", input.toString());
            if (input.getFilterSpecific() == null) {
                final LogicalDatastoreType datastoreType;
                SxpPeerGroup
                        group =
                        datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpPeerGroups.class)
                                        .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName())),
                                LogicalDatastoreType.CONFIGURATION);
                if (group == null || group.getSxpFilter() == null || group.getSxpFilter().isEmpty()) {
                    datastoreType = LogicalDatastoreType.OPERATIONAL;
                    group =
                            datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpPeerGroups.class)
                                            .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName())),
                                    datastoreType);
                } else {
                    datastoreType = LogicalDatastoreType.CONFIGURATION;
                }
                final List<SxpFilter>
                        filters =
                        group == null || group.getSxpFilter() == null ? new ArrayList<>() : group.getSxpFilter();

                filters.forEach(f -> {
                    if (f.getFilterType().equals(input.getFilterType()))
                        output.setResult(datastoreAccess.checkAndDelete(getIdentifier(nodeId).child(SxpPeerGroups.class)
                                        .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName()))
                                        .child(SxpFilter.class, new SxpFilterKey(f.getFilterSpecific(), f.getFilterType())),
                                datastoreType));
                });
            } else {
                final InstanceIdentifier
                        identifier =
                        getIdentifier(nodeId).child(SxpPeerGroups.class)
                                .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName()))
                                .child(SxpFilter.class,
                                        new SxpFilterKey(input.getFilterSpecific(), input.getFilterType()));
                output.setResult(datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION)
                        || datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<DeletePeerGroupOutput>> deletePeerGroup(final DeletePeerGroupInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final DeletePeerGroupOutputBuilder output = new DeletePeerGroupOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeletePeerGroup event | {}", input.toString());
            final InstanceIdentifier
                    identifier =
                    getIdentifier(nodeId).child(SxpPeerGroups.class)
                            .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName()));
            output.setResult(datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION)
                    || datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL));

            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<DeleteDomainOutput>> deleteDomain(final DeleteDomainInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final DeleteDomainOutputBuilder output = new DeleteDomainOutputBuilder().setResult(false);
        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeleteDomain event | {}", input.toString());
            if (input.getDomainName() != null) {
                final InstanceIdentifier
                        identifier =
                        getIdentifier(nodeId).child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()));
                output.setResult(datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION)
                        || datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<AddConnectionTemplateOutput>> addConnectionTemplate(
            final AddConnectionTemplateInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final AddConnectionTemplateOutputBuilder output = new AddConnectionTemplateOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddConnectionTemplate event | {}", input.toString());
            if (input.getDomainName() != null && !input.getDomainName().isEmpty()
                    && input.getTemplatePrefix() != null) {
                final ConnectionTemplateBuilder builder = new ConnectionTemplateBuilder(input);

                output.setResult(serviceDatastoreAccess.checkAndPut(getIdentifier(nodeId).child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                .child(ConnectionTemplates.class)
                                .child(ConnectionTemplate.class, new ConnectionTemplateKey(builder.getTemplatePrefix())),
                        builder.build(), getDatastoreType(input.getConfigPersistence()), false));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectionsOutput>> getConnections(final GetConnectionsInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder();
        final GetConnectionsOutputBuilder
                output =
                new GetConnectionsOutputBuilder().setConnections(connectionsBuilder.build());

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcGetConnectionsStatus event | {}", input.toString());
            final Connections
                    connections =
                    datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpDomains.class)
                            .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                            .child(Connections.class), LogicalDatastoreType.OPERATIONAL);
            if (connections != null && connections.getConnection() != null) {
                output.setConnections(connections);
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<GetNodeBindingsOutput>> getNodeBindings(final GetNodeBindingsInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final GetNodeBindingsOutputBuilder output = new GetNodeBindingsOutputBuilder().setBinding(new ArrayList<>());

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcGetNodeBindings event | {}", input.toString());
            final MasterDatabaseInf masterDatabase = getMasterDatabase(nodeId, input.getDomainName());

            if (masterDatabase != null) {
                final Collection<MasterDatabaseBinding> bindings;
                if (GetNodeBindingsInput.BindingsRange.Local.equals(input.getBindingsRange())) {
                    bindings = masterDatabase.getBindings(BindingOriginsConfig.LOCAL_ORIGIN);
                } else {
                    bindings = masterDatabase.getBindings();
                }

                final Map<Sgt, List<IpPrefix>> sgtListMap = bindings.stream()
                        .collect(Collectors.groupingBy(MasterDatabaseBinding::getSecurityGroupTag,
                                Collectors.mapping(MasterDatabaseBinding::getIpPrefix, Collectors.toList())));

                final List<Binding> result = sgtListMap.entrySet().stream()
                        .map(sgtIpPrefixEntry -> new BindingBuilder()
                                .setSgt(sgtIpPrefixEntry.getKey())
                                .setIpPrefix(sgtIpPrefixEntry.getValue())
                                .build())
                        .collect(Collectors.toList());

                output.setBinding(result);
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<GetPeerGroupOutput>> getPeerGroup(final GetPeerGroupInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final GetPeerGroupOutputBuilder output = new GetPeerGroupOutputBuilder().setSxpPeerGroup(null);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcGetPeerGroup {}", input.toString());
            if (input.getPeerGroupName() != null) {
                final SxpPeerGroup
                        peerGroup =
                        datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpPeerGroups.class)
                                        .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName())),
                                LogicalDatastoreType.OPERATIONAL);
                if (peerGroup != null) {
                    output.setSxpPeerGroup(
                            new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroupBuilder(
                                    peerGroup).build());
                }
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<GetPeerGroupsOutput>> getPeerGroups(final GetPeerGroupsInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final GetPeerGroupsOutputBuilder output = new GetPeerGroupsOutputBuilder().setSxpPeerGroup(new ArrayList<>());

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcGetPeerGroups event | {}", input.toString());
            final SxpPeerGroups
                    groups =
                    datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpPeerGroups.class),
                            LogicalDatastoreType.OPERATIONAL);
            if (groups != null) {
                output.setSxpPeerGroup(groups.getSxpPeerGroup());
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<AddNodeOutput>> addNode(final AddNodeInput input) {
        final AddNodeOutputBuilder output = new AddNodeOutputBuilder().setResult(false);

        return executor.submit(() -> {
            if (input.getNodeId() != null) {
                LOG.info("RpcAddNode event | {}", input.toString());

                final String nodeId = getNodeId(input.getNodeId());
                final LogicalDatastoreType datastoreType = getDatastoreType(input.getConfigPersistence());
                ConfigLoader.initTopologyNode(nodeId, getDatastoreType(input.getConfigPersistence()), serviceDatastoreAccess);

                // put node to data-store
                final boolean putSxpNodeToDs = putSxpNodeToDs(nodeId, buildSxpNode(input), datastoreType);
                if (!putSxpNodeToDs) {
                    LOG.error("Failed to put SXP node " + nodeId + " to data store");
                    return RpcResultBuilder.success(output.build()).build();
                }

                // set success
                output.setResult(true);
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    private SxpNodeIdentity buildSxpNode(final AddNodeInput input) {
        final SxpNodeIdentityBuilder nodeBuilder = new SxpNodeIdentityBuilder(input);
        if (nodeBuilder.getTcpPort() == null)
            nodeBuilder.setTcpPort(new PortNumber(64999));
        if (nodeBuilder.getVersion() == null)
            nodeBuilder.setVersion(Version.Version4);
        if (nodeBuilder.getMappingExpanded() == null)
            nodeBuilder.setMappingExpanded(0);
        if (nodeBuilder.getSxpDomains() == null || nodeBuilder.getSxpDomains().getSxpDomain() == null)
            nodeBuilder.setSxpDomains(new SxpDomainsBuilder().setSxpDomain(new ArrayList<>()).build());
        if (nodeBuilder.getSxpDomains().getSxpDomain().isEmpty() || nodeBuilder.getSxpDomains()
                .getSxpDomain()
                .stream()
                .noneMatch(d -> SxpNode.DEFAULT_DOMAIN.equals(d.getDomainName()))) {
            nodeBuilder.getSxpDomains()
                    .getSxpDomain()
                    .add(new SxpDomainBuilder().setConnections(
                            new ConnectionsBuilder().setConnection(new ArrayList<>()).build())
                            .setDomainName(org.opendaylight.sxp.core.SxpNode.DEFAULT_DOMAIN)
                            .setDomainFilters(
                                    new DomainFiltersBuilder().setDomainFilter(new ArrayList<>()).build())
                            .setConnectionTemplates(
                                    new ConnectionTemplatesBuilder().setConnectionTemplate(new ArrayList<>())
                                            .build())
                            .build());
        }
        if (nodeBuilder.getSxpPeerGroups() == null)
            nodeBuilder.setSxpPeerGroups(new SxpPeerGroupsBuilder().build());
        if (nodeBuilder.getSecurity() == null)
            nodeBuilder.setSecurity(new SecurityBuilder().build());
        if (nodeBuilder.getTimers() == null)
            nodeBuilder.setTimers(new TimersBuilder().build());
        if (nodeBuilder.getMessageBuffering() == null)
            nodeBuilder.setMessageBuffering(new MessageBufferingBuilder().setInBuffer(input.getInBuffer())
                    .setOutBuffer(input.getOutBuffer())
                    .build());

        return nodeBuilder.build();
    }

    private boolean putSxpNodeToDs(final String nodeId, final SxpNodeIdentity node,
            final LogicalDatastoreType datastoreType) {
        final InstanceIdentifier<SxpNodeIdentity> path = getIdentifier(nodeId);
        // do not allow replacing of existing node
        if (serviceDatastoreAccess.readSynchronous(path, datastoreType) != null) {
            LOG.warn("SXP node " + nodeId + " has already exist");
            return false;
        }
        return serviceDatastoreAccess.putSynchronous(path, node, datastoreType);
    }

    @Override
    public ListenableFuture<RpcResult<AddDomainOutput>> addDomain(final AddDomainInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final AddDomainOutputBuilder output = new AddDomainOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDomain event | {}", input.toString());

            final String domainName = input.getDomainName();
            if (domainName != null) {
                final SxpDomainBuilder domainBuilder = new SxpDomainBuilder();
                domainBuilder.setDomainName(domainName);
                domainBuilder.setConnections(ConfigLoader.parseConnections(input.getConnections()));
                domainBuilder.setDomainFilters(new DomainFiltersBuilder().setDomainFilter(new ArrayList<>()).build());
                domainBuilder.setConnectionTemplates(
                        new ConnectionTemplatesBuilder().setConnectionTemplate(new ArrayList<>()).build());

                // merge domain itself
                try {
                    mergeSxpDomainToDs(nodeId, domainName, domainBuilder.build(), LogicalDatastoreType.CONFIGURATION)
                            .get(1000, TimeUnit.MILLISECONDS);
                } catch (final Exception e) {
                    LOG.error("Failed to merge SXP domain " + domainName + " to data store | {}", e);
                    return RpcResultBuilder.success(output.build()).build();
                }

                // merge domain's bindings
                if (containsBindings(input.getMasterDatabase())) {
                    final OriginType origin = input.getOrigin();
                    if (origin == null) {
                        LOG.warn("RpcDomain event exception | Parameter 'origin' not defined");
                        return RpcResultBuilder.success(output.build()).build();
                    }

                    // wait until domain is present in configuration
                    final SxpNode registeredNode = NodesRegister.getRegisteredNode(nodeId);
                    for (int i = 0;
                            (registeredNode == null || registeredNode.getDomain(domainName) == null) && i < 10;
                            i++) {
                        try {
                            Thread.sleep(100);
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            LOG.error("Failed to wait for SXP domain " + domainName + " to appear in configuration | {}", e);
                            return RpcResultBuilder.success(output.build()).build();
                        }
                    }
                    if (registeredNode == null || registeredNode.getDomain(domainName) == null) {
                        return RpcResultBuilder.success(output.build()).build();
                    }

                    // merge bindings
                    final boolean mergeBindingsToDs = mergeDatabaseBindingsToDs(nodeId, domainName,
                            input.getMasterDatabase(), origin);
                    output.setResult(mergeBindingsToDs);
                } else {
                    output.setResult(true);
                }
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    private FluentFuture<? extends CommitInfo> mergeSxpDomainToDs(String nodeId, String domain, SxpDomain sxpNode,
                                                                  LogicalDatastoreType datastoreType) {
       return getDatastoreAccess(nodeId).merge(getIdentifier(nodeId).child(SxpDomains.class).child(SxpDomain.class,
               new SxpDomainKey(domain)), sxpNode, datastoreType);
    }

    @Override
    public ListenableFuture<RpcResult<AddBindingsOutput>> addBindings(final AddBindingsInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final AddBindingsOutputBuilder output = new AddBindingsOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug("RpcAddBindings event | {}", input.toString());
            } else {
                LOG.info("RpcAddBindings event | {}", input.getNodeId());
            }
            if (input.getDomainName() == null) {
                LOG.warn("RpcAddEntry exception | Parameter 'domain-name' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }

            final MasterDatabaseInf masterDatabase = getMasterDatabase(nodeId, input.getDomainName());
            if (containsBindings(input.getMasterDatabase()) && masterDatabase != null) {
                final OriginType origin = input.getOrigin();
                if (origin == null) {
                    LOG.warn("RpcAddEntry exception | Parameter 'origin' not defined");
                    return RpcResultBuilder.success(output.build()).build();
                }
                final List<MasterDatabaseBinding> bindingsToBeAdded = transformBindings(
                        input.getMasterDatabase().getBinding(), new PeerSequenceBuilder().setPeer(Collections.emptyList()).build(),
                        TimeConv.toDt(System.currentTimeMillis()), origin);
                final List<MasterDatabaseBinding> addedBindings = masterDatabase.addBindings(bindingsToBeAdded);
                output.setResult(!addedBindings.isEmpty());
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override
    public ListenableFuture<RpcResult<UpdateFilterOutput>> updateFilter(final UpdateFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final UpdateFilterOutputBuilder output = new UpdateFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcUpdateFilter event | {}", input.toString());
            if (input.getSxpFilter() != null && input.getSxpFilter().getFilterType() != null) {
                final SxpFilterBuilder filter = new SxpFilterBuilder(input.getSxpFilter());
                filter.setFilterSpecific(getFilterSpecific(filter.getFilterEntries()));
                output.setResult(datastoreAccess.checkAndPut(getIdentifier(nodeId).child(SxpPeerGroups.class)
                                .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName()))
                                .child(SxpFilter.class, new SxpFilterKey(filter.getFilterSpecific(), filter.getFilterType())),
                        filter.build(), getDatastoreType(input.getConfigPersistence()), true));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

}
