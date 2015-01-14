/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.controller.util.database.SxpDatastoreImpl;
import org.opendaylight.sxp.controller.util.database.access.DatastoreAccess;
import org.opendaylight.sxp.controller.util.database.access.MasterDatabaseAccessImpl;
import org.opendaylight.sxp.controller.util.database.access.SxpDatabaseAccessImpl;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.util.database.MasterBindingIdentity;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddEntryInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddEntryOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddEntryOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteEntryInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteEntryOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteEntryOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetBindingSgtsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetBindingSgtsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetBindingSgtsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetConnectionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetConnectionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetConnectionsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetNodeBindingsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetNodeBindingsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetNodeBindingsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateEntryInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateEntryOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateEntryOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class RpcServiceImpl implements SxpControllerService, AutoCloseable {

    private static DatastoreAccess datastoreAccess;

    private static final Logger LOG = LoggerFactory.getLogger(RpcServiceImpl.class.getName());

    public static List<NodeId> getBindingsSources(MasterDatabase database) {
        List<NodeId> bindingsSources = new ArrayList<>();
        if (database.getSource() != null) {
            for (Source source : database.getSource()) {
                if (source.getPrefixGroup() != null) {
                    for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                        if (prefixGroup.getBinding() != null) {
                            for (Binding binding : prefixGroup.getBinding()) {
                                if (binding.getSources() != null && binding.getSources().getSource() != null) {
                                    for (NodeId bindingSource : binding.getSources().getSource()) {
                                        boolean contains = false;
                                        for (NodeId _bindingSource : bindingsSources) {
                                            if (NodeIdConv.equalTo(_bindingSource, bindingSource)) {
                                                contains = true;
                                                break;
                                            }
                                        }
                                        if (!contains) {
                                            bindingsSources.add(bindingSource);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return bindingsSources;
    }

    protected static MasterDatabaseInf getDatastoreProviderMaster(String nodeId) {
        return new MasterDatastoreImpl(nodeId, new MasterDatabaseAccessImpl(nodeId, datastoreAccess,
                LogicalDatastoreType.OPERATIONAL));
    }

    protected static SxpDatabaseInf getDatastoreProviderSxp(String nodeId) {
        return new SxpDatastoreImpl(nodeId, new SxpDatabaseAccessImpl(nodeId, datastoreAccess,
                LogicalDatastoreType.OPERATIONAL));
    }

    public static List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.Binding> getNodeBindings(
            MasterDatabase database, NodeId requestedNodeId) {
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.Binding> bindings = new ArrayList<>();
        if (database.getSource() != null) {
            for (Source source : database.getSource()) {
                if (source.getPrefixGroup() != null) {
                    for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                        if (prefixGroup.getBinding() != null) {
                            List<IpPrefix> ipPrefixes = new ArrayList<IpPrefix>();
                            for (Binding binding : prefixGroup.getBinding()) {
                                if (binding.getSources() != null && binding.getSources().getSource() != null) {
                                    boolean contains = false;
                                    for (NodeId _nodeId : binding.getSources().getSource()) {
                                        if (NodeIdConv.equalTo(_nodeId, requestedNodeId)) {
                                            contains = true;
                                            break;
                                        }
                                    }
                                    if (contains) {
                                        ipPrefixes.add(binding.getIpPrefix());
                                    }
                                }
                            }
                            if (!ipPrefixes.isEmpty()) {
                                org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.BindingBuilder bindingBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.BindingBuilder();
                                bindingBuilder.setSgt(prefixGroup.getSgt());
                                bindingBuilder.setIpPrefix(ipPrefixes);
                                bindings.add(bindingBuilder.build());
                            }
                        }
                    }
                }
            }
        }
        return bindings;
    }

    private static String getNodeId(NodeId requestedNodeId) {
        String nodeId = Configuration.CONTROLLER_NAME;
        if (requestedNodeId != null) {
            nodeId = NodeIdConv.toString(requestedNodeId);
        } else if (Configuration.isNodesRegistered()) {
            nodeId = Configuration.getNextNodeName();
        }
        return nodeId;
    }

    private static boolean isBindingPresent(String nodeId, Sgt sgt, IpPrefix ipPrefix) throws Exception {

        InstanceIdentifier<Binding> bindingIdentifier = InstanceIdentifier
                .builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class,
                        new NodeKey(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                        nodeId))).augmentation(SxpNodeIdentity.class).child(MasterDatabase.class)
                .child(Source.class, new SourceKey(DatabaseBindingSource.Local))
                .child(PrefixGroup.class, new PrefixGroupKey(sgt)).child(Binding.class, new BindingKey(ipPrefix))
                .build();

        CheckedFuture<Optional<Binding>, ReadFailedException> binding = datastoreAccess.read(bindingIdentifier,
                LogicalDatastoreType.OPERATIONAL);
        if (binding.get() != null && binding.get().isPresent()) {
            return true;
        }
        return false;
    }

    private static boolean isConnectionPresent(String nodeId, Ipv4Address peerAddress, PortNumber tcpPort)
            throws Exception {

        InstanceIdentifier<Connection> connectionIdentifier = InstanceIdentifier
                .builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class,
                        new NodeKey(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                        nodeId))).augmentation(SxpNodeIdentity.class).child(Connections.class)
                .child(Connection.class, new ConnectionKey(peerAddress, tcpPort)).build();

        CheckedFuture<Optional<Connection>, ReadFailedException> connection = datastoreAccess.read(
                connectionIdentifier, LogicalDatastoreType.OPERATIONAL);
        if (connection.get() != null && connection.get().isPresent()) {
            return true;
        }
        return false;
    }

    private static boolean isPrefixGroupPresent(String nodeId, Sgt sgt) throws Exception {

        InstanceIdentifier<PrefixGroup> prefixGroupIdentifier = InstanceIdentifier
                .builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(Configuration.TOPOLOGY_NAME)))
                .child(Node.class,
                        new NodeKey(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                        nodeId))).augmentation(SxpNodeIdentity.class).child(MasterDatabase.class)
                .child(Source.class, new SourceKey(DatabaseBindingSource.Local))
                .child(PrefixGroup.class, new PrefixGroupKey(sgt)).build();

        CheckedFuture<Optional<PrefixGroup>, ReadFailedException> prefixGroup = datastoreAccess.read(
                prefixGroupIdentifier, LogicalDatastoreType.OPERATIONAL);
        if (prefixGroup.get() != null && prefixGroup.get().isPresent()) {
            return true;
        }
        return false;
    }

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public RpcServiceImpl(DatastoreAccess datastoreAccess) {
        RpcServiceImpl.datastoreAccess = datastoreAccess;
    }

    @Override
    public Future<RpcResult<AddConnectionOutput>> addConnection(final AddConnectionInput input) {
        return executor.submit(new Callable<RpcResult<AddConnectionOutput>>() {

            @SuppressWarnings("unchecked")
            @Override
            public RpcResult<AddConnectionOutput> call() throws Exception {
                LOG.info("RpcAddConnection event | {}", input.toString());

                Connections connections = input.getConnections();
                if (connections == null || connections.getConnection() == null || connections.getConnection().isEmpty()) {
                    LOG.warn("RpcAddConnection exception | Parameter 'connections' not defined");
                    AddConnectionOutputBuilder output = new AddConnectionOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<AddConnectionOutput>) output).build();
                }

                String nodeId = getNodeId(input.getRequestedNode());

                List<Connection> _connections = new ArrayList<Connection>();
                for (Connection connection : connections.getConnection()) {
                    Ipv4Address peerAddress = connection.getPeerAddress();
                    if (peerAddress == null || peerAddress.getValue() == null || peerAddress.getValue().isEmpty()) {
                        LOG.warn("RpcAddConnection exception | Connection parameter 'peer-address' not defined");
                        continue;
                    }
                    PortNumber tcpPort = connection.getTcpPort();
                    if (tcpPort == null || tcpPort.getValue() <= 0) {
                        tcpPort = new PortNumber(Configuration.getConstants().getPort());
                    }
                    if (isConnectionPresent(nodeId, peerAddress, tcpPort)) {
                        LOG.warn("RpcAddConnection exception | Connection already exists '{}:{}'",
                                peerAddress.getValue(), tcpPort.getValue());
                        continue;
                    }
                    _connections.add(connection);
                }
                if (_connections.isEmpty()) {
                    LOG.warn("RpcAddConnection exception | No new connections");
                    AddConnectionOutputBuilder output = new AddConnectionOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<AddConnectionOutput>) output).build();
                }

                ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder();
                connectionsBuilder.setConnection(_connections);

                Configuration.getRegisteredNode(nodeId).addConnections(connectionsBuilder.build());
                AddConnectionOutputBuilder output = new AddConnectionOutputBuilder();
                output.setResult(true);
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override
    public Future<RpcResult<AddEntryOutput>> addEntry(final AddEntryInput input) {
        return executor.submit(new Callable<RpcResult<AddEntryOutput>>() {
            @SuppressWarnings("unchecked")
            @Override
            public RpcResult<AddEntryOutput> call() throws Exception {
                LOG.info("RpcAddEntry event | {}", input.toString());

                IpPrefix ipPrefix = input.getIpPrefix();
                if (ipPrefix == null) {
                    LOG.warn("RpcAddEntry exception | Parameter 'ip-prefix' not defined");
                    AddEntryOutputBuilder addEntryOutput = new AddEntryOutputBuilder();
                    addEntryOutput.setResult(false);
                    RpcResultBuilder<AddEntryOutput> rpcResultBuilder = RpcResultBuilder
                            .from((RpcResult<AddEntryOutput>) addEntryOutput);
                    return rpcResultBuilder.build();
                }
                Sgt sgt = input.getSgt();
                if (sgt == null) {
                    LOG.warn("RpcAddEntry exception | Parameter 'sgt' not defined");
                    AddEntryOutputBuilder output = new AddEntryOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<AddEntryOutput>) output).build();
                }
                String nodeId = getNodeId(input.getRequestedNode());

                if (isBindingPresent(nodeId, sgt, ipPrefix)) {
                    LOG.warn("RpcAddEntry exception | Binding already defined '{} {}'", sgt.getValue(),
                            IpPrefixConv.toString(ipPrefix));
                    AddEntryOutputBuilder output = new AddEntryOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<AddEntryOutput>) output).build();
                }

                DateAndTime timestamp = TimeConv.toDt(System.currentTimeMillis());

                BindingBuilder bindingBuilder = new BindingBuilder();
                bindingBuilder.setIpPrefix(ipPrefix);

                List<NodeId> nodeIds = new ArrayList<>();
                nodeIds.add(Configuration.getRegisteredNode(nodeId).getNodeId());

                bindingBuilder.setAction(DatabaseAction.Add);
                bindingBuilder.setPeerSequence(NodeIdConv.createPeerSequence(nodeIds));
                bindingBuilder.setSources(NodeIdConv.createSources(nodeIds));
                bindingBuilder.setTimestamp(timestamp);
                bindingBuilder.setChanged(true);

                List<Binding> bindings = new ArrayList<>();
                bindings.add(bindingBuilder.build());

                List<PrefixGroup> prefixGroups = new ArrayList<>();
                PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder();
                prefixGroupBuilder.setSgt(sgt);
                prefixGroupBuilder.setBinding(bindings);
                prefixGroups.add(prefixGroupBuilder.build());

                getDatastoreProviderMaster(nodeId).addBindingsLocal(prefixGroups);
                notifyService(nodeId);

                AddEntryOutputBuilder output = new AddEntryOutputBuilder();
                output.setResult(true);
                return RpcResultBuilder.success(output.build()).build();
            }

        });
    }

    @Override
    public void close() throws Exception {
        executor.shutdown();
    }

    @Override
    public Future<RpcResult<DeleteConnectionOutput>> deleteConnection(final DeleteConnectionInput input) {
        return executor.submit(new Callable<RpcResult<DeleteConnectionOutput>>() {
            @SuppressWarnings("unchecked")
            @Override
            public RpcResult<DeleteConnectionOutput> call() throws Exception {
                LOG.info("RpcDeleteConnection event | {}", input.toString());

                Ipv4Address peerAddress = input.getPeerAddress();
                if (peerAddress == null || peerAddress.getValue() == null || peerAddress.getValue().isEmpty()) {
                    LOG.warn("RpcDeleteConnection exception | Parameter 'peer-address' not defined");
                    DeleteConnectionOutputBuilder output = new DeleteConnectionOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<DeleteConnectionOutput>) output).build();
                }

                PortNumber portNumber = input.getTcpPort();
                if (portNumber == null) {
                    portNumber = new PortNumber(Configuration.getConstants().getPort());
                    LOG.info("RpcDeleteConnection | Parameter 'tcp-port' default value used ['{}']", Configuration
                            .getConstants().getPort());
                }
                if (portNumber.getValue() <= 0) {
                    LOG.warn("RpcDeleteConnection exception | Parameter 'tcp-port' <= 0");
                    DeleteConnectionOutputBuilder output = new DeleteConnectionOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<DeleteConnectionOutput>) output).build();
                }
                InetSocketAddress destination = IpPrefixConv.parseInetPrefix(peerAddress.getValue());
                destination = new InetSocketAddress(destination.getAddress(), portNumber.getValue());

                String nodeId = getNodeId(input.getRequestedNode());

                SxpConnection connection = Configuration.getRegisteredNode(nodeId).removeConnection(destination);
                if (connection == null) {
                    LOG.warn("RpcDeleteConnection exception | Connection '{}' not exists", destination);
                    DeleteConnectionOutputBuilder output = new DeleteConnectionOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<DeleteConnectionOutput>) output).build();
                }
                DeleteConnectionOutputBuilder output = new DeleteConnectionOutputBuilder();
                output.setResult(true);
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override
    public Future<RpcResult<DeleteEntryOutput>> deleteEntry(final DeleteEntryInput input) {
        return executor.submit(new Callable<RpcResult<DeleteEntryOutput>>() {
            @SuppressWarnings("unchecked")
            @Override
            public RpcResult<DeleteEntryOutput> call() throws Exception {
                LOG.info("RpcDeleteEntry event | {}", input.toString());

                Sgt sgt = input.getSgt();
                if (sgt == null) {
                    LOG.warn("RpcDeleteEntry exception | Parameter 'sgt' not defined");
                    DeleteEntryOutputBuilder output = new DeleteEntryOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<DeleteEntryOutput>) output).build();
                }
                String nodeId = getNodeId(input.getRequestedNode());

                if (!isPrefixGroupPresent(nodeId, sgt)) {
                    LOG.warn("RpcDeleteEntry exception | Prefix group doesn't exist '{}'", sgt.getValue());
                    DeleteEntryOutputBuilder output = new DeleteEntryOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<DeleteEntryOutput>) output).build();
                }

                List<PrefixGroup> prefixGroups = new ArrayList<>();
                PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder();
                prefixGroupBuilder.setSgt(sgt);

                if (input.getIpPrefix() != null) {
                    List<Binding> bindings = new ArrayList<>();
                    for (IpPrefix ipPrefix : input.getIpPrefix()) {
                        BindingBuilder bindingBuilder = new BindingBuilder();
                        bindingBuilder.setIpPrefix(ipPrefix);
                        bindings.add(bindingBuilder.build());
                    }
                    prefixGroupBuilder.setBinding(bindings);
                }
                prefixGroups.add(prefixGroupBuilder.build());

                getDatastoreProviderMaster(nodeId).setAsDeleted(prefixGroups);
                notifyService(nodeId);

                DeleteEntryOutputBuilder output = new DeleteEntryOutputBuilder();
                output.setResult(true);
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override
    public Future<RpcResult<GetBindingSgtsOutput>> getBindingSgts(final GetBindingSgtsInput input) {
        return executor.submit(new Callable<RpcResult<GetBindingSgtsOutput>>() {
            @SuppressWarnings("unchecked")
            @Override
            public RpcResult<GetBindingSgtsOutput> call() throws Exception {
                LOG.info("RpcGetBindingSgts event | {}", input.toString());

                IpPrefix ipPrefix = input.getIpPrefix();
                if (ipPrefix == null) {
                    LOG.warn("RpcGetBindingSgts exception | Parameter 'ip-prefix' not defined");
                    GetBindingSgtsOutputBuilder output = new GetBindingSgtsOutputBuilder();
                    output.setSgt(null);
                    return RpcResultBuilder.from((RpcResult<GetBindingSgtsOutput>) output).build();
                }
                String nodeId = getNodeId(input.getRequestedNode());

                List<Sgt> sgts = new ArrayList<>();
                for (MasterBindingIdentity bindingIdentity : getDatastoreProviderMaster(nodeId).readBindings()) {
                    if (IpPrefixConv.equalTo(bindingIdentity.getBinding().getIpPrefix(), ipPrefix)) {
                        sgts.add(new Sgt(bindingIdentity.getPrefixGroup().getSgt()));
                    }
                }

                GetBindingSgtsOutputBuilder output = new GetBindingSgtsOutputBuilder();
                output.setSgt(sgts);
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override
    public Future<RpcResult<GetConnectionsOutput>> getConnections(final GetConnectionsInput input) {
        return executor.submit(new Callable<RpcResult<GetConnectionsOutput>>() {
            @SuppressWarnings("unchecked")
            @Override
            public RpcResult<GetConnectionsOutput> call() throws Exception {
                LOG.info("RpcGetConnectionsStatus event | {}", input.toString());

                String nodeId = getNodeId(input.getRequestedNode());

                ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder();
                SxpNode node = Configuration.getRegisteredNode(nodeId);
                if (node == null) {
                    LOG.warn("RpcGetConnectionsStatus exception | Requested node '" + nodeId + "' not exist");
                    GetConnectionsOutputBuilder output = new GetConnectionsOutputBuilder();
                    output.setConnections(connectionsBuilder.build());
                    return RpcResultBuilder.from((RpcResult<GetConnectionsOutput>) output).build();

                }

                List<Connection> connections = new ArrayList<Connection>();
                for (SxpConnection connection : Configuration.getRegisteredNode(nodeId).values()) {
                    connections.add(connection.getConnection());
                }
                connectionsBuilder.setConnection(connections);
                GetConnectionsOutputBuilder output = new GetConnectionsOutputBuilder();
                output.setConnections(connectionsBuilder.build());
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override
    public Future<RpcResult<GetNodeBindingsOutput>> getNodeBindings(final GetNodeBindingsInput input) {
        return executor.submit(new Callable<RpcResult<GetNodeBindingsOutput>>() {
            @SuppressWarnings("unchecked")
            @Override
            public RpcResult<GetNodeBindingsOutput> call() throws Exception {
                LOG.info("RpcGetNodeBindings event | {}", input.toString());

                NodeId requestedNodeId = input.getRequestedNode();
                if (requestedNodeId == null) {
                    LOG.warn("RpcGetBindingSgts exception | Parameter 'requested-node' not defined");
                    GetNodeBindingsOutputBuilder output = new GetNodeBindingsOutputBuilder();
                    output.setBinding(new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.Binding>());
                    return RpcResultBuilder.from((RpcResult<GetNodeBindingsOutput>) output).build();
                }

                String nodeId = getNodeId(input.getLocalRequestedNode());

                MasterDatabase database = getDatastoreProviderMaster(nodeId).get();
                GetNodeBindingsOutputBuilder output = new GetNodeBindingsOutputBuilder();
                output.setBinding(getNodeBindings(database, requestedNodeId));
                return RpcResultBuilder.success(output.build()).build();

            }
        });
    }

    private void notifyService(String nodeName) {
        Configuration.getRegisteredNode(nodeName).notifyService();
    }

    @Override
    public Future<RpcResult<UpdateEntryOutput>> updateEntry(final UpdateEntryInput input) {
        return executor.submit(new Callable<RpcResult<UpdateEntryOutput>>() {
            @SuppressWarnings("unchecked")
            @Override
            public RpcResult<UpdateEntryOutput> call() throws Exception {
                LOG.info("RpcUpdateEntry event | {}", input.toString());

                IpPrefix originalIpPrefix = input.getOriginalBinding().getIpPrefix();
                if (originalIpPrefix == null) {
                    LOG.warn("RpcUpdateEntry exception | Parameter 'original-binding/ip-prefix' not defined");
                    UpdateEntryOutputBuilder output = new UpdateEntryOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<UpdateEntryOutput>) output).build();
                }
                Sgt originalSgt = input.getOriginalBinding().getSgt();
                if (originalSgt == null) {
                    LOG.warn("RpcUpdateEntry exception | Parameter 'original-binding/sgt' not defined");
                    UpdateEntryOutputBuilder output = new UpdateEntryOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<UpdateEntryOutput>) output).build();
                }
                IpPrefix newIpPrefix = input.getNewBinding().getIpPrefix();
                if (newIpPrefix == null) {
                    LOG.warn("RpcUpdateEntry exception | Parameter 'new-binding/ip-prefix' not defined");
                    UpdateEntryOutputBuilder output = new UpdateEntryOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<UpdateEntryOutput>) output).build();
                }
                Sgt newSgt = input.getNewBinding().getSgt();
                if (newSgt == null) {
                    LOG.warn("RpcUpdateEntry exception | Parameter 'new-binding/sgt' not defined");
                    UpdateEntryOutputBuilder output = new UpdateEntryOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<UpdateEntryOutput>) output).build();
                }
                String nodeId = getNodeId(input.getRequestedNode());

                if (!isBindingPresent(nodeId, originalSgt, originalIpPrefix)) {
                    LOG.warn("RpcAddEntry exception | Original binding doesn't exist '{} {}'", originalSgt.getValue(),
                            IpPrefixConv.toString(originalIpPrefix));
                    UpdateEntryOutputBuilder output = new UpdateEntryOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<UpdateEntryOutput>) output).build();
                } else if (isBindingPresent(nodeId, newSgt, newIpPrefix)) {
                    LOG.warn("RpcAddEntry exception | New binding already exists '{} {}'", newSgt.getValue(),
                            IpPrefixConv.toString(newIpPrefix));
                    UpdateEntryOutputBuilder output = new UpdateEntryOutputBuilder();
                    output.setResult(false);
                    return RpcResultBuilder.from((RpcResult<UpdateEntryOutput>) output).build();
                }

                // Remove original.
                BindingBuilder bindingBuilder = new BindingBuilder();
                bindingBuilder.setIpPrefix(originalIpPrefix);

                List<Binding> bindings = new ArrayList<>();
                bindings.add(bindingBuilder.build());

                List<PrefixGroup> prefixGroups = new ArrayList<>();
                PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder();
                prefixGroupBuilder.setSgt(originalSgt);
                prefixGroupBuilder.setBinding(bindings);
                prefixGroups.add(prefixGroupBuilder.build());

                getDatastoreProviderMaster(nodeId).setAsDeleted(prefixGroups);
                notifyService(nodeId);

                // Add new.
                bindingBuilder = new BindingBuilder();
                bindingBuilder.setIpPrefix(newIpPrefix);

                DateAndTime timestamp = TimeConv.toDt(System.currentTimeMillis());

                List<NodeId> nodeIds = new ArrayList<>();
                nodeIds.add(Configuration.getRegisteredNode(nodeId).getNodeId());

                bindingBuilder.setAction(DatabaseAction.Add);
                bindingBuilder.setPeerSequence(NodeIdConv.createPeerSequence(nodeIds));
                bindingBuilder.setSources(NodeIdConv.createSources(nodeIds));
                bindingBuilder.setTimestamp(timestamp);
                bindingBuilder.setChanged(true);

                bindings.clear();
                bindings.add(bindingBuilder.build());

                prefixGroups.clear();
                prefixGroupBuilder = new PrefixGroupBuilder();
                prefixGroupBuilder.setSgt(newSgt);
                prefixGroupBuilder.setBinding(bindings);
                prefixGroups.add(prefixGroupBuilder.build());

                getDatastoreProviderMaster(nodeId).addBindingsLocal(prefixGroups);
                notifyService(nodeId);

                UpdateEntryOutputBuilder output = new UpdateEntryOutputBuilder();
                output.setResult(true);
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }
}
