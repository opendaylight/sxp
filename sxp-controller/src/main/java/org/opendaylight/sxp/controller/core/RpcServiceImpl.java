/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.AbstractFuture;
import org.opendaylight.sxp.controller.util.database.access.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupOutputBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetBindingSgtsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetBindingSgtsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetBindingSgtsOutputBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.PrefixListEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.PrefixListFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.acl.filter.entries.AclEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RpcServiceImpl implements SxpControllerService, AutoCloseable {

    //TODO
    private static DatastoreAccess datastoreAccess;

    private static final Logger LOG = LoggerFactory.getLogger(RpcServiceImpl.class.getName());


    protected synchronized static org.opendaylight.sxp.util.database.spi.MasterDatabase getDatastoreProviderMaster(String nodeId) {
        return Configuration.getRegisteredNode(nodeId).getBindingMasterDatabase();
    }

    protected synchronized static org.opendaylight.sxp.util.database.spi.SxpDatabase getDatastoreProviderSxp(String nodeId) {
        return Configuration.getRegisteredNode(nodeId).getBindingSxpDatabase();
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

    public RpcServiceImpl(DatastoreAccess datastoreAccess) {
        RpcServiceImpl.datastoreAccess = datastoreAccess;
    }

    private <T> Future<RpcResult<T>> getResponse(String nodeId, final T response, Callable<RpcResult<T>> resultCallable) {
        SxpNode node = Configuration.getRegisteredNode(nodeId);
        if (nodeId == null || node == null) {
            return new AbstractFuture<RpcResult<T>>() {
                @Override
                public RpcResult<T> get() throws InterruptedException, ExecutionException {
                    return RpcResultBuilder.success(response).build();
                }
            };
        } else {
            return node.getWorker().executeTaskInSequence(resultCallable, ThreadsWorker.WorkerType.DEFAULT);
        }
    }

    @Override
    public Future<RpcResult<AddConnectionOutput>> addConnection(final AddConnectionInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final AddConnectionOutputBuilder output = new AddConnectionOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), new Callable<RpcResult<AddConnectionOutput>>() {

            @Override
            public RpcResult<AddConnectionOutput> call() throws Exception {
                LOG.info("RpcAddConnection event | {}", input.toString());

                Connections connections = input.getConnections();
                if (connections == null || connections.getConnection() == null || connections.getConnection()
                        .isEmpty()) {
                    LOG.warn("RpcAddConnection exception | Parameter 'connections' not defined");
                    return RpcResultBuilder.success(output.build()).build();
                }


                List<Connection> _connections = new ArrayList<>();
                for (Connection connection : connections.getConnection()) {
                    IpAddress peerAddress = connection.getPeerAddress();
                    if (peerAddress == null || peerAddress.getValue() == null || peerAddress.getValue().length == 0) {
                        LOG.warn("RpcAddConnection exception | Connection parameter 'peer-address' not defined");
                        continue;
                    }
                    if (connection.getTcpPort().getValue() <= 0) {
                        LOG.warn("RpcDeleteConnection exception | Parameter 'tcp-port' <= 0");
                        return RpcResultBuilder.success(output.build()).build();
                    }
                    _connections.add(connection);
                }
                if (_connections.isEmpty()) {
                    LOG.warn("RpcAddConnection exception | No new connections");
                    return RpcResultBuilder.success(output.build()).build();
                }

                ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder();
                connectionsBuilder.setConnection(_connections);

                Configuration.getRegisteredNode(nodeId).addConnections(connectionsBuilder.build());
                output.setResult(true);
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override
    public Future<RpcResult<AddEntryOutput>> addEntry(final AddEntryInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final AddEntryOutputBuilder output = new AddEntryOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), new Callable<RpcResult<AddEntryOutput>>() {
            @SuppressWarnings("unchecked")
            @Override
            public RpcResult<AddEntryOutput> call() throws Exception {
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
                bindingBuilder
                    .setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());
                bindings.add(bindingBuilder.build());

                BindingDispatcher
                    .propagateUpdate(null, getDatastoreProviderMaster(nodeId).addLocalBindings(bindings),
                        Configuration.getRegisteredNode(nodeId).getAllOnSpeakerConnections());

                output.setResult(true);
                return RpcResultBuilder.success(output.build()).build();
            }

        });
    }

    @Override public Future<RpcResult<AddFilterOutput>> addFilter(final AddFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final AddFilterOutputBuilder output = new AddFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), new Callable<RpcResult<AddFilterOutput>>() {

            @Override
            public RpcResult<AddFilterOutput> call() throws Exception {
                String msg = "RpcAddFilter";
                LOG.info("{} event | {}", msg, input.toString());

                if (input.getRequestedNode() == null) {
                    LOG.warn("{} exception | Parameter 'requested-node' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                SxpNode node = Configuration.getRegisteredNode(nodeId);
                if (node == null) {
                    LOG.warn("{} exception | SxpNode '{}' doesn't exists", msg, input.getRequestedNode());
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (input.getPeerGroupName() == null) {
                    LOG.warn("{} exception | Parameter 'group-name' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (input.getSxpFilter() == null) {
                    LOG.warn("{} exception | Parameter 'sxp-filter' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (checkFilterFields(new SxpFilterBuilder(input.getSxpFilter()).build(), msg + " exception |")) {
                    return RpcResultBuilder.success(output.build()).build();
                }

                output.setResult(node.addFilterToPeerGroup(input.getPeerGroupName(),
                        new SxpFilterBuilder(input.getSxpFilter()).build()));
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    /**
     * TODO: can be removed after yang generation is fully supported
     * Verify if fields of Filter are correctly set
     *
     * @param sxpFilter Filter to be checked
     * @param msg       Additional error message that will be logged
     * @return If Filter has inappropriate fields
     */
    private static boolean checkFilterFields(SxpFilter sxpFilter, String msg) {
        if (sxpFilter.getFilterType() == null) {
            LOG.warn("{} Parameter 'filter-type' not defined", msg);
            return true;
        }
        if (sxpFilter.getFilterEntries() == null) {
            LOG.warn("{} Parameter 'filter-entries' not defined", msg);
            return true;
        }
        if (sxpFilter.getFilterEntries() instanceof AclFilterEntries) {
            AclFilterEntries entries = (AclFilterEntries) sxpFilter.getFilterEntries();
            if (entries.getAclEntry() == null || entries.getAclEntry().isEmpty()) {
                LOG.warn("{} In parameter 'filter-entries' no entries was defined", msg);
                return true;
            }
            for (AclEntry aclEntry : entries.getAclEntry()) {
                if (aclEntry.getEntryType() == null) {
                    LOG.warn("{} In parameter 'entry-type' no entries was defined", msg);
                    return true;
                }
                if (aclEntry.getEntrySeq() == null) {
                    LOG.warn("{} In parameter 'entry-seq' no entries was defined", msg);
                    return true;
                }
                if (aclEntry.getAclMatch() == null && aclEntry.getSgtMatch() == null) {
                    LOG.warn("{} At least one of sgt-match-field or acl-match must be used", msg);
                    return true;
                }
            }
        } else if (sxpFilter.getFilterEntries() instanceof PrefixListFilterEntries) {
            PrefixListFilterEntries entries = (PrefixListFilterEntries) sxpFilter.getFilterEntries();
            if (entries.getPrefixListEntry() == null || entries.getPrefixListEntry().isEmpty()) {
                LOG.warn("{} In parameter 'filter-entries' no entries was defined", msg);
                return true;
            }
            for (PrefixListEntry prefixListEntry : entries.getPrefixListEntry()) {
                if (prefixListEntry.getEntryType() == null) {
                    LOG.warn("{} In parameter 'entry-type' no entries was defined", msg);
                    return true;
                }
                if (prefixListEntry.getEntrySeq() == null) {
                    LOG.warn("{} In parameter 'entry-seq' no entries was defined", msg);
                    return true;
                }
                if (prefixListEntry.getPrefixListMatch() == null && prefixListEntry.getSgtMatch() == null) {
                    LOG.warn("{} At least one of sgt-match-field or prefix-list-match must be used", msg);
                    return true;
                }
            }
        }
        return false;
    }

    @Override public Future<RpcResult<AddPeerGroupOutput>> addPeerGroup(final AddPeerGroupInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final AddPeerGroupOutputBuilder output = new AddPeerGroupOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), new Callable<RpcResult<AddPeerGroupOutput>>() {

            @Override
            public RpcResult<AddPeerGroupOutput> call() throws Exception {
                String msg = "RpcAddPerGroup";
                LOG.info("{} event | {}", msg, input.toString());

                if (input.getRequestedNode() == null) {
                    LOG.warn("{} exception | Parameter 'requested-node' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                SxpNode node = Configuration.getRegisteredNode(nodeId);
                if (node == null) {
                    LOG.warn("{} exception | SxpNode '{}' doesn't exists", msg, input.getRequestedNode());
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (input.getSxpPeerGroup() == null) {
                    LOG.warn("{} exception | Parameter 'sxp-peer-group' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (input.getSxpPeerGroup().getSxpPeers() == null) {
                    LOG.warn("{} exception | Parameter 'sxp-peers' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (input.getSxpPeerGroup().getName() == null) {
                    LOG.warn("{} exception | Parameter 'name' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (node.getPeerGroup(input.getSxpPeerGroup().getName()) != null) {
                    LOG.warn("{} exception | PeerGroup with name '{}' already defined", msg,
                            input.getSxpPeerGroup().getName());
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (input.getSxpPeerGroup().getSxpFilter() != null) {
                    for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter filter : input
                            .getSxpPeerGroup()
                            .getSxpFilter()) {
                        if (checkFilterFields(filter, msg + " exception |")) {
                            return RpcResultBuilder.success(output.build()).build();
                        }
                    }
                }
                output.setResult(node.addPeerGroup(input.getSxpPeerGroup()));
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override
    public void close() {
        for (SxpNode node : Configuration.getNodes().values()) {
            node.shutdown();
        }
    }

    @Override
    public Future<RpcResult<DeleteConnectionOutput>> deleteConnection(final DeleteConnectionInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DeleteConnectionOutputBuilder output = new DeleteConnectionOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), new Callable<RpcResult<DeleteConnectionOutput>>() {
            @SuppressWarnings("unchecked")
            @Override
            public RpcResult<DeleteConnectionOutput> call() throws Exception {
                LOG.info("RpcDeleteConnection event | {}", input.toString());

                Ipv4Address peerAddress = input.getPeerAddress();
                if (peerAddress == null || peerAddress.getValue() == null || peerAddress.getValue().isEmpty()) {
                    LOG.warn("RpcDeleteConnection exception | Parameter 'peer-address' not defined");
                    return RpcResultBuilder.success(output.build()).build();
                }

                PortNumber portNumber = input.getTcpPort();
                if (portNumber == null) {
                    portNumber = new PortNumber(Configuration.getConstants().getPort());
                    LOG.info("RpcDeleteConnection | Parameter 'tcp-port' default value used ['{}']", Configuration
                            .getConstants().getPort());
                }
                if (portNumber.getValue() <= 0) {
                    LOG.warn("RpcDeleteConnection exception | Parameter 'tcp-port' <= 0");
                    return RpcResultBuilder.success(output.build()).build();
                }
                InetSocketAddress destination = IpPrefixConv.parseInetPrefix(peerAddress.getValue());
                destination = new InetSocketAddress(destination.getAddress(), portNumber.getValue());


                SxpConnection connection = Configuration.getRegisteredNode(nodeId).removeConnection(destination);
                if (connection == null) {
                    LOG.warn("RpcDeleteConnection exception | Connection '{}' not exists", destination);
                    return RpcResultBuilder.success(output.build()).build();
                }
                output.setResult(true);
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override
    public Future<RpcResult<DeleteEntryOutput>> deleteEntry(final DeleteEntryInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DeleteEntryOutputBuilder output = new DeleteEntryOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), new Callable<RpcResult<DeleteEntryOutput>>() {

            @Override
            public RpcResult<DeleteEntryOutput> call() throws Exception {
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
                bindingBuilder
                    .setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());

                if (input.getIpPrefix() != null) {
                    for (IpPrefix ipPrefix : input.getIpPrefix()) {
                        bindingBuilder.setIpPrefix(ipPrefix).setTimestamp(timestamp);
                        bindings.add(bindingBuilder.build());
                    }
                }

                List<MasterDatabaseBinding> deletedBindings =
                    getDatastoreProviderMaster(nodeId).deleteBindingsLocal(bindings);
                List<SxpDatabaseBinding> replacedBindings =
                    getDatastoreProviderSxp(nodeId).getReplaceForBindings(deletedBindings);

                BindingDispatcher.propagateUpdate(deletedBindings,
                    getDatastoreProviderMaster(nodeId).addBindings(replacedBindings),
                    Configuration.getRegisteredNode(nodeId).getAllOnSpeakerConnections());

                DeleteEntryOutputBuilder output = new DeleteEntryOutputBuilder();
                output.setResult(!deletedBindings.isEmpty());
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override public Future<RpcResult<DeleteFilterOutput>> deleteFilter(final DeleteFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DeleteFilterOutputBuilder output = new DeleteFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), new Callable<RpcResult<DeleteFilterOutput>>() {

            @Override public RpcResult<DeleteFilterOutput> call() throws Exception {
                String msg = "RpcDeleteFilter";
                LOG.info("{} event | {}", msg, input.toString());

                if (input.getRequestedNode() == null) {
                    LOG.warn("{} exception | Parameter 'requested-node' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                SxpNode node = Configuration.getRegisteredNode(nodeId);
                if (node == null) {
                    LOG.warn("{} exception | SxpNode '{}' doesn't exists", msg, input.getRequestedNode());
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (input.getPeerGroupName() == null) {
                    LOG.warn("{} exception | Parameter 'group-name' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (input.getFilterType() == null) {
                    LOG.warn("{} exception | Parameter 'filter-type' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                output.setResult(
                        node.removeFilterFromPeerGroup(input.getPeerGroupName(), input.getFilterType()) != null);
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override public Future<RpcResult<DeletePeerGroupOutput>> deletePeerGroup(final DeletePeerGroupInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final DeletePeerGroupOutputBuilder output =
            new DeletePeerGroupOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(),
            new Callable<RpcResult<DeletePeerGroupOutput>>() {

            @Override public RpcResult<DeletePeerGroupOutput> call() throws Exception {
                String msg = "RpcDeletePeerGroup";
                LOG.info("{} event | {}", msg, input.toString());

                if (input.getRequestedNode() == null) {
                    LOG.warn("{} exception | Parameter 'requested-node' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                SxpNode node = Configuration.getRegisteredNode(nodeId);
                if (node == null) {
                    LOG.warn("{} exception | SxpNode '{}' doesn't exists", msg, input.getRequestedNode());
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (input.getPeerGroupName() == null) {
                    LOG.warn("{} exception | Parameter 'group-name' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                output.setResult(node.removePeerGroup(input.getPeerGroupName()) != null);
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override
    public Future<RpcResult<GetBindingSgtsOutput>> getBindingSgts(final GetBindingSgtsInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final GetBindingSgtsOutputBuilder output = new GetBindingSgtsOutputBuilder().setSgt(null);

        return getResponse(nodeId, output.build(), new Callable<RpcResult<GetBindingSgtsOutput>>() {

            @SuppressWarnings("unchecked")
            @Override
            public RpcResult<GetBindingSgtsOutput> call() throws Exception {
                LOG.info("RpcGetBindingSgts event | {}", input.toString());

                IpPrefix ipPrefix = input.getIpPrefix();
                if (ipPrefix == null) {
                    LOG.warn("RpcGetBindingSgts exception | Parameter 'ip-prefix' not defined");
                    return RpcResultBuilder.success(output.build()).build();
                }
                List<Sgt> sgts = new ArrayList<>();
                for (MasterDatabaseBinding bindingIdentity : getDatastoreProviderMaster(nodeId).getBindings()) {
                    if (IpPrefixConv.equalTo(bindingIdentity.getIpPrefix(), ipPrefix)) {
                        sgts.add(new Sgt(bindingIdentity.getSecurityGroupTag()));
                    }
                }
                output.setSgt(sgts);
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override
    public Future<RpcResult<GetConnectionsOutput>> getConnections(final GetConnectionsInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final ConnectionsBuilder connectionsBuilder = new ConnectionsBuilder();
        final GetConnectionsOutputBuilder output =
            new GetConnectionsOutputBuilder().setConnections(connectionsBuilder.build());

        return getResponse(nodeId, output.build(), new Callable<RpcResult<GetConnectionsOutput>>() {
            @Override
            public RpcResult<GetConnectionsOutput> call() throws Exception {
                LOG.info("RpcGetConnectionsStatus event | {}", input.toString());

                SxpNode node = Configuration.getRegisteredNode(nodeId);
                if (node == null) {
                    LOG.warn("RpcGetConnectionsStatus exception | Requested node '" + nodeId + "' not exist");
                    return RpcResultBuilder.success(output.build()).build();
                }

                List<Connection> connections = new ArrayList<>();
                for (SxpConnection connection : Configuration.getRegisteredNode(nodeId).getAllConnections()) {
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
        final String nodeId = getNodeId(input.getRequestedNode());
        final GetNodeBindingsOutputBuilder output = new GetNodeBindingsOutputBuilder().setBinding(
            new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.Binding>());


        return getResponse(nodeId, output.build(),
            new Callable<RpcResult<GetNodeBindingsOutput>>() {

             @Override public RpcResult<GetNodeBindingsOutput> call() throws Exception {
                LOG.info("RpcGetNodeBindings event | {}", input.toString());
                List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.Binding>
                    bindings = new ArrayList<>();

                NodeId requestedNodeId = input.getRequestedNode();
                if (requestedNodeId == null) {
                    LOG.warn("RpcGetBindingSgts exception | Parameter 'requested-node' not defined");
                    return RpcResultBuilder.success(output.build()).build();
                }

                Map<Sgt, List<IpPrefix>> sgtListMap = new HashMap<Sgt, List<IpPrefix>>();
                for (MasterDatabaseBinding binding : getDatastoreProviderMaster(
                    getNodeId(input.getRequestedNode())).getBindings()) {
                    if (sgtListMap.get(binding.getSecurityGroupTag()) == null) {
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

                GetNodeBindingsOutputBuilder output = new GetNodeBindingsOutputBuilder();
                output.setBinding(bindings);
                return RpcResultBuilder.success(output.build()).build();

            }
        });
    }

    @Override public Future<RpcResult<GetPeerGroupOutput>> getPeerGroup(final GetPeerGroupInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final GetPeerGroupOutputBuilder output = new GetPeerGroupOutputBuilder().setSxpPeerGroup(null);

        return getResponse(nodeId, output.build(), new Callable<RpcResult<GetPeerGroupOutput>>() {

            @Override public RpcResult<GetPeerGroupOutput> call() throws Exception {
                String msg = "RpcGetPeerGroup";
                LOG.info("{} {}", msg, input.toString());

                if (input.getRequestedNode() == null) {
                    LOG.warn("{} Parameter 'requested-node' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                SxpNode node = Configuration.getRegisteredNode(nodeId);
                if (node == null) {
                    LOG.warn("{} SxpNode '{}' doesn't exists", msg, input.getRequestedNode());
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (input.getPeerGroupName() == null) {
                    LOG.warn("{} Parameter 'group-name' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                output.setSxpPeerGroup(node.getPeerGroup(input.getPeerGroupName()));
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override public Future<RpcResult<GetPeerGroupsOutput>> getPeerGroups(final GetPeerGroupsInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final GetPeerGroupsOutputBuilder output = new GetPeerGroupsOutputBuilder().setSxpPeerGroup(new ArrayList<SxpPeerGroup>());

        return getResponse(nodeId, output.build(), new Callable<RpcResult<GetPeerGroupsOutput>>() {

            @Override public RpcResult<GetPeerGroupsOutput> call() throws Exception {
                String msg = "RpcGetPeerGroups";
                LOG.info("{} event | {}", msg, input.toString());

                if (input.getRequestedNode() == null) {
                    LOG.warn("{} exception | Parameter 'requested-node' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                SxpNode node = Configuration.getRegisteredNode(nodeId);
                if (node == null) {
                    LOG.warn("{} exception | SxpNode '{}' doesn't exists", msg, input.getRequestedNode());
                    return RpcResultBuilder.success(output.build()).build();
                }
                output.setSxpPeerGroup(new ArrayList<>(Collections2.transform(node.getPeerGroups(),
                    new Function<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup, SxpPeerGroup>() {

                        @Nullable @Override public SxpPeerGroup apply(
                            org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup peerGroup) {

                            return new SxpPeerGroupBuilder(peerGroup).build();
                        }
                    })));
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override
    public Future<RpcResult<UpdateEntryOutput>> updateEntry(final UpdateEntryInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final UpdateEntryOutputBuilder output = new UpdateEntryOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), new Callable<RpcResult<UpdateEntryOutput>>() {

            @Override
            public RpcResult<UpdateEntryOutput> call() throws Exception {
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

                //TODO
                DateAndTime timestamp = TimeConv.toDt(System.currentTimeMillis());
                List<MasterDatabaseBinding> deleteBindings = new ArrayList<>(), addedBindings = new ArrayList<>();
                MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
                bindingBuilder
                        .setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());

                bindingBuilder.setIpPrefix(originalIpPrefix).setSecurityGroupTag(originalSgt).setTimestamp(timestamp);
                deleteBindings.add(bindingBuilder.build());

                bindingBuilder.setIpPrefix(newIpPrefix).setSecurityGroupTag(newSgt).setTimestamp(timestamp);
                addedBindings.add(bindingBuilder.build());


                List<MasterDatabaseBinding> deletedBindings =
                        getDatastoreProviderMaster(nodeId).deleteBindingsLocal(deleteBindings);
                List<SxpDatabaseBinding> replacedBindings =
                        getDatastoreProviderSxp(nodeId).getReplaceForBindings(deletedBindings);

                addedBindings = getDatastoreProviderMaster(nodeId).addLocalBindings(addedBindings);
                addedBindings.addAll(getDatastoreProviderMaster(nodeId).addLocalBindings(replacedBindings));

                BindingDispatcher.propagateUpdate(deletedBindings, addedBindings,
                        Configuration.getRegisteredNode(nodeId).getAllOnSpeakerConnections());

                UpdateEntryOutputBuilder output = new UpdateEntryOutputBuilder();
                output.setResult(!addedBindings.isEmpty());
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    @Override public Future<RpcResult<UpdateFilterOutput>> updateFilter(final UpdateFilterInput input) {
        final String nodeId = getNodeId(input.getRequestedNode());
        final UpdateFilterOutputBuilder output = new UpdateFilterOutputBuilder().setResult(false);

        return getResponse(nodeId, output.build(), new Callable<RpcResult<UpdateFilterOutput>>() {

            @Override public RpcResult<UpdateFilterOutput> call() throws Exception {
                String msg = "RpcUpdateFilter";
                LOG.info("{} event | {}", msg, input.toString());

                if (input.getRequestedNode() == null) {
                    LOG.warn("{} exception | Parameter 'requested-node' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                SxpNode node = Configuration.getRegisteredNode(nodeId);
                if (node == null) {
                    LOG.warn("{} exception | SxpNode '{}' doesn't exists", msg, input.getRequestedNode());
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (input.getPeerGroupName() == null) {
                    LOG.warn("{} exception | Parameter 'group-name' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (input.getSxpFilter() == null) {
                    LOG.warn("{} exception | Parameter 'sxp-filter' not defined", msg);
                    return RpcResultBuilder.success(output.build()).build();
                }
                if (checkFilterFields(new SxpFilterBuilder(input.getSxpFilter()).build(), msg)) {
                    return RpcResultBuilder.success(output.build()).build();
                }

                output.setResult(node.updateFilterInPeerGroup(input.getPeerGroupName(),
                        new SxpFilterBuilder(input.getSxpFilter()).build()) != null);
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

}
