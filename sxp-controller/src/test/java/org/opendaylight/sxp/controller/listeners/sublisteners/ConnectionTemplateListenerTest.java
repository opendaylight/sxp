/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import java.net.SocketAddress;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.ConnectionTemplates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.connection.templates.ConnectionTemplate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.templates.fields.connection.templates.ConnectionTemplateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({Configuration.class, DatastoreAccess.class})
public class ConnectionTemplateListenerTest {

    private ConnectionTemplateListener identityListener;
    private DatastoreAccess datastoreAccess;
    private SxpNode sxpNode;
    private org.opendaylight.sxp.core.SxpDomain domain;
    private SxpConnection connection;

    @Before public void setUp() throws Exception {
        datastoreAccess = PowerMockito.mock(DatastoreAccess.class);
        domain = mock(org.opendaylight.sxp.core.SxpDomain.class);
        identityListener = new ConnectionTemplateListener(datastoreAccess);
        sxpNode = mock(SxpNode.class);
        when(sxpNode.getDomain(anyString())).thenReturn(domain);
        connection = mock(SxpConnection.class);
        when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(connection);
        when(sxpNode.shutdown()).thenReturn(sxpNode);
        PowerMockito.mockStatic(Configuration.class);
        PowerMockito.when(Configuration.getRegisteredNode(anyString())).thenReturn(sxpNode);
        PowerMockito.when(Configuration.register(any(SxpNode.class))).thenReturn(sxpNode);
        PowerMockito.when(Configuration.unRegister(anyString())).thenReturn(sxpNode);
        PowerMockito.when(Configuration.getConstants()).thenCallRealMethod();
    }

    private DataObjectModification<ConnectionTemplate> getObjectModification(
            DataObjectModification.ModificationType modificationType, ConnectionTemplate before,
            ConnectionTemplate after) {
        DataObjectModification<ConnectionTemplate> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(modificationType);
        when(modification.getDataAfter()).thenReturn(after);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataType()).thenReturn(ConnectionTemplate.class);
        return modification;
    }

    private DataObjectModification<ConnectionTemplates> getObjectModification(
            DataObjectModification<ConnectionTemplate> change) {
        DataObjectModification<ConnectionTemplates> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(modification.getDataType()).thenReturn(ConnectionTemplates.class);
        when(modification.getModifiedChildren()).thenReturn(Collections.singletonList(change));
        return modification;
    }

    private InstanceIdentifier<SxpDomain> getIdentifier() {
        return NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(new NodeId("0.0.0.0")))
                .augmentation(SxpNodeIdentity.class)
                .child(SxpDomains.class)
                .child(SxpDomain.class, new SxpDomainKey("GLOBAL"));
    }

    private ConnectionTemplate getConnectionTemplate(String prefix, int port, String pass, ConnectionMode mode,
            Version version) {
        ConnectionTemplateBuilder builder = new ConnectionTemplateBuilder();
        builder.setTemplateTcpPort(new PortNumber(port));
        builder.setTemplatePrefix(new IpPrefix(prefix.toCharArray()));
        builder.setTemplatePassword(pass);
        builder.setTemplateMode(mode);
        builder.setTemplateVersion(version);
        return builder.build();
    }

    @Test public void testHandleOperational_1() throws Exception {
        identityListener.handleOperational(getObjectModification(DataObjectModification.ModificationType.WRITE, null,
                getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4)),
                getIdentifier(), sxpNode);
        verify(domain).addConnectionTemplate(any(ConnectionTemplate.class));
    }

    @Test public void testHandleOperational_2() throws Exception {
        identityListener.handleOperational(getObjectModification(DataObjectModification.ModificationType.WRITE,
                getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4), null),
                getIdentifier(), sxpNode);
        verify(domain).removeConnectionTemplate(any(IpPrefix.class));
    }

    @Test public void testHandleOperational_3() throws Exception {
        identityListener.handleOperational(getObjectModification(DataObjectModification.ModificationType.DELETE,
                getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4), null),
                getIdentifier(), sxpNode);
        verify(domain).removeConnectionTemplate(any(IpPrefix.class));
    }

    @Test public void testHandleOperational_4() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Listener, Version.Version4)),
                getIdentifier(), sxpNode);
        verify(domain).addConnectionTemplate(any(ConnectionTemplate.class));
        verify(domain).removeConnectionTemplate(any(IpPrefix.class));

        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 56, "passs", ConnectionMode.Listener, Version.Version4)),
                getIdentifier(), sxpNode);
        verify(domain, times(2)).addConnectionTemplate(any(ConnectionTemplate.class));
        verify(domain, times(2)).removeConnectionTemplate(any(IpPrefix.class));

        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Speaker, Version.Version4)),
                getIdentifier(), sxpNode);
        verify(domain, times(3)).addConnectionTemplate(any(ConnectionTemplate.class));
        verify(domain, times(3)).removeConnectionTemplate(any(IpPrefix.class));

        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version2)),
                getIdentifier(), sxpNode);
        verify(domain, times(4)).addConnectionTemplate(any(ConnectionTemplate.class));
        verify(domain, times(4)).removeConnectionTemplate(any(IpPrefix.class));
    }

    @Test public void testGetIdentifier() throws Exception {
        assertNotNull(identityListener.getIdentifier(
                new ConnectionTemplateBuilder().setTemplateTcpPort(new PortNumber(64))
                        .setTemplatePrefix(new IpPrefix("1.1.1.1/32".toCharArray()))
                        .build(), getIdentifier()));

        assertTrue(identityListener.getIdentifier(new ConnectionTemplateBuilder().setTemplateTcpPort(new PortNumber(64))
                .setTemplatePrefix(new IpPrefix("1.1.1.1/32".toCharArray()))
                .build(), getIdentifier()).getTargetType().equals(ConnectionTemplate.class));
    }

    @Test public void testHandleChange() throws Exception {
        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE,
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4)))),
                LogicalDatastoreType.OPERATIONAL, getIdentifier());
        verify(datastoreAccess, never()).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
        verify(datastoreAccess, never()).mergeSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
        verify(datastoreAccess, never()).checkAndDelete(any(InstanceIdentifier.class),
                eq(LogicalDatastoreType.OPERATIONAL));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null,
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Listener, Version.Version4)))),
                LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(datastoreAccess).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE,
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Listener, Version.Version2)))),
                LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(datastoreAccess).mergeSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.DELETE,
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Speaker, Version.Version4)))),
                LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(datastoreAccess).checkAndDelete(any(InstanceIdentifier.class), eq(LogicalDatastoreType.OPERATIONAL));
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
