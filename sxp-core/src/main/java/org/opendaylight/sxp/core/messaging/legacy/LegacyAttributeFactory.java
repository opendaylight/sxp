/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging.legacy;

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

    public static MappingRecord createAddIpv4(Sgt sgt, IpPrefix prefix) throws Exception {
        return org.opendaylight.sxp.core.messaging.legacy.MappingRecord.create(AttributeType.AddIpv4, prefix, sgt);
    }

    public static MappingRecord createAddIpv6(Sgt sgt, IpPrefix prefix) throws Exception {
        return org.opendaylight.sxp.core.messaging.legacy.MappingRecord.create(AttributeType.AddIpv6, prefix, sgt);
    }

    public static MappingRecord createDeleteIpv4(IpPrefix prefix) throws Exception {
        return org.opendaylight.sxp.core.messaging.legacy.MappingRecord.create(AttributeType.DelIpv4, prefix);
    }

    public static MappingRecord createDeleteIpv6(IpPrefix prefix) throws Exception {
        return org.opendaylight.sxp.core.messaging.legacy.MappingRecord.create(AttributeType.DelIpv6, prefix);
    }

    public static AttributeOptionalFields decodeAddIPv4(AttributeType operationCode, int length, byte[] value)
            throws Exception {
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

    public static AttributeOptionalFields decodeAddIPv6(AttributeType operationCode, int length, byte[] value)
            throws Exception {
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

    public static AttributeOptionalFields decodeDeleteIPv4(AttributeType operationCode, int length, byte[] value)
            throws Exception {
        MappingRecord mappingRecord = org.opendaylight.sxp.core.messaging.legacy.MappingRecord.decodeAddress(
                operationCode, length, value);

        DeleteIpv4AttributeBuilder _attributeBuilder = new DeleteIpv4AttributeBuilder();
        DeleteIpv4AttributesBuilder _attributesBuilder = new DeleteIpv4AttributesBuilder();
        _attributesBuilder.setIpPrefix(mappingRecord.getAddress());
        _attributeBuilder.setDeleteIpv4Attributes(_attributesBuilder.build());
        return _attributeBuilder.build();
    }

    public static AttributeOptionalFields decodeDeleteIPv6(AttributeType operationCode, int length, byte[] value)
            throws Exception {
        MappingRecord mappingRecord = org.opendaylight.sxp.core.messaging.legacy.MappingRecord.decodeAddress(
                operationCode, length, value);

        DeleteIpv6AttributeBuilder _attributeBuilder = new DeleteIpv6AttributeBuilder();
        DeleteIpv6AttributesBuilder _attributesBuilder = new DeleteIpv6AttributesBuilder();
        _attributesBuilder.setIpPrefix(mappingRecord.getAddress());
        _attributeBuilder.setDeleteIpv6Attributes(_attributesBuilder.build());
        return _attributeBuilder.build();
    }
}
