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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.BindingTransactionChain;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionChainListener;
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
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
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class SxpRpcServiceImplTest {

    @Mock
    private SxpNode node;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;

    private static SxpRpcServiceImpl service;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        final DatastoreAccess datastoreAccess = prepareDataStore(dataBroker, readTransaction, writeTransaction);
        final NodeId nodeId = NodeId.getDefaultInstance("0.0.0.0");
        final MasterDatabaseInf masterDatabase = new MasterDatastoreImpl(datastoreAccess, nodeId.getValue(), SxpNode.DEFAULT_DOMAIN);

        when(node.getNodeId()).thenReturn(nodeId);
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
        final org.opendaylight.sxp.core.SxpDomain domain = mock(org.opendaylight.sxp.core.SxpDomain.class);
        when(node.getDomain(eq(SxpNode.DEFAULT_DOMAIN))).thenReturn(domain);
        when(domain.getMasterDatabase()).thenReturn(masterDatabase);
        Configuration.register(node);
        service = new SxpRpcServiceImpl(dataBroker);
        when(node.getBindingMasterDatabase()).thenReturn(masterDatabase);
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

    /**
     * Prepare {@link DatastoreAccess} mock instance backed by {@link DataBroker} for tests.
     * <p>
     * {@link ReadTransaction} and {@link WriteTransaction} are assumed to be created by
     * {@link DatastoreAccess} {@link BindingTransactionChain}.
     * <p>
     * {@link ReadTransaction} reads an mock instance of {@link DataObject} on any read.
     * {@link WriteTransaction} is committed successfully.
     *
     * @param dataBroker mock of {@link DataBroker}
     * @param readTransaction mock of {@link ReadTransaction}
     * @param writeTransaction mock of {@link WriteTransaction}
     * @return mock of {@link DatastoreAccess}
     */
    private static DatastoreAccess prepareDataStore(DataBroker dataBroker, ReadTransaction readTransaction,
            WriteTransaction writeTransaction) {
        BindingTransactionChain transactionChain = mock(BindingTransactionChain.class);
        doReturn(CommitInfo.emptyFluentFuture())
                .when(writeTransaction).commit();
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(mock(DataObject.class))));
        when(transactionChain.newReadOnlyTransaction())
                .thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction())
                .thenReturn(writeTransaction);
        when(dataBroker.createTransactionChain(any(TransactionChainListener.class)))
                .thenReturn(transactionChain);

        return DatastoreAccess.getInstance(dataBroker);
    }
}
