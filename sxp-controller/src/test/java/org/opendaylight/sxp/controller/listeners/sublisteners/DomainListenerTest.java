/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.SxpDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;

@RunWith(PowerMockRunner.class) @PrepareForTest({Configuration.class, DatastoreAccess.class})
public class DomainListenerTest {

    private DomainListener identityListener;
    private DatastoreAccess datastoreAccess;
    private SxpNode sxpNode;

    @Before public void setUp() throws Exception {
        datastoreAccess = PowerMockito.mock(DatastoreAccess.class);
        identityListener = new DomainListener(datastoreAccess);
        sxpNode = mock(SxpNode.class);
        when(sxpNode.shutdown()).thenReturn(sxpNode);
        when(sxpNode.removeDomain(anyString())).thenReturn(mock(org.opendaylight.sxp.core.SxpDomain.class));
        PowerMockito.mockStatic(Configuration.class);
        PowerMockito.when(Configuration.getRegisteredNode(anyString())).thenReturn(sxpNode);
        PowerMockito.when(Configuration.register(any(SxpNode.class))).thenReturn(sxpNode);
        PowerMockito.when(Configuration.unRegister(anyString())).thenReturn(sxpNode);
        PowerMockito.when(Configuration.getConstants()).thenCallRealMethod();
    }

    private DataObjectModification<SxpDomain> getObjectModification(
            DataObjectModification.ModificationType modificationType, SxpDomain before, SxpDomain after) {
        DataObjectModification<SxpDomain> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(modificationType);
        when(modification.getDataAfter()).thenReturn(after);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataType()).thenReturn(SxpDomain.class);
        return modification;
    }

    private InstanceIdentifier<SxpNodeIdentity> getIdentifier() {
        return NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(new NodeId("0.0.0.0")))
                .augmentation(SxpNodeIdentity.class);
    }

    private SxpDomain getDomain(String name) {
        SxpDomainBuilder builder = new SxpDomainBuilder();
        builder.setConnections(new ConnectionsBuilder().setConnection(new ArrayList<>()).build());
        builder.setSxpDatabase(new SxpDatabaseBuilder().setBindingDatabase(new ArrayList<>()).build());
        builder.setMasterDatabase(new MasterDatabaseBuilder().setMasterDatabaseBinding(new ArrayList<>()).build());
        builder.setDomainName(name);
        return builder.build();
    }

    @Test public void testHandleOperational_1() throws Exception {
        SxpDomain domain = getDomain("global");
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, domain), getIdentifier());
        verify(sxpNode).addDomain(domain);

        domain = getDomain("secure");
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, domain), getIdentifier());
        verify(sxpNode).addDomain(domain);
    }

    @Test public void testHandleOperational_2() throws Exception {
        SxpDomain domain = getDomain("global");
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, domain, null), getIdentifier());
        verify(sxpNode).removeDomain("global");

        domain = getDomain("secure");
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, domain, null), getIdentifier());
        verify(sxpNode).removeDomain("secure");
    }

    @Test public void testGetModifications() throws Exception {
        assertNotNull(identityListener.getIdentifier(new SxpDomainBuilder().setDomainName("global").build(),
                getIdentifier()));
        assertTrue(
                identityListener.getIdentifier(new SxpDomainBuilder().setDomainName("global").build(), getIdentifier())
                        .getTargetType()
                        .equals(SxpDomain.class));
    }
}
