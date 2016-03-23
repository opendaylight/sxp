/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.util.io;

import com.google.common.util.concurrent.AbstractCheckedFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.config.yang.sxp.controller.conf.Binding;
import org.opendaylight.controller.config.yang.sxp.controller.conf.Connection;
import org.opendaylight.controller.config.yang.sxp.controller.conf.ConnectionTimers;
import org.opendaylight.controller.config.yang.sxp.controller.conf.Connections;
import org.opendaylight.controller.config.yang.sxp.controller.conf.MasterDatabase;
import org.opendaylight.controller.config.yang.sxp.controller.conf.SxpController;
import org.opendaylight.controller.config.yang.sxp.controller.conf.SxpNode;
import org.opendaylight.controller.config.yang.sxp.controller.conf.Timers;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sxp.controller.util.database.DatastoreValidator;
import org.opendaylight.sxp.controller.util.database.access.DatastoreAccess;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatastoreValidator.class, DatastoreAccess.class, org.opendaylight.sxp.core.SxpNode.class,
        org.opendaylight.sxp.util.inet.Search.class, Configuration.class}) public class ConfigLoaderTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static DatastoreValidator validator;
        private static DatastoreAccess access;

        @Before public void init() throws Exception {
                validator = PowerMockito.mock(DatastoreValidator.class);
                access = PowerMockito.mock(DatastoreAccess.class);
                PowerMockito.when(validator.getDatastoreAccess()).thenReturn(access);
                PowerMockito.when(access.put(any(InstanceIdentifier.class), any(SxpNodeIdentity.class),
                        any(LogicalDatastoreType.class))).thenReturn(mock(AbstractCheckedFuture.class));
                PowerMockito.mockStatic(org.opendaylight.sxp.core.SxpNode.class, RETURNS_MOCKS);
                PowerMockito.mockStatic(org.opendaylight.sxp.util.inet.Search.class);
        }

        @Test public void testCreate() throws Exception {
                ConfigLoader.create(validator);
                exception.expect(NullPointerException.class);
                ConfigLoader.create(null);
        }

        private Connection getConnection(Version version, ConnectionMode mode) {
                Connection connection = new Connection();
                connection.setVersion(version);
                connection.setMode(mode);
                connection.setPeerAddress(new IpAddress("1.1.1.1".toCharArray()));
                connection.setTcpPort(PortNumber.getDefaultInstance("60000"));
                connection.setConnectionTimers(new ConnectionTimers());
                return connection;
        }

        private Binding getBinding(Integer sgt, String... strings) {
                Binding binding = new Binding();
                if (sgt != null) {
                        binding.setSgt(new Sgt(sgt));
                }
                if (strings != null) {
                        List<IpPrefix> ipPrefixes = new ArrayList<>();
                        for (String s : strings) {
                                if (s.contains(":")) {
                                        ipPrefixes.add(new IpPrefix(Ipv6Prefix.getDefaultInstance(s)));
                                } else {
                                        ipPrefixes.add(new IpPrefix(Ipv4Prefix.getDefaultInstance(s)));
                                }
                        }
                        binding.setIpPrefix(ipPrefixes);
                }
                return binding;
        }

        @Test public void testLoad() throws Exception {
                ConfigLoader configLoader = ConfigLoader.create(validator);

                SxpController controller = new SxpController();
                List<SxpNode> sxpNodes = new ArrayList<>();
                SxpNode node = new SxpNode();

                node.setVersion(Version.Version4);
                node.setTcpPort(PortNumber.getDefaultInstance("64999"));
                node.setNodeId(NodeId.getDefaultInstance("0.0.0.0"));
                node.setEnabled(true);

                node.setTimers(new Timers());
                Connections connections = new Connections();

                List<Connection> connectionsList = new ArrayList<>();
                connectionsList.add(getConnection(Version.Version3, ConnectionMode.Both));
                connections.setConnection(connectionsList);

                node.setConnections(connections);
                node.setMappingExpanded(10);
                node.setSourceIp(new IpAddress("0.0.0.0".toCharArray()));

                MasterDatabase masterDatabase = new MasterDatabase();
                List<Binding> bindings = new ArrayList<>();

                bindings.add(getBinding(10, "1.1.1.1/32"));
                bindings.add(getBinding(20, "2000:0:0:0:0:0:0:1/128"));
                bindings.add(getBinding(200, null));
                bindings.add(getBinding(null, "2000:0:0:0:0:0:0:2/128"));
                masterDatabase.setBinding(bindings);

                node.setMasterDatabase(masterDatabase);

                sxpNodes.add(node);
                controller.setSxpNode(sxpNodes);

                configLoader.load(controller);

                ArgumentCaptor<SxpNodeIdentity> argumentCaptor = ArgumentCaptor.forClass(SxpNodeIdentity.class);

                verify(access).merge(any(InstanceIdentifier.class), argumentCaptor.capture(),
                        any(LogicalDatastoreType.class));
                verify(validator, times(2)).validateSxpNodePath(anyString(), any(LogicalDatastoreType.class));

                SxpNodeIdentity sxpNode = argumentCaptor.getValue();
                assertEquals(64999, (long) sxpNode.getTcpPort().getValue());
                assertEquals(10, (long) sxpNode.getMappingExpanded());
                //Capabilities are config False
                assertEquals(null, sxpNode.getCapabilities());
                org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection
                        connection =
                        sxpNode.getConnections().getConnection().get(0);
                assertEquals(Version.Version3, connection.getVersion());
                assertEquals(ConnectionMode.Both, connection.getMode());

                //Master-DB is config False
                assertEquals(null, sxpNode.getMasterDatabase());

                node.setNodeId(null);
                configLoader.load(controller);
                verify(access).merge(any(InstanceIdentifier.class), argumentCaptor.capture(),
                        any(LogicalDatastoreType.class));
        }
}
