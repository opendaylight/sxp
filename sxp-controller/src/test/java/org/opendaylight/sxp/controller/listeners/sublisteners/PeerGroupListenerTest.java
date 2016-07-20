/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpPeersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.sxp.peers.SxpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.sxp.peers.SxpPeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({Configuration.class, DatastoreAccess.class})
public class PeerGroupListenerTest {

    private PeerGroupListener identityListener;
    private DatastoreAccess datastoreAccess;
    private SxpNode sxpNode;

    @Before public void setUp() throws Exception {
        datastoreAccess = PowerMockito.mock(DatastoreAccess.class);
        identityListener = new PeerGroupListener(datastoreAccess);
        sxpNode = mock(SxpNode.class);
        when(sxpNode.shutdown()).thenReturn(sxpNode);
        PowerMockito.mockStatic(Configuration.class);
        PowerMockito.when(Configuration.getRegisteredNode(anyString())).thenReturn(sxpNode);
        PowerMockito.when(Configuration.register(any(SxpNode.class))).thenReturn(sxpNode);
        PowerMockito.when(Configuration.unRegister(anyString())).thenReturn(sxpNode);
        PowerMockito.when(Configuration.getConstants()).thenCallRealMethod();
    }

    private DataObjectModification<SxpPeerGroup> getObjectModification(
            DataObjectModification.ModificationType modificationType, SxpPeerGroup before, SxpPeerGroup after) {
        DataObjectModification<SxpPeerGroup> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(modificationType);
        when(modification.getDataAfter()).thenReturn(after);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataType()).thenReturn(SxpPeerGroup.class);
        return modification;
    }

    private DataObjectModification<SxpPeerGroups> getObjectModification(DataObjectModification<SxpPeerGroup> change) {
        DataObjectModification<SxpPeerGroups> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(modification.getDataType()).thenReturn(SxpPeerGroups.class);
        when(modification.getModifiedChildren()).thenReturn(Collections.singletonList(change));
        return modification;
    }

    private InstanceIdentifier<SxpNodeIdentity> getIdentifier() {
        return NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(new NodeId("0.0.0.0")))
                .augmentation(SxpNodeIdentity.class);
    }

    private SxpPeerGroup getPeerGroup(String name, int peers) {
        SxpPeerGroupBuilder builder = new SxpPeerGroupBuilder();
        builder.setName(name);
        List<SxpPeer> peerList = new ArrayList<>();
        for (int i = 0; i < peers; i++) {
            peerList.add(new SxpPeerBuilder().setPeerAddress(new IpAddress(("1.0.0." + i).toCharArray())).build());
        }
        builder.setSxpPeers(new SxpPeersBuilder().setSxpPeer(peerList).build());
        return builder.build();
    }

    @Test public void testHandleOperational_1() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, getPeerGroup("GR", 2)),
                getIdentifier(), sxpNode);
        verify(sxpNode).addPeerGroup(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup.class));
    }

    @Test public void testHandleOperational_2() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getPeerGroup("GR", 2), null),
                getIdentifier(), sxpNode);
        verify(sxpNode).removePeerGroup(anyString());
    }

    @Test public void testHandleOperational_3() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED, getPeerGroup("GR", 5),
                        getPeerGroup("GR", 2)), getIdentifier(), sxpNode);
        verify(sxpNode).removePeerGroup(anyString());
        verify(sxpNode).addPeerGroup(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.SxpPeerGroup.class));
    }

    @Test public void testHandleOperational_4() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.DELETE, getPeerGroup("GR", 5), null),
                getIdentifier(), sxpNode);
        verify(sxpNode).removePeerGroup(anyString());
    }

    @Test public void testGetIdentifier() throws Exception {
        assertNotNull(identityListener.getIdentifier(new SxpPeerGroupBuilder().setName("TO").build(), getIdentifier()));
        assertTrue(identityListener.getIdentifier(new SxpPeerGroupBuilder().setName("TO").build(), getIdentifier())
                .getTargetType()
                .equals(SxpPeerGroup.class));
    }

    @Test public void testHandleChange() throws Exception {
        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getPeerGroup("GR", 2),
                        getPeerGroup("GR", 3)))), LogicalDatastoreType.OPERATIONAL, getIdentifier());
        verify(datastoreAccess, never()).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
        verify(datastoreAccess, never()).mergeSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
        verify(datastoreAccess).checkAndDelete(any(InstanceIdentifier.class), eq(LogicalDatastoreType.OPERATIONAL));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, getPeerGroup("GR", 2)))),
                LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(datastoreAccess).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getPeerGroup("GR", 2),
                        getPeerGroup("GR", 3)))), LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(datastoreAccess).mergeSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.DELETE, getPeerGroup("GR", 3),
                        getPeerGroup("GR", 2)))), LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(datastoreAccess, times(2)).checkAndDelete(any(InstanceIdentifier.class),
                eq(LogicalDatastoreType.OPERATIONAL));
    }

    @Test public void testGetModifications() throws Exception {
        assertNotNull(identityListener.getObjectModifications(null));
        assertNotNull(identityListener.getObjectModifications(mock(DataObjectModification.class)));
        assertNotNull(identityListener.getModifications(null));
        DataTreeModification dtm = mock(DataTreeModification.class);
        when(dtm.getRootNode()).thenReturn(mock(DataObjectModification.class));
        assertNotNull(identityListener.getModifications(dtm));
    }
}
