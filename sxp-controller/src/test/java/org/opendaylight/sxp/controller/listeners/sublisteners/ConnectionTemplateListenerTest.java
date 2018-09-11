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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sxp.controller.core.DatastoreAccess;
import org.opendaylight.sxp.controller.listeners.NodeIdentityListener;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
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
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ConnectionTemplateListenerTest {

    @Mock
    private SxpNode sxpNode;
    @Mock
    private org.opendaylight.sxp.core.SxpDomain domain;
    @Mock
    private SxpConnection connection;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;

    private ConnectionTemplateListener templateListener;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        DatastoreAccess datastoreAccess = prepareDataStore(dataBroker, readTransaction, writeTransaction);
        when(sxpNode.getDomain(anyString())).thenReturn(domain);
        when(sxpNode.getConnection(any(SocketAddress.class))).thenReturn(connection);
        templateListener = new ConnectionTemplateListener(datastoreAccess);
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
        when(modification.getModifiedChildren()).thenAnswer(invocation -> Collections.singletonList(change));
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
        builder.setTemplatePrefix(IpPrefixBuilder.getDefaultInstance(prefix));
        builder.setTemplatePassword(pass);
        builder.setTemplateMode(mode);
        builder.setTemplateVersion(version);
        return builder.build();
    }

    @Test
    public void testHandleOperational_1() throws Exception {
        templateListener.handleOperational(getObjectModification(DataObjectModification.ModificationType.WRITE, null,
                getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4)),
                getIdentifier(), sxpNode);
        verify(domain).addConnectionTemplate(any(ConnectionTemplate.class));
    }

    @Test
    public void testHandleOperational_2() throws Exception {
        templateListener.handleOperational(getObjectModification(DataObjectModification.ModificationType.WRITE,
                getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4), null),
                getIdentifier(), sxpNode);
        verify(domain).removeConnectionTemplate(any(IpPrefix.class));
    }

    @Test
    public void testHandleOperational_3() throws Exception {
        templateListener.handleOperational(getObjectModification(DataObjectModification.ModificationType.DELETE,
                getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4), null),
                getIdentifier(), sxpNode);
        verify(domain).removeConnectionTemplate(any(IpPrefix.class));
    }

    @Test
    public void testHandleOperational_4() throws Exception {
        templateListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Listener, Version.Version4)),
                getIdentifier(), sxpNode);
        verify(domain).addConnectionTemplate(any(ConnectionTemplate.class));
        verify(domain).removeConnectionTemplate(any(IpPrefix.class));

        templateListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 56, "passs", ConnectionMode.Listener, Version.Version4)),
                getIdentifier(), sxpNode);
        verify(domain, times(2)).addConnectionTemplate(any(ConnectionTemplate.class));
        verify(domain, times(2)).removeConnectionTemplate(any(IpPrefix.class));

        templateListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Speaker, Version.Version4)),
                getIdentifier(), sxpNode);
        verify(domain, times(3)).addConnectionTemplate(any(ConnectionTemplate.class));
        verify(domain, times(3)).removeConnectionTemplate(any(IpPrefix.class));

        templateListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED,
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version2)),
                getIdentifier(), sxpNode);
        verify(domain, times(4)).addConnectionTemplate(any(ConnectionTemplate.class));
        verify(domain, times(4)).removeConnectionTemplate(any(IpPrefix.class));
    }

    @Test
    public void testGetIdentifier() throws Exception {
        assertNotNull(templateListener.getIdentifier(
                new ConnectionTemplateBuilder().setTemplateTcpPort(new PortNumber(64))
                        .setTemplatePrefix(IpPrefixBuilder.getDefaultInstance("1.1.1.1/32"))
                        .build(), getIdentifier()));

        assertTrue(templateListener.getIdentifier(new ConnectionTemplateBuilder().setTemplateTcpPort(new PortNumber(64))
                .setTemplatePrefix(IpPrefixBuilder.getDefaultInstance("1.1.1.1/32"))
                .build(), getIdentifier()).getTargetType().equals(ConnectionTemplate.class));
    }

    @Test
    public void testHandleChange() throws Exception {
        templateListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE,
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 56, "pass", ConnectionMode.Listener, Version.Version4)))),
                LogicalDatastoreType.OPERATIONAL, getIdentifier());
        verify(writeTransaction, never())
                .put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
        verify(writeTransaction, never())
                .merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
        verify(writeTransaction, never()).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));

        templateListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null,
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Listener, Version.Version4)))),
                LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(writeTransaction).put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(DataObject.class));

        templateListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE,
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Listener, Version.Version2)))),
                LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(writeTransaction)
                .merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));

        templateListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.DELETE,
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Listener, Version.Version4),
                        getConnectionTemplate("1.1.1.1/32", 55, "pass", ConnectionMode.Speaker, Version.Version4)))),
                LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(writeTransaction).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }

    @Test
    public void testGetModifications() throws Exception {
        assertNotNull(templateListener.getObjectModifications(null));
        assertNotNull(templateListener.getObjectModifications(mock(DataObjectModification.class)));
        assertNotNull(templateListener.getModifications(null));
        DataTreeModification dtm = mock(DataTreeModification.class);
        when(dtm.getRootNode()).thenReturn(mock(DataObjectModification.class));
        assertNotNull(templateListener.getModifications(dtm));
    }

    /**
     * Prepare {@link DatastoreAccess} mock instance backed by {@link DataBroker} for tests.
     * <p>
     * {@link ReadTransaction} and {@link WriteTransaction} are assumed to be created by {@link TransactionChain}.
     * <p>
     * {@link ReadTransaction} reads {@link Optional#empty()} or a mock instance of {@link DataObject}
     * to satisfies {@link DatastoreAccess#checkAndPut(InstanceIdentifier, DataObject, LogicalDatastoreType, boolean)}
     * called with {@code false}.
     * <p>
     * {@link WriteTransaction} is committed successfully.
     *
     * @param dataBroker mock of {@link DataBroker}
     * @param readTransaction mock of {@link ReadTransaction}
     * @param writeTransaction mock of {@link WriteTransaction}
     * @return mock of {@link DatastoreAccess}
     */
    private static DatastoreAccess prepareDataStore(DataBroker dataBroker, ReadTransaction readTransaction,
            WriteTransaction writeTransaction) {
        TransactionChain transactionChain = mock(TransactionChain.class);
        doReturn(CommitInfo.emptyFluentFuture())
                .when(writeTransaction).commit();
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(mock(DataObject.class))))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.empty()))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(mock(DataObject.class))));
        when(transactionChain.newReadOnlyTransaction())
                .thenReturn(readTransaction);
        when(transactionChain.newWriteOnlyTransaction())
                .thenReturn(writeTransaction);
        when(dataBroker.createTransactionChain(any(TransactionChainListener.class)))
                .thenReturn(transactionChain);

        return DatastoreAccess.getInstance(dataBroker);
    }
}
