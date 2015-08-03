/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.CapabilityLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.HoldTimeMaxException;
import org.opendaylight.sxp.util.exception.message.attribute.HoldTimeMinException;
import org.opendaylight.sxp.util.exception.message.attribute.SecurityGroupTagValueException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeVariant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.AttributeOptionalFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.CapabilitiesAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.HoldTimeAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.Ipv4AddPrefixAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.Ipv4DeletePrefixAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.Ipv6AddPrefixAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.Ipv6DeletePrefixAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.PeerSequenceAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.SourceGroupTagAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.SxpNodeIdAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.capabilities.attribute.CapabilitiesAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.capabilities.attribute.fields.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.capabilities.attribute.fields.CapabilitiesBuilder;
import org.osgi.resource.Capability;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AttributeFactoryTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        @Test public void testCreateCapabilities() throws Exception {
                CapabilitiesAttributeBuilder _attributeBuilder = new CapabilitiesAttributeBuilder();
                CapabilitiesAttributesBuilder _attributesBuilder = new CapabilitiesAttributesBuilder();

                ArrayList<Capabilities> capabilities = new ArrayList<>();
                CapabilitiesBuilder capability = new CapabilitiesBuilder();
                capability.setCode(CapabilityType.Ipv4Unicast);
                capabilities.add(capability.build());

                _attributesBuilder.setCapabilities(capabilities);
                _attributeBuilder.setCapabilitiesAttributes(_attributesBuilder.build());

                AttributeOptionalFields fields = _attributeBuilder.build();
                assertEquals(fields,
                        AttributeFactory.createCapabilities(Version.Version1).getAttributeOptionalFields());

                capability = new CapabilitiesBuilder();
                capability.setCode(CapabilityType.Ipv6Unicast);
                capabilities.add(0, capability.build());
                _attributesBuilder.setCapabilities(capabilities);
                _attributeBuilder.setCapabilitiesAttributes(_attributesBuilder.build());
                fields = _attributeBuilder.build();
                assertEquals(fields,
                        AttributeFactory.createCapabilities(Version.Version2).getAttributeOptionalFields());

                capability = new CapabilitiesBuilder();
                capability.setCode(CapabilityType.SubnetBindings);
                capabilities.add(0, capability.build());
                _attributesBuilder.setCapabilities(capabilities);
                _attributeBuilder.setCapabilitiesAttributes(_attributesBuilder.build());
                fields = _attributeBuilder.build();
                assertEquals(fields,
                        AttributeFactory.createCapabilities(Version.Version3).getAttributeOptionalFields());

                fields = _attributeBuilder.build();
                assertEquals(fields,
                        AttributeFactory.createCapabilities(Version.Version4).getAttributeOptionalFields());
        }

        @Test public void testCreateHoldTime() throws Exception {
                Attribute attribute = AttributeFactory.createHoldTime(50);
                assertEquals(AttributeType.HoldTime, attribute.getType());
                assertEquals(AttributeVariant.Compact, attribute.getAttributeVariant());
                HoldTimeAttribute holdTimeAttribute = (HoldTimeAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(holdTimeAttribute);
                assertEquals(50l, (long) holdTimeAttribute.getHoldTimeAttributes().getHoldTimeMinValue());

                attribute = AttributeFactory.createHoldTime(0);
                assertEquals(AttributeType.HoldTime, attribute.getType());
                assertEquals(AttributeVariant.Compact, attribute.getAttributeVariant());
                holdTimeAttribute = (HoldTimeAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(holdTimeAttribute);
                assertEquals(0l, (long) holdTimeAttribute.getHoldTimeAttributes().getHoldTimeMinValue());

                attribute = AttributeFactory.createHoldTime(0, 150);
                assertEquals(AttributeType.HoldTime, attribute.getType());
                assertEquals(AttributeVariant.Compact, attribute.getAttributeVariant());

                holdTimeAttribute = (HoldTimeAttribute) attribute.getAttributeOptionalFields();
                assertEquals(0l, (long) holdTimeAttribute.getHoldTimeAttributes().getHoldTimeMinValue());
                assertEquals(0l, (long) holdTimeAttribute.getHoldTimeAttributes().getHoldTimeMaxValue());

                attribute = AttributeFactory.createHoldTime(50, 150);
                assertEquals(AttributeType.HoldTime, attribute.getType());
                assertEquals(AttributeVariant.Compact, attribute.getAttributeVariant());

                holdTimeAttribute = (HoldTimeAttribute) attribute.getAttributeOptionalFields();
                assertEquals(50l, (long) holdTimeAttribute.getHoldTimeAttributes().getHoldTimeMinValue());
                assertEquals(150l, (long) holdTimeAttribute.getHoldTimeAttributes().getHoldTimeMaxValue());
        }

        @Test public void testCreateHoldTimeException0() throws Exception {
                exception.expect(HoldTimeMinException.class);
                AttributeFactory.createHoldTime(2);
        }

        @Test public void testCreateHoldTimeException1() throws Exception {
                exception.expect(HoldTimeMinException.class);
                AttributeFactory.createHoldTime(2, 50);
        }

        @Test public void testCreateHoldTimeException2() throws Exception {
                exception.expect(HoldTimeMaxException.class);
                AttributeFactory.createHoldTime(25, 5);
        }

        @Test public void testCreateIpv4AddPrefix() throws Exception {
                List<IpPrefix> ipPrefixes = new ArrayList<>();
                ipPrefixes.add(new IpPrefix("127.0.0.0/32".toCharArray()));
                Attribute attribute = AttributeFactory.createIpv4AddPrefix(ipPrefixes);
                assertEquals(AttributeType.Ipv4AddPrefix, attribute.getType());
                assertEquals(AttributeVariant.Compact, attribute.getAttributeVariant());

                Ipv4AddPrefixAttribute
                        ipv4AddPrefixAttribute =
                        (Ipv4AddPrefixAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(ipv4AddPrefixAttribute);
                assertEquals(ipPrefixes, ipv4AddPrefixAttribute.getIpv4AddPrefixAttributes().getIpPrefix());

                for (int i = 1; i <= 256; i++) {
                        String ip = "127.0." + ((i > 254) ? i - 254 : 0) + "." + i % 255 + "/32";
                        ipPrefixes.add(new IpPrefix(ip.toCharArray()));
                }

                attribute = AttributeFactory.createIpv4AddPrefix(ipPrefixes);
                assertEquals(AttributeType.Ipv4AddPrefix, attribute.getType());
                assertEquals(AttributeVariant.CompactExtendedLength, attribute.getAttributeVariant());

                ipv4AddPrefixAttribute = (Ipv4AddPrefixAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(ipv4AddPrefixAttribute);
                assertEquals(ipPrefixes, ipv4AddPrefixAttribute.getIpv4AddPrefixAttributes().getIpPrefix());
        }

        @Test public void testCreateIpv4DeletePrefix() throws Exception {
                List<IpPrefix> ipPrefixes = new ArrayList<>();
                ipPrefixes.add(new IpPrefix("127.0.0.0/32".toCharArray()));
                Attribute attribute = AttributeFactory.createIpv4DeletePrefix(ipPrefixes);
                assertEquals(AttributeType.Ipv4DeletePrefix, attribute.getType());
                assertEquals(AttributeVariant.Compact, attribute.getAttributeVariant());

                Ipv4DeletePrefixAttribute
                        ipv4DeletePrefixAttribute =
                        (Ipv4DeletePrefixAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(ipv4DeletePrefixAttribute);
                assertEquals(ipPrefixes, ipv4DeletePrefixAttribute.getIpv4DeletePrefixAttributes().getIpPrefix());

                for (int i = 1; i <= 256; i++) {
                        String ip = "127.0." + ((i > 254) ? i - 254 : 0) + "." + i % 255 + "/32";
                        ipPrefixes.add(new IpPrefix(ip.toCharArray()));
                }

                attribute = AttributeFactory.createIpv4DeletePrefix(ipPrefixes);
                assertEquals(AttributeType.Ipv4DeletePrefix, attribute.getType());
                assertEquals(AttributeVariant.CompactExtendedLength, attribute.getAttributeVariant());

                ipv4DeletePrefixAttribute = (Ipv4DeletePrefixAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(ipv4DeletePrefixAttribute);
                assertEquals(ipPrefixes, ipv4DeletePrefixAttribute.getIpv4DeletePrefixAttributes().getIpPrefix());
        }

        @Test public void testCreateIpv6AddPrefix() throws Exception {
                List<IpPrefix> ipPrefixes = new ArrayList<>();
                ipPrefixes.add(new IpPrefix("127.0.0.0/32".toCharArray()));
                Attribute attribute = AttributeFactory.createIpv6AddPrefix(ipPrefixes);
                assertEquals(AttributeType.Ipv6AddPrefix, attribute.getType());
                assertEquals(AttributeVariant.Compact, attribute.getAttributeVariant());

                Ipv6AddPrefixAttribute
                        ipv6AddPrefixAttribute =
                        (Ipv6AddPrefixAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(ipv6AddPrefixAttribute);
                assertEquals(ipPrefixes, ipv6AddPrefixAttribute.getIpv6AddPrefixAttributes().getIpPrefix());

                for (int i = 1; i <= 256; i++) {
                        String ip = "127.0." + ((i > 254) ? i - 254 : 0) + "." + i % 255 + "/32";
                        ipPrefixes.add(new IpPrefix(ip.toCharArray()));
                }

                attribute = AttributeFactory.createIpv6AddPrefix(ipPrefixes);
                assertEquals(AttributeType.Ipv6AddPrefix, attribute.getType());
                assertEquals(AttributeVariant.CompactExtendedLength, attribute.getAttributeVariant());

                ipv6AddPrefixAttribute = (Ipv6AddPrefixAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(ipv6AddPrefixAttribute);
                assertEquals(ipPrefixes, ipv6AddPrefixAttribute.getIpv6AddPrefixAttributes().getIpPrefix());
        }

        @Test public void testCreateIpv6DeletePrefix() throws Exception {
                List<IpPrefix> ipPrefixes = new ArrayList<>();
                ipPrefixes.add(new IpPrefix("127.0.0.0/32".toCharArray()));
                Attribute attribute = AttributeFactory.createIpv6DeletePrefix(ipPrefixes);
                assertEquals(AttributeType.Ipv6DeletePrefix, attribute.getType());
                assertEquals(AttributeVariant.Compact, attribute.getAttributeVariant());

                Ipv6DeletePrefixAttribute
                        ipv6DeletePrefixAttribute =
                        (Ipv6DeletePrefixAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(ipv6DeletePrefixAttribute);
                assertEquals(ipPrefixes, ipv6DeletePrefixAttribute.getIpv6DeletePrefixAttributes().getIpPrefix());

                for (int i = 1; i <= 256; i++) {
                        String ip = "127.0." + ((i > 254) ? i - 254 : 0) + "." + i % 255 + "/32";
                        ipPrefixes.add(new IpPrefix(ip.toCharArray()));
                }

                attribute = AttributeFactory.createIpv6DeletePrefix(ipPrefixes);
                assertEquals(AttributeType.Ipv6DeletePrefix, attribute.getType());
                assertEquals(AttributeVariant.CompactExtendedLength, attribute.getAttributeVariant());

                ipv6DeletePrefixAttribute = (Ipv6DeletePrefixAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(ipv6DeletePrefixAttribute);
                assertEquals(ipPrefixes, ipv6DeletePrefixAttribute.getIpv6DeletePrefixAttributes().getIpPrefix());
        }

        @Test public void testCreatePeerSequence() throws Exception {
                List<NodeId> nodeIdList = new ArrayList<>();
                nodeIdList.add(NodeIdConv.createNodeId("192.168.0.1"));
                nodeIdList.add(NodeIdConv.createNodeId("192.168.5.1"));

                Attribute attribute = AttributeFactory.createPeerSequence(nodeIdList);
                assertEquals(AttributeType.PeerSequence, attribute.getType());
                assertEquals(AttributeVariant.Compact, attribute.getAttributeVariant());
                PeerSequenceAttribute
                        peerSequenceAttribute =
                        (PeerSequenceAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(peerSequenceAttribute);
                assertEquals(nodeIdList, peerSequenceAttribute.getPeerSequenceAttributes().getNodeId());
        }

        @Test public void testCreateSourceGroupTag() throws Exception {
                Attribute attribute = AttributeFactory.createSourceGroupTag(5000);
                assertEquals(AttributeType.SourceGroupTag, attribute.getType());
                assertEquals(AttributeVariant.Compact, attribute.getAttributeVariant());
                SourceGroupTagAttribute
                        sxpNodeIdAttribute =
                        (SourceGroupTagAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(sxpNodeIdAttribute);
                assertEquals(5000l, (long) sxpNodeIdAttribute.getSourceGroupTagAttributes().getSgt());
        }

        @Test public void testCreateSourceGroupTagException0() throws Exception {
                exception.expect(SecurityGroupTagValueException.class);
                AttributeFactory.createSourceGroupTag(0);
        }

        @Test public void testCreateSourceGroupTagException1() throws Exception {
                exception.expect(SecurityGroupTagValueException.class);
                AttributeFactory.createSourceGroupTag(65520);
        }

        @Test public void testCreateSxpNodeId() throws Exception {
                NodeId nodeId = NodeIdConv.createNodeId("192.168.0.1");
                Attribute attribute = AttributeFactory.createSxpNodeId(nodeId);
                assertEquals(AttributeType.SxpNodeId, attribute.getType());
                assertEquals(AttributeVariant.Compact, attribute.getAttributeVariant());
                SxpNodeIdAttribute sxpNodeIdAttribute = (SxpNodeIdAttribute) attribute.getAttributeOptionalFields();
                assertNotNull(sxpNodeIdAttribute);
                assertEquals(nodeId, sxpNodeIdAttribute.getSxpNodeIdAttributes().getNodeId());
        }

        @Test public void testEncodeCapability() throws Exception {
                //Version4,Version3:
                CapabilitiesBuilder capability = new CapabilitiesBuilder();
                capability.setCode(CapabilityType.SubnetBindings);
                capability.setValue(new byte[2]);
                byte[] message = AttributeFactory.encodeCapability(capability.build());
                assertArrayEquals(new byte[] {3, 0, 0, 0, 0, 0}, message);
                //Version2:
                capability.setCode(CapabilityType.Ipv6Unicast);
                message = AttributeFactory.encodeCapability(capability.build());
                assertArrayEquals(new byte[] {2, 0, 0, 0, 0, 0}, message);
                //Version1:
                capability.setCode(CapabilityType.Ipv4Unicast);
                message = AttributeFactory.encodeCapability(capability.build());
                assertArrayEquals(new byte[] {1, 0, 0, 0, 0, 0}, message);

                capability.setValue(new byte[300]);
                exception.expect(CapabilityLengthException.class);
                AttributeFactory.encodeCapability(capability.build());

        }

        @Test public void testDecode() throws Exception {
                assertEquals(AttributeType.Capabilities,
                        AttributeFactory.decode(new byte[] {80, 6, 6, 3, 0, 2, 0, 1, 0}).getType());

                assertEquals(AttributeType.SourceGroupTag,
                        AttributeFactory.decode(new byte[] {16, 17, 2, 0, 20}).getType());

                assertEquals(AttributeType.HoldTime, AttributeFactory.decode(new byte[] {80, 7, 2, 0, 10}).getType());

                assertEquals(AttributeType.SxpNodeId,
                        AttributeFactory.decode(new byte[] {80, 5, 4, 0, 0, 0, 0}).getType());

                assertEquals(AttributeType.PeerSequence,
                        AttributeFactory.decode(new byte[] {16, 16, 4, 0, 0, 0, 0}).getType());

                assertEquals(AttributeType.Ipv4AddPrefix, AttributeFactory.decode(new byte[] {16, 11, 0}).getType());

                assertEquals(AttributeType.Ipv6AddPrefix, AttributeFactory.decode(new byte[] {16, 12, 0}).getType());

                assertEquals(AttributeType.Ipv4DeletePrefix, AttributeFactory.decode(new byte[] {16, 13, 0}).getType());

                assertEquals(AttributeType.Ipv6DeletePrefix, AttributeFactory.decode(new byte[] {16, 14, 0}).getType());

                assertEquals(AttributeType.Ipv4AddTable, AttributeFactory.decode(new byte[] {16, 21, 0}).getType());

                assertEquals(AttributeType.Ipv6AddTable, AttributeFactory.decode(new byte[] {16, 22, 0}).getType());
        }

        @Test public void testDecodeException0() throws Exception {
                exception.expect(AttributeLengthException.class);
                AttributeFactory.decode(new byte[] {24, 6, 0, 0});
        }

        @Test public void testDecodeException1() throws Exception {
                exception.expect(AttributeLengthException.class);
                AttributeFactory.decode(new byte[] {0, 0, 0, 0, 0, 1, 0, 0});
        }
}
