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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddEntryInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddEntryOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddEntryOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.ConfigPersistence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteEntryInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteEntryOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteEntryOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteFilterOutputBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.FilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.PeerSequenceFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.PrefixListFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.opendaylight.sxp.controller.core.SxpDatastoreNode.getIdentifier;

public class RpcServiceImpl implements SxpControllerService, AutoCloseable {

    private final DatastoreAccess datastoreAccess;

    private static final Logger LOG = LoggerFactory.getLogger(RpcServiceImpl.class.getName());

    private static String getNodeId(NodeId requestedNodeId) {
        return NodeIdConv.toString(Preconditions.checkNotNull(requestedNodeId));
    }

    public RpcServiceImpl(DatastoreAccess datastoreAccess) {
        this.datastoreAccess = datastoreAccess;
    }

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    private <T> Future<RpcResult<T>> getResponse(String nodeId, final T response,
            Callable<RpcResult<T>> resultCallable) {
        SxpNode node = Configuration.getRegisteredNode(nodeId);
        if (nodeId == null) {
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

    //TODO
    private LogicalDatastoreType getDatastoreType(ConfigPersistence persistence) {
        if (ConfigPersistence.Operational.equals(persistence))
            return LogicalDatastoreType.OPERATIONAL;
        else
            return LogicalDatastoreType.CONFIGURATION;
    }

    private FilterSpecific getFilterSpecific(FilterEntries entries) {
        Preconditions.checkNotNull(entries);
        if (entries instanceof AclFilterEntries || entries instanceof PrefixListFilterEntries) {
            return FilterSpecific.AccessOrPrefixList;
        } else if (entries instanceof PeerSequenceFilterEntries) {
            return FilterSpecific.PeerSequence;
        }
        throw new IllegalArgumentException("Unknown entries type");
    }

    @Override public Future<RpcResult<AddConnectionOutput>> addConnection(final AddConnectionInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final AddConnectionOutputBuilder output = new AddConnectionOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddConnection event | {}", input.toString());
            Preconditions.checkNotNull(input.getConnections());
            Preconditions.checkNotNull(input.getConnections().getConnection()).forEach(c -> {
                output.setResult(datastoreAccess.checkAndPut(getIdentifier(nodeId).child(Connections.class)
                                .child(Connection.class, new ConnectionKey(c.getPeerAddress(), c.getTcpPort())),
                        new ConnectionBuilder(c).setNodeId(null)
                                .setState(null)
                                .setTimestampUpdateOrKeepAliveMessage(null)
                                .build(), getDatastoreType(input.getConfigPersistence()), false));
            });
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<AddEntryOutput>> addEntry(final AddEntryInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final AddEntryOutputBuilder output = new AddEntryOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddEntry event | {}", input.toString());

            IpPrefix ipPrefix = input.getIpPrefix();
            if (ipPrefix == null) {
                LOG.warn("RpcAddEntry exception | Parameter 'ip-prefix' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }
            Sgt sgt = input.getSgt();
            if (sgt == null) {
                LOG.warn("RpcAddEntry exception | Parameter 'sgt' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }

            DateAndTime timestamp = TimeConv.toDt(System.currentTimeMillis());
            List<MasterDatabaseBinding> bindings = new ArrayList<>();

            MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
            bindingBuilder.setIpPrefix(ipPrefix).setTimestamp(timestamp);
            bindingBuilder.setSecurityGroupTag(sgt);
            bindingBuilder.setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());
            bindings.add(bindingBuilder.build());

            Configuration.getRegisteredNode(nodeId).putLocalBindingsMasterDatabase(bindings);
            output.setResult(true);
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<AddFilterOutput>> addFilter(final AddFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final AddFilterOutputBuilder output = new AddFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddFilter event | {}", input.toString());
            SxpFilterBuilder filter = new SxpFilterBuilder(input.getSxpFilter());
            filter.setFilterSpecific(getFilterSpecific(filter.getFilterEntries()));
            output.setResult(datastoreAccess.checkAndPut(getIdentifier(nodeId).child(SxpPeerGroups.class)
                            .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName()))
                            .child(SxpFilter.class, new SxpFilterKey(filter.getFilterSpecific(), filter.getFilterType())),
                    filter.build(), getDatastoreType(input.getConfigPersistence()), false));
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<AddPeerGroupOutput>> addPeerGroup(final AddPeerGroupInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final AddPeerGroupOutputBuilder output = new AddPeerGroupOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcAddPerGroup event | {}", input.toString());
            output.setResult(datastoreAccess.checkAndPut(getIdentifier(nodeId).child(SxpPeerGroups.class)
                            .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getSxpPeerGroup().getName())),
                    new SxpPeerGroupBuilder(input.getSxpPeerGroup()).build(),
                    getDatastoreType(input.getConfigPersistence()), false));
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public void close() {
        executor.shutdown();
        Configuration.getNodes().values().forEach(SxpNode::shutdown);
    }

    @Override public Future<RpcResult<DeleteConnectionOutput>> deleteConnection(final DeleteConnectionInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DeleteConnectionOutputBuilder output = new DeleteConnectionOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcDeleteConnection event | {}", input.toString());
            InstanceIdentifier
                    identifier =
                    getIdentifier(nodeId).child(Connections.class)
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

            Sgt sgt = input.getSgt();
            if (sgt == null) {
                LOG.warn("RpcDeleteEntry exception | Parameter 'sgt' not defined");
                return RpcResultBuilder.success(output.build()).build();
            }

            DateAndTime timestamp = TimeConv.toDt(System.currentTimeMillis());
            List<MasterDatabaseBinding> bindings = new ArrayList<>();
            MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
            bindingBuilder.setSecurityGroupTag(sgt);
            bindingBuilder.setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());

            if (input.getIpPrefix() != null) {
                for (IpPrefix ipPrefix : input.getIpPrefix()) {
                    bindingBuilder.setIpPrefix(ipPrefix).setTimestamp(timestamp);
                    bindings.add(bindingBuilder.build());
                }
            }

            DeleteEntryOutputBuilder output1 = new DeleteEntryOutputBuilder();
            output1.setResult(
                    !Configuration.getRegisteredNode(nodeId).removeLocalBindingsMasterDatabase(bindings).isEmpty());
            return RpcResultBuilder.success(output1.build()).build();
        });
    }

    @Override public Future<RpcResult<DeleteFilterOutput>> deleteFilter(final DeleteFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
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
                        output.setResult(datastoreAccess.checkAndDelete(
                                getIdentifier(nodeId).child(SxpPeerGroups.class)
                                        .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName()))
                                        .child(SxpFilter.class,
                                                new SxpFilterKey(f.getFilterSpecific(), f.getFilterType())),
                                datastoreType));
                });
            } else {
                InstanceIdentifier
                        identifier =
                        getIdentifier(nodeId).child(SxpPeerGroups.class)
                                .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName()))
                                .child(SxpFilter.class,
                                        new SxpFilterKey(input.getFilterSpecific(), input.getFilterType()));
                output.setResult(
                        datastoreAccess.checkAndDelete(identifier, LogicalDatastoreType.CONFIGURATION)
                                || datastoreAccess.checkAndDelete(identifier,
                                LogicalDatastoreType.OPERATIONAL));
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<DeletePeerGroupOutput>> deletePeerGroup(final DeletePeerGroupInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
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

    @Override public Future<RpcResult<GetConnectionsOutput>> getConnections(final GetConnectionsInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder();
        final GetConnectionsOutputBuilder
                output =
                new GetConnectionsOutputBuilder().setConnections(connectionsBuilder.build());

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcGetConnectionsStatus event | {}", input.toString());
            Connections
                    connections =
                    datastoreAccess.readSynchronous(getIdentifier(nodeId).child(Connections.class),
                            LogicalDatastoreType.OPERATIONAL);
            if (connections != null && connections.getConnection() != null) {
                output.setConnections(connections);
            }
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<GetNodeBindingsOutput>> getNodeBindings(final GetNodeBindingsInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final GetNodeBindingsOutputBuilder output = new GetNodeBindingsOutputBuilder().setBinding(new ArrayList<>());

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcGetNodeBindings event | {}", input.toString());
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.Binding>
                    bindings =
                    new ArrayList<>();
            List<MasterDatabaseBinding> databaseBindings = new ArrayList<>();
            switch (input.getBindingsRange()) {
                case All:
                    databaseBindings.addAll(Preconditions.checkNotNull(
                            datastoreAccess.readSynchronous(getIdentifier(nodeId).child(MasterDatabase.class),
                                    LogicalDatastoreType.OPERATIONAL)).getMasterDatabaseBinding());
                case Local:
                    databaseBindings.addAll(Preconditions.checkNotNull(
                            datastoreAccess.readSynchronous(getIdentifier(nodeId).child(MasterDatabase.class),
                                    LogicalDatastoreType.CONFIGURATION)).getMasterDatabaseBinding());
                    break;
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
        final GetPeerGroupOutputBuilder output = new GetPeerGroupOutputBuilder().setSxpPeerGroup(null);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcGetPeerGroup {}", input.toString());
            output.setSxpPeerGroup(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroupBuilder(
                            datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpPeerGroups.class)
                                            .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName())),
                                    LogicalDatastoreType.OPERATIONAL)).build());
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<GetPeerGroupsOutput>> getPeerGroups(final GetPeerGroupsInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final GetPeerGroupsOutputBuilder output = new GetPeerGroupsOutputBuilder().setSxpPeerGroup(new ArrayList<>());

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcGetPeerGroups event | {}", input.toString());
            output.setSxpPeerGroup(Preconditions.checkNotNull(
                    datastoreAccess.readSynchronous(getIdentifier(nodeId).child(SxpPeerGroups.class),
                            LogicalDatastoreType.OPERATIONAL)).getSxpPeerGroup());
            return RpcResultBuilder.success(output.build()).build();
        });
    }

    @Override public Future<RpcResult<UpdateEntryOutput>> updateEntry(final UpdateEntryInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final UpdateEntryOutputBuilder output = new UpdateEntryOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), new Callable<RpcResult<UpdateEntryOutput>>() {

            @Override public RpcResult<UpdateEntryOutput> call() throws Exception {
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
                IpPrefix newIpPrefix = input.getNewBinding().getIpPrefix();
                if (newIpPrefix == null) {
                    LOG.warn("RpcUpdateEntry exception | Parameter 'new-binding/ip-prefix' not defined");
                    return RpcResultBuilder.success(output.build()).build();
                }
                Sgt newSgt = input.getNewBinding().getSgt();
                if (newSgt == null) {
                    LOG.warn("RpcUpdateEntry exception | Parameter 'new-binding/sgt' not defined");
                    return RpcResultBuilder.success(output.build()).build();
                }

                DateAndTime timestamp = TimeConv.toDt(System.currentTimeMillis());
                List<MasterDatabaseBinding> deleteBindings = new ArrayList<>(), addedBindings = new ArrayList<>();
                MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
                bindingBuilder.setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());

                bindingBuilder.setIpPrefix(originalIpPrefix).setSecurityGroupTag(originalSgt).setTimestamp(timestamp);
                deleteBindings.add(bindingBuilder.build());

                bindingBuilder.setIpPrefix(newIpPrefix).setSecurityGroupTag(newSgt).setTimestamp(timestamp);
                addedBindings.add(bindingBuilder.build());

                Configuration.getRegisteredNode(nodeId).removeLocalBindingsMasterDatabase(deleteBindings);

                UpdateEntryOutputBuilder output = new UpdateEntryOutputBuilder();
                output.setResult(!Configuration.getRegisteredNode(nodeId)
                        .putLocalBindingsMasterDatabase(addedBindings)
                        .isEmpty());
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override public Future<RpcResult<UpdateFilterOutput>> updateFilter(final UpdateFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final UpdateFilterOutputBuilder output = new UpdateFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), () -> {
            LOG.info("RpcUpdateFilter event | {}", input.toString());
            SxpFilterBuilder filter = new SxpFilterBuilder(input.getSxpFilter());
            filter.setFilterSpecific(getFilterSpecific(filter.getFilterEntries()));
            output.setResult(datastoreAccess.checkAndPut(getIdentifier(nodeId).child(SxpPeerGroups.class)
                            .child(SxpPeerGroup.class, new SxpPeerGroupKey(input.getPeerGroupName()))
                            .child(SxpFilter.class, new SxpFilterKey(filter.getFilterSpecific(), filter.getFilterType())),
                    filter.build(), getDatastoreType(input.getConfigPersistence()), true));
            return RpcResultBuilder.success(output.build()).build();
        });
    }

}
