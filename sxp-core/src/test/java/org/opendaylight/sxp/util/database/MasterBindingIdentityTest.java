/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseBindingSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MasterBindingIdentityTest {

        private static boolean isChanged;

        @Before public void init() {
                isChanged = false;
        }

        private Binding getBinding(String prefix) {
                BindingBuilder bindingBuilder = new BindingBuilder();
                bindingBuilder.setIpPrefix(new IpPrefix(prefix.toCharArray()));
                bindingBuilder.setTimestamp(DateAndTime.getDefaultInstance("2015-06-30T12:00:00Z"));
                bindingBuilder.setChanged(isChanged);
                return bindingBuilder.build();
        }

        private PrefixGroup getPrefixGroup(int sgt, String... prefixes) {
                PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder();
                List<Binding> bindings = new ArrayList<>();
                for (String s : prefixes) {
                        bindings.add(getBinding(s));
                }
                prefixGroupBuilder.setBinding(bindings);
                prefixGroupBuilder.setSgt(new Sgt(sgt));
                return prefixGroupBuilder.build();
        }

        private Source getSource(PrefixGroup... prefixGroups_) {
                SourceBuilder builder = new SourceBuilder();
                List<PrefixGroup> prefixGroups = new ArrayList<>();
                for (PrefixGroup group : prefixGroups_) {
                        prefixGroups.add(group);
                }
                builder.setPrefixGroup(prefixGroups);
                builder.setBindingSource(DatabaseBindingSource.Local);
                return builder.build();
        }

        private void assertMasterBindingIdentities(List<MasterBindingIdentity> masterBindingIdentities,
                boolean onlyChanged, PrefixGroup... prefixGroups) {
                for (MasterBindingIdentity identity : masterBindingIdentities) {
                        boolean hasBinding = false;
                        for (PrefixGroup prefixGroup : prefixGroups) {
                                for (Binding binding : prefixGroup.getBinding()) {
                                        if (binding.getIpPrefix().equals(identity.getBinding().getIpPrefix())) {
                                                hasBinding = true;
                                                assertEquals(identity.getPrefixGroup().getSgt().getValue(),
                                                        prefixGroup.getSgt().getValue());
                                        }
                                }
                        }
                        assertTrue(hasBinding);
                        if (onlyChanged) {
                                assertEquals(onlyChanged, identity.getBinding().isChanged());
                        }
                }
        }

        @Test public void testCreate() throws Exception {
                MasterDatabase database = mock(MasterDatabase.class);
                List<Source> sourceList = new ArrayList<>();
                when(database.getSource()).thenReturn(sourceList);
                sourceList.add(getSource(getPrefixGroup(10, "127.0.0.0/32"), getPrefixGroup(100, "127.0.0.1/24")));
                isChanged = true;
                sourceList.add(getSource(getPrefixGroup(20, "2001:0:0:0:0:0:0:1/128"),
                        getPrefixGroup(200, "2001:0:0:0:0:0:0:0/32")));
                List<MasterBindingIdentity> masterBindingIdentities = MasterBindingIdentity.create(null, true);
                assertNotNull(masterBindingIdentities);
                masterBindingIdentities = MasterBindingIdentity.create(database, true);
                assertMasterBindingIdentities(masterBindingIdentities, true,
                        getPrefixGroup(20, "2001:0:0:0:0:0:0:1/128"), getPrefixGroup(200, "2001:0:0:0:0:0:0:0/32"));

                masterBindingIdentities = MasterBindingIdentity.create(database, false);
                assertMasterBindingIdentities(masterBindingIdentities, false,
                        getPrefixGroup(20, "2001:0:0:0:0:0:0:1/128"), getPrefixGroup(200, "2001:0:0:0:0:0:0:0/32"),
                        getPrefixGroup(10, "127.0.0.0/32"), getPrefixGroup(100, "127.0.0.1/24"));
        }

        @Test public void testEquals() throws Exception {
                MasterBindingIdentity
                        identity =
                        MasterBindingIdentity.create(getBinding("127.0.0.1/32"), getPrefixGroup(10, "127.0.0.1/32"),
                                getSource(getPrefixGroup(10, "127.0.0.1/32")));
                MasterBindingIdentity
                        identity2 =
                        MasterBindingIdentity.create(getBinding("127.0.0.2/32"), getPrefixGroup(20, "127.0.0.2/32"),
                                getSource(getPrefixGroup(20, "127.0.0.2/32")));

                assertTrue(identity.equals(identity));
                assertFalse(identity.equals(identity2));
                assertFalse(identity2.equals(identity));
                assertFalse(identity.equals(null));

        }

        @Test public void testToString() throws Exception {
                List<MasterBindingIdentity> identities = new ArrayList<>();
                identities.add(
                        MasterBindingIdentity.create(getBinding("127.0.0.1/32"), getPrefixGroup(10, "127.0.0.1/32"),
                                getSource(getPrefixGroup(10, "127.0.0.1/32"))));

                assertEquals("Local 10 127.0.0.1/32", identities.get(0).toString());
                identities.add(
                        MasterBindingIdentity.create(getBinding("127.0.0.2/32"), getPrefixGroup(20, "127.0.0.2/32"),
                                getSource(getPrefixGroup(20, "127.0.0.2/32"))));
                assertEquals("Local 10 127.0.0.1/32\n" + "Local 20 127.0.0.2/32\n",
                        MasterBindingIdentity.toString(identities));
        }
}
