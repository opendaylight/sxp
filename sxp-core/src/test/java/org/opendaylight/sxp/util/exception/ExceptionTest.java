/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.exception;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.IncompatiblePeerModeException;
import org.opendaylight.sxp.util.exception.connection.IncompatiblePeerVersionException;
import org.opendaylight.sxp.util.exception.connection.NoNetworkInterfacesException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageCompositionException;
import org.opendaylight.sxp.util.exception.message.UpdateMessagePeerSequenceException;
import org.opendaylight.sxp.util.exception.message.UpdateMessagePrefixException;
import org.opendaylight.sxp.util.exception.message.UpdateMessagePrefixGroupsException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageSgtException;
import org.opendaylight.sxp.util.exception.message.attribute.AddressLengthException;
import org.opendaylight.sxp.util.exception.message.attribute.AttributeTypeException;
import org.opendaylight.sxp.util.exception.message.attribute.PrefixLengthException;
import org.opendaylight.sxp.util.exception.node.BindingNotFoundException;
import org.opendaylight.sxp.util.exception.node.InetAddressPrefixValueException;
import org.opendaylight.sxp.util.exception.node.NodeIdNotDefinedException;
import org.opendaylight.sxp.util.exception.unknown.UnknownConnectionModeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownContextException;
import org.opendaylight.sxp.util.exception.unknown.UnknownErrorCodeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownErrorSubCodeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpConnectionException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpMessageTypeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownSxpNodeException;
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorSubCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;

/**
 *
 * @author Martin Dindoffer
 */
public class ExceptionTest {

    @Test
    public void testExceptions() {
        Assert.assertNotNull(new AddressLengthException());
        Assert.assertNotNull(new AttributeTypeException());
        Assert.assertNotNull(new BindingNotFoundException());
        Assert.assertNotNull(new ChannelHandlerContextDiscrepancyException());
        Assert.assertNotNull(new ErrorMessageException(ErrorCode.MessageHeaderError, ErrorSubCode.MalformedAttribute, new byte[0], null).getData());
        Assert.assertNotNull(new IncompatiblePeerModeException(null, null));
        Assert.assertNotNull(new IncompatiblePeerVersionException(false, Version.Version1));
        Assert.assertNotNull(new IncompatiblePeerVersionException(true, Version.Version1));
        Assert.assertNotNull(new InetAddressPrefixValueException());
        Assert.assertNotNull(new NodeIdNotDefinedException());
        Assert.assertNotNull(new NoNetworkInterfacesException(null));
        Assert.assertNotNull(new NotImplementedException());
        Assert.assertNotNull(new PrefixLengthException());
        Assert.assertNotNull(new UnknownConnectionModeException());
        Assert.assertNotNull(new UnknownContextException());
        Assert.assertNotNull(new UnknownErrorCodeException());
        Assert.assertNotNull(new UnknownErrorSubCodeException());
        Assert.assertNotNull(new UnknownSxpConnectionException(null));
        Assert.assertNotNull(new UnknownSxpMessageTypeException());
        Assert.assertNotNull(new UnknownSxpNodeException());
        Assert.assertNotNull(new UpdateMessagePeerSequenceException(null));
        Assert.assertNotNull(new UpdateMessagePrefixException(null));
        Assert.assertNotNull(new UpdateMessagePrefixGroupsException(null));
        Assert.assertNotNull(new UpdateMessageSgtException(null));
        Assert.assertNotNull(new UnknownTimerTypeException(""));
        Assert.assertNotNull(new UnknownTimerTypeException(TimerType.DeleteHoldDownTimer));
        Assert.assertNotNull(new UpdateMessageCompositionException(Version.Version1, true, new Exception()));
    }

}
