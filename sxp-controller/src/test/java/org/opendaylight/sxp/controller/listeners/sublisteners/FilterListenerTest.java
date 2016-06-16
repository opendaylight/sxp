/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.core.RpcServiceImpl;
import org.opendaylight.sxp.controller.core.SxpDatastoreNode;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.AclFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.fields.filter.entries.acl.filter.entries.AclEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.group.fields.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.peer.groups.SxpPeerGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpPeerGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({RpcServiceImpl.class, DatastoreAccess.class})
public class FilterListenerTest {

    private FilterListener identityListener;
    private DatastoreAccess datastoreAccess;
    private SxpDatastoreNode sxpNode;

    @Before public void setUp() throws Exception {
        datastoreAccess = PowerMockito.mock(DatastoreAccess.class);
        identityListener = new FilterListener(datastoreAccess);
        sxpNode = mock(SxpDatastoreNode.class);
        when(sxpNode.shutdown()).thenReturn(sxpNode);
        PowerMockito.mockStatic(RpcServiceImpl.class);
        PowerMockito.when(RpcServiceImpl.getNode(anyString())).thenReturn(sxpNode);
        PowerMockito.when(RpcServiceImpl.registerNode(any(SxpDatastoreNode.class))).thenReturn(sxpNode);
        PowerMockito.when(RpcServiceImpl.unregisterNode(anyString())).thenReturn(sxpNode);
    }

    private DataObjectModification<SxpFilter> getObjectModification(
            DataObjectModification.ModificationType modificationType, SxpFilter before, SxpFilter after) {
        DataObjectModification<SxpFilter> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(modificationType);
        when(modification.getDataAfter()).thenReturn(after);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataType()).thenReturn(SxpFilter.class);
        return modification;
    }

    private InstanceIdentifier<SxpPeerGroup> getIdentifier() {
        return NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(new NodeId("0.0.0.0")))
                .augmentation(SxpNodeIdentity.class)
                .child(SxpPeerGroups.class)
                .child(SxpPeerGroup.class, new SxpPeerGroupKey("GROUP"));
    }

    private SxpFilter getSxpFilter(int entries) {
        SxpFilterBuilder builder = new SxpFilterBuilder();
        builder.setFilterType(FilterType.InboundDiscarding);
        builder.setFilterSpecific(FilterSpecific.AccessOrPrefixList);
        List<AclEntry> entrList = new ArrayList<>();
        for (int i = 0; i < entries; i++) {
            entrList.add(mock(AclEntry.class));
        }
        builder.setFilterEntries(new AclFilterEntriesBuilder().setAclEntry(entrList).build());
        return builder.build();
    }

    @Test public void testHandleOperational_1() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, getSxpFilter(5)),
                getIdentifier());
        verify(sxpNode).addFilterToPeerGroup(anyString(), any(SxpFilter.class));
    }

    @Test public void testHandleOperational_2() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getSxpFilter(5), null),
                getIdentifier());
        verify(sxpNode).removeFilterFromPeerGroup(anyString(), any(FilterType.class), any(FilterSpecific.class));
    }

    @Test public void testHandleOperational_3() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED, getSxpFilter(5),
                        getSxpFilter(8)), getIdentifier());
        verify(sxpNode).removeFilterFromPeerGroup(anyString(), any(FilterType.class), any(FilterSpecific.class));
        verify(sxpNode).addFilterToPeerGroup(anyString(), any(SxpFilter.class));
    }

    @Test public void testHandleOperational_4() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.DELETE, getSxpFilter(5), null),
                getIdentifier());
        verify(sxpNode).removeFilterFromPeerGroup(anyString(), any(FilterType.class), any(FilterSpecific.class));
    }

    @Test public void testGetModifications() throws Exception {
        assertNotNull(identityListener.getIdentifier(new SxpFilterBuilder().setFilterType(FilterType.InboundDiscarding)
                .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                .build(), getIdentifier()));
        assertTrue(identityListener.getIdentifier(new SxpFilterBuilder().setFilterType(FilterType.InboundDiscarding)
                .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                .build(), getIdentifier()).getTargetType().equals(SxpFilter.class));
    }
}
