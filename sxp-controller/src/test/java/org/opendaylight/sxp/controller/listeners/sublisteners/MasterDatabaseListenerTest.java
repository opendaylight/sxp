/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.core.SxpDatastoreNode;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabaseBuilder;
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
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({Configuration.class, DatastoreAccess.class})
public class MasterDatabaseListenerTest {

    private MasterDatabaseListener identityListener;
    private DatastoreAccess datastoreAccess;
    private SxpNode sxpNode;

    @Before public void setUp() throws Exception {
        datastoreAccess = PowerMockito.mock(DatastoreAccess.class);
        identityListener = new MasterDatabaseListener(datastoreAccess);
        sxpNode = mock(SxpNode.class);
        when(sxpNode.getDomain(anyString())).thenReturn(mock(org.opendaylight.sxp.core.SxpDomain.class));
        when(sxpNode.shutdown()).thenReturn(sxpNode);
        when(datastoreAccess.readSynchronous(eq(SxpDatastoreNode.getIdentifier("0.0.0.0")
                .child(SxpDomains.class)
                .child(SxpDomain.class, new SxpDomainKey(SxpNode.DEFAULT_DOMAIN))
                .child(MasterDatabase.class)), any(LogicalDatastoreType.class))).thenReturn(
                new MasterDatabaseBuilder().setMasterDatabaseBinding(new ArrayList<>()).build());
        PowerMockito.mockStatic(Configuration.class);
        PowerMockito.when(Configuration.getRegisteredNode(anyString())).thenReturn(sxpNode);
        PowerMockito.when(Configuration.register(any(SxpNode.class))).thenReturn(sxpNode);
        PowerMockito.when(Configuration.unRegister(anyString())).thenReturn(sxpNode);
        PowerMockito.when(Configuration.getConstants()).thenCallRealMethod();
    }

    private DataObjectModification<MasterDatabase> getObjectModification(MasterDatabase before, MasterDatabase after) {
        DataObjectModification<MasterDatabase> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);
        when(modification.getDataAfter()).thenReturn(after);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataType()).thenReturn(MasterDatabase.class);
        return modification;
    }

    private InstanceIdentifier<SxpDomain> getIdentifier() {
        return NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(new NodeId("0.0.0.0")))
                .augmentation(SxpNodeIdentity.class)
                .child(SxpDomains.class)
                .child(SxpDomain.class, new SxpDomainKey("global"));
    }

    private MasterDatabase getMasterDatabase(MasterDatabaseBinding... bindings) {
        return new MasterDatabaseBuilder().setMasterDatabaseBinding(
                bindings == null ? Collections.emptyList() : Arrays.asList(bindings)).build();
    }

    private MasterDatabaseBinding getBinding(String prefix, int sgt) {
        return new MasterDatabaseBindingBuilder().setIpPrefix(new IpPrefix(prefix.toCharArray()))
                .setSecurityGroupTag(new Sgt(sgt))
                .setPeerSequence(new PeerSequenceBuilder().build())
                .build();
    }

    @Test public void testHandleConfig_1() throws Exception {
        identityListener.handleConfig(getObjectModification(getMasterDatabase(null), getMasterDatabase(null)),
                getIdentifier());
        verify(datastoreAccess, never()).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
    }

    @Test public void testHandleConfig_2() throws Exception {
        identityListener.handleConfig(
                getObjectModification(getMasterDatabase(null), getMasterDatabase(getBinding("1.1.1.1/32", 1))),
                getIdentifier());
        verify(datastoreAccess).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
    }

    @Test public void testHandleOperational_1() throws Exception {
        identityListener.handleOperational(
                getObjectModification(getMasterDatabase(null), getMasterDatabase(getBinding("1.1.1.1/32", 1))),
                getIdentifier(), sxpNode);

        verify(sxpNode).putLocalBindingsMasterDatabase(anyList(), anyString());
        verify(sxpNode, never()).removeLocalBindingsMasterDatabase(anyList(), anyString());
    }

    @Test public void testHandleOperational_2() throws Exception {
        identityListener.handleOperational(getObjectModification(getMasterDatabase(getBinding("1.1.1.1/32", 1)),
                getMasterDatabase(getBinding("1.1.1.1/32", 1))), getIdentifier(), sxpNode);

        verify(sxpNode, never()).putLocalBindingsMasterDatabase(anyList(), anyString());
        verify(sxpNode, never()).removeLocalBindingsMasterDatabase(anyList(), anyString());
    }

    @Test public void testHandleOperational_3() throws Exception {
        identityListener.handleOperational(
                getObjectModification(getMasterDatabase(getBinding("1.1.1.1/32", 1)), getMasterDatabase(null)),
                getIdentifier(), sxpNode);

        verify(sxpNode, never()).putLocalBindingsMasterDatabase(anyList(), anyString());
        verify(sxpNode).removeLocalBindingsMasterDatabase(anyList(), anyString());
    }

    @Test public void testHandleOperational_4() throws Exception {
        identityListener.handleOperational(
                getObjectModification(getMasterDatabase(getBinding("1.1.1.1/32", 1), getBinding("1.1.1.8/32", 10)),
                        getMasterDatabase(getBinding("1.1.1.1/32", 1), getBinding("1.1.10.1/32", 100))),
                getIdentifier(), sxpNode);

        verify(sxpNode).putLocalBindingsMasterDatabase(anyList(), anyString());
        verify(sxpNode).removeLocalBindingsMasterDatabase(anyList(), anyString());
    }

    @Test public void testGetModifications() throws Exception {
        assertNotNull(identityListener.getIdentifier(new MasterDatabaseBuilder().build(), getIdentifier()));
        assertTrue(identityListener.getIdentifier(new MasterDatabaseBuilder().build(), getIdentifier())
                .getTargetType()
                .equals(MasterDatabase.class));

        assertNotNull(identityListener.getObjectModifications(null));
        assertNotNull(identityListener.getObjectModifications(mock(DataObjectModification.class)));
        assertNotNull(identityListener.getModifications(null));
        DataTreeModification dtm = mock(DataTreeModification.class);
        when(dtm.getRootNode()).thenReturn(mock(DataObjectModification.class));
        assertNotNull(identityListener.getModifications(dtm));
    }
}
