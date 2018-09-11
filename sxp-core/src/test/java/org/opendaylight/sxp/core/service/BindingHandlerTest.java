/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.opendaylight.sxp.core.BindingOriginsConfig;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.AttributeFactory;
import org.opendaylight.sxp.core.messaging.legacy.LegacyAttributeFactory;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.exception.message.attribute.SecurityGroupTagValueException;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBindingBuilder;
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

public class BindingHandlerTest {

    @Rule public ExpectedException exception = ExpectedException.none();
    private static SxpNode sxpNode;
    private static SxpConnection connection;
    private static ThreadsWorker worker;
    private ArgumentCaptor<Callable> taskCaptor;
    private static SxpDatabaseInf sxpDatabaseInf;
    private static MasterDatabaseInf masterDatabaseInf;
    private static BindingHandler handler;

    @BeforeClass
    public static void initClass() {
        BindingOriginsConfig.INSTANCE.addBindingOrigins(BindingOriginsConfig.DEFAULT_ORIGIN_PRIORITIES);
    }

    @AfterClass
    public static void tearDown() {
        BindingOriginsConfig.INSTANCE.deleteConfiguration();
    }

    @Before
    public void init() throws Exception {
        sxpNode = mock(SxpNode.class);
        when(sxpNode.isEnabled()).thenReturn(true);
        connection = mock(SxpConnection.class);

        worker = mock(ThreadsWorker.class);
        taskCaptor = ArgumentCaptor.forClass(Callable.class);
        when(worker.executeTaskInSequence(taskCaptor.capture(), any(ThreadsWorker.WorkerType.class),
                any(SxpConnection.class))).thenReturn(mock(ListenableFuture.class));
        when(connection.getOwner()).thenReturn(sxpNode);
        when(connection.getDomainName()).thenReturn("default");
        when(connection.getId()).thenReturn(NodeId.getDefaultInstance("0.0.0.0"));
        when(connection.getOwnerId()).thenReturn(NodeId.getDefaultInstance("1.1.1.1"));
        when(sxpNode.getWorker()).thenReturn(worker);
        List<CapabilityType> capabilities = new ArrayList<>();
        capabilities.add(CapabilityType.LoopDetection);
        when(connection.getCapabilities()).thenReturn(capabilities);
        when(connection.getDestination()).thenReturn(new InetSocketAddress(InetAddress.getByName("1.1.1.1"), 5));
        sxpDatabaseInf = new SxpDatabaseImpl();
        masterDatabaseInf = new MasterDatabaseImpl();
        BindingDispatcher dispatcher = new BindingDispatcher(sxpNode);
        when(sxpNode.getSvcBindingDispatcher()).thenReturn(dispatcher);
        when(sxpNode.getBindingSxpDatabase(anyString())).thenReturn(sxpDatabaseInf);
        when(sxpNode.getBindingMasterDatabase(anyString())).thenReturn(masterDatabaseInf);
        SxpDomain myDomain = SxpDomain.createInstance(sxpNode, "default", sxpDatabaseInf, masterDatabaseInf);
        when(sxpNode.getDomain(anyString()))
                .thenReturn(myDomain);
        handler = new BindingHandler(sxpNode, dispatcher);
    }

    private Peer getPeer(String id, int key) {
        PeerBuilder peerBuilder = new PeerBuilder();
        peerBuilder.setNodeId(new NodeId(id));
        peerBuilder.withKey(new PeerKey(key));
        return peerBuilder.build();
    }

    private SxpDatabaseBinding getBinding(String prefix, int sgt, PeerSequence peerSequence) {
        SxpDatabaseBindingBuilder bindingBuilder = new SxpDatabaseBindingBuilder();
        bindingBuilder.setIpPrefix(IpPrefixBuilder.getDefaultInstance(prefix));
        bindingBuilder.setSecurityGroupTag(new Sgt(sgt));
        bindingBuilder.setPeerSequence(peerSequence);
        bindingBuilder.setTimestamp(TimeConv.toDt(System.currentTimeMillis()));
        bindingBuilder.setOrigin(BindingOriginsConfig.NETWORK_ORIGIN);
        return bindingBuilder.build();
    }

    @Test
    public void testLoopDetection() throws Exception {
        List<SxpBindingFields> bindings = new ArrayList<>();

        List<Peer> peerList = new ArrayList<>();
        List<Peer> peerList_ = new ArrayList<>();

        peerList.add(getPeer("127.0.0.0", 0));
        peerList.add(getPeer("127.0.0.1", 1));
        peerList.add(getPeer("127.0.0.2", 2));
        peerList_.addAll(peerList);
        bindings.add(getBinding("5.5.5.5/32", 20, new PeerSequenceBuilder().setPeer(peerList).build()));

        peerList = new ArrayList<>();
        peerList.add(getPeer("127.0.1.0", 0));
        peerList.add(getPeer("127.0.2.1", 1));
        peerList.add(getPeer("127.0.3.2", 2));
        bindings.add(getBinding("15.15.15.15/32", 10, new PeerSequenceBuilder().setPeer(peerList).build()));

        List<SxpBindingFields>
                bindings_ =
                BindingHandler.loopDetection(new NodeId("127.0.2.1"), bindings.stream()).collect(Collectors.toList());
        assertNotNull(bindings);
        assertEquals(1, bindings_.size());
        assertEquals("5.5.5.5/32", IpPrefixConv.toString(bindings_.get(0).getIpPrefix()));

        //test with null node
        Stream<SxpBindingFields> result = BindingHandler.loopDetection(null, bindings.stream());
        assertEquals(result.count(), bindings.size());

        //test with null bindings
        Stream<SxpBindingFields> result2 = BindingHandler.loopDetection(new NodeId("127.0.2.1"), null);
        assertEquals(result2, null);
    }

    private List<IpPrefix> getIpPrefixes(String... strings) {
        List<IpPrefix> ipPrefixes = new ArrayList<>();
        for (String s : strings) {
            ipPrefixes.add(IpPrefixBuilder.getDefaultInstance(s));
        }
        return ipPrefixes;
    }

    private Attribute getDeleteIpv4(String prefix) {
        DeleteIpv4AttributeBuilder deleteIpv4AttributeBuilder = new DeleteIpv4AttributeBuilder();
        DeleteIpv4AttributesBuilder deleteIpv4AttributesBuilder = new DeleteIpv4AttributesBuilder();
        deleteIpv4AttributesBuilder.setIpPrefix(IpPrefixBuilder.getDefaultInstance(prefix));
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
        deleteIpv6AttributesBuilder.setIpPrefix(IpPrefixBuilder.getDefaultInstance(prefix));
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
        addIpv4AttributesBuilder.setIpPrefix(IpPrefixBuilder.getDefaultInstance(prefix));
        addIpv4AttributeBuilder.setAddIpv4Attributes(addIpv4AttributesBuilder.build());
        AttributeBuilder builder = new AttributeBuilder();
        builder.setAttributeOptionalFields(addIpv4AttributeBuilder.build());
        builder.setType(AttributeType.AddIpv4);
        builder.setFlags(new FlagsFields.Flags(true, false, true, false, false));
        return builder.build();
    }

    private MappingRecord getAddIp(int sgt, String prefix, AttributeType attributeType) {
        MappingRecordBuilder mappingRecordBuilder = new MappingRecordBuilder();
        mappingRecordBuilder.setAddress(IpPrefixBuilder.getDefaultInstance(prefix));
        mappingRecordBuilder.setOperationCode(attributeType);
        List<Tlv> tlvs = new ArrayList<>();
        mappingRecordBuilder.setTlv(tlvs);
        TlvBuilder tlvBuilder = new TlvBuilder();
        tlvBuilder.setType(TlvType.Sgt);
        SourceGroupTagTlvAttributeBuilder sourceGroupTagTlvAttributeBuilder = new SourceGroupTagTlvAttributeBuilder();
        SourceGroupTagTlvAttributesBuilder
                sourceGroupTagTlvAttributesBuilder =
                new SourceGroupTagTlvAttributesBuilder();
        sourceGroupTagTlvAttributesBuilder.setSgt(sgt);
        sourceGroupTagTlvAttributeBuilder.setSourceGroupTagTlvAttributes(sourceGroupTagTlvAttributesBuilder.build());
        tlvBuilder.setTlvOptionalFields(sourceGroupTagTlvAttributeBuilder.build());
        tlvs.add(tlvBuilder.build());
        return mappingRecordBuilder.build();
    }

    private Attribute getAddIpv6(String prefix) {
        AddIpv6AttributeBuilder addIpv6AttributeBuilder = new AddIpv6AttributeBuilder();
        AddIpv6AttributesBuilder addIpv6AttributesBuilder = new AddIpv6AttributesBuilder();
        addIpv6AttributesBuilder.setIpPrefix(IpPrefixBuilder.getDefaultInstance(prefix));
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

    private <T extends SxpBindingFields> void assertDatabase(List<T> database, List<IpPrefix> ipPrefixes) {
        assertEquals(database.stream().map(b -> b.getIpPrefix()).collect(Collectors.toSet()),
                ipPrefixes.stream().collect(Collectors.toSet()));
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
                AttributeFactory.COMPACT));
        attributes.add(getPeerSequence("2.2.2.2"));
        attributes.add(AttributeFactory.createSourceGroupTag(45));
        ipPrefixes.addAll(getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"));
        attributes.add(
                AttributeFactory.createIpv6AddPrefix(getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"),
                        AttributeFactory.COMPACT));

        //Legacy
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("128.0.0.0/32"));
        attributes.add(getAddIpv4("128.0.0.0/32"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("128.50.0.0/24"));
        attributes.add(getAddIpv4("128.50.0.0/24"));

        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("2001:0:0:0:0:0:0:8/32"));
        attributes.add(getAddIpv6("2001:0:0:0:0:0:0:8/32"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("2001:0:0:0:0:C:0:8/128"));
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
                AttributeFactory.COMPACT));
        attributes.add(AttributeFactory.createIpv6DeletePrefix(getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"), AttributeFactory.COMPACT));
        //Legacy
        attributes.add(getDeleteIpv4("128.0.0.0/32"));
        attributes.add(getDeleteIpv4("128.50.0.0/24"));
        attributes.add(getDeleteIpv6("2001:0:0:0:0:0:0:8/32"));
        attributes.add(getDeleteIpv6("2001:0:0:0:0:C:0:8/128"));
        return attributes;
    }

    private List<MappingRecord> getLegacyDeletion() {
        List<MappingRecord> attributes = new ArrayList<>();
        attributes.add(LegacyAttributeFactory.createDeleteIpv4(IpPrefixBuilder.getDefaultInstance("128.0.0.0/32")));
        attributes.add(LegacyAttributeFactory.createDeleteIpv4(IpPrefixBuilder.getDefaultInstance("128.50.0.0/24")));

        attributes.add(LegacyAttributeFactory.createDeleteIpv4(IpPrefixBuilder.getDefaultInstance("2001:0:0:0:0:0:0:8/32")));
        attributes.add(LegacyAttributeFactory.createDeleteIpv4(IpPrefixBuilder.getDefaultInstance("2001:0:0:0:0:C:0:8/128")));
        return attributes;
    }

    @Test
    public void testProcessMessageAddition() throws Exception {
        List<IpPrefix> ipPrefixes = getIpPrefixes("127.0.0.0/32", "127.0.10.2/32");
        ipPrefixes.addAll(getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"));
        //Legacy
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("128.0.0.0/32"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("128.50.0.0/24"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("2001:0:0:0:0:0:0:8/32"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("2001:0:0:0:0:C:0:8/128"));

        List<SxpDatabaseBinding> bindings = BindingHandler.processMessageAddition(getMessage(getAddition()), null);
        assertDatabase(bindings, ipPrefixes);
    }

    @Test
    public void testProcessMessageAdditionLegacy() throws Exception {
        List<IpPrefix> ipPrefixes = new ArrayList<>();
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("128.0.0.0/32"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("128.50.0.0/24"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("2001:0:0:0:0:0:0:8/32"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("2001:0:0:0:0:C:0:8/128"));

        List<SxpDatabaseBinding>
                bindings =
                BindingHandler.processMessageAddition(getMessageLegacy(getLegacyAddition()), null,
                        new NodeId("0.0.0.0"));
        assertDatabase(bindings, ipPrefixes);
    }

    @Test
    public void testProcessMessageDeletion() throws Exception {
        List<IpPrefix> ipPrefixes = getIpPrefixes("127.0.0.0/32", "127.0.10.2/32");
        ipPrefixes.addAll(getIpPrefixes("2001:0:0:0:0:0:0:1/128", "2001:0:0:0:0:0:0:0/64"));
        //Legacy
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("128.0.0.0/32"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("128.50.0.0/24"));

        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("2001:0:0:0:0:0:0:8/32"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("2001:0:0:0:0:C:0:8/128"));

        List<SxpDatabaseBinding> bindings = BindingHandler.processMessageDeletion(getMessage(getDeletion()));
        assertDatabase(bindings, ipPrefixes);
    }

    @Test
    public void testProcessMessageDeletionWithNoPrefixes() {
        List<Attribute> attrList = new ArrayList<>();
        UpdateMessage msgMock = mock(UpdateMessage.class);
        when(msgMock.getAttribute()).thenReturn(attrList);
        Attribute attr = new AttributeBuilder().setFlags(new FlagsFields.Flags(true, true, false, true, true)).build();
        attrList.add(attr);
        BindingHandler.processMessageDeletion(msgMock);
    }

    @Test
    public void testProcessMessageDeletionLegacy() throws Exception {
        List<IpPrefix> ipPrefixes = new ArrayList<>();

        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("128.0.0.0/32"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("128.50.0.0/24"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("2001:0:0:0:0:0:0:8/32"));
        ipPrefixes.add(IpPrefixBuilder.getDefaultInstance("2001:0:0:0:0:C:0:8/128"));

        List<SxpDatabaseBinding>
                bindings =
                BindingHandler.processMessageDeletion(getMessageLegacy(getLegacyDeletion()));
        assertDatabase(bindings, ipPrefixes);
    }

    @Test
    public void testProcessPurgeAllMessage() throws Exception {
        handler.processPurgeAllMessage(connection).get();
        verify(worker).executeTaskInSequence(any(Callable.class), eq(ThreadsWorker.WorkerType.INBOUND), eq(connection));
    }

    @Test
    public void testProcessUpdate() throws Exception {
        List<SxpBindingFields> add = new ArrayList<>(), dell = new ArrayList<>();
        List<Peer> peerList = new ArrayList<>();

        peerList.add(getPeer("127.0.0.0", 0));
        peerList.add(getPeer("1.1.1.1", 1));
        peerList.add(getPeer("127.0.0.2", 2));
        add.add(getBinding("5.5.5.5/32", 20, new PeerSequenceBuilder().setPeer(new ArrayList<>()).build()));
        add.add(getBinding("5.5.5.5/32", 25, new PeerSequenceBuilder().setPeer(new ArrayList<>()).build()));
        add.add(getBinding("15.5.15.0/24", 40, new PeerSequenceBuilder().setPeer(new ArrayList<>()).build()));
        add.add(getBinding("5.0.5.50/32", 120, new PeerSequenceBuilder().setPeer(peerList).build()));

        for (int i = 0; i < 5; i++)
            handler.processUpdate(dell, add, connection);
        taskCaptor.getValue().call();
        assertDatabase(sxpDatabaseInf.getBindings(), getIpPrefixes("5.5.5.5/32", "15.5.15.0/24"));
        assertDatabase(masterDatabaseInf.getBindings(), getIpPrefixes("5.5.5.5/32", "15.5.15.0/24"));

        add.clear();
        dell.clear();
        when(connection.getId()).thenReturn(NodeId.getDefaultInstance("0.0.0.1"));

        dell.add(getBinding("5.5.5.5/32", 20, new PeerSequenceBuilder().setPeer(new ArrayList<>()).build()));
        dell.add(getBinding("15.5.15.0/24", 30, new PeerSequenceBuilder().setPeer(new ArrayList<>()).build()));
        add.add(getBinding("55.2.0.0/16", 10, new PeerSequenceBuilder().setPeer(new ArrayList<>()).build()));
        add.add(getBinding("5.5.0.0/32", 80, new PeerSequenceBuilder().setPeer(new ArrayList<>()).build()));
        for (int i = 0; i < 5; i++)
            handler.processUpdate(dell, add, connection);
        taskCaptor.getValue().call();

        assertDatabase(masterDatabaseInf.getBindings(),
                getIpPrefixes("5.5.5.5/32", "55.2.0.0/16", "5.5.0.0/32", "15.5.15.0/24"));

        add.clear();
        dell.clear();

        dell.add(getBinding("55.2.0.0/16", 10, new PeerSequenceBuilder().setPeer(new ArrayList<>()).build()));
        dell.add(getBinding("5.5.0.0/32", 80, new PeerSequenceBuilder().setPeer(new ArrayList<>()).build()));
        for (int i = 0; i < 5; i++)
            handler.processUpdate(dell, add, connection);
        taskCaptor.getValue().call();

        assertDatabase(masterDatabaseInf.getBindings(), getIpPrefixes("5.5.5.5/32", "15.5.15.0/24"));
    }

    @Test
    public void testSetBufferLimit() throws Exception {
        handler.setBufferLimit(25);
        exception.expect(IllegalArgumentException.class);
        handler.setBufferLimit(-10);
    }

    @Test
    public void testCreateHandlerWithSetBufferSize() {
        BindingHandler bindingHandler = new BindingHandler(sxpNode, new BindingDispatcher(sxpNode), 1);
        assertNotNull(bindingHandler);
    }
}
