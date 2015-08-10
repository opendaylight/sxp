/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging.legacy;

import java.net.UnknownHostException;
import org.opendaylight.sxp.util.exception.message.attribute.AddressLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.TlvNotFoundException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.TlvType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.AttributeOptionalFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.AddIpv4AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.AddIpv6AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.DeleteIpv4AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.DeleteIpv6AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.add.ipv4.attribute.AddIpv4AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.add.ipv6.attribute.AddIpv6AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.delete.ipv4.attribute.DeleteIpv4AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.delete.ipv6.attribute.DeleteIpv6AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlv.fields.tlv.optional.fields.SourceGroupTagTlvAttribute;

public class LegacyAttributeFactory {

    /**
     * Create MappingRecord with attribute type AddIpv4
     * using defined values
     *
     * @param sgt    Sgt used
     * @param prefix IpPrefix used
     * @return MappingRecord created
     */
    public static MappingRecord createAddIpv4(Sgt sgt, IpPrefix prefix) {
        return org.opendaylight.sxp.core.messaging.legacy.MappingRecord.create(AttributeType.AddIpv4, prefix, sgt);
    }

    /**
     * Create MappingRecord with attribute type AddIpv6
     * using defined values
     *
     * @param sgt    Sgt used
     * @param prefix IpPrefix used
     * @return MappingRecord created
     */
    public static MappingRecord createAddIpv6(Sgt sgt, IpPrefix prefix) {
        return org.opendaylight.sxp.core.messaging.legacy.MappingRecord.create(AttributeType.AddIpv6, prefix, sgt);
    }

    /**
     * Create MappingRecord with attribute type DellIpv4
     * using defined values
     *
     * @param prefix IpPrefix used
     * @return MappingRecord created
     */
    public static MappingRecord createDeleteIpv4(IpPrefix prefix) {
        return org.opendaylight.sxp.core.messaging.legacy.MappingRecord.create(AttributeType.DelIpv4, prefix);
    }

    /**
     * Create MappingRecord with attribute type DellIpv6
     * using defined values
     *
     * @param prefix IpPrefix used
     * @return MappingRecord created
     */
    public static MappingRecord createDeleteIpv6(IpPrefix prefix) {
        return org.opendaylight.sxp.core.messaging.legacy.MappingRecord.create(AttributeType.DelIpv6, prefix);
    }

    /**
     * Decode AddIpv4 Attribute from byte Array
     *
     * @param length Length of attribute
     * @param value  Byte Array containing attribute
     * @return AttributeOptionalFields containing decoded AddIpv4
     * @throws UnknownHostException   If address in Array is incorrect
     * @throws AddressLengthException If address length is incorrect
     * @throws UnknownPrefixException If Prefix in Array is incorrect
     * @throws TlvNotFoundException   If Tvl was not found in decoded attribute
     */
    public static AttributeOptionalFields decodeAddIPv4(AttributeType operationCode, int length, byte[] value) throws UnknownHostException, AddressLengthException, UnknownPrefixException, TlvNotFoundException {
        MappingRecord mappingRecord = org.opendaylight.sxp.core.messaging.legacy.MappingRecord.decodeAddress(
                operationCode, length, value);
        int sgt = ((SourceGroupTagTlvAttribute) org.opendaylight.sxp.core.messaging.legacy.MappingRecord.create(
                mappingRecord.getTlv()).get(TlvType.Sgt)).getSourceGroupTagTlvAttributes().getSgt();

        AddIpv4AttributeBuilder _attributeBuilder = new AddIpv4AttributeBuilder();
        AddIpv4AttributesBuilder _attributesBuilder = new AddIpv4AttributesBuilder();
        _attributesBuilder.setIpPrefix(mappingRecord.getAddress());
        _attributesBuilder.setSgt(sgt);
        _attributeBuilder.setAddIpv4Attributes(_attributesBuilder.build());
        return _attributeBuilder.build();
    }

    /**
     * Decode AddIpv6 Attribute from byte Array
     *
     * @param length Length of attribute
     * @param value  Byte Array containing attribute
     * @return AttributeOptionalFields containing decoded AddIpv6
     * @throws UnknownHostException   If address in Array is incorrect
     * @throws AddressLengthException If address length is incorrect
     * @throws UnknownPrefixException If Prefix in Array is incorrect
     * @throws TlvNotFoundException   If Tvl was not found in decoded attribute
     */
    public static AttributeOptionalFields decodeAddIPv6(AttributeType operationCode, int length, byte[] value) throws UnknownHostException, AddressLengthException, UnknownPrefixException, TlvNotFoundException {
        MappingRecord mappingRecord = org.opendaylight.sxp.core.messaging.legacy.MappingRecord.decodeAddress(
                operationCode, length, value);
        int sgt = ((SourceGroupTagTlvAttribute) org.opendaylight.sxp.core.messaging.legacy.MappingRecord.create(
                mappingRecord.getTlv()).get(TlvType.Sgt)).getSourceGroupTagTlvAttributes().getSgt();

        AddIpv6AttributeBuilder _attributeBuilder = new AddIpv6AttributeBuilder();
        AddIpv6AttributesBuilder _attributesBuilder = new AddIpv6AttributesBuilder();
        _attributesBuilder.setIpPrefix(mappingRecord.getAddress());
        _attributesBuilder.setSgt(sgt);
        _attributeBuilder.setAddIpv6Attributes(_attributesBuilder.build());
        return _attributeBuilder.build();
    }

    /**
     * Decode DeleteIpv4 Attribute from byte Array
     *
     * @param length Length of attribute
     * @param value  Byte Array containing attribute
     * @return AttributeOptionalFields containing decoded DelIpv4
     * @throws UnknownHostException   If address in Array is incorrect
     * @throws AddressLengthException If address length is incorrect
     * @throws UnknownPrefixException If Prefix in Array is incorrect
     */
    public static AttributeOptionalFields decodeDeleteIPv4(AttributeType operationCode, int length, byte[] value) throws UnknownHostException, AddressLengthException, UnknownPrefixException {
        MappingRecord mappingRecord = org.opendaylight.sxp.core.messaging.legacy.MappingRecord.decodeAddress(
                operationCode, length, value);

        DeleteIpv4AttributeBuilder _attributeBuilder = new DeleteIpv4AttributeBuilder();
        DeleteIpv4AttributesBuilder _attributesBuilder = new DeleteIpv4AttributesBuilder();
        _attributesBuilder.setIpPrefix(mappingRecord.getAddress());
        _attributeBuilder.setDeleteIpv4Attributes(_attributesBuilder.build());
        return _attributeBuilder.build();
    }

    /**
     * Decode DeleteIpv6 Attribute from byte Array
     *
     * @param length Length of attribute
     * @param value  Byte Array containing attribute
     * @return AttributeOptionalFields containing decoded DelIpv6
     * @throws UnknownHostException   If address in Array is incorrect
     * @throws AddressLengthException If address length is incorrect
     * @throws UnknownPrefixException If Prefix in Array is incorrect
     */
    public static AttributeOptionalFields decodeDeleteIPv6(AttributeType operationCode, int length, byte[] value) throws UnknownHostException, AddressLengthException, UnknownPrefixException {
        MappingRecord mappingRecord = org.opendaylight.sxp.core.messaging.legacy.MappingRecord.decodeAddress(
                operationCode, length, value);

        DeleteIpv6AttributeBuilder _attributeBuilder = new DeleteIpv6AttributeBuilder();
        DeleteIpv6AttributesBuilder _attributesBuilder = new DeleteIpv6AttributesBuilder();
        _attributesBuilder.setIpPrefix(mappingRecord.getAddress());
        _attributeBuilder.setDeleteIpv6Attributes(_attributesBuilder.build());
        return _attributeBuilder.build();
    }
}
