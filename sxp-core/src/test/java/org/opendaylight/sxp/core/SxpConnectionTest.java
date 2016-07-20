/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.core.service.UpdateExportTask;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.database.spi.SxpDatabaseInf;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.exception.connection.SocketAddressNotRecognizedException;
import org.opendaylight.sxp.util.exception.message.ErrorMessageException;
import org.opendaylight.sxp.util.exception.unknown.UnknownTimerTypeException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterSpecific;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sxp.filter.SxpFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.TimerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.capabilities.fields.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.HoldTimeAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.SxpNodeIdAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.hold.time.attribute.HoldTimeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.attributes.fields.attribute.attribute.optional.fields.sxp.node.id.attribute.SxpNodeIdAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.sxp.messages.OpenMessage;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class) @PrepareForTest({SxpNode.class, UpdateExportTask.class, BindingDispatcher.class})
public class SxpConnectionTest {

        @Rule public ExpectedException exception = ExpectedException.none();

        private static String DOMAIN_NAME = "global";
        private static int ip4Address = 0;
        private static SxpNode sxpNode;
        private static SxpConnection sxpConnection;
        private static SxpDatabaseInf sxpDatabase;
        private static ThreadsWorker worker;

        @Before public void init() throws Exception {
                worker = mock(ThreadsWorker.class);
                when(worker.scheduleTask(any(Callable.class), anyInt(), any(TimeUnit.class))).thenReturn(
                        mock(ListenableScheduledFuture.class));
                when(worker.executeTask(any(Runnable.class), any(ThreadsWorker.WorkerType.class))).thenReturn(
                        mock(ListenableFuture.class));
                when(worker.executeTask(any(Callable.class), any(ThreadsWorker.WorkerType.class))).thenReturn(
                        mock(ListenableFuture.class));
                sxpNode = PowerMockito.mock(SxpNode.class);
                sxpDatabase = mock(SxpDatabaseInf.class);
                SxpDomain domain = mock(SxpDomain.class);
                PowerMockito.when(sxpNode.getDomain(anyString())).thenReturn(domain);
                PowerMockito.when(sxpNode.getBindingSxpDatabase(anyString())).thenReturn(sxpDatabase);
                when(domain.getSxpDatabase()).thenReturn(sxpDatabase);
                PowerMockito.when(sxpNode.getBindingMasterDatabase(anyString())).thenReturn(mock(MasterDatabaseInf.class));
                when(domain.getMasterDatabase()).thenReturn(mock(MasterDatabaseInf.class));
                PowerMockito.when(sxpNode.getAllOnSpeakerConnections(anyString())).thenReturn(new ArrayList<>());
                PowerMockito.when(sxpNode.getSvcBindingDispatcher()).thenReturn(mock(BindingDispatcher.class));
                PowerMockito.when(sxpNode.getHoldTimeMax()).thenReturn(120);
                PowerMockito.when(sxpNode.getHoldTimeMin()).thenReturn(60);
                PowerMockito.when(sxpNode.getHoldTimeMinAcceptable()).thenReturn(60);
                PowerMockito.when(sxpNode.getWorker()).thenReturn(worker);
                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.None, ConnectionState.On),
                                DOMAIN_NAME);
        }

        private Connection mockConnection(ConnectionMode mode, ConnectionState state) {
                Connection connection = mock(Connection.class);
                when(connection.getMode()).thenReturn(mode);
                when(connection.getPeerAddress()).thenReturn(new IpAddress(("127.0.0." + (++ip4Address)).toCharArray()));
                when(connection.getState()).thenReturn(state);
                when(connection.getVersion()).thenReturn(Version.Version4);
                ConnectionTimers timers = mock(ConnectionTimers.class);
                when(timers.getDeleteHoldDownTime()).thenReturn(120);
                when(timers.getReconciliationTime()).thenReturn(60);
                when(timers.getHoldTimeMinAcceptable()).thenReturn(10);
                when(connection.getTcpPort()).thenReturn(new PortNumber(65001));
                when(connection.getConnectionTimers()).thenReturn(timers);
                return connection;
        }

        @Test public void testCleanUpBindings() throws Exception {
                sxpConnection.cleanUpBindings();
                verify(sxpNode.getBindingSxpDatabase(anyString())).reconcileBindings(any(NodeId.class));
        }

        @Test public void testCloseChannelHandlerContext() throws Exception {
                ChannelHandlerContext context = mock(ChannelHandlerContext.class);
                when(context.close()).thenReturn(mock(ChannelFuture.class));
                sxpConnection.addChannelHandlerContext(context);
                sxpConnection.closeChannelHandlerContext(context);
                verify(context).close();

                sxpConnection.markChannelHandlerContext(context,
                        SxpConnection.ChannelHandlerContextType.ListenerContext);
                sxpConnection.closeChannelHandlerContext(context);
                exception.expect(ChannelHandlerContextNotFoundException.class);
                sxpConnection.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.ListenerContext);
        }

        @Test public void testCloseChannelHandlerContextComplements() throws Exception {
                ChannelHandlerContext context = mock(ChannelHandlerContext.class);
                when(context.close()).thenReturn(mock(ChannelFuture.class));
                ChannelHandlerContext context1 = mock(ChannelHandlerContext.class);
                when(context1.close()).thenReturn(mock(ChannelFuture.class));
                ChannelHandlerContext context2 = mock(ChannelHandlerContext.class);
                when(context2.close()).thenReturn(mock(ChannelFuture.class));
                sxpConnection.addChannelHandlerContext(context);
                sxpConnection.addChannelHandlerContext(context1);
                sxpConnection.closeChannelHandlerContextComplements(context2);
                verify(context).close();
                verify(context1).close();
                verify(context2, never()).close();
        }

        @Test public void testCloseChannelHandlerContexts() throws Exception {
                ChannelHandlerContext context = mock(ChannelHandlerContext.class);
                when(context.close()).thenReturn(mock(ChannelFuture.class));
                sxpConnection.addChannelHandlerContext(context);
                sxpConnection.closeChannelHandlerContexts();
                verify(context).close();

                sxpConnection.markChannelHandlerContext(context,
                        SxpConnection.ChannelHandlerContextType.ListenerContext);
                sxpConnection.closeChannelHandlerContexts();
                exception.expect(ChannelHandlerContextNotFoundException.class);
                sxpConnection.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.ListenerContext);
        }

        @Test public void testPurgeBindings() throws Exception {
                ArgumentCaptor<Runnable> argument = ArgumentCaptor.forClass(Runnable.class);
                sxpConnection.purgeBindings();
                verify(sxpNode.getWorker()).addListener(any(ListenableFuture.class), argument.capture());
                verify(sxpNode.getWorker()).executeTaskInSequence(any(Callable.class),
                        eq(ThreadsWorker.WorkerType.INBOUND), eq(sxpConnection));
                argument.getValue().run();
                assertEquals(ConnectionState.Off,sxpConnection.getState());
        }

        @Test public void testSetDeleteHoldDownTimer() throws Exception {
                ListenableScheduledFuture future = mock(ListenableScheduledFuture.class);
                when(future.isDone()).thenReturn(false);
                when(worker.scheduleTask(any(Callable.class), anyInt(), any(TimeUnit.class))).thenReturn(future);

                sxpConnection.setTimer(TimerType.ReconciliationTimer, 60);
                sxpConnection.setDeleteHoldDownTimer();
                assertNull(sxpConnection.getTimer(TimerType.ReconciliationTimer));
                assertNotNull(sxpConnection.getTimer(TimerType.DeleteHoldDownTimer));
                assertEquals(ConnectionState.DeleteHoldDown, sxpConnection.getState());
        }

        @Test public void testSetReconciliationTimer() throws Exception {
                ListenableScheduledFuture future = mock(ListenableScheduledFuture.class);
                when(future.isDone()).thenReturn(true);
                when(worker.scheduleTask(any(Callable.class), anyInt(), any(TimeUnit.class))).thenReturn(future);

                sxpConnection.setReconciliationTimer();
                assertNotNull(sxpConnection.getTimer(TimerType.ReconciliationTimer));
                sxpConnection.setTimer(TimerType.DeleteHoldDownTimer, 120);
                sxpConnection.setReconciliationTimer();
                assertNotNull(sxpConnection.getTimer(TimerType.ReconciliationTimer));
                when(future.isDone()).thenReturn(false);
                sxpConnection.setReconciliationTimer();
                assertNotNull(sxpConnection.getTimer(TimerType.ReconciliationTimer));
        }

        @Test public void testSetStateOff() throws Exception {
                ChannelHandlerContext context = mock(ChannelHandlerContext.class);
                when(context.close()).thenReturn(mock(ChannelFuture.class));
                ChannelHandlerContext context1 = mock(ChannelHandlerContext.class);
                when(context1.close()).thenReturn(mock(ChannelFuture.class));
                sxpConnection = spy(sxpConnection);
                when(sxpConnection.getContextType(any(ChannelHandlerContext.class))).thenReturn(
                        SxpConnection.ChannelHandlerContextType.ListenerContext);
                //ListenerDown
                sxpConnection.setTimer(TimerType.DeleteHoldDownTimer, 120);
                sxpConnection.setTimer(TimerType.KeepAliveTimer, 120);
                sxpConnection.setTimer(TimerType.ReconciliationTimer, 120);
                sxpConnection.setTimer(TimerType.HoldTimer, 120);

                sxpConnection.markChannelHandlerContext(context,
                        SxpConnection.ChannelHandlerContextType.ListenerContext);
                sxpConnection.markChannelHandlerContext(context1,
                        SxpConnection.ChannelHandlerContextType.SpeakerContext);

                sxpConnection.setStateOff(context);
                assertNull(sxpConnection.getTimer(TimerType.DeleteHoldDownTimer));
                assertNull(sxpConnection.getTimer(TimerType.ReconciliationTimer));
                assertNull(sxpConnection.getTimer(TimerType.HoldTimer));
                assertNotNull(sxpConnection.getTimer(TimerType.KeepAliveTimer));

                //Speaker Down
                when(sxpConnection.getContextType(any(ChannelHandlerContext.class))).thenReturn(
                        SxpConnection.ChannelHandlerContextType.SpeakerContext);
                sxpConnection.setTimer(TimerType.DeleteHoldDownTimer, 120);
                sxpConnection.setTimer(TimerType.HoldTimer, 120);
                sxpConnection.setTimer(TimerType.ReconciliationTimer, 120);
                sxpConnection.markChannelHandlerContext(context,
                        SxpConnection.ChannelHandlerContextType.ListenerContext);

                sxpConnection.setStateOff(context1);
                assertNotNull(sxpConnection.getTimer(TimerType.DeleteHoldDownTimer));
                assertNotNull(sxpConnection.getTimer(TimerType.ReconciliationTimer));
                assertNotNull(sxpConnection.getTimer(TimerType.HoldTimer));
                assertNull(sxpConnection.getTimer(TimerType.KeepAliveTimer));
        }

        @Test public void testSetTimer() throws Exception {
                ListenableScheduledFuture future = mock(ListenableScheduledFuture.class);
                when(future.isDone()).thenReturn(false);
                when(worker.scheduleTask(any(Callable.class), anyInt(), any(TimeUnit.class))).thenReturn(future);

                sxpConnection.setTimer(TimerType.DeleteHoldDownTimer, 0);
                assertNull(sxpConnection.getTimer(TimerType.DeleteHoldDownTimer));

                sxpConnection.setTimer(TimerType.KeepAliveTimer, 50);
                assertNotNull(sxpConnection.getTimer(TimerType.KeepAliveTimer));

                sxpConnection.setTimer(TimerType.ReconciliationTimer, 50);
                assertNotNull(sxpConnection.getTimer(TimerType.ReconciliationTimer));

                exception.expect(UnknownTimerTypeException.class);
                sxpConnection.setTimer(TimerType.RetryOpenTimer, 50);
        }

        @Test public void testShutdown() throws Exception {
                Connection connection = mockConnection(ConnectionMode.Listener, ConnectionState.On);
                Connection connection1 = mockConnection(ConnectionMode.Speaker, ConnectionState.On);

                when(worker.executeTaskInSequence(any(Callable.class), any(ThreadsWorker.WorkerType.class),
                        any(SxpConnection.class))).thenReturn(mock(ListenableFuture.class))
                        .thenReturn(mock(ListenableFuture.class));

                sxpConnection = SxpConnection.create(sxpNode, connection, DOMAIN_NAME);
                sxpConnection.shutdown();
                verify(sxpDatabase).deleteBindings(sxpConnection.getNodeIdRemote());
                assertEquals(ConnectionState.Off, sxpConnection.getState());

                sxpConnection = SxpConnection.create(sxpNode, connection1, DOMAIN_NAME);
                sxpConnection.setTimer(TimerType.KeepAliveTimer, 50);

                sxpConnection.shutdown();
                assertNull(sxpConnection.getTimer(TimerType.KeepAliveTimer));
                try {
                        sxpConnection.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.SpeakerContext);
                        fail();
                } catch (ChannelHandlerContextNotFoundException e) {
                        assertEquals(ConnectionState.Off, sxpConnection.getState());
                }
        }

        @Test public void testSetConnection() throws Exception {
                sxpConnection =
                        spy(SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Listener, ConnectionState.On),
                                DOMAIN_NAME));

                OpenMessage message = mock(OpenMessage.class);
                when(message.getVersion()).thenReturn(Version.Version4);
                when(message.getSxpMode()).thenReturn(ConnectionMode.Speaker);
                List<Attribute> attributes = new ArrayList<>();
                attributes.add(getNodeId("1.1.1.1"));
                when(message.getAttribute()).thenReturn(attributes);
                sxpConnection.setConnection(message);
                verify(sxpConnection).setConnectionListenerPart(any(OpenMessage.class));
                assertEquals(ConnectionState.On, sxpConnection.getState());

                sxpConnection =
                        spy(SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Speaker, ConnectionState.On),
                                DOMAIN_NAME));

                attributes.clear();
                when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
                sxpConnection.setConnection(message);
                verify(sxpConnection).setConnectionSpeakerPart(any(OpenMessage.class));
                assertEquals(ConnectionState.On, sxpConnection.getState());
        }

        private Attribute getNodeId(String id) {
                AttributeBuilder attributeBuilder = new AttributeBuilder();
                attributeBuilder.setType(AttributeType.SxpNodeId);

                SxpNodeIdAttributeBuilder _attributeBuilder = new SxpNodeIdAttributeBuilder();
                SxpNodeIdAttributesBuilder _attributesBuilder = new SxpNodeIdAttributesBuilder();
                _attributesBuilder.setNodeId(new NodeId(id));
                _attributeBuilder.setSxpNodeIdAttributes(_attributesBuilder.build());
                attributeBuilder.setAttributeOptionalFields(_attributeBuilder.build());
                return attributeBuilder.build();
        }

        private Attribute getHoldTime(int min, int max) {
                AttributeBuilder attributeBuilder = new AttributeBuilder();
                attributeBuilder.setType(AttributeType.HoldTime);

                HoldTimeAttributeBuilder _attributeBuilder = new HoldTimeAttributeBuilder();
                HoldTimeAttributesBuilder _attributesBuilder = new HoldTimeAttributesBuilder();
                _attributesBuilder.setHoldTimeMinValue(min);
                _attributesBuilder.setHoldTimeMaxValue(max);
                _attributeBuilder.setHoldTimeAttributes(_attributesBuilder.build());
                attributeBuilder.setAttributeOptionalFields(_attributeBuilder.build());
                return attributeBuilder.build();
        }

        @Test public void testSetConnectionListenerPartOpen() throws Exception {
                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Listener, ConnectionState.On),
                                DOMAIN_NAME);
                OpenMessage message = mock(OpenMessage.class);
                when(message.getSxpMode()).thenReturn(ConnectionMode.Speaker);
                when(message.getVersion()).thenReturn(Version.Version3);
                when(message.getType()).thenReturn(MessageType.Open);
                List<Attribute> attributes = new ArrayList<>();
                when(message.getAttribute()).thenReturn(attributes);

                attributes.add(getHoldTime(75, 130));

                exception.expect(ErrorMessageException.class);
                sxpConnection.setConnectionListenerPart(message);
        }

        @Test public void testSetConnectionListenerPartResp() throws Exception {
                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Listener, ConnectionState.On),
                                DOMAIN_NAME);
                OpenMessage message = mock(OpenMessage.class);
                when(message.getSxpMode()).thenReturn(ConnectionMode.Speaker);
                when(message.getVersion()).thenReturn(Version.Version3);
                when(message.getType()).thenReturn(MessageType.OpenResp);
                List<Attribute> attributes = new ArrayList<>();
                when(message.getAttribute()).thenReturn(attributes);

                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Listener, ConnectionState.On),
                                DOMAIN_NAME);
                attributes.clear();
                attributes.add(getHoldTime(80, 180));

                exception.expect(ErrorMessageException.class);
                sxpConnection.setConnectionListenerPart(message);
        }

        @Test public void testSetConnectionSpeakerPartOpen() throws Exception {
                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Speaker, ConnectionState.On),
                                DOMAIN_NAME);
                OpenMessage message = mock(OpenMessage.class);
                when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
                when(message.getVersion()).thenReturn(Version.Version3);
                when(message.getType()).thenReturn(MessageType.Open);
                List<Attribute> attributes = new ArrayList<>();
                when(message.getAttribute()).thenReturn(attributes);
                attributes.add(getHoldTime(75, 100));

                sxpConnection.setConnectionSpeakerPart(message);
                assertEquals(ConnectionMode.Listener, sxpConnection.getModeRemote());
                assertEquals(25, sxpConnection.getKeepaliveTime());
                assertEquals(75, sxpConnection.getHoldTimeMinAcceptable());
                assertNotNull(sxpConnection.getTimer(TimerType.KeepAliveTimer));

                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Speaker, ConnectionState.On),
                                DOMAIN_NAME);
                attributes.clear();

                sxpConnection.setConnectionSpeakerPart(message);
                assertNull(sxpConnection.getNodeIdRemote());
                assertNull(sxpConnection.getTimer(TimerType.KeepAliveTimer));
                assertEquals(0, sxpConnection.getKeepaliveTime());

                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Speaker, ConnectionState.On),
                                DOMAIN_NAME);
                attributes.clear();
                attributes.add(getNodeId("1.1.1.1"));

                exception.expect(ErrorMessageException.class);
                sxpConnection.setConnectionSpeakerPart(message);
        }

        @Test public void testSetConnectionSpeakerPartResp() throws Exception {
                OpenMessage message = mock(OpenMessage.class);
                when(message.getSxpMode()).thenReturn(ConnectionMode.Listener);
                when(message.getVersion()).thenReturn(Version.Version3);
                when(message.getType()).thenReturn(MessageType.OpenResp);
                List<Attribute> attributes = new ArrayList<>();
                when(message.getAttribute()).thenReturn(attributes);

                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Speaker, ConnectionState.On),
                                DOMAIN_NAME);
                attributes.clear();
                attributes.add(getHoldTime(80, 100));

                sxpConnection.setConnectionSpeakerPart(message);
                assertEquals(ConnectionMode.Listener, sxpConnection.getModeRemote());
                assertEquals((int) (80 / 3.0), sxpConnection.getKeepaliveTime());
                assertNotNull(sxpConnection.getTimer(TimerType.KeepAliveTimer));

                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Speaker, ConnectionState.On),
                                DOMAIN_NAME);
                attributes.clear();
                attributes.add(getHoldTime(25, 100));

                sxpConnection.setConnectionSpeakerPart(message);
                assertNull(sxpConnection.getNodeIdRemote());
                assertNotNull(sxpConnection.getTimer(TimerType.KeepAliveTimer));
                assertEquals(8, sxpConnection.getKeepaliveTime());
        }

        @Test public void testIsStateOn() throws Exception {
                assertTrue(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.ListenerContext));
                assertTrue(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.SpeakerContext));

                sxpConnection.setStateOff();
                assertFalse(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.ListenerContext));
                assertFalse(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.SpeakerContext));

                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Both, ConnectionState.On),
                                DOMAIN_NAME);

                ChannelHandlerContext context = mock(ChannelHandlerContext.class);
                when(context.isRemoved()).thenReturn(false);
                sxpConnection.markChannelHandlerContext(context,
                        SxpConnection.ChannelHandlerContextType.ListenerContext);
                assertTrue(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.ListenerContext));

                context = mock(ChannelHandlerContext.class);
                when(context.isRemoved()).thenReturn(true);
                sxpConnection.markChannelHandlerContext(context, SxpConnection.ChannelHandlerContextType.SpeakerContext);
                assertFalse(sxpConnection.isStateOn(SxpConnection.ChannelHandlerContextType.SpeakerContext));
        }

        @Test public void testSetInetSocketAddresses() throws Exception {
                SocketAddress socketAddress = new InetSocketAddress("0.0.0.0", 50);
                sxpConnection.setInetSocketAddresses(socketAddress);
                assertNotNull(sxpConnection.getDestination());
                assertNotNull(sxpConnection.getLocalAddress());
        }

        @Test public void testSetInetSocketAddressesException0() throws Exception {
                exception.expect(SocketAddressNotRecognizedException.class);
                sxpConnection.setInetSocketAddresses(mock(SocketAddress.class));
        }

        @Test public void testGetContextType() throws Exception {
                assertEquals(SxpConnection.ChannelHandlerContextType.None,
                        sxpConnection.getContextType(mock(ChannelHandlerContext.class)));
                ChannelHandlerContext context = mock(ChannelHandlerContext.class);

                sxpConnection.markChannelHandlerContext(context,
                        SxpConnection.ChannelHandlerContextType.ListenerContext);
                assertEquals(SxpConnection.ChannelHandlerContextType.ListenerContext,
                        sxpConnection.getContextType(context));

                context = mock(ChannelHandlerContext.class);
                sxpConnection.markChannelHandlerContext(context, SxpConnection.ChannelHandlerContextType.SpeakerContext);
                assertEquals(SxpConnection.ChannelHandlerContextType.SpeakerContext,
                        sxpConnection.getContextType(context));
        }

        private SxpBindingFilter<?,SxpFilter> getFilter(FilterType type, String name) {
                SxpBindingFilter<?,SxpFilter> bindingFilter = mock(SxpBindingFilter.class);
                when(bindingFilter.getIdentifier()).thenReturn(name);
                SxpFilterBuilder builder = new SxpFilterBuilder();
                builder.setFilterType(type);
                builder.setFilterSpecific(FilterSpecific.AccessOrPrefixList);
                builder.setFilterEntries(mock(AclFilterEntries.class));
                when(bindingFilter.getSxpFilter()).thenReturn(builder.build());
                return bindingFilter;
        }

        @Test public void testGetFilter() throws Exception {
                assertNull(sxpConnection.getFilter(FilterType.Inbound));
                assertNull(sxpConnection.getFilter(FilterType.Outbound));

                sxpConnection.putFilter(getFilter(FilterType.Inbound, "TEST"));
                assertNotNull(sxpConnection.getFilter(FilterType.Inbound));
                assertNull(sxpConnection.getFilter(FilterType.Outbound));

                sxpConnection.putFilter(getFilter(FilterType.Outbound, "TEST2"));
                assertNotNull(sxpConnection.getFilter(FilterType.Inbound));
                assertNotNull(sxpConnection.getFilter(FilterType.Outbound));
        }

        @Test public void testPutFilter() throws Exception {
                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Both, ConnectionState.On),
                                DOMAIN_NAME);
                sxpConnection.markChannelHandlerContext(mock(ChannelHandlerContext.class),
                        SxpConnection.ChannelHandlerContextType.SpeakerContext);
                assertNull(sxpConnection.getFilter(FilterType.Inbound));
                assertNull(sxpConnection.getFilter(FilterType.Outbound));
                assertNull(sxpConnection.getFilter(FilterType.InboundDiscarding));

                sxpConnection.putFilter(getFilter(FilterType.Inbound, "TEST"));
                verify(sxpNode.getWorker(), atLeastOnce()).executeTaskInSequence(any(Callable.class),
                        eq(ThreadsWorker.WorkerType.INBOUND), eq(sxpConnection));
                sxpConnection.putFilter(getFilter(FilterType.InboundDiscarding, "TEST1"));
                verify(sxpNode.getWorker(), atLeastOnce()).executeTaskInSequence(any(Callable.class),
                        eq(ThreadsWorker.WorkerType.INBOUND), eq(sxpConnection));
                sxpConnection.putFilter(getFilter(FilterType.Outbound, "TEST2"));
                verify(sxpNode.getWorker(), atLeastOnce()).executeTaskInSequence(any(Callable.class),
                        eq(ThreadsWorker.WorkerType.OUTBOUND), eq(sxpConnection));

                assertNotNull(sxpConnection.getFilter(FilterType.Inbound));
                assertNotNull(sxpConnection.getFilter(FilterType.InboundDiscarding));
                assertNotNull(sxpConnection.getFilter(FilterType.Outbound));
        }

        @Test public void testGetGroupName() throws Exception {
                assertNull(sxpConnection.getGroupName(FilterType.Inbound));
                assertNull(sxpConnection.getGroupName(FilterType.Outbound));
                assertNull(sxpConnection.getGroupName(FilterType.InboundDiscarding));

                sxpConnection.putFilter(getFilter(FilterType.Inbound, "TEST"));
                sxpConnection.putFilter(getFilter(FilterType.InboundDiscarding, "TEST1"));
                sxpConnection.putFilter(getFilter(FilterType.Outbound, "TEST2"));

                assertEquals("TEST", sxpConnection.getGroupName(FilterType.Inbound));
                assertEquals("TEST1", sxpConnection.getGroupName(FilterType.InboundDiscarding));
                assertEquals("TEST2", sxpConnection.getGroupName(FilterType.Outbound));
        }

        @Test public void testRemoveFilter() throws Exception {
                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Both, ConnectionState.On),
                                DOMAIN_NAME);
                sxpConnection.markChannelHandlerContext(mock(ChannelHandlerContext.class),
                        SxpConnection.ChannelHandlerContextType.SpeakerContext);
                assertNull(sxpConnection.getFilter(FilterType.Inbound));
                assertNull(sxpConnection.getFilter(FilterType.InboundDiscarding));
                assertNull(sxpConnection.getFilter(FilterType.Outbound));

                sxpConnection.putFilter(getFilter(FilterType.Inbound, "TEST"));
                sxpConnection.putFilter(getFilter(FilterType.InboundDiscarding, "TEST1"));
                sxpConnection.putFilter(getFilter(FilterType.Outbound, "TEST2"));

                assertNotNull(sxpConnection.getFilter(FilterType.Inbound));
                assertNotNull(sxpConnection.getFilter(FilterType.InboundDiscarding));
                assertNotNull(sxpConnection.getFilter(FilterType.Outbound));

                sxpConnection.removeFilter(FilterType.Inbound, FilterSpecific.AccessOrPrefixList);
                assertNull(sxpConnection.getFilter(FilterType.Inbound));
                verify(sxpNode.getWorker(), atLeastOnce()).executeTaskInSequence(any(Callable.class),
                        eq(ThreadsWorker.WorkerType.OUTBOUND), eq(sxpConnection));

                sxpConnection.removeFilter(FilterType.InboundDiscarding, FilterSpecific.AccessOrPrefixList);
                assertNull(sxpConnection.getFilter(FilterType.InboundDiscarding));
                verify(sxpNode.getWorker(), atLeastOnce()).executeTaskInSequence(any(Callable.class),
                        eq(ThreadsWorker.WorkerType.INBOUND), eq(sxpConnection));

                sxpConnection.removeFilter(FilterType.Outbound, FilterSpecific.AccessOrPrefixList);
                assertNull(sxpConnection.getFilter(FilterType.Outbound));
        }

        @Test public void testSetCapabilitiesRemote() throws Exception {
                sxpConnection =
                        SxpConnection.create(sxpNode, mockConnection(ConnectionMode.Both, ConnectionState.On),
                                DOMAIN_NAME);
                assertTrue(sxpConnection.getCapabilitiesRemote().isEmpty());
                List<CapabilityType> capabilityTypes = new ArrayList<>();
                capabilityTypes.add(CapabilityType.Ipv4Unicast);
                sxpConnection.setCapabilitiesRemote(capabilityTypes);
                assertTrue(sxpConnection.getCapabilitiesRemote().contains(CapabilityType.Ipv4Unicast));

                capabilityTypes.clear();
                capabilityTypes.add(CapabilityType.Ipv6Unicast);
                sxpConnection.setCapabilitiesRemote(capabilityTypes);
                assertFalse(sxpConnection.getCapabilitiesRemote().contains(CapabilityType.Ipv4Unicast));
                assertTrue(sxpConnection.getCapabilitiesRemote().contains(CapabilityType.Ipv6Unicast));
        }

        @Test public void testConnectionGetters() throws Exception {
                sxpConnection =
                        SxpConnection.create(sxpNode,
                                mockConnection(ConnectionMode.Speaker, ConnectionState.AdministrativelyDown),
                                DOMAIN_NAME);
                assertFalse(sxpConnection.getCapabilities().isEmpty());
                assertTrue(sxpConnection.getCapabilitiesRemote().isEmpty());
                assertNotNull(sxpConnection.getConnection());
                assertNotNull(sxpConnection.getContext());

                assertEquals(-1, sxpConnection.getTimestampUpdateOrKeepAliveMessage());
                sxpConnection.setUpdateOrKeepaliveMessageTimestamp();
                assertNotEquals(-1, sxpConnection.getTimestampUpdateOrKeepAliveMessage());

                assertFalse(sxpConnection.isStatePendingOn());
                sxpConnection.setStatePendingOn();
                assertTrue(sxpConnection.isStatePendingOn());
        }

        @Test public void testSetTimers() throws Exception {
                ConnectionTimers
                        timers =
                        new ConnectionTimersBuilder().setHoldTime(60)
                                .setHoldTimeMax(180)
                                .setHoldTimeMinAcceptable(90)
                                .build();
                sxpConnection.setTimers(timers);
                assertEquals(60, sxpConnection.getHoldTime());
                assertEquals(180, sxpConnection.getHoldTimeMax());
                assertEquals(90, sxpConnection.getHoldTimeMinAcceptable());
        }

        @Test public void testSetCapabilities() throws Exception {
                Capabilities capabilities = Configuration.getCapabilities(Version.Version4);
                sxpConnection.setCapabilities(capabilities);
                assertFalse(sxpConnection.getCapabilitiesRemote().contains(CapabilityType.Ipv4Unicast));
                assertFalse(sxpConnection.getCapabilitiesRemote().contains(CapabilityType.Ipv6Unicast));
                assertFalse(sxpConnection.getCapabilitiesRemote().contains(CapabilityType.LoopDetection));
                assertFalse(sxpConnection.getCapabilitiesRemote().contains(CapabilityType.SubnetBindings));
                assertFalse(sxpConnection.getCapabilitiesRemote().contains(CapabilityType.SxpCapabilityExchange));
        }

        @Test public void testSetVersion() throws Exception {
                sxpConnection.setVersion(Version.Version1);
                assertEquals(Version.Version1, sxpConnection.getVersion());
        }

        @Test public void testSetState() throws Exception {
                sxpConnection.setState(ConnectionState.DeleteHoldDown);
                assertEquals(ConnectionState.DeleteHoldDown, sxpConnection.getState());
        }

        @Test public void testSetNodeIdRemote() throws Exception {
                NodeId nodeId = new NodeId("1.1.1.1");
                sxpConnection.setNodeIdRemote(nodeId);
                assertEquals(nodeId, sxpConnection.getNodeIdRemote());
        }
}
