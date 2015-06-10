/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeNotFoundException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeVariantException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeVariant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.AttributeOptionalFields;

public class AttributeList extends ArrayList<Attribute> {
    private static final int INITIAL_CAPACITY = 5;

    /** */
    private static final long serialVersionUID = -5995575431003181322L;

    public static AttributeList create(List<Attribute> list) {
        return new AttributeList(list);
    }

    public static AttributeList decode(byte[] array) throws Exception {
        AttributeList attributes = new AttributeList();
        while (array.length != 0) {
            Attribute attribute = AttributeFactory.decode(array);
            attributes.add(attribute);
            array = ArraysUtil.readBytes(array, attribute.getLength());
        }
        return attributes;
    }

    public static AttributeOptionalFields get(List<Attribute> attributes, AttributeType type) throws Exception {
        return new AttributeList(attributes).get(type);
    }

    private static byte[] toBytes(Attribute attribute) throws Exception {
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

    public AttributeList() {
        super(INITIAL_CAPACITY);
    }

    private AttributeList(List<Attribute> attributes) {
        addAll(attributes);
    }

    public AttributeOptionalFields get(AttributeType type) throws Exception {
        for (AttributeFields attribute : this) {
            if (attribute.getType().equals(type) && attribute instanceof Attribute) {
                return ((Attribute) attribute).getAttributeOptionalFields();
            }
        }
        throw new AttributeNotFoundException(type);
    }

    public byte[] toBytes() throws Exception {
        byte[] attributes = new byte[0];
        for (Attribute attribute : this) {
            attributes = ArraysUtil.combine(attributes, toBytes(attribute));
        }
        return attributes;
    }
}
