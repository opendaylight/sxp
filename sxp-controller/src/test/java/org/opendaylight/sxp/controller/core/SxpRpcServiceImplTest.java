/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.sxp.controller.core.SxpDatastoreNode.getIdentifier;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddBindingsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddBindingsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionTemplateInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionTemplateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteBindingsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteBindingsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionTemplateInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionTemplateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteNodeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeletePeerGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeletePeerGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetConnectionsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetConnectionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetNodeBindingsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetNodeBindingsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetNodeBindingsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.fields.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.SxpDomainFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MasterDatastoreImpl.class, DatastoreAccess.class, SxpNode.class})
public class SxpRpcServiceImplTest {

    private static SxpNode node;
    private static SxpRpcServiceImpl service;
    private static DatastoreAccess datastoreAccess;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        node = PowerMockito.mock(SxpNode.class);
        datastoreAccess = mock(DatastoreAccess.class);
        final org.opendaylight.sxp.core.SxpDomain domain = mock(org.opendaylight.sxp.core.SxpDomain.class);
        final MasterDatabaseInf masterDatabase = mock(MasterDatastoreImpl.class);
        when(datastoreAccess.checkAndDelete(any(InstanceIdentifier.class), any(LogicalDatastoreType.class))).thenReturn(
                true);
        when(datastoreAccess.checkAndPut(any(InstanceIdentifier.class), any(DataObject.class),
                any(LogicalDatastoreType.class), anyBoolean())).thenReturn(true);
        when(datastoreAccess.putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                any(LogicalDatastoreType.class))).thenReturn(true);
        when(datastoreAccess.readSynchronous(eq(getIdentifier("0.0.0.0").child(SxpDomains.class)
                .child(SxpDomain.class, new SxpDomainKey(SxpNode.DEFAULT_DOMAIN))
                .child(MasterDatabase.class)), any(LogicalDatastoreType.class))).thenReturn(
                new MasterDatabaseBuilder().setMasterDatabaseBinding(new ArrayList<>()).build());
        when(datastoreAccess.readSynchronous(eq(getIdentifier("0.0.0.0")), any(LogicalDatastoreType.class))).thenReturn(
                mock(SxpNodeIdentity.class));
        when(datastoreAccess.merge(any(InstanceIdentifier.class), any(DataObject.class),
                any(LogicalDatastoreType.class))).thenReturn(Futures.immediateCheckedFuture(null));
        when(datastoreAccess.putSynchronous(eq(getIdentifier("0.0.0.1")), any(SxpNodeIdentity.class),
                eq(LogicalDatastoreType.CONFIGURATION))).thenAnswer(invocation -> {
            final SxpNode sxpNode = mock(SxpNode.class);
            when(sxpNode.getNodeId()).thenReturn(NodeId.getDefaultInstance("0.0.0.1"));
            Configuration.register(sxpNode);
            return true;
        });
        final org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup sxpPeerGroup =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupBuilder()
                        .setName(SxpNode.DEFAULT_DOMAIN)
                        .build();
        final SxpPeerGroups sxpPeerGroups = new SxpPeerGroupsBuilder()
                .setSxpPeerGroup(Collections.singletonList(sxpPeerGroup))
                .build();
        when(datastoreAccess.readSynchronous(eq(getIdentifier("0.0.0.0").child(SxpPeerGroups.class)), eq(LogicalDatastoreType.OPERATIONAL)))
                .thenReturn(sxpPeerGroups);
        when(datastoreAccess.readSynchronous(
                eq(getIdentifier("0.0.0.0")
                        .child(SxpPeerGroups.class)
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup.class,
                                new SxpPeerGroupKey(SxpNode.DEFAULT_DOMAIN))),
                eq(LogicalDatastoreType.OPERATIONAL))).thenReturn(sxpPeerGroup);
        PowerMockito.mockStatic(DatastoreAccess.class);
        PowerMockito.when(DatastoreAccess.getInstance(any(DataBroker.class))).thenReturn(datastoreAccess);
        when(node.getNodeId()).thenReturn(NodeId.getDefaultInstance("0.0.0.0"));
        when(node.getPeerGroups()).thenReturn(Collections.singletonList(mock(SxpPeerGroup.class)));
        when(node.getWorker()).thenReturn(new ThreadsWorker());
        when(node.getPeerGroup("TEST")).thenReturn(mock(SxpPeerGroup.class));
        when(node.removePeerGroup("TEST")).thenReturn(mock(SxpPeerGroup.class));
        when(node.removeFilterFromPeerGroup(anyString(), any(FilterType.class), any(FilterSpecific.class))).thenReturn(
                true);
        when(node.addPeerGroup(any(SxpPeerGroup.class))).thenReturn(true);
        when(node.addFilterToPeerGroup(anyString(),
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter.class)))
                .thenReturn(true);
        when(node.updateFilterInPeerGroup(anyString(),
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter.class)))
                .thenReturn(
                        mock(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter.class));
        when(node.getDomain(eq(SxpNode.DEFAULT_DOMAIN))).thenReturn(domain);
        when(domain.getMasterDatabase()).thenReturn(masterDatabase);
        Configuration.register(node);
        service = new SxpRpcServiceImpl(mock(DataBroker.class));
        when(node.getBindingMasterDatabase()).thenReturn(masterDatabase);

        final MasterDatabaseBinding localBinding = getBinding(
                "10.0.0.1/24", 1, BindingOriginsConfig.LOCAL_ORIGIN);
        final MasterDatabaseBinding networkBinding = getBinding(
                "10.0.0.2/24", 2, BindingOriginsConfig.NETWORK_ORIGIN);
        when(masterDatabase.getBindings(eq(BindingOriginsConfig.LOCAL_ORIGIN)))
                .thenReturn(Lists.newArrayList(localBinding));
        when(masterDatabase.getBindings()).thenReturn(Lists.newArrayList(localBinding, networkBinding));

        when(masterDatabase.addBindings(anyListOf(MasterDatabaseBinding.class))).thenAnswer(invocation -> {
            final List<MasterDatabaseBinding> input = (List<MasterDatabaseBinding>) invocation.getArguments()[0];
            if (input.isEmpty()) {
                return Collections.emptyList();
            } else {
                // assume all bindings were added
                return input;
            }
        });

        when(masterDatabase.deleteBindings(anyListOf(MasterDatabaseBinding.class))).thenAnswer(invocation -> {
            final List<MasterDatabaseBinding> input = (List<MasterDatabaseBinding>) invocation.getArguments()[0];
            if (input.isEmpty()) {
                return Collections.emptyList();
            } else {
                // assume all bindings were deleted
                return input;
            }
        });
    }

    private MasterDatabaseBinding getBinding(String prefix, int sgt, OriginType origin, String... peers) {
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        bindingBuilder.setIpPrefix(IpPrefixBuilder.getDefaultInstance(prefix));
        bindingBuilder.setSecurityGroupTag(new Sgt(sgt));
        bindingBuilder.setOrigin(origin);
        bindingBuilder.setTimestamp(TimeConv.toDt(System.currentTimeMillis()));
        PeerSequenceBuilder sequenceBuilder = new PeerSequenceBuilder();
        sequenceBuilder.setPeer(new ArrayList<>());
        for (int i = 0; i < peers.length; i++) {
            sequenceBuilder.getPeer()
                    .add(new PeerBuilder().setSeq(i).setNodeId(NodeId.getDefaultInstance(peers[i])).build());
        }
        bindingBuilder.setPeerSequence(sequenceBuilder.build());
        return bindingBuilder.build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.fields.Binding getBinding(
            String prefix, String sgt) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.fields.BindingBuilder
                bindingBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.fields.BindingBuilder();

        bindingBuilder.setSgt(Sgt.getDefaultInstance(sgt));
        List<IpPrefix> ipPrefixes = new ArrayList<>();
        bindingBuilder.setIpPrefix(ipPrefixes);
        bindingBuilder.withKey(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.fields.BindingKey(
                        bindingBuilder.getSgt()));
        if (prefix.contains(":")) {
            ipPrefixes.add(new IpPrefix(Ipv6Prefix.getDefaultInstance(prefix)));
        } else {
            ipPrefixes.add(new IpPrefix(Ipv4Prefix.getDefaultInstance(prefix)));
        }
        return bindingBuilder.build();
    }

    private Connection getConnection(String ip, Integer port) {
        ConnectionBuilder connection = new ConnectionBuilder();
        connection.setTcpPort(port != null ? new PortNumber(port) : null);
        connection.setPeerAddress(ip != null ? IpAddressBuilder.getDefaultInstance(ip) : null);
        return connection.build();
    }

    @Test
    public void testAddConnection() throws Exception {
        List<Connection> connections = new ArrayList<>();
        RpcResult<AddConnectionOutput>
                result =
                service.addConnection(new AddConnectionInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setConnections(new ConnectionsBuilder().setConnection(connections).build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        connections.add(getConnection("1.1.1.1", 64999));
        result =
                service.addConnection(new AddConnectionInputBuilder().setRequestedNode(new NodeId("0.0.0.1"))
                        .setConnections(new ConnectionsBuilder().setConnection(connections).build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addConnection(new AddConnectionInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setConnections(new ConnectionsBuilder().setConnection(connections).build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testAddFilter() throws Exception {
        RpcResult<AddFilterOutput>
                result =
                service.addFilter(new AddFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.1"))
                        .setPeerGroupName(SxpNode.DEFAULT_DOMAIN)
                        .setSxpFilter(new SxpFilterBuilder().build())
                        .build()).get();

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addFilter(new AddFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName(SxpNode.DEFAULT_DOMAIN)
                        .setSxpFilter(new SxpFilterBuilder().build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());
        result =
                service.addFilter(new AddFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName(SxpNode.DEFAULT_DOMAIN)
                        .setSxpFilter(
                                new SxpFilterBuilder().setFilterEntries(new AclFilterEntriesBuilder().build()).build())
                        .build()).get();

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testAddPeerGroup() throws Exception {
        RpcResult<AddPeerGroupOutput>
                result =
                service.addPeerGroup(new AddPeerGroupInputBuilder().setRequestedNode(new NodeId("0.0.0.1")).build())
                        .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addPeerGroup(new AddPeerGroupInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setSxpPeerGroup(new SxpPeerGroupBuilder().build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testDeleteNode() throws Exception {
        Configuration.unRegister(NodeIdConv.toString(node.getNodeId()));
        RpcResult<DeleteNodeOutput> result = service.deleteNode(
                new DeleteNodeInputBuilder().setNodeId(new NodeId("0.0.0.0")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testDeleteConnection() throws Exception {
        RpcResult<DeleteConnectionOutput>
                result =
                service.deleteConnection(new DeleteConnectionInputBuilder().setRequestedNode(new NodeId("0.0.0.1"))
                        .setPeerAddress(new Ipv4Address("1.1.1.1"))
                        .setTcpPort(new PortNumber(64999))
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteConnection(new DeleteConnectionInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerAddress(new Ipv4Address("1.1.1.1"))
                        .setTcpPort(new PortNumber(64999))
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testDeleteFilter() throws Exception {
        RpcResult<DeleteFilterOutput>
                result =
                service.deleteFilter(new DeleteFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.1")).build())
                        .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteFilter(new DeleteFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName(SxpNode.DEFAULT_DOMAIN)
                        .setFilterType(FilterType.Inbound)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteFilter(new DeleteFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName(SxpNode.DEFAULT_DOMAIN)
                        .setFilterType(FilterType.Inbound)
                        .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testDeletePeerGroup() throws Exception {
        RpcResult<DeletePeerGroupOutput>
                result =
                service.deletePeerGroup(new DeletePeerGroupInputBuilder().setRequestedNode(new NodeId("0.0.0.1"))
                        .setPeerGroupName(SxpNode.DEFAULT_DOMAIN)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deletePeerGroup(new DeletePeerGroupInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName(SxpNode.DEFAULT_DOMAIN)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testGetConnections() throws Exception {
        RpcResult<GetConnectionsOutput> result =
                service.getConnections(new GetConnectionsInputBuilder().setRequestedNode(new NodeId("0.0.0.0")).build())
                        .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getConnections());
    }

    @Test
    public void testGetNodeBindingsNotExistingDomain() throws Exception {
        final RpcResult<GetNodeBindingsOutput>
                result =
                service.getNodeBindings(new GetNodeBindingsInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setDomainName("guest")
                        .setBindingsRange(GetNodeBindingsInput.BindingsRange.All)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getBinding());
        assertTrue(result.getResult().getBinding().isEmpty());
    }

    @Test
    public void testGetEmptyNodeBindings() throws Exception {
        final RpcResult<GetNodeBindingsOutput>
                result =
                service.getNodeBindings(new GetNodeBindingsInputBuilder().setRequestedNode(new NodeId("0.0.0.1"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setBindingsRange(GetNodeBindingsInput.BindingsRange.All)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getBinding());
        assertTrue(result.getResult().getBinding().isEmpty());
    }

    @Test
    public void testGetNodeBindings() throws Exception {
        final RpcResult<GetNodeBindingsOutput>
                result =
                service.getNodeBindings(new GetNodeBindingsInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setBindingsRange(GetNodeBindingsInput.BindingsRange.All)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getBinding());
        assertFalse(result.getResult().getBinding().isEmpty());
    }

    @Test
    public void testGetEmptyLocalNodeBindings() throws Exception {
        final RpcResult<GetNodeBindingsOutput>
                result =
                service.getNodeBindings(new GetNodeBindingsInputBuilder().setRequestedNode(new NodeId("0.0.0.1"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setBindingsRange(GetNodeBindingsInput.BindingsRange.Local)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getBinding());
        assertTrue(result.getResult().getBinding().isEmpty());
    }

    @Test
    public void testGetLocalNodeBindings() throws Exception {
        final RpcResult<GetNodeBindingsOutput>
                result =
                service.getNodeBindings(new GetNodeBindingsInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setBindingsRange(GetNodeBindingsInput.BindingsRange.Local)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getBinding());
        assertFalse(result.getResult().getBinding().isEmpty());
    }

    @Test
    public void testGetPeerGroup() throws Exception {
        final RpcResult<GetPeerGroupOutput> result = service.getPeerGroup(
                new GetPeerGroupInputBuilder()
                        .setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName(SxpNode.DEFAULT_DOMAIN)
                        .build())
                .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getSxpPeerGroup());
    }

    @Test
    public void testGetPeerGroupNotExistingNode() throws Exception {
        final RpcResult<GetPeerGroupOutput> result = service.getPeerGroup(
                new GetPeerGroupInputBuilder()
                        .setRequestedNode(new NodeId("0.0.0.1"))
                        .setPeerGroupName(SxpNode.DEFAULT_DOMAIN)
                        .build())
                .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNull(result.getResult().getSxpPeerGroup());
    }

    @Test
    public void testGetPeerGroups() throws Exception {
        final RpcResult<GetPeerGroupsOutput> result = service.getPeerGroups(
                new GetPeerGroupsInputBuilder().setRequestedNode(new NodeId("0.0.0.0")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getSxpPeerGroup());
        assertFalse(result.getResult().getSxpPeerGroup().isEmpty());
    }

    @Test
    public void testGetPeerGroupsNotExistingNode() throws Exception {
        final RpcResult<GetPeerGroupsOutput> result = service.getPeerGroups(
                new GetPeerGroupsInputBuilder().setRequestedNode(new NodeId("0.0.0.1")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getSxpPeerGroup());
        assertTrue(result.getResult().getSxpPeerGroup().isEmpty());
    }

    @Test
    public void testAddNode() throws Exception {
        final RpcResult<AddNodeOutput> result = service.addNode(
                new AddNodeInputBuilder().setNodeId(NodeId.getDefaultInstance("0.0.0.1")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testAddNodeNullNodeId() throws Exception {
        final RpcResult<AddNodeOutput> result = service.addNode(
                new AddNodeInputBuilder().build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testUpdateFilter() throws Exception {
        RpcResult<UpdateFilterOutput>
                result =
                service.updateFilter(new UpdateFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.1")).build())
                        .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.updateFilter(new UpdateFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName(SxpNode.DEFAULT_DOMAIN)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.updateFilter(new UpdateFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName(SxpNode.DEFAULT_DOMAIN)
                        .setSxpFilter(new SxpFilterBuilder().setFilterType(FilterType.InboundDiscarding)
                                .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                                .build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testDeleteBindingsFromNonExistingNode() throws Exception {
        final RpcResult<DeleteBindingsOutput>
                result =
                service.deleteBindings(new DeleteBindingsInputBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setNodeId(new NodeId("0.0.0.1"))
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testDeleteNullBindings() throws Exception {
        final RpcResult<DeleteBindingsOutput>
                result =
                service.deleteBindings(new DeleteBindingsInputBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setNodeId(new NodeId("0.0.0.0"))
                        .setBinding(null)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testDeleteEmptyBindings() throws Exception {
        final RpcResult<DeleteBindingsOutput>
                result =
                service.deleteBindings(new DeleteBindingsInputBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setNodeId(new NodeId("0.0.0.0"))
                        .setBinding(new ArrayList<>())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testDeleteBindings() throws Exception {
        final RpcResult<DeleteBindingsOutput>
                result =
                service.deleteBindings(new DeleteBindingsInputBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setNodeId(new NodeId("0.0.0.0"))
                        .setBinding(Collections.singletonList(new BindingBuilder().setSgt(new Sgt(112))
                                .setIpPrefix(Collections.singletonList(IpPrefixBuilder.getDefaultInstance("1.1.1.1/32")))
                                .build()))
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        Boolean aBoolean = result.getResult().isResult();
        assertTrue(aBoolean);
    }

    @Test
    public void testDeleteDomain() throws Exception {
        RpcResult<DeleteDomainOutput>
                result =
                service.deleteDomain(new DeleteDomainInputBuilder().setNodeId(new NodeId("0.0.0.1")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteDomain(
                        new DeleteDomainInputBuilder().setNodeId(new NodeId("0.0.0.0")).setDomainName(null).build())
                        .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteDomain(new DeleteDomainInputBuilder().setNodeId(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testAddDomain() throws Exception {
        final RpcResult<AddDomainOutput> result = service
                .addDomain(new AddDomainInputBuilder()
                        .setNodeId(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testAddDomainNullDomainName() throws Exception {
        final RpcResult<AddDomainOutput> result = service.addDomain(
                new AddDomainInputBuilder()
                        .setNodeId(new NodeId("0.0.0.0"))
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testAddDomainWithBindings() throws Exception {
        final RpcResult<AddDomainOutput> result = service
                .addDomain(new AddDomainInputBuilder()
                        .setNodeId(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setMasterDatabase(
                                new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.MasterDatabaseBuilder()
                                        .setBinding(Lists.newArrayList(
                                                getBinding("1.1.1.1/32", "10"),
                                                getBinding("2.2.2.2/32", "20")))
                                        .build())
                        .setOrigin(BindingOriginsConfig.LOCAL_ORIGIN)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testAddDomainNullBindingsOrigin() throws Exception {
        final RpcResult<AddDomainOutput> result = service
                .addDomain(new AddDomainInputBuilder()
                        .setNodeId(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setMasterDatabase(
                                new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.MasterDatabaseBuilder()
                                        .setBinding(Lists.newArrayList(
                                                getBinding("1.1.1.1/32", "10"),
                                                getBinding("2.2.2.2/32", "20")))
                                        .build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testAddDomainToNonExistingNode() throws Exception {
        final RpcResult<AddDomainOutput> result = service.addDomain(
                new AddDomainInputBuilder()
                        .setNodeId(new NodeId("0.0.0.1"))
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testAddBindingsToNonExistingNode() throws Exception {
        final RpcResult<AddBindingsOutput> result =
                service.addBindings(new AddBindingsInputBuilder()
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setNodeId(new NodeId("0.0.0.1"))
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testAddNullBindings() throws Exception {
        final RpcResult<AddBindingsOutput> result =
                service.addBindings(new AddBindingsInputBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setNodeId(new NodeId("0.0.0.0"))
                        .setMasterDatabase(null)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testAddEmptyBindings() throws Exception {
        final RpcResult<AddBindingsOutput> result =
                service.addBindings(new AddBindingsInputBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setNodeId(new NodeId("0.0.0.0"))
                        .setMasterDatabase(
                                new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.MasterDatabaseBuilder()
                                        .setBinding(Collections.emptyList())
                                        .build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());
    }

    @Test
    public void testAddBindingsNullBindingsOrigin() throws Exception {
        final RpcResult<AddBindingsOutput> result =
                service.addBindings(new AddBindingsInputBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setNodeId(new NodeId("0.0.0.0"))
                        .setMasterDatabase(
                                new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.MasterDatabaseBuilder()
                                        .setBinding(Lists.newArrayList(
                                                getBinding("1.1.1.1/32", "10"),
                                                getBinding("2.2.2.2/32", "20")))
                                        .build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        Boolean aBoolean = result.getResult().isResult();
        assertFalse(aBoolean);
    }

    @Test
    public void testAddBindings() throws Exception {
        final RpcResult<AddBindingsOutput> result =
                service.addBindings(new AddBindingsInputBuilder().setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setNodeId(new NodeId("0.0.0.0"))
                        .setMasterDatabase(
                                new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.master.database.configuration.MasterDatabaseBuilder()
                                        .setBinding(Lists.newArrayList(
                                                getBinding("1.1.1.1/32", "10"),
                                                getBinding("2.2.2.2/32", "20")))
                                        .build())
                        .setOrigin(BindingOriginsConfig.LOCAL_ORIGIN)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        Boolean aBoolean = result.getResult().isResult();
        assertTrue(aBoolean);
    }

    @Test
    public void testDeleteDomainFilter() throws Exception {
        RpcResult<DeleteDomainFilterOutput>
                result =
                service.deleteDomainFilter(
                        new DeleteDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.1")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteDomainFilter(new DeleteDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setDomainName(null)
                        .setFilterName(null)
                        .setFilterSpecific(null)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteDomainFilter(new DeleteDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setFilterName(null)
                        .setFilterSpecific(null)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteDomainFilter(new DeleteDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setFilterName("basic-filter")
                        .setFilterSpecific(null)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteDomainFilter(new DeleteDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setFilterName("basic-filter")
                        .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testAddDomainFilter() throws Exception {
        RpcResult<AddDomainFilterOutput>
                result =
                service.addDomainFilter(
                        new AddDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.1")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addDomainFilter(new AddDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setDomainName(null)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addDomainFilter(new AddDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setSxpDomainFilter(null)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addDomainFilter(new AddDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setSxpDomainFilter(new SxpDomainFilterBuilder().setFilterName(null)
                                .setFilterEntries(new AclFilterEntriesBuilder().build())
                                .build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addDomainFilter(new AddDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setSxpDomainFilter(new SxpDomainFilterBuilder().setFilterName("basic-filter")
                                .setFilterEntries(new AclFilterEntriesBuilder().build())
                                .build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testClose() throws Exception {
        service.close();
        verify(datastoreAccess).close();
    }

    @Test
    public void testDeleteConnectionTemplate() throws Exception {
        RpcResult<DeleteConnectionTemplateOutput>
                result =
                service.deleteConnectionTemplate(
                        new DeleteConnectionTemplateInputBuilder().setNodeId(new NodeId("0.0.0.1")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteConnectionTemplate(
                        new DeleteConnectionTemplateInputBuilder().setNodeId(new NodeId("0.0.0.0")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteConnectionTemplate(
                        new DeleteConnectionTemplateInputBuilder().setNodeId(new NodeId("0.0.0.0"))
                                .setDomainName(SxpNode.DEFAULT_DOMAIN)
                                .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteConnectionTemplate(
                        new DeleteConnectionTemplateInputBuilder().setNodeId(new NodeId("0.0.0.0"))
                                .setTemplatePrefix(IpPrefixBuilder.getDefaultInstance("0.0.0.0/0"))
                                .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteConnectionTemplate(
                        new DeleteConnectionTemplateInputBuilder().setNodeId(new NodeId("0.0.0.0"))
                                .setTemplatePrefix(IpPrefixBuilder.getDefaultInstance("0.0.0.0/0"))
                                .setDomainName(SxpNode.DEFAULT_DOMAIN)
                                .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test
    public void testAddConnectionTemplate() throws Exception {
        RpcResult<AddConnectionTemplateOutput>
                result =
                service.addConnectionTemplate(
                        new AddConnectionTemplateInputBuilder().setNodeId(new NodeId("0.0.0.1")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addConnectionTemplate(
                        new AddConnectionTemplateInputBuilder().setNodeId(new NodeId("0.0.0.0")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addConnectionTemplate(new AddConnectionTemplateInputBuilder().setNodeId(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addConnectionTemplate(new AddConnectionTemplateInputBuilder().setNodeId(new NodeId("0.0.0.0"))
                        .setTemplatePrefix(IpPrefixBuilder.getDefaultInstance("0.0.0.0/0"))
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addConnectionTemplate(new AddConnectionTemplateInputBuilder().setNodeId(new NodeId("0.0.0.0"))
                        .setDomainName(SxpNode.DEFAULT_DOMAIN)
                        .setTemplatePrefix(IpPrefixBuilder.getDefaultInstance("0.0.0.0/0"))
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }
}
