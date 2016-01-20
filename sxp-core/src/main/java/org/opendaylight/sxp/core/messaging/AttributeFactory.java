/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.messaging.legacy.LegacyAttributeFactory;
import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.message.attribute.AddressLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeVariantException;
import org.opendaylight.sxp.util.exception.message.attribute.CapabilityLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.HoldTimeMaxException;
import org.opendaylight.sxp.util.exception.message.attribute.HoldTimeMinException;
import org.opendaylight.sxp.util.exception.message.attribute.SecurityGroupTagValueException;
import org.opendaylight.sxp.util.exception.message.attribute.TlvNotFoundException;
import org.opendaylight.sxp.util.exception.unknown.UnknownNodeIdException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeVariant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.FlagsFields.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.AttributeOptionalFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.CapabilitiesAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.HoldTimeAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.Ipv4AddPrefixAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.Ipv4DeletePrefixAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.Ipv6AddPrefixAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.Ipv6DeletePrefixAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.PeerSequenceAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.SourceGroupTagAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.SxpNodeIdAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.capabilities.attribute.CapabilitiesAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.capabilities.attribute.capabilities.attributes.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.capabilities.attribute.capabilities.attributes.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.hold.time.attribute.HoldTimeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.ipv4.add.prefix.attribute.Ipv4AddPrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.ipv4.delete.prefix.attribute.Ipv4DeletePrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.ipv6.add.prefix.attribute.Ipv6AddPrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.ipv6.delete.prefix.attribute.Ipv6DeletePrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.peer.sequence.attribute.PeerSequenceAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.source.group.tag.attribute.SourceGroupTagAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.sxp.node.id.attribute.SxpNodeIdAttributesBuilder;

/**
 * AttributeFactory class contains logic for decoding and encoding Attributes
 */
public final class AttributeFactory {

    /** ONPCE000 (Optional-NonTransitive-Partial-Compact-ExtendedLength-0-0-0) */
    public static final byte _onpCe = 16;
    public static final byte _OnpCe = -112;
    public static final byte _oNpCe = 80;

    /**
     * Decode Capabilities from Byte Array
     *
     * @param array Byte Array containing Capabilities
     * @return Decoded Capabilities
     */
    private static List<Capabilities> _decodeCapabilities(byte[] array) {
        List<Capabilities> _capabilities = new ArrayList<>();
        while (array != null && array.length != 0) {
            Capabilities capability = _decodeCapability(array);
            _capabilities.add(capability);
            array = ArraysUtil.readBytes(array, capability.getBytesLength());
        }
        return _capabilities;
    }

    /**
     * Decode Capability from Byte Array
     *
     * @param array Byte Array containing Capability
     * @return Decoded Capabilities
     */
    private static Capabilities _decodeCapability(byte[] array) {
        int code = ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 0, 1));
        short length = (short) ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 1, 1));
        byte[] value = length == 0 ? new byte[0] : ArraysUtil.readBytes(array, 4, length);
        CapabilitiesBuilder capabilitiesBuilder = new CapabilitiesBuilder();
        capabilitiesBuilder.setCode(CapabilityType.forValue(code));
        capabilitiesBuilder.setLength(length);
        capabilitiesBuilder.setValue(value);
        capabilitiesBuilder.setBytesLength((short) (1 + 1 + value.length));
        return capabilitiesBuilder.build();
    }

    /**
     * Create Capability Attribute according to provided version
     *
     * @param version Version used
     * @return Attribute with Capabilities unique to each version
     * @throws UnknownVersionException   If version isn't supported
     * @throws CapabilityLengthException If some Attributes has incorrect length
     */
    public static Attribute createCapabilities(Version version)
            throws UnknownVersionException, CapabilityLengthException {
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setFlags(getFlags(_oNpCe));
        attributeBuilder.setAttributeVariant(AttributeVariant.Compact);
        attributeBuilder.setType(AttributeType.Capabilities);

        List<Capabilities> capabilities = getCapabilities(version);
        byte[] value = encodeCapabilities(capabilities);
        attributeBuilder.setLength(value.length);
        attributeBuilder.setValue(value);

        CapabilitiesAttributeBuilder _attributeBuilder = new CapabilitiesAttributeBuilder();
        CapabilitiesAttributesBuilder _attributesBuilder = new CapabilitiesAttributesBuilder();
        _attributesBuilder.setCapabilities(capabilities);
        _attributeBuilder.setCapabilitiesAttributes(_attributesBuilder.build());
        attributeBuilder.setAttributeOptionalFields(_attributeBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Create HoldTime Attribute using provided value
     *
     * @param holdTimeMin Value used in attribute
     * @return New HoldTime attribute
     * @throws HoldTimeMinException If Hold time value isn't [0, 65535].
     */
    public static Attribute createHoldTime(int holdTimeMin) throws HoldTimeMinException {
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setFlags(getFlags(_oNpCe));
        attributeBuilder.setAttributeVariant(AttributeVariant.Compact);
        attributeBuilder.setType(AttributeType.HoldTime);

        byte[] value = null;
        int holdTimeMax;
        if (holdTimeMin == 0) {
            value = new byte[] { (byte) 0xFF, (byte) 0xFF };
            holdTimeMax = 0;
        }
        // Unsigned integer <0,65535>. 0 or at least 3 seconds.
        else if (holdTimeMin < 0 || 65535 < holdTimeMin || holdTimeMin < 3) {
            throw new HoldTimeMinException(holdTimeMin);
        } else {
            value = ArraysUtil.combine(ArraysUtil.int2bytesCropp(holdTimeMin, 2));
            holdTimeMax = 0;
        }
        attributeBuilder.setLength(value.length);
        attributeBuilder.setValue(value);

        HoldTimeAttributeBuilder _attributeBuilder = new HoldTimeAttributeBuilder();
        HoldTimeAttributesBuilder _attributesBuilder = new HoldTimeAttributesBuilder();
        _attributesBuilder.setHoldTimeMinValue(holdTimeMin);
        _attributesBuilder.setHoldTimeMaxValue(holdTimeMax);
        _attributeBuilder.setHoldTimeAttributes(_attributesBuilder.build());
        attributeBuilder.setAttributeOptionalFields(_attributeBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Create HoldTime Attribute using provided values
     *
     * @param holdTimeMin Minimal Hold time
     * @param holdTimeMax Maximal Hold time
     * @return New HoldTime attribute
     * @throws HoldTimeMinException If value is lower than 3 or greater than 65535
     * @throws HoldTimeMaxException If value is lower than minimal Hold time
     */
    public static Attribute createHoldTime(int holdTimeMin, int holdTimeMax)
            throws HoldTimeMinException, HoldTimeMaxException {
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setFlags(getFlags(_oNpCe));
        attributeBuilder.setAttributeVariant(AttributeVariant.Compact);
        attributeBuilder.setType(AttributeType.HoldTime);

        byte[] value = null;
        if (holdTimeMin == 0 || holdTimeMax == 0) {
            value = new byte[] { (byte) 0xFF, (byte) 0xFF, 0x00, 0x00 };
            holdTimeMin = 0;
            holdTimeMax = 0;
        } else {
            // Unsigned integer <0,65535>. 0 or at least 3 seconds.
            if (holdTimeMin != 0 && (holdTimeMin < 3 || 65535 < holdTimeMin)) {
                throw new HoldTimeMinException(holdTimeMin);
            } else if (holdTimeMin != 0 && holdTimeMax != 0 && holdTimeMax <= holdTimeMin) {
                throw new HoldTimeMaxException(holdTimeMin, holdTimeMax);
            } else {
                value = ArraysUtil.combine(ArraysUtil.int2bytesCropp(holdTimeMin, 2),
                        ArraysUtil.int2bytesCropp(holdTimeMax, 2));
            }
        }
        attributeBuilder.setLength(value.length);
        attributeBuilder.setValue(value);

        HoldTimeAttributeBuilder _attributeBuilder = new HoldTimeAttributeBuilder();
        HoldTimeAttributesBuilder _attributesBuilder = new HoldTimeAttributesBuilder();
        _attributesBuilder.setHoldTimeMinValue(holdTimeMin);
        _attributesBuilder.setHoldTimeMaxValue(holdTimeMax);
        _attributeBuilder.setHoldTimeAttributes(_attributesBuilder.build());
        attributeBuilder.setAttributeOptionalFields(_attributeBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Create AddIpv4Prefix Attribute using provided prefixes
     *
     * @param prefixes Prefixes to be used in attribute
     * @return Attribute containing AddIpv4 prefixes
     */
    public static Attribute createIpv4AddPrefix(List<IpPrefix> prefixes, byte flags) {
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setType(AttributeType.Ipv4AddPrefix);

        byte[] value = IpPrefixConv.toBytes(prefixes);
        if (value.length < 256) {
            attributeBuilder.setFlags(getFlags(flags));
            attributeBuilder.setAttributeVariant(AttributeVariant.Compact);
        } else {
            attributeBuilder.setFlags(getFlags((byte) (flags + 8)));
            attributeBuilder.setAttributeVariant(AttributeVariant.CompactExtendedLength);
        }
        attributeBuilder.setLength(value.length);
        attributeBuilder.setValue(value);

        Ipv4AddPrefixAttributeBuilder _attributeBuilder = new Ipv4AddPrefixAttributeBuilder();
        Ipv4AddPrefixAttributesBuilder _attributesBuilder = new Ipv4AddPrefixAttributesBuilder();
        _attributesBuilder.setIpPrefix(prefixes);
        _attributeBuilder.setIpv4AddPrefixAttributes(_attributesBuilder.build());
        attributeBuilder.setAttributeOptionalFields(_attributeBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Create DeleteIpv4Prefix Attribute using provided prefixes
     *
     * @param prefixes Prefixes to be used in attribute
     * @return Attribute containing DeleteIpv4 prefixes
     */
    public static Attribute createIpv4DeletePrefix(List<IpPrefix> prefixes, byte flags) {
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setType(AttributeType.Ipv4DeletePrefix);

        byte[] value = IpPrefixConv.toBytes(prefixes);
        if (value.length < 256) {
            attributeBuilder.setFlags(getFlags(flags));
            attributeBuilder.setAttributeVariant(AttributeVariant.Compact);
        } else {
            attributeBuilder.setFlags(getFlags((byte) (flags + 8)));
            attributeBuilder.setAttributeVariant(AttributeVariant.CompactExtendedLength);
        }
        attributeBuilder.setLength(value.length);
        attributeBuilder.setValue(value);

        Ipv4DeletePrefixAttributeBuilder _attributeBuilder = new Ipv4DeletePrefixAttributeBuilder();
        Ipv4DeletePrefixAttributesBuilder _attributesBuilder = new Ipv4DeletePrefixAttributesBuilder();
        _attributesBuilder.setIpPrefix(prefixes);
        _attributeBuilder.setIpv4DeletePrefixAttributes(_attributesBuilder.build());
        attributeBuilder.setAttributeOptionalFields(_attributeBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Create AddIpv6Prefix Attribute using provided prefixes
     *
     * @param prefixes Prefixes to be used in attribute
     * @return Attribute containing AddIpv6 prefixes
     */
    public static Attribute createIpv6AddPrefix(List<IpPrefix> prefixes, byte flags) {
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setType(AttributeType.Ipv6AddPrefix);

        byte[] value = IpPrefixConv.toBytes(prefixes);
        if (value.length < 256) {
            attributeBuilder.setFlags(getFlags(flags));
            attributeBuilder.setAttributeVariant(AttributeVariant.Compact);
        } else {
            attributeBuilder.setFlags(getFlags((byte) (flags + 8)));
            attributeBuilder.setAttributeVariant(AttributeVariant.CompactExtendedLength);
        }

        attributeBuilder.setLength(value.length);
        attributeBuilder.setValue(value);

        Ipv6AddPrefixAttributeBuilder _attributeBuilder = new Ipv6AddPrefixAttributeBuilder();
        Ipv6AddPrefixAttributesBuilder _attributesBuilder = new Ipv6AddPrefixAttributesBuilder();
        _attributesBuilder.setIpPrefix(prefixes);
        _attributeBuilder.setIpv6AddPrefixAttributes(_attributesBuilder.build());
        attributeBuilder.setAttributeOptionalFields(_attributeBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Create DeleteIpv6Prefix Attribute using provided prefixes
     *
     * @param prefixes Prefixes to be used in attribute
     * @return Attribute containing DeleteIpv6 prefixes
     */
    public static Attribute createIpv6DeletePrefix(List<IpPrefix> prefixes, byte flags) {
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setType(AttributeType.Ipv6DeletePrefix);

        byte[] value = IpPrefixConv.toBytes(prefixes);
        if (value.length < 256) {
            attributeBuilder.setFlags(getFlags(flags));
            attributeBuilder.setAttributeVariant(AttributeVariant.Compact);
        } else {
            attributeBuilder.setFlags(getFlags((byte) (flags + 8)));
            attributeBuilder.setAttributeVariant(AttributeVariant.CompactExtendedLength);
        }
        attributeBuilder.setLength(value.length);
        attributeBuilder.setValue(value);

        Ipv6DeletePrefixAttributeBuilder _attributeBuilder = new Ipv6DeletePrefixAttributeBuilder();
        Ipv6DeletePrefixAttributesBuilder _attributesBuilder = new Ipv6DeletePrefixAttributesBuilder();
        _attributesBuilder.setIpPrefix(prefixes);
        _attributeBuilder.setIpv6DeletePrefixAttributes(_attributesBuilder.build());
        attributeBuilder.setAttributeOptionalFields(_attributeBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Create PeerSequence Attribute using provided NodeIds
     *
     * @param nodesIds NodeIds to be used in attribute
     * @return Attribute containing Peer sequence
     */
    public static Attribute createPeerSequence(List<NodeId> nodesIds) {
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setFlags(getFlags(_onpCe));
        attributeBuilder.setAttributeVariant(AttributeVariant.Compact);
        attributeBuilder.setType(AttributeType.PeerSequence);

        byte[] value = NodeIdConv.toBytes(nodesIds);
        attributeBuilder.setLength(value.length);
        attributeBuilder.setValue(value);

        PeerSequenceAttributeBuilder _attributeBuilder = new PeerSequenceAttributeBuilder();
        PeerSequenceAttributesBuilder _attributesBuilder = new PeerSequenceAttributesBuilder();
        _attributesBuilder.setNodeId(nodesIds);
        _attributeBuilder.setPeerSequenceAttributes(_attributesBuilder.build());
        attributeBuilder.setAttributeOptionalFields(_attributeBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Creates Sgt Attribute using provided value
     *
     * @param sgt Value of Sgt to be used in attribute
     * @return Attribute containing Sgt
     * @throws SecurityGroupTagValueException If Sgt isn't in rage [2,65519]
     */
    public static Attribute createSourceGroupTag(int sgt) throws SecurityGroupTagValueException {
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setFlags(getFlags(_onpCe));
        attributeBuilder.setAttributeVariant(AttributeVariant.Compact);
        attributeBuilder.setType(AttributeType.SourceGroupTag);

        if (sgt < 2 || 65519 < sgt) {
            throw new SecurityGroupTagValueException();
        }
        byte[] value = ArraysUtil.int2bytesCropp(sgt, 2);
        if (!Configuration.SET_COMPOSITION_ATTRIBUTE_COMPACT_NO_RESERVED_FIELDS) {
            value = ArraysUtil.combine(value, new byte[] { 0x00, 0x00 });
        }
        attributeBuilder.setLength(value.length);
        attributeBuilder.setValue(value);

        SourceGroupTagAttributeBuilder _attributeBuilder = new SourceGroupTagAttributeBuilder();
        SourceGroupTagAttributesBuilder _attributesBuilder = new SourceGroupTagAttributesBuilder();
        _attributesBuilder.setSgt(sgt);
        _attributeBuilder.setSourceGroupTagAttributes(_attributesBuilder.build());
        attributeBuilder.setAttributeOptionalFields(_attributeBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Create NodeId Attribute using provided value
     *
     * @param nodeId NodeId used in attribute
     * @return Attribute containing NodeID
     */
    public static Attribute createSxpNodeId(NodeId nodeId) {
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setFlags(getFlags(_oNpCe));
        attributeBuilder.setAttributeVariant(AttributeVariant.Compact);
        attributeBuilder.setType(AttributeType.SxpNodeId);

        byte[] value = NodeIdConv.toBytes(nodeId);
        attributeBuilder.setLength(value.length);
        attributeBuilder.setValue(value);

        SxpNodeIdAttributeBuilder _attributeBuilder = new SxpNodeIdAttributeBuilder();
        SxpNodeIdAttributesBuilder _attributesBuilder = new SxpNodeIdAttributesBuilder();
        _attributesBuilder.setNodeId(nodeId);
        _attributeBuilder.setSxpNodeIdAttributes(_attributesBuilder.build());
        attributeBuilder.setAttributeOptionalFields(_attributeBuilder.build());
        return attributeBuilder.build();
    }

    protected static Attribute decode(byte[] array)
            throws AttributeLengthException, AddressLengthException, UnknownNodeIdException, UnknownPrefixException,
            TlvNotFoundException, UnknownHostException, AttributeVariantException {
        // 1 or 0 byte: O N P C E 0 0 0
        Flags flags = getFlags(array[0]);

        AttributeVariant variant = AttributeVariant.NonCompact;
        if (flags.isCompact()) {
            variant = AttributeVariant.Compact;
            if (flags.isExtendedLength()) {
                variant = AttributeVariant.CompactExtendedLength;
            }
        }

        AttributeType type = AttributeType.Unspecified;
        int length = 0;
        byte[] value = new byte[length];

        switch (variant) {
        case Compact:
            type = AttributeType.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 1, 1)));
            length = ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 2, 1));
            if (length < 0 || 255 < length) {
                throw new AttributeLengthException();
            }
            if (Configuration.SET_COMPOSITION_ATTRIBUTE_COMPACT_NO_RESERVED_FIELDS) {
                value = ArraysUtil.readBytes(array, 3, length);
                length += 3;
                break;
            }
            value = ArraysUtil.readBytes(array, 4, length);
            length += 4;
            break;
        case CompactExtendedLength:
            type = AttributeType.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 1, 1)));
            length = ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 2, 2));
            if (length < 256 || 4084 < length) {
                throw new AttributeLengthException();
            }
            value = ArraysUtil.readBytes(array, 4, length);
            length += 4;
            break;
        case NonCompact:
            type = AttributeType.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 1, 3)));
            length = ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 4, 4));
            if (length < 0 || 4080 < length) {
                throw new AttributeLengthException();
            }
            value = ArraysUtil.readBytes(array, 8, length);
            length += 8;
            break;
        default:
            variant = AttributeVariant.None;
        }

        return decode(flags, variant, type, length, value);
    }

    /**
     * Decode specific Attribute according to provided data
     *
     * @param flags   Flags to be set
     * @param variant Variant of attribute
     * @param type    Type of attribute that will be decoded
     * @param length  Length of attribute
     * @param value   Data that will be decoded
     * @return Attribute with specific type and data
     * @throws AddressLengthException If length of Attribute is incorrect
     * @throws TlvNotFoundException   If Attribute doesn't have Tlv
     * @throws UnknownPrefixException If Attribute has incorrect or none Prefix
     * @throws UnknownHostException   If address in Attribute is incorrect
     * @throws UnknownNodeIdException If Attribute doesn't have NodeId
     */
    private static Attribute decode(Flags flags, AttributeVariant variant, AttributeType type, int length, byte[] value)
            throws AddressLengthException, TlvNotFoundException, UnknownPrefixException, UnknownHostException,
            UnknownNodeIdException, AttributeVariantException {

        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setFlags(flags);
        attributeBuilder.setAttributeVariant(variant);
        attributeBuilder.setType(type);
        attributeBuilder.setLength(length);
        attributeBuilder.setValue(value);

        AttributeOptionalFields attributeOptionalFields;
        switch (type) {
        case AddIpv4:
            attributeOptionalFields = LegacyAttributeFactory.decodeAddIPv4(type, 4, value);
            break;
        case AddIpv6:
            attributeOptionalFields = LegacyAttributeFactory.decodeAddIPv6(type, 16, value);
            break;
        case Capabilities:
            attributeOptionalFields = decodeCapabilities(value);
            break;
        case DelIpv4:
            attributeOptionalFields = LegacyAttributeFactory.decodeDeleteIPv4(type, 4, value);
            break;
        case DelIpv6:
            attributeOptionalFields = LegacyAttributeFactory.decodeDeleteIPv6(type, 16, value);
            break;
        case HoldTime:
            if (!flags.isNonTransitive()) {
                throw new AttributeVariantException();
            }
            attributeOptionalFields = decodeHoldTime(value);
            break;
        case Ipv4AddPrefix:
            attributeOptionalFields = decodeIpv4AddPrefix(value, flags.isCompact());
            break;
        case Ipv4DeletePrefix:
            attributeOptionalFields = decodeIpv4DeletePrefix(value, flags.isCompact());
            break;
        case Ipv6AddPrefix:
            attributeOptionalFields = decodeIpv6AddPrefix(value, flags.isCompact());
            break;
        case Ipv6DeletePrefix:
            attributeOptionalFields = decodeIpv6DeletePrefix(value, flags.isCompact());
            break;
        case PeerSequence:
            attributeOptionalFields = decodePeerSequence(value);
            break;
        case SourceGroupTag:
            attributeOptionalFields = decodeSourceGroupTag(value);
            break;
        case SxpNodeId:
            attributeOptionalFields = decodeSxpNodeId(value);
            break;
        default:
            attributeOptionalFields = decodeUnrecognized(value);
            break;
        }

        attributeBuilder.setAttributeOptionalFields(attributeOptionalFields);
        return attributeBuilder.build();
    }

    /**
     * Decode Capabilities from Byte Array
     *
     * @param value Byte Array containing Capabilities
     * @return Decoded Capabilities
     */
    private static AttributeOptionalFields decodeCapabilities(byte[] value) {
        CapabilitiesAttributeBuilder attributeBuilder = new CapabilitiesAttributeBuilder();
        CapabilitiesAttributesBuilder attributesBuilder = new CapabilitiesAttributesBuilder();
        attributesBuilder.setCapabilities(_decodeCapabilities(value));
        attributeBuilder.setCapabilitiesAttributes(attributesBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Decode HoldTime attribute from Byte Array
     *
     * @param value Byte Array containing HoldTime attribute
     * @return Decoded HoldTime attribute
     */
    private static AttributeOptionalFields decodeHoldTime(byte[] value) {
        int holdTimeMin = 0, holdTimeMax = 0;
        if (value.length > 1 && value[0] != (byte) 0xFF && value[1] != (byte) 0xFF) {
            holdTimeMin = ArraysUtil.bytes2int(new byte[] {value[0], value[1]});
        }
        if (value.length > 3 && value[2] != (byte) 0xFF && value[3] != (byte) 0xFF) {
            holdTimeMax = ArraysUtil.bytes2int(new byte[] {value[2], value[3]});
        }
        HoldTimeAttributeBuilder attributeBuilder = new HoldTimeAttributeBuilder();
        HoldTimeAttributesBuilder attributesBuilder = new HoldTimeAttributesBuilder();
        attributesBuilder.setHoldTimeMinValue(holdTimeMin);
        attributesBuilder.setHoldTimeMaxValue(holdTimeMax);
        attributeBuilder.setHoldTimeAttributes(attributesBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Decode AddIpv4 prefix from Byte Array
     *
     * @param value   Byte Array containing attribute
     * @param compact If attribute is compact type
     * @return Decoded AddIpv4 prefix attribute
     * @throws UnknownHostException   If address in Attribute is incorrect
     * @throws UnknownPrefixException If Attribute has incorrect or none Prefix
     */
    private static AttributeOptionalFields decodeIpv4AddPrefix(byte[] value, boolean compact) throws UnknownHostException, UnknownPrefixException {
        Ipv4AddPrefixAttributeBuilder attributeBuilder = new Ipv4AddPrefixAttributeBuilder();
        Ipv4AddPrefixAttributesBuilder attributesBuilder = new Ipv4AddPrefixAttributesBuilder();
        attributesBuilder.setIpPrefix(IpPrefixConv.decodeIpv4(value, compact));
        attributeBuilder.setIpv4AddPrefixAttributes(attributesBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Decode DeleteIpv4 prefix from Byte Array
     *
     * @param value   Byte Array containing attribute
     * @param compact If attribute is compact type
     * @return Decoded DeleteIpv4 prefix attribute
     * @throws UnknownHostException   If address in Attribute is incorrect
     * @throws UnknownPrefixException If Attribute has incorrect or none Prefix
     */
    private static AttributeOptionalFields decodeIpv4DeletePrefix(byte[] value, boolean compact) throws UnknownHostException, UnknownPrefixException {
        Ipv4DeletePrefixAttributeBuilder attributeBuilder = new Ipv4DeletePrefixAttributeBuilder();
        Ipv4DeletePrefixAttributesBuilder attributesBuilder = new Ipv4DeletePrefixAttributesBuilder();
        attributesBuilder.setIpPrefix(IpPrefixConv.decodeIpv4(value, compact));
        attributeBuilder.setIpv4DeletePrefixAttributes(attributesBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Decode AddIpv6 prefix from Byte Array
     *
     * @param value   Byte Array containing attribute
     * @param compact If attribute is compact type
     * @return Decoded AddIpv6 prefix attribute
     * @throws UnknownHostException   If address in Attribute is incorrect
     * @throws UnknownPrefixException If Attribute has incorrect or none Prefix
     */
    private static AttributeOptionalFields decodeIpv6AddPrefix(byte[] value, boolean compact) throws UnknownHostException, UnknownPrefixException {
        Ipv6AddPrefixAttributeBuilder attributeBuilder = new Ipv6AddPrefixAttributeBuilder();
        Ipv6AddPrefixAttributesBuilder attributesBuilder = new Ipv6AddPrefixAttributesBuilder();
        attributesBuilder.setIpPrefix(IpPrefixConv.decodeIpv6(value, compact));
        attributeBuilder.setIpv6AddPrefixAttributes(attributesBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Decode DeleteIpv6 prefix from Byte Array
     *
     * @param value   Byte Array containing attribute
     * @param compact If attribute is compact type
     * @return Decoded DeleteIpv4 prefix attribute
     * @throws UnknownHostException   If address in Attribute is incorrect
     * @throws UnknownPrefixException If Attribute has incorrect or none Prefix
     */
    private static AttributeOptionalFields decodeIpv6DeletePrefix(byte[] value, boolean compact) throws UnknownHostException, UnknownPrefixException {
        Ipv6DeletePrefixAttributeBuilder attributeBuilder = new Ipv6DeletePrefixAttributeBuilder();
        Ipv6DeletePrefixAttributesBuilder attributesBuilder = new Ipv6DeletePrefixAttributesBuilder();
        attributesBuilder.setIpPrefix(IpPrefixConv.decodeIpv6(value, compact));
        attributeBuilder.setIpv6DeletePrefixAttributes(attributesBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Decode PeerSequence attribute from Byte Array
     *
     * @param value Byte Array containing PeerSequence attribute
     * @return Decoded PeerSequence attribute
     * @throws UnknownHostException      If address in Attribute is incorrect
     */
    private static AttributeOptionalFields decodePeerSequence(byte[] value) throws UnknownHostException, UnknownNodeIdException {
        PeerSequenceAttributeBuilder attributeBuilder = new PeerSequenceAttributeBuilder();
        PeerSequenceAttributesBuilder attributesBuilder = new PeerSequenceAttributesBuilder();
        attributesBuilder.setNodeId(NodeIdConv.decode(value));
        attributeBuilder.setPeerSequenceAttributes(attributesBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Decode Sgt attribute from Byre Array
     *
     * @param value Byte Array containing Sgt attribute
     * @return Decoded Sgt attribute
     */
    private static AttributeOptionalFields decodeSourceGroupTag(byte[] value) {
        SourceGroupTagAttributeBuilder attributeBuilder = new SourceGroupTagAttributeBuilder();
        SourceGroupTagAttributesBuilder attributesBuilder = new SourceGroupTagAttributesBuilder();
        attributesBuilder.setSgt(ArraysUtil.bytes2int(ArraysUtil.readBytes(value, 0, 2)));
        attributeBuilder.setSourceGroupTagAttributes(attributesBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * Decode NodeId attribute from Byte Array
     *
     * @param value Byte Array containing NodeId attribute
     * @return Decode NodeId attribute
     * @throws UnknownHostException   If address in Attribute is incorrect
     * @throws UnknownNodeIdException If address isn't in IPv4 format
     */
    private static AttributeOptionalFields decodeSxpNodeId(byte[] value) throws UnknownHostException, UnknownNodeIdException {
        SxpNodeIdAttributeBuilder attributeBuilder = new SxpNodeIdAttributeBuilder();
        SxpNodeIdAttributesBuilder attributesBuilder = new SxpNodeIdAttributesBuilder();
        attributesBuilder.setNodeId(NodeIdConv._decode(value));
        attributeBuilder.setSxpNodeIdAttributes(attributesBuilder.build());
        return attributeBuilder.build();
    }

    /**
     * @param value Unimportant
     * @return Gets null
     */
    private static AttributeOptionalFields decodeUnrecognized(byte[] value) {
        return null;
    }

    /**
     * Generate Byte Array representation of Capabilities
     *
     * @param capabilities Capabilities to be encoded
     * @return Encoded Capabilities
     * @throws CapabilityLengthException If one of Capabilities length isn't correct
     */
    private static byte[] encodeCapabilities(List<Capabilities> capabilities) throws CapabilityLengthException {
        byte[] _capabilities = new byte[0];
        for (Capabilities capabilityType : capabilities) {
            _capabilities = ArraysUtil.combine(_capabilities, encodeCapability(capabilityType));
        }
        return _capabilities;
    }

    /**
     * Generate Byte Array representation of Capability
     *
     * @param capability Capability to be encoded
     * @return Encoded Capability
     * @throws CapabilityLengthException If Capability length isn't correct
     */
    public static byte[] encodeCapability(Capabilities capability) throws CapabilityLengthException {
        byte code = (byte) capability.getCode().getIntValue();

        byte[] value = capability.getValue();
        if (value == null || value.length == 0) {
            return new byte[] { code, 0x00 };
        } else if (value.length < 0 || 255 < value.length) {
            throw new CapabilityLengthException();
        }

        byte length = ArraysUtil.int2bytes(value.length)[0];
        byte[] reserved = new byte[] { 0x00, 0x00 };
        return ArraysUtil.combine(new byte[] { code, length }, reserved, value);
    }

    /**
     * Create List of capabilities according to provided version
     *
     * @param version Version used
     * @return List of Capabilities unique to each version
     * @throws UnknownVersionException If version isn't supported
     */
    private static List<Capabilities> getCapabilities(Version version) throws UnknownVersionException {
        List<Capabilities> capabilities = new ArrayList<>();
        switch (version) {
        case Version4:
        case Version3:
            CapabilitiesBuilder capability = new CapabilitiesBuilder();
            capability.setCode(CapabilityType.SubnetBindings);
            capabilities.add(capability.build());
        case Version2:
            capability = new CapabilitiesBuilder();
            capability.setCode(CapabilityType.Ipv6Unicast);
            capabilities.add(capability.build());
        case Version1:
            capability = new CapabilitiesBuilder();
            capability.setCode(CapabilityType.Ipv4Unicast);
            capabilities.add(capability.build());
            break;
        default:
            throw new UnknownVersionException();
        }
        return capabilities;
    }

    /**
     * Converts Byte into Flags according to bit values
     *
     * @param flags Byte to be converted
     * @return Generated Flags
     */
    private static Flags getFlags(byte flags) {
        boolean o = ArraysUtil.getBit(flags, 8) == 1;
        boolean n = ArraysUtil.getBit(flags, 7) == 1;
        boolean p = ArraysUtil.getBit(flags, 6) == 1;
        boolean c = ArraysUtil.getBit(flags, 5) == 1;
        boolean e = ArraysUtil.getBit(flags, 4) == 1;
        return new Flags(c, e, n, o, p);
    }
}
