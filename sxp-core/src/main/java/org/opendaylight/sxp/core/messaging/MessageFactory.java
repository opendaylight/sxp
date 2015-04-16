/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.messaging;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import java.util.ArrayList;
import java.util.List;
import java.util.UnknownFormatConversionException;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.messaging.legacy.LegacyMessageFactory;
import org.opendaylight.sxp.util.ArraysUtil;
import org.opendaylight.sxp.util.exception.ErrorCodeDataLengthException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageBindingSourceException;
import org.opendaylight.sxp.util.exception.unknown.UnknownConnectionModeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpMessageTypeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorSubCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.KeepaliveMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.OpenMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.PurgeAllMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.SxpHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.SxpPayload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yangtools.yang.binding.Notification;

public class MessageFactory {

    private static final int MESSAGE_HEADER_LENGTH_LENGTH = Configuration.getConstants().getMessageHeaderLengthLength();

    private static final int MESSAGE_HEADER_TYPE_LENGTH = Configuration.getConstants().getMessageHeaderTypeLength();

    private static final int MESSAGE_LENGTH_MAX = Configuration.getConstants().getMessageLengthMax();

    public static ByteBuf createError(ErrorCode errorCode, ErrorSubCode errorSubCode, byte[] data) throws Exception {
        if (data == null) {
            data = new byte[0];
        } else if (data.length > 10) {
            throw new ErrorCodeDataLengthException("Variable message length - 10 bytes.");
        }

        byte _errorCode = (byte) errorCode.getIntValue();
        byte _errorSubCode = errorSubCode != null ? (byte) errorSubCode.getIntValue() : 0x00;
        byte[] payload = ArraysUtil.combine(new byte[] { ArraysUtil.setBit(_errorCode, 8, true), _errorSubCode, 0x00,
                0x00 }, data);
        return getMessage(MessageType.Error, payload);
    }

    public static ByteBuf createKeepalive() {
        return getMessage(MessageType.Keepalive, new byte[0]);
    }

    public static ByteBuf createOpen(Version version, ConnectionMode nodeMode, NodeId nodeID) throws Exception {
        AttributeList attributes = createOpenAttribute(version, nodeMode, nodeID);
        // Add optional attributes..
        byte[] payload = ArraysUtil.combine(new byte[] { 0x00, 0x00, 0x00, (byte) version.getIntValue(), 0x00, 0x00,
                0x00, (byte) nodeMode.getIntValue() }, attributes.toBytes());
        return getMessage(MessageType.Open, payload);
    }

    public static ByteBuf createOpen(Version version, ConnectionMode nodeMode, NodeId nodeID, int holdTimeMinAcceptable)
            throws Exception {
        AttributeList attributes = createOpenAttribute(version, nodeMode, nodeID);
        attributes.add(AttributeFactory.createHoldTime(holdTimeMinAcceptable));
        // Add optional attributes..
        byte[] payload = ArraysUtil.combine(new byte[] { 0x00, 0x00, 0x00, (byte) version.getIntValue(), 0x00, 0x00,
                0x00, (byte) nodeMode.getIntValue() }, attributes.toBytes());
        return getMessage(MessageType.Open, payload);
    }

    public static ByteBuf createOpen(Version version, ConnectionMode nodeMode, NodeId nodeID, int holdTimeMin,
            int holdTimeMax) throws Exception {
        AttributeList attributes = createOpenAttribute(version, nodeMode, nodeID);
        attributes.add(AttributeFactory.createHoldTime(holdTimeMin, holdTimeMax));
        // Add optional attributes..
        byte[] payload = ArraysUtil.combine(new byte[] { 0x00, 0x00, 0x00, (byte) version.getIntValue(), 0x00, 0x00,
                0x00, (byte) nodeMode.getIntValue() }, attributes.toBytes());
        return getMessage(MessageType.Open, payload);
    }

    private static AttributeList createOpenAttribute(Version version, ConnectionMode nodeMode, NodeId nodeID)
            throws Exception {
        AttributeList attributes = new AttributeList();
        if (nodeMode.equals(ConnectionMode.Speaker)) {
            attributes.add(AttributeFactory.createSxpNodeId(nodeID));
        } else if (nodeMode.equals(ConnectionMode.Listener)) {
            attributes.add(AttributeFactory.createCapabilities(version));
        }
        return attributes;
    }

    public static ByteBuf createOpenResp(Version version, ConnectionMode nodeMode, NodeId nodeID) throws Exception {
        AttributeList attributes = createOpenAttribute(version, nodeMode, nodeID);
        // Add optional attributes..
        byte[] payload = ArraysUtil.combine(new byte[] { 0x00, 0x00, 0x00, (byte) version.getIntValue(), 0x00, 0x00,
                0x00, (byte) nodeMode.getIntValue() }, attributes.toBytes());
        return getMessage(MessageType.OpenResp, payload);
    }

    public static ByteBuf createOpenResp(Version version, ConnectionMode nodeMode, NodeId nodeID,
            int holdTimeMinAcceptable) throws Exception {
        AttributeList attributes = createOpenAttribute(version, nodeMode, nodeID);
        attributes.add(AttributeFactory.createHoldTime(holdTimeMinAcceptable));
        // Add optional attributes..
        byte[] payload = ArraysUtil.combine(new byte[] { 0x00, 0x00, 0x00, (byte) version.getIntValue(), 0x00, 0x00,
                0x00, (byte) nodeMode.getIntValue() }, attributes.toBytes());
        return getMessage(MessageType.OpenResp, payload);
    }

    public static ByteBuf createOpenResp(Version version, ConnectionMode nodeMode, NodeId nodeID, int holdTimeMin,
            int holdTimeMax) throws Exception {
        AttributeList attributes = createOpenAttribute(version, nodeMode, nodeID);
        attributes.add(AttributeFactory.createHoldTime(holdTimeMin, holdTimeMax));
        // Add optional attributes..
        byte[] payload = ArraysUtil.combine(new byte[] { 0x00, 0x00, 0x00, (byte) version.getIntValue(), 0x00, 0x00,
                0x00, (byte) nodeMode.getIntValue() }, attributes.toBytes());
        return getMessage(MessageType.OpenResp, payload);
    }

    public static ByteBuf createPurgeAll() {
        return getMessage(MessageType.PurgeAll, new byte[0]);
    }

    public static ByteBuf createUpdate(MasterDatabase masterDatabase, NodeId nodeId, boolean changed) throws Exception {
        AttributeList attributes = new AttributeList();
        // 2. Processing of global optional attributes.
        for (Attribute attribute : masterDatabase.getAttribute()) {
            if (attribute.getFlags().isOptional()
                    && (attribute.getFlags().isPartial() || !attribute.getFlags().isNonTransitive())) {
                attributes.add(attribute);
            }
        }

        // 3. Processing of binding delete attributes.
        List<IpPrefix> removeIpv4 = new ArrayList<>();
        List<IpPrefix> removeIpv6 = new ArrayList<>();
        for (Source source : masterDatabase.getSource()) {
            if (source.getPrefixGroup() != null) {
                for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                    if (prefixGroup.getBinding() != null) {
                        for (Binding binding : prefixGroup.getBinding()) {
                            if (changed && binding.isChanged() == null || changed && !binding.isChanged()) {
                                continue;
                            } else if (binding.getAction() == null
                                    || !binding.getAction().equals(DatabaseAction.Delete)) {
                                continue;
                            } else if (binding.getIpPrefix().getIpv4Prefix() != null) {
                                removeIpv4.add(binding.getIpPrefix());
                            } else if (binding.getIpPrefix().getIpv6Prefix() != null) {
                                removeIpv6.add(binding.getIpPrefix());
                            }
                        }
                    }
                }
            }
        }
        // Binding delete attributes include any of IPv4-Del-Prefix,
        // IPv6-Del-Prefix, Del-IPv4, or Del-IPv6 attributes.
        if (!removeIpv4.isEmpty()) {
            attributes.add(AttributeFactory.createIpv4DeletePrefix(removeIpv4));
        }
        if (!removeIpv6.isEmpty()) {
            attributes.add(AttributeFactory.createIpv6DeletePrefix(removeIpv6));
        }

        // 4. Processing path-groups.
        for (Source source : masterDatabase.getSource()) {

            AttributeList _attributes = new AttributeList();
            // A. Process per-path common optional attributes
            if (source.getAttribute() != null) {
                _attributes.addAll(source.getAttribute());
            }

            // B. Add-Prefix groups. Each Add-Prefix group starts with a
            // Source-Group-Tag attribute.

            boolean oneShot = false;
            boolean added = false;

            if (source.getPrefixGroup() != null) {
                for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                    List<IpPrefix> addIpv4 = new ArrayList<>();
                    List<IpPrefix> addIpv6 = new ArrayList<>();
                    List<NodeId> peerSequence = null;

                    boolean _added = false;
                    if (prefixGroup.getBinding() != null) {
                        for (Binding binding : prefixGroup.getBinding()) {
                            if (changed && binding.isChanged() == null || changed && !binding.isChanged()) {
                                continue;
                            } else if (binding.getAction() == null || !binding.getAction().equals(DatabaseAction.Add)) {
                                continue;
                            } else if (binding.getIpPrefix().getIpv4Prefix() != null) {
                                addIpv4.add(binding.getIpPrefix());
                                _added = true;
                            } else if (binding.getIpPrefix().getIpv6Prefix() != null) {
                                addIpv6.add(binding.getIpPrefix());
                                _added = true;
                            }

                            if (_added && peerSequence == null) {
                                if (source.getBindingSource().equals(DatabaseBindingSource.Sxp)) {
                                    peerSequence = NodeIdConv.getPeerSequence(binding.getPeerSequence());
                                } else if (source.getBindingSource().equals(DatabaseBindingSource.Local)) {
                                    peerSequence = new ArrayList<NodeId>();
                                } else {
                                    throw new UpdateMessageBindingSourceException(source.getBindingSource());
                                }
                            }
                        }
                    }
                    if (addIpv4.isEmpty() && addIpv6.isEmpty()) {
                        continue;
                    }

                    peerSequence.add(0, nodeId);
                    Attribute peerSequenceAttribute = AttributeFactory.createPeerSequence(peerSequence);

                    if (Configuration.SET_COMPOSITION_UPDATE_MESSAGE_PEER_SEQUENCE_WITH_EACH_SGT) {
                        _attributes.add(peerSequenceAttribute);
                    } else if (!Configuration.SET_COMPOSITION_UPDATE_MESSAGE_PEER_SEQUENCE_WITH_EACH_SGT && !oneShot) {
                        _attributes.add(peerSequenceAttribute);
                        oneShot = true;
                    }

                    _attributes.add(AttributeFactory.createSourceGroupTag(prefixGroup.getSgt().getValue()));

                    // i. Process per <path, SGT> optional attributed.
                    if (prefixGroup.getAttribute() != null) {
                        _attributes.addAll(prefixGroup.getAttribute());
                    }
                    if (!addIpv4.isEmpty()) {
                        _attributes.add(AttributeFactory.createIpv4AddPrefix(addIpv4));
                        added = true;
                    }
                    if (!addIpv6.isEmpty()) {
                        _attributes.add(AttributeFactory.createIpv6AddPrefix(addIpv6));
                        added = true;
                    }
                }
            }

            // TODO: C. Processing IPv4-Add-Table attribute

            // TODO: D. Processing IPv6-Add-Table attribute

            // E. Processing Add-IPv4 attributes

            // F. Processing Add-IPv6 attributes

            if (added) {
                attributes.addAll(_attributes);
            }
        }

        // 5. Processing trailing optional non-transitive attributes
        for (Attribute attribute : masterDatabase.getAttribute()) {
            if (attribute.getFlags().isOptional() && attribute.getFlags().isNonTransitive()) {
                attributes.add(attribute);
            }
        }

        if (attributes.isEmpty()) {
            return null;
        }

        return getMessage(MessageType.Update, attributes.toBytes());
    }

    public static ByteBuf createUpdateAddPrefixes(NodeId nodeID, List<NodeId> peerSequence,
            List<PrefixGroup> prefixGroups) throws Exception {
        AttributeList attributes = new AttributeList();
        // Prepend local node ID.
        peerSequence.add(0, nodeID);
        attributes.add(AttributeFactory.createPeerSequence(peerSequence));

        for (PrefixGroup prefixGroup : prefixGroups) {
            attributes.add(AttributeFactory.createSourceGroupTag(prefixGroup.getSgt().getValue()));

            List<IpPrefix> addIpv4 = new ArrayList<>();
            List<IpPrefix> addIpv6 = new ArrayList<>();
            if (prefixGroup.getBinding() != null) {
                for (Binding binding : prefixGroup.getBinding()) {
                    if (binding.getIpPrefix().getIpv4Prefix() != null) {
                        addIpv4.add(binding.getIpPrefix());
                    } else if (binding.getIpPrefix().getIpv6Prefix() != null) {
                        addIpv6.add(binding.getIpPrefix());
                    }
                }
            }
            if (!addIpv4.isEmpty()) {
                attributes.add(AttributeFactory.createIpv4AddPrefix(addIpv4));
            }
            if (!addIpv6.isEmpty()) {
                attributes.add(AttributeFactory.createIpv6AddPrefix(addIpv6));
            }
        }
        return getMessage(MessageType.Update, attributes.toBytes());
    }

    public static ByteBuf createUpdateDeletePrefixes(List<PrefixGroup> prefixGroups) throws UnknownVersionException,
            UnknownConnectionModeException, Exception {
        AttributeList attributes = new AttributeList();
        List<IpPrefix> removeIpv4 = new ArrayList<>();
        List<IpPrefix> removeIpv6 = new ArrayList<>();

        for (PrefixGroup prefixGroup : prefixGroups) {
            if (prefixGroup.getBinding() != null) {
                for (Binding binding : prefixGroup.getBinding()) {
                    if (binding.getIpPrefix().getIpv4Prefix() != null) {
                        removeIpv4.add(binding.getIpPrefix());
                    } else if (binding.getIpPrefix().getIpv6Prefix() != null) {
                        removeIpv6.add(binding.getIpPrefix());
                    }
                }
            }
        }
        attributes.add(AttributeFactory.createIpv4DeletePrefix(removeIpv4));
        attributes.add(AttributeFactory.createIpv6DeletePrefix(removeIpv6));
        return getMessage(MessageType.Update, attributes.toBytes());
    }

    public static ByteBuf createUpdateTableAddPrefixes(NodeId nodeID, List<NodeId> peerSequence,
            List<PrefixGroup> prefixGroups) throws Exception {
        AttributeList attributes = new AttributeList();
        // Prepend local node ID.
        peerSequence.add(0, nodeID);
        Attribute _peerSequence = AttributeFactory.createPeerSequence(peerSequence);

        PrefixTable addIpv4Table = new PrefixTable(2);
        PrefixTable addIpv6Table = new PrefixTable(2);
        for (PrefixGroup prefixGroup : prefixGroups) {
            if (prefixGroup.getBinding() != null) {
                for (Binding binding : prefixGroup.getBinding()) {
                    Attribute _sgt = AttributeFactory.createSourceGroupTag(prefixGroup.getSgt().getValue());
                    if (binding.getIpPrefix().getIpv4Prefix() != null) {
                        addIpv4Table.addItem(binding.getIpPrefix(), _peerSequence, _sgt);
                    } else if (binding.getIpPrefix().getIpv6Prefix() != null) {
                        addIpv6Table.addItem(binding.getIpPrefix(), _peerSequence, _sgt);
                    } else {
                        throw new UnknownFormatConversionException("PrefixTable: Binding IP address is not supported.");
                    }
                }
            }
        }
        attributes.add(AttributeFactory.createIpv4AddTable(addIpv4Table));
        attributes.add(AttributeFactory.createIpv6AddTable(addIpv6Table));
        return getMessage(MessageType.Update, attributes.toBytes());
    }

    private static Notification decode(Version version, byte[] headerType, byte[] payload) throws Exception {
        MessageType messageType = MessageType.forValue(ArraysUtil.bytes2int(headerType));

        // Remote can send OpenResp with different version
        if(messageType == MessageType.OpenResp) {
            final Version remoteVersion = extractVersion(payload);
            // Override version setting for parsing
            if(remoteVersion != version) {
                version = remoteVersion;
            }
        }

        if (isLegacy(version)) {
            switch (messageType) {
            case Open:
                return LegacyMessageFactory.decodeOpen(payload);
            case OpenResp:
                return LegacyMessageFactory.decodeOpenResp(payload);
            case Update:
                return LegacyMessageFactory.decodeUpdate(payload);
            case Error:
                return decodeErrorMessage(payload);
            case PurgeAll:
                return decodePurgeAll(payload);
            default:
                break;
            }
        } else if (version.equals(Version.Version4)) {
            switch (messageType) {
            case Open:
                try {
                    return decodeOpen(payload);
                } catch (Exception e) {
                    throw new ErrorMessageException(ErrorCode.OpenMessageError, e);
                }
            case OpenResp:
                try {
                    return decodeOpenResp(payload);
                } catch (Exception e) {
                    throw new ErrorMessageException(ErrorCode.OpenMessageError, e);
                }
            case Update:
                return decodeUpdate(payload);
            case Error:
                return decodeErrorMessage(payload);
            case PurgeAll:
                return decodePurgeAll(payload);
            case Keepalive:
                return decodeKeepalive(payload);
            }
        }
        throw new UnknownSxpMessageTypeException();
    }

    public static Notification decodeErrorMessage(byte[] payload) {
        ErrorMessageBuilder messageBuilder = new ErrorMessageBuilder();
        messageBuilder.setType(MessageType.Error);
        messageBuilder.setLength(MESSAGE_HEADER_LENGTH_LENGTH + MESSAGE_HEADER_TYPE_LENGTH + payload.length);
        messageBuilder.setPayload(payload);

        if (ArraysUtil.getBit(payload[0], 8) == 1) {
            messageBuilder.setErrorType(ErrorType.Extended);
            messageBuilder.setErrorCode(ErrorCode.forValue(payload[0] & 0x7F));
            messageBuilder.setErrorSubCode(ErrorSubCode.forValue(payload[1]));
            messageBuilder.setData(ArraysUtil.readBytes(payload, 2));
            messageBuilder.setInformation(messageBuilder.getErrorCode() + " | " + messageBuilder.getErrorSubCode()
                    + getInformation(messageBuilder.getData()));
            return messageBuilder.build();
        }

        messageBuilder.setErrorType(ErrorType.Legacy);
        messageBuilder.setErrorCodeNonExtended(ErrorCodeNonExtended.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(
                payload, 2, 2))));
        messageBuilder.setData(ArraysUtil.readBytes(payload, 4));
        messageBuilder.setInformation(messageBuilder.getErrorCodeNonExtended()
                + getInformation(messageBuilder.getData()));
        return messageBuilder.build();
    }

    public static Notification decodeKeepalive(byte[] payload) {
        KeepaliveMessageBuilder messageBuilder = new KeepaliveMessageBuilder();
        messageBuilder.setType(MessageType.Keepalive);
        messageBuilder.setLength(MESSAGE_HEADER_LENGTH_LENGTH + MESSAGE_HEADER_TYPE_LENGTH + payload.length);
        return messageBuilder.build();
    }

    public static Notification decodeOpen(byte[] payload) throws Exception {
        OpenMessageBuilder messageBuilder = new OpenMessageBuilder();
        messageBuilder.setType(MessageType.Open);
        messageBuilder.setLength(MESSAGE_HEADER_LENGTH_LENGTH + MESSAGE_HEADER_TYPE_LENGTH + payload.length);
        messageBuilder.setPayload(payload);

        Version version = extractVersion(payload);
        ConnectionMode nodeMode = ConnectionMode.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(payload, 4, 4)));
        AttributeList attributes = AttributeList.decode(ArraysUtil.readBytes(payload, 8));

        messageBuilder.setVersion(version);
        messageBuilder.setSxpMode(nodeMode);
        messageBuilder.setAttribute(attributes);
        return messageBuilder.build();
    }

    public static Notification decodeOpenResp(byte[] payload) throws Exception {
        OpenMessageBuilder messageBuilder = new OpenMessageBuilder();
        messageBuilder.setType(MessageType.OpenResp);
        messageBuilder.setLength(MESSAGE_HEADER_LENGTH_LENGTH + MESSAGE_HEADER_TYPE_LENGTH + payload.length);
        messageBuilder.setPayload(payload);

        Version version = extractVersion(payload);
        ConnectionMode nodeMode = ConnectionMode.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(payload, 4, 4)));
        AttributeList attributes = AttributeList.decode(ArraysUtil.readBytes(payload, 8));

        messageBuilder.setVersion(version);
        messageBuilder.setSxpMode(nodeMode);
        messageBuilder.setAttribute(attributes);
        return messageBuilder.build();
    }

    public static Version extractVersion(final byte[] payload) {
        return Version.forValue(ArraysUtil.bytes2int(ArraysUtil.readBytes(payload, 0, 4)));
    }

    public static Notification decodePurgeAll(byte[] payload) {
        PurgeAllMessageBuilder messageBuilder = new PurgeAllMessageBuilder();
        messageBuilder.setType(MessageType.PurgeAll);
        messageBuilder.setLength(MESSAGE_HEADER_LENGTH_LENGTH + MESSAGE_HEADER_TYPE_LENGTH + payload.length);
        return messageBuilder.build();
    }

    public static Notification decodeUpdate(byte[] payload) throws Exception {
        UpdateMessageBuilder messageBuilder = new UpdateMessageBuilder();
        messageBuilder.setType(MessageType.Update);
        messageBuilder.setLength(MESSAGE_HEADER_LENGTH_LENGTH + MESSAGE_HEADER_TYPE_LENGTH + payload.length);
        messageBuilder.setPayload(payload);

        messageBuilder.setAttribute(AttributeList.decode(payload));
        return messageBuilder.build();
    }

    private static String getInformation(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        String result = " | ";
        for (int i = 0; i < data.length; i++) {
            result += data[i] + " ";
        }
        return result.trim();
    }

    protected static ByteBuf getMessage(MessageType messageType, byte[] payload) {
        byte[] header = getMessageHeader(messageType, payload.length);
        int messageLength = header.length + payload.length;
        ByteBuf message = PooledByteBufAllocator.DEFAULT.buffer(messageLength, messageLength);
        message.writeBytes(header);
        message.writeBytes(payload);
        return message;
    }

    private static byte[] getMessageHeader(MessageType messageType, int payloadLength) {
        return ArraysUtil.combine(
                ArraysUtil.int2bytes(MESSAGE_HEADER_LENGTH_LENGTH + MESSAGE_HEADER_TYPE_LENGTH + payloadLength),
                new byte[] { 0x00, 0x00, 0x00, (byte) messageType.getIntValue() });
    }

    protected static boolean isLegacy(Version version) {
        return version.equals(Version.Version1) || version.equals(Version.Version2) || version.equals(Version.Version3);

    }

    public static Notification parse(Version version, ByteBuf request) throws Exception {
        request.resetReaderIndex();
        byte[] headerLength, headerType, payload;
        int messageLength;
        try {
            headerLength = new byte[MESSAGE_HEADER_LENGTH_LENGTH];
            request = request.readBytes(headerLength);

            headerType = new byte[MESSAGE_HEADER_TYPE_LENGTH];
            request = request.readBytes(headerType);

            messageLength = ArraysUtil.bytes2int(headerLength);
            int payloadLength = messageLength
                    - (MESSAGE_HEADER_LENGTH_LENGTH + Configuration.getConstants().getMessageHeaderTypeLength());

            payload = new byte[payloadLength];
            request = request.readBytes(payload);
        } catch (Exception e) {
            throw new ErrorMessageException(ErrorCode.MessageHeaderError, e);
        }

        validate(headerLength.length + headerType.length, payload.length, messageLength);
        return decode(version, headerType, payload);
    }

    public static String toString(byte[] message) {
        String result = "";
        for (int i = 0; i < message.length; i++) {
            if (i == MESSAGE_HEADER_LENGTH_LENGTH + MESSAGE_HEADER_TYPE_LENGTH) {
                result += "| ";
            }
            result += (0xFF & message[i]) + " ";
        }
        return result;
    }

    public static String toString(ByteBuf message) {
        message.resetReaderIndex();
        byte[] _message = new byte[message.readableBytes()];
        message.readBytes(_message);
        message.resetReaderIndex();
        return toString(_message);
    }

    public static String toString(Notification message) {
        String result = "Unrecognized";
        byte[] _message = new byte[0];
        if (message instanceof SxpHeader) {
            MessageType messageType = ((SxpHeader) message).getType();
            if (messageType.equals(MessageType.OpenResp)) {
                result = "RESP";
            } else {
                result = messageType.toString().toUpperCase();
            }

            byte[] length = ArraysUtil.int2bytes(((SxpHeader) message).getLength());
            byte[] type = ArraysUtil.int2bytes(messageType.getIntValue());
            _message = ArraysUtil.combine(_message, length, type);
        }
        if (message instanceof SxpPayload) {
            _message = ArraysUtil.combine(_message, ((SxpPayload) message).getPayload());
        }
        return result += " " + toString(_message);
    }

    private static void validate(int headerLength, int payloadLength, int messageLength) throws Exception {
        if (headerLength + payloadLength > MESSAGE_LENGTH_MAX) {
            throw new ErrorMessageException(ErrorCode.MessageHeaderError, new Exception(
                    "Message maximum length exceeded"));

        } else if (headerLength + payloadLength != messageLength) {
            throw new ErrorMessageException(ErrorCode.MessageHeaderError, new Exception(
                    "Message incorporated length is not consistent"));
        }
    }
}
