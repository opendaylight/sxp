/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging.legacy;

import io.netty.buffer.ByteBuf;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.ErrorCodeDataLengthException;
import org.opendaylight.sxp.util.exception.connection.IncompatiblePeerVersionException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.attribute.AddressLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeLengthException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.Notification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessageLegacyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.UpdateMessageLegacyBuilder;

import java.net.UnknownHostException;
import java.util.List;

/**
 * LegacyMessageFactory class contains logic for creating and parsing legacy messages
 */
public class LegacyMessageFactory extends MessageFactory {
    private static final int MESSAGE_HEADER_LENGTH_LENGTH = Configuration.getConstants().getMessageHeaderLengthLength();

    private static final int MESSAGE_HEADER_TYPE_LENGTH = Configuration.getConstants().getMessageHeaderTypeLength();

    /**
     * Creates Error message based on provided error code
     *
     * @param errorCode ErrorCodeNonExtended defining type of error message
     * @return ByteBuf representation of Error
     * @throws ErrorCodeDataLengthException If message Length is to long
     */
    public static ByteBuf createError(ErrorCodeNonExtended errorCode) throws ErrorCodeDataLengthException {
        return createError(errorCode, null);
    }

    /**
     * Creates Error message based on provided error code
     *
     * @param errorCode ErrorCodeNonExtended defining type of error message
     * @param data      Byte Array containing additional information
     * @return ByteBuf representation of Error
     * @throws ErrorCodeDataLengthException If message Length is to long
     */
    public static ByteBuf createError(ErrorCodeNonExtended errorCode, byte[] data) throws ErrorCodeDataLengthException {
        if (data == null) {
            data = new byte[0];
        } else if (data.length > 10) {
            throw new ErrorCodeDataLengthException("Variable message length - 10 bytes.");
        }
        byte _errorCode = (byte) errorCode.getIntValue();
        byte[] payload = ArraysUtil.combine(new byte[] { 0x00, 0x00, 0x00, _errorCode }, data);
        return getMessage(MessageType.Error, payload);
    }

    /**
     * Creates OpenMessage using provided values
     *
     * @param version  Version included in message
     * @param nodeMode ConnectionMode included in message
     * @return ByteBuf representation of created OpenMessage
     */
    public static ByteBuf createOpen(Version version, ConnectionMode nodeMode) {
        return getMessage(
                MessageType.Open,
                ArraysUtil.combine(ArraysUtil.int2bytes(version.getIntValue()),
                        ArraysUtil.int2bytes(nodeMode.getIntValue())));
    }

    /**
     * Creates OpenRespMessage using provided values
     *
     * @param version  Version included in message
     * @param nodeMode ConnectionMode included in message
     * @return ByteBuf representation if created OpenRespMessage
     */
    public static ByteBuf createOpenResp(Version version, ConnectionMode nodeMode) {
        return getMessage(
                MessageType.OpenResp,
                ArraysUtil.combine(ArraysUtil.int2bytes(version.getIntValue()),
                        ArraysUtil.int2bytes(nodeMode.getIntValue())));
    }

    /**
     * UPDATE message contains one or more SXP mapping records.
     * <pre>
     * Mapping-Record = [Opcode (4 octets), Length (4 bytes), IPv4/IPv6 address (4/16 bytes), [List of TLV’s]]
     * TLV = [Type (4 octets), Length of the value portion below (4 octets), Value (variable number of octets)]
     * </pre>
     * Defined TLVs:
     * <pre>
     * Mandatory SGT TLV
     * [Type = 01, Length = 2, Value = SGT value (16 bits)]
     * Optional Prefix Length TLV
     * [Type = 02, Length = 1, Value = The prefix length of the IP address]
     * </pre>
     * If the Prefix Length TLV is not presented, the IP address is considered
     * as host address.
     */
    public static <T extends SxpBindingFields> ByteBuf createUpdate(List<T> deleteBindings, List<T> addBindings,
            Version version, SxpBindingFilter bindingFilter) throws UnknownVersionException {
        if (version == null || !MessageFactory.isLegacy(version))
            throw new UnknownVersionException();

        MappingRecordList mappingRecords = new MappingRecordList();
        if (deleteBindings != null && !deleteBindings.isEmpty()) {
            deleteBindings.stream().forEach(binding -> {
                switch (version) {
                    case Version3:
                        if (binding.getIpPrefix().getIpv4Prefix() != null) {
                            mappingRecords.add(LegacyAttributeFactory.createDeleteIpv4(binding.getIpPrefix()));
                        } else if (binding.getIpPrefix().getIpv6Prefix() != null) {
                            mappingRecords.add(LegacyAttributeFactory.createDeleteIpv6(binding.getIpPrefix()));
                        }
                        break;
                    case Version2:
                        if (binding.getIpPrefix().getIpv6Prefix() != null) {
                            if (IpPrefixConv.getPrefixLength(binding.getIpPrefix()) == 128) {
                                mappingRecords.add(LegacyAttributeFactory.createDeleteIpv6(binding.getIpPrefix()));
                            }
                            break;
                        }
                    case Version1:
                        if (binding.getIpPrefix().getIpv4Prefix() != null) {
                            if (IpPrefixConv.getPrefixLength(binding.getIpPrefix()) == 32) {
                                mappingRecords.add(LegacyAttributeFactory.createDeleteIpv4(binding.getIpPrefix()));
                            }
                        }
                        break;
                }
            });
        }

        if (addBindings != null && !addBindings.isEmpty()) {
            addBindings.stream().forEach(binding -> {
                if (bindingFilter != null && bindingFilter.apply(binding))
                    return;
                switch (version) {
                    case Version3:
                        if (binding.getIpPrefix().getIpv4Prefix() != null) {
                            mappingRecords.add(LegacyAttributeFactory.createAddIpv4(binding.getSecurityGroupTag(),
                                    binding.getIpPrefix()));
                        } else if (binding.getIpPrefix().getIpv6Prefix() != null) {
                            mappingRecords.add(LegacyAttributeFactory.createAddIpv6(binding.getSecurityGroupTag(),
                                    binding.getIpPrefix()));
                        }
                        break;
                    case Version2:
                        if (binding.getIpPrefix().getIpv6Prefix() != null) {
                            if (IpPrefixConv.getPrefixLength(binding.getIpPrefix()) == 128) {
                                mappingRecords.add(LegacyAttributeFactory.createAddIpv6(binding.getSecurityGroupTag(),
                                        binding.getIpPrefix()));
                            }
                            break;
                        }
                    case Version1:
                        if (binding.getIpPrefix().getIpv4Prefix() != null) {
                            if (IpPrefixConv.getPrefixLength(binding.getIpPrefix()) == 32) {
                                mappingRecords.add(LegacyAttributeFactory.createAddIpv4(binding.getSecurityGroupTag(),
                                        binding.getIpPrefix()));
                            }
                        }
                        break;
                }
            });
        }

        if (mappingRecords.isEmpty()) {
            return null;
        }
        return getMessage(MessageType.Update, mappingRecords.toBytes());
    }

    /**
     * Decode OpenMessageLegacy from provided Byte Array
     *
     * @param payload Byte Array containing message
     * @return Notification with decoded OpenMessageLegacy
     */
    public static Notification decodeOpen(byte[] payload) {
        OpenMessageLegacyBuilder messageBuilder = new OpenMessageLegacyBuilder();
        messageBuilder.setType(MessageType.Open);
        messageBuilder.setLength(MESSAGE_HEADER_LENGTH_LENGTH + MESSAGE_HEADER_TYPE_LENGTH + payload.length);
        messageBuilder.setPayload(payload);

        Version version = Version.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(payload, 0, 4)));
        ConnectionMode nodeMode = ConnectionMode.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(payload, 4, 4)));

        messageBuilder.setVersion(version);
        messageBuilder.setSxpMode(nodeMode);
        return messageBuilder.build();
    }

    /**
     * Decode OpenRespMessageLegacy from provided Byte Array
     *
     * @param payload Byte Array containing message
     * @return Notification with decoded OpenRespMessageLegacy
     * @throws ErrorMessageException If version isn't legacy 1/2/3
     */
    public static Notification decodeOpenResp(byte[] payload) throws ErrorMessageException {
        OpenMessageLegacyBuilder messageBuilder = new OpenMessageLegacyBuilder();
        messageBuilder.setType(MessageType.OpenResp);
        messageBuilder.setLength(MESSAGE_HEADER_LENGTH_LENGTH + MESSAGE_HEADER_TYPE_LENGTH + payload.length);
        messageBuilder.setPayload(payload);

        Version version = Version.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(payload, 0, 4)));
        if (!isLegacy(version)) {
            throw new ErrorMessageException(ErrorCodeNonExtended.VersionMismatch, new IncompatiblePeerVersionException(
                    true, version));
        }
        ConnectionMode nodeMode = ConnectionMode.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(payload, 4, 4)));

        messageBuilder.setVersion(version);
        messageBuilder.setSxpMode(nodeMode);
        return messageBuilder.build();
    }

    /**
     * Decode UpdateMessageLegacy from provided Byte Array
     *
     * @param payload Byte Array containing message
     * @return Notification with decoded UpdateMessageLegacy
     * @throws UnknownPrefixException   If some attribute has incorrect or none Prefix
     * @throws AddressLengthException   If address length of some attribute is incorrect
     * @throws AttributeLengthException If length of some attribute is incorrect
     * @throws UnknownHostException     If some attribute have incorrect or none address
     */
    public static Notification decodeUpdate(byte[] payload)
            throws UnknownPrefixException, AddressLengthException, AttributeLengthException, UnknownHostException {
        UpdateMessageLegacyBuilder messageBuilder = new UpdateMessageLegacyBuilder();
        messageBuilder.setType(MessageType.Update);
        messageBuilder.setLength(MESSAGE_HEADER_LENGTH_LENGTH + MESSAGE_HEADER_TYPE_LENGTH + payload.length);
        messageBuilder.setPayload(payload);

        messageBuilder.setMappingRecord(MappingRecordList.decode(payload));
        return messageBuilder.build();
    }
}
