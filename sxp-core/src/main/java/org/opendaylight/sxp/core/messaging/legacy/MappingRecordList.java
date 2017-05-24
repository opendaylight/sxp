/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging.legacy;

import com.google.common.net.InetAddresses;
import java.net.UnknownHostException;
import java.util.ArrayList;
import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.message.attribute.AddressLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeLengthException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.mapping.records.fields.MappingRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.tlvs.fields.Tlv;

public class MappingRecordList extends ArrayList<MappingRecord> {

    private static final int INITIAL_CAPACITY = 5;

    /** */
    private static final long serialVersionUID = -427123702376957236L;

    /**
     * @param array Byte array that will b decoded
     * @return MappingRecordList created from provided values
     * @throws UnknownPrefixException   If provided data is not valid
     * @throws AddressLengthException   If provided data is not valid
     * @throws AttributeLengthException If provided data is not valid
     * @throws UnknownHostException     If provided data is not valid
     */
    public static MappingRecordList decode(byte[] array)
            throws UnknownPrefixException, AddressLengthException, AttributeLengthException, UnknownHostException {
        MappingRecordList mappingRecordList = new MappingRecordList();
        while (array != null && array.length != 0) {
            MappingRecord mappingRecord = org.opendaylight.sxp.core.messaging.legacy.MappingRecord.decode(array);
            mappingRecordList.add(mappingRecord);
            array = ArraysUtil.readBytes(array, mappingRecord.getLength());
        }
        return mappingRecordList;
    }

    /**
     * @param mappingRecord that will be converted
     * @return Byte representation of MappingRecord
     */
    private static byte[] toBytes(MappingRecord mappingRecord) {
        String _prefix = new String(mappingRecord.getAddress().getValue());
        if (_prefix.startsWith("/")) {
            _prefix = _prefix.substring(1);
        }
        int i = _prefix.lastIndexOf("/");
        if (i != -1) {
            _prefix = _prefix.substring(0, i);
        }

        byte[] bprefix = InetAddresses.forString(_prefix).getAddress();
        byte[]
                _mappingRecord =
                ArraysUtil.combine(ArraysUtil.int2bytes(mappingRecord.getOperationCode().getIntValue()),
                        ArraysUtil.int2bytes(mappingRecord.getLength()), bprefix);

        for (Tlv tlv : mappingRecord.getTlv()) {
            _mappingRecord =
                    ArraysUtil.combine(_mappingRecord, ArraysUtil.int2bytes(tlv.getType().getIntValue()),
                            ArraysUtil.int2bytes(tlv.getLength()), tlv.getValue());
        }
        return _mappingRecord;
    }

    /**
     * Default MappingRecordList constructor
     */
    public MappingRecordList() {
        super(INITIAL_CAPACITY);
    }

    /**
     * @return Byte representation of MappingRecordList
     */
    public byte[] toBytes() {
        byte[] mappingRecords = new byte[0];
        for (MappingRecord mappingRecord : this) {
            mappingRecords = ArraysUtil.combine(mappingRecords, toBytes(mappingRecord));
        }
        return mappingRecords;
    }
}
