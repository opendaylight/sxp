/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.core.SxpDatastoreNode;
import org.opendaylight.sxp.controller.listeners.spi.Listener;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({Configuration.class, DatastoreAccess.class})
public class NodeIdentityListenerTest {

    private NodeIdentityListener identityListener;
    private Listener listener;
    private DatastoreAccess datastoreAccess;
    private SxpDatastoreNode sxpNode;

    @Before public void setUp() throws Exception {
        datastoreAccess = mock(DatastoreAccess.class);
        listener = mock(Listener.class);
        identityListener = new NodeIdentityListener(datastoreAccess);
        identityListener.addSubListener(listener);
        sxpNode = mock(SxpDatastoreNode.class);
        when(sxpNode.shutdown()).thenReturn(sxpNode);
        PowerMockito.mockStatic(Configuration.class);
        PowerMockito.mockStatic(DatastoreAccess.class);
        PowerMockito.when(DatastoreAccess.getInstance(any(DatastoreAccess.class))).thenReturn(datastoreAccess);
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

    private SxpNodeIdentity createIdentity(boolean enabled, String ip, int port, Version version,
            int deleteHoldDownTimer) {
        SxpNodeIdentityBuilder builder = new SxpNodeIdentityBuilder();
        builder.setCapabilities(Configuration.getCapabilities(Version.Version4));
        builder.setSecurity(new SecurityBuilder().build());
        builder.setEnabled(enabled);
        builder.setSxpDomains(new SxpDomainsBuilder().setSxpDomain(Collections.singletonList(
                new SxpDomainBuilder().setConnections(new ConnectionsBuilder().build()).build())).build());
        builder.setVersion(version);
        builder.setTcpPort(new PortNumber(port));
        builder.setSourceIp(new IpAddress(ip.toCharArray()));
        builder.setTimers(new TimersBuilder().setDeleteHoldDownTime(deleteHoldDownTimer).build());
        return builder.build();
    }

    @Test public void testOnDataTreeChanged_1() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.OPERATIONAL,
                        null, createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).start();
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test public void testOnDataTreeChanged_2() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.OPERATIONAL,
                        createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120), null));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
    }

    @Test public void testOnDataTreeChanged_3() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.DELETE, LogicalDatastoreType.OPERATIONAL,
                        createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120), null));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).close();
    }

    @Test public void testOnDataTreeChanged_4() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120),
                createIdentity(true, "0.0.0.0", 64999, Version.Version3, 120)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
        verify(sxpNode).start();
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test public void testOnDataTreeChanged_5() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120),
                createIdentity(true, "0.0.0.0", 64990, Version.Version4, 120)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
        verify(sxpNode).start();
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test public void testOnDataTreeChanged_6() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120),
                createIdentity(true, "0.0.0.1", 64999, Version.Version4, 120)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
        verify(sxpNode).start();
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test public void testOnDataTreeChanged_7() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120),
                createIdentity(false, "0.0.0.0", 64999, Version.Version4, 120)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdown();
    }

    @Test public void testOnDataTreeChanged_8() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(false, "0.0.0.0", 64999, Version.Version4, 120),
                createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).start();
    }

    @Test public void testOnDataTreeChanged_9() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.OPERATIONAL, createIdentity(true, "0.0.0.0", 64999, Version.Version4, 120),
                createIdentity(true, "0.0.0.0", 64999, Version.Version4, 12)));

        identityListener.onDataTreeChanged(modificationList);
        verify(sxpNode).shutdownConnections();
    }

    @Test public void testOnDataTreeChanged_10() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.CONFIGURATION, null,
                createIdentity(true, "0.0.0.0", 64999, Version.Version4, 12)));

        identityListener.onDataTreeChanged(modificationList);
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test public void testOnDataTreeChanged_11() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(getTreeModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                LogicalDatastoreType.CONFIGURATION, createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12),
                createIdentity(true, "0.0.0.0", 64999, Version.Version4, 12)));

        identityListener.onDataTreeChanged(modificationList);
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test public void testOnDataTreeChanged_12() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.DELETE, LogicalDatastoreType.CONFIGURATION,
                        createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12), null));

        identityListener.onDataTreeChanged(modificationList);
        verify(datastoreAccess).checkAndDelete(any(InstanceIdentifier.class), eq(LogicalDatastoreType.OPERATIONAL));
    }

    @Test public void testOnDataTreeChanged_13() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.CONFIGURATION,
                        null, createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12)));

        identityListener.onDataTreeChanged(modificationList);
        verify(datastoreAccess).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
        verify(datastoreAccess, never()).checkAndDelete(any(InstanceIdentifier.class),
                eq(LogicalDatastoreType.OPERATIONAL));
    }

    @Test public void testOnDataTreeChanged_14() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.CONFIGURATION,
                        createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12),
                        createIdentity(true, "0.0.0.0", 64999, Version.Version3, 122)));

        identityListener.onDataTreeChanged(modificationList);
        verify(listener).handleChange(anyList(), any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(datastoreAccess).mergeSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
        verify(datastoreAccess, never()).checkAndDelete(any(InstanceIdentifier.class),
                eq(LogicalDatastoreType.OPERATIONAL));
    }

    @Test public void testOnDataTreeChanged_15() throws Exception {
        List<DataTreeModification<SxpNodeIdentity>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.CONFIGURATION,
                        createIdentity(true, "0.0.0.0", 64999, Version.Version3, 12), null));

        identityListener.onDataTreeChanged(modificationList);
        verify(datastoreAccess).checkAndDelete(any(InstanceIdentifier.class), eq(LogicalDatastoreType.OPERATIONAL));
    }
}
