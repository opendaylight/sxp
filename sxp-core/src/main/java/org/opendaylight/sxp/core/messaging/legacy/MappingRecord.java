/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging.legacy;

import com.google.common.net.InetAddresses;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.message.attribute.AddressLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.TlvNotFoundException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
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

/**
 * MappingRecord class represent entity that contains Tlv,
 * and has logic to work with Tlv
 */
public class MappingRecord extends ArrayList<Tlv> {

    private static final long serialVersionUID = -6818880065966274888L;

    /**
     * Decode Tlv from provided Byte Array
     *
     * @param array Byte Array containing Tlv
     * @return Decoded Tvl
     */
    private static Tlv _decode(byte[] array) {
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

    /**
     * Create MappingRecord with provided values
     *
     * @param operationCode Type of MappingRecord
     * @param prefix        IpPrefix used
     * @return MappingRecord created
     */
    public static org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord create(
            AttributeType operationCode, IpPrefix prefix) {
        return create(operationCode, prefix, null);
    }

    /**
     * Create MappingRecord with provided values
     *
     * @param operationCode Type of MappingRecord
     * @param prefix        IpPrefix used
     * @param sgt           Sgt used
     * @return MappingRecord created
     */
    public static org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord create(
            AttributeType operationCode, IpPrefix prefix, Sgt sgt) {
        String _prefix = new String(prefix.getValue());
        if (_prefix.startsWith("/")) {
            _prefix = _prefix.substring(1);
        }
        int i = _prefix.lastIndexOf("/");
        if (i != -1) {
            _prefix = _prefix.substring(0, i);
        }

        MappingRecord tlvs = new MappingRecord();
        InetAddress inetAddress = InetAddresses.forString(_prefix);
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

    /**
     * Create new MappingRecord using provided Attributes
     *
     * @param tlvs Collection of Tlv used
     * @return MappingRecord containing provided attributes
     */
    public static MappingRecord create(Collection<? extends Tlv> tlvs) {
        return new MappingRecord(tlvs);
    }

    /**
     * Decode MappingRecord from provided Byte Array
     *
     * @param array Byte Array containing MappingRecord
     * @return Decoded MappingRecord
     * @throws UnknownPrefixException   If attribute has incorrect or none Prefix
     * @throws AddressLengthException   If address length of attribute is incorrect
     * @throws AttributeLengthException If length of attribute is incorrect
     * @throws UnknownHostException     If attribute have incorrect or none address
     */
    public static org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord decode(
            byte[] array)
            throws AttributeLengthException, UnknownPrefixException, AddressLengthException, UnknownHostException {
        AttributeType operationCode = AttributeType.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 0, 4)));
        int length = ArraysUtil.bytes2int(ArraysUtil.readBytes(array, 4, 4));
        if (length < 0 || 4080 < length) {
            throw new AttributeLengthException();
        }
        return decodeAddress(operationCode, length, array);

    }

    /**
     * Decode MappingRecord from provided Byte Array
     *
     * @param operationCode Type attribute
     * @param length        Length of attribute
     * @param array         Byte Array containing MappingRecord
     * @return Decoded MappingRecord
     * @throws AddressLengthException If address length of attribute is incorrect
     * @throws UnknownPrefixException If attribute has incorrect or none Prefix
     * @throws UnknownHostException   If attribute have incorrect or none address
     */
    public static org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord decodeAddress(
            AttributeType operationCode, int length, byte[] array) throws AddressLengthException, UnknownPrefixException, UnknownHostException {
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
        } catch (TlvNotFoundException e) {
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

    /**
     * Decode PrefixLengthTlvAttribute from provided Byte Array
     *
     * @param value Byte Array containing attribute
     * @return Decoded PrefixLengthTlvAttribute
     */
    private static TlvOptionalFields decodeTlvPrefixLength(byte[] value) {
        PrefixLengthTlvAttributeBuilder _attributeBuilder = new PrefixLengthTlvAttributeBuilder();
        PrefixLengthTlvAttributesBuilder _attributesBuilder = new PrefixLengthTlvAttributesBuilder();
        _attributesBuilder.setPrefixLength(ArraysUtil.bytes2int(value));
        _attributeBuilder.setPrefixLengthTlvAttributes(_attributesBuilder.build());
        return _attributeBuilder.build();
    }

    /**
     * Decode SourceGroupTagTlvAttribute from provided Byte Array
     *
     * @param value Byte Array containing attribute
     * @return Decoded SourceGroupTagTlvAttribute
     */
    private static TlvOptionalFields decodeTlvSourceGroupTag(byte[] value) {
        SourceGroupTagTlvAttributeBuilder _attributeBuilder = new SourceGroupTagTlvAttributeBuilder();
        SourceGroupTagTlvAttributesBuilder _attributesBuilder = new SourceGroupTagTlvAttributesBuilder();
        _attributesBuilder.setSgt(ArraysUtil.bytes2int(value));
        _attributeBuilder.setSourceGroupTagTlvAttributes(_attributesBuilder.build());
        return _attributeBuilder.build();
    }

    /**
     * TLV = [Type (4 octets), Length of the value portion below (4 octets),
     * Value (variable number of octets)]
     * Optional Prefix Length TLV [Type = 02, Length = 1, Value = The prefix
     * length of the IP address] <br>
     * If the Prefix Length TLV is not presented, the IP address is considered
     * as host address.
     */
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

    /**
     * Create new MappingRecord using provided Attributes
     *
     * @param tlvs Collection of Tlv used
     * @return MappingRecord containing provided attributes
     */
    private MappingRecord(Collection<? extends Tlv> tlvs) {
        super(tlvs);
    }

    public TlvOptionalFields get(TlvType type) throws TlvNotFoundException {
        for (Tlv tlv : this) {
            if (tlv.getType().equals(type) && tlv instanceof Tlv) {
                return tlv.getTlvOptionalFields();
            }
        }
        throw new TlvNotFoundException(type);
    }

    /**
     * @return Gets current MappingRecord
     */
    public org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord getMappingRecord() {
        return mappingRecord;
    }
}
