/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.sublisteners;

import java.util.ArrayList;
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
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.acl.filter.entries.AclEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.fields.DomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.domain.filter.fields.domains.DomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.DomainFilters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.domain.fields.domain.filters.DomainFilterBuilder;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({Configuration.class, DatastoreAccess.class})
public class DomainFilterListenerTest {

    private DomainFilterListener identityListener;
    private DatastoreAccess datastoreAccess;
    private SxpNode sxpNode;

    @Before public void setUp() throws Exception {
        datastoreAccess = PowerMockito.mock(DatastoreAccess.class);
        identityListener = new DomainFilterListener(datastoreAccess);
        sxpNode = mock(SxpNode.class);
        when(sxpNode.addFilterToDomain(anyString(), any(DomainFilter.class))).thenReturn(true);
        PowerMockito.mockStatic(Configuration.class);
        PowerMockito.when(Configuration.getRegisteredNode(anyString())).thenReturn(sxpNode);
        PowerMockito.when(Configuration.register(any(SxpNode.class))).thenReturn(sxpNode);
        PowerMockito.when(Configuration.unregister(anyString())).thenReturn(sxpNode);
        PowerMockito.when(Configuration.getConstants()).thenCallRealMethod();
    }

    private DataObjectModification<DomainFilter> getObjectModification(
            DataObjectModification.ModificationType modificationType, DomainFilter before, DomainFilter after) {
        DataObjectModification<DomainFilter> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(modificationType);
        when(modification.getDataAfter()).thenReturn(after);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataType()).thenReturn(DomainFilter.class);
        return modification;
    }

    private DataObjectModification<DomainFilters> getObjectModification(DataObjectModification<DomainFilter> change) {
        DataObjectModification<DomainFilters> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(modification.getDataType()).thenReturn(DomainFilters.class);
        when(modification.getModifiedChildren()).thenReturn(Collections.singletonList(change));
        return modification;
    }

    private InstanceIdentifier<SxpDomain> getIdentifier() {
        return NodeIdentityListener.SUBSCRIBED_PATH.child(Node.class, new NodeKey(new NodeId("0.0.0.0")))
                .augmentation(SxpNodeIdentity.class)
                .child(SxpDomains.class)
                .child(SxpDomain.class, new SxpDomainKey("global"));
    }

    private DomainFilter getDomainFilter(int entries, int domains) {
        DomainFilterBuilder builder = new DomainFilterBuilder();
        builder.setFilterName("basic");
        builder.setFilterSpecific(FilterSpecific.AccessOrPrefixList);
        builder.setFilterEntries(new AclFilterEntriesBuilder().setAclEntry(new ArrayList<>()).build());
        builder.setDomains(new DomainsBuilder().setDomain(new ArrayList<>()).build());
        for (int i = 0; i < entries; i++) {
            ((AclFilterEntries) builder.getFilterEntries()).getAclEntry().add(mock(AclEntry.class));
        }
        for (int i = 0; i < domains; i++) {
            builder.getDomains().getDomain().add(new DomainBuilder().setName(Integer.toString(i)).build());
        }
        return builder.build();
    }

    @Test public void testHandleOperational_1() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, getDomainFilter(5, 2)),
                getIdentifier());
        verify(sxpNode).addFilterToDomain(anyString(), any(DomainFilter.class));
    }

    @Test public void testHandleOperational_2() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getDomainFilter(5, 2), null),
                getIdentifier());
        verify(sxpNode).removeFilterFromDomain(anyString(), any(FilterSpecific.class), anyString());
    }

    @Test public void testHandleOperational_3() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED, getDomainFilter(5, 2),
                        getDomainFilter(8, 2)), getIdentifier());
        verify(sxpNode).updateDomainFilter(anyString(), any(DomainFilter.class));
    }

    @Test public void testHandleOperational_4() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.SUBTREE_MODIFIED, getDomainFilter(5, 2),
                        getDomainFilter(5, 8)), getIdentifier());
        verify(sxpNode).updateDomainFilter(anyString(), any(DomainFilter.class));
    }

    @Test public void testHandleOperational_5() throws Exception {
        identityListener.handleOperational(
                getObjectModification(DataObjectModification.ModificationType.DELETE, getDomainFilter(5, 2), null),
                getIdentifier());
        verify(sxpNode).removeFilterFromDomain(anyString(), any(FilterSpecific.class), anyString());
    }

    @Test public void testGetModifications() throws Exception {
        assertNotNull(identityListener.getIdentifier(new DomainFilterBuilder().setFilterName("basic")
                .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                .build(), getIdentifier()));
        assertTrue(identityListener.getIdentifier(new DomainFilterBuilder().setFilterName("extended")
                .setFilterSpecific(FilterSpecific.AccessOrPrefixList)
                .build(), getIdentifier()).getTargetType().equals(DomainFilter.class));

        assertNotNull(identityListener.getObjectModifications(null));
        assertNotNull(identityListener.getObjectModifications(mock(DataObjectModification.class)));
        assertNotNull(identityListener.getModifications(null));
        assertNotNull(identityListener.getModifications(mock(DataTreeModification.class)));
    }

    @Test public void testHandleChange() throws Exception {
        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getDomainFilter(5, 2),
                        getDomainFilter(5, 3)))), LogicalDatastoreType.OPERATIONAL, getIdentifier());
        verify(datastoreAccess, never()).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
        verify(datastoreAccess, never()).mergeSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));
        verify(datastoreAccess, never()).checkAndDelete(any(InstanceIdentifier.class),
                eq(LogicalDatastoreType.OPERATIONAL));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, null, getDomainFilter(5, 2)))),
                LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(datastoreAccess).putSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.WRITE, getDomainFilter(5, 2),
                        getDomainFilter(5, 4)))), LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(datastoreAccess).mergeSynchronous(any(InstanceIdentifier.class), any(DataObject.class),
                eq(LogicalDatastoreType.OPERATIONAL));

        identityListener.handleChange(Collections.singletonList(getObjectModification(
                getObjectModification(DataObjectModification.ModificationType.DELETE, getDomainFilter(5, 2),
                        getDomainFilter(5, 4)))), LogicalDatastoreType.CONFIGURATION, getIdentifier());
        verify(datastoreAccess).checkAndDelete(any(InstanceIdentifier.class), eq(LogicalDatastoreType.OPERATIONAL));
    }
}
