/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({Configuration.class, DatastoreAccess.class})
public class NodeIdentityListenerTest {

    private NodeIdentityListener identityListener;
    private DatastoreAccess datastoreAccess;
    private SxpNode sxpNode;

    @Before public void setUp() throws Exception {
        datastoreAccess = mock(DatastoreAccess.class);
        identityListener = new NodeIdentityListener(datastoreAccess);
        sxpNode = mock(SxpNode.class);
        when(sxpNode.shutdown()).thenReturn(sxpNode);
        PowerMockito.mockStatic(Configuration.class);
        PowerMockito.mockStatic(DatastoreAccess.class);
        PowerMockito.when(DatastoreAccess.getInstance()).thenReturn(mock(DatastoreAccess.class));
        PowerMockito.when(DatastoreAccess.getInstance(any(DataBroker.class))).thenReturn(mock(DatastoreAccess.class));
        PowerMockito.when(Configuration.getRegisteredNode(anyString())).thenReturn(sxpNode);
        PowerMockito.when(Configuration.register(any(SxpNode.class))).thenReturn(sxpNode);
        PowerMockito.when(Configuration.unregister(anyString())).thenReturn(sxpNode);
        PowerMockito.when(Configuration.getConstants()).thenCallRealMethod();
    }

    @Test public void testRegister() throws Exception {
        DataBroker dataBroker = mock(DataBroker.class);
        identityListener.register(dataBroker, LogicalDatastoreType.CONFIGURATION);
        verify(dataBroker).registerDataTreeChangeListener(any(DataTreeIdentifier.class), eq(identityListener));
    }

    private DataTreeModification<SxpNodeIdentity> getTreeModification(
            DataObjectModification.ModificationType modificationType, LogicalDatastoreType datastoreType,
            SxpNodeIdentity before, SxpNodeIdentity after) {
        DataTreeModification<SxpNodeIdentity> modification = mock(DataTreeModification.class);
        DataObjectModification<SxpNodeIdentity>
                objectModification =
                getObjectModification(modificationType, before, after);
        when(modification.getRootNode()).thenReturn(objectModification);
        DataTreeIdentifier<SxpNodeIdentity>
                identifier =
                new DataTreeIdentifier<>(datastoreType,
                        NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(new NodeId("0.0.0.0")))
                                .augmentation(SxpNodeIdentity.class));
        when(modification.getRootPath()).thenReturn(identifier);
        return modification;
    }

    private DataObjectModification<SxpNodeIdentity> getObjectModification(
            DataObjectModification.ModificationType modificationType, SxpNodeIdentity before, SxpNodeIdentity after) {
        DataObjectModification<SxpNodeIdentity> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(modificationType);
        when(modification.getDataAfter()).thenReturn(after);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataType()).thenReturn(SxpNodeIdentity.class);
        return modification;
    }

    private SxpNodeIdentity createIdentity(boolean enabled, String ip, int port, Version version) {
        SxpNodeIdentityBuilder builder = new SxpNodeIdentityBuilder();
        builder.setCapabilities(Configuration.getCapabilities(Version.Version4));
        builder.setSecurity(new SecurityBuilder().build());
        builder.setEnabled(enabled);
        builder.setConnections(new ConnectionsBuilder().build());
        builder.setVersion(version);
        builder.setTcpPort(new PortNumber(port));
        builder.setSourceIp(new IpAddress(ip.toCharArray()));
        return builder.build();
    }

    @Test public void testOnDataTreeChanged_1() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.OPERATIONAL,
                        null, createIdentity(true, "0.0.0.0", 64999, Version.Version4)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).start();
    }

    @Test public void testOnDataTreeChanged_2() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.OPERATIONAL,
                        createIdentity(true, "0.0.0.0", 64999, Version.Version4), null));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
    }

    @Test public void testOnDataTreeChanged_3() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.DELETE, LogicalDatastoreType.OPERATIONAL,
                        createIdentity(true, "0.0.0.0", 64999, Version.Version4), null));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
    }

    @Test public void testOnDataTreeChanged_4() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4),
                createIdentity(true, "0.0.0.0", 64999, Version.Version3)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
        verify(sxpNode).start();
    }

    @Test public void testOnDataTreeChanged_5() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4),
                createIdentity(true, "0.0.0.0", 64990, Version.Version4)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
        verify(sxpNode).start();
    }

    @Test public void testOnDataTreeChanged_6() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4),
                createIdentity(true, "0.0.0.1", 64999, Version.Version4)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
        verify(sxpNode).start();
    }

    @Test public void testOnDataTreeChanged_7() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4),
                createIdentity(false, "0.0.0.0", 64999, Version.Version4)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
    }
}
