/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.core.IsEqual;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.sxp.core.messaging.AttributeList;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.core.messaging.legacy.LegacyMessageFactory;
import org.opendaylight.sxp.core.service.BindingHandler;
import org.opendaylight.sxp.util.database.Database;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.SxpDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ErrorCodeNonExtended;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.Notification;

public class MessagesCompositionTest {

    private static NodeId nodeId;

    private static List<NodeId> peerSequence;

    private static List<PrefixGroup> prefixGroups;

    @BeforeClass
    public static void init() throws Exception {
        nodeId = NodeIdConv.createNodeId("192.168.0.1");

        prefixGroups = new ArrayList<>();
        prefixGroups.add(Database.createPrefixGroup(10000, "192.168.0.1/32"));
        prefixGroups.add(Database.createPrefixGroup(20000, "2001::1/64", "10.10.10.10/30"));
        prefixGroups.add(Database.createPrefixGroup(30000, "2002::1/128"));
        prefixGroups.add(Database.createPrefixGroup(40000, "11.11.11.0/29"));
        prefixGroups.add(Database.createPrefixGroup(65000, "172.168.1.0/28"));

        peerSequence = new ArrayList<NodeId>();
        peerSequence.add(NodeIdConv.createNodeId("192.168.0.2"));
        peerSequence.add(NodeIdConv.createNodeId("192.168.0.3"));
    }

    @Test
    public void MessageCompositionLegacy() throws Exception {
        ByteBuf message = LegacyMessageFactory.createOpen(Version.Version1, ConnectionMode.Listener);
        byte[] result = new byte[] { 0, 0, 0, 16, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 2 };
        assertThat(toBytes(message), IsEqual.equalTo(result));

        message = LegacyMessageFactory.createOpenResp(Version.Version3, ConnectionMode.Speaker);
        result = new byte[] { 0, 0, 0, 16, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0, 1 };
        assertThat(toBytes(message), IsEqual.equalTo(result));

        MasterDatabaseImpl masterDatabase = new MasterDatabaseImpl();
        masterDatabase.addBindingsLocal(prefixGroups);

        message = LegacyMessageFactory.createUpdate(masterDatabase.get(), false, Version.Version1);
        message = LegacyMessageFactory.createUpdate(masterDatabase.get(), false, Version.Version2);
        message = LegacyMessageFactory.createUpdate(masterDatabase.get(), false, Version.Version3);

        message = LegacyMessageFactory.createError(ErrorCodeNonExtended.MessageParseError);
        result = new byte[] { 0, 0, 0, 12, 0, 0, 0, 4, 0, 0, 0, 2 };
        assertThat(toBytes(message), IsEqual.equalTo(result));

        message = MessageFactory.createPurgeAll();
        result = new byte[] { 0, 0, 0, 8, 0, 0, 0, 5 };
        assertThat(toBytes(message), IsEqual.equalTo(result));
    }

    @Test
    public void MessageCompositionVersion4() throws Exception {
        ByteBuf message = MessageFactory.createOpen(Version.Version4, ConnectionMode.Listener, nodeId, 120, 150);
        byte[] result = new byte[] { 0, 0, 0, 32, 0, 0, 0, 1, 0, 0, 0, 4, 0, 0, 0, 2, 80, 6, 6, 3, 0, 2, 0, 1, 0, 80,
                7, 4, 0, 120, 0, -106 };
        assertThat(toBytes(message), IsEqual.equalTo(result));

        message = MessageFactory.createUpdateAddPrefixes(nodeId, peerSequence, prefixGroups);
        Notification notification = MessageFactory.parse(Version.Version4, message);
        assertNotNull(notification);
        UpdateMessage updateMessage = (UpdateMessage) notification;
        assertNotNull(AttributeList.get(updateMessage.getAttribute(), AttributeType.PeerSequence));
        assertNotNull(AttributeList.get(updateMessage.getAttribute(), AttributeType.Ipv4AddPrefix));
        assertNotNull(AttributeList.get(updateMessage.getAttribute(), AttributeType.Ipv6AddPrefix));

        message = MessageFactory.createUpdateTableAddPrefixes(nodeId, peerSequence, prefixGroups);
        notification = MessageFactory.parse(Version.Version4, message);
        assertNotNull(notification);
        updateMessage = (UpdateMessage) notification;
        assertNotNull(AttributeList.get(updateMessage.getAttribute(), AttributeType.Ipv4AddTable));
        assertNotNull(AttributeList.get(updateMessage.getAttribute(), AttributeType.Ipv6AddTable));

        message = MessageFactory.createUpdateAddPrefixes(nodeId, peerSequence, prefixGroups);
        notification = MessageFactory.parse(Version.Version4, message);
        assertNotNull(notification);

        SxpDatabase database = BindingHandler.processMessageAddition((UpdateMessage) notification);
        int count = 0;
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup pathGroup : database
                .getPathGroup()) {
            count += pathGroup.getPrefixGroup().size();
        }
        assertEquals(prefixGroups.size(), count);

        message = MessageFactory.createKeepalive();
        result = new byte[] { 0, 0, 0, 8, 0, 0, 0, 6 };
        assertThat(toBytes(message), IsEqual.equalTo(result));
    }

    private byte[] toBytes(ByteBuf message) {
        byte[] _message = new byte[message.readableBytes()];
        message.readBytes(_message);
        message.release();
        return _message;
    }
}
