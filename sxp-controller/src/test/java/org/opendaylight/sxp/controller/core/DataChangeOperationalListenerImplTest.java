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
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.util.database.DatastoreValidator;
import org.opendaylight.sxp.controller.util.database.MasterDatastoreImpl;
import org.opendaylight.sxp.controller.util.database.access.DatastoreAccess;
import org.opendaylight.sxp.controller.util.database.access.MasterDatabaseAccessImpl;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.sxp.controller.conf.rev141002.modules.module.configuration.sxp.controller.sxp.controller.SxpNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sources.fields.SourcesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class) @PrepareForTest({DatastoreAccess.class, DatastoreValidator.class, SxpNode.class})
public class DataChangeOperationalListenerImplTest {

        private static SxpNode node;
        private static DatastoreAccess datastoreAccess;
        private static DatastoreValidator datastoreValidator;
        private static DataChangeOperationalListenerImpl operationalListener;
        private static MasterDatabase database;
        private static List<Source> sources;

        @BeforeClass public static void initClass() throws Exception {
                node = PowerMockito.mock(SxpNode.class);
                when(node.getNodeId()).thenReturn(NodeId.getDefaultInstance("0.0.0.0"));
                Configuration.register(node);
        }

        @Before public void init() throws ExecutionException, InterruptedException {
                MasterDatabaseBuilder masterDatabaseBuilder = new MasterDatabaseBuilder();
                sources = new ArrayList<>();
                masterDatabaseBuilder.setSource(sources);
                database = masterDatabaseBuilder.build();
                datastoreAccess = PowerMockito.mock(DatastoreAccess.class);
                datastoreValidator = PowerMockito.mock(DatastoreValidator.class);
                CheckedFuture future = mock(CheckedFuture.class);
                Optional optional = mock(Optional.class);
                when(optional.get()).thenReturn(database);
                when(optional.isPresent()).thenReturn(true);
                when(future.get()).thenReturn(optional);

                PowerMockito.when(datastoreAccess.read(any(InstanceIdentifier.class), any(LogicalDatastoreType.class)))
                        .thenReturn(future);
                PowerMockito.when(datastoreAccess.merge(any(InstanceIdentifier.class), any(DataObject.class),
                        any(LogicalDatastoreType.class))).thenReturn(future);
                PowerMockito.when(datastoreValidator.getDatastoreAccess()).thenReturn(datastoreAccess);
                operationalListener = new DataChangeOperationalListenerImpl("0.0.0.0", datastoreValidator);
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

        @Test public void testGetSubscribedPath() throws Exception {
                InstanceIdentifier identifier = operationalListener.getSubscribedPath();
                assertNotNull(identifier);
                assertEquals(MasterDatabase.class, identifier.getTargetType());
        }

        @Test public void testOnDataChanged() throws Exception {
                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = mock(AsyncDataChangeEvent.class);
                Map<InstanceIdentifier<?>, DataObject> dataObjectMap = new HashMap<>();
                when(change.getUpdatedData()).thenReturn(dataObjectMap);
                when(change.getCreatedData()).thenReturn(dataObjectMap);
                when(change.getOriginalData()).thenReturn(dataObjectMap);

                dataObjectMap.put(InstanceIdentifier.create(MasterDatabase.class), null);
                dataObjectMap.put(InstanceIdentifier.create(Node.class), null);
                sources.add(getSource(getPrefixGroup(200, "2.2.2.1/32"), getPrefixGroup(20, "2.2.2.2/32")));

                operationalListener.onDataChanged(change);
                ArgumentCaptor<MasterDatabase> argumentCaptor = ArgumentCaptor.forClass(MasterDatabase.class);
                verify(datastoreAccess, atLeastOnce()).merge(any(InstanceIdentifier.class), argumentCaptor.capture(),
                        any(LogicalDatastoreType.class));
                assertNotNull(argumentCaptor.getValue());
                assertMasterDatabase(database, argumentCaptor.getAllValues());
        }

        private void assertMasterDatabase(MasterDatabase database, List<MasterDatabase> databases) {
                for (MasterDatabase masterDatabase : databases) {
                        for (Source source : masterDatabase.getSource()) {
                                for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                                        assertPrefixGroup(prefixGroup, database.getSource());
                                }
                        }
                }
        }

        private void assertPrefixGroup(PrefixGroup prefixGroup, List<Source> source) {
                for (Source source1 : source) {
                        for (PrefixGroup prefixGroup1 : source1.getPrefixGroup()) {
                                if (prefixGroup.getSgt().getValue().equals(prefixGroup1.getSgt().getValue())) {
                                        assertBindings(prefixGroup.getBinding(), prefixGroup1.getBinding());
                                        return;
                                }
                        }
                }
                fail();
        }

        private void assertBindings(List<Binding> bindings, List<Binding> bindings1) {
                assertEquals(1, bindings.size());
                Binding binding = bindings.get(0);
                for (Binding _binding : bindings1) {
                        if (binding.getIpPrefix().equals(_binding.getIpPrefix())) {
                                return;
                        }
                }
                fail();
        }
}
