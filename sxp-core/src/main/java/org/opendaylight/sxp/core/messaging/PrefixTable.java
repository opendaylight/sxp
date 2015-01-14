/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.message.attribute.PrefixTableAttributeIsNotCompactException;
import org.opendaylight.sxp.util.exception.message.attribute.PrefixTableColumnsSizeException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;

public class PrefixTable extends HashMap<IpPrefix, List<Attribute>> {

    /** */
    private static final long serialVersionUID = -5663814987902930673L;

    private byte columns;

    public PrefixTable(int columns) throws PrefixTableColumnsSizeException {
        if (columns < 1 && 255 < columns) {
            throw new PrefixTableColumnsSizeException();
        }
        this.columns = ArraysUtil.int2bytes(columns)[0];
    }

    public void addItem(IpPrefix prefix, Attribute... attributes) throws PrefixTableAttributeIsNotCompactException {
        List<Attribute> _attributes = new ArrayList<Attribute>(columns);
        for (int i = 0; i < columns; i++) {
            // Fields flags, type and width should be 1 byte long.
            if (!attributes[i].getFlags().isCompact()) {
                throw new PrefixTableAttributeIsNotCompactException();
            }

            _attributes.add(attributes[i]);
        }
        put(prefix, _attributes);
    }

    public byte[] toBytes() {
        // Number of columns and reserved fields.
        byte[] _head = new byte[] { columns, 0x00, 0x00, 0x00 };

        int addedAttributes = 0;
        byte[] _columns = new byte[0];
        for (IpPrefix prefix : keySet()) {
            for (int i = 0; i < columns; i++) {
                Attribute attribute = get(prefix).get(i);

                _columns = ArraysUtil.combine(
                        _columns,
                        new byte[] {
                                ArraysUtil.convertBits(attribute.getFlags().isOptional(), attribute.getFlags()
                                        .isNonTransitive(), attribute.getFlags().isPartial(), attribute.getFlags()
                                        .isCompact(), attribute.getFlags().isExtendedLength(), false, false, false),
                                ArraysUtil.int2bytes(attribute.getType().getIntValue())[0],
                                ArraysUtil.int2bytes(attribute.getLength())[0] });
                addedAttributes++;
            }
        }

        // Fill empty space.
        byte[] _emptys = new byte[4 - addedAttributes % 4];
        _columns = ArraysUtil.combine(_columns, _emptys);

        // Values.
        byte[] _rows = new byte[0];
        for (IpPrefix prefix : keySet()) {
            for (int i = 0; i < columns; i++) {
                Attribute attribute = get(prefix).get(i);
                _rows = ArraysUtil.combine(_rows, attribute.getValue());
            }

            _rows = ArraysUtil.combine(_rows, new String(prefix.getValue()).getBytes());
        }

        return ArraysUtil.combine(_head, _columns, _rows);
    }
}
