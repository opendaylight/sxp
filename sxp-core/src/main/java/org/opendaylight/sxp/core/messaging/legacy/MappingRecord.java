/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging.legacy;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;

import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.message.attribute.AddressLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.TlvNotFoundException;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.TlvType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlv.fields.TlvOptionalFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlv.fields.tlv.optional.fields.PrefixLengthTlvAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlv.fields.tlv.optional.fields.PrefixLengthTlvAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlv.fields.tlv.optional.fields.SourceGroupTagTlvAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlv.fields.tlv.optional.fields.prefix.length.tlv.attribute.PrefixLengthTlvAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlv.fields.tlv.optional.fields.source.group.tag.tlv.attribute.SourceGroupTagTlvAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlvs.fields.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlvs.fields.TlvBuilder;

public class MappingRecord extends ArrayList<Tlv> {

    /** */
    private static final long serialVersionUID = -6818880065966274888L;

    private static Tlv _decode(byte[] array) throws Exception {
        TlvBuilder tlvBuilder = new TlvBuilder();

        tlvBuilder.setType(TlvType.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 0, 4))));
        int length = ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 4, 4));
        tlvBuilder.setValue(ArraysUtil.readBytes(array, 8, length));

        TlvOptionalFields tlvOptionalFields = null;
        switch (tlvBuilder.getType()) {
        case PrefixLength:
            tlvOptionalFields = decodeTlvPrefixLength(tlvBuilder.getValue());
            break;
        case Sgt:
            tlvOptionalFields = decodeTlvSourceGroupTag(tlvBuilder.getValue());
            break;
        }
        tlvBuilder.setLength(8 + length);
        tlvBuilder.setTlvOptionalFields(tlvOptionalFields);
        return tlvBuilder.build();
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord create(
            AttributeType operationCode, IpPrefix prefix) throws Exception {
        return create(operationCode, prefix, null);
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord create(
            AttributeType operationCode, IpPrefix prefix, Sgt sgt) throws Exception {
        String _prefix = new String(prefix.getValue());
        if (_prefix.startsWith("/")) {
            _prefix = _prefix.substring(1);
        }
        int i = _prefix.lastIndexOf("/");
        if (i != -1) {
            _prefix = _prefix.substring(0, i);
        }

        MappingRecord tlvs = new MappingRecord();
        InetAddress inetAddress = InetAddress.getByName(_prefix);
        int length = IpPrefixConv.getPrefixLength(prefix);
        if (inetAddress instanceof Inet4Address && length < 32 || inetAddress instanceof Inet6Address && length < 128) {
            tlvs.add(getTlvPrefixLength(length));
        }
        if (sgt != null) {
            tlvs.add(getTlvSgt(sgt.getValue()));
        }

        int mappingRecordLength = 0;
        for (Tlv tlv : tlvs) {
            mappingRecordLength += 8 + tlv.getLength();
        }

        MappingRecordBuilder recordBuilder = new MappingRecordBuilder();
        recordBuilder.setOperationCode(operationCode);
        recordBuilder.setLength(inetAddress.getAddress().length + mappingRecordLength);
        recordBuilder.setAddress(prefix);
        recordBuilder.setTlv(tlvs);
        return recordBuilder.build();
    }

    public static MappingRecord create(Collection<? extends Tlv> tlvs) {
        return new MappingRecord(tlvs);
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord decode(
            byte[] array) throws Exception {
        AttributeType operationCode = AttributeType.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 0, 4)));
        int length = ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 4, 4));
        if (length < 0 || 4080 < length) {
            throw new AttributeLengthException();
        }
        return decodeAddress(operationCode, length, array);

    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord decodeAddress(
            AttributeType operationCode, int length, byte[] array) throws Exception {
        int addressLength;
        boolean add = false;
        switch (operationCode) {
        case AddIpv4:
            add = true;
        case DelIpv4:
            addressLength = 4;
            break;
        case AddIpv6:
            add = true;
        case DelIpv6:
            addressLength = 16;
            break;
        default:
            throw new AddressLengthException();
        }
        byte[] address = ArraysUtil.readBytes(array, 8, addressLength);

        MappingRecord mappingRecord = new MappingRecord();
        if (add) {
            array = ArraysUtil.readBytes(array, 8 + addressLength);

            int tlvsLength = 0;
            do {
                Tlv tlv = _decode(array);
                mappingRecord.add(tlv);
                array = ArraysUtil.readBytes(array, tlv.getLength());
                tlvsLength += tlv.getLength();
            } while (addressLength + tlvsLength < length);
        }
        MappingRecordBuilder recordBuilder = new MappingRecordBuilder();
        recordBuilder.setOperationCode(operationCode);
        recordBuilder.setLength(8 + length);

        InetAddress inetAddress = InetAddress.getByAddress(address);
        int prefixLength;
        try {
            prefixLength = ((PrefixLengthTlvAttribute) mappingRecord.get(TlvType.PrefixLength))
                    .getPrefixLengthTlvAttributes().getPrefixLength();
        } catch (Exception e) {
            if (inetAddress instanceof Inet4Address) {
                prefixLength = 32;
            } else if (inetAddress instanceof Inet6Address) {
                prefixLength = 128;
            } else {
                prefixLength = 0;
            }
        }
        recordBuilder.setAddress(IpPrefixConv.createPrefix(inetAddress.getHostAddress() + "/" + prefixLength));
        recordBuilder.setTlv(mappingRecord);
        return recordBuilder.build();
    }

    private static TlvOptionalFields decodeTlvPrefixLength(byte[] value) throws Exception {
        PrefixLengthTlvAttributeBuilder _attributeBuilder = new PrefixLengthTlvAttributeBuilder();
        PrefixLengthTlvAttributesBuilder _attributesBuilder = new PrefixLengthTlvAttributesBuilder();
        _attributesBuilder.setPrefixLength(ArraysUtil.bytes2int(value));
        _attributeBuilder.setPrefixLengthTlvAttributes(_attributesBuilder.build());
        return _attributeBuilder.build();
    }

    private static TlvOptionalFields decodeTlvSourceGroupTag(byte[] value) throws Exception {
        SourceGroupTagTlvAttributeBuilder _attributeBuilder = new SourceGroupTagTlvAttributeBuilder();
        SourceGroupTagTlvAttributesBuilder _attributesBuilder = new SourceGroupTagTlvAttributesBuilder();
        _attributesBuilder.setSgt(ArraysUtil.bytes2int(value));
        _attributeBuilder.setSourceGroupTagTlvAttributes(_attributesBuilder.build());
        return _attributeBuilder.build();
    }

    /**
     * TLV = [Type (4 octets), Length of the value portion below (4 octets),
     * Value (variable number of octets)]
     * <p>
     * Optional Prefix Length TLV [Type = 02, Length = 1, Value = The prefix
     * length of the IP address] <br>
     * 
     * If the Prefix Length TLV is not presented, the IP address is considered
     * as host address.
     * */
    private static Tlv getTlvPrefixLength(int length) {
        TlvBuilder tlvBuilder = new TlvBuilder();
        tlvBuilder.setType(TlvType.PrefixLength);
        tlvBuilder.setLength(1);
        tlvBuilder.setValue(ArraysUtil.int2bytesCropp(length, 3));
        return tlvBuilder.build();
    }

    /**
     * TLV = [Type (4 octets), Length of the value portion below (4 octets),
     * Value (variable number of octets)]
     * <p>
     * Mandatory SGT TLV [Type = 01, Length = 2, Value = SGT value (16 bits)]
     */
    private static Tlv getTlvSgt(int sgt) {
        TlvBuilder tlvBuilder = new TlvBuilder();
        tlvBuilder.setType(TlvType.Sgt);
        tlvBuilder.setLength(2);
        tlvBuilder.setValue(ArraysUtil.int2bytesCropp(sgt, 2));
        return tlvBuilder.build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord mappingRecord;

    private MappingRecord() {
    }

    private MappingRecord(Collection<? extends Tlv> tlvs) {
        super(tlvs);
    }

    public TlvOptionalFields get(TlvType type) throws Exception {
        for (Tlv tlv : this) {
            if (tlv.getType().equals(type) && tlv instanceof Tlv) {
                return tlv.getTlvOptionalFields();
            }
        }
        throw new TlvNotFoundException(type);
    }

    public org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord getMappingRecord() {
        return mappingRecord;
    }
}
