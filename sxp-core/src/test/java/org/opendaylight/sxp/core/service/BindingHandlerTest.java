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
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.AttributeFactory;
import org.opendaylight.sxp.core.messaging.legacy.LegacyAttributeFactory;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.SxpDatabase;
import org.opendaylight.sxp.util.exception.message.attribute.SecurityGroupTagValueException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.FlagsFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.TlvType;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessageLegacy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessageLegacyBuilder;
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

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class}) public class BindingHandlerTest {

        private static SxpNode sxpNode;
        private static SxpConnection connection;
        private static ThreadsWorker worker;
        private static SxpDatabase databaseProvider;
        private static BindingHandler bindingHandler;

        @Before public void init() throws Exception {
                sxpNode = PowerMockito.mock(SxpNode.class);
                PowerMockito.when(sxpNode.isEnabled()).thenReturn(true);
                connection = mock(SxpConnection.class);

                worker = mock(ThreadsWorker.class);
                when(connection.getOwner()).thenReturn(sxpNode);
                when(connection.getNodeIdRemote()).thenReturn(NodeId.getDefaultInstance("0.0.0.0"));
                PowerMockito.when(sxpNode.getWorker()).thenReturn(worker);
                List<CapabilityType> capabilities = new ArrayList<>();
                capabilities.add(CapabilityType.LoopDetection);
                when(connection.getCapabilities()).thenReturn(capabilities);
                when(connection.getDestination()).thenReturn(
                        new InetSocketAddress(InetAddress.getByName("1.1.1.1"), 5));
                databaseProvider = mock(SxpDatabase.class);
                //when(databaseProvider.addBindings(any(SxpDatabase.class))).thenReturn(true);
                PowerMockito.when(sxpNode.getBindingSxpDatabase()).thenReturn(databaseProvider);
                bindingHandler = new BindingHandler(sxpNode, mock(BindingDispatcher.class));
        }

        private Peer getPeer(String id, int key) {
                PeerBuilder peerBuilder = new PeerBuilder();
                peerBuilder.setNodeId(new NodeId(id));
                peerBuilder.setKey(new PeerKey(key));
                return peerBuilder.build();
        }

        @Test public void testLoopDetection() throws Exception {
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
                builder.setFlags(new FlagsFields.Flags(true, false, true, false, false));
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
                builder.setFlags(new FlagsFields.Flags(true, false, true, false, false));
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
                builder.setFlags(new FlagsFields.Flags(true, false, true, false, false));
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
                builder.setFlags(new FlagsFields.Flags(true, false, true, false, false));
                return builder.build();
        }

        private Attribute getPeerSequence(String... strings) {
                List<NodeId> nodeIds = new ArrayList<>();
                for (String s : strings) {
                        nodeIds.add(new NodeId(s));
                }
                return AttributeFactory.createPeerSequence(nodeIds);
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
                attributes.add(AttributeFactory.createIpv4AddPrefix(getIpPrefixes("127.0.0.0/32", "127.0.10.2/32"),
                        AttributeFactory._onpCe));
                attributes.add(getPeerSequence("2.2.2.2"));
                attributes.add(AttributeFactory.createSourceGroupTag(45));
                ipPrefixes.addAll(getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"));
                attributes.add(AttributeFactory.createIpv6AddPrefix(
                        getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"), AttributeFactory._onpCe));

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
                attributes.add(AttributeFactory.createIpv4DeletePrefix(getIpPrefixes("127.0.0.0/32", "127.0.10.2/32"),
                        AttributeFactory._onpCe));
                attributes.add(AttributeFactory.createIpv6DeletePrefix(
                        getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"), AttributeFactory._onpCe));
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
                //TODO
                List<IpPrefix> ipPrefixes = getIpPrefixes("127.0.0.0/32", "127.0.10.2/32");
                ipPrefixes.addAll(getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"));
                //Legacy
                ipPrefixes.add(new IpPrefix("128.0.0.0/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("128.50.0.0/24".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:0:0:8/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:C:0:8/128".toCharArray()));

                /*SxpDatabase database = BindingHandler.processMessageAddition(getMessage(getAddition()), null);
                assertDatabase(database, ipPrefixes);
                assertDatabase(database, new int[] {25, 45});*/
        }

        @Test public void testProcessMessageAdditionLegacy() throws Exception {
                //TODO
                List<IpPrefix> ipPrefixes = new ArrayList<>();
                ipPrefixes.add(new IpPrefix("128.0.0.0/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("128.50.0.0/24".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:0:0:8/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:C:0:8/128".toCharArray()));

                /*SxpDatabase
                        database =
                        BindingHandler.processMessageAddition(new NodeId("0.0.0.0"), getMessageLegacy(getLegacyAddition()),
                                null);
                assertDatabase(database, ipPrefixes);
                assertDatabase(database, new int[] {10});*/
        }

        @Test public void testProcessMessageDeletion() throws Exception {
                //TODO
                List<IpPrefix> ipPrefixes = getIpPrefixes("127.0.0.0/32", "127.0.10.2/32");
                ipPrefixes.addAll(getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"));
                //Legacy
                ipPrefixes.add(new IpPrefix("128.0.0.0/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("128.50.0.0/24".toCharArray()));

                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:0:0:8/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:C:0:8/128".toCharArray()));

                /*SxpDatabase
                        database =
                        BindingHandler.processMessageDeletion(new NodeId("0.0.0.0"), getMessage(getDeletion()));
                assertDatabase(database, ipPrefixes);*/
        }

        @Test public void testProcessMessageDeletionLegacy() throws Exception {
                //TODO
                List<IpPrefix> ipPrefixes = new ArrayList<>();

                ipPrefixes.add(new IpPrefix("128.0.0.0/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("128.50.0.0/24".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:0:0:8/32".toCharArray()));
                ipPrefixes.add(new IpPrefix("2001:0:0:0:0:C:0:8/128".toCharArray()));

                /*SxpDatabase
                        database =
                        BindingHandler.processMessageDeletion(new NodeId("0.0.0.0"),
                                getMessageLegacy(getLegacyDeletion()));
                assertDatabase(database, ipPrefixes);*/
        }

        @Test public void testProcessUpdateMessage() throws Exception {
                List<Attribute> attributes = getDeletion();
                attributes.addAll(getAddition());
                UpdateMessage updateMessage = getMessage(attributes);

                bindingHandler.processUpdateMessage(updateMessage, connection);
                verify(worker).executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class));

                bindingHandler.processUpdateMessage(updateMessage, connection);
                verify(worker).executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class));

                bindingHandler.processUpdateMessage(updateMessage, connection);
                verify(worker, atLeastOnce()).executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class));
        }

        @Test public void testProcessUpdateMessageLegacy() throws Exception {
                List<MappingRecord> mappingRecords = getLegacyDeletion();
                mappingRecords.addAll(getLegacyAddition());

                UpdateMessageLegacy updateMessageLegacy = getMessageLegacy(mappingRecords);

                bindingHandler.processUpdateMessage(updateMessageLegacy, connection);
                verify(worker).executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class));

                bindingHandler.processUpdateMessage(updateMessageLegacy, connection);
                verify(worker).executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class));

                bindingHandler.processUpdateMessage(updateMessageLegacy, connection);
                verify(worker, atLeastOnce()).executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class));
        }
}
