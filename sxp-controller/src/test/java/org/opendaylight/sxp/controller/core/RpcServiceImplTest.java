/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddBindingsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddBindingsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddEntryInputBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteEntryInputBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateEntryInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.update.entry.input.NewBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.update.entry.input.NewBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.update.entry.input.OriginalBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.update.entry.input.OriginalBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.BindingBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({MasterDatastoreImpl.class, DatastoreAccess.class, SxpNode.class})
public class RpcServiceImplTest {

    private static SxpNode node;
    private static RpcServiceImpl service;
    private static DatastoreAccess datastoreAccess;
    private MasterDatabaseInf masterDatabase;

    @Before public void init() throws ExecutionException, InterruptedException {
        node = PowerMockito.mock(SxpNode.class);
        datastoreAccess = mock(DatastoreAccess.class);
        when(datastoreAccess.checkAndDelete(any(InstanceIdentifier.class), any(LogicalDatastoreType.class))).thenReturn(
                true);
        when(datastoreAccess.checkAndPut(any(InstanceIdentifier.class), any(DataObject.class),
                any(LogicalDatastoreType.class), anyBoolean())).thenReturn(true);
        when(datastoreAccess.checkAndMerge(any(InstanceIdentifier.class), any(DataObject.class),
                any(LogicalDatastoreType.class), anyBoolean())).thenReturn(true);

        when(node.getNodeId()).thenReturn(NodeId.getDefaultInstance("0.0.0.0"));
        ArrayList<SxpPeerGroup> sxpPeerGroups = new ArrayList<>();
        sxpPeerGroups.add(mock(SxpPeerGroup.class));
        when(node.getPeerGroups()).thenReturn(sxpPeerGroups);
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
        Configuration.register(node);
        service = new RpcServiceImpl(datastoreAccess);
        masterDatabase = mock(MasterDatastoreImpl.class);
        when(node.getBindingMasterDatabase()).thenReturn(masterDatabase);
    }

    private MasterDatabaseBinding getBinding(String prefix, int sgt, String... peers) {
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        bindingBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
        bindingBuilder.setSecurityGroupTag(new Sgt(sgt));
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

    private org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.Binding getBinding(
            String prefix, String sgt) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.BindingBuilder
                bindingBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.BindingBuilder();

        bindingBuilder.setSgt(Sgt.getDefaultInstance(sgt));
        List<IpPrefix> ipPrefixes = new ArrayList<>();
        bindingBuilder.setIpPrefix(ipPrefixes);
        bindingBuilder.setKey(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.configuration.fields.BindingKey(
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
        connection.setPeerAddress(ip != null ? new IpAddress(ip.toCharArray()) : null);
        return connection.build();
    }

    @Test public void testAddEntry() throws Exception {
        when(node.putLocalBindingsMasterDatabase(anyList(), anyString())).thenReturn(
                Collections.singletonList(mock(MasterDatabaseBinding.class)));
        AddEntryInputBuilder input = new AddEntryInputBuilder();
        input.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));

        input.setSgt(new Sgt(20));
        input.setIpPrefix(new IpPrefix(Ipv4Prefix.getDefaultInstance("2.2.2.2/32")));
        assertTrue(service.addEntry(input.build()).get().getResult().isResult());

        input.setSgt(null);
        assertFalse(service.addEntry(input.build()).get().getResult().isResult());

        input.setSgt(new Sgt(20));
        input.setIpPrefix(null);
        assertFalse(service.addEntry(input.build()).get().getResult().isResult());
    }

    @Test public void testDeleteEntry() throws Exception {
        List<MasterDatabaseBinding> deletedBindings = new ArrayList<>();
        deletedBindings.add(getBinding("0.0.0.5/32", 20));

        DeleteEntryInputBuilder input = new DeleteEntryInputBuilder();
        input.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));

        input.setSgt(new Sgt(20));
        assertFalse(service.deleteEntry(input.build()).get().getResult().isResult());

        List<IpPrefix> ipPrefixes = new ArrayList<>();
        input.setIpPrefix(ipPrefixes);
        ipPrefixes.add(new IpPrefix(Ipv4Prefix.getDefaultInstance("0.0.0.5/32")));
        when(node.removeLocalBindingsMasterDatabase(anyList(), anyString())).thenReturn(deletedBindings);
        assertTrue(service.deleteEntry(input.build()).get().getResult().isResult());

        input.setSgt(null);
        assertFalse(service.deleteEntry(input.build()).get().getResult().isResult());
    }

    private OriginalBinding getOriginalBinding(String s, Integer i) {
        OriginalBindingBuilder builder = new OriginalBindingBuilder();
        builder.setSgt(i == null ? null : new Sgt(i));
        builder.setIpPrefix(s == null ? null : new IpPrefix(Ipv4Prefix.getDefaultInstance(s)));
        return builder.build();
    }

    private NewBinding getNewBinding(String s, Integer i) {
        NewBindingBuilder builder = new NewBindingBuilder();
        builder.setSgt(i == null ? null : new Sgt(i));
        builder.setIpPrefix(s == null ? null : new IpPrefix(Ipv4Prefix.getDefaultInstance(s)));
        return builder.build();
    }

    @Test public void testAddConnection() throws Exception {
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

    @Test public void testAddFilter() throws Exception {
        RpcResult<AddFilterOutput>
                result =
                service.addFilter(new AddFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.1"))
                        .setPeerGroupName("Group")
                        .setSxpFilter(new SxpFilterBuilder().build())
                        .build()).get();

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addFilter(new AddFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName("Group")
                        .setSxpFilter(new SxpFilterBuilder().build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());
        result =
                service.addFilter(new AddFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName("Group")
                        .setSxpFilter(
                                new SxpFilterBuilder().setFilterEntries(new AclFilterEntriesBuilder().build()).build())
                        .build()).get();

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test public void testAddPeerGroup() throws Exception {
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

    @Test public void testDeleteNode() throws Exception {
        RpcResult<DeleteNodeOutput> result = service.deleteNode(new DeleteNodeInputBuilder().build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        Configuration.unRegister(NodeIdConv.toString(node.getNodeId()));
        result = service.deleteNode(new DeleteNodeInputBuilder().setNodeId(new NodeId("0.0.0.0")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test public void testDeleteConnection() throws Exception {
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

    @Test public void testDeleteFilter() throws Exception {
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
                        .setPeerGroupName("GOURP")
                        .setFilterType(FilterType.Inbound)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteFilter(new DeleteFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName("GOURP")
                        .setFilterType(FilterType.Inbound)
                        .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test public void testDeletePeerGroup() throws Exception {
        RpcResult<DeletePeerGroupOutput>
                result =
                service.deletePeerGroup(new DeletePeerGroupInputBuilder().setRequestedNode(new NodeId("0.0.0.1"))
                        .setPeerGroupName("Group")
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deletePeerGroup(new DeletePeerGroupInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName("Group")
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test public void testGetConnections() throws Exception {
        RpcResult<GetConnectionsOutput> result = service.getConnections(new GetConnectionsInputBuilder().build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getConnections());

        result =
                service.getConnections(new GetConnectionsInputBuilder().setRequestedNode(new NodeId("0.0.0.0")).build())
                        .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getConnections());
    }

    @Test public void testGetNodeBindings() throws Exception {
        RpcResult<GetNodeBindingsOutput>
                result =
                service.getNodeBindings(new GetNodeBindingsInputBuilder().setRequestedNode(new NodeId("0.0.0.1"))
                        .setBindingsRange(GetNodeBindingsInput.BindingsRange.All)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getBinding());

        result =
                service.getNodeBindings(new GetNodeBindingsInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setBindingsRange(GetNodeBindingsInput.BindingsRange.All)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getBinding());

        result =
                service.getNodeBindings(new GetNodeBindingsInputBuilder().setRequestedNode(new NodeId("0.0.0.1"))
                        .setBindingsRange(GetNodeBindingsInput.BindingsRange.Local)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getBinding());

        result =
                service.getNodeBindings(new GetNodeBindingsInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setBindingsRange(GetNodeBindingsInput.BindingsRange.Local)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getBinding());
    }

    @Test public void testGetPeerGroup() throws Exception {
        RpcResult<GetPeerGroupOutput>
                result =
                service.getPeerGroup(new GetPeerGroupInputBuilder().setRequestedNode(new NodeId("0.0.0.1")).build())
                        .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNull(result.getResult().getSxpPeerGroup());

        result =
                service.getPeerGroup(new GetPeerGroupInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName("GROUP")
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNull(result.getResult().getSxpPeerGroup());
    }

    @Test public void testGetPeerGroups() throws Exception {
        RpcResult<GetPeerGroupsOutput>
                result =
                service.getPeerGroups(new GetPeerGroupsInputBuilder().setRequestedNode(new NodeId("0.0.0.1")).build())
                        .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getSxpPeerGroup());

        result =
                service.getPeerGroups(new GetPeerGroupsInputBuilder().setRequestedNode(new NodeId("0.0.0.1")).build())
                        .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getSxpPeerGroup());
    }

    @Test public void testAddNode() throws Exception {
        RpcResult<AddNodeOutput> result = service.addNode(new AddNodeInputBuilder().build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result = service.addNode(new AddNodeInputBuilder().setNodeId(node.getNodeId()).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test public void testUpdateEntry() throws Exception {
        List<MasterDatabaseBinding> bindings = new ArrayList<>();
        bindings.add(getBinding("0.0.0.5/32", 20));

        UpdateEntryInputBuilder input = new UpdateEntryInputBuilder();
        input.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));

        input.setNewBinding(getNewBinding("1.1.10.1/32", 50));
        input.setOriginalBinding(getOriginalBinding("1.1.1.1/32", 450));

        assertFalse(service.updateEntry(input.build()).get().getResult().isResult());
        when(node.putLocalBindingsMasterDatabase(anyList(), anyString())).thenReturn(bindings);
        assertTrue(service.updateEntry(input.build()).get().getResult().isResult());
        when(node.putLocalBindingsMasterDatabase(anyList(), anyString())).thenReturn(new ArrayList<>());

        assertFalse(service.updateEntry(input.build()).get().getResult().isResult());
        when(node.putLocalBindingsMasterDatabase(anyList(), anyString())).thenReturn(bindings);

        input.setNewBinding(getNewBinding("1.1.10.1/32", null));
        input.setOriginalBinding(getOriginalBinding("1.1.1.1/32", 450));
        assertFalse(service.updateEntry(input.build()).get().getResult().isResult());

        input.setNewBinding(getNewBinding(null, 50));
        input.setOriginalBinding(getOriginalBinding("1.1.1.1/32", 450));
        assertFalse(service.updateEntry(input.build()).get().getResult().isResult());

        input.setNewBinding(getNewBinding("1.1.10.1/32", 50));
        input.setOriginalBinding(getOriginalBinding("1.1.1.1/32", null));
        assertFalse(service.updateEntry(input.build()).get().getResult().isResult());

        input.setNewBinding(getNewBinding("1.1.10.1/32", 50));
        input.setOriginalBinding(getOriginalBinding(null, 450));
        assertFalse(service.updateEntry(input.build()).get().getResult().isResult());
    }

    @Test public void testUpdateFilter() throws Exception {
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
                        .setPeerGroupName("GROUP")
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.updateFilter(new UpdateFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setPeerGroupName("GROUP")
                        .setSxpFilter(new SxpFilterBuilder().setFilterType(FilterType.InboundDiscarding)
                                .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                                .build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test public void testDeleteBindings() throws Exception {
        when(node.removeLocalBindingsMasterDatabase(anyList(), anyString())).thenReturn(
                Collections.singletonList(mock(MasterDatabaseBinding.class)));
        RpcResult<DeleteBindingsOutput>
                result =
                service.deleteBindings(new DeleteBindingsInputBuilder().setNodeId(new NodeId("0.0.0.1")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteBindings(
                        new DeleteBindingsInputBuilder().setNodeId(new NodeId("0.0.0.0")).setBinding(null).build())
                        .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteBindings(new DeleteBindingsInputBuilder().setNodeId(new NodeId("0.0.0.0"))
                        .setBinding(new ArrayList<>())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteBindings(new DeleteBindingsInputBuilder().setNodeId(new NodeId("0.0.0.0"))
                        .setBinding(Collections.singletonList(new BindingBuilder().setSgt(new Sgt(112))
                                .setIpPrefix(Collections.singletonList(new IpPrefix("1.1.1.1/32".toCharArray())))
                                .build()))
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        Boolean aBoolean = result.getResult().isResult();
        assertTrue(aBoolean);
    }

    @Test public void testDeleteDomain() throws Exception {
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
                service.deleteDomain(
                        new DeleteDomainInputBuilder().setNodeId(new NodeId("0.0.0.0")).setDomainName("dom").build())
                        .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test public void testAddDomain() throws Exception {
        RpcResult<AddDomainOutput>
                result =
                service.addDomain(new AddDomainInputBuilder().setNodeId(new NodeId("0.0.0.1")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addDomain(
                        new AddDomainInputBuilder().setNodeId(new NodeId("0.0.0.0")).setDomainName(null).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addDomain(
                        new AddDomainInputBuilder().setNodeId(new NodeId("0.0.0.0")).setDomainName("dom").build())
                        .get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test public void testAddBindings() throws Exception {
        when(node.putLocalBindingsMasterDatabase(anyList(), anyString())).thenReturn(
                Collections.singletonList(mock(MasterDatabaseBinding.class)));
        RpcResult<AddBindingsOutput>
                result =
                service.addBindings(new AddBindingsInputBuilder().setNodeId(new NodeId("0.0.0.1")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addBindings(
                        new AddBindingsInputBuilder().setNodeId(new NodeId("0.0.0.0")).setBinding(null).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addBindings(new AddBindingsInputBuilder().setNodeId(new NodeId("0.0.0.0"))
                        .setBinding(new ArrayList<>())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addBindings(new AddBindingsInputBuilder().setNodeId(new NodeId("0.0.0.0"))
                        .setBinding(Collections.singletonList(new BindingBuilder().setSgt(new Sgt(112))
                                .setIpPrefix(Collections.singletonList(new IpPrefix("1.1.1.1/32".toCharArray())))
                                .build()))
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        Boolean aBoolean = result.getResult().isResult();
        assertTrue(aBoolean);
    }

    @Test public void testDeleteDomainFilter() throws Exception {
        RpcResult<DeleteDomainFilterOutput>
                result =
                service.deleteDomainFilter(
                        new DeleteDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.1")).build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteDomainFilter(
                        new DeleteDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                                .setDomainName(null)
                                .setFilterName(null)
                                .setFilterSpecific(null)
                                .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteDomainFilter(
                        new DeleteDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                                .setDomainName("dom")
                                .setFilterName(null)
                                .setFilterSpecific(null)
                                .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteDomainFilter(
                        new DeleteDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                                .setDomainName("dom")
                                .setFilterName("basic-filter")
                                .setFilterSpecific(null)
                                .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.deleteDomainFilter(
                        new DeleteDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                                .setDomainName("dom")
                                .setFilterName("basic-filter")
                                .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                                .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test public void testAddDomainFilter() throws Exception {
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
                        .setDomainName("dom")
                        .setSxpDomainFilter(null)
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertFalse(result.getResult().isResult());

        result =
                service.addDomainFilter(new AddDomainFilterInputBuilder().setRequestedNode(new NodeId("0.0.0.0"))
                        .setDomainName("dom")
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
                        .setDomainName("dom")
                        .setSxpDomainFilter(new SxpDomainFilterBuilder().setFilterName("basic-filter")
                                .setFilterEntries(new AclFilterEntriesBuilder().build())
                                .build())
                        .build()).get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isResult());
    }

    @Test public void testClose() throws Exception {
        service.close();
        verify(node).shutdown();
        verify(datastoreAccess).close();
    }
}
