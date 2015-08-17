/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.exception;

import org.junit.Test;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.exception.connection.IncompatiblePeerModeException;
import org.opendaylight.sxp.util.exception.connection.IncompatiblePeerVersionException;
import org.opendaylight.sxp.util.exception.connection.NoNetworkInterfacesException;
import org.opendaylight.sxp.util.exception.connection.SocketAddressNotRecognizedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageBindingSourceException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageCompositionException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageConnectionStateException;
import org.opendaylight.sxp.util.exception.message.UpdateMessagePeerSequenceException;
import org.opendaylight.sxp.util.exception.message.UpdateMessagePrefixException;
import org.opendaylight.sxp.util.exception.message.UpdateMessagePrefixGroupsException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageSgtException;
import org.opendaylight.sxp.util.exception.message.attribute.AddressLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeNotFoundException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeTypeException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeVariantException;
import org.opendaylight.sxp.util.exception.message.attribute.CapabilityLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.HoldTimeMaxException;
import org.opendaylight.sxp.util.exception.message.attribute.HoldTimeMinException;
import org.opendaylight.sxp.util.exception.message.attribute.PrefixLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.PrefixTableAttributeIsNotCompactException;
import org.opendaylight.sxp.util.exception.message.attribute.PrefixTableColumnsSizeException;
import org.opendaylight.sxp.util.exception.message.attribute.TlvNotFoundException;
import org.opendaylight.sxp.util.exception.node.BindingNotFoundException;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.sxp.util.exception.node.DatabaseNotFoundException;
import org.opendaylight.sxp.util.exception.node.InetAddressPrefixValueException;
import org.opendaylight.sxp.util.exception.node.NodeIdNotDefinedException;
import org.opendaylight.sxp.util.exception.unknown.UnknownConnectionModeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownContextException;
import org.opendaylight.sxp.util.exception.unknown.UnknownErrorCodeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownErrorSubCodeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownNodeIdException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpConnectionException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpMessageTypeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpNodeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownVersionException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.TlvType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

import java.net.InetSocketAddress;

public class ExceptionTest {

        @Test public void testForExceptionsCoverageCover() throws Exception {
                Exception exception;
                exception = new NotImplementedException();
                exception = new ErrorMessageReceivedException("");
                exception = new ErrorCodeDataLengthException("");

                exception = new UnknownContextException();
                exception = new UnknownConnectionModeException();
                exception = new UnknownErrorCodeException();
                exception = new UnknownErrorSubCodeException();
                exception = new UnknownNodeIdException("");
                exception = new UnknownPrefixException("");
                exception = new UnknownSxpConnectionException("");
                exception = new UnknownSxpMessageTypeException();
                exception = new UnknownSxpNodeException();
                exception = new UnknownTimerTypeException("");
                exception = new UnknownTimerTypeException(TimerType.DeleteHoldDownTimer);
                exception = new UnknownVersionException();

                exception = new BindingNotFoundException();
                exception = new DatabaseAccessException("", "");
                exception = new DatabaseAccessException("", "", exception);
                exception = new DatabaseNotFoundException(DatabaseType.MasterDatabase);
                exception = new InetAddressPrefixValueException();
                exception = new NodeIdNotDefinedException();

                exception = new ChannelHandlerContextDiscrepancyException();
                exception = new ChannelHandlerContextNotFoundException();
                exception = new IncompatiblePeerModeException(ConnectionMode.Both, ConnectionMode.Both);
                exception = new IncompatiblePeerVersionException(true, Version.Version2);
                exception = new IncompatiblePeerVersionException(false, Version.Version2);
                exception = new IncompatiblePeerVersionException(Version.Version1, Version.Version2);
                exception = new NoNetworkInterfacesException();
                exception = new SocketAddressNotRecognizedException(new InetSocketAddress(147));

                exception = new AddressLengthException();
                exception = new AttributeLengthException();
                exception = new AttributeNotFoundException(AttributeType.AddIpv4);
                exception = new AttributeTypeException();
                exception = new AttributeVariantException();
                exception = new CapabilityLengthException();
                exception = new HoldTimeMaxException(0, 0);
                exception = new HoldTimeMinException(0);
                exception = new PrefixLengthException();
                exception = new PrefixTableAttributeIsNotCompactException();
                exception = new PrefixTableColumnsSizeException();
                exception = new TlvNotFoundException(TlvType.Sgt);

                exception = new ErrorMessageException(ErrorCodeNonExtended.NoError, null);
                exception = new UpdateMessageBindingSourceException(DatabaseBindingSource.Sxp);
                exception = new UpdateMessageCompositionException(Version.Version4, false, exception);
                exception = new UpdateMessageConnectionStateException(ConnectionState.AdministrativelyDown);
                exception = new UpdateMessagePeerSequenceException("");
                exception = new UpdateMessagePrefixException("");
                exception = new UpdateMessagePrefixGroupsException("");
                exception = new UpdateMessageSgtException("");

        }

}
