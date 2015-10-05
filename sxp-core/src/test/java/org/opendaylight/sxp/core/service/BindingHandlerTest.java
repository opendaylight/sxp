/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.ThreadsWorker;
import org.opendaylight.sxp.core.messaging.AttributeFactory;
import org.opendaylight.sxp.core.messaging.legacy.LegacyAttributeFactory;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseProvider;
import org.opendaylight.sxp.util.exception.message.attribute.SecurityGroupTagValueException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.peer.sequence.fields.peer.sequence.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.peer.sequence.fields.peer.sequence.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.TlvType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessageLegacyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.AddIpv4AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.AddIpv6AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.DeleteIpv4AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.DeleteIpv6AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.add.ipv4.attribute.AddIpv4AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.add.ipv6.attribute.AddIpv6AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.delete.ipv4.attribute.DeleteIpv4AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.delete.ipv6.attribute.DeleteIpv6AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlv.fields.tlv.optional.fields.SourceGroupTagTlvAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlv.fields.tlv.optional.fields.source.group.tag.tlv.attribute.SourceGroupTagTlvAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlvs.fields.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlvs.fields.TlvBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class}) public class BindingHandlerTest {

        private static SxpNode sxpNode;
        private static SxpConnection connection;
        private static ThreadsWorker worker;
        private static SxpDatabaseProvider databaseProvider;

        @Before public void init() throws Exception {
                sxpNode = PowerMockito.mock(SxpNode.class);
                PowerMockito.when(sxpNode.isEnabled()).thenReturn(true);
                connection = mock(SxpConnection.class);
                when(connection.getInboundMonitor()).thenReturn(new AtomicLong(0));

                worker = mock(ThreadsWorker.class);
                when(connection.getOwner()).thenReturn(sxpNode);
                when(connection.getNodeIdRemote()).thenReturn(NodeId.getDefaultInstance("0.0.0.0"));
                PowerMockito.when(sxpNode.getWorker()).thenReturn(worker);
                List<CapabilityType> capabilities = new ArrayList<>();
                capabilities.add(CapabilityType.LoopDetection);
                when(connection.getCapabilities()).thenReturn(capabilities);
                when(connection.getDestination()).thenReturn(
                        new InetSocketAddress(InetAddress.getByName("1.1.1.1"), 5));
                databaseProvider = mock(SxpDatabaseProvider.class);
                when(databaseProvider.addBindings(any(SxpDatabase.class), any(SxpBindingFilter.class))).thenReturn(true);
                PowerMockito.when(sxpNode.getBindingSxpDatabase()).thenReturn(databaseProvider);
        }

        private Peer getPeer(String id, int key) {
                PeerBuilder peerBuilder = new PeerBuilder();
                peerBuilder.setNodeId(new NodeId(id));
                peerBuilder.setKey(new PeerKey(key));
                return peerBuilder.build();
        }

        private PathGroup getPathGroup(List<Peer> peerList) {
                PathGroupBuilder pathGroupBuilder = new PathGroupBuilder();
                PeerSequenceBuilder peerSequenceBuilder = new PeerSequenceBuilder();
                peerSequenceBuilder.setPeer(peerList);
                pathGroupBuilder.setPeerSequence(peerSequenceBuilder.build());
                return pathGroupBuilder.build();
        }

        @Test public void testLoopDetection() throws Exception {
                List<PathGroup> pathGroups = new ArrayList<>();
                SxpDatabaseBuilder sxpDatabaseBuilder = new SxpDatabaseBuilder();
                sxpDatabaseBuilder.setPathGroup(pathGroups);

                List<Peer> peerList = new ArrayList<>();
                List<Peer> peerList_ = new ArrayList<>();

                peerList.add(getPeer("127.0.0.0", 0));
                peerList.add(getPeer("127.0.0.1", 1));
                peerList.add(getPeer("127.0.0.2", 2));
                peerList_.addAll(peerList);
                pathGroups.add(getPathGroup(peerList));

                peerList = new ArrayList<>();
                peerList.add(getPeer("127.0.1.0", 0));
                peerList.add(getPeer("127.0.2.1", 1));
                peerList.add(getPeer("127.0.3.2", 2));
                pathGroups.add(getPathGroup(peerList));

                SxpDatabase
                        sxpDatabase =
                        BindingHandler.loopDetection(new NodeId("127.0.2.1"), sxpDatabaseBuilder.build());
                assertNotNull(sxpDatabase);

                List<PathGroup> pathGroups_ = new ArrayList<>();
                SxpDatabaseBuilder sxpDatabaseBuilder_ = new SxpDatabaseBuilder();
                sxpDatabaseBuilder_.setPathGroup(pathGroups_);
                pathGroups_.add(getPathGroup(peerList_));

                assertEquals(sxpDatabaseBuilder_.build(), sxpDatabase);
        }

        private List<IpPrefix> getIpPrefixes(String... strings) {
                List<IpPrefix> ipPrefixes = new ArrayList<>();
                for (String s : strings) {
                        ipPrefixes.add(new IpPrefix(s.toCharArray()));
                }
                return ipPrefixes;
        }

        private Attribute getDeleteIpv4(String prefix) {
                DeleteIpv4AttributeBuilder deleteIpv4AttributeBuilder = new DeleteIpv4AttributeBuilder();
                DeleteIpv4AttributesBuilder deleteIpv4AttributesBuilder = new DeleteIpv4AttributesBuilder();
                deleteIpv4AttributesBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
                deleteIpv4AttributeBuilder.setDeleteIpv4Attributes(deleteIpv4AttributesBuilder.build());
                AttributeBuilder builder = new AttributeBuilder();
                builder.setAttributeOptionalFields(deleteIpv4AttributeBuilder.build());
                builder.setType(AttributeType.DelIpv4);
                return builder.build();
        }

        private Attribute getDeleteIpv6(String prefix) {
                DeleteIpv6AttributeBuilder deleteIpv6AttributeBuilder = new DeleteIpv6AttributeBuilder();
                DeleteIpv6AttributesBuilder deleteIpv6AttributesBuilder = new DeleteIpv6AttributesBuilder();
                deleteIpv6AttributesBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
                deleteIpv6AttributeBuilder.setDeleteIpv6Attributes(deleteIpv6AttributesBuilder.build());
                AttributeBuilder builder = new AttributeBuilder();
                builder.setAttributeOptionalFields(deleteIpv6AttributeBuilder.build());
                builder.setType(AttributeType.DelIpv6);
                return builder.build();
        }

        private Attribute getAddIpv4(String prefix) {
                AddIpv4AttributeBuilder addIpv4AttributeBuilder = new AddIpv4AttributeBuilder();
                AddIpv4AttributesBuilder addIpv4AttributesBuilder = new AddIpv4AttributesBuilder();
                addIpv4AttributesBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
                addIpv4AttributeBuilder.setAddIpv4Attributes(addIpv4AttributesBuilder.build());
                AttributeBuilder builder = new AttributeBuilder();
                builder.setAttributeOptionalFields(addIpv4AttributeBuilder.build());
                builder.setType(AttributeType.AddIpv4);
                return builder.build();
        }

        private MappingRecord getAddIp(int sgt, String prefix, AttributeType attributeType) {
                MappingRecordBuilder mappingRecordBuilder = new MappingRecordBuilder();
                mappingRecordBuilder.setAddress(new IpPrefix(prefix.toCharArray()));
                mappingRecordBuilder.setOperationCode(attributeType);
                List<Tlv> tlvs = new ArrayList<>();
                mappingRecordBuilder.setTlv(tlvs);
                TlvBuilder tlvBuilder = new TlvBuilder();
                tlvBuilder.setType(TlvType.Sgt);
                SourceGroupTagTlvAttributeBuilder
                        sourceGroupTagTlvAttributeBuilder =
                        new SourceGroupTagTlvAttributeBuilder();
                SourceGroupTagTlvAttributesBuilder
                        sourceGroupTagTlvAttributesBuilder =
                        new SourceGroupTagTlvAttributesBuilder();
                sourceGroupTagTlvAttributesBuilder.setSgt(sgt);
                sourceGroupTagTlvAttributeBuilder.setSourceGroupTagTlvAttributes(
                        sourceGroupTagTlvAttributesBuilder.build());
                tlvBuilder.setTlvOptionalFields(sourceGroupTagTlvAttributeBuilder.build());
                tlvs.add(tlvBuilder.build());
                return mappingRecordBuilder.build();
        }

        private Attribute getAddIpv6(String prefix) {
                AddIpv6AttributeBuilder addIpv6AttributeBuilder = new AddIpv6AttributeBuilder();
                AddIpv6AttributesBuilder addIpv6AttributesBuilder = new AddIpv6AttributesBuilder();
                addIpv6AttributesBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
                addIpv6AttributeBuilder.setAddIpv6Attributes(addIpv6AttributesBuilder.build());
                AttributeBuilder builder = new AttributeBuilder();
                builder.setAttributeOptionalFields(addIpv6AttributeBuilder.build());
                builder.setType(AttributeType.AddIpv6);
                return builder.build();
        }

        private Attribute getPeerSequence(String... strings) {
                List<NodeId> nodeIds = new ArrayList<>();
                for (String s : strings) {
                        nodeIds.add(new NodeId(s));
                }
                return AttributeFactory.createPeerSequence(nodeIds);
        }

        private void assertDatabase(SxpDatabase database, int[] sgt) {
                int sgtCount = 0;
                for (PathGroup pathGroup : database.getPathGroup()) {
                        assertFalse(pathGroup.getPrefixGroup().isEmpty());
                        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup prefixGroup : pathGroup
                                .getPrefixGroup()) {
                                assertEquals(sgt[sgtCount], (long) prefixGroup.getSgt().getValue());
                        }
                        sgtCount++;
                }

        }

        private void assertDatabase(SxpDatabase database, List<IpPrefix> ipPrefixes) {
                int count = 0;
                assertFalse(database.getPathGroup().isEmpty());
                for (PathGroup pathGroup : database.getPathGroup()) {
                        assertFalse(pathGroup.getPrefixGroup().isEmpty());
                        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup prefixGroup : pathGroup
                                .getPrefixGroup()) {
                                assertFalse(prefixGroup.getBinding().isEmpty());
                                for (Binding binding : prefixGroup.getBinding()) {
                                        assertTrue(ipPrefixes.contains(binding.getIpPrefix()));
                                        count++;
                                }
                        }
                }
                assertEquals(count, ipPrefixes.size());

        }

        private UpdateMessage getMessage(List<Attribute> attributes) {
                UpdateMessageBuilder updateMessageBuilder = new UpdateMessageBuilder();
                updateMessageBuilder.setType(MessageType.Update);
                updateMessageBuilder.setLength(0);
                updateMessageBuilder.setPayload(new byte[] {});
                updateMessageBuilder.setAttribute(attributes);
                return updateMessageBuilder.build();
        }

        private UpdateMessageLegacy getMessageLegacy(List<MappingRecord> mappingRecords) {
                UpdateMessageLegacyBuilder updateMessageBuilder = new UpdateMessageLegacyBuilder();
                updateMessageBuilder.setType(MessageType.Update);
                updateMessageBuilder.setLength(0);
                updateMessageBuilder.setPayload(new byte[] {});
                updateMessageBuilder.setMappingRecord(mappingRecords);
                return updateMessageBuilder.build();
        }

        private List<Attribute> getAddition() throws SecurityGroupTagValueException {
                List<Attribute> attributes = new ArrayList<>();
                attributes.add(getPeerSequence("1.1.1.1"));
                attributes.add(AttributeFactory.createSourceGroupTag(25));

                List<IpPrefix> ipPrefixes = getIpPrefixes("127.0.0.0/32", "127.0.10.2/32");
                attributes.add(AttributeFactory.createIpv4AddPrefix(getIpPrefixes("127.0.0.0/32", "127.0.10.2/32")));
                attributes.add(getPeerSequence("2.2.2.2"));
                attributes.add(AttributeFactory.createSourceGroupTag(45));
                ipPrefixes.addAll(getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"));
                attributes.add(AttributeFactory.createIpv6AddPrefix(
                        getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64")));

                //Legacy
                ipPrefixes.add(new IpPrefix("128.0.0.0/32".toCharArray()));
                attributes.add(getAddIpv4("128.0.0.0/32"));
                ipPrefixes.add(new IpPrefix("128.50.0.0/24".toCharArray()));
                attributes.add(getAddIpv4("128.50.0.0/24"));

                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:0:0:8/32".toCharArray()));
                attributes.add(getAddIpv6("2001:0:0:0:0:0:0:8/32"));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:C:0:8/128".toCharArray()));
                attributes.add(getAddIpv6("2001:0:0:0:0:C:0:8/128"));
                return attributes;
        }

        private List<MappingRecord> getLegacyAddition() {
                List<MappingRecord> mappingRecords = new ArrayList<>();

                mappingRecords.add(getAddIp(10, "128.0.0.0/32", AttributeType.AddIpv4));
                mappingRecords.add(getAddIp(10, "128.50.0.0/24", AttributeType.AddIpv4));
                mappingRecords.add(getAddIp(10, "2001:0:0:0:0:0:0:8/32", AttributeType.AddIpv6));
                mappingRecords.add(getAddIp(10, "2001:0:0:0:0:C:0:8/128", AttributeType.AddIpv6));
                return mappingRecords;
        }

        private List<Attribute> getDeletion() {
                List<Attribute> attributes = new ArrayList<>();
                attributes.add(AttributeFactory.createIpv4DeletePrefix(getIpPrefixes("127.0.0.0/32", "127.0.10.2/32")));
                attributes.add(AttributeFactory.createIpv6DeletePrefix(
                        getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64")));
                //Legacy
                attributes.add(getDeleteIpv4("128.0.0.0/32"));
                attributes.add(getDeleteIpv4("128.50.0.0/24"));
                attributes.add(getDeleteIpv6("2001:0:0:0:0:0:0:8/32"));
                attributes.add(getDeleteIpv6("2001:0:0:0:0:C:0:8/128"));
                return attributes;
        }

        private List<MappingRecord> getLegacyDeletion() {
                List<MappingRecord> attributes = new ArrayList<>();
                attributes.add(LegacyAttributeFactory.createDeleteIpv4(new IpPrefix("128.0.0.0/32".toCharArray())));
                attributes.add(LegacyAttributeFactory.createDeleteIpv4(new IpPrefix("128.50.0.0/24".toCharArray())));

                attributes.add(
                        LegacyAttributeFactory.createDeleteIpv4(new IpPrefix("2001:0:0:0:0:0:0:8/32".toCharArray())));
                attributes.add(
                        LegacyAttributeFactory.createDeleteIpv4(new IpPrefix("2001:0:0:0:0:C:0:8/128".toCharArray())));
                return attributes;
        }

        @Test public void testProcessMessageAddition() throws Exception {
                List<IpPrefix> ipPrefixes = getIpPrefixes("127.0.0.0/32", "127.0.10.2/32");
                ipPrefixes.addAll(getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"));
                //Legacy
                ipPrefixes.add(new IpPrefix("128.0.0.0/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("128.50.0.0/24".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:0:0:8/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:C:0:8/128".toCharArray()));

                SxpDatabase database = BindingHandler.processMessageAddition(getMessage(getAddition()));
                assertDatabase(database, ipPrefixes);
                assertDatabase(database, new int[] {25, 45});
        }

        @Test public void testProcessMessageAdditionLegacy() throws Exception {
                List<IpPrefix> ipPrefixes = new ArrayList<>();
                ipPrefixes.add(new IpPrefix("128.0.0.0/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("128.50.0.0/24".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:0:0:8/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:C:0:8/128".toCharArray()));

                SxpDatabase
                        database =
                        BindingHandler.processMessageAddition(new NodeId("0.0.0.0"),
                                getMessageLegacy(getLegacyAddition()));
                assertDatabase(database, ipPrefixes);
                assertDatabase(database, new int[] {10});
        }

        @Test public void testProcessMessageDeletion() throws Exception {
                List<IpPrefix> ipPrefixes = getIpPrefixes("127.0.0.0/32", "127.0.10.2/32");
                ipPrefixes.addAll(getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"));
                //Legacy
                ipPrefixes.add(new IpPrefix("128.0.0.0/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("128.50.0.0/24".toCharArray()));

                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:0:0:8/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:C:0:8/128".toCharArray()));

                SxpDatabase
                        database =
                        BindingHandler.processMessageDeletion(new NodeId("0.0.0.0"), getMessage(getDeletion()));
                assertDatabase(database, ipPrefixes);
        }

        @Test public void testProcessMessageDeletionLegacy() throws Exception {
                List<IpPrefix> ipPrefixes = new ArrayList<>();

                ipPrefixes.add(new IpPrefix("128.0.0.0/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("128.50.0.0/24".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:0:0:8/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:C:0:8/128".toCharArray()));

                SxpDatabase
                        database =
                        BindingHandler.processMessageDeletion(new NodeId("0.0.0.0"),
                                getMessageLegacy(getLegacyDeletion()));
                assertDatabase(database, ipPrefixes);
        }

        @Test public void testProcessUpdateMessage() throws Exception {
                List<Attribute> attributes = getDeletion();
                attributes.addAll(getAddition());
                UpdateMessage updateMessage = getMessage(attributes);
                ArgumentCaptor<Callable> argument = ArgumentCaptor.forClass(Callable.class);

                BindingHandler.processUpdateMessage(updateMessage, connection);
                verify(connection, never()).pushUpdateMessageInbound(any(Callable.class));
                verify(worker).executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class));

                BindingHandler.processUpdateMessage(updateMessage, connection);
                verify(connection).pushUpdateMessageInbound(any(Callable.class));
                verify(worker).executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class));

                BindingHandler.processUpdateMessage(updateMessage, connection);
                verify(worker, atLeastOnce()).executeTask(argument.capture(), any(ThreadsWorker.WorkerType.class));

                argument.getValue().call();
                verify(databaseProvider).deleteBindings(any(SxpDatabase.class),any(SxpBindingFilter.class));
                verify(databaseProvider).addBindings(any(SxpDatabase.class),any(SxpBindingFilter.class));
                verify(sxpNode, times(2)).setSvcBindingManagerNotify();
        }

        @Test public void testProcessUpdateMessageLegacy() throws Exception {
                ArgumentCaptor<Callable> argument = ArgumentCaptor.forClass(Callable.class);
                List<MappingRecord> mappingRecords = getLegacyDeletion();
                mappingRecords.addAll(getLegacyAddition());

                UpdateMessageLegacy updateMessageLegacy = getMessageLegacy(mappingRecords);

                BindingHandler.processUpdateMessage(updateMessageLegacy, connection);
                verify(worker).executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class));

                BindingHandler.processUpdateMessage(updateMessageLegacy, connection);
                verify(connection).pushUpdateMessageInbound(any(Callable.class));
                verify(worker).executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class));

                BindingHandler.processUpdateMessage(updateMessageLegacy, connection);
                verify(worker, atLeastOnce()).executeTask(argument.capture(), any(ThreadsWorker.WorkerType.class));

                argument.getValue().call();
                verify(databaseProvider).deleteBindings(any(SxpDatabase.class),any(SxpBindingFilter.class));
                verify(databaseProvider).addBindings(any(SxpDatabase.class),any(SxpBindingFilter.class));
                verify(sxpNode, times(2)).setSvcBindingManagerNotify();

        }
}
