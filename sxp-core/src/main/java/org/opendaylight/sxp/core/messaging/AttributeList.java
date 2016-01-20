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
import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.message.attribute.AddressLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeNotFoundException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeVariantException;
import org.opendaylight.sxp.util.exception.message.attribute.TlvNotFoundException;
import org.opendaylight.sxp.util.exception.unknown.UnknownNodeIdException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeVariant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.AttributeOptionalFields;

/**
 * AttributeList class represent entity that contains Attributes,
 * and has logic to work with Attributes
 */
public class AttributeList extends ArrayList<Attribute> {

    private static final int INITIAL_CAPACITY = 5;

    private static final long serialVersionUID = -5995575431003181322L;

    /**
     * Constructor initialized by Attributes
     *
     * @param list Attributes to be included in new AttributeList
     */
    public static AttributeList create(List<Attribute> list) {
        return new AttributeList(list);
    }

    /**
     * Decode AttributeList from provided Byte Array
     *
     * @param array Byte Array containing Attributes
     * @return AttributeList decoded from provided data
     * @throws AttributeLengthException If length of some attribute is incorrect
     * @throws AddressLengthException   If address length of some attribute is incorrect
     * @throws UnknownNodeIdException   If NodeId is missing in some attribute
     * @throws TlvNotFoundException     If Tlv is missing in some attribute
     * @throws UnknownPrefixException   If some attribute has incorrect or none Prefix
     * @throws UnknownHostException     If some attribute have incorrect or none address
     */
    public static AttributeList decode(byte[] array)
            throws AttributeLengthException, AddressLengthException, UnknownNodeIdException, TlvNotFoundException,
            UnknownPrefixException, UnknownHostException, AttributeVariantException {
        AttributeList attributes = new AttributeList();
        while (array.length != 0) {
            Attribute attribute = AttributeFactory.decode(array);
            attributes.add(attribute);
            array = ArraysUtil.readBytes(array, attribute.getLength());
        }
        return attributes;
    }

    /**
     * Gets Attribute of specific type
     *
     * @param attributes Attributes where to look for
     * @param type       Attribute type to look for
     * @return Attribute of provided type
     * @throws AttributeNotFoundException If AttributeType wasn't found
     */
    public static AttributeOptionalFields get(List<Attribute> attributes, AttributeType type)
            throws AttributeNotFoundException {
        return new AttributeList(attributes).get(type);
    }

    /**
     * Generate byte representation of Attribute
     *
     * @param attribute Attribute to be encoded
     * @return Byte Array representation of Attribute
     * @throws AttributeVariantException If attribute variant isn't supported
     */
    private static byte[] toBytes(Attribute attribute) throws AttributeVariantException {
        byte flags = ArraysUtil.convertBits(attribute.getFlags().isOptional(), attribute.getFlags().isNonTransitive(),
                attribute.getFlags().isPartial(), attribute.getFlags().isCompact(), attribute.getFlags()
                        .isExtendedLength(), false, false, false);
        byte[] value = attribute.getValue();

        if (attribute.getAttributeVariant().equals(AttributeVariant.Compact)) {
            byte type = ArraysUtil.int2bytes(attribute.getType().getIntValue())[3];
            byte length = ArraysUtil.int2bytes(attribute.getLength())[3];
            if (Configuration.SET_COMPOSITION_ATTRIBUTE_COMPACT_NO_RESERVED_FIELDS) {
                return ArraysUtil.combine(new byte[] { flags, type, length }, value);
            }
            return ArraysUtil.combine(new byte[] { flags, type, length, 0x00 }, value);

        } else if (attribute.getAttributeVariant().equals(AttributeVariant.CompactExtendedLength)) {
            byte type = ArraysUtil.int2bytes(attribute.getType().getIntValue())[3];
            byte[] length = ArraysUtil.int2bytesCropp(attribute.getLength(), 2);
            return ArraysUtil.combine(new byte[] { flags, type }, length, value);

        } else if (attribute.getAttributeVariant().equals(AttributeVariant.NonCompact)) {
            byte[] type = ArraysUtil.int2bytesCropp(attribute.getType().getIntValue(), 1);
            byte[] length = ArraysUtil.int2bytes(attribute.getLength());
            return ArraysUtil.combine(type, length, value);

        } else {
            throw new AttributeVariantException();
        }
    }

    /**
     * Constructor that have no Attributes
     */
    public AttributeList() {
        super(INITIAL_CAPACITY);
    }

    /**
     * Constructor initialized by Attributes
     *
     * @param attributes Attributes to be included in new AttributeList
     */
    private AttributeList(List<Attribute> attributes) {
        addAll(attributes);
    }

    /**
     * Gets Attribute of specific type
     *
     * @param type AttributeType to look for
     * @return Attribute of provided type
     * @throws AttributeNotFoundException If AttributeType wasn't found
     */
    public AttributeOptionalFields get(AttributeType type) throws AttributeNotFoundException {
        for (AttributeFields attribute : this) {
            if (attribute.getType().equals(type) && attribute instanceof Attribute) {
                return ((Attribute) attribute).getAttributeOptionalFields();
            }
        }
        throw new AttributeNotFoundException(type);
    }

    /**
     * Generate byte representation of AttributeList
     *
     * @return Encoded AttributeList
     * @throws AttributeVariantException If attribute variant isn't supported
     */
    public byte[] toBytes() throws AttributeVariantException {
        byte[] attributes = new byte[0];
        for (Attribute attribute : this) {
            attributes = ArraysUtil.combine(attributes, toBytes(attribute));
        }
        return attributes;
    }
}
