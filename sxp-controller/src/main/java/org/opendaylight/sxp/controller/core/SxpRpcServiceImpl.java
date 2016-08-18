/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.controller.util.io.ConfigLoader;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddEntryInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddEntryOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddEntryOutputBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteEntryInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteEntryOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteEntryOutputBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateEntryInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateEntryOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateEntryOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
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

import static org.opendaylight.sxp.controller.core.SxpDatastoreNode.getIdentifier;

public class SxpRpcServiceImpl implements SxpControllerService, AutoCloseable {

    private final DatastoreAccess datastoreAccess;

    private static final Logger LOG = LoggerFactory.getLogger(SxpRpcServiceImpl.class.getName());

    /**
     * @param requestedNodeId NodeId to be converted
     * @return String representation of NodeId
     */
    private static String getNodeId(NodeId requestedNodeId) {
        return requestedNodeId != null ? NodeIdConv.toString(requestedNodeId) : null;
    }

    public SxpRpcServiceImpl(DataBroker broker) {
        this.datastoreAccess = DatastoreAccess.getInstance(broker);
        LOG.info("RpcService started for {}", this.getClass().getSimpleName());
    }

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    /**
     * @param nodeId         NodeId specifying Node where task will be executed
     * @param response       Response used for failure case
     * @param resultCallable Task representing request
     * @param <T>            Any type
     * @return Future callback to RpcResult
     */
    private <T> Future<RpcResult<T>> getResponse(String nodeId, final T response,
            Callable<RpcResult<T>> resultCallable) {
        SxpNode node = Configuration.getRegisteredNode(nodeId);
        if (nodeId == null || (
                datastoreAccess.readSynchronous(getIdentifier(nodeId), LogicalDatastoreType.CONFIGURATION) == null
                        && datastoreAccess.readSynchronous(getIdentifier(nodeId), LogicalDatastoreType.OPERATIONAL)
                        == null)) {
            return new AbstractFuture<RpcResult<T>>() {

                @Override public RpcResult<T> get() throws InterruptedException, ExecutionException {
                    return RpcResultBuilder.success(response).build();
                }
            };
        } else if (node != null) {
            return node.getWorker().executeTaskInSequence(resultCallable, ThreadsWorker.WorkerType.DEFAULT);
        } else {
            return executor.submit(resultCallable);
        }
    }

    /**
     * @param nodeId SxpNode identifier
     * @return DatastoreAcces associated with SxpNode or default if nothing found
     */
    private DatastoreAccess getDatastoreAccess(String nodeId) {
        SxpNode node = Configuration.getRegisteredNode(nodeId);
        if (node instanceof SxpDatastoreNode) {
            return ((SxpDatastoreNode) node).getDatastoreAccess();
        }
        return datastoreAccess;
    }

    /**
     * @param persistence Config Persistence to be checked
     * @return DataStore type corresponding to config persistence
     */
    private LogicalDatastoreType getDatastoreType(ConfigPersistence persistence) {
        if (ConfigPersistence.Operational.equals(persistence))
            return LogicalDatastoreType.OPERATIONAL;
        else
            return LogicalDatastoreType.CONFIGURATION;
    }

    /**
     * @param entries Entries to be checked
     * @return FilterSpecific corresponding to entries
     */
    private FilterSpecific getFilterSpecific(FilterEntries entries) {
        if (entries instanceof AclFilterEntries || entries instanceof PrefixListFilterEntries) {
            return FilterSpecific.AccessOrPrefixList;
        } else if (entries instanceof PeerSequenceFilterEntries) {
            return FilterSpecific.PeerSequence;
        }
        return null;
    }

    @Override public Future<RpcResult<AddConnectionOutput>> addConnection(final AddConnectionInput input) {
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

    @Override public Future<RpcResult<DeleteDomainFilterOutput>> deleteDomainFilter(DeleteDomainFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final DeleteDomainFilterOutputBuilder output = new DeleteDomainFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeleteDomainFilter event | {}", input.toString());
            if (input.getDomainName() != null) {
                if (input.getFilterSpecific() != null) {
                    InstanceIdentifier
                            identifier =
                            getIdentifier(nodeId).child(SxpDomains.class)
                                    .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                    .child(DomainFilters.class)
                                    .child(DomainFilter.class,
                                            new DomainFilterKey(input.getFilterName(), input.getFilterSpecific()));
                    output.setResult(datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION)
                            || datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL));
                } else {
                    DomainFilters
                            domainFilters =
                            datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpDomains.class)
                                    .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                    .child(DomainFilters.class), LogicalDatastoreType.OPERATIONAL);
                    if (domainFilters != null && domainFilters.getDomainFilter() != null) {
                        domainFilters.getDomainFilter()
                                .stream()
                                .filter(f -> input.getFilterName().equals(f.getFilterName()))
                                .forEach(f -> {
                                    InstanceIdentifier
                                            identifier =
                                            getIdentifier(nodeId).child(SxpDomains.class)
                                                    .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                                    .child(DomainFilters.class)
                                                    .child(DomainFilter.class, new DomainFilterKey(f.getFilterName(),
                                                            f.getFilterSpecific()));
                                    output.setResult(datastoreAccess.checkAndDelete(identifier,
                                            LogicalDatastoreType.CONFIGURATION) || datastoreAccess.checkAndDelete(
                                            identifier, LogicalDatastoreType.OPERATIONAL));
                                });
                    }
                }
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<AddDomainFilterOutput>> addDomainFilter(AddDomainFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final AddDomainFilterOutputBuilder output = new AddDomainFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddDomainFilter event | {}", input.toString());
            if (input.getDomainName() != null && input.getSxpDomainFilter() != null
                    && input.getSxpDomainFilter().getFilterName() != null) {
                DomainFilterBuilder filter = new DomainFilterBuilder(input.getSxpDomainFilter());
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

    @Override public Future<RpcResult<AddEntryOutput>> addEntry(final AddEntryInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final AddEntryOutputBuilder output = new AddEntryOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddEntry event | {}", input.toString());
            if (input.getIpPrefix() == null) {
                LOG.warn("RpcAddEntry exception | Parameter 'ip-prefix' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            if (input.getSgt() == null || input.getSgt().getValue() == null) {
                LOG.warn("RpcAddEntry exception | Parameter 'sgt' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            if (input.getDomainName() == null) {
                LOG.warn("RpcAddEntry exception | Parameter 'domain-name' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            MasterDatabaseBinding
                    binding =
                    new MasterDatabaseBindingBuilder().setIpPrefix(input.getIpPrefix())
                            .setTimestamp(TimeConv.toDt(System.currentTimeMillis()))
                            .setSecurityGroupTag(input.getSgt())
                            .setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build())
                            .build();
            output.setResult(datastoreAccess.checkAndPut(getIdentifier(nodeId).child(SxpDomains.class)
                            .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                            .child(MasterDatabase.class)
                            .child(MasterDatabaseBinding.class, new MasterDatabaseBindingKey(binding.getIpPrefix())), binding,
                    getDatastoreType(input.getConfigPersistence()), false));

            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<AddFilterOutput>> addFilter(final AddFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final AddFilterOutputBuilder output = new AddFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddFilter event | {}", input.toString());
            SxpFilterBuilder filter = new SxpFilterBuilder(input.getSxpFilter());
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

    @Override public Future<RpcResult<AddPeerGroupOutput>> addPeerGroup(final AddPeerGroupInput input) {
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

    @Override public Future<RpcResult<DeleteConnectionTemplateOutput>> deleteConnectionTemplate(
            DeleteConnectionTemplateInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final DeleteConnectionTemplateOutputBuilder
                output =
                new DeleteConnectionTemplateOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeleteConnectionTemplate event | {}", input.toString());
            if (input.getDomainName() != null && !input.getDomainName().isEmpty()
                    && input.getTemplatePrefix() != null) {
                InstanceIdentifier
                        identifier =
                        getIdentifier(nodeId).child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                .child(ConnectionTemplates.class)
                                .child(ConnectionTemplate.class, new ConnectionTemplateKey(input.getTemplatePrefix()));
                output.setResult(datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION)
                        || datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<DeleteNodeOutput>> deleteNode(DeleteNodeInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final DeleteNodeOutputBuilder output = new DeleteNodeOutputBuilder().setResult(false);
        return executor.submit(() -> {
            if (nodeId != null) {
                InstanceIdentifier
                        identifier =
                        InstanceIdentifier.builder(NetworkTopology.class)
                                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                                .child(Node.class, new NodeKey(
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                                nodeId)))
                                .build();
                LOG.info("RpcDeleteNode event | {}", input.toString());
                output.setResult(datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION));
                output.setResult(datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL)
                        || output.isResult());
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<DeleteBindingsOutput>> deleteBindings(DeleteBindingsInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final DeleteBindingsOutputBuilder output = new DeleteBindingsOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeleteBindings event | {}", input.toString());
            if (input.getDomainName() == null) {
                LOG.warn("RpcDeleteEntry exception | Parameter 'domain-name' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            final MasterDatabase
                    database =
                    datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpDomains.class)
                            .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                            .child(MasterDatabase.class), LogicalDatastoreType.CONFIGURATION);

            if (input.getBinding() != null && !input.getBinding().isEmpty() && database != null
                    && database.getMasterDatabaseBinding() != null) {
                final Map<Sgt, Set<IpPrefix>>
                        bindings =
                        input.getBinding()
                                .stream()
                                .filter(b -> b != null && b.getSgt() != null && b.getIpPrefix() != null
                                        && !b.getIpPrefix().isEmpty())
                                .collect(Collectors.toMap((Function<Binding, Sgt>) Binding::getSgt,
                                        (Function<Binding, Set<IpPrefix>>) binding -> binding.getIpPrefix()
                                                .stream()
                                                .collect(Collectors.toSet())));
                if (database.getMasterDatabaseBinding()
                        .removeIf(b -> bindings.containsKey(b.getSecurityGroupTag()) && bindings.get(
                                b.getSecurityGroupTag()).contains(b.getIpPrefix()))) {
                    output.setResult(datastoreAccess.putSynchronous(getIdentifier(nodeId).child(SxpDomains.class)
                            .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                            .child(MasterDatabase.class), database, LogicalDatastoreType.CONFIGURATION));
                } else {
                    output.setResult(true);
                }
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public void close() {
        executor.shutdown();
        datastoreAccess.close();
    }

    @Override public Future<RpcResult<DeleteConnectionOutput>> deleteConnection(final DeleteConnectionInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final DeleteConnectionOutputBuilder output = new DeleteConnectionOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeleteConnection event | {}", input.toString());
            InstanceIdentifier
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

    @Override public Future<RpcResult<DeleteEntryOutput>> deleteEntry(final DeleteEntryInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DeleteEntryOutputBuilder output = new DeleteEntryOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeleteEntry event | {}", input.toString());
            if (input.getSgt() == null || input.getSgt().getValue() == null) {
                LOG.warn("RpcDeleteEntry exception | Parameter 'sgt' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            if (input.getDomainName() == null) {
                LOG.warn("RpcDeleteEntry exception | Parameter 'domain-name' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            final MasterDatabase
                    database =
                    datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpDomains.class)
                            .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                            .child(MasterDatabase.class), LogicalDatastoreType.CONFIGURATION);
            if (input.getIpPrefix() != null && database != null && database.getMasterDatabaseBinding() != null) {
                final Set<IpPrefix> prefixes = input.getIpPrefix().stream().collect(Collectors.toSet());
                output.setResult(database.getMasterDatabaseBinding()
                        .removeIf(b -> input.getSgt().equals(b.getSecurityGroupTag()) && prefixes.contains(
                                b.getIpPrefix())) && datastoreAccess.putSynchronous(
                        getIdentifier(nodeId).child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                .child(MasterDatabase.class), database, LogicalDatastoreType.CONFIGURATION));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<DeleteFilterOutput>> deleteFilter(final DeleteFilterInput input) {
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
                List<SxpFilter>
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
                InstanceIdentifier
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

    @Override public Future<RpcResult<DeletePeerGroupOutput>> deletePeerGroup(final DeletePeerGroupInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final DeletePeerGroupOutputBuilder output = new DeletePeerGroupOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeletePeerGroup event | {}", input.toString());
            InstanceIdentifier
                    identifier =
                    getIdentifier(nodeId).child(SxpPeerGroups.class)
                            .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName()));
            output.setResult(datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION)
                    || datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.OPERATIONAL));

            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<DeleteDomainOutput>> deleteDomain(DeleteDomainInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final DeleteDomainOutputBuilder output = new DeleteDomainOutputBuilder().setResult(false);
        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeleteDomain event | {}", input.toString());
            if (input.getDomainName() != null) {
                InstanceIdentifier
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
    public Future<RpcResult<AddConnectionTemplateOutput>> addConnectionTemplate(AddConnectionTemplateInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final AddConnectionTemplateOutputBuilder output = new AddConnectionTemplateOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddConnectionTemplate event | {}", input.toString());
            if (input.getDomainName() != null && !input.getDomainName().isEmpty()
                    && input.getTemplatePrefix() != null) {
                ConnectionTemplateBuilder builder = new ConnectionTemplateBuilder(input);

                output.setResult(datastoreAccess.checkAndPut(getIdentifier(nodeId).child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                .child(ConnectionTemplates.class)
                                .child(ConnectionTemplate.class, new ConnectionTemplateKey(builder.getTemplatePrefix())),
                        builder.build(), getDatastoreType(input.getConfigPersistence()), false));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<GetConnectionsOutput>> getConnections(final GetConnectionsInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder();
        final GetConnectionsOutputBuilder
                output =
                new GetConnectionsOutputBuilder().setConnections(connectionsBuilder.build());

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcGetConnectionsStatus event | {}", input.toString());
            Connections
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

    @Override public Future<RpcResult<GetNodeBindingsOutput>> getNodeBindings(final GetNodeBindingsInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final GetNodeBindingsOutputBuilder output = new GetNodeBindingsOutputBuilder().setBinding(new ArrayList<>());

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcGetNodeBindings event | {}", input.toString());
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.Binding>
                    bindings =
                    new ArrayList<>();
            List<MasterDatabaseBinding> databaseBindings = new ArrayList<>();
            MasterDatabase
                    database =
                    datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpDomains.class)
                            .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                            .child(MasterDatabase.class), LogicalDatastoreType.OPERATIONAL);
            if (database != null && database.getMasterDatabaseBinding() != null) {
                databaseBindings.addAll(database.getMasterDatabaseBinding());
            }
            if (GetNodeBindingsInput.BindingsRange.Local.equals(input.getBindingsRange())) {
                databaseBindings.removeIf(b -> b.getPeerSequence() != null && b.getPeerSequence().getPeer() != null
                        && !b.getPeerSequence().getPeer().isEmpty());
            }
            Map<Sgt, List<IpPrefix>> sgtListMap = new HashMap<>();
            for (MasterDatabaseBinding binding : databaseBindings) {
                if (!sgtListMap.containsKey(binding.getSecurityGroupTag())) {
                    sgtListMap.put(binding.getSecurityGroupTag(), new ArrayList<>());
                }
                sgtListMap.get(binding.getSecurityGroupTag()).add(binding.getIpPrefix());
            }
            for (Map.Entry<Sgt, List<IpPrefix>> entry : sgtListMap.entrySet()) {
                org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.BindingBuilder
                        bindingBuilder =
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.BindingBuilder();
                bindingBuilder.setSgt(entry.getKey());
                bindingBuilder.setIpPrefix(entry.getValue());
                bindings.add(bindingBuilder.build());
            }

            GetNodeBindingsOutputBuilder output1 = new GetNodeBindingsOutputBuilder();
            output1.setBinding(bindings);
            return RpcResultBuilder.success(output1.build()).build();

        });
    }

    @Override public Future<RpcResult<GetPeerGroupOutput>> getPeerGroup(final GetPeerGroupInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final GetPeerGroupOutputBuilder output = new GetPeerGroupOutputBuilder().setSxpPeerGroup(null);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcGetPeerGroup {}", input.toString());
            if (input.getPeerGroupName() != null) {
                SxpPeerGroup
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

    @Override public Future<RpcResult<GetPeerGroupsOutput>> getPeerGroups(final GetPeerGroupsInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final GetPeerGroupsOutputBuilder output = new GetPeerGroupsOutputBuilder().setSxpPeerGroup(new ArrayList<>());

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcGetPeerGroups event | {}", input.toString());
            SxpPeerGroups
                    groups =
                    datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpPeerGroups.class),
                            LogicalDatastoreType.OPERATIONAL);
            if (groups != null) {
                output.setSxpPeerGroup(groups.getSxpPeerGroup());
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<AddNodeOutput>> addNode(AddNodeInput input) {
        final AddNodeOutputBuilder output = new AddNodeOutputBuilder().setResult(false);
        return executor.submit(() -> {
            if (input.getNodeId() != null) {
                LOG.info("RpcAddNode event | {}", input.toString());
                final String nodeId = getNodeId(input.getNodeId());
                SxpNodeIdentityBuilder identityBuilder = new SxpNodeIdentityBuilder(input);
                if (identityBuilder.getTcpPort() == null)
                    identityBuilder.setTcpPort(new PortNumber(64999));
                if (identityBuilder.getVersion() == null)
                    identityBuilder.setVersion(Version.Version4);
                if (identityBuilder.getMappingExpanded() == null)
                    identityBuilder.setMappingExpanded(0);
                if (identityBuilder.getSxpDomains() == null || identityBuilder.getSxpDomains().getSxpDomain() == null)
                    identityBuilder.setSxpDomains(new SxpDomainsBuilder().setSxpDomain(new ArrayList<>()).build());
                if (identityBuilder.getSxpDomains().getSxpDomain().isEmpty() || identityBuilder.getSxpDomains()
                        .getSxpDomain()
                        .stream()
                        .noneMatch(d -> SxpNode.DEFAULT_DOMAIN.equals(d.getDomainName()))) {
                    identityBuilder.getSxpDomains()
                            .getSxpDomain()
                            .add(new SxpDomainBuilder().setConnections(
                                    new ConnectionsBuilder().setConnection(new ArrayList<>()).build())
                                    .setDomainName(org.opendaylight.sxp.core.SxpNode.DEFAULT_DOMAIN)
                                    .setMasterDatabase(ConfigLoader.parseMasterDatabase(input.getMasterDatabase()))
                                    .setDomainFilters(
                                            new DomainFiltersBuilder().setDomainFilter(new ArrayList<>()).build())
                                    .setConnectionTemplates(
                                            new ConnectionTemplatesBuilder().setConnectionTemplate(new ArrayList<>())
                                                    .build())
                                    .build());
                }
                if (identityBuilder.getSxpPeerGroups() == null)
                    identityBuilder.setSxpPeerGroups(new SxpPeerGroupsBuilder().build());
                if (identityBuilder.getSecurity() == null)
                    identityBuilder.setSecurity(new SecurityBuilder().build());
                if (identityBuilder.getTimers() == null)
                    identityBuilder.setTimers(new TimersBuilder().build());
                ConfigLoader.initTopologyNode(nodeId, getDatastoreType(input.getConfigPersistence()), datastoreAccess);
                output.setResult(datastoreAccess.checkAndPut(NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class,
                        new NodeKey(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                        nodeId))).augmentation(SxpNodeIdentity.class), identityBuilder.build(),
                        getDatastoreType(input.getConfigPersistence()), false));
                if (output.isResult())
                    for (int i = 0; Configuration.getRegisteredNode(nodeId) != null && i < 10; i++)
                        Thread.sleep(100);
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<AddDomainOutput>> addDomain(AddDomainInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final AddDomainOutputBuilder output = new AddDomainOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDomain event | {}", input.toString());
            if (input.getDomainName() != null) {
                SxpDomainBuilder builder = new SxpDomainBuilder();
                builder.setDomainName(input.getDomainName());
                builder.setMasterDatabase(ConfigLoader.parseMasterDatabase(input.getMasterDatabase()));
                builder.setConnections(ConfigLoader.parseConnections(input.getConnections()));
                builder.setDomainFilters(new DomainFiltersBuilder().setDomainFilter(new ArrayList<>()).build());
                builder.setConnectionTemplates(
                        new ConnectionTemplatesBuilder().setConnectionTemplate(new ArrayList<>()).build());
                output.setResult(datastoreAccess.checkAndPut(getIdentifier(nodeId).child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(input.getDomainName())), builder.build(),
                        getDatastoreType(input.getConfigPersistence()), false));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<AddBindingsOutput>> addBindings(AddBindingsInput input) {
        final String nodeId = getNodeId(input.getNodeId());
        final AddBindingsOutputBuilder output = new AddBindingsOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddBindings event | {}", input.toString());
            if (input.getDomainName() == null) {
                LOG.warn("RpcDeleteEntry exception | Parameter 'domain-name' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            final MasterDatabase
                    database =
                    datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpDomains.class)
                            .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                            .child(MasterDatabase.class), LogicalDatastoreType.CONFIGURATION);
            if (input.getBinding() != null && !input.getBinding().isEmpty() && database != null
                    && database.getMasterDatabaseBinding() != null) {
                MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
                bindingBuilder.setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());
                bindingBuilder.setTimestamp(TimeConv.toDt(System.currentTimeMillis()));

                final Map<IpPrefix, MasterDatabaseBinding> bindings = new HashMap<>();
                input.getBinding()
                        .stream()
                        .filter(b -> b != null && b.getSgt() != null && b.getIpPrefix() != null && !b.getIpPrefix()
                                .isEmpty())
                        .forEach(g -> g.getIpPrefix()
                                .forEach(p -> bindings.put(p,
                                        bindingBuilder.setIpPrefix(p).setSecurityGroupTag(g.getSgt()).build())));

                database.getMasterDatabaseBinding().removeIf(b -> bindings.containsKey(b.getIpPrefix()));
                database.getMasterDatabaseBinding().addAll(bindings.values());
                output.setResult(datastoreAccess.putSynchronous(getIdentifier(nodeId).child(SxpDomains.class)
                        .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                        .child(MasterDatabase.class), database, LogicalDatastoreType.CONFIGURATION));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<UpdateEntryOutput>> updateEntry(final UpdateEntryInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final UpdateEntryOutputBuilder output = new UpdateEntryOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcUpdateEntry event | {}", input.toString());
            IpPrefix originalIpPrefix = input.getOriginalBinding().getIpPrefix();
            if (originalIpPrefix == null) {
                LOG.warn("RpcUpdateEntry exception | Parameter 'original-binding/ip-prefix' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            Sgt originalSgt = input.getOriginalBinding().getSgt();
            if (originalSgt == null) {
                LOG.warn("RpcUpdateEntry exception | Parameter 'original-binding/sgt' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            if (input.getDomainName() == null) {
                LOG.warn("RpcUpdateEntry exception | Parameter 'domain-name' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            if (input.getNewBinding().getIpPrefix() == null) {
                LOG.warn("RpcUpdateEntry exception | Parameter 'new-binding/ip-prefix' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            if (input.getNewBinding().getSgt() == null) {
                LOG.warn("RpcUpdateEntry exception | Parameter 'new-binding/sgt' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            MasterDatabaseBinding
                    binding =
                    datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpDomains.class)
                                    .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                    .child(MasterDatabase.class)
                                    .child(MasterDatabaseBinding.class, new MasterDatabaseBindingKey(originalIpPrefix)),
                            getDatastoreType(input.getConfigPersistence()));
            if (binding != null && originalSgt.getValue().equals(binding.getSecurityGroupTag().getValue())) {
                binding =
                        new MasterDatabaseBindingBuilder().setIpPrefix(input.getNewBinding().getIpPrefix())
                                .setTimestamp(TimeConv.toDt(System.currentTimeMillis()))
                                .setSecurityGroupTag(input.getNewBinding().getSgt())
                                .setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build())
                                .build();

                output.setResult(datastoreAccess.checkAndDelete(getIdentifier(nodeId).child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                .child(MasterDatabase.class)
                                .child(MasterDatabaseBinding.class, new MasterDatabaseBindingKey(originalIpPrefix)),
                        getDatastoreType(input.getConfigPersistence())) && datastoreAccess.checkAndPut(
                        getIdentifier(nodeId).child(SxpDomains.class)
                                .child(SxpDomain.class, new SxpDomainKey(input.getDomainName()))
                                .child(MasterDatabase.class)
                                .child(MasterDatabaseBinding.class,
                                        new MasterDatabaseBindingKey(binding.getIpPrefix())), binding,
                        getDatastoreType(input.getConfigPersistence()), false));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<UpdateFilterOutput>> updateFilter(final UpdateFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DatastoreAccess datastoreAccess = getDatastoreAccess(nodeId);
        final UpdateFilterOutputBuilder output = new UpdateFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcUpdateFilter event | {}", input.toString());
            if (input.getSxpFilter() != null && input.getSxpFilter().getFilterType() != null) {
                SxpFilterBuilder filter = new SxpFilterBuilder(input.getSxpFilter());
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
