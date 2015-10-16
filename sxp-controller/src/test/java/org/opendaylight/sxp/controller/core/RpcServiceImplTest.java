/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.core;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.controller.util.database.access.DatastoreAccess;
import org.opendaylight.sxp.controller.util.database.access.MasterDatabaseAccessImpl;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddEntryInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteEntryInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeletePeerGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetBindingSgtsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetConnectionsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetNodeBindingsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateEntryInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdatePeerGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.update.entry.input.NewBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.update.entry.input.NewBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.update.entry.input.OriginalBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.update.entry.input.OriginalBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sources.fields.SourcesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.entry.AclMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.acl.filter.entries.AclEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpPeers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({MasterDatastoreImpl.class, DatastoreAccess.class, SxpNode.class})
public class RpcServiceImplTest {

        private static SxpNode node;
        private static RpcServiceImpl service;
        private static DatastoreAccess datastoreAccess;
        private static CheckedFuture checkedFuture;

        @BeforeClass public static void initClass() throws Exception {
                node = PowerMockito.mock(SxpNode.class);
                when(node.getNodeId()).thenReturn(NodeId.getDefaultInstance("0.0.0.0"));
                ArrayList<SxpPeerGroup> sxpPeerGroups = new ArrayList<>();
                sxpPeerGroups.add(mock(SxpPeerGroup.class));
                when(node.getPeerGroups()).thenReturn(sxpPeerGroups);
                when(node.getPeerGroup("TEST")).thenReturn(mock(SxpPeerGroup.class));
                when(node.removePeerGroup("TEST")).thenReturn(mock(SxpPeerGroup.class));
                when(node.removeFilterFromPeerGroup(anyString(), any(FilterType.class))).thenReturn(
                        mock(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter.class));
                when(node.addPeerGroup(any(SxpPeerGroup.class))).thenReturn(true);
                when(node.addFilterToPeerGroup(anyString(),
                        any(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter.class)))
                        .thenReturn(true);
                when(node.updateFilterInPeerGroup(anyString(),
                        any(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter.class)))
                        .thenReturn(
                                mock(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter.class));
                Configuration.register(node);
        }

        @Before public void init() throws ExecutionException, InterruptedException {
                datastoreAccess = PowerMockito.mock(DatastoreAccess.class);

                checkedFuture = mock(CheckedFuture.class);
                when(checkedFuture.get()).thenReturn(null);
                when(datastoreAccess.read(any(InstanceIdentifier.class), any(LogicalDatastoreType.class))).thenReturn(
                        checkedFuture);
                when(datastoreAccess.put(any(InstanceIdentifier.class), any(DataObject.class),
                        any(LogicalDatastoreType.class))).thenReturn(checkedFuture);
                service = new RpcServiceImpl(datastoreAccess);
                when(node.getBindingMasterDatabase()).thenReturn(new MasterDatastoreImpl("0.0.0.0",
                        new MasterDatabaseAccessImpl("0.0.0.0", datastoreAccess, LogicalDatastoreType.OPERATIONAL)));
        }

        private Binding getBinding(String prefix) {
                BindingBuilder bindingBuilder = new BindingBuilder();
                List<NodeId> nodeIds = new ArrayList<>();
                if (prefix.contains(":")) {
                        bindingBuilder.setIpPrefix(new IpPrefix(Ipv6Prefix.getDefaultInstance(prefix)));
                } else {
                        bindingBuilder.setIpPrefix(new IpPrefix(Ipv4Prefix.getDefaultInstance(prefix)));
                        nodeIds.add(NodeId.getDefaultInstance(prefix.split("/")[0]));
                }
                bindingBuilder.setKey(new BindingKey(bindingBuilder.getIpPrefix()));
                SourcesBuilder sourcesBuilder = new SourcesBuilder();
                sourcesBuilder.setSource(nodeIds);
                bindingBuilder.setSources(sourcesBuilder.build());
                return bindingBuilder.build();
        }

        private org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.Binding getBinding(
                String prefix, String sgt) {
                org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.BindingBuilder
                        bindingBuilder =
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.BindingBuilder();

                bindingBuilder.setSgt(Sgt.getDefaultInstance(sgt));
                List<IpPrefix> ipPrefixes = new ArrayList<>();
                bindingBuilder.setIpPrefix(ipPrefixes);
                bindingBuilder.setKey(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.BindingKey(
                                bindingBuilder.getSgt()));
                if (prefix.contains(":")) {
                        ipPrefixes.add(new IpPrefix(Ipv6Prefix.getDefaultInstance(prefix)));
                } else {
                        ipPrefixes.add(new IpPrefix(Ipv4Prefix.getDefaultInstance(prefix)));
                }
                return bindingBuilder.build();
        }

        private PrefixGroup getPrefixGroup(int sgt, String... prefixes) {
                PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder();
                List<Binding> bindings = new ArrayList<>();
                for (String s : prefixes) {
                        bindings.add(getBinding(s));
                }
                prefixGroupBuilder.setBinding(bindings);
                prefixGroupBuilder.setSgt(new Sgt(sgt));
                return prefixGroupBuilder.build();
        }

        private Source getSource(PrefixGroup... prefixGroups_) {
                SourceBuilder sourceBuilder = new SourceBuilder();
                List<PrefixGroup> prefixGroups = new ArrayList<>();
                sourceBuilder.setPrefixGroup(prefixGroups);
                for (PrefixGroup group : prefixGroups_) {
                        prefixGroups.add(group);
                }
                sourceBuilder.setBindingSource(DatabaseBindingSource.Local);
                sourceBuilder.setKey(new SourceKey(sourceBuilder.getBindingSource()));
                return sourceBuilder.build();
        }

        private MasterDatabase getMasterDatabase(Source... sources_) {
                MasterDatabaseBuilder masterDatabaseBuilder = new MasterDatabaseBuilder();
                List<Source> sources = new ArrayList<>();
                for (Source source : sources_) {
                        sources.add(source);
                }
                masterDatabaseBuilder.setSource(sources);
                return masterDatabaseBuilder.build();
        }

        @Test public void testGetBindingsSources() throws Exception {
                List<NodeId>
                        nodeIds =
                        RpcServiceImpl.getBindingsSources(getMasterDatabase(
                                getSource(getPrefixGroup(10, "0.0.0.0/32", "1.1.1.1/32"),
                                        getPrefixGroup(50, "5.5.5.5/32"), getPrefixGroup(150, "5.5.5.5/32"))));
                assertNotNull(nodeIds);
                assertTrue(nodeIds.contains(NodeId.getDefaultInstance("0.0.0.0")));
                assertTrue(nodeIds.contains(NodeId.getDefaultInstance("1.1.1.1")));
                assertTrue(nodeIds.contains(NodeId.getDefaultInstance("5.5.5.5")));
                assertFalse(nodeIds.contains(NodeId.getDefaultInstance("5.5.5.50")));
        }

        @Test public void testGetNodeBindings() throws Exception {
                List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.Binding>
                        bindings =
                        RpcServiceImpl.getNodeBindings(getMasterDatabase(
                                        getSource(getPrefixGroup(10, "0.0.0.0/32", "1.1.1.1/32"),
                                                getPrefixGroup(50, "5.5.5.5/32"), getPrefixGroup(150, "0.0.0.0/32"))),
                                NodeId.getDefaultInstance("0.0.0.0"));
                assertNotNull(bindings);
                assertBinding(bindings, "0.0.0.0/32", 10);
                assertBinding(bindings, "0.0.0.0/32", 150);
        }

        private void assertBinding(
                List<org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.Binding> bindings,
                String prefix, int sgt) {
                for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.configuration.fields.Binding binding : bindings) {
                        if (binding.getSgt().getValue().equals(sgt)) {
                                for (IpPrefix ipPrefix : binding.getIpPrefix()) {
                                        if (ipPrefix.getIpv4Prefix().getValue().equals(prefix)) {
                                                return;
                                        }
                                }
                        }
                }
                fail();
        }

        private Connection getConnection(String ip, Integer port) {
                ConnectionBuilder connection = new ConnectionBuilder();
                connection.setTcpPort(port != null ? new PortNumber(port) : null);
                connection.setPeerAddress(ip != null ? new IpAddress(ip.toCharArray()) : null);
                return connection.build();
        }

        @Test public void testAddConnection() throws Exception {
                AddConnectionInputBuilder input = new AddConnectionInputBuilder();
                input.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));
                ConnectionsBuilder connectionBuilder = new ConnectionsBuilder();
                List<Connection> connections = new ArrayList<>();
                connectionBuilder.setConnection(connections);
                input.setConnections(connectionBuilder.build());

                connections.add(getConnection("1.1.1.1", null));
                connections.add(getConnection("10.1.10.1", 60456));
                assertTrue(service.addConnection(input.build()).get().getResult().isResult());

                connections.clear();
                connections.add(getConnection(null, 60000));
                assertFalse(service.addConnection(input.build()).get().getResult().isResult());

                connections.clear();
                assertFalse(service.addConnection(input.build()).get().getResult().isResult());

        }

        @Test public void testAddEntry() throws Exception {
                Optional optional = mock(Optional.class);
                when(optional.isPresent()).thenReturn(true);
                when(optional.get()).thenReturn(mock(MasterDatabase.class));
                when(checkedFuture.get()).thenReturn(null).thenReturn(optional);

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

        @Test public void testDeleteConnection() throws Exception {
                when(node.removeConnection(any(InetSocketAddress.class))).thenReturn(mock(SxpConnection.class));
                DeleteConnectionInputBuilder input = new DeleteConnectionInputBuilder();
                input.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));

                input.setTcpPort(new PortNumber(60000));
                input.setPeerAddress(Ipv4Address.getDefaultInstance("5.5.5.5"));
                assertTrue(service.deleteConnection(input.build()).get().getResult().isResult());

                input.setTcpPort(null);
                input.setPeerAddress(Ipv4Address.getDefaultInstance("5.5.5.5"));
                assertTrue(service.deleteConnection(input.build()).get().getResult().isResult());

                input.setTcpPort(new PortNumber(0));
                assertFalse(service.deleteConnection(input.build()).get().getResult().isResult());

                input.setTcpPort(new PortNumber(60000));
                input.setPeerAddress(null);
                assertFalse(service.deleteConnection(input.build()).get().getResult().isResult());

                input.setTcpPort(new PortNumber(60000));
                input.setPeerAddress(Ipv4Address.getDefaultInstance("5.5.5.5"));
                when(node.removeConnection(any(InetSocketAddress.class))).thenReturn(null);
                assertFalse(service.deleteConnection(input.build()).get().getResult().isResult());
        }

        @Test public void testDeleteEntry() throws Exception {
                Optional optional = mock(Optional.class);
                when(optional.isPresent()).thenReturn(true);
                when(optional.get()).thenReturn(mock(MasterDatabase.class));
                when(checkedFuture.get()).thenReturn(null).thenReturn(optional);

                DeleteEntryInputBuilder input = new DeleteEntryInputBuilder();
                input.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));

                input.setSgt(new Sgt(20));
                assertFalse(service.deleteEntry(input.build()).get().getResult().isResult());

                List<IpPrefix> ipPrefixes = new ArrayList<>();
                input.setIpPrefix(ipPrefixes);
                ipPrefixes.add(new IpPrefix(Ipv4Prefix.getDefaultInstance("0.0.0.5/32")));
                assertTrue(service.deleteEntry(input.build()).get().getResult().isResult());

                input.setSgt(null);
                assertFalse(service.deleteEntry(input.build()).get().getResult().isResult());
        }

        @Test public void testGetBindingSgts() throws Exception {
                GetBindingSgtsInputBuilder input = new GetBindingSgtsInputBuilder();
                input.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));
                Optional optional = mock(Optional.class);
                when(optional.isPresent()).thenReturn(true);
                when(optional.get()).thenReturn(getMasterDatabase(
                                getSource(getPrefixGroup(10, "0.0.0.0/32", "1.1.1.1/32"),
                                        getPrefixGroup(50, "5.5.5.5/32"), getPrefixGroup(150, "0.0.0.0/32"))),
                        NodeId.getDefaultInstance("0.0.0.0"));
                when(checkedFuture.get()).thenReturn(optional);

                input.setIpPrefix(new IpPrefix(Ipv4Prefix.getDefaultInstance("0.0.0.0/32")));
                assertFalse(service.getBindingSgts(input.build()).get().getResult().getSgt().isEmpty());

                input.setIpPrefix(null);
                assertNull(service.getBindingSgts(input.build()).get().getResult().getSgt());
        }

        @Test public void testGetConnections() throws Exception {
                GetConnectionsInputBuilder input = new GetConnectionsInputBuilder();
                input.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));
                List<SxpConnection> map = new ArrayList<>();
                map.add(mock(SxpConnection.class));
                when(node.getAllConnections()).thenReturn(map);

                assertFalse(service.getConnections(input.build())
                        .get()
                        .getResult()
                        .getConnections()
                        .getConnection()
                        .isEmpty());

                input.setRequestedNode(NodeId.getDefaultInstance("0.0.0.1"));
                assertNull(service.getConnections(input.build()).get().getResult().getConnections().getConnection());

        }

        @Test public void testGetNodeBindings1() throws Exception {
                Optional optional = mock(Optional.class);
                when(optional.isPresent()).thenReturn(true);
                when(optional.get()).thenReturn(getMasterDatabase(
                                getSource(getPrefixGroup(10, "0.0.0.0/32", "1.1.1.1/32"),
                                        getPrefixGroup(50, "5.5.5.5/32"), getPrefixGroup(150, "0.0.0.0/32"))),
                        NodeId.getDefaultInstance("0.0.0.0"));
                when(checkedFuture.get()).thenReturn(optional);
                GetNodeBindingsInputBuilder input = new GetNodeBindingsInputBuilder();
                input.setLocalRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));

                input.setRequestedNode(null);
                assertTrue(service.getNodeBindings(input.build()).get().getResult().getBinding().isEmpty());

                input.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));
                assertFalse(service.getNodeBindings(input.build()).get().getResult().getBinding().isEmpty());
        }

        @Test public void testUpdateEntry() throws Exception {
                Optional optional = mock(Optional.class);
                when(optional.isPresent()).thenReturn(true);
                when(optional.get()).thenReturn(mock(MasterDatabase.class));

                Optional optional_ = mock(Optional.class);
                when(optional_.isPresent()).thenReturn(false);
                when(optional_.get()).thenReturn(null);
                when(checkedFuture.get()).thenReturn(optional)
                        .thenReturn(optional)
                        .thenReturn(optional_)
                        .thenReturn(optional_)
                        .thenReturn(optional);

                UpdateEntryInputBuilder input = new UpdateEntryInputBuilder();
                input.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));

                input.setNewBinding(getNewBinding("1.1.10.1/32", 50));
                input.setOriginalBinding(getOriginalBinding("1.1.1.1/32", 450));
                assertTrue(service.updateEntry(input.build()).get().getResult().isResult());

                when(checkedFuture.get()).thenReturn(optional)
                        .thenReturn(optional)
                        .thenReturn(optional_)
                        .thenReturn(optional);
                assertFalse(service.updateEntry(input.build()).get().getResult().isResult());

                when(checkedFuture.get()).thenReturn(optional).thenReturn(optional_);
                assertFalse(service.updateEntry(input.build()).get().getResult().isResult());

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

        @Test public void testAddFilter() throws Exception {
                AddFilterInputBuilder inputBuilder = new AddFilterInputBuilder();
                assertFalse(service.addFilter(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.10"));
                assertFalse(service.addFilter(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));
                assertFalse(service.addFilter(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setPeerGroupName("TEST");
                assertFalse(service.addFilter(inputBuilder.build()).get().getResult().isResult());

                SxpFilter filter = mock(SxpFilter.class);
                inputBuilder.setSxpFilter(filter);
                assertFalse(service.addFilter(inputBuilder.build()).get().getResult().isResult());

                when(filter.getFilterType()).thenReturn(FilterType.Inbound);
                assertFalse(service.addFilter(inputBuilder.build()).get().getResult().isResult());

                when(filter.getFilterEntries()).thenReturn(mock(AclFilterEntries.class));
                assertFalse(service.addFilter(inputBuilder.build()).get().getResult().isResult());

                AclFilterEntries entries = mock(AclFilterEntries.class);
                when(filter.getFilterEntries()).thenReturn(entries);
                assertFalse(service.addFilter(inputBuilder.build()).get().getResult().isResult());

                ArrayList<AclEntry> aclEntries = new ArrayList<>();
                when(entries.getAclEntry()).thenReturn(aclEntries);
                AclEntry entry = mock(AclEntry.class);
                aclEntries.add(entry);
                assertFalse(service.addFilter(inputBuilder.build()).get().getResult().isResult());

                when(entry.getEntryType()).thenReturn(FilterEntryType.Deny);
                assertFalse(service.addFilter(inputBuilder.build()).get().getResult().isResult());

                when(entry.getEntrySeq()).thenReturn(1);
                assertFalse(service.addFilter(inputBuilder.build()).get().getResult().isResult());

                when(entry.getAclMatch()).thenReturn(mock(AclMatch.class));
                assertTrue(service.addFilter(inputBuilder.build()).get().getResult().isResult());
        }

        @Test public void testAddPeerGroup() throws Exception {
                AddPeerGroupInputBuilder inputBuilder = new AddPeerGroupInputBuilder();
                assertFalse(service.addPeerGroup(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.10"));
                assertFalse(service.addPeerGroup(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));
                assertFalse(service.addPeerGroup(inputBuilder.build()).get().getResult().isResult());

                SxpPeerGroup peerGroup = mock(SxpPeerGroup.class);
                inputBuilder.setSxpPeerGroup(peerGroup);
                assertFalse(service.addPeerGroup(inputBuilder.build()).get().getResult().isResult());

                when(peerGroup.getSxpPeers()).thenReturn(mock(SxpPeers.class));
                assertFalse(service.addPeerGroup(inputBuilder.build()).get().getResult().isResult());

                when(peerGroup.getName()).thenReturn("TEST");
                assertFalse(service.addPeerGroup(inputBuilder.build()).get().getResult().isResult());

                when(peerGroup.getName()).thenReturn("TEST2");
                assertTrue(service.addPeerGroup(inputBuilder.build()).get().getResult().isResult());
        }

        @Test public void testDeleteFilter() throws Exception {
                DeleteFilterInputBuilder inputBuilder = new DeleteFilterInputBuilder();
                assertFalse(service.deleteFilter(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.10"));
                assertFalse(service.deleteFilter(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));
                assertFalse(service.deleteFilter(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setPeerGroupName("TEST");
                assertFalse(service.deleteFilter(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setFilterType(FilterType.Outbound);
                assertTrue(service.deleteFilter(inputBuilder.build()).get().getResult().isResult());
        }

        @Test public void testDeletePeerGroup() throws Exception {
                DeletePeerGroupInputBuilder inputBuilder = new DeletePeerGroupInputBuilder();
                assertFalse(service.deletePeerGroup(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.10"));
                assertFalse(service.deletePeerGroup(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));
                assertFalse(service.deletePeerGroup(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setPeerGroupName("TEST");
                assertTrue(service.deletePeerGroup(inputBuilder.build()).get().getResult().isResult());
        }

        @Test public void testGetPeerGroup() throws Exception {
                GetPeerGroupInputBuilder inputBuilder = new GetPeerGroupInputBuilder();
                assertNull(service.getPeerGroup(inputBuilder.build()).get().getResult().getSxpPeerGroup());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.10"));
                assertNull(service.getPeerGroup(inputBuilder.build()).get().getResult().getSxpPeerGroup());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));
                assertNull(service.getPeerGroup(inputBuilder.build()).get().getResult().getSxpPeerGroup());

                inputBuilder.setPeerGroupName("TEST");
                assertNotNull(service.getPeerGroup(inputBuilder.build()).get().getResult().getSxpPeerGroup());
        }

        @Test public void testGetPeerGroups() throws Exception {
                GetPeerGroupsInputBuilder inputBuilder = new GetPeerGroupsInputBuilder();
                assertTrue(service.getPeerGroups(inputBuilder.build()).get().getResult().getSxpPeerGroup().isEmpty());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.10"));
                assertTrue(service.getPeerGroups(inputBuilder.build()).get().getResult().getSxpPeerGroup().isEmpty());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));
                assertFalse(service.getPeerGroups(inputBuilder.build()).get().getResult().getSxpPeerGroup().isEmpty());
        }

        @Test public void testUpdateFilter() throws Exception {
                UpdateFilterInputBuilder inputBuilder = new UpdateFilterInputBuilder();
                assertFalse(service.updateFilter(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.10"));
                assertFalse(service.updateFilter(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));
                assertFalse(service.updateFilter(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setPeerGroupName("TEST");
                assertFalse(service.updateFilter(inputBuilder.build()).get().getResult().isResult());

                SxpFilter filter = mock(SxpFilter.class);
                inputBuilder.setSxpFilter(filter);
                assertFalse(service.updateFilter(inputBuilder.build()).get().getResult().isResult());

                when(filter.getFilterType()).thenReturn(FilterType.Inbound);
                assertFalse(service.updateFilter(inputBuilder.build()).get().getResult().isResult());

                when(filter.getFilterEntries()).thenReturn(mock(AclFilterEntries.class));
                assertFalse(service.updateFilter(inputBuilder.build()).get().getResult().isResult());

                AclFilterEntries entries = mock(AclFilterEntries.class);
                when(filter.getFilterEntries()).thenReturn(entries);
                assertFalse(service.updateFilter(inputBuilder.build()).get().getResult().isResult());

                ArrayList<AclEntry> aclEntries = new ArrayList<>();
                when(entries.getAclEntry()).thenReturn(aclEntries);
                AclEntry entry = mock(AclEntry.class);
                aclEntries.add(entry);
                assertFalse(service.updateFilter(inputBuilder.build()).get().getResult().isResult());

                when(entry.getEntryType()).thenReturn(FilterEntryType.Deny);
                assertFalse(service.updateFilter(inputBuilder.build()).get().getResult().isResult());

                when(entry.getEntrySeq()).thenReturn(1);
                assertFalse(service.updateFilter(inputBuilder.build()).get().getResult().isResult());

                when(entry.getAclMatch()).thenReturn(mock(AclMatch.class));
                assertTrue(service.updateFilter(inputBuilder.build()).get().getResult().isResult());
        }

        @Test public void testUpdatePeerGroup() throws Exception {
                UpdatePeerGroupInputBuilder inputBuilder = new UpdatePeerGroupInputBuilder();
                assertFalse(service.updatePeerGroup(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.10"));
                assertFalse(service.updatePeerGroup(inputBuilder.build()).get().getResult().isResult());

                inputBuilder.setRequestedNode(NodeId.getDefaultInstance("0.0.0.0"));
                assertFalse(service.updatePeerGroup(inputBuilder.build()).get().getResult().isResult());

                SxpPeerGroup peerGroup = mock(SxpPeerGroup.class);
                inputBuilder.setSxpPeerGroup(peerGroup);
                assertFalse(service.updatePeerGroup(inputBuilder.build()).get().getResult().isResult());

                when(peerGroup.getSxpPeers()).thenReturn(mock(SxpPeers.class));
                assertFalse(service.updatePeerGroup(inputBuilder.build()).get().getResult().isResult());

                when(peerGroup.getName()).thenReturn("TEST2");
                assertFalse(service.updatePeerGroup(inputBuilder.build()).get().getResult().isResult());

                when(peerGroup.getName()).thenReturn("TEST");
                assertTrue(service.updatePeerGroup(inputBuilder.build()).get().getResult().isResult());
        }
}
